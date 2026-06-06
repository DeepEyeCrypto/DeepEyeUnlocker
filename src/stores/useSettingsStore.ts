import { create } from 'zustand';
import { invoke } from '@tauri-apps/api/core';
import { AppSettings, DEFAULT_SETTINGS } from '../lib/settings-types';

interface SettingsState {
  settings: AppSettings;
  isLoading: boolean;
  error: string | null;

  loadSettings: () => Promise<void>;
  updateSettings: (next: Partial<AppSettings>) => Promise<void>;
  resetToDefaults: () => Promise<void>;
}

export const useSettingsStore = create<SettingsState>((set, get) => ({
  settings: DEFAULT_SETTINGS,
  isLoading: false,
  error: null,

  loadSettings: async () => {
    set({ isLoading: true, error: null });
    try {
      const settings = await invoke<AppSettings>('get_settings');
      set({ settings, isLoading: false });
    } catch (err: any) {
      set({ error: err.toString(), isLoading: false });
    }
  },

  updateSettings: async (next: Partial<AppSettings>) => {
    const current = get().settings;
    const updated = { ...current, ...next };

    // Optimistic UI update
    set({ settings: updated, error: null });

    try {
      await invoke('save_settings', { settings: updated });
    } catch (err: any) {
      // Revert if failed
      set({ settings: current, error: err.toString() });
      throw err;
    }
  },

  resetToDefaults: async () => {
    set({ isLoading: true, error: null });
    try {
      const defaultSettings = await invoke<AppSettings>('reset_settings');
      set({ settings: defaultSettings, isLoading: false });
    } catch (err: any) {
      set({ error: err.toString(), isLoading: false });
      throw err;
    }
  },
}));
