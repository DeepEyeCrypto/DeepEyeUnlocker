import { useMemo } from 'react';
import { useOperationSessionStore } from '../stores/useOperationSessionStore';
import {
  applyFilters,
  DEFAULT_FILTERS,
  HistoryFilters,
  SessionHistoryRow,
} from '../lib/session-history-utils';

export function useSessionHistory(filters: HistoryFilters = DEFAULT_FILTERS): SessionHistoryRow[] {
  const sessionHistory = useOperationSessionStore((state) => state.sessionHistory);

  return useMemo(() => {
    return applyFilters(sessionHistory, filters);
  }, [sessionHistory, filters]);
}
