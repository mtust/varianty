import { Language } from '@/i18n';
import { apiClient } from '@/services/api.service';

export type QuestionCategory = 'facts' | 'quotes';

export const MIN_ROUNDS = 5;
export const MAX_ROUNDS = 20;
export const DEFAULT_ROUNDS = 6;
/** Seconds players get to write their answer; the server accepts 15-180. */
export const ANSWER_SECONDS_OPTIONS = [30, 45, 60, 90] as const;
export const DEFAULT_ANSWER_SECONDS = 45;

export const roomService = {
  async createRoom(
    hostless = false,
    category: QuestionCategory = 'facts',
    language: Language = 'uk',
    rounds: number = DEFAULT_ROUNDS
  ): Promise<{ code: string }> {
    const { data } = await apiClient.post<{ code: string }>('/api/rooms', null, {
      params: { hostless, category, language, rounds },
    });
    return data;
  },

  async roomExists(code: string): Promise<boolean> {
    const { data } = await apiClient.get<{ exists: boolean }>(`/api/rooms/${code}/exists`);
    return data.exists;
  },
};
