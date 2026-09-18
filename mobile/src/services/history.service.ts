import { apiClient } from '@/services/api.service';
import { GameHistoryEntry } from '@/types/game';

export const historyService = {
  async myHistory(): Promise<GameHistoryEntry[]> {
    const { data } = await apiClient.get<GameHistoryEntry[]>('/api/history');
    return data;
  },
};
