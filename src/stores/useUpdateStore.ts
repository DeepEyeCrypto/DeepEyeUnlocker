import { create } from 'zustand';
import { invoke } from '@tauri-apps/api/core';
import type { UpdateInfo } from '../lib/settings-types';

interface UpdateState {
  info: UpdateInfo | null;
  isChecking: boolean;
  isInstalling: boolean;
  error: string | null;

  checkForUpdates: () => Promise<void>;
  installUpdate: () => Promise<void>;
}

export const useUpdateStore = create<UpdateState>((set) => ({
  info: null,
  isChecking: false,
  isInstalling: false,
  error: null,

  checkForUpdates: async () => {
    set({ isChecking: true, error: null });
    try {
      const info = await invoke<UpdateInfo>('check_for_updates');
      set({ info, isChecking: false });
    } catch (err: any) {
      set({ error: err.toString(), isChecking: false });
    }
  },

  installUpdate: async () => {
    set({ isInstalling: true, error: null });
    try {
      await invoke('do_install_update');
      // usually restarts, but if not:
      set({ isInstalling: false });
    } catch (err: any) {
      set({ error: err.toString(), isInstalling: false });
      throw err;
    }
  },
}));
