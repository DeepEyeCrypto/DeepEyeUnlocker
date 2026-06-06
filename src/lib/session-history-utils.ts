import { OperationSession, OperationType, SessionStatus } from './session-types';

export interface SessionHistoryRow {
  sessionId: string;
  operationType: OperationType;
  deviceModel: string;
  deviceSerial: string;
  platform: string;
  status: SessionStatus;
  outcome?: 'success' | 'partial' | 'failed' | 'cancelled' | null;
  startedAt: string;
  completedAt?: string | null;
  durationMs?: number;
  stepCount: number;
  logCount: number;
  hasErrors: boolean;
  errorCode?: string | null;
}

export interface HistoryFilters {
  search: string; // keyword match on logs/model
  operationType: OperationType | 'all';
  status: SessionStatus | 'all';
  platform: string | 'all';
  dateFrom: string | null;
  dateTo: string | null;
}

export const DEFAULT_FILTERS: HistoryFilters = {
  search: '',
  operationType: 'all',
  status: 'all',
  platform: 'all',
  dateFrom: null,
  dateTo: null,
};

export function toHistoryRow(s: OperationSession): SessionHistoryRow {
  const snap = s.deviceSnapshotAtStart;
  const start = new Date(s.startedAt).getTime();
  const end = s.completedAt ? new Date(s.completedAt).getTime() : null;
  return {
    sessionId: s.sessionId,
    operationType: s.operationType,
    deviceModel: snap?.model ?? 'Unknown',
    deviceSerial: snap?.serial ?? '—',
    platform: snap?.platform ?? 'unknown',
    status: s.status,
    outcome: s.outcome,
    startedAt: s.startedAt,
    completedAt: s.completedAt,
    durationMs: end ? end - start : undefined,
    stepCount: s.steps.length,
    logCount: s.logs.length,
    hasErrors: s.logs.some((l) => l.level === 'error'),
    errorCode: s.errorCode,
  };
}

export function applyFilters(
  sessions: OperationSession[],
  filters: HistoryFilters,
): SessionHistoryRow[] {
  return sessions
    .filter((s) => {
      // Direct exact matches first
      if (filters.operationType !== 'all') {
        const opStr1 =
          typeof filters.operationType === 'string'
            ? filters.operationType
            : (filters.operationType as any).customCommand;
        const opStr2 =
          typeof s.operationType === 'string'
            ? s.operationType
            : (s.operationType as any).customCommand;
        if (opStr1 !== opStr2) return false;
      }

      if (filters.status !== 'all' && s.status !== filters.status) return false;
      if (filters.platform !== 'all' && s.deviceSnapshotAtStart?.platform !== filters.platform)
        return false;

      // Date bounds
      if (
        filters.dateFrom &&
        new Date(s.startedAt).getTime() < new Date(filters.dateFrom).getTime()
      )
        return false;
      if (
        filters.dateTo &&
        new Date(s.startedAt).getTime() > new Date(filters.dateTo).getTime() + 86400000
      )
        return false; // add 1 day so 'To' is inclusive

      // Full text search
      if (filters.search) {
        const q = filters.search.toLowerCase();
        const model = s.deviceSnapshotAtStart?.model?.toLowerCase() ?? '';
        const logs = s.logs
          .map((l) => l.message)
          .join(' ')
          .toLowerCase();
        const typeStr =
          typeof s.operationType === 'string'
            ? s.operationType
            : (s.operationType as any).customCommand;
        const type = typeStr.toLowerCase();

        if (!model.includes(q) && !logs.includes(q) && !type.includes(q)) {
          return false;
        }
      }

      return true;
    })
    .map(toHistoryRow)
    .sort((a, b) => new Date(b.startedAt).getTime() - new Date(a.startedAt).getTime());
}
