import { create } from 'zustand';
import { invoke } from '@tauri-apps/api/core';
import type { LicenseStatus } from '../lib/settings-types';

interface LicenseState {
  status: LicenseStatus | null;
  isLoading: boolean;
  error: string | null;

  loadStatus: () => Promise<void>;
  activate: (key: string) => Promise<void>;
  deactivate: () => Promise<void>;
}

export const useLicenseStore = create<LicenseState>((set) => ({
  status: null,
  isLoading: false,
  error: null,

  loadStatus: async () => {
    set({ isLoading: true, error: null });
    try {
      const status = await invoke<LicenseStatus>('get_license_status');
      set({ status, isLoading: false });
    } catch (err: any) {
      set({ error: err.toString(), isLoading: false });
    }
  },

  activate: async (key: string) => {
    set({ isLoading: true, error: null });
    try {
      const status = await invoke<LicenseStatus>('activate_license', { key });
      set({ status, isLoading: false });
    } catch (err: any) {
      set({ error: err.toString(), isLoading: false });
      throw err;
    }
  },

  deactivate: async () => {
    set({ isLoading: true, error: null });
    try {
      await invoke('deactivate_license');
      const status = await invoke<LicenseStatus>('get_license_status');
      set({ status, isLoading: false });
    } catch (err: any) {
      set({ error: err.toString(), isLoading: false });
      throw err;
    }
  },
}));
