import React, { useState, useEffect } from 'react';
import { invoke } from '@tauri-apps/api/core';
import { listen } from '@tauri-apps/api/event';
import { open } from '@tauri-apps/plugin-dialog';

interface DfuState {
  mode: 'Normal' | 'Recovery' | 'DFU' | 'Restore' | 'Unknown';
  ecid?: string;
}

export const DfuRestore: React.FC = () => {
  const [state, setState] = useState<DfuState>({ mode: 'Unknown' });
  const [, setStatus] = useState("Idle");
  const [logs, setLogs] = useState<string[]>([]);
  const [udid] = useState("");
  const [ipswPath, setIpswPath] = useState<string | null>(null);

  useEffect(() => {
    const unlistenProgress = listen<string>('dfu-progress', (event) => {
      setLogs((prev) => [...prev.slice(-49), `[PROGRESS] ${event.payload}`]);
    });
    
    const unlistenError = listen<string>('dfu-error', (event) => {
      setLogs((prev) => [...prev.slice(-49), `[ERROR] ${event.payload}`]);
    });

    const unlistenStep = listen<string>('dfu-step', (event) => {
        setStatus(`Phase: ${event.payload}`);
    });

    return () => {
      unlistenProgress.then(f => f());
      unlistenError.then(f => f());
      unlistenStep.then(f => f());
    };
  }, []);

  const detectMode = async () => {
    try {
      const res = await invoke<DfuState>('ios_detect_dfu_state');
      setState(res);
      setLogs(prev => [...prev, `[INIT] Mode detected: ${res.mode}`]);
    } catch (e) {
      setLogs(prev => [...prev, `[ERROR] ${e}`]);
    }
  };

  const selectIpsw = async () => {
    const selected = await open({
      multiple: false,
      filters: [{ name: "IPSW Firmware", extensions: ["ipsw"] }],
    });
    if (selected) {
      const path = typeof selected === "string" ? selected : selected;
      setIpswPath(path as string);
      setLogs(prev => [...prev, `[IPSW] Selected: ${path}`]);
    }
  };

  const handleRestore = async () => {
    if (!ipswPath) {
      setLogs(prev => [...prev, "[ERROR] No IPSW file selected. Use SELECT IPSW button first."]);
      return;
    }
    setStatus("Restoring...");
    try {
      await invoke('ios_restore_device', { udid, ipswPath });
    } catch (e) {
      setLogs(prev => [...prev, `[FATAL] ${e}`]);
    }
  };

    return (
    <div className="p-4 sm:p-6 bg-black/40 backdrop-blur-xl rounded-2xl border border-white/10 text-white font-mono text-xs">
      <div className="flex flex-wrap justify-between items-center gap-3 mb-6">
        <h2 className="text-xl font-bold tracking-tighter text-blue-400">DFU+IPSW RESTORE ENGINE</h2>
        <div className={`px-3 py-1 rounded-full border ${state.mode === 'DFU' ? 'border-green-500 text-green-400' : 'border-white/20'}`}>
          MODE: {state.mode}
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <button 
          onClick={detectMode}
          className="p-4 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl transition-all"
        >
          SCAN DEVICE STATE
        </button>
        <button
          onClick={selectIpsw}
          className="p-4 bg-blue-500/10 hover:bg-blue-500/20 border border-blue-500/20 rounded-xl transition-all text-blue-400"
        >
          {ipswPath ? "✅ IPSW SELECTED" : "SELECT IPSW FILE"}
        </button>
        <button 
          onClick={() => invoke('ios_enter_dfu', { udid })}
          className="p-4 bg-red-500/10 hover:bg-red-500/20 border border-red-500/20 rounded-xl transition-all text-red-400"
        >
          FORCE RECOVERY MODE
        </button>
      </div>

      {ipswPath && (
        <div className="mb-4 p-3 bg-blue-500/5 border border-blue-500/10 rounded-xl text-blue-300/70 truncate">
          📦 {ipswPath}
        </div>
      )}

      <div className="bg-black/60 rounded-xl p-4 min-h-64 max-h-80 overflow-y-auto border border-white/5 space-y-1">
        {logs.map((log, i) => (
          <div key={i} className={log.includes('ERROR') ? 'text-red-400' : 'text-gray-400'}>
            {log}
          </div>
        ))}
      </div>

      <div className="mt-6">
        <button 
          onClick={handleRestore}
          disabled={state.mode === 'Unknown' || !ipswPath}
          className="w-full py-4 bg-blue-600 hover:bg-blue-500 disabled:opacity-50 rounded-xl font-bold transition-all shadow-lg shadow-blue-900/40"
        >
          INITIATE FULL ERASE + RESTORE
        </button>
      </div>
    </div>
  );
};
