import { create } from 'zustand';
import { DeviceSnapshot, DeviceConnectionState } from '../lib/device-types';
import { invoke } from '@tauri-apps/api/core';
import { listen } from '@tauri-apps/api/event';

interface DeviceStatusState {
  currentDevice: DeviceSnapshot | null;
  connectionState: DeviceConnectionState;
  isScanning: boolean;
  error: string | null;

  bootstrap: () => Promise<() => void>;
  refresh: () => Promise<void>;
  clear: () => void;
}

export const useDeviceStatusStore = create<DeviceStatusState>((set) => ({
  currentDevice: null,
  connectionState: 'disconnected',
  isScanning: false,
  error: null,

  bootstrap: async () => {
    // Get initial state
    try {
      const initial: DeviceSnapshot | null = await invoke('get_current_device_snapshot');
      if (initial) {
        set({ currentDevice: initial, connectionState: initial.connectionState });
      }
    } catch (err) {
      console.error('Failed fetching initial device state:', err);
    }

    // Subscribe to event updates from Tauri backend
    const unlisten = await listen<DeviceSnapshot | null>('device://status-changed', (event) => {
      const snap = event.payload;
      set({
        currentDevice: snap,
        connectionState: snap ? snap.connectionState : 'disconnected',
        isScanning: snap?.connectionState === 'detecting',
      });
    });

    return () => {
      unlisten();
    };
  },

  refresh: async () => {
    set({ isScanning: true });
    try {
      const snap: DeviceSnapshot | null = await invoke('refresh_device_detection');
      set({
        currentDevice: snap,
        connectionState: snap ? snap.connectionState : 'disconnected',
        error: null,
      });
    } catch (err) {
      set({ error: String(err), connectionState: 'error' });
    } finally {
      set({ isScanning: false });
    }
  },

  clear: () => {
    set({ currentDevice: null, connectionState: 'disconnected', error: null });
  },
}));
