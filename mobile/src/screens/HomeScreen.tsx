import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useState } from 'react';
import { Alert, StyleSheet, View } from 'react-native';
import { Button, HelperText, IconButton, SegmentedButtons, Text } from 'react-native-paper';

import { LanguageSwitch } from '@/components/LanguageSwitch';
import { useLanguageStore, useT } from '@/i18n';
import { RootStackParamList } from '@/navigation/types';
import { authService } from '@/services/auth.service';
import { QuestionCategory, roomService } from '@/services/room.service';
import { useAuthStore } from '@/stores/authStore';
import { theme } from '@/theme/theme';
import { useShallow } from 'zustand/react/shallow';

const MIN_ROUNDS = 5;
const MAX_ROUNDS = 20;
const DEFAULT_ROUNDS = 6;

type Props = NativeStackScreenProps<RootStackParamList, 'Home'>;

export function HomeScreen({ navigation }: Props) {
  const t = useT();
  const language = useLanguageStore((state) => state.language);
  const { displayName, guest, signOut } = useAuthStore(
    useShallow((state) => ({
      displayName: state.displayName,
      guest: state.guest,
      signOut: state.signOut,
    }))
  );
  const [category, setCategory] = useState<QuestionCategory>('facts');
  const [rounds, setRounds] = useState(DEFAULT_ROUNDS);
  const [creating, setCreating] = useState<'host' | 'hostless' | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [deleting, setDeleting] = useState(false);

  const deleteAccount = async () => {
    setDeleting(true);
    setError(null);
    try {
      await authService.deleteAccount();
      await signOut();
    } catch {
      setError(t('home.deleteFailed'));
      setDeleting(false);
    }
  };

  const confirmDeleteAccount = () => {
    Alert.alert(t('home.deleteTitle'), t('home.deleteMessage'), [
      { text: t('home.cancel'), style: 'cancel' },
      { text: t('home.deleteConfirm'), style: 'destructive', onPress: deleteAccount },
    ]);
  };

  const handleCreateRoom = async (hostless: boolean) => {
    setCreating(hostless ? 'hostless' : 'host');
    setError(null);
    try {
      const { code } = await roomService.createRoom(hostless, category, language, rounds);
      navigation.navigate('Lobby', { code, isHost: true });
    } catch {
      setError(t('home.createFailed'));
    } finally {
      setCreating(null);
    }
  };

  return (
    <View style={styles.container}>
      <LanguageSwitch />
      <Text variant="titleLarge" style={styles.greeting}>
        {t('home.greeting', { name: displayName ?? '' })}
      </Text>

      <Text style={styles.categoryLabel}>{t('home.category')}</Text>
      <SegmentedButtons
        style={styles.button}
        value={category}
        onValueChange={(value) => setCategory(value as QuestionCategory)}
        buttons={[
          { value: 'facts', label: t('home.facts'), icon: 'lightbulb-outline' },
          { value: 'quotes', label: t('home.quotes'), icon: 'chat-outline' },
        ]}
      />

      <Text style={styles.roundsLabel}>{t('home.rounds')}</Text>
      <View style={styles.stepper}>
        <IconButton
          icon="minus"
          mode="contained-tonal"
          disabled={rounds <= MIN_ROUNDS}
          onPress={() => setRounds((r) => Math.max(MIN_ROUNDS, r - 1))}
        />
        <Text variant="titleLarge" style={styles.roundsValue}>
          {rounds}
        </Text>
        <IconButton
          icon="plus"
          mode="contained-tonal"
          disabled={rounds >= MAX_ROUNDS}
          onPress={() => setRounds((r) => Math.min(MAX_ROUNDS, r + 1))}
        />
      </View>

      <Button
        mode="contained"
        style={styles.button}
        loading={creating === 'host'}
        disabled={!!creating}
        onPress={() => handleCreateRoom(false)}
      >
        {t('home.createHosted')}
      </Button>
      <Button
        mode="contained-tonal"
        style={styles.button}
        loading={creating === 'hostless'}
        disabled={!!creating}
        onPress={() => handleCreateRoom(true)}
      >
        {t('home.createHostless')}
      </Button>
      <Button mode="outlined" style={styles.button} onPress={() => navigation.navigate('JoinRoom')}>
        {t('home.joinByCode')}
      </Button>
      {!guest && (
        <Button mode="text" style={styles.button} onPress={() => navigation.navigate('History')}>
          {t('home.history')}
        </Button>
      )}
      <HelperText type="error" visible={!!error}>
        {error}
      </HelperText>

      <Button mode="text" textColor="#999" style={styles.signOut} onPress={() => signOut()}>
        {t('home.signOut')}
      </Button>
      {!guest && (
        <Button
          mode="text"
          textColor={theme.colors.error}
          loading={deleting}
          disabled={deleting}
          onPress={confirmDeleteAccount}
        >
          {t('home.deleteAccount')}
        </Button>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 24, justifyContent: 'center' },
  greeting: { textAlign: 'center', marginBottom: 32 },
  categoryLabel: { textAlign: 'center', opacity: 0.7, marginBottom: 8 },
  roundsLabel: { textAlign: 'center', opacity: 0.7, marginTop: 16 },
  stepper: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center' },
  roundsValue: { minWidth: 40, textAlign: 'center' },
  button: { marginTop: 12 },
  signOut: { marginTop: 32 },
});
