import { useOperationSessionStore } from '../stores/useOperationSessionStore';
import { OperationSession, SessionStatus } from '../lib/session-types';

export function useActiveSession(): OperationSession | null {
  return useOperationSessionStore((state) => state.activeSession);
}

export function useSessionStatus(): SessionStatus {
  return useOperationSessionStore((state) => state.activeSession?.status || 'idle');
}

export function useIsSessionActive(): boolean {
  return useOperationSessionStore((state) => {
    const status = state.activeSession?.status;
    return (
      status === 'starting' ||
      status === 'running' ||
      status === 'cancelling' ||
      status === 'completing'
    );
  });
}

export function useSessionHistory(): OperationSession[] {
  return useOperationSessionStore((state) => state.sessionHistory);
}
