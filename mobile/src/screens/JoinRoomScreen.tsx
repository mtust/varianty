import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { Button, HelperText, Text, TextInput } from 'react-native-paper';

import { RootStackParamList } from '@/navigation/types';
import { roomService } from '@/services/room.service';

type Props = NativeStackScreenProps<RootStackParamList, 'JoinRoom'>;

export function JoinRoomScreen({ navigation }: Props) {
  const [code, setCode] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleJoin = async () => {
    const normalized = code.trim().toUpperCase();
    if (!normalized) {
      setError('Введіть код кімнати');
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const exists = await roomService.roomExists(normalized);
      if (!exists) {
        setError('Кімнату не знайдено');
        return;
      }
      navigation.navigate('Lobby', { code: normalized, isHost: false });
    } catch {
      setError('Не вдалося перевірити код');
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <Text variant="bodyLarge" style={styles.hint}>
        Введіть код, який показав ведучий
      </Text>
      <TextInput
        mode="outlined"
        label="Код кімнати"
        value={code}
        onChangeText={setCode}
        autoCapitalize="characters"
        maxLength={6}
      />
      <HelperText type="error" visible={!!error}>
        {error}
      </HelperText>
      <Button mode="contained" loading={loading} disabled={loading} onPress={handleJoin}>
        Приєднатися
      </Button>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 24, justifyContent: 'center' },
  hint: { marginBottom: 16, opacity: 0.7 },
});
