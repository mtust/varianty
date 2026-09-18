import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { Button, HelperText, Text, TextInput } from 'react-native-paper';

import { RootStackParamList } from '@/navigation/types';
import { authService } from '@/services/auth.service';
import { useAuthStore } from '@/stores/authStore';

type Props = NativeStackScreenProps<RootStackParamList, 'GuestName'>;

export function GuestNameScreen({}: Props) {
  const [displayName, setDisplayName] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const setSession = useAuthStore((state) => state.setSession);

  const handleContinue = async () => {
    if (!displayName.trim()) {
      setError("Введіть ім'я");
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const session = await authService.continueAsGuest(displayName.trim());
      await setSession(session);
    } catch {
      setError('Не вдалося підключитись до сервера');
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <Text variant="bodyLarge" style={styles.hint}>
        Це ім'я побачать інші гравці в кімнаті
      </Text>
      <TextInput
        mode="outlined"
        label="Ваше ім'я"
        value={displayName}
        onChangeText={setDisplayName}
        maxLength={40}
      />
      <HelperText type="error" visible={!!error}>
        {error}
      </HelperText>
      <Button mode="contained" loading={loading} disabled={loading} onPress={handleContinue}>
        Продовжити
      </Button>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 24, justifyContent: 'center' },
  hint: { marginBottom: 16, opacity: 0.7 },
});
