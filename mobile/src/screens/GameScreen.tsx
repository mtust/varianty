import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useEffect, useState } from 'react';
import { Alert, ScrollView, StyleSheet, View } from 'react-native';
import { ActivityIndicator, Button, Card, Chip, IconButton, Snackbar, Text, TextInput } from 'react-native-paper';

import { RootStackParamList } from '@/navigation/types';
import { useGameStore } from '@/stores/gameStore';
import { colors } from '@/theme/theme';
import { PlayerScore } from '@/types/game';
import { useShallow } from 'zustand/react/shallow';

type Props = NativeStackScreenProps<RootStackParamList, 'Game'>;

export function GameScreen({ navigation }: Props) {
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
            <Text variant="titleMedium">{snapshot.fact}</Text>
          </Card.Content>
        </Card>
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

      {snapshot.status === 'RESULTS' && state.roundResult && (
        <View>
          {state.roundResult.options.map((option) => {
            const pointsLabel = option.correct
              ? '+2 голосувавшим'
              : option.hostTrap
                ? '−1 голосувавшим'
                : option.votedByDisplayNames.length > 0
                  ? `+${option.votedByDisplayNames.length} автору`
                  : null;
            // Fill an option only if it's the one you voted for — green when you got it
            // right, red when you fell for the trap, grey for another player's answer.
            // The real correct answer still gets a subtle outline even if you didn't pick it.
            const isMyVote = option.id === state.myVoteOptionId;
            const cardStyle = isMyVote
              ? option.correct
                ? styles.correctCard
                : option.hostTrap
                  ? styles.trapCard
                  : styles.neutralCard
              : option.correct
                ? styles.correctOutline
                : undefined;
            const onFill = isMyVote && (option.correct || option.hostTrap) ? styles.onFillText : undefined;
            return (
              <Card key={option.id} style={[styles.resultCard, cardStyle]}>
                <Card.Content>
                  <Text variant="bodyLarge" style={onFill}>
                    {option.text}
                  </Text>
                  <Text style={[styles.authorLabel, onFill]}>{option.authorLabel}</Text>
                  {option.votedByDisplayNames.length > 0 && (
                    <Text style={[styles.votedBy, onFill]}>
                      Голосували: {option.votedByDisplayNames.join(', ')}
                      {pointsLabel ? ` (${pointsLabel})` : ''}
                    </Text>
                  )}
                </Card.Content>
              </Card>
            );
          })}

          <Text variant="titleMedium" style={styles.scoreboardTitle}>
            Рахунок
          </Text>
          {state.roundResult.scoreboard.map((player) => (
            <View key={player.id} style={styles.scoreRow}>
              <Text>{player.displayName}</Text>
              <Text style={styles.scoreDelta}>
                {player.score} {formatDelta(state.roundResult!.scoreDeltas[player.id])}
              </Text>
            </View>
          ))}

          {state.isHost && (
            <Button mode="contained" style={styles.action} onPress={() => nextRound()}>
              {state.roundResult.gameFinished ? 'Завершити гру' : 'Наступний раунд'}
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

function formatDelta(delta: number | undefined) {
  if (!delta) return '';
  return delta > 0 ? `(+${delta})` : `(${delta})`;
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
  correctAnswer: { opacity: 0.6, marginBottom: 4 },
  instructions: { marginBottom: 12 },
  action: { marginTop: 16 },
  optionButton: { marginTop: 10 },
  waiting: { alignItems: 'center', paddingVertical: 32 },
  waitingText: { marginTop: 12, opacity: 0.7 },
  resultCard: { marginBottom: 10 },
  correctCard: { backgroundColor: colors.correct },
  correctOutline: { borderColor: colors.correct, borderWidth: 2 },
  trapCard: { backgroundColor: colors.trap },
  neutralCard: { backgroundColor: '#DADCE3' },
  onFillText: { color: '#FFFFFF' },
  authorLabel: { opacity: 0.8, marginTop: 4, fontWeight: '600' },
  votedBy: { marginTop: 4, fontSize: 12, opacity: 0.5 },
  scoreboardTitle: { marginTop: 20, marginBottom: 8 },
  scoreRow: { flexDirection: 'row', justifyContent: 'space-between', paddingVertical: 6 },
  scoreDelta: { fontWeight: '600' },
  finishedTitle: { textAlign: 'center', marginBottom: 20 },
});
