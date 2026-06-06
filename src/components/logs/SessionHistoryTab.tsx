import { useState, useEffect } from 'react';
import { HistoryFilterBar } from './HistoryFilterBar';
import { HistoryTable } from './HistoryTable';
import { SessionDetailPanel } from './SessionDetailPanel';
import { useSessionHistory } from '../../hooks/useSessionHistory';
import { DEFAULT_FILTERS, HistoryFilters } from '../../lib/session-history-utils';
import { useOperationSessionStore } from '../../stores/useOperationSessionStore';
import { OperationSession } from '../../lib/session-types';

export function SessionHistoryTab() {
  const [filters, setFilters] = useState<HistoryFilters>(DEFAULT_FILTERS);
  const [selectedSessionId, setSelectedSessionId] = useState<string | null>(null);

  // Custom debounce logic for the search filter
  const [debouncedFilters, setDebouncedFilters] = useState<HistoryFilters>(filters);
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedFilters(filters);
    }, 300);
    return () => clearTimeout(timer);
  }, [filters]);

  const rows = useSessionHistory(debouncedFilters);
  const sessionHistory = useOperationSessionStore((state) => state.sessionHistory);
  const fetchHistory = useOperationSessionStore((state) => state.fetchHistory);

  useEffect(() => {
    fetchHistory();
  }, [fetchHistory]);

  const selectedSession: OperationSession | null = selectedSessionId
    ? sessionHistory.find((s) => s.sessionId === selectedSessionId) || null
    : null;

  return (
    <div className="flex flex-col h-full bg-[#0a0a0a] rounded-lg border border-white/10 overflow-hidden relative">
      <HistoryFilterBar filters={filters} onChange={setFilters} />

      <div className="flex-1 overflow-y-auto relative">
        <HistoryTable rows={rows} onRowClick={setSelectedSessionId} />
      </div>

      {selectedSessionId && (
        <SessionDetailPanel session={selectedSession} onClose={() => setSelectedSessionId(null)} />
      )}
    </div>
  );
}
