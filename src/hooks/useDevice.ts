import { useDeviceStatusStore } from '../stores/useDeviceStatusStore';
import { CapabilityFlags } from '../lib/device-types';

export function useCurrentDevice() {
  return useDeviceStatusStore((state) => state.currentDevice);
}

export function useDeviceConnection() {
  const connectionState = useDeviceStatusStore((state) => state.connectionState);
  const isScanning = useDeviceStatusStore((state) => state.isScanning);
  const error = useDeviceStatusStore((state) => state.error);
  const refresh = useDeviceStatusStore((state) => state.refresh);

  return { connectionState, isScanning, error, refresh };
}

export function useDeviceCapabilities() {
  const device = useCurrentDevice();
  return {
    capabilities: device?.capabilityFlags ?? [],
    hasCapability: (cap: CapabilityFlags) => device?.capabilityFlags.includes(cap) ?? false,
    riskFlags: device?.riskFlags ?? [],
  };
}
