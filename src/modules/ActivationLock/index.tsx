import React, { useState, useEffect } from 'react';
import { invoke } from '@tauri-apps/api/core';
import { listen } from '@tauri-apps/api/event';

interface ActivationState {
    locked: bool,
    fmi_enabled: bool,
    apple_id_bound: string | null,
    removal_path: 'None' | 'DfuRestore' | 'DirectFmiOff' | 'Checkra1n' | 'A12Ramdisk',
    model: string,
    chip: string,
}

export const ActivationLock: React.FC = () => {
    const [state, setState] = useState<ActivationState | null>(null);
    const [status, setStatus] = useState("Idle");
    const [logs, setLogs] = useState<string[]>([]);
    const [udid, setUdid] = useState("");

    useEffect(() => {
        const unlistenProgress = listen<string>('checkra1n-progress', (event) => {
            setLogs((prev: string[]) => [...prev.slice(-49), `[JAILBREAK] ${event.payload}`]);
        });

        const unlistenError = listen<string>('activation-error', (event) => {
            setLogs((prev: string[]) => [...prev.slice(-49), `[ERROR] ${event.payload}`]);
        });

        const unlistenComplete = listen<number | null>('activation-complete', (event) => {
            setStatus("Process Terminated");
            setLogs((prev: string[]) => [...prev, `[INIT] Task finished with code: ${event.payload}`]);
        });

        return () => {
            unlistenProgress.then((f) => f());
            unlistenError.then((f) => f());
            unlistenComplete.then((f) => f());
        };
    }, []);

    const checkState = async () => {
        try {
            setStatus("Checking...");
            const res = await invoke<ActivationState>('ios_check_activation_state', { udid });
            setState(res);
            setLogs((prev: string[]) => [...prev, `[INIT] Found ${res.model} (${res.chip}). FMI: ${res.fmi_enabled ? 'ON' : 'OFF'}`]);
            setStatus("Ready");
        } catch (e) {
            setLogs((prev: string[]) => [...prev, `[ERROR] ${e}`]);
            setStatus("Error");
        }
    };

    const runBypass = async () => {
        if (!state) return;
        setStatus("Running Bypass...");
        try {
            if (state.removal_path === 'Checkra1n') {
                await invoke('ios_run_checkra1n', { udid });
            } else {
                setLogs((prev: string[]) => [...prev, `[WARN] Automated bypass not available for this chip/path.`]);
            }
        } catch (e) {
            setLogs((prev: string[]) => [...prev, `[ERROR] ${e}`]);
        }
    };

    return (
        <div className="p-6 bg-black/40 backdrop-blur-xl rounded-2xl border border-white/10 text-white font-mono text-xs">
            <div className="flex justify-between items-center mb-6">
                <h2 className="text-xl font-bold tracking-tighter text-purple-400">ACTIVATION LOCK ANALYSIS</h2>
                <div className={`px-3 py-1 rounded-full border ${state?.locked ? 'border-red-500 text-red-400' : 'border-green-500 text-green-400'}`}>
                    STATE: {state ? (state.locked ? 'LOCKED' : 'UNLOCKED') : 'UNKNOWN'}
                </div>
            </div>

            <div className="flex gap-2 mb-6">
                <input 
                    type="text" 
                    placeholder="Enter Device UDID"
                    value={udid}
                    onChange={(e) => setUdid(e.target.value)}
                    className="flex-1 bg-white/5 border border-white/10 p-3 rounded-xl outline-none focus:border-purple-500 transition-all font-mono"
                />
                <button 
                    onClick={checkState}
                    className="px-6 py-3 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl transition-all font-bold"
                >
                    AUDIT
                </button>
            </div>

            {state && (
                <div className="grid grid-cols-2 gap-4 mb-6">
                    <div className="p-4 bg-white/5 border border-white/10 rounded-xl">
                        <div className="text-[10px] text-gray-500 uppercase mb-1">Detected Chip</div>
                        <div className="text-sm font-bold">{state.chip}</div>
                    </div>
                    <div className="p-4 bg-white/5 border border-white/10 rounded-xl">
                        <div className="text-[10px] text-gray-500 uppercase mb-1">Removal Path</div>
                        <div className="text-sm font-bold text-purple-300">{state.removal_path}</div>
                    </div>
                </div>
            )}

            <div className="bg-black/60 rounded-xl p-4 h-64 overflow-y-auto border border-white/5 space-y-1 mb-6">
                {logs.map((log, i) => (
                    <div key={i} className={log.includes('ERROR') ? 'text-red-400' : 'text-gray-400'}>
                        {log}
                    </div>
                ))}
            </div>

            <button 
                onClick={runBypass}
                disabled={!state || state.removal_path === 'None' || status.includes("Running")}
                className="w-full py-4 bg-purple-600 hover:bg-purple-500 disabled:opacity-50 rounded-xl font-bold transition-all shadow-lg shadow-purple-900/40"
            >
                RUN DIRECT BYPASShandshake (A7-A11)
            </button>
        </div>
    );
};
