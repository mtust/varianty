import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useState } from 'react';
import { Image, StyleSheet, View } from 'react-native';
import { Button, Text } from 'react-native-paper';

import { LanguageSwitch } from '@/components/LanguageSwitch';
import { RulesDialog } from '@/components/RulesDialog';
import { useLanguageStore, useT } from '@/i18n';
import { gameRules } from '@/i18n/rules';
import { RootStackParamList } from '@/navigation/types';

type Props = NativeStackScreenProps<RootStackParamList, 'Landing'>;

export function LandingScreen({ navigation }: Props) {
  const t = useT();
  const language = useLanguageStore((state) => state.language);
  const [rulesVisible, setRulesVisible] = useState(false);

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
      <Button mode="text" icon="help-circle-outline" style={styles.button} onPress={() => setRulesVisible(true)}>
        {gameRules[language].button}
      </Button>

      <RulesDialog visible={rulesVisible} onDismiss={() => setRulesVisible(false)} />
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
