import AsyncStorage from '@react-native-async-storage/async-storage';
import { create } from 'zustand';

const STORAGE_KEY = 'varianty.auth';

interface AuthState {
  token: string | null;
  playerId: string | null;
  displayName: string | null;
  guest: boolean;
  hydrated: boolean;
  hydrate: () => Promise<void>;
  setSession: (session: { token: string; playerId: string; displayName: string; guest: boolean }) => Promise<void>;
  signOut: () => Promise<void>;
}

export const useAuthStore = create<AuthState>((set) => ({
  token: null,
  playerId: null,
  displayName: null,
  guest: false,
  hydrated: false,

  hydrate: async () => {
    try {
      const raw = await AsyncStorage.getItem(STORAGE_KEY);
      if (raw) {
        const parsed = JSON.parse(raw);
        set({ ...parsed, hydrated: true });
      } else {
        set({ hydrated: true });
      }
    } catch {
      set({ hydrated: true });
    }
  },

  setSession: async (session) => {
    await AsyncStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    set({ ...session, hydrated: true });
  },

  signOut: async () => {
    await AsyncStorage.removeItem(STORAGE_KEY);
    set({ token: null, playerId: null, displayName: null, guest: false });
  },
}));
