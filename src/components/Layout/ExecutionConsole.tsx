import { useEffect, useRef } from 'react';
import './ExecutionConsole.css';

interface ExecutionConsoleProps {
  lines: string[];
  onClear?: () => void;
  title?: string;
}

export function ExecutionConsole({
  lines,
  onClear,
  title = 'GLOBAL EXECUTION LOG',
}: ExecutionConsoleProps) {
  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [lines]);

  return (
    <div className="execution-console">
      <div className="console-header">
        <span className="console-title">{title}</span>
        <button
          className="btn btn-sm btn-ghost"
          onClick={onClear}
          disabled={!onClear || lines.length === 0}
        >
          Clear
        </button>
      </div>
      <div className="console-body" ref={scrollRef}>
        {lines.length === 0 ? (
          <div className="empty-console">No operations started.</div>
        ) : (
          lines.map((line, i) => (
            <div
              key={i}
              className={`console-line ${
                line.includes('❌')
                  ? 'error'
                  : line.includes('✅')
                    ? 'success'
                    : line.includes('⚠️')
                      ? 'warn'
                      : ''
              }`}
            >
              {line}
            </div>
          ))
        )}
      </div>
    </div>
  );
}
