import { renderHook } from '@testing-library/react';
import { useToolCapability } from '../useToolCapability';
import { useDeviceStatusStore } from '../../stores/useDeviceStatusStore';
import { useOperationSessionStore } from '../../stores/useOperationSessionStore';
import { DeviceSnapshot } from '../../lib/device-types';

// Mock the stores
jest.mock('../../stores/useDeviceStatusStore', () => ({
  useDeviceStatusStore: jest.fn(),
}));
jest.mock('../../stores/useOperationSessionStore', () => ({
  useOperationSessionStore: jest.fn(),
}));

const mockDeviceStatusStore = useDeviceStatusStore as unknown as jest.Mock;
const mockSessionStore = useOperationSessionStore as unknown as jest.Mock;

describe('useToolCapability', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  const baseDevice: DeviceSnapshot = {
    id: '123',
    connectionState: 'connected',
    platform: 'ios',
    mode: 'normal',
    model: 'iPhone 14',
    serial: 'ABCD',
    osVersion: '16.0',
    isSupported: true,
    riskFlags: [],
    capabilityFlags: ['canEnterRecovery'],
    detectedAt: 0,
    updatedAt: 0,
  };

  it('Test 19: useToolCapability_disabled_when_no_device', () => {
    mockDeviceStatusStore.mockImplementation((selector) => selector({ currentDevice: null }));
    mockSessionStore.mockImplementation((selector) => selector({ activeSession: null }));

    const { result } = renderHook(() => useToolCapability('RecoveryEnter', 'any'));

    expect(result.current.enabled).toBe(false);
    expect(result.current.reason).toBe('No device connected');
  });

  it('Test 20: useToolCapability_disabled_when_session_active', () => {
    mockDeviceStatusStore.mockImplementation((selector) => selector({ currentDevice: baseDevice }));
    mockSessionStore.mockImplementation((selector) =>
      selector({ activeSession: { sessionId: '1' } }),
    );

    const { result } = renderHook(() => useToolCapability('RecoveryEnter', 'any'));

    expect(result.current.enabled).toBe(false);
    expect(result.current.reason).toBe('Session in progress');
  });

  it('Test 21: useToolCapability_disabled_when_wrong_mode', () => {
    mockDeviceStatusStore.mockImplementation((selector) => selector({ currentDevice: baseDevice }));
    mockSessionStore.mockImplementation((selector) => selector({ activeSession: null }));

    const { result } = renderHook(() => useToolCapability('RecoveryExit', 'recovery'));

    expect(result.current.enabled).toBe(false);
    expect(result.current.reason).toContain('recovery');
  });

  it('Test 22: useToolCapability_enabled_when_all_conditions_met', () => {
    // baseDevice is connected, normal mode, has canEnterRecovery flag.
    mockDeviceStatusStore.mockImplementation((selector) => selector({ currentDevice: baseDevice }));
    mockSessionStore.mockImplementation((selector) => selector({ activeSession: null }));

    const { result } = renderHook(() => useToolCapability('RecoveryEnter', 'normal'));

    expect(result.current.enabled).toBe(true);
    expect(result.current.reason).toBeNull();
  });
});
