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

  async function handleRun() {
    setStatus('running');
    setLogs([]);
    setShowLogs(true);
    
    try {
      // Tauri invoke → Rust backend
      // Note: Rust backend needs to handle emitting logs via window events or returning them
      const result = await invoke<string>(tool.fn);
      setLogs(prev => [...prev, `✅ OK: ${result}`]);
      setStatus('success');
      if (onRun) onRun([...logs, `✅ ${tool.name} completed`]);
    } catch (e) {
      setStatus('error');
      setLogs(prev => [...prev, `❌ Error: ${e}`]);
      if (onRun) onRun([...logs, `❌ ${tool.name} failed: ${e}`]);
    }
  }

  return (
    <div className={`tool-card tool-card--${status} glass-card`}>
      <div className="tool-card__header">
        <div className="tool-card__meta">
          <span className="protocol-badge">{tool.protocol}</span>
          <span className={`status-dot status-dot--${tool.status}`} />
        </div>
        <h3 className="tool-card__name">{tool.name}</h3>
        <p className="tool-card__desc">{tool.description}</p>
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
