import { useCallback, useEffect, useRef, useState } from 'react'
import { invoke } from '@tauri-apps/api/core'
import { listen, type UnlistenFn } from '@tauri-apps/api/event'

import type {
  BackupInfo,
  BackupMode,
  CommandResult,
  CrackFoundPayload,
  CrackProgress,
  ScreenTimeResult,
} from '../types'

export interface RunBackupArgs {
  backupPath: string
  password?: string
  wordlist?: string
}

type CommandName =
  | 'ios_backup_info'
  | 'ios_extract_hash'
  | 'ios_extract_screentime'
  | 'ios_run_crack'

const DEFAULT_ERROR_MESSAGE = 'Unexpected iOS backup module error.'

export const DEFAULT_WORDLIST = '/usr/share/wordlists/rockyou.txt'

const commandCatalog = {
  info: 'ios_backup_info',
  hash: 'ios_extract_hash',
  screentime: 'ios_extract_screentime',
  crack: 'ios_run_crack',
} as const satisfies Record<'info' | 'hash' | 'screentime' | 'crack', CommandName>

// Keeps the full Rust command surface mapped in one place.
const commandsByMode = {
  info: { run: commandCatalog.info },
  crack: { prepare: commandCatalog.hash, run: commandCatalog.crack },
  screentime: { run: commandCatalog.screentime },
} as const

const isRecord = (value: unknown): value is Record<string, unknown> => {
  return typeof value === 'object' && value !== null
}

const readString = (value: Record<string, unknown>, ...keys: string[]): string | undefined => {
  for (const key of keys) {
    const candidate = value[key]

    if (typeof candidate === 'string') {
      return candidate
    }

    if (typeof candidate === 'number' || typeof candidate === 'boolean') {
      return String(candidate)
    }
  }

  return undefined
}

const readBoolean = (value: Record<string, unknown>, ...keys: string[]): boolean | undefined => {
  for (const key of keys) {
    const candidate = value[key]

    if (typeof candidate === 'boolean') {
      return candidate
    }

    if (typeof candidate === 'string') {
      const normalized = candidate.trim().toLowerCase()

      if (normalized === 'true' || normalized === '1' || normalized === 'yes') {
        return true
      }

      if (normalized === 'false' || normalized === '0' || normalized === 'no') {
        return false
      }
    }
  }

  return undefined
}

const parseJsonRecord = (value: string): Record<string, unknown> | null => {
  try {
    const parsed: unknown = JSON.parse(value)
    return isRecord(parsed) ? parsed : null
  } catch {
    return null
  }
}

const normalizeBackupInfo = (value: unknown): BackupInfo => {
  if (!isRecord(value)) {
    return {
      device_name: 'Unknown Device',
      ios_version: 'Unknown',
      encrypted: false,
      last_backup: 'Unknown',
    }
  }

  return {
    device_name: readString(value, 'device_name', 'deviceName', 'name') ?? 'Unknown Device',
    ios_version: readString(value, 'ios_version', 'version', 'iosVersion') ?? 'Unknown',
    encrypted: readBoolean(value, 'encrypted', 'is_encrypted', 'isEncrypted') ?? false,
    last_backup:
      readString(value, 'last_backup', 'lastBackup', 'backup_date', 'backupDate') ?? 'Unknown',
    serial: readString(value, 'serial', 'serial_number', 'serialNumber'),
    udid: readString(value, 'udid'),
  }
}

const normalizeScreenTimeResult = (value: unknown): ScreenTimeResult => {
  if (!isRecord(value)) {
    return {
      method: 'backup',
      passcode: null,
      success: false,
      detail: 'Screen Time extraction returned an unreadable payload.',
    }
  }

  const passcode = readString(value, 'passcode', 'password', 'pin') ?? null
  const success = readBoolean(value, 'success') ?? Boolean(passcode)

  return {
    method: readString(value, 'method', 'source', 'mode') ?? 'backup',
    passcode,
    success,
    detail:
      readString(value, 'detail', 'message', 'status') ??
      (success
        ? 'Screen Time passcode extraction completed successfully.'
        : 'Screen Time passcode was not recovered.'),
  }
}

const parseCrackProgress = (payload: CrackProgress | string): CrackProgress => {
  if (typeof payload !== 'string') {
    return {
      speed: payload.speed,
      progress: payload.progress,
      phase: payload.phase,
    }
  }

  const jsonPayload = parseJsonRecord(payload)

  if (jsonPayload) {
    return {
      speed: readString(jsonPayload, 'speed') ?? '—',
      progress: readString(jsonPayload, 'progress', 'message', 'status') ?? 'Working…',
      phase: readString(jsonPayload, 'phase', 'stage'),
    }
  }

  const trimmed = payload.trim()
  const speedMatch = trimmed.match(/([0-9][0-9.,]*\s*(?:[kKmMgGTP]?H\/s))/)
  const percentMatch = trimmed.match(/([0-9]+(?:\.[0-9]+)?%)/)
  const phaseMatch =
    trimmed.match(/STATUS\s*[:\-]\s*([^|]+)/i) ?? trimmed.match(/^\[([^\]]+)\]/)

  return {
    speed: speedMatch?.[1] ?? '—',
    progress: (percentMatch?.[1] ?? trimmed) || 'Working…',
    phase: phaseMatch?.[1]?.trim() ?? 'running',
  }
}

