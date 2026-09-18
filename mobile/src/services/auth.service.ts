import { apiClient } from '@/services/api.service';

export interface AuthResponse {
  token: string;
  playerId: string;
  displayName: string;
  guest: boolean;
}

export const authService = {
  async registerWithEmail(email: string, password: string, displayName: string): Promise<AuthResponse> {
    const { data } = await apiClient.post<AuthResponse>('/api/auth/register', { email, password, displayName });
    return data;
  },

  async loginWithEmail(email: string, password: string): Promise<AuthResponse> {
    const { data } = await apiClient.post<AuthResponse>('/api/auth/login', { email, password });
    return data;
  },

  async continueAsGuest(displayName: string): Promise<AuthResponse> {
    const { data } = await apiClient.post<AuthResponse>('/api/auth/guest', { displayName });
    return data;
  },
};
