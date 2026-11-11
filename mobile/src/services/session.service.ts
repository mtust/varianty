import axios from 'axios';
import { AppState, AppStateStatus } from 'react-native';

import { apiClient } from '@/services/api.service';
import type { AuthResponse } from '@/services/auth.service';
import { isPastHalfLife, isTokenExpired } from '@/services/jwt';
import { useAuthStore } from '@/stores/authStore';

let inFlight: Promise<void> | null = null;

/**
 * Sliding session for registered players: once less than half of the token's lifetime is left,
 * swap it for a fresh one. Guests keep their hard 24h lifetime and are never refreshed.
 * - 401 (expired token / deleted account): apiClient's interceptor signs out -> start screen.
 * - Network or other errors: keep the current token and try again next time.
 */
export function refreshSessionIfNeeded(): Promise<void> {
  if (inFlight) return inFlight;
  const { token, guest } = useAuthStore.getState();
  if (!token || guest || isTokenExpired(token) || !isPastHalfLife(token)) {
    // An expired token is handled by the request interceptor / hydrate (sign out).
    return Promise.resolve();
  }
  inFlight = (async () => {
    try {
      const { data } = await apiClient.post<AuthResponse>('/api/auth/refresh');
      // Ignore the result if the user signed out or switched accounts meanwhile.
      if (useAuthStore.getState().token === token) {
        await useAuthStore.getState().setSession(data);
      }
    } catch (error) {
      if (!axios.isAxiosError(error) || error.response?.status !== 401) {
        console.warn('[auth] token refresh failed, keeping current token', error);
      }
    } finally {
      inFlight = null;
    }
  })();
  return inFlight;
}

/** Refreshes now (call after hydrate) and whenever the app returns to the foreground. */
export function startSessionRefresh(): () => void {
  refreshSessionIfNeeded();
  const sub = AppState.addEventListener('change', (state: AppStateStatus) => {
    if (state === 'active') {
      refreshSessionIfNeeded();
    }
  });
  return () => sub.remove();
}
