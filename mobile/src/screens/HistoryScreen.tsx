import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useEffect, useState } from 'react';
import { FlatList, StyleSheet, View } from 'react-native';
import { ActivityIndicator, Card, Text } from 'react-native-paper';

import { useLanguageStore, useT } from '@/i18n';
import { RootStackParamList } from '@/navigation/types';
import { historyService } from '@/services/history.service';
import { GameHistoryEntry } from '@/types/game';

type Props = NativeStackScreenProps<RootStackParamList, 'History'>;

export function HistoryScreen({}: Props) {
  const t = useT();
  const language = useLanguageStore((state) => state.language);
  const [entries, setEntries] = useState<GameHistoryEntry[] | null>(null);

  useEffect(() => {
    historyService
      .myHistory()
      .then(setEntries)
      .catch(() => setEntries([]));
  }, []);

  if (!entries) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" />
      </View>
    );
  }

  return (
    <FlatList
      contentContainerStyle={styles.container}
      data={entries}
      keyExtractor={(item) => item.id}
      ListEmptyComponent={<Text style={styles.empty}>{t('history.empty')}</Text>}
      renderItem={({ item }) => (
        <Card style={styles.card}>
          <Card.Content>
            <Text variant="titleMedium">
              {t('history.room', { code: item.roomCode, placement: item.placement, count: item.playerCount })}
            </Text>
            <Text style={styles.details}>
              {t('history.score', { score: item.score, date: new Date(item.playedAt).toLocaleDateString(language) })}
            </Text>
          </Card.Content>
        </Card>
      )}
    />
  );
}

const styles = StyleSheet.create({
  container: { padding: 16 },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  card: { marginBottom: 12 },
  details: { opacity: 0.6, marginTop: 4 },
  empty: { textAlign: 'center', marginTop: 48, opacity: 0.6 },
});
