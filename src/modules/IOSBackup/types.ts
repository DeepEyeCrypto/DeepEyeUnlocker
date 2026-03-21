export type BackupMode = 'info' | 'crack' | 'screentime'

export interface BackupInfo {
  device_name: string
  ios_version: string
  encrypted: boolean
  last_backup: string
  serial?: string
  udid?: string
}

export interface ScreenTimeResult {
  method: string
  passcode: string | null
  success: boolean
  detail: string
}

export interface CrackProgress {
  speed: string
  progress: string
  phase?: string
}

export interface CrackFoundPayload {
  password: string
}

export type CommandResult =
  | { ok: true; data: BackupInfo | ScreenTimeResult | CrackFoundPayload | null }
  | { ok: false; error: string }
