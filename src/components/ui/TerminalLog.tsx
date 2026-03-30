import { useEffect, useRef } from "react";

function classifyLine(line: string): string {
  if (/error|fail|denied/i.test(line)) return "log-error";
  if (/warn|caution/i.test(line)) return "log-warn";
  if (/success|done|complete/i.test(line)) return "log-success";
  if (/^\[info\]/i.test(line)) return "log-info";
  return "";
}

export function TerminalLog({ lines }: { lines: string[] }) {
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (ref.current) {
      ref.current.scrollTop = ref.current.scrollHeight;
    }
  }, [lines]);

  return (
    <div className="terminal-log" ref={ref}>
      {lines.map((line, i) => (
        <div key={`${line}-${i}`} className={`log-line ${classifyLine(line)}`}>
          <span className="log-prefix">&gt;</span>
          <span className="log-text">{line}</span>
        </div>
      ))}
    </div>
  );
}

