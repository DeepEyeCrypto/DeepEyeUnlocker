import { create } from 'zustand';
// No local storage settings import needed anymore

// Matches Rust ConnectedDevice struct
export interface ConnectedDevice {
  id: string; // UDID / Serial
  model: string;
  serial: string;
  os: string;
  mode: string;
  bootloaderStatus: string;
  carrier?: string;
  source: string;
}

export interface LogEntry {
  id: string;
  timestamp: number;
  level: 'info' | 'warn' | 'error' | 'success';
  message: string;
}

interface AppState {
  // Device State
  device: ConnectedDevice | null;
  setDevice: (device: ConnectedDevice | null) => void;

  // Active Operation State
  activeOperation: string | null;
  operationProgress: number; // 0-100
  startOperation: (name: string) => void;
  updateProgress: (progress: number) => void;
  endOperation: () => void;

  // Logs
  logs: LogEntry[];
  appendLog: (level: 'info' | 'warn' | 'error' | 'success', message: string) => void;
  clearLogs: () => void;
}

export const useAppStore = create<AppState>((set) => ({
  // Device
  device: null,
  setDevice: (device) => set({ device }),

  // Operation
  activeOperation: null,
  operationProgress: 0,
  startOperation: (name) => set({ activeOperation: name, operationProgress: 0 }),
  updateProgress: (progress) => set({ operationProgress: progress }),
  endOperation: () => set({ activeOperation: null, operationProgress: 100 }),

  // Logs
  logs: [],
  appendLog: (level, message) =>
    set((state) => {
      const newLog: LogEntry = {
        id: Math.random().toString(36).substring(2, 9),
        timestamp: Date.now(),
        level,
        message,
      };
      const newLogs = [...state.logs, newLog];
      if (newLogs.length > 1000) newLogs.shift();
      return { logs: newLogs };
    }),
  clearLogs: () => set({ logs: [] }),
}));
