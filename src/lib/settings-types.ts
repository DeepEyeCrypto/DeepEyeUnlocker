export interface AppSettings {
  theme: 'dark' | 'light' | 'system';
  language: 'en' | 'ar' | 'tr' | 'zh';
  logLevel: 'debug' | 'info' | 'warn' | 'error';
  autoDetectDevice: boolean;
  confirmDangerousActions: boolean;
  showRiskBadges: boolean;
  autoCheckUpdates: boolean;
  sendAnonymousDiagnostics: boolean;
  exportPath: string | null;
}

export const DEFAULT_SETTINGS: AppSettings = {
  theme: 'dark',
  language: 'en',
  logLevel: 'info',
  autoDetectDevice: true,
  confirmDangerousActions: true,
  showRiskBadges: true,
  autoCheckUpdates: true,
  sendAnonymousDiagnostics: false,
  exportPath: null,
};

export type LicenseType = 'free' | 'trial' | 'pro' | 'expired';

export interface LicenseFeatureSet {
  maxDevicesPerSession: number;
  canUseJailbreakTools: boolean;
  canUseBootFiles: boolean;
  canUseFmiOff: boolean;
  canExportLogs: boolean;
  canUseEdlPipeline: boolean;
  canUseMtkBrom: boolean;
}

export interface LicenseStatus {
  licenseType: LicenseType;
  isValid: boolean;
  expiresAt: string | null;
  daysRemaining: number | null;
  seatId: string | null;
  activatedAt: string | null;
  lastValidatedAt: string;
  features: LicenseFeatureSet;
}

export interface UpdateInfo {
  currentVersion: string;
  latestVersion: string | null;
  updateAvailable: boolean;
  releaseUrl: string | null;
  releaseNotes: string | null;
  checkedAt: string;
}
