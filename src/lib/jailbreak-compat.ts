export function isJailbreakCompatible(
  tool: 'JailbreakPalera1n' | 'JailbreakCheckra1n',
  osVersion: string | undefined,
): { compatible: boolean; reason?: string } {
  if (!osVersion) {
    return { compatible: false, reason: 'iOS version unknown' };
  }

  // Basic float parsing, e.g., '16.6' -> 16.6, '14.8.1' -> 14.81
  const parts = osVersion.split('.');
  let v = 0;
  if (parts.length > 0) {
    v += parseInt(parts[0], 10);
  }
  if (parts.length > 1) {
    v += parseInt(parts[1], 10) / 10;
  }
  if (parts.length > 2) {
    v += parseInt(parts[2], 10) / 100;
  }

  if (tool === 'JailbreakPalera1n') {
    if (v < 15.0 || v >= 16.8) {
      return {
        compatible: false,
        reason: `palera1n supports iOS 15.0–16.7. Detected: iOS ${osVersion}`,
      };
    }
  }

  if (tool === 'JailbreakCheckra1n') {
    if (v < 12.0 || v >= 14.82) {
      return {
        compatible: false,
        reason: `checkra1n supports iOS 12.0–14.8.1. Detected: iOS ${osVersion}`,
      };
    }
  }

  return { compatible: true };
}
