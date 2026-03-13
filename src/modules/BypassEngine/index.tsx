import React, { useState } from 'react';
import { invoke } from '@tauri-apps/api/core';

interface HelloState {
    on_hello_screen: boolean;
    raw_state: string;
}

export const BypassEngine: React.FC = () => {
    const [udid, setUdid] = useState("");
    const [state, setState] = useState<HelloState | null>(null);
    const [status, setStatus] = useState("Idle");
    const [logs, setLogs] = useState<string[]>([]);

    const checkState = async () => {
        try {
            setStatus("Querying...");
            const res = await invoke<HelloState>('ios_check_hello_state', { udid });
            setState(res);
            setLogs(prev => [...prev, `[INIT] Target on Hello Screen: ${res.on_hello_screen ? 'YES' : 'NO'}`]);
            setStatus("Ready");
        } catch (e) {
            setLogs(prev => [...prev, `[ERROR] ${e}`]);
            setStatus("Error");
        }
    };

    const runBypass = async () => {
        if (!state) return;
        setStatus("Exploiting...");
        try {
            const success = await invoke<boolean>('ios_run_hello_bypass', { udid });
            if (success) {
                setLogs(prev => [...prev, `[SUCCESS] Bypass signals sent successfully.`]);
                setStatus("Bypassed");
            }
        } catch (e) {
            setLogs(prev => [...prev, `[ERROR] ${e}`]);
            setStatus("Error");
        }
    };

    return (
        <div className="p-6 bg-black/40 backdrop-blur-xl rounded-2xl border border-white/10 text-white font-mono text-xs">
            <div className="flex justify-between items-center mb-6">
                <h2 className="text-xl font-bold tracking-tighter text-red-400">HELLO SCREEN BYPASS ENGINE</h2>
                <div className={`px-3 py-1 rounded-full border ${state?.on_hello_screen ? 'border-red-500 text-red-400' : 'border-green-500 text-green-400'}`}>
                    HELLO STATE: {state ? (state.on_hello_screen ? 'ACTIVE' : 'NONE') : 'UNKNOWN'}
                </div>
            </div>

            <div className="flex gap-2 mb-6">
                <input 
                    type="text" 
                    placeholder="Enter Device UDID"
                    value={udid}
                    onChange={(e) => setUdid(e.target.value)}
                    className="flex-1 bg-white/5 border border-white/10 p-3 rounded-xl outline-none focus:border-red-500 transition-all font-mono"
                />
                <button 
                    onClick={checkState}
                    className="px-6 py-3 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl transition-all font-bold"
                >
                    SCAN
                </button>
            </div>

            <div className="bg-black/60 rounded-xl p-4 h-48 overflow-y-auto border border-white/5 space-y-1 mb-6">
                {logs.map((log, i) => (
                    <div key={i} className={log.includes('ERROR') ? 'text-red-400' : 'text-gray-400'}>
                        {log}
                    </div>
                ))}
            </div>

            <button 
                onClick={runBypass}
                disabled={!state || !state.on_hello_screen || status.includes("Exploiting")}
                className="w-full py-4 bg-red-600 hover:bg-red-500 disabled:opacity-50 rounded-xl font-bold transition-all shadow-lg shadow-red-900/40"
            >
                EXECUTE SIGNAL BYPASS
            </button>
        </div>
    );
};
