import { StyleSheet } from 'react-native';
import { SegmentedButtons } from 'react-native-paper';

import { Language, useLanguageStore } from '@/i18n';

export function LanguageSwitch() {
  const language = useLanguageStore((state) => state.language);
  const setLanguage = useLanguageStore((state) => state.setLanguage);

  return (
    <SegmentedButtons
      density="small"
      style={styles.switch}
      value={language}
      onValueChange={(value) => setLanguage(value as Language)}
      buttons={[
        { value: 'uk', label: 'УКР' },
        { value: 'en', label: 'ENG' },
      ]}
    />
  );
}

const styles = StyleSheet.create({
  switch: { alignSelf: 'center', width: 180, marginBottom: 24 },
});
