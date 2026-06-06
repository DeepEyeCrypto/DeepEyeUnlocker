import { applyFilters, DEFAULT_FILTERS, toHistoryRow } from '../session-history-utils';
import { OperationSession } from '../session-types';

const mockBaseSession: OperationSession = {
  sessionId: '123',
  operationType: 'HelloActivation',
  deviceSnapshotAtStart: {
    id: '12345',
    connectionState: 'connected',
    model: 'iPhone 14 Pro',
    serial: 'A1B2C3D4',
    platform: 'ios',
    osVersion: '16.6',
    isSupported: true,
    mode: 'normal',
    riskFlags: [],
    capabilityFlags: [],
    detectedAt: 0,
    updatedAt: 0,
  },
  status: 'completed',
  steps: [
    {
      id: '1',
      index: 0,
      label: 'test',
      detail: null,
      status: 'done',
      emittedAt: '2026-06-06T20:00:00Z',
    },
  ],
  logs: [
    { sessionId: '123', level: 'info', message: 'test log', timestamp: '2026-06-06T20:00:00Z' },
  ],
  startedAt: '2026-06-06T20:00:00Z',
  updatedAt: '2026-06-06T20:00:00Z',
  completedAt: '2026-06-06T20:00:10Z',
  outcome: 'success',
  retryCount: 0,
  canRetry: false,
  canCancel: false,
  currentStepIndex: 0,
};

describe('History Filters', () => {
  test('Test 13: toHistoryRow_maps_session_correctly', () => {
    const row = toHistoryRow(mockBaseSession);
    expect(row.durationMs).toBe(10000);
    expect(row.hasErrors).toBe(false);
    expect(row.deviceModel).toBe('iPhone 14 Pro');
  });

  test('Test 14: applyFilters_returns_all_when_no_filters', () => {
    const sessions = [mockBaseSession, { ...mockBaseSession, sessionId: '124' }];
    const result = applyFilters(sessions, DEFAULT_FILTERS);
    expect(result.length).toBe(2);
  });

  test('Test 15: applyFilters_by_operationType', () => {
    const sessions: OperationSession[] = [
      mockBaseSession,
      { ...mockBaseSession, sessionId: '124', operationType: 'FmiOff' },
      { ...mockBaseSession, sessionId: '125', operationType: 'FmiOff' },
    ];
    const result = applyFilters(sessions, { ...DEFAULT_FILTERS, operationType: 'FmiOff' });
    expect(result.length).toBe(2);
    expect(result[0].operationType).toBe('FmiOff');
  });

  test('Test 16: applyFilters_by_search_keyword', () => {
    const sessions: OperationSession[] = [
      mockBaseSession,
      {
        ...mockBaseSession,
        sessionId: '124',
        deviceSnapshotAtStart: { ...mockBaseSession.deviceSnapshotAtStart, model: 'iPad' },
      },
    ];
    const result = applyFilters(sessions, { ...DEFAULT_FILTERS, search: 'iphone' });
    expect(result.length).toBe(1);
    expect(result[0].deviceModel).toBe('iPhone 14 Pro');
  });

  test('Test 17: applyFilters_by_status', () => {
    const sessions: OperationSession[] = [
      mockBaseSession,
      { ...mockBaseSession, sessionId: '124', status: 'failed' },
    ];
    const result = applyFilters(sessions, { ...DEFAULT_FILTERS, status: 'failed' });
    expect(result.length).toBe(1);
    expect(result[0].status).toBe('failed');
  });
});
