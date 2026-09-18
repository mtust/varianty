import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useEffect, useRef, useState } from 'react';
import { FlatList, StyleSheet, View } from 'react-native';
import { ActivityIndicator, Button, Snackbar, Text } from 'react-native-paper';

import { RootStackParamList } from '@/navigation/types';
import { useAuthStore } from '@/stores/authStore';
import { useGameStore } from '@/stores/gameStore';
import { useShallow } from 'zustand/react/shallow';

type Props = NativeStackScreenProps<RootStackParamList, 'Lobby'>;

export function LobbyScreen({ route, navigation }: Props) {
  const { code, isHost } = route.params;
  const token = useAuthStore((state) => state.token);
  const { snapshot, error, clearError, connectAndJoin, leaveRoom, startGame } = useGameStore(
    useShallow((state) => ({
      snapshot: state.snapshot,
      error: state.error,
      clearError: state.clearError,
      connectAndJoin: state.connectAndJoin,
      leaveRoom: state.leaveRoom,
      startGame: state.startGame,
    }))
  );
  const [starting, setStarting] = useState(false);
  const advancingToGame = useRef(false);

  useEffect(() => {
    connectAndJoin(token!, code, isHost).catch((err: Error) => {
      useGameStore.setState({ error: err.message || 'Не вдалося підключитися до кімнати' });
    });
    return () => {
      // Don't tear down the connection when we're navigating forward into the
      // Game screen for this same room — only when actually leaving the lobby.
      if (!advancingToGame.current) {
        leaveRoom();
      }
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [code]);

  useEffect(() => {
    if (snapshot && snapshot.status !== 'LOBBY') {
      advancingToGame.current = true;
      navigation.replace('Game');
    }
  }, [snapshot?.status, navigation]);

  if (!snapshot) {
    return (
      <View style={styles.center}>
        {error ? (
          <>
            <Text style={styles.loadingText}>{error}</Text>
            <Button onPress={() => navigation.goBack()}>Назад</Button>
          </>
        ) : (
          <>
            <ActivityIndicator size="large" />
            <Text style={styles.loadingText}>Підключення до кімнати...</Text>
          </>
        )}
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <Text variant="displaySmall" style={styles.code}>
        {snapshot.code}
      </Text>
      <Text style={styles.hostLabel}>
        {snapshot.hostless ? `Кімнату створив: ${snapshot.hostDisplayName}` : `Ведучий: ${snapshot.hostDisplayName}`}
      </Text>

      <FlatList
        style={styles.list}
        data={snapshot.players}
        keyExtractor={(item) => item.id}
        ListEmptyComponent={<Text style={styles.empty}>Ще немає гравців — поділіться кодом кімнати</Text>}
        renderItem={({ item }) => (
          <View style={styles.playerRow}>
            <Text style={styles.playerName}>{item.displayName}</Text>
            {!item.connected && <Text style={styles.disconnected}>відключився</Text>}
          </View>
        )}
      />

      {isHost && (
        <Button
          mode="contained"
          loading={starting}
          disabled={starting || snapshot.players.length < 2}
          onPress={() => {
            setStarting(true);
            startGame();
          }}
        >
          Почати гру ({snapshot.players.length} гравців)
        </Button>
      )}
      {isHost && snapshot.players.length < 2 && (
        <Text style={styles.hint}>Потрібно щонайменше 2 гравці</Text>
      )}
      {!isHost && (
        <Text style={styles.hint}>
          {snapshot.hostless ? 'Очікуємо, поки організатор почне гру...' : 'Очікуємо, поки ведучий почне гру...'}
        </Text>
      )}

      <Snackbar visible={!!error} onDismiss={clearError} duration={4000}>
        {error}
      </Snackbar>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 24 },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  loadingText: { marginTop: 12 },
  code: { textAlign: 'center', letterSpacing: 8, fontWeight: '700', marginBottom: 4 },
  hostLabel: { textAlign: 'center', opacity: 0.7, marginBottom: 24 },
  list: { flex: 1 },
  playerRow: { flexDirection: 'row', justifyContent: 'space-between', paddingVertical: 10 },
  playerName: { fontSize: 16 },
  disconnected: { opacity: 0.5, fontStyle: 'italic' },
  empty: { textAlign: 'center', opacity: 0.6, marginTop: 24 },
  hint: { textAlign: 'center', marginTop: 12, opacity: 0.7 },
});
