import { DeviceSnapshot } from './device-types';

export type OperationType =
  | 'HelloActivation'
  | 'HelloNoSignalActivation'
  | 'HelloWifiActivation'
  | 'HelloGsmActivation'
  | 'PasscodeActivation'
  | 'FmiOff'
  | 'DfuAssist'
  | 'RecoveryEnter'
  | 'RecoveryExit'
  | 'DfuExit'
  | 'BootFilesActivation'
  | 'BootFilesBackup'
  | 'PurpleModeEntry'
  | 'PurpleModeRestore'
  | 'JailbreakPalera1n'
  | 'JailbreakCheckra1n'
  | 'OtaBlock'
  | 'RestoreBlock'
  | 'Reboot'
  | 'RebootToHello'
  | 'DeviceCheck'
  | 'EdlBypass'
  | 'MtkBrom'
  | { customCommand: string };

export type SessionStatus =
  | 'idle'
  | 'preflightPending'
  | 'preflightFailed'
  | 'starting'
  | 'running'
  | 'paused'
  | 'cancelling'
  | 'cancelled'
  | 'completing'
  | 'completed'
  | 'failed'
  | 'retrying';

export interface ProgressStep {
  id: string;
  index: number;
  label: string;
  detail: string | null;
  status: string; // pending, running, done, skipped, failed
  durationMs?: number | null;
  emittedAt: string;
}

export type LogLevel = 'info' | 'warn' | 'error' | 'debug' | 'success';

export interface SessionLog {
  sessionId: string;
  level: LogLevel;
  message: string;
  context?: Record<string, any> | null;
  timestamp: string;
}

export interface PreflightCheck {
  name: string;
  required: boolean;
  passed: boolean;
  message: string;
}

export interface PreflightResult {
  passed: boolean;
  checks: PreflightCheck[];
  blockingIssues: string[];
  warnings: string[];
}

export interface OperationSession {
  sessionId: string;
  operationType: OperationType;
  deviceSnapshotAtStart: DeviceSnapshot;
  status: SessionStatus;
  steps: ProgressStep[];
  currentStepIndex: number;
  logs: SessionLog[];
  preflight?: PreflightResult | null;
  startedAt: string;
  updatedAt: string;
  completedAt?: string | null;
  outcome?: 'success' | 'partial' | 'failed' | 'cancelled' | null;
  resultPayload?: Record<string, any> | null;
  errorCode?: string | null;
  errorMessage?: string | null;
  retryCount: number;
  canRetry: boolean;
  canCancel: boolean;
}
