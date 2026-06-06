import { exportLogBundle } from '../log-export';
import { OperationSession } from '../session-types';

// Mock URL.createObjectURL
global.URL.createObjectURL = jest.fn();
global.URL.revokeObjectURL = jest.fn();

const mockSession: OperationSession = {
  sessionId: 'export-test-123',
  operationType: 'HelloActivation',
  deviceSnapshotAtStart: {
    id: 'test',
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
      label: 'Detect mode',
      detail: null,
      status: 'done',
      emittedAt: '2026-06-06T20:00:00Z',
    },
  ],
  logs: [
    {
      sessionId: 'export-test-123',
      level: 'info',
      message: 'Session started: HelloActivation',
      timestamp: '2026-06-06T20:00:00Z',
    },
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

describe('Log Export', () => {
  let mockClick: jest.Mock;
  let mockAnchor: any;

  beforeEach(() => {
    mockClick = jest.fn();
    mockAnchor = { click: mockClick, href: '', download: '' };
    jest.spyOn(document, 'createElement').mockReturnValue(mockAnchor);
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  test('Test 18: exportLogBundle_txt_format_contains_session_id', () => {
    const mockBlob = jest.spyOn(global, 'Blob').mockImplementation(function (
      this: any,
      content: any[],
    ) {
      this.content = content[0];
      return this;
    } as any);

    exportLogBundle([mockSession], 'txt');

    expect(mockBlob).toHaveBeenCalled();
    const blobContent = (mockBlob.mock.instances[0] as any).content as string;
    expect(blobContent).toContain('HelloActivation');
    expect(blobContent).toContain('iPhone 14 Pro');
    expect(blobContent).toContain('Session started: HelloActivation');
    expect(blobContent).toContain('Detect mode');
  });
});
