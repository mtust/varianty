import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { Button, HelperText, SegmentedButtons, Text } from 'react-native-paper';

import { LanguageSwitch } from '@/components/LanguageSwitch';
import { useLanguageStore, useT } from '@/i18n';
import { RootStackParamList } from '@/navigation/types';
import { QuestionCategory, roomService } from '@/services/room.service';
import { useAuthStore } from '@/stores/authStore';
import { useShallow } from 'zustand/react/shallow';

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
  const [creating, setCreating] = useState<'host' | 'hostless' | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleCreateRoom = async (hostless: boolean) => {
    setCreating(hostless ? 'hostless' : 'host');
    setError(null);
    try {
      const { code } = await roomService.createRoom(hostless, category, language);
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
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 24, justifyContent: 'center' },
  greeting: { textAlign: 'center', marginBottom: 32 },
  categoryLabel: { textAlign: 'center', opacity: 0.7, marginBottom: 8 },
  button: { marginTop: 12 },
  signOut: { marginTop: 32 },
});
