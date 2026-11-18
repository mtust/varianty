import { Checkbox, SegmentedButtons } from 'react-native-paper';
import { useT } from '@/i18n';
import { ANSWER_SECONDS_OPTIONS } from '@/services/room.service';

/** Lets the host pick how many seconds players get to write their answer each round. */
export function AnswerTimePicker({
  value,
  onChange,
  style,
}: {
  value: number;
  onChange: (seconds: number) => void;
  style?: object;
}) {
  const t = useT();
  return (
    <SegmentedButtons
      style={style}
      density="small"
      value={String(value)}
      onValueChange={(v) => onChange(Number(v))}
      buttons={ANSWER_SECONDS_OPTIONS.map((seconds) => ({
        value: String(seconds),
        label: t('game.seconds', { seconds }),
      }))}
    />
  );
}

/** Rooms with a host: turn the countdown off so the host starts and ends voting by hand. */
export function ManualTimerCheckbox({
  checked,
  onChange,
  label,
}: {
  checked: boolean;
  onChange: (checked: boolean) => void;
  label: string;
}) {
  return (
    <Checkbox.Item
      mode="android"
      position="leading"
      label={label}
      labelVariant="bodyMedium"
      labelStyle={{ textAlign: 'left' }}
      status={checked ? 'checked' : 'unchecked'}
      onPress={() => onChange(!checked)}
    />
  );
}
