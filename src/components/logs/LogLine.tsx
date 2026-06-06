import { memo } from 'react';
import { SessionLog } from '../../lib/session-types';

interface LogLineProps {
  log: SessionLog;
}

const colorMap: Record<string, string> = {
  info: 'text-blue-400',
  warn: 'text-yellow-400',
  error: 'text-red-400',
  success: 'text-green-400',
  debug: 'text-gray-400',
};

export const LogLine = memo(({ log }: LogLineProps) => {
  const time = new Date(log.timestamp).toLocaleTimeString('en-US', { hour12: false });
  const color = colorMap[log.level] || 'text-gray-200';

  return (
    <div className="font-mono text-xs py-0.5 leading-relaxed break-words">
      <span className="text-gray-500 mr-3 shrink-0">[{time}]</span>
      <span className={`mr-2 font-semibold ${color}`}>[{log.level.toUpperCase()}]</span>
      <span className="text-gray-300">{log.message}</span>
    </div>
  );
});
