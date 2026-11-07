import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useEffect, useRef, useState } from 'react';
import * as Clipboard from 'expo-clipboard';
import { FlatList, Pressable, StyleSheet, View } from 'react-native';
import { ActivityIndicator, Button, Icon, IconButton, Snackbar, Text } from 'react-native-paper';

import { confirmKick, showKickedAlert } from '@/components/confirmKick';

import { useT } from '@/i18n';
import { RootStackParamList } from '@/navigation/types';
import { useAuthStore } from '@/stores/authStore';
import { useGameStore } from '@/stores/gameStore';
import { theme } from '@/theme/theme';
import { useShallow } from 'zustand/react/shallow';

type Props = NativeStackScreenProps<RootStackParamList, 'Lobby'>;

export function LobbyScreen({ route, navigation }: Props) {
  const t = useT();
  const { code, isHost } = route.params;
  const token = useAuthStore((state) => state.token);
  const playerId = useAuthStore((state) => state.playerId);
  const { snapshot, error, kicked, clearError, connectAndJoin, leaveRoom, startGame, kickPlayer } = useGameStore(
    useShallow((state) => ({
      snapshot: state.snapshot,
      error: state.error,
      kicked: state.kicked,
      kickPlayer: state.kickPlayer,
      clearError: state.clearError,
      connectAndJoin: state.connectAndJoin,
      leaveRoom: state.leaveRoom,
      startGame: state.startGame,
    }))
  );
  const [starting, setStarting] = useState(false);
  const [copied, setCopied] = useState(false);
  const advancingToGame = useRef(false);

  useEffect(() => {
    connectAndJoin(token!, code, isHost).catch((err: Error) => {
      useGameStore.setState({ error: err.message || t('errors.connectRoom') });
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
    if (kicked) {
      showKickedAlert(() => {
        leaveRoom();
        navigation.popToTop();
      });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [kicked]);

  useEffect(() => {
    if (snapshot && snapshot.status !== 'LOBBY' && !kicked) {
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
            <Button onPress={() => navigation.goBack()}>{t('lobby.back')}</Button>
          </>
        ) : (
          <>
            <ActivityIndicator size="large" />
            <Text style={styles.loadingText}>{t('lobby.connecting')}</Text>
          </>
        )}
      </View>
    );
  }

  return (
    <View style={styles.container}>
      {isHost ? (
        <Pressable
          onPress={async () => {
            await Clipboard.setStringAsync(snapshot.code);
            setCopied(true);
          }}
          accessibilityRole="button"
          accessibilityLabel={t('lobby.copyCode')}
          style={({ pressed }) => [styles.codeRow, pressed && styles.codePressed]}
        >
          <Text variant="displaySmall" style={styles.code}>
            {snapshot.code}
          </Text>
          <View style={styles.copyIcon}>
            <Icon source="content-copy" size={22} color={theme.colors.primary} />
          </View>
        </Pressable>
      ) : (
        <Text variant="displaySmall" style={styles.code}>
          {snapshot.code}
        </Text>
      )}
      <Text style={styles.hostLabel}>
        {t(snapshot.hostless ? 'lobby.creator' : 'lobby.host', { name: snapshot.hostDisplayName })}
      </Text>

      <FlatList
        style={styles.list}
        data={snapshot.players}
        keyExtractor={(item) => item.id}
        ListEmptyComponent={<Text style={styles.empty}>{t('lobby.empty')}</Text>}
        renderItem={({ item }) => (
          <View style={styles.playerRow}>
            <Text style={styles.playerName}>{item.displayName}</Text>
            <View style={styles.playerMeta}>
              {!item.connected && <Text style={styles.disconnected}>{t('lobby.disconnected')}</Text>}
              {isHost && item.id !== playerId && (
                <IconButton
                  icon="account-remove"
                  size={20}
                  iconColor={theme.colors.error}
                  style={styles.removeButton}
                  accessibilityLabel={t('lobby.removeLabel', { name: item.displayName })}
                  onPress={() => confirmKick(item, () => kickPlayer(item.id))}
                />
              )}
            </View>
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
          {t('lobby.start', { count: snapshot.players.length })}
        </Button>
      )}
      {isHost && snapshot.players.length < 2 && (
        <Text style={styles.hint}>{t('lobby.needTwo')}</Text>
      )}
      {!isHost && (
        <Text style={styles.hint}>
          {t(snapshot.hostless ? 'lobby.waitCreator' : 'lobby.waitHost')}
        </Text>
      )}

      <Snackbar visible={!!error} onDismiss={clearError} duration={4000}>
        {error}
      </Snackbar>
      <Snackbar visible={copied && !error} onDismiss={() => setCopied(false)} duration={1500}>
        {t('lobby.codeCopied', { code: snapshot.code })}
      </Snackbar>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 24 },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  loadingText: { marginTop: 12 },
  code: { textAlign: 'center', letterSpacing: 8, fontWeight: '700', marginBottom: 4 },
  codeRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', borderRadius: 12 },
  codePressed: { opacity: 0.6 },
  copyIcon: { marginBottom: 4, marginLeft: 4 },
  hostLabel: { textAlign: 'center', opacity: 0.7, marginBottom: 24 },
  list: { flex: 1 },
  playerRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', minHeight: 44 },
  playerMeta: { flexDirection: 'row', alignItems: 'center' },
  removeButton: { margin: 0 },
  playerName: { fontSize: 16 },
  disconnected: { opacity: 0.5, fontStyle: 'italic' },
  empty: { textAlign: 'center', opacity: 0.6, marginTop: 24 },
  hint: { textAlign: 'center', marginTop: 12, opacity: 0.7 },
});
