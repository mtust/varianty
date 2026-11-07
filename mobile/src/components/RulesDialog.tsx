import { ScrollView, StyleSheet, View } from 'react-native';
import { Button, Dialog, Portal, Text } from 'react-native-paper';

import { useLanguageStore } from '@/i18n';
import { gameRules } from '@/i18n/rules';
import { theme } from '@/theme/theme';

interface Props {
  visible: boolean;
  onDismiss: () => void;
}

export function RulesDialog({ visible, onDismiss }: Props) {
  const language = useLanguageStore((state) => state.language);
  const rules = gameRules[language];

  return (
    <Portal>
      <Dialog visible={visible} onDismiss={onDismiss} style={styles.dialog}>
        <Dialog.Title>{rules.title}</Dialog.Title>
        <Dialog.ScrollArea style={styles.scrollArea}>
          <ScrollView contentContainerStyle={styles.content}>
            <Text variant="bodyMedium">{rules.goal}</Text>

            <Text variant="titleSmall" style={styles.section}>
              {rules.stepsTitle}
            </Text>
            {rules.steps.map((step, index) => (
              <View key={index} style={styles.row}>
                <Text variant="bodyMedium" style={styles.marker}>
                  {index + 1}.
                </Text>
                <Text variant="bodyMedium" style={styles.rowText}>
                  {step}
                </Text>
              </View>
            ))}

            <Text variant="titleSmall" style={styles.section}>
              {rules.scoringTitle}
            </Text>
            {rules.scoring.map((item) => (
              <View key={item.points} style={styles.row}>
                <Text variant="bodyMedium" style={[styles.marker, styles.points]}>
                  {item.points}
                </Text>
                <Text variant="bodyMedium" style={styles.rowText}>
                  {item.text}
                </Text>
              </View>
            ))}

            <Text variant="titleSmall" style={styles.section}>
              {rules.modesTitle}
            </Text>
            {rules.modes.map((mode, index) => (
              <View key={index} style={styles.row}>
                <Text variant="bodyMedium" style={styles.marker}>
                  •
                </Text>
                <Text variant="bodyMedium" style={styles.rowText}>
                  {mode}
                </Text>
              </View>
            ))}
          </ScrollView>
        </Dialog.ScrollArea>
        <Dialog.Actions>
          <Button onPress={onDismiss}>{rules.close}</Button>
        </Dialog.Actions>
      </Dialog>
    </Portal>
  );
}

const styles = StyleSheet.create({
  dialog: { maxHeight: '85%' },
  scrollArea: { paddingHorizontal: 0 },
  content: { paddingHorizontal: 24, paddingVertical: 12 },
  section: { marginTop: 16, marginBottom: 4, fontWeight: '700' },
  row: { flexDirection: 'row', marginTop: 6 },
  marker: { width: 28 },
  points: { fontWeight: '700', color: theme.colors.primary },
  rowText: { flex: 1 },
});
