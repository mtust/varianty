import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { Button, HelperText, SegmentedButtons, Text, TextInput } from 'react-native-paper';

import { RootStackParamList } from '@/navigation/types';
import { authService } from '@/services/auth.service';
import { useAuthStore } from '@/stores/authStore';

type Props = NativeStackScreenProps<RootStackParamList, 'EmailAuth'>;

export function EmailAuthScreen({}: Props) {
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const setSession = useAuthStore((state) => state.setSession);

  const handleSubmit = async () => {
    setError(null);
    if (!email.trim() || !password.trim() || (mode === 'register' && !displayName.trim())) {
      setError("Заповніть усі поля");
      return;
    }
    setLoading(true);
    try {
      const session =
        mode === 'login'
          ? await authService.loginWithEmail(email.trim(), password)
          : await authService.registerWithEmail(email.trim(), password, displayName.trim());
      await setSession(session);
    } catch (e: any) {
      setError(e?.response?.data?.message ?? 'Не вдалося увійти');
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <SegmentedButtons
        value={mode}
        onValueChange={(value) => setMode(value as 'login' | 'register')}
        style={styles.segmented}
        buttons={[
          { value: 'login', label: 'Вхід' },
          { value: 'register', label: 'Реєстрація' },
        ]}
      />
      {mode === 'register' && (
        <TextInput mode="outlined" label="Ім'я" value={displayName} onChangeText={setDisplayName} style={styles.field} />
      )}
      <TextInput
        mode="outlined"
        label="Email"
        value={email}
        onChangeText={setEmail}
        autoCapitalize="none"
        keyboardType="email-address"
        style={styles.field}
      />
      <TextInput
        mode="outlined"
        label="Пароль"
        value={password}
        onChangeText={setPassword}
        secureTextEntry
        style={styles.field}
      />
      <HelperText type="error" visible={!!error}>
        {error}
      </HelperText>
      <Button mode="contained" loading={loading} disabled={loading} onPress={handleSubmit}>
        {mode === 'login' ? 'Увійти' : 'Зареєструватися'}
      </Button>
      <Text style={styles.note}>Акаунт зберігає історію та результати ваших ігор</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 24, justifyContent: 'center' },
  segmented: { marginBottom: 24 },
  field: { marginBottom: 12 },
  note: { marginTop: 24, textAlign: 'center', opacity: 0.6 },
});
