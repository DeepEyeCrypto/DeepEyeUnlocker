import React, { useState, useEffect, useRef } from 'react';
import { invoke } from '@tauri-apps/api/core';
import { listen } from '@tauri-apps/api/event';

const AdbTerminal: React.FC = () => {
  const [logs, setLogs] = useState<string[]>([]);
  const [isStreaming, setIsStreaming] = useState(false);
  const [deviceSerial, setDeviceSerial] = useState('');
  const logEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const unlistenLog = listen<string>('adb-log-line', (event) => {
      setLogs((prev: string[]) => [...prev.slice(-100), event.payload]);
    });

    const unlistenError = listen<string>('adb-log-error', (event) => {
      setLogs((prev: string[]) => [...prev.slice(-100), `[ERROR] ${event.payload}`]);
    });

    const unlistenTerminated = listen<string>('adb-log-terminated', (event) => {
      setLogs((prev: string[]) => [...prev.slice(-100), `[TERMINATED] ${event.payload}`]);
      setIsStreaming(false);
    });

    return () => {
      unlistenLog.then((u: () => void) => u());
      unlistenError.then((u: () => void) => u());
      unlistenTerminated.then((u: () => void) => u());
    };
  }, []);

  useEffect(() => {
    logEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [logs]);

  const handleStartStream = async () => {
    try {
      setIsStreaming(true);
      setLogs(['[SYSTEM] Initializing ADB Log Stream...']);
      await invoke('stream_adb_logs', { deviceSerial: deviceSerial || null });
    } catch (e: unknown) {
      setLogs((prev: string[]) => [...prev, `[CRITICAL] ${String(e)}`]);
      setIsStreaming(false);
    }
  };

  return (
    <div className="space-y-6">
      <section className="glass rounded-2xl p-6 flex gap-4 items-end border-l-4 border-emerald-500">
        <div className="flex-1 space-y-2">
          <label className="text-[10px] uppercase tracking-wider text-slate-400 font-bold">Target Device Serial (Optional)</label>
          <input 
            type="text" 
            value={deviceSerial}
            onChange={(e) => setDeviceSerial(e.target.value)}
            placeholder="e.g. R58N1234567"
            className="w-full bg-black/40 border border-white/10 rounded-lg px-4 py-3 outline-none focus:border-emerald-500/50 transition-all font-mono text-xs"
          />
        </div>
        <button 
          disabled={isStreaming}
          onClick={handleStartStream}
          className={`${isStreaming ? 'bg-slate-700' : 'bg-emerald-600 hover:bg-emerald-500'} text-white px-8 py-3 rounded-lg font-bold text-sm shadow-[0_0_20px_rgba(16,185,129,0.2)] transition-all`}
        >
          {isStreaming ? 'STREAMING...' : 'START LOGCAT'}
        </button>
      </section>

      <div className="glass rounded-2xl overflow-hidden border border-white/5 bg-black/40">
        <div className="h-8 bg-white/5 flex items-center px-4 gap-2">
          <div className="w-2.5 h-2.5 rounded-full bg-red-500/40"></div>
          <div className="w-2.5 h-2.5 rounded-full bg-amber-500/40"></div>
          <div className="w-2.5 h-2.5 rounded-full bg-emerald-500/40"></div>
          <span className="ml-2 text-[9px] font-bold text-slate-500 tracking-[0.2em] uppercase">System Monitor // adb logcat</span>
        </div>
        
        <div className="h-[500px] overflow-y-auto p-4 font-mono text-[11px] leading-relaxed">
          {logs.length === 0 ? (
            <div className="h-full flex items-center justify-center text-slate-600 uppercase tracking-widest animate-pulse">
              Waiting for log stream to begin...
            </div>
          ) : (
            <>
              {logs.map((log, i) => (
                <div key={i} className={`flex gap-3 ${log.includes('[ERROR]') ? 'text-red-400' : log.includes('[TERMINATED]') ? 'text-amber-500' : 'text-slate-300'}`}>
                  <span className="opacity-30 select-none">{(i + 1).toString().padStart(4, '0')}</span>
                  <span className="break-all">{log}</span>
                </div>
              ))}
              <div ref={logEndRef} />
            </>
          )}
        </div>
      </div>
    </div>
  );
};

export default AdbTerminal;
