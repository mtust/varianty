import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useEffect, useState } from 'react';
import { Alert, ScrollView, StyleSheet, View } from 'react-native';
import { ActivityIndicator, Button, Card, Chip, Icon, IconButton, Snackbar, Text, TextInput } from 'react-native-paper';

import { RootStackParamList } from '@/navigation/types';
import { useAuthStore } from '@/stores/authStore';
import { useGameStore } from '@/stores/gameStore';
import { colors, theme } from '@/theme/theme';
import { PlayerScore, RevealedOption } from '@/types/game';
import { useShallow } from 'zustand/react/shallow';

type Props = NativeStackScreenProps<RootStackParamList, 'Game'>;

export function GameScreen({ navigation }: Props) {
  const playerId = useAuthStore((s) => s.playerId);
  const state = useGameStore(
    useShallow((s) => ({
      isHost: s.isHost,
      snapshot: s.snapshot,
      hostQuestion: s.hostQuestion,
      votingOptions: s.votingOptions,
      roundResult: s.roundResult,
      mySubmittedAnswer: s.mySubmittedAnswer,
      myVoted: s.myVoted,
      myVoteOptionId: s.myVoteOptionId,
      error: s.error,
    }))
  );
  const { submitTrap, submitAnswer, submitVote, nextRound, leaveRoom, clearError } = useGameStore(
    useShallow((s) => ({
      submitTrap: s.submitTrap,
      submitAnswer: s.submitAnswer,
      submitVote: s.submitVote,
      nextRound: s.nextRound,
      leaveRoom: s.leaveRoom,
      clearError: s.clearError,
    }))
  );

  const [trapText, setTrapText] = useState('');
  const [answerText, setAnswerText] = useState('');

  useEffect(() => {
    setTrapText('');
    setAnswerText('');
  }, [state.snapshot?.roundNumber]);

  const [remaining, setRemaining] = useState<number | null>(null);

  // Resync the countdown from the server whenever a fresh timed snapshot arrives —
  // the server is the source of truth for the deadline.
  useEffect(() => {
    const status = state.snapshot?.status;
    if (status === 'ANSWERING' || status === 'VOTING') {
      setRemaining(state.snapshot!.secondsRemaining);
    } else {
      setRemaining(null);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [state.snapshot?.status, state.snapshot?.secondsRemaining, state.snapshot?.roundNumber]);

  // Tick the countdown down locally between server updates.
  useEffect(() => {
    if (remaining === null || remaining <= 0) return;
    const id = setTimeout(() => setRemaining((r) => (r === null ? null : r - 1)), 1000);
    return () => clearTimeout(id);
  }, [remaining]);

  const snapshot = state.snapshot;
  if (!snapshot) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" />
      </View>
    );
  }

  // In a hostless room the creator plays like everyone else — only a "real"
  // (non-playing, trap-writing) host should be excluded from answering/voting.
  const isNonPlayingHost = state.isHost && !snapshot.hostless;
  const roundResult = snapshot.status === 'RESULTS' ? state.roundResult : null;
  const correctAnswer = roundResult?.options.find((option) => option.correct)?.text;
  // A non-playing host isn't in scoreDeltas, so they get no personal points line.
  const myDelta = roundResult && playerId ? roundResult.scoreDeltas[playerId] : undefined;

  const goHome = () => {
    leaveRoom();
    navigation.popToTop();
  };

  const confirmLeave = () => {
    Alert.alert('Вийти з кімнати?', 'Ви покинете гру, і вона продовжиться без вас.', [
      { text: 'Скасувати', style: 'cancel' },
      { text: 'Вийти', style: 'destructive', onPress: goHome },
    ]);
  };

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <View style={styles.header}>
        <View style={styles.headerLeft}>
          <Chip icon="numeric">
            Раунд {snapshot.roundNumber}/{snapshot.totalRounds}
          </Chip>
          {remaining !== null && (
            <Chip icon="timer-outline" style={[styles.timerChip, remaining <= 10 && styles.timerUrgent]}>
              {remaining}с
            </Chip>
          )}
        </View>
        <IconButton icon="exit-to-app" onPress={confirmLeave} accessibilityLabel="Вийти з кімнати" />
      </View>

      <ScoreBoard players={snapshot.players} />

      {snapshot.fact && (
        <Card style={styles.factCard}>
          <Card.Content>
            <FactText fact={snapshot.fact} answer={correctAnswer} />
          </Card.Content>
        </Card>
      )}

      {snapshot.status === 'RESULTS' && (
        <NextRoundBanner
          isHost={state.isHost}
          hostless={snapshot.hostless}
          hostName={snapshot.hostDisplayName}
          lastRound={snapshot.roundNumber >= snapshot.totalRounds}
        />
      )}

      {snapshot.status === 'TRAP' && state.isHost && (
        <View>
          <Text style={styles.correctAnswer}>
            Правильна відповідь: {state.hostQuestion?.correctAnswer ?? '...'}
          </Text>
          <Text style={styles.instructions}>Придумайте переконливу, але хибну відповідь-пастку</Text>
          <TextInput mode="outlined" label="Ваша пастка" value={trapText} onChangeText={setTrapText} />
          <Button mode="contained" style={styles.action} disabled={!trapText.trim()} onPress={() => submitTrap(trapText.trim())}>
            Надіслати пастку
          </Button>
        </View>
      )}
      {snapshot.status === 'TRAP' && !state.isHost && (
        <WaitingBlock text="Ведучий готує пастку..." />
      )}

      {snapshot.status === 'ANSWERING' && isNonPlayingHost && (
        <WaitingBlock text={`Гравці відповідають: ${snapshot.answeredCount}/${snapshot.players.length}`} />
      )}
      {snapshot.status === 'ANSWERING' && !isNonPlayingHost && !state.mySubmittedAnswer && (
        <View>
          <Text style={styles.instructions}>Що, на вашу думку, може бути правильною відповіддю?</Text>
          <TextInput mode="outlined" label="Ваша відповідь" value={answerText} onChangeText={setAnswerText} />
          <Button
            mode="contained"
            style={styles.action}
            disabled={!answerText.trim()}
            onPress={() => submitAnswer(answerText.trim())}
          >
            Надіслати відповідь
          </Button>
        </View>
      )}
      {snapshot.status === 'ANSWERING' && !isNonPlayingHost && state.mySubmittedAnswer && (
        <WaitingBlock text="Очікуємо на інших гравців..." />
      )}

      {snapshot.status === 'VOTING' && isNonPlayingHost && (
        <WaitingBlock text={`Голосування: ${snapshot.votedCount}/${snapshot.players.length}`} />
      )}
      {snapshot.status === 'VOTING' && !isNonPlayingHost && !state.myVoted && state.votingOptions && (
        <View>
          <Text style={styles.instructions}>Яка відповідь, на вашу думку, правильна?</Text>
          {state.votingOptions.options.map((option) => (
            <Button key={option.id} mode="outlined" style={styles.optionButton} onPress={() => submitVote(option.id)}>
              {option.text}
            </Button>
          ))}
        </View>
      )}
      {snapshot.status === 'VOTING' && !isNonPlayingHost && state.myVoted && (
        <WaitingBlock text="Очікуємо на інших гравців..." />
      )}

      {roundResult && (
        <View>
          {myDelta !== undefined && (
            <View style={styles.myDelta}>
              <Text variant="titleMedium">Ваші бали за раунд</Text>
              <Text
                variant="headlineMedium"
                style={[styles.myDeltaValue, myDelta > 0 && styles.gain, myDelta < 0 && styles.loss]}
              >
                {formatPoints(myDelta)}
              </Text>
            </View>
          )}

          {sortOptions(roundResult.options).map((option) => (
            <ResultOptionCard key={option.id} option={option} isMyVote={option.id === state.myVoteOptionId} />
          ))}

          <Text variant="titleMedium" style={styles.scoreboardTitle}>
            Рахунок
          </Text>
          {roundResult.scoreboard.map((player, index) => (
            <View key={player.id} style={[styles.scoreRow, player.id === playerId && styles.myScoreRow]}>
              <Text style={player.id === playerId ? styles.scoreDelta : undefined}>
                {index + 1}. {player.displayName}
                {player.id === playerId ? ' (ви)' : ''}
              </Text>
              <Text style={styles.scoreDelta}>
                {player.score} {formatDelta(roundResult.scoreDeltas[player.id])}
              </Text>
            </View>
          ))}

          {state.isHost && (
            <Button mode="contained" style={styles.action} onPress={() => nextRound()}>
              {roundResult.gameFinished ? 'Завершити гру' : 'Наступний раунд'}
            </Button>
          )}
        </View>
      )}

      {snapshot.status === 'FINISHED' && (
        <View>
          <Text variant="headlineSmall" style={styles.finishedTitle}>
            Гру завершено!
          </Text>
          {snapshot.players.map((player, index) => (
            <View key={player.id} style={styles.scoreRow}>
              <Text>
                {index + 1}. {player.displayName}
              </Text>
              <Text style={styles.scoreDelta}>{player.score}</Text>
            </View>
          ))}
          <Button mode="contained" style={styles.action} onPress={goHome}>
            На головну
          </Button>
        </View>
      )}

      <Snackbar visible={!!state.error} onDismiss={clearError} duration={4000}>
        {state.error}
      </Snackbar>
    </ScrollView>
  );
}

