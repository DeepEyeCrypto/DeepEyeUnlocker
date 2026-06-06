import { useDeviceStatusStore } from '../stores/useDeviceStatusStore';
import { useOperationSessionStore } from '../stores/useOperationSessionStore';
import { OperationType } from '../lib/session-types';
import { CapabilityFlags, DeviceMode } from '../lib/device-types';

export function useToolCapability(
  operationType: OperationType,
  requiredMode: DeviceMode | DeviceMode[] | 'any',
): {
  enabled: boolean;
  reason: string | null;
} {
  const device = useDeviceStatusStore((s) => s.currentDevice);
  const sessionActive = useOperationSessionStore((s) => s.activeSession !== null);

  if (sessionActive) {
    return { enabled: false, reason: 'Session in progress' };
  }
  if (!device || device.connectionState === 'disconnected') {
    return { enabled: false, reason: 'No device connected' };
  }
  if (device.connectionState === 'unstable') {
    return { enabled: false, reason: 'Connection unstable' };
  }
  if (requiredMode !== 'any') {
    const modes = Array.isArray(requiredMode) ? requiredMode : [requiredMode];
    if (!modes.includes(device.mode as DeviceMode)) {
      return {
        enabled: false,
        reason: `Needs device in ${modes.join(' or ')} mode`,
      };
    }
  }

  // check capability flags
  const cap = getRequiredCapability(operationType);
  if (cap && !device.capabilityFlags?.includes(cap)) {
    return { enabled: false, reason: `Missing capability: ${cap}` };
  }

  return { enabled: true, reason: null };
}

function getRequiredCapability(type: OperationType): CapabilityFlags | null {
  if (typeof type === 'object') return null;

  const map: Partial<Record<string, CapabilityFlags>> = {
    RecoveryEnter: 'canEnterRecovery',
    RecoveryExit: 'canExitRecovery',
    JailbreakPalera1n: 'canUseAdb',
    JailbreakCheckra1n: 'canUseAdb',
    HelloActivation: 'canStartSession',
    FmiOff: 'canStartSession',
    BootFilesActivation: 'canStartSession',
  };
  return map[type as string] ?? null;
}
