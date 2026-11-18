import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useEffect, useMemo, useRef, useState } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, View } from 'react-native';
import { Button, Card, HelperText, IconButton, SegmentedButtons, Snackbar, Text, TextInput } from 'react-native-paper';
import { useShallow } from 'zustand/react/shallow';

import { TranslationKey } from '@/i18n/translations';
import { useT } from '@/i18n';
import { RootStackParamList } from '@/navigation/types';
import { MAX_ROUNDS } from '@/services/room.service';
import { useGameStore } from '@/stores/gameStore';
import { theme } from '@/theme/theme';

type Props = NativeStackScreenProps<RootStackParamList, 'CustomQuestions'>;

// Same limits as the server.
const MAX_FACT_LENGTH = 300;
const MAX_ANSWER_LENGTH = 120;

type ParsedLine = { fact: string; answer: string };
type LineError = { key: TranslationKey; line: number };

/**
 * Bulk format: one question per line, "question text with _ for the answer | correct answer".
 * Empty lines are skipped; the last | separates the answer, so the question itself may contain one.
 */
function parseBulk(text: string): {
  questions: ParsedLine[];
  errors: LineError[];
} {
  const questions: ParsedLine[] = [];
  const errors: LineError[] = [];
  text.split(/\r?\n/).forEach((raw, index) => {
    const line = raw.trim();
    if (!line) return;
    const separator = line.lastIndexOf('|');
    if (separator < 0) {
      errors.push({ key: 'custom.bulkNoSeparator', line: index + 1 });
      return;
    }
    const fact = line.slice(0, separator).trim();
    const answer = line.slice(separator + 1).trim();
    if (!fact || !answer) {
      errors.push({ key: 'custom.bulkEmptyPart', line: index + 1 });
    } else if (fact.length > MAX_FACT_LENGTH || answer.length > MAX_ANSWER_LENGTH) {
      errors.push({ key: 'custom.bulkTooLong', line: index + 1 });
    } else {
      questions.push({ fact, answer });
    }
  });
  return { questions, errors };
}

