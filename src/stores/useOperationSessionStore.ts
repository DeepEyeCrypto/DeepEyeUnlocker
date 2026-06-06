import { create } from 'zustand';
import {
  OperationSession,
  OperationType,
  ProgressStep,
  SessionLog,
  PreflightResult,
} from '../lib/session-types';
import { invoke } from '@tauri-apps/api/core';
import { listen, UnlistenFn } from '@tauri-apps/api/event';

interface OperationSessionState {
  activeSession: OperationSession | null;
  sessionHistory: OperationSession[];
  isStarting: boolean;
  error: string | null;

  bootstrap: () => Promise<() => void>;
  startSession: (type: OperationType) => Promise<void>;
  cancelSession: () => Promise<void>;
  clearSession: () => Promise<void>;
  fetchHistory: () => Promise<void>;

  // Reducers for partial updates
  applyStepUpdate: (step: ProgressStep) => void;
  applyLog: (log: SessionLog) => void;
  applyPreflight: (preflight: PreflightResult) => void;
}

export const useOperationSessionStore = create<OperationSessionState>((set) => ({
  activeSession: null,
  sessionHistory: [],
  isStarting: false,
  error: null,

  bootstrap: async () => {
    try {
      const initial: OperationSession | null = await invoke('get_active_session');
      set({ activeSession: initial });
    } catch (err) {
      console.error('Failed fetching active session state:', err);
    }

    const unlistens: UnlistenFn[] = [];

    // Full status replacement
    unlistens.push(
      await listen<OperationSession | null>('session://status-changed', (event) => {
        set({ activeSession: event.payload });
      }),
    );

    // Preflight Result
    unlistens.push(
      await listen<PreflightResult>('session://preflight', (event) => {
        set((state) => {
          if (!state.activeSession) return state;
          return {
            activeSession: { ...state.activeSession, preflight: event.payload },
          };
        });
      }),
    );

    // Step partial update
    unlistens.push(
      await listen<ProgressStep>('session://step-update', (event) => {
        set((state) => {
          if (!state.activeSession) return state;
          const steps = state.activeSession.steps.map((s) =>
            s.id === event.payload.id ? event.payload : s,
          );
          return {
            activeSession: { ...state.activeSession, steps },
          };
        });
      }),
    );

    // Log append
    unlistens.push(
      await listen<SessionLog>('session://log', (event) => {
        set((state) => {
          if (!state.activeSession) return state;
          return {
            activeSession: {
              ...state.activeSession,
              logs: [...state.activeSession.logs, event.payload],
            },
          };
        });
      }),
    );

    return () => {
      for (const unlisten of unlistens) {
        unlisten();
      }
    };
  },

  startSession: async (type: OperationType) => {
    set({ isStarting: true, error: null });
    try {
      const session: OperationSession = await invoke('start_session', { operationType: type });
      set({ activeSession: session });
    } catch (err) {
      set({ error: String(err) });
      throw err;
    } finally {
      set({ isStarting: false });
    }
  },

  cancelSession: async () => {
    try {
      await invoke('cancel_session');
    } catch (err) {
      set({ error: String(err) });
      throw err;
    }
  },

  clearSession: async () => {
    try {
      await invoke('clear_active_session');
      set({ activeSession: null, error: null });
    } catch (err) {
      set({ error: String(err) });
      throw err;
    }
  },

  fetchHistory: async () => {
    try {
      const history: OperationSession[] = await invoke('get_session_history', { limit: 50 });
      set({ sessionHistory: history });
    } catch (err) {
      console.error('Failed to fetch session history:', err);
    }
  },

  applyStepUpdate: (step: ProgressStep) => {
    set((state) => {
      if (!state.activeSession) return state;
      const steps = state.activeSession.steps.map((s) => (s.id === step.id ? step : s));
      return { activeSession: { ...state.activeSession, steps } };
    });
  },

  applyLog: (log: SessionLog) => {
    set((state) => {
      if (!state.activeSession) return state;
      return {
        activeSession: { ...state.activeSession, logs: [...state.activeSession.logs, log] },
      };
    });
  },

  applyPreflight: (preflight: PreflightResult) => {
    set((state) => {
      if (!state.activeSession) return state;
      return { activeSession: { ...state.activeSession, preflight } };
    });
  },
}));
