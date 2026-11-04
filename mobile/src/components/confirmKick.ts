import { Alert } from 'react-native';

import { translate } from '@/i18n';
import { PlayerScore } from '@/types/game';

/** Asks the host to confirm removing a player, then calls onConfirm. */
export function confirmKick(player: PlayerScore, onConfirm: () => void) {
  Alert.alert(translate('room.kickTitle'), translate('room.kickMessage', { name: player.displayName }), [
    { text: translate('room.kickCancel'), style: 'cancel' },
    { text: translate('room.kickConfirm'), style: 'destructive', onPress: onConfirm },
  ]);
}

/** Tells a removed player what happened; onDismiss runs once they close the alert. */
export function showKickedAlert(onDismiss: () => void) {
  Alert.alert(translate('room.kicked'), undefined, [{ text: 'OK', onPress: onDismiss }], {
    cancelable: false,
  });
}
