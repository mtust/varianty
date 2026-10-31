import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { Image, StyleSheet, View } from 'react-native';
import { Button, Text } from 'react-native-paper';

import { LanguageSwitch } from '@/components/LanguageSwitch';
import { useT } from '@/i18n';
import { RootStackParamList } from '@/navigation/types';

type Props = NativeStackScreenProps<RootStackParamList, 'Landing'>;

export function LandingScreen({ navigation }: Props) {
  const t = useT();

  return (
    <View style={styles.container}>
      <LanguageSwitch />
      <Image source={require('../../assets/logo.png')} style={styles.logo} accessibilityLabel={t('app.logoLabel')} />
      <Text variant="displaySmall" style={styles.title}>
        {t('app.name')}
      </Text>
      <Text variant="bodyLarge" style={styles.subtitle}>
        {t('app.tagline')}
      </Text>

      <Button mode="contained" style={styles.button} onPress={() => navigation.navigate('GuestName')}>
        {t('landing.guest')}
      </Button>
      <Button mode="outlined" style={styles.button} onPress={() => navigation.navigate('EmailAuth')}>
        {t('landing.signIn')}
      </Button>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, justifyContent: 'center', padding: 24 },
  logo: { width: 120, height: 120, alignSelf: 'center', marginBottom: 20 },
  title: { textAlign: 'center', fontWeight: '700' },
  subtitle: { textAlign: 'center', marginTop: 8, marginBottom: 48, opacity: 0.7 },
  button: { marginTop: 12 },
});
