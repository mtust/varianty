import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useEffect, useRef, useState } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, View } from 'react-native';
import { Button, Card, HelperText, IconButton, Snackbar, Text, TextInput } from 'react-native-paper';
import { useShallow } from 'zustand/react/shallow';

import { useT } from '@/i18n';
import { RootStackParamList } from '@/navigation/types';
import { MAX_ROUNDS } from '@/services/room.service';
import { useGameStore } from '@/stores/gameStore';
import { theme } from '@/theme/theme';

type Props = NativeStackScreenProps<RootStackParamList, 'CustomQuestions'>;

// Same limits as the server.
const MAX_FACT_LENGTH = 300;
const MAX_ANSWER_LENGTH = 120;

// The host writes their own questions before a game. They are kept on the server for this
// room and asked first; the other players only see how many there are.
export function CustomQuestionsScreen({ navigation }: Props) {
  const t = useT();
  const { snapshot, customQuestions, error, addCustomQuestion, removeCustomQuestion, clearError } = useGameStore(
    useShallow((s) => ({
      snapshot: s.snapshot,
      customQuestions: s.customQuestions,
      error: s.error,
      addCustomQuestion: s.addCustomQuestion,
      removeCustomQuestion: s.removeCustomQuestion,
      clearError: s.clearError,
    }))
  );
  const [fact, setFact] = useState('');
  const [answer, setAnswer] = useState('');
  const [sending, setSending] = useState(false);
  const previousCount = useRef(customQuestions.length);

  // The server answers with the updated list; once it has grown, the question was saved.
  useEffect(() => {
    if (customQuestions.length > previousCount.current) {
      setFact('');
      setAnswer('');
    }
    previousCount.current = customQuestions.length;
    setSending(false);
  }, [customQuestions]);

  useEffect(() => {
    if (error) setSending(false);
  }, [error]);

  // Left the room (or got removed from it) while editing.
  useEffect(() => {
    if (!snapshot) navigation.goBack();
  }, [snapshot, navigation]);

  if (!snapshot) return null;

  const editable = snapshot.status === 'LOBBY' || snapshot.status === 'FINISHED';
  const full = customQuestions.length >= MAX_ROUNDS;
  const canAdd = editable && !full && !sending && fact.trim().length > 0 && answer.trim().length > 0;
  const moreThanRounds = snapshot.status === 'LOBBY' && customQuestions.length > snapshot.totalRounds;

  const add = () => {
    if (!canAdd) return;
    if (addCustomQuestion(fact.trim(), answer.trim())) {
      setSending(true);
    }
  };

  return (
    <KeyboardAvoidingView style={styles.flex} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScrollView contentContainerStyle={styles.container} keyboardShouldPersistTaps="handled">
        <Text variant="bodyMedium" style={styles.intro}>
          {t('custom.intro')}
        </Text>

        {editable ? (
          <>
            <TextInput
              mode="outlined"
              label={t('custom.fact')}
              value={fact}
              onChangeText={setFact}
              multiline
              maxLength={MAX_FACT_LENGTH}
              disabled={full}
            />
            <HelperText type="info">{t('custom.factHint')}</HelperText>
            <TextInput
              mode="outlined"
              label={t('custom.answer')}
              value={answer}
              onChangeText={setAnswer}
              maxLength={MAX_ANSWER_LENGTH}
              disabled={full}
              returnKeyType="done"
              onSubmitEditing={add}
            />
            <Button
              mode="contained"
              icon="plus"
              style={styles.addButton}
              loading={sending}
              disabled={!canAdd}
              onPress={add}
            >
              {t('custom.add')}
            </Button>
          </>
        ) : (
          <Text style={styles.locked}>{t('custom.locked')}</Text>
        )}

        <Text variant="titleSmall" style={styles.count}>
          {t('custom.count', { count: customQuestions.length, max: MAX_ROUNDS })}
        </Text>
        {moreThanRounds && (
          <Text style={styles.hint}>
            {t('custom.moreRounds', { rounds: snapshot.totalRounds, count: customQuestions.length })}
          </Text>
        )}
        {customQuestions.length === 0 && <Text style={styles.empty}>{t('custom.empty')}</Text>}

        {customQuestions.map((question, index) => (
          <Card key={question.id} style={styles.card} mode="outlined">
            <View style={styles.cardRow}>
              <View style={styles.cardText}>
                <Text variant="bodyMedium">
                  {index + 1}. {question.fact}
                </Text>
                <Text variant="bodyMedium" style={styles.answer}>
                  {question.answer}
                </Text>
              </View>
              {editable && (
                <IconButton
                  icon="delete-outline"
                  iconColor={theme.colors.error}
                  accessibilityLabel={t('custom.remove')}
                  onPress={() => removeCustomQuestion(question.id)}
                />
              )}
            </View>
          </Card>
        ))}
      </ScrollView>

      <Snackbar visible={!!error} onDismiss={clearError} duration={4000}>
        {error}
      </Snackbar>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1 },
  container: { padding: 24, paddingBottom: 48 },
  intro: { opacity: 0.8, marginBottom: 16 },
  addButton: { marginTop: 12 },
  locked: { textAlign: 'center', opacity: 0.7, marginVertical: 12 },
  count: { marginTop: 24, marginBottom: 8 },
  hint: { opacity: 0.7, marginBottom: 8 },
  empty: { opacity: 0.6, marginTop: 8 },
  card: { marginTop: 8 },
  cardRow: { flexDirection: 'row', alignItems: 'center', paddingLeft: 16, paddingVertical: 8 },
  cardText: { flex: 1 },
  answer: { marginTop: 4, fontWeight: '700', color: theme.colors.primary },
});
