import AsyncStorage from '@react-native-async-storage/async-storage';
import { getLocales } from 'expo-localization';
import { useCallback } from 'react';
import { create } from 'zustand';

import { en, serverErrors, TranslationKey, uk } from '@/i18n/translations';

export type Language = 'uk' | 'en';

type TranslationParams = Record<string, string | number>;

const STORAGE_KEY = 'varianty.language';
const dictionaries: Record<Language, Record<TranslationKey, string>> = { uk, en };

function deviceLanguage(): Language {
  const code = getLocales()[0]?.languageCode;
  return code === 'uk' || code === 'ru' ? 'uk' : 'en';
}

interface LanguageState {
  language: Language;
  hydrate: () => Promise<void>;
  setLanguage: (language: Language) => Promise<void>;
}

export const useLanguageStore = create<LanguageState>((set) => ({
  language: deviceLanguage(),

  hydrate: async () => {
    try {
      const stored = await AsyncStorage.getItem(STORAGE_KEY);
      if (stored === 'uk' || stored === 'en') {
        set({ language: stored });
      }
    } catch {
      // Keep the device language if storage can't be read.
    }
  },

  setLanguage: async (language) => {
    set({ language });
    await AsyncStorage.setItem(STORAGE_KEY, language);
  },
}));

function format(language: Language, key: TranslationKey, params?: TranslationParams) {
  const text = dictionaries[language][key];
  if (!params) return text;
  return text.replace(/\{(\w+)\}/g, (match, name: string) => String(params[name] ?? match));
}

/** For code outside React, such as services and stores. */
export function translate(key: TranslationKey, params?: TranslationParams) {
  return format(useLanguageStore.getState().language, key, params);
}

export function useT() {
  const language = useLanguageStore((state) => state.language);
  return useCallback(
    (key: TranslationKey, params?: TranslationParams) => format(language, key, params),
    [language]
  );
}

/** Shows a server message in the player's language; unknown messages pass through unchanged. */
export function localizeServerError(message: string) {
  const key = serverErrors[message];
  return key ? translate(key) : message;
}
