import { useEffect, useRef } from 'react';
import { SessionLog } from '../../lib/session-types';
import { LogLine } from './LogLine';

interface LogLineListProps {
  logs: SessionLog[];
}

export function LogLineList({ logs }: LogLineListProps) {
  const containerRef = useRef<HTMLDivElement>(null);

  // Auto-scroll to bottom on new logs
  useEffect(() => {
    if (containerRef.current) {
      containerRef.current.scrollTop = containerRef.current.scrollHeight;
    }
  }, [logs.length]);

  return (
    <div
      ref={containerRef}
      className="flex-1 overflow-y-auto bg-black p-4 space-y-1 font-mono overscroll-contain"
    >
      {logs.length === 0 ? (
        <div className="text-gray-600 text-sm italic h-full flex items-center justify-center">
          Waiting for logs...
        </div>
      ) : (
        logs.map((log, i) => <LogLine key={`${log.timestamp}-${i}`} log={log} />)
      )}
    </div>
  );
}
