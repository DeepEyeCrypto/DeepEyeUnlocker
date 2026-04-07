export type DetectInterval = 1000 | 2000 | 5000;

export type AppSettings = {
  adbBinaryPath: string;
  adbOverTcp: boolean;
  tcpPort: number;
  usbDetectIntervalMs: DetectInterval;
  usbDebugLogging: boolean;
};

const STORAGE_KEY = "deepeye.settings.v2027.10.0";

export const DEFAULT_APP_SETTINGS: AppSettings = {
  adbBinaryPath: "adb",
  adbOverTcp: false,
  tcpPort: 5555,
  usbDetectIntervalMs: 2000,
  usbDebugLogging: false,
};

function clampTcpPort(value: unknown): number {
  const parsed = Number(value);
  if (!Number.isFinite(parsed)) {
    return DEFAULT_APP_SETTINGS.tcpPort;
  }

  return Math.min(65535, Math.max(1, Math.trunc(parsed)));
}

function normalizeInterval(value: unknown): DetectInterval {
  if (value === 1000 || value === 2000 || value === 5000) {
    return value;
  }

  return DEFAULT_APP_SETTINGS.usbDetectIntervalMs;
}

export function loadAppSettings(): AppSettings {
  if (typeof window === "undefined") {
    return DEFAULT_APP_SETTINGS;
  }

  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return DEFAULT_APP_SETTINGS;
    }

    const parsed = JSON.parse(raw) as Partial<AppSettings>;

    return {
      adbBinaryPath:
        typeof parsed.adbBinaryPath === "string" && parsed.adbBinaryPath.trim().length > 0
          ? parsed.adbBinaryPath
          : DEFAULT_APP_SETTINGS.adbBinaryPath,
      adbOverTcp:
        typeof parsed.adbOverTcp === "boolean"
          ? parsed.adbOverTcp
          : DEFAULT_APP_SETTINGS.adbOverTcp,
      tcpPort: clampTcpPort(parsed.tcpPort),
      usbDetectIntervalMs: normalizeInterval(parsed.usbDetectIntervalMs),
      usbDebugLogging:
        typeof parsed.usbDebugLogging === "boolean"
          ? parsed.usbDebugLogging
          : DEFAULT_APP_SETTINGS.usbDebugLogging,
    };
  } catch {
    return DEFAULT_APP_SETTINGS;
  }
}

export function saveAppSettings(settings: AppSettings): void {
  if (typeof window === "undefined") {
    return;
  }

  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(settings));
}
