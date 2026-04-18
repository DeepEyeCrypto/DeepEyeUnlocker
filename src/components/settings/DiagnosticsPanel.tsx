import { useState, useEffect } from 'react';
import { invoke } from '@tauri-apps/api/core';
import './DiagnosticsPanel.css'; // Optional styling if needed

interface DiagResult {
  name: string;
  status: 'ok' | 'missing';
  version: string;
}

export function DiagnosticsPanel() {
  const [results, setResults] = useState<DiagResult[]>([])
  const [loading, setLoading] = useState(false)

  const tools = [
    { name: "ADB",           cmd: "adb",              args: ["version"] },
    { name: "Fastboot",      cmd: "fastboot",          args: ["--version"] },
    { name: "idevice_id",    cmd: "idevice_id",        args: ["--version"] },
    { name: "ideviceinfo",   cmd: "ideviceinfo",       args: ["--version"] },
    { name: "Heimdall",      cmd: "heimdall",          args: ["version"] },
    { name: "tsschecker",    cmd: "tsschecker",        args: ["--version"] },
  ]

  async function runDiagnostics() {
    setLoading(true)
    const res: DiagResult[] = []

    for (const tool of tools) {
      try {
        // Use check_usb_permissions or a generic version check
        const out = await invoke<string>('run_tool_version_check', {
          bin: tool.cmd,
          args: tool.args
        })
        res.push({
          name: tool.name,
          status: 'ok',
          version: out.split('\n')[0].trim()
        })
      } catch {
        res.push({
          name: tool.name,
          status: 'missing',
          version: 'Not installed'
        })
      }
    }

    setResults(res)
    setLoading(false)
  }

  useEffect(() => { runDiagnostics() }, [])

  return (
    <div className="diagnostics-panel">
      <div className="diag-header">
        <h2>🔧 Tool Diagnostics</h2>
        <button onClick={runDiagnostics} disabled={loading}>
          {loading ? '⏳ Checking...' : '🔄 Refresh'}
        </button>
      </div>

      <div className="diag-grid">
        {results.map(r => (
          <div key={r.name}
            className={`diag-item diag-item--${r.status}`}>
            <span className={`diag-dot ${
              r.status === 'ok' ? 'green' : 'red'
            }`} />
            <span className="diag-name">{r.name}</span>
            <span className="diag-ver">{r.version}</span>
          </div>
        ))}
      </div>

      {results.some(r => r.status === 'missing') && (
        <div className="diag-install-hint">
          <p>⚠️ Missing tools detected. Run:</p>
          <code>brew install libimobiledevice android-platform-tools heimdall-flash</code>
        </div>
      )}
    </div>
  )
}
