import axios from 'axios';

import { useAuthStore } from '@/stores/authStore';

export const API_BASE_URL = process.env.EXPO_PUBLIC_API_BASE_URL ?? 'http://localhost:8080';
export const WS_URL = process.env.EXPO_PUBLIC_WS_URL ?? 'ws://localhost:8080/ws';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
});

apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});
