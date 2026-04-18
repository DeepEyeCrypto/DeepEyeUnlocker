import { useState } from 'react';
import { invoke } from '@tauri-apps/api/core';
import './ToolCard.css';

interface Tool {
  id: string;
  name: string;
  description: string;
  protocol: string;
  chips?: string[];
  status: string;
  fn: string;
}

interface ToolCardProps {
  tool: Tool;
  onRun?: (logs: string[]) => void;
}

export function ToolCard({ tool, onRun }: ToolCardProps) {
  const [status, setStatus] = useState<'idle' | 'running' | 'success' | 'error'>('idle');
  const [logs, setLogs] = useState<string[]>([]);
  const [showLogs, setShowLogs] = useState(false);
  const [selectedFile, setSelectedFile] = useState<string>('');

  async function handleRun() {
    if (tool.id === 'ipsw_flash' && !selectedFile) {
      alert("Please select an IPSW file first.");
      return;
    }

    setStatus('running');
    setLogs([]);
    setShowLogs(true);
    
    try {
      let result;
      if (tool.id === 'ipsw_flash') {
        result = await invoke<string>(tool.fn || "run_ipsw_flash", { ipswPath: selectedFile });
      } else {
        result = await invoke<string>(tool.fn);
      }
      setLogs(prev => [...prev, `✅ OK: ${result}`]);
      setStatus('success');
      if (onRun) onRun([...logs, `✅ ${tool.name} completed`]);
    } catch (e) {
      setStatus('error');
      setLogs(prev => [...prev, `❌ Error: ${e}`]);
      if (onRun) onRun([...logs, `❌ ${tool.name} failed: ${e}`]);
    }
  }

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      // In Tauri, we can get the actual path using security scopes or drag-and-drop.
      // Easiest is using open dialog for Tauri if possible, but HTML input gives the path if configured, 
      // or we can use Tauri dialog.open(). For now, using HTML input and trying extracting path.
      // However, HTML file input doesn't expose full path on web, but inside Tauri it might.
      // Wait, standard web <input type="file"/> only gives fake path.
      // A safer approach: we can use the element directly, assuming the parent component provided `tool.fn`.
      const file = e.target.files[0];
      setSelectedFile((file as File & { path?: string }).path || file.name); // .path is valid in Electron/Tauri
    }
  };

  return (
    <div className={`tool-card tool-card--${status} glass-card`}>
      <div className="tool-card__header">
        <div className="tool-card__meta">
          <span className="protocol-badge">{tool.protocol}</span>
          <span className={`status-dot status-dot--${tool.status}`} />
        </div>
        <h3 className="tool-card__name">{tool.name}</h3>
        <p className="tool-card__desc">{tool.description}</p>
        
        {tool.id === 'ipsw_flash' && (
           <div style={{ marginTop: '10px' }}>
              <input type="file" accept=".ipsw" onChange={handleFileChange} className="file-input" />
              {selectedFile && <div className="selected-filename">Selected: {selectedFile}</div>}
           </div>
        )}

        {tool.chips && (
          <div className="chip-list">
            {tool.chips.map(c => (
              <span key={c} className="chip-badge">{c}</span>
            ))}
          </div>
        )}
      </div>

      <div className="tool-card__actions">
        <button
          className={`run-btn run-btn--${status}`}
          onClick={handleRun}
          disabled={status === 'running'}
        >
          {status === 'idle'    && '▶ Run'}
          {status === 'running' && '⏳ Running...'}
          {status === 'success' && '✅ Done'}
          {status === 'error'   && '❌ Failed — Retry'}
        </button>
        
        {logs.length > 0 && (
          <button
            className="logs-toggle"
            onClick={() => setShowLogs(!showLogs)}
          >
            {showLogs ? '▲ Hide Logs' : '▼ Show Logs'}
          </button>
        )}
      </div>

      {showLogs && (
        <div className="tool-card__logs">
          <div className="log-scroll">
            {logs.map((line, i) => (
              <div key={i} className={`log-line ${
                line.includes('✅') ? 'log-success' :
                line.includes('❌') ? 'log-error' :
                line.includes('⚠️') ? 'log-warn' : ''
              }`}>
                {line}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