function ScoreBoard({ players }: { players: PlayerScore[] }) {
  if (players.length === 0) return null;
  return (
    <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.scoreboardScroll}>
      {players.map((player) => (
        <Chip key={player.id} style={styles.scoreboardChip} icon={player.connected ? undefined : 'wifi-off'}>
          {player.displayName}: {player.score}
        </Chip>
      ))}
    </ScrollView>
  );
}

function WaitingBlock({ text }: { text: string }) {
  return (
    <View style={styles.waiting}>
      <ActivityIndicator />
      <Text style={styles.waitingText}>{text}</Text>
    </View>
  );
}

// The server sends the fact with a ___ blank; on results it's filled with the real answer.
function FactText({ fact, answer }: { fact: string; answer?: string }) {
  const parts = fact.split('___');
  if (!answer || parts.length < 2) {
    return <Text variant="titleMedium">{fact}</Text>;
  }
  return (
    <Text variant="titleMedium">
      {parts[0]}
      <Text variant="titleMedium" style={styles.factAnswer}>
        {answer}
      </Text>
      {parts.slice(1).join('___')}
    </Text>
  );
}

function NextRoundBanner({
  isHost,
  hostless,
  hostName,
  lastRound,
}: {
  isHost: boolean;
  hostless: boolean;
  hostName: string;
  lastRound: boolean;
}) {
  const who = hostless ? hostName : `ведучий ${hostName}`;
  const text = isHost
    ? `Усі чекають на вас. Перегляньте результати й натисніть «${lastRound ? 'Завершити гру' : 'Наступний раунд'}» унизу`
    : `Раунд завершено. Чекаємо, поки ${who} ${lastRound ? 'завершить гру' : 'почне наступний раунд'}`;
  return (
    <View style={styles.banner} accessibilityLiveRegion="polite">
      <Icon source={isHost ? 'gesture-tap' : 'clock-outline'} size={24} color={theme.colors.secondary} />
      <Text variant="bodyMedium" style={styles.bannerText}>
        {text}
      </Text>
    </View>
  );
}

