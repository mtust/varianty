import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useEffect, useState } from 'react';
import { FlatList, StyleSheet, View } from 'react-native';
import { ActivityIndicator, Card, Text } from 'react-native-paper';

import { RootStackParamList } from '@/navigation/types';
import { historyService } from '@/services/history.service';
import { GameHistoryEntry } from '@/types/game';

type Props = NativeStackScreenProps<RootStackParamList, 'History'>;

export function HistoryScreen({}: Props) {
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
      ListEmptyComponent={<Text style={styles.empty}>Ви ще не завершили жодної гри</Text>}
      renderItem={({ item }) => (
        <Card style={styles.card}>
          <Card.Content>
            <Text variant="titleMedium">
              Кімната {item.roomCode} — {item.placement} місце з {item.playerCount}
            </Text>
            <Text style={styles.details}>
              {item.score} балів · {new Date(item.playedAt).toLocaleDateString()}
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
