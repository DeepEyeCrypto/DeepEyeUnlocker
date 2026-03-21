import React, { useMemo, useState, type CSSProperties } from 'react'

import { DEFAULT_WORDLIST, useBackupModule } from './hooks/useBackupModule'
import type { BackupInfo, BackupMode, CrackFoundPayload, ScreenTimeResult } from './types'

const glassPanelStyle: CSSProperties = {
  background: 'rgba(255,255,255,0.04)',
  backdropFilter: 'blur(24px)',
  WebkitBackdropFilter: 'blur(24px)',
  border: '1px solid rgba(255,255,255,0.08)',
  boxShadow: '0 24px 80px rgba(15, 23, 42, 0.36)',
  borderRadius: 24,
}

const inputStyle: CSSProperties = {
  width: '100%',
  borderRadius: 16,
  border: '1px solid rgba(255,255,255,0.08)',
  background: 'rgba(8, 15, 33, 0.66)',
  color: '#f8fafc',
  padding: '14px 16px',
  fontSize: 13,
  outline: 'none',
  boxSizing: 'border-box',
  fontFamily: '"JetBrains Mono", "Fira Code", monospace',
}

const labelStyle: CSSProperties = {
  display: 'block',
  marginBottom: 8,
  fontSize: 10,
  letterSpacing: '0.24em',
  textTransform: 'uppercase',
  color: 'rgba(196, 181, 253, 0.76)',
}

const sectionTitleStyle: CSSProperties = {
  margin: 0,
  fontSize: 11,
  letterSpacing: '0.26em',
  textTransform: 'uppercase',
  color: '#cbd5f5',
}

const titleGradientStyle: CSSProperties = {
  margin: 0,
  fontSize: 'clamp(28px, 5vw, 42px)',
  fontWeight: 800,
  lineHeight: 1.05,
  letterSpacing: '-0.04em',
  backgroundImage: 'linear-gradient(135deg, #e9d5ff 0%, #c4b5fd 28%, #93c5fd 68%, #f5d0fe 100%)',
  WebkitBackgroundClip: 'text',
  backgroundClip: 'text',
  color: 'transparent',
}

const modes: Array<{ value: BackupMode; title: string; description: string }> = [
  { value: 'info', title: 'INFO', description: 'Manifest metadata + encryption posture' },
  { value: 'crack', title: 'CRACK', description: 'Wordlist-backed password recovery' },
  { value: 'screentime', title: 'SCREENTIME', description: 'Extract Screen Time passcode material' },
]

const resultToneStyles: Record<'green' | 'yellow' | 'red', CSSProperties> = {
  green: {
    borderColor: 'rgba(74, 222, 128, 0.42)',
    boxShadow: '0 24px 80px rgba(22, 101, 52, 0.2)',
  },
  yellow: {
    borderColor: 'rgba(250, 204, 21, 0.34)',
    boxShadow: '0 24px 80px rgba(161, 98, 7, 0.16)',
  },
  red: {
    borderColor: 'rgba(248, 113, 113, 0.38)',
    boxShadow: '0 24px 80px rgba(127, 29, 29, 0.16)',
  },
}

const isRecord = (value: unknown): value is Record<string, unknown> => {
  return typeof value === 'object' && value !== null
}

const isBackupInfo = (value: any): value is BackupInfo => {
  return (
    isRecord(value) &&
    typeof value.device_name === 'string' &&
    typeof value.ios_version === 'string' &&
    typeof value.encrypted === 'boolean'
  )
}

const isScreenTimeResult = (value: any): value is ScreenTimeResult => {
  return (
    isRecord(value) &&
    typeof value.method === 'string' &&
    typeof value.success === 'boolean' &&
    typeof value.detail === 'string'
  )
}

const isCrackFoundPayload = (value: any): value is CrackFoundPayload => {
  return isRecord(value) && typeof value.password === 'string'
}

const extractPercent = (value: string | undefined): number | null => {
  if (!value) {
    return null
  }

  const match = value.match(/([0-9]+(?:\.[0-9]+)?)%/)

  if (!match) {
    return null
  }

  return Math.max(0, Math.min(100, Number(match[1])))
}

const toneLabelMap: Record<'green' | 'yellow' | 'red', string> = {
  green: 'verified',
  yellow: 'informational',
  red: 'critical',
}

