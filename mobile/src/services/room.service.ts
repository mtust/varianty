import { Language } from '@/i18n';
import { apiClient } from '@/services/api.service';

export type QuestionCategory = 'facts' | 'quotes';

export const roomService = {
  async createRoom(
    hostless = false,
    category: QuestionCategory = 'facts',
    language: Language = 'uk'
  ): Promise<{ code: string }> {
    const { data } = await apiClient.post<{ code: string }>('/api/rooms', null, {
      params: { hostless, category, language },
    });
    return data;
  },

  async roomExists(code: string): Promise<boolean> {
    const { data } = await apiClient.get<{ exists: boolean }>(`/api/rooms/${code}/exists`);
    return data.exists;
  },
};
