import { useEffect, useRef } from 'react';
import './ExecutionConsole.css';

interface ExecutionConsoleProps {
  lines: string[];
}

export function ExecutionConsole({ lines }: ExecutionConsoleProps) {
  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [lines]);

  return (
    <div className="execution-console">
      <div className="console-header">
        <span className="console-title">GLOBAL EXECUTION LOG</span>
        <button className="clear-btn" onClick={() => {}}>Clear</button>
      </div>
      <div className="console-body" ref={scrollRef}>
        {lines.length === 0 ? (
          <div className="empty-console">No operations started.</div>
        ) : (
          lines.map((line, i) => (
            <div key={i} className={`console-line ${
              line.includes('❌') ? 'error' : 
              line.includes('✅') ? 'success' : 
              line.includes('⚠️') ? 'warn' : ''
            }`}>
              <span className="line-prefix">[{new Date().toLocaleTimeString()}]</span> {line}
            </div>
          ))
        )}
      </div>
    </div>
  );
}