const IOSBackupLayer: React.FC = () => {
  const [mode, setMode] = useState<BackupMode>('info')
  const [backupPath, setBackupPath] = useState('')
  const [password, setPassword] = useState('')
  const [wordlist, setWordlist] = useState(DEFAULT_WORDLIST)

  const { run, cancel, result, progress, loading, clearResult } = useBackupModule(mode)

  const activeMode = useMemo(() => {
    return modes.find((entry) => entry.value === mode) ?? modes[0]
  }, [mode])

  const resultTone = useMemo<'green' | 'yellow' | 'red'>(() => {
    if (!result) {
      return 'yellow'
    }

    if (!result.ok) {
      return 'red'
    }

    if (result.data && isCrackFoundPayload(result.data)) {
      return 'green'
    }

    if (result.data && isScreenTimeResult(result.data)) {
      return result.data.success ? 'green' : 'yellow'
    }

    return 'yellow'
  }, [result])

  const progressPercent = useMemo(() => {
    return extractPercent(progress?.progress)
  }, [progress])

  const primaryLabel = useMemo(() => {
    if (mode === 'info') {
      return loading ? 'ANALYZING…' : 'ANALYZE BACKUP'
    }

    if (mode === 'crack') {
      return loading ? 'BRUTEFORCING…' : 'RUN BRUTEFORCE'
    }

    return loading ? 'EXTRACTING…' : 'EXTRACT SCREEN TIME'
  }, [loading, mode])

  const canRun = useMemo(() => {
    if (!backupPath.trim() || loading) {
      return false
    }

    if (mode === 'screentime') {
      return password.trim().length > 0
    }

    return true
  }, [backupPath, loading, mode, password])

  const handleRun = async () => {
    await run({ backupPath, password, wordlist })
  }

  const handleModeChange = (nextMode: BackupMode) => {
    if (nextMode === mode) {
      return
    }

    cancel()
    clearResult()
    setMode(nextMode)
  }

  const renderResult = () => {
    if (!result) {
      return (
        <div style={{ color: 'rgba(226, 232, 240, 0.62)', fontSize: 13, lineHeight: 1.7 }}>
          Awaiting command output. Select a mode, review the required inputs, then dispatch the module.
        </div>
      )
    }

    if (!result.ok) {
      return (
        <div style={{ display: 'grid', gap: 12 }}>
          <div style={{ color: '#fca5a5', fontSize: 12, letterSpacing: '0.18em', textTransform: 'uppercase' }}>
            Execution fault
          </div>
          <div style={{ color: '#fecaca', fontSize: 13, lineHeight: 1.7 }}>{result.error}</div>
        </div>
      )
    }

    if (result.data && isBackupInfo(result.data)) {
      const info = result.data
      return (
        <div style={{ display: 'grid', gap: 14 }}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 14 }}>
            <div>
              <div style={labelStyle}>Device Name</div>
              <div style={{ color: '#f8fafc', fontSize: 15 }}>{info.device_name}</div>
            </div>
            <div>
              <div style={labelStyle}>iOS Version</div>
              <div style={{ color: '#f8fafc', fontSize: 15 }}>{info.ios_version}</div>
            </div>
            <div>
              <div style={labelStyle}>Encryption</div>
              <div style={{ color: info.encrypted ? '#facc15' : '#86efac', fontSize: 15 }}>
                {info.encrypted ? 'ACTIVE (SECURE)' : 'INACTIVE (OPEN)'}
              </div>
            </div>
            <div>
              <div style={labelStyle}>Last Backup</div>
              <div style={{ color: '#cbd5e1', fontSize: 15 }}>{info.last_backup}</div>
            </div>
          </div>

          {(info.serial || info.udid) && (
            <div
              style={{
                borderRadius: 18,
                background: 'rgba(2, 6, 23, 0.42)',
                border: '1px solid rgba(255,255,255,0.06)',
                padding: 14,
                display: 'grid',
                gap: 8,
              }}
            >
              {info.serial && (
                <div style={{ color: '#cbd5e1', fontSize: 12 }}>
                  <span style={{ color: 'rgba(196,181,253,0.76)' }}>SERIAL</span> — {info.serial}
                </div>
              )}
              {info.udid && (
                <div style={{ color: '#cbd5e1', fontSize: 12, wordBreak: 'break-all' }}>
                  <span style={{ color: 'rgba(196,181,253,0.76)' }}>UDID</span> — {info.udid}
                </div>
              )}
            </div>
          )}
        </div>
      )
    }

    if (result.data && isScreenTimeResult(result.data)) {
      const st = result.data
      return (
        <div style={{ display: 'grid', gap: 14 }}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 14 }}>
            <div>
              <div style={labelStyle}>Method</div>
              <div style={{ color: '#e9d5ff', fontSize: 15 }}>{st.method}</div>
            </div>
            <div>
              <div style={labelStyle}>Passcode</div>
              <div style={{ color: st.passcode ? '#86efac' : '#fcd34d', fontSize: 15 }}>
                {st.passcode ?? 'Not recovered'}
              </div>
            </div>
            <div>
              <div style={labelStyle}>Success</div>
              <div style={{ color: st.success ? '#86efac' : '#fcd34d', fontSize: 15 }}>
                {st.success ? 'TRUE' : 'FALSE'}
              </div>
            </div>
          </div>

          <div
            style={{
              borderRadius: 18,
              background: 'rgba(2, 6, 23, 0.42)',
              border: '1px solid rgba(255,255,255,0.06)',
              padding: 14,
              color: '#cbd5e1',
              fontSize: 13,
              lineHeight: 1.7,
            }}
          >
            {st.detail}
          </div>
        </div>
      )
    }

    if (result.data && isCrackFoundPayload(result.data)) {
      return (
        <div style={{ display: 'grid', gap: 12 }}>
          <div style={{ color: '#86efac', fontSize: 12, letterSpacing: '0.2em', textTransform: 'uppercase' }}>
            Password recovered
          </div>
          <div
            style={{
              borderRadius: 18,
              background: 'rgba(20, 83, 45, 0.16)',
              border: '1px solid rgba(74, 222, 128, 0.3)',
              padding: '16px 18px',
              color: '#f8fafc',
              fontSize: 24,
              fontWeight: 700,
              wordBreak: 'break-word',
            }}
          >
            {result.data.password}
          </div>
        </div>
      )
    }

    return (
      <div style={{ color: '#fcd34d', fontSize: 13, lineHeight: 1.7 }}>
        Crack session completed without a recovered password.
      </div>
    )
  }

  return (
    <div
      style={{
        maxWidth: 1120,
        margin: '0 auto',
        display: 'grid',
        gap: 24,
        color: '#f8fafc',
        fontFamily: '"JetBrains Mono", "Fira Code", monospace',
        position: 'relative',
      }}
    >
      <style>{`
        @keyframes ios-backup-scan {
          0% { transform: translateX(-120%); }
          50% { transform: translateX(15%); }
          100% { transform: translateX(220%); }
        }
      `}</style>

      <section style={{ ...glassPanelStyle, position: 'relative', overflow: 'hidden', padding: 28 }}>
        <div
          style={{
            position: 'absolute',
            inset: 0,
            background:
              'radial-gradient(circle at top right, rgba(99,102,241,0.22), transparent 36%), radial-gradient(circle at bottom left, rgba(168,85,247,0.16), transparent 40%)',
            pointerEvents: 'none',
          }}
        />

        <div style={{ position: 'relative', display: 'grid', gap: 24 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', gap: 16, flexWrap: 'wrap', alignItems: 'flex-start' }}>
            <div style={{ display: 'grid', gap: 10, maxWidth: 760 }}>
              <div style={{ ...labelStyle, marginBottom: 0 }}>DeepEye iOS Backup Module</div>
              <h1 style={titleGradientStyle}>Tauri v2 backup forensics + recovery console</h1>
              <p style={{ margin: 0, color: 'rgba(226, 232, 240, 0.72)', lineHeight: 1.8, fontSize: 13 }}>
                Preserve the existing glassmorphism surface while routing all desktop command execution through a
                typed Tauri v2 hook boundary.
              </p>
            </div>

            <div
              style={{
                alignSelf: 'center',
                padding: '10px 14px',
                borderRadius: 999,
                background: 'rgba(76, 29, 148, 0.2)',
                border: '1px solid rgba(196,181,253,0.18)',
                color: '#ddd6fe',
                fontSize: 11,
                letterSpacing: '0.16em',
                textTransform: 'uppercase',
              }}
            >
              {loading ? 'engine busy' : activeMode.title.toLowerCase()}
            </div>
          </div>

          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
              gap: 12,
            }}
          >
            {modes.map((entry) => {
              const isActive = entry.value === mode

              return (
                <button
                  key={entry.value}
                  onClick={() => handleModeChange(entry.value)}
                  style={{
                    borderRadius: 18,
                    padding: '14px 16px',
                    textAlign: 'left',
                    border: `1px solid ${isActive ? 'rgba(147,197,253,0.34)' : 'rgba(255,255,255,0.08)'}`,
                    background: isActive
                      ? 'linear-gradient(135deg, rgba(79,70,229,0.28), rgba(124,58,237,0.18))'
                      : 'rgba(8,15,33,0.46)',
                    color: '#f8fafc',
                    cursor: 'pointer',
                    boxShadow: isActive ? '0 18px 50px rgba(67, 56, 202, 0.22)' : 'none',
                    fontFamily: 'inherit',
                  }}
                  type="button"
                >
                  <div style={{ fontSize: 11, letterSpacing: '0.24em', textTransform: 'uppercase', marginBottom: 6 }}>
                    {entry.title}
                  </div>
                  <div style={{ fontSize: 12, color: 'rgba(226,232,240,0.72)', lineHeight: 1.6 }}>
                    {entry.description}
                  </div>
                </button>
              )
            })}
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: 16 }}>
            <div style={{ gridColumn: '1 / -1' }}>
              <label htmlFor="ios-backup-path" style={labelStyle}>
                Local Backup Path
              </label>
              <input
                id="ios-backup-path"
                onChange={(event) => setBackupPath(event.target.value)}
                placeholder="/Users/example/Library/Application Support/MobileSync/Backup/..."
                style={inputStyle}
                type="text"
                value={backupPath}
              />
            </div>

            {mode === 'screentime' && (
              <div>
                <label htmlFor="ios-backup-password" style={labelStyle}>
                  Backup Password
                </label>
                <input
                  id="ios-backup-password"
                  onChange={(event) => setPassword(event.target.value)}
                  placeholder="Required for Screen Time extraction"
                  style={inputStyle}
                  type="password"
                  value={password}
                />
              </div>
            )}

            {mode === 'crack' && (
              <div style={{ gridColumn: '1 / -1' }}>
                <label htmlFor="ios-backup-wordlist" style={labelStyle}>
                  Wordlist Path (Optional Override)
                </label>
                <input
                  id="ios-backup-wordlist"
                  onChange={(event) => setWordlist(event.target.value)}
                  placeholder={DEFAULT_WORDLIST}
                  style={inputStyle}
                  type="text"
                  value={wordlist}
                />
              </div>
            )}
          </div>

          <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
            <button
              disabled={!canRun}
              onClick={handleRun}
              style={{
                padding: '14px 22px',
                borderRadius: 16,
                border: '1px solid rgba(96,165,250,0.32)',
                background: canRun
                  ? 'linear-gradient(135deg, rgba(37,99,235,0.82), rgba(109,40,217,0.82))'
                  : 'rgba(71, 85, 105, 0.4)',
                color: '#ffffff',
                fontSize: 12,
                letterSpacing: '0.18em',
                textTransform: 'uppercase',
                cursor: canRun ? 'pointer' : 'not-allowed',
                fontFamily: 'inherit',
                boxShadow: canRun ? '0 16px 40px rgba(37,99,235,0.28)' : 'none',
              }}
              type="button"
            >
              {primaryLabel}
            </button>

            {loading && mode === 'crack' && (
              <button
                onClick={cancel}
                style={{
                  padding: '14px 22px',
                  borderRadius: 16,
                  border: '1px solid rgba(248,113,113,0.28)',
                  background: 'rgba(127,29,29,0.24)',
                  color: '#fecaca',
                  fontSize: 12,
                  letterSpacing: '0.18em',
                  textTransform: 'uppercase',
                  cursor: 'pointer',
                  fontFamily: 'inherit',
                }}
                type="button"
              >
                Cancel
              </button>
            )}

            {result && (
              <button
                onClick={clearResult}
                style={{
                  padding: '14px 22px',
                  borderRadius: 16,
                  border: '1px solid rgba(255,255,255,0.08)',
                  background: 'rgba(8,15,33,0.46)',
                  color: '#cbd5e1',
                  fontSize: 12,
                  letterSpacing: '0.18em',
                  textTransform: 'uppercase',
                  cursor: 'pointer',
                  fontFamily: 'inherit',
                }}
                type="button"
              >
                Clear Result
              </button>
            )}
          </div>
        </div>
      </section>

      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))',
          gap: 24,
        }}
      >
        <section style={{ ...glassPanelStyle, padding: 24, display: 'grid', gap: 18 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12, alignItems: 'center', flexWrap: 'wrap' }}>
            <h2 style={sectionTitleStyle}>Live Status</h2>
            <span
              style={{
                padding: '8px 12px',
                borderRadius: 999,
                border: '1px solid rgba(255,255,255,0.08)',
                background: 'rgba(8,15,33,0.46)',
                color: '#cbd5e1',
                fontSize: 10,
                letterSpacing: '0.2em',
                textTransform: 'uppercase',
              }}
            >
              {loading ? 'active' : 'idle'}
            </span>
          </div>

          {mode === 'crack' ? (
            <div style={{ display: 'grid', gap: 16 }}>
              <div
                style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))',
                  gap: 12,
                }}
              >
                <div
                  style={{
                    borderRadius: 18,
                    padding: 14,
                    background: 'rgba(2, 6, 23, 0.42)',
                    border: '1px solid rgba(255,255,255,0.06)',
                  }}
                >
                  <div style={labelStyle}>Phase</div>
                  <div style={{ color: '#f8fafc', fontSize: 15 }}>{progress?.phase ?? 'idle'}</div>
                </div>

                <div
                  style={{
                    borderRadius: 18,
                    padding: 14,
                    background: 'rgba(2, 6, 23, 0.42)',
                    border: '1px solid rgba(255,255,255,0.06)',
                  }}
                >
                  <div style={labelStyle}>Speed</div>
                  <div style={{ color: '#93c5fd', fontSize: 15 }}>{progress?.speed ?? '—'}</div>
                </div>
              </div>

              <div>
                <div style={{ ...labelStyle, marginBottom: 10 }}>Progress</div>
                <div
                  style={{
                    position: 'relative',
                    overflow: 'hidden',
                    height: 10,
                    borderRadius: 999,
                    background: 'rgba(255,255,255,0.06)',
                    border: '1px solid rgba(255,255,255,0.05)',
                  }}
                >
                  {progressPercent !== null ? (
                    <div
                      style={{
                        width: `${progressPercent}%`,
                        height: '100%',
                        borderRadius: 999,
                        background: 'linear-gradient(90deg, #22c55e 0%, #60a5fa 100%)',
                      }}
                    />
                  ) : loading ? (
                    <div
                      style={{
                        width: '36%',
                        height: '100%',
                        borderRadius: 999,
                        background: 'linear-gradient(90deg, #22c55e 0%, #60a5fa 100%)',
                        animation: 'ios-backup-scan 1.8s ease-in-out infinite',
                      }}
                    />
                  ) : null}
                </div>
              </div>

              <div
                style={{
                  borderRadius: 18,
                  padding: 16,
                  background: 'rgba(2, 6, 23, 0.42)',
                  border: '1px solid rgba(255,255,255,0.06)',
                  color: '#cbd5e1',
                  fontSize: 13,
                  lineHeight: 1.7,
                  minHeight: 82,
                }}
              >
                {progress?.progress ?? 'Ready to launch the cracking engine.'}
              </div>
            </div>
          ) : (
            <div
              style={{
                borderRadius: 20,
                padding: 18,
                background: 'rgba(2, 6, 23, 0.42)',
                border: '1px solid rgba(255,255,255,0.06)',
                color: 'rgba(226, 232, 240, 0.72)',
                fontSize: 13,
                lineHeight: 1.8,
              }}
            >
              {mode === 'info'
                ? 'Info mode reads manifest metadata and normalizes the response into a strict BackupInfo payload.'
                : 'Screen Time mode requires the backup password, then normalizes the response into a typed ScreenTimeResult object.'}
            </div>
          )}
        </section>

        <section
          style={{
            ...glassPanelStyle,
            ...resultToneStyles[resultTone],
            padding: 24,
            display: 'grid',
            gap: 18,
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12, alignItems: 'center', flexWrap: 'wrap' }}>
            <h2 style={sectionTitleStyle}>Result Matrix</h2>
            <span
              style={{
                padding: '8px 12px',
                borderRadius: 999,
                background:
                  resultTone === 'green'
                    ? 'rgba(20, 83, 45, 0.22)'
                    : resultTone === 'red'
                      ? 'rgba(127, 29, 29, 0.22)'
                      : 'rgba(146, 64, 14, 0.18)',
                border: '1px solid rgba(255,255,255,0.08)',
                color:
                  resultTone === 'green'
                    ? '#86efac'
                    : resultTone === 'red'
                      ? '#fca5a5'
                      : '#fcd34d',
                fontSize: 10,
                letterSpacing: '0.2em',
                textTransform: 'uppercase',
              }}
            >
              {toneLabelMap[resultTone]}
            </span>
          </div>

          {renderResult()}
        </section>
      </div>
    </div>
  )
}

export default IOSBackupLayer