// The host writes their own questions before a game. They are kept on the server for this
// room and asked first; the other players only see how many there are.
export function CustomQuestionsScreen({ navigation }: Props) {
  const t = useT();
  const { snapshot, customQuestions, error, addCustomQuestion, addCustomQuestions, removeCustomQuestion, clearError } =
    useGameStore(
      useShallow((s) => ({
        snapshot: s.snapshot,
        customQuestions: s.customQuestions,
        error: s.error,
        addCustomQuestion: s.addCustomQuestion,
        addCustomQuestions: s.addCustomQuestions,
        removeCustomQuestion: s.removeCustomQuestion,
        clearError: s.clearError,
      })),
    );
  const [fact, setFact] = useState('');
  const [answer, setAnswer] = useState('');
  const [mode, setMode] = useState<'single' | 'bulk'>('single');
  const [bulkText, setBulkText] = useState('');
  const bulk = useMemo(() => parseBulk(bulkText), [bulkText]);
  const [sending, setSending] = useState(false);
  const previousCount = useRef(customQuestions.length);

  // The server answers with the updated list; once it has grown, the question was saved.
  useEffect(() => {
    if (customQuestions.length > previousCount.current) {
      setFact('');
      setAnswer('');
      setBulkText('');
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
  const left = MAX_ROUNDS - customQuestions.length;
  const bulkTooMany = bulk.questions.length > left;
  const canAddBulk = editable && !sending && bulk.questions.length > 0 && bulk.errors.length === 0 && !bulkTooMany;
  const moreThanRounds = snapshot.status === 'LOBBY' && customQuestions.length > snapshot.totalRounds;

  const add = () => {
    if (!canAdd) return;
    if (addCustomQuestion(fact.trim(), answer.trim())) {
      setSending(true);
    }
  };

  const addBulk = () => {
    if (!canAddBulk) return;
    if (addCustomQuestions(bulk.questions)) {
      setSending(true);
    }
  };

  return (
    <KeyboardAvoidingView style={styles.flex} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScrollView contentContainerStyle={styles.container} keyboardShouldPersistTaps="handled">
        <Text variant="bodyMedium" style={styles.intro}>
          {t('custom.intro')}
        </Text>

        {editable && (
          <SegmentedButtons
            style={styles.modes}
            value={mode}
            onValueChange={(value) => setMode(value as 'single' | 'bulk')}
            buttons={[
              { value: 'single', label: t('custom.modeSingle'), icon: 'plus' },
              {
                value: 'bulk',
                label: t('custom.modeBulk'),
                icon: 'format-list-bulleted',
              },
            ]}
          />
        )}

        {editable && mode === 'bulk' && (
          <>
            <Text variant="bodyMedium">{t('custom.bulkHelp')}</Text>
            <Card mode="contained" style={styles.example}>
              <Card.Content>
                <Text variant="labelLarge" style={styles.exampleTitle}>
                  {t('custom.bulkExampleTitle')}
                </Text>
                <Text variant="bodyMedium" style={styles.exampleText} selectable>
                  {t('custom.bulkExample')}
                </Text>
                <Button
                  compact
                  mode="text"
                  icon="content-paste"
                  style={styles.exampleButton}
                  onPress={() => setBulkText(t('custom.bulkExample'))}
                >
                  {t('custom.bulkUseExample')}
                </Button>
              </Card.Content>
            </Card>
            <TextInput
              mode="outlined"
              label={t('custom.bulkLabel')}
              value={bulkText}
              onChangeText={setBulkText}
              multiline
              numberOfLines={6}
              style={styles.bulkInput}
              autoCapitalize="sentences"
            />
            {bulk.errors.slice(0, 5).map((lineError) => (
              <HelperText key={`${lineError.key}-${lineError.line}`} type="error">
                {t(lineError.key, { line: lineError.line })}
              </HelperText>
            ))}
            {bulkTooMany && <HelperText type="error">{t('custom.bulkTooMany', { max: MAX_ROUNDS, left })}</HelperText>}
            <Button
              mode="contained"
              icon="playlist-plus"
              style={styles.addButton}
              loading={sending}
              disabled={!canAddBulk}
              onPress={addBulk}
            >
              {t('custom.bulkAdd', { count: bulk.questions.length })}
            </Button>
          </>
        )}

        {editable && mode === 'single' && (
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
        )}

        {!editable && <Text style={styles.locked}>{t('custom.locked')}</Text>}

        <Text variant="titleSmall" style={styles.count}>
          {t('custom.count', {
            count: customQuestions.length,
            max: MAX_ROUNDS,
          })}
        </Text>
        {moreThanRounds && (
          <Text style={styles.hint}>
            {t('custom.moreRounds', {
              rounds: snapshot.totalRounds,
              count: customQuestions.length,
            })}
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
  modes: { marginBottom: 16 },
  example: { marginTop: 12, marginBottom: 12 },
  exampleTitle: { marginBottom: 4 },
  exampleText: {
    fontFamily: Platform.select({ ios: 'Menlo', default: 'monospace' }),
    lineHeight: 22,
  },
  exampleButton: { alignSelf: 'flex-start', marginTop: 4, marginLeft: -8 },
  bulkInput: { minHeight: 140 },
  locked: { textAlign: 'center', opacity: 0.7, marginVertical: 12 },
  count: { marginTop: 24, marginBottom: 8 },
  hint: { opacity: 0.7, marginBottom: 8 },
  empty: { opacity: 0.6, marginTop: 8 },
  card: { marginTop: 8 },
  cardRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingLeft: 16,
    paddingVertical: 8,
  },
  cardText: { flex: 1 },
  answer: { marginTop: 4, fontWeight: '700', color: theme.colors.primary },
});
