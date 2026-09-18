import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';

import { useAuthStore } from '@/stores/authStore';
import { RootStackParamList } from '@/navigation/types';
import { EmailAuthScreen } from '@/screens/EmailAuthScreen';
import { GameScreen } from '@/screens/GameScreen';
import { GuestNameScreen } from '@/screens/GuestNameScreen';
import { HistoryScreen } from '@/screens/HistoryScreen';
import { HomeScreen } from '@/screens/HomeScreen';
import { JoinRoomScreen } from '@/screens/JoinRoomScreen';
import { LandingScreen } from '@/screens/LandingScreen';
import { LobbyScreen } from '@/screens/LobbyScreen';

const Stack = createNativeStackNavigator<RootStackParamList>();

export function AppNavigator() {
  const isAuthenticated = useAuthStore((state) => !!state.token);

  return (
    <NavigationContainer>
      <Stack.Navigator screenOptions={{ headerTitleAlign: 'center' }}>
        {isAuthenticated ? (
          <>
            <Stack.Screen name="Home" component={HomeScreen} options={{ title: 'Варіанти' }} />
            <Stack.Screen name="JoinRoom" component={JoinRoomScreen} options={{ title: 'Приєднатися' }} />
            <Stack.Screen name="Lobby" component={LobbyScreen} options={{ title: 'Лобі' }} />
            <Stack.Screen name="Game" component={GameScreen} options={{ headerShown: false }} />
            <Stack.Screen name="History" component={HistoryScreen} options={{ title: 'Історія ігор' }} />
          </>
        ) : (
          <>
            <Stack.Screen name="Landing" component={LandingScreen} options={{ headerShown: false }} />
            <Stack.Screen name="GuestName" component={GuestNameScreen} options={{ title: 'Гра як гість' }} />
            <Stack.Screen name="EmailAuth" component={EmailAuthScreen} options={{ title: 'Акаунт' }} />
          </>
        )}
      </Stack.Navigator>
    </NavigationContainer>
  );
}