const parseCrackFoundPayload = (payload: CrackFoundPayload | string): CrackFoundPayload => {
  if (typeof payload !== 'string') {
    return { password: payload.password }
  }

  const jsonPayload = parseJsonRecord(payload)
  const jsonPassword = jsonPayload ? readString(jsonPayload, 'password', 'passcode') : undefined

  if (jsonPassword) {
    return { password: jsonPassword }
  }

  const match =
    payload.match(/PASSWORD\s+FOUND\s*[:\-]\s*(.+)$/i) ??
    payload.match(/password\s*[:\-]\s*(.+)$/i)

  return {
    password: (match?.[1] ?? payload).trim().replace(/^["'`]+|["'`]+$/g, ''),
  }
}

const getErrorMessage = (error: unknown): string => {
  if (error instanceof Error) {
    return error.message
  }

  if (typeof error === 'string') {
    return error
  }

  return DEFAULT_ERROR_MESSAGE
}

export const useBackupModule = (mode: BackupMode) => {
  const unlistenRef = useRef<UnlistenFn | null>(null)
  const foundPasswordRef = useRef(false)
  const operationIdRef = useRef(0)

  const [result, setResult] = useState<CommandResult | null>(null)
  const [progress, setProgress] = useState<CrackProgress | null>(null)
  const [loading, setLoading] = useState(false)

  const detachListeners = useCallback(() => {
    if (unlistenRef.current) {
      unlistenRef.current()
      unlistenRef.current = null
    }
  }, [])

  const invalidateOperation = useCallback(() => {
    operationIdRef.current += 1
  }, [])

  const isCurrentOperation = useCallback((operationId: number) => {
    return operationIdRef.current === operationId
  }, [])

  const clearResult = useCallback(() => {
    setResult(null)
  }, [])

  const cancel = useCallback(() => {
    invalidateOperation()
    detachListeners()
    setLoading(false)
  }, [detachListeners, invalidateOperation])

  useEffect(() => {
    return () => {
      invalidateOperation()
      detachListeners()
    }
  }, [detachListeners, invalidateOperation])

  useEffect(() => {
    invalidateOperation()
    detachListeners()
    foundPasswordRef.current = false
    setLoading(false)
    setProgress(null)
  }, [mode, detachListeners, invalidateOperation])

  const attachCrackListeners = useCallback(
    async (operationId: number) => {
      detachListeners()

      const unlistenProgress = await listen<CrackProgress | string>(
        'ios-crack-progress',
        (event) => {
          if (!isCurrentOperation(operationId)) {
            return
          }

          setProgress(parseCrackProgress(event.payload))
        },
      )

      const unlistenFound = await listen<CrackFoundPayload | string>('ios-crack-found', (event) => {
        if (!isCurrentOperation(operationId)) {
          return
        }

        foundPasswordRef.current = true
        setResult({ ok: true, data: parseCrackFoundPayload(event.payload) })
        setProgress((current) => ({
          speed: current?.speed ?? '—',
          progress: 'Password recovered',
          phase: 'complete',
        }))
        setLoading(false)
      })

      const unlistenError = await listen<string>('ios-crack-error', (event) => {
        if (!isCurrentOperation(operationId)) {
          return
        }

        setResult({ ok: false, error: event.payload })
        setLoading(false)
      })

      if (!isCurrentOperation(operationId)) {
        unlistenProgress()
        unlistenFound()
        unlistenError()
        return
      }

      unlistenRef.current = () => {
        unlistenProgress()
        unlistenFound()
        unlistenError()
      }
    },
    [detachListeners, isCurrentOperation],
  )

  const run = useCallback(
    async ({ backupPath, password, wordlist }: RunBackupArgs) => {
      const trimmedBackupPath = backupPath.trim()
      const trimmedPassword = password?.trim() ?? ''
      const trimmedWordlist = wordlist?.trim() || DEFAULT_WORDLIST

      if (!trimmedBackupPath) {
        setResult({ ok: false, error: 'Backup path is required.' })
        return
      }

      if (mode === 'screentime' && !trimmedPassword) {
        setResult({ ok: false, error: 'Backup password is required for Screen Time extraction.' })
        return
      }

      operationIdRef.current += 1
      const operationId = operationIdRef.current

      foundPasswordRef.current = false
      setResult(null)
      setProgress(null)
      setLoading(true)

      try {
        if (mode === 'info') {
          const response = await invoke<unknown>(commandsByMode.info.run, {
            backupPath: trimmedBackupPath,
          })

          if (!isCurrentOperation(operationId)) {
            return
          }

          setResult({ ok: true, data: normalizeBackupInfo(response) })
          return
        }

        if (mode === 'screentime') {
          const response = await invoke<unknown>(commandsByMode.screentime.run, {
            backupPath: trimmedBackupPath,
            password: trimmedPassword,
          })

          if (!isCurrentOperation(operationId)) {
            return
          }

          setResult({ ok: true, data: normalizeScreenTimeResult(response) })
          return
        }

        await attachCrackListeners(operationId)

        if (!isCurrentOperation(operationId)) {
          return
        }

        setProgress({
          speed: '—',
          progress: 'Launching wordlist attack…',
          phase: 'initializing',
        })

        await invoke<unknown>(commandsByMode.crack.run, {
          backupPath: trimmedBackupPath,
          wordlist: trimmedWordlist,
        })

        if (!isCurrentOperation(operationId) || foundPasswordRef.current) {
          return
        }

        setResult({ ok: true, data: null })
        setProgress((current) =>
          current ?? {
            speed: '—',
            progress: 'Crack session finished without a recovered password.',
            phase: 'complete',
          },
        )
      } catch (error: unknown) {
        if (!isCurrentOperation(operationId)) {
          return
        }

        setResult({ ok: false, error: getErrorMessage(error) })
      } finally {
        detachListeners()

        if (isCurrentOperation(operationId)) {
          setLoading(false)
        }
      }
    },
    [attachCrackListeners, detachListeners, isCurrentOperation, mode],
  )

  return {
    run,
    cancel,
    result,
    progress,
    loading,
    clearResult,
  }
}