function ResultOptionCard({ option, isMyVote }: { option: RevealedOption; isMyVote: boolean }) {
  const voters = option.votedByDisplayNames;
  const label = option.correct
    ? '✓ Правильна відповідь'
    : option.hostTrap
      ? 'Пастка ведучого'
      : option.authorLabel.replace(/^Автор: /, 'Брехня: ');
  const pointsLabel =
    voters.length === 0
      ? 'Ніхто не обрав'
      : option.correct
        ? '+2 кожному, хто вгадав'
        : option.hostTrap
          ? '−1 кожному, хто повівся'
          : `+${voters.length} автору`;
  const onFill = option.correct ? styles.onFillText : undefined;

  return (
    <Card
      style={[
        styles.resultCard,
        option.correct && styles.correctCard,
        option.hostTrap && styles.trapCard,
        isMyVote && styles.myVoteCard,
      ]}
    >
      <Card.Content>
        <Text style={[styles.optionLabel, onFill, option.hostTrap && styles.loss]}>{label}</Text>
        <Text variant="titleMedium" style={onFill}>
          {option.text}
        </Text>
        {isMyVote && <Text style={[styles.myVoteLabel, onFill]}>Ваш вибір</Text>}
        {voters.length > 0 && (
          <View style={styles.voters}>
            {voters.map((name, index) => (
              <Chip key={`${name}-${index}`} compact style={styles.voterChip}>
                {name}
              </Chip>
            ))}
          </View>
        )}
        <Text style={[styles.pointsLabel, onFill]}>{pointsLabel}</Text>
      </Card.Content>
    </Card>
  );
}

