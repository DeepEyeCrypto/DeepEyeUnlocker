import { OperationSession } from './session-types';

export interface LogExportBundle {
  exportedAt: string;
  appVersion: string;
  sessions: OperationSession[];
  format: 'json' | 'txt';
}

function formatDate(isoString: string): string {
  if (!isoString) return '—';
  return new Date(isoString).toLocaleString('en-US', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  });
}

function getOpString(op: any): string {
  if (typeof op === 'string') return op;
  return op.customCommand || 'Unknown';
}

function generateTxtContent(sessions: OperationSession[]): string {
  let output = `DeepEyeUnlocker — Log Export\nExported: ${formatDate(new Date().toISOString())}\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n`;

  for (const s of sessions) {
    const snap = s.deviceSnapshotAtStart;
    const start = new Date(s.startedAt).getTime();
    const end = s.completedAt ? new Date(s.completedAt).getTime() : start;
    const durationStr = s.completedAt ? `${Math.round((end - start) / 1000)}s` : 'Ongoing';
    const outcomeStr = (s.outcome || s.status).toUpperCase();

    output += `SESSION: ${getOpString(s.operationType)}\n`;
    output += `  Device:    ${snap.model} | iOS ${snap.osVersion || 'Unknown'} | ECID: ${snap.serial || 'Unknown'}\n`;
    output += `  Started:   ${formatDate(s.startedAt)}\n`;
    output += `  Completed: ${s.completedAt ? formatDate(s.completedAt) : '—'}\n`;
    output += `  Duration:  ${durationStr}\n`;
    output += `  Outcome:   ${outcomeStr}\n\n`;

    output += `  STEPS\n`;
    for (const step of s.steps) {
      let icon = '[ ]';
      if (step.status === 'done') icon = '[✓]';
      if (step.status === 'failed') icon = '[✗]';
      if (step.status === 'running') icon = '[*]';
      output += `  ${icon} Step ${step.index}: ${step.label.padEnd(25)} (${step.durationMs ? step.durationMs + 'ms' : '—'})\n`;
    }

    output += `\n  LOGS\n`;
    for (const log of s.logs) {
      const time = new Date(log.timestamp).toLocaleTimeString('en-US', { hour12: false });
      output += `  ${time} [${log.level.toUpperCase()}]  ${log.message}\n`;
    }

    output += `\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n`;
  }

  return output;
}

export function downloadBundle(content: string, filename: string, mimeType: string) {
  const blob = new Blob([content], { type: mimeType });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}

export function exportLogBundle(sessions: OperationSession[], format: 'txt' | 'json') {
  const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
  const filename = `deepeye-logs-${timestamp}.${format}`;

  if (format === 'json') {
    const bundle: LogExportBundle = {
      exportedAt: new Date().toISOString(),
      appVersion: '2027.21.0', // Could be fetched dynamically
      sessions,
      format,
    };
    downloadBundle(JSON.stringify(bundle, null, 2), filename, 'application/json');
  } else {
    const content = generateTxtContent(sessions);
    downloadBundle(content, filename, 'text/plain');
  }
}
