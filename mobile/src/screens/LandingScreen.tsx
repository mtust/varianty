import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { StyleSheet, View } from 'react-native';
import { Button, Text } from 'react-native-paper';

import { RootStackParamList } from '@/navigation/types';

type Props = NativeStackScreenProps<RootStackParamList, 'Landing'>;

export function LandingScreen({ navigation }: Props) {
  return (
    <View style={styles.container}>
      <Text variant="displaySmall" style={styles.title}>
        Варіанти
      </Text>
      <Text variant="bodyLarge" style={styles.subtitle}>
        Гра, де найкраща брехня перемагає
      </Text>

      <Button mode="contained" style={styles.button} onPress={() => navigation.navigate('GuestName')}>
        Грати як гість
      </Button>
      <Button mode="outlined" style={styles.button} onPress={() => navigation.navigate('EmailAuth')}>
        Увійти / Зареєструватися
      </Button>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, justifyContent: 'center', padding: 24 },
  title: { textAlign: 'center', fontWeight: '700' },
  subtitle: { textAlign: 'center', marginTop: 8, marginBottom: 48, opacity: 0.7 },
  button: { marginTop: 12 },
});
