import axios, { AxiosError } from 'axios';

import { isTokenExpired } from '@/services/jwt';
import { useAuthStore } from '@/stores/authStore';

export const API_BASE_URL = process.env.EXPO_PUBLIC_API_BASE_URL ?? 'http://localhost:8080';
export const WS_URL = process.env.EXPO_PUBLIC_WS_URL ?? 'ws://localhost:8080/ws';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
});

/** Auth endpoints answer 401 for a wrong password; that must not end the session. */
const SESSION_LESS_PATHS = ['/api/auth/login', '/api/auth/register', '/api/auth/guest'];

function isSessionLessRequest(url: string | undefined): boolean {
  return !!url && SESSION_LESS_PATHS.some((path) => url.includes(path));
}

apiClient.interceptors.request.use(async (config) => {
  const token = useAuthStore.getState().token;
  if (token && !isSessionLessRequest(config.url)) {
    if (isTokenExpired(token)) {
      // Signing out swaps the navigator to the start (Landing) screen.
      await useAuthStore.getState().signOut();
      throw new axios.CanceledError('Session expired');
    }
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(undefined, async (error: AxiosError) => {
  // 401 = missing/invalid/expired token (the server could also have revoked it).
  if (
    error.response?.status === 401 &&
    !isSessionLessRequest(error.config?.url) &&
    useAuthStore.getState().token
  ) {
    await useAuthStore.getState().signOut();
  }
  return Promise.reject(error);
});