// The real answer first, then the trap, then the lies that fooled the most players.
function sortOptions(options: RevealedOption[]) {
  const rank = (option: RevealedOption) => (option.correct ? 0 : option.hostTrap ? 1 : 2);
  return [...options].sort(
    (a, b) => rank(a) - rank(b) || b.votedByDisplayNames.length - a.votedByDisplayNames.length
  );
}

function formatPoints(delta: number) {
  if (delta > 0) return `+${delta}`;
  return delta < 0 ? `−${-delta}` : '0';
}

function formatDelta(delta: number | undefined) {
  return delta ? `(${formatPoints(delta)})` : '';
}

const styles = StyleSheet.create({
  container: { padding: 24, paddingTop: 48 },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  header: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: 8 },
  headerLeft: { flexDirection: 'row', alignItems: 'center' },
  timerChip: { marginLeft: 8 },
  timerUrgent: { backgroundColor: colors.trap },
  scoreboardScroll: { marginBottom: 16 },
  scoreboardChip: { marginRight: 8 },
  factCard: { marginBottom: 20 },
  factAnswer: { color: colors.correct, fontWeight: '700' },
  banner: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.bannerSoft,
    borderColor: theme.colors.secondary,
    borderWidth: 1,
    borderRadius: 12,
    padding: 12,
    marginBottom: 16,
  },
  bannerText: { flex: 1, marginLeft: 10, fontWeight: '600' },
  myDelta: {
    alignItems: 'center',
    backgroundColor: colors.highlightSoft,
    borderRadius: 12,
    padding: 12,
    marginBottom: 16,
  },
  myDeltaValue: { fontWeight: '700' },
  gain: { color: colors.correct },
  loss: { color: colors.trap },
  correctAnswer: { opacity: 0.6, marginBottom: 4 },
  instructions: { marginBottom: 12 },
  action: { marginTop: 16 },
  optionButton: { marginTop: 10 },
  waiting: { alignItems: 'center', paddingVertical: 32 },
  waitingText: { marginTop: 12, opacity: 0.7 },
  resultCard: { marginBottom: 10 },
  correctCard: { backgroundColor: colors.correct },
  trapCard: { backgroundColor: colors.trapSoft },
  myVoteCard: { borderColor: theme.colors.primary, borderWidth: 2 },
  onFillText: { color: '#FFFFFF' },
  optionLabel: { fontWeight: '700', marginBottom: 4 },
  myVoteLabel: { marginTop: 4, fontWeight: '600' },
  voters: { flexDirection: 'row', flexWrap: 'wrap', marginTop: 8 },
  voterChip: { marginRight: 6, marginBottom: 6 },
  pointsLabel: { marginTop: 4, fontSize: 13, fontWeight: '600', opacity: 0.8 },
  scoreboardTitle: { marginTop: 20, marginBottom: 8 },
  scoreRow: { flexDirection: 'row', justifyContent: 'space-between', paddingVertical: 6, paddingHorizontal: 8 },
  myScoreRow: { backgroundColor: colors.highlightSoft, borderRadius: 8 },
  scoreDelta: { fontWeight: '600' },
  finishedTitle: { textAlign: 'center', marginBottom: 20 },
});
