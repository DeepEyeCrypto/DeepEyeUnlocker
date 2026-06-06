import { useEffect, useState, useRef, useMemo } from 'react';
import { useOperationSessionStore } from '../../stores/useOperationSessionStore';
import {
  CheckCircle2,
  XCircle,
  Loader2,
  Circle,
  Terminal,
  Copy,
  Download,
  AlertTriangle,
  Search,
  X,
} from 'lucide-react';

export function OperationSessionOverlay() {
  const activeSession = useOperationSessionStore((state) => state.activeSession);
  const cancelSession = useOperationSessionStore((state) => state.cancelSession);
  const clearSession = useOperationSessionStore((state) => state.clearSession);

  const [searchQuery, setSearchQuery] = useState('');
  const [copied, setCopied] = useState(false);
  const [elapsedTime, setElapsedTime] = useState(0);

  const logsEndRef = useRef<HTMLDivElement>(null);

  // Auto-scroll to bottom of logs on change
  useEffect(() => {
    if (logsEndRef.current) {
      logsEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [activeSession?.logs]);

  // Timer calculation
  useEffect(() => {
    if (!activeSession) return;

    if (
      activeSession.status !== 'running' &&
      activeSession.status !== 'starting' &&
      activeSession.status !== 'preflightPending'
    ) {
      if (activeSession.completedAt && activeSession.startedAt) {
        setElapsedTime(
          Math.round(
            (new Date(activeSession.completedAt).getTime() -
              new Date(activeSession.startedAt).getTime()) /
              1000,
          ),
        );
      }
      return;
    }

    const interval = setInterval(() => {
      const elapsed = Math.round((Date.now() - new Date(activeSession.startedAt).getTime()) / 1000);
      setElapsedTime(elapsed > 0 ? elapsed : 0);
    }, 1000);

    return () => clearInterval(interval);
  }, [activeSession]);

  const filteredLogs = useMemo(() => {
    if (!activeSession) return [];
    if (!searchQuery) return activeSession.logs;
    return activeSession.logs.filter(
      (log) =>
        log.message.toLowerCase().includes(searchQuery.toLowerCase()) ||
        log.level.toLowerCase().includes(searchQuery.toLowerCase()),
    );
  }, [activeSession, searchQuery]);

  if (!activeSession) return null;

  const handleCopyLogs = () => {
    const text = activeSession.logs
      .map(
        (log) =>
          `[${new Date(log.timestamp).toLocaleTimeString()}] [${log.level.toUpperCase()}] ${log.message}`,
      )
      .join('\n');
    navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleDownloadLogs = () => {
    const text = activeSession.logs
      .map(
        (log) =>
          `[${new Date(log.timestamp).toLocaleTimeString()}] [${log.level.toUpperCase()}] ${log.message}`,
      )
      .join('\n');
    const blob = new Blob([text], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `session-${activeSession.operationType}-${activeSession.sessionId}.log`;
    link.click();
    URL.revokeObjectURL(url);
  };

  const isTerminal =
    activeSession.status === 'completed' ||
    activeSession.status === 'failed' ||
    activeSession.status === 'cancelled' ||
    activeSession.status === 'preflightFailed';

  const formatTime = (secs: number) => {
    const m = Math.floor(secs / 60)
      .toString()
      .padStart(2, '0');
    const s = (secs % 60).toString().padStart(2, '0');
    return `${m}:${s}`;
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-md p-4 md:p-8 animate-in fade-in duration-300">
      {/* Background radial highlight */}
      <div className="absolute inset-0 z-0 overflow-hidden pointer-events-none">
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[60%] h-[60%] rounded-full bg-purple-500/10 blur-[120px] pointer-events-none" />
      </div>

      <div className="relative z-10 w-full max-w-6xl h-[85vh] flex flex-col rounded-2xl bg-white/[0.03] border border-white/10 backdrop-blur-xl shadow-2xl overflow-hidden animate-in zoom-in-95 duration-300">
        {/* Header */}
        <div className="p-6 border-b border-white/10 flex flex-col md:flex-row md:items-center justify-between gap-4 bg-white/[0.02]">
          <div className="flex items-center gap-3">
            <div className="p-3 bg-purple-500/10 rounded-lg text-purple-400">
              <Terminal className="w-6 h-6 animate-pulse" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-xl font-bold text-white tracking-wide">
                  {typeof activeSession.operationType === 'string'
                    ? activeSession.operationType.replace(/([A-Z])/g, ' $1').trim()
                    : (activeSession.operationType as any).customCommand}
                </h3>
                <span className="text-xs text-gray-500 font-mono select-all">
                  ({activeSession.sessionId})
                </span>
              </div>
              <p className="text-sm text-gray-400 mt-0.5">
                Device:{' '}
                <span className="text-gray-300 font-medium">
                  {activeSession.deviceSnapshotAtStart.model}
                </span>
                {activeSession.deviceSnapshotAtStart.serial && (
                  <span className="text-gray-500 font-mono text-xs ml-2 select-all">
                    [{activeSession.deviceSnapshotAtStart.serial}]
                  </span>
                )}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-4">
            <div className="flex flex-col items-end">
              <span className="text-xs text-gray-400 uppercase tracking-wider">Duration</span>
              <span className="text-lg font-mono font-bold text-white">
                {formatTime(elapsedTime)}
              </span>
            </div>

            <div className="h-8 w-[1px] bg-white/10" />

            <div className="flex items-center gap-2">
              <span className="text-xs text-gray-400 uppercase tracking-wider mr-1">Status</span>
              <span
                className={`px-3 py-1 rounded-full text-xs font-semibold uppercase tracking-wider ${
                  activeSession.status === 'completed'
                    ? 'bg-green-500/20 text-green-400 border border-green-500/30'
                    : activeSession.status === 'failed' ||
                        activeSession.status === 'preflightFailed'
                      ? 'bg-red-500/20 text-red-400 border border-red-500/30'
                      : activeSession.status === 'cancelled'
                        ? 'bg-yellow-500/20 text-yellow-400 border border-yellow-500/30'
                        : 'bg-blue-500/20 text-blue-400 border border-blue-500/30 animate-pulse'
                }`}
              >
                {activeSession.status}
              </span>
            </div>
          </div>
        </div>

        {/* Workspace Panels */}
        <div className="flex-1 flex flex-col md:flex-row overflow-hidden min-h-0">
          {/* Left Panel: Steps */}
          <div className="w-full md:w-96 border-r border-white/10 p-6 overflow-y-auto flex flex-col gap-6 bg-white/[0.01]">
            <h4 className="text-sm font-bold uppercase tracking-wider text-gray-400">
              Progress Steps
            </h4>

            <div className="flex flex-col gap-4 relative">
              {activeSession.steps.map((step, idx) => {
                const isRunning = step.status === 'running';
                const isDone = step.status === 'done';
                const isFailed = step.status === 'failed';

                return (
                  <div key={step.id} className="flex gap-4 relative z-10">
                    {/* Stepper line */}
                    {idx < activeSession.steps.length - 1 && (
                      <div
                        className={`absolute top-8 left-4 w-[2px] h-[calc(100%)] ${
                          isDone ? 'bg-green-500/40' : 'bg-white/10'
                        }`}
                      />
                    )}

                    <div className="flex-shrink-0 relative">
                      {isDone ? (
                        <div className="p-1 bg-green-500/20 rounded-full text-green-400 border border-green-500/40">
                          <CheckCircle2 className="w-6 h-6" />
                        </div>
                      ) : isFailed ? (
                        <div className="p-1 bg-red-500/20 rounded-full text-red-400 border border-red-500/40">
                          <XCircle className="w-6 h-6 animate-shake" />
                        </div>
                      ) : isRunning ? (
                        <div className="p-1 bg-blue-500/20 rounded-full text-blue-400 border border-blue-500/40 animate-spin">
                          <Loader2 className="w-6 h-6" />
                        </div>
                      ) : (
                        <div className="p-1 bg-white/5 rounded-full text-gray-500 border border-white/10">
                          <Circle className="w-6 h-6" />
                        </div>
                      )}
                    </div>

                    <div className="flex-1 pt-1 min-h-[50px]">
                      <h5
                        className={`font-semibold text-sm ${
                          isRunning
                            ? 'text-blue-400'
                            : isDone
                              ? 'text-green-400'
                              : isFailed
                                ? 'text-red-400'
                                : 'text-gray-400'
                        }`}
                      >
                        {step.label}
                      </h5>
                      <p className="text-xs text-gray-500 mt-1 leading-relaxed">{step.detail}</p>
                    </div>
                  </div>
                );
              })}
            </div>

            {/* Error Message Box if failed */}
            {(activeSession.errorMessage || activeSession.errorCode) && (
              <div className="mt-auto p-4 rounded-xl bg-red-500/10 border border-red-500/20 flex gap-3 text-red-400 animate-in fade-in duration-300">
                <AlertTriangle className="w-5 h-5 flex-shrink-0 mt-0.5" />
                <div className="text-xs flex-col flex gap-1">
                  <span className="font-bold uppercase tracking-wide">
                    Error: {activeSession.errorCode || 'UNKNOWN_ERROR'}
                  </span>
                  <p className="leading-relaxed text-gray-300">{activeSession.errorMessage}</p>
                </div>
              </div>
            )}
          </div>

          {/* Right Panel: Terminal Logs */}
          <div className="flex-1 flex flex-col overflow-hidden min-w-0">
            {/* Terminal Actions */}
            <div className="px-6 py-4 border-b border-white/10 flex items-center justify-between gap-4 bg-white/[0.01]">
              <div className="relative flex-1 max-w-sm">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500" />
                <input
                  type="text"
                  placeholder="Filter logs..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="w-full pl-9 pr-4 py-1.5 rounded-lg bg-white/5 border border-white/10 text-sm placeholder-gray-500 focus:outline-none focus:border-purple-500/50 focus:ring-1 focus:ring-purple-500/30 text-white"
                />
                {searchQuery && (
                  <button
                    onClick={() => setSearchQuery('')}
                    className="absolute right-3 top-1/2 -translate-y-1/2 hover:text-white text-gray-500"
                  >
                    <X className="w-4 h-4" />
                  </button>
                )}
              </div>

              <div className="flex items-center gap-2">
                <button
                  onClick={handleCopyLogs}
                  className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-white/5 hover:bg-white/10 border border-white/10 text-xs font-semibold text-gray-300 hover:text-white transition"
                  title="Copy logs to clipboard"
                >
                  <Copy className="w-3.5 h-3.5" />
                  {copied ? 'Copied!' : 'Copy'}
                </button>
                <button
                  onClick={handleDownloadLogs}
                  className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-white/5 hover:bg-white/10 border border-white/10 text-xs font-semibold text-gray-300 hover:text-white transition"
                  title="Download logs"
                >
                  <Download className="w-3.5 h-3.5" />
                  Export
                </button>
              </div>
            </div>

            {/* Terminal Box */}
            <div className="flex-1 overflow-y-auto p-6 bg-[#04060C]/90 font-mono text-xs leading-relaxed flex flex-col gap-1.5 shadow-[inset_0_2px_15px_rgba(0,0,0,0.8)]">
              {filteredLogs.length === 0 ? (
                <div className="text-gray-600 italic">No matching log entries found.</div>
              ) : (
                filteredLogs.map((log, idx) => (
                  <div key={idx} className="flex gap-3">
                    <span className="text-gray-600 select-none">
                      [{new Date(log.timestamp).toLocaleTimeString()}]
                    </span>
                    <span
                      className={`${
                        log.level === 'error'
                          ? 'text-red-400 font-bold'
                          : log.level === 'warn'
                            ? 'text-yellow-400'
                            : log.level === 'success'
                              ? 'text-green-400'
                              : log.level === 'debug'
                                ? 'text-blue-400/80'
                                : 'text-gray-300'
                      }`}
                    >
                      {log.message}
                    </span>
                  </div>
                ))
              )}
              <div ref={logsEndRef} />
            </div>
          </div>
        </div>

        {/* Footer actions */}
        <div className="p-6 border-t border-white/10 bg-white/[0.02] flex justify-end gap-3">
          {activeSession.canCancel && (
            <button
              onClick={cancelSession}
              className="flex items-center gap-2 px-5 py-2.5 rounded-xl bg-red-500/10 hover:bg-red-500/20 border border-red-500/30 text-sm font-semibold text-red-400 transition cursor-pointer"
            >
              Cancel Operation
            </button>
          )}

          {isTerminal && (
            <button
              onClick={clearSession}
              className="flex items-center gap-2 px-6 py-2.5 rounded-xl bg-purple-600 hover:bg-purple-700 text-sm font-semibold text-white transition shadow-lg shadow-purple-600/20 cursor-pointer"
            >
              Close Panel
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
