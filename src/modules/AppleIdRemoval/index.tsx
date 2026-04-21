import React, { useState } from 'react';
import { invoke } from '@tauri-apps/api/core';

interface AppleIdState {
    fmi_on: boolean;
    apple_id_bound: boolean;
    ios_version: string;
    model: string;
}

interface RemovalResult {
    success: boolean;
    message?: string;
    error?: string;
}

export const AppleIdRemoval: React.FC = () => {
    const [state, setState] = useState<AppleIdState | null>(null);
    const [status, setStatus] = useState("Idle");
    const [logs, setLogs] = useState<string[]>([]);
    const [udid, setUdid] = useState("");

    const checkState = async () => {
        try {
            setStatus("Auditing...");
            const res = await invoke<AppleIdState>('ios_apple_id_state', { udid });
            setState(res);
            setLogs(prev => [...prev, `[AUDIT] Model: ${res.model}, iOS: ${res.ios_version}, FMI: ${res.fmi_on ? 'ON' : 'OFF'}`]);
            setStatus("Ready");
        } catch (e) {
            setLogs(prev => [...prev, `[ERROR] ${e}`]);
            setStatus("Error");
        }
    };

    const removeAppleId = async () => {
        if (!state) return;
        if (state.fmi_on && parseFloat(state.ios_version) >= 11.4) {
            setLogs(prev => [...prev, `[CRITICAL] FMI is ON and iOS is >= 11.4. Direct removal is NOT possible. Use DFU Restore module.`]);
            return;
        }

        setStatus("Removing...");
        try {
            const res = await invoke<RemovalResult>('ios_remove_apple_id', { udid });
            if (res.success) {
                setLogs(prev => [...prev, `[SUCCESS] ${res.message}`]);
                setStatus("Removed");
            } else {
                setLogs(prev => [...prev, `[FAILED] ${res.error}`]);
                setStatus("Failed");
            }
        } catch (e) {
            setLogs(prev => [...prev, `[ERROR] ${e}`]);
            setStatus("Error");
        }
    };

    return (
        <div className="p-6 bg-black/40 backdrop-blur-xl rounded-2xl border border-white/10 text-white font-mono text-xs">
            <div className="flex justify-between items-center mb-6">
                <h2 className="text-xl font-bold tracking-tighter text-green-400">APPLE ID BINDING AUDIT</h2>
                <div className={`px-3 py-1 rounded-full border ${state?.apple_id_bound ? 'border-yellow-500 text-yellow-400' : 'border-green-500 text-green-400'}`}>
                    BINDING: {state ? (state.apple_id_bound ? 'ACTIVE' : 'NONE') : 'UNKNOWN'}
                </div>
            </div>

            <div className="flex gap-2 mb-6">
                <input 
                    type="text" 
                    placeholder="Enter Device UDID"
                    value={udid}
                    onChange={(e) => setUdid(e.target.value)}
                    className="flex-1 bg-white/5 border border-white/10 p-3 rounded-xl outline-none focus:border-green-500 transition-all font-mono"
                />
                <button 
                    onClick={checkState}
                    className="px-6 py-3 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl transition-all font-bold"
                >
                    SCAN
                </button>
            </div>

            {state && (
                <div className="space-y-4 mb-6">
                    <div className="p-4 bg-white/5 border border-white/10 rounded-xl flex justify-between items-center">
                        <div>
                            <div className="text-[10px] text-gray-500 uppercase">FMI State</div>
                            <div className={`text-sm font-bold ${state.fmi_on ? 'text-red-400' : 'text-green-400'}`}>
                                {state.fmi_on ? 'ENABLED (FIND MY ON)' : 'DISABLED'}
                            </div>
                        </div>
                        <div className="text-right">
                            <div className="text-[10px] text-gray-500 uppercase">iOS Version</div>
                            <div className="text-sm font-bold">{state.ios_version}</div>
                        </div>
                    </div>
                </div>
            )}

            <div className="bg-black/60 rounded-xl p-4 min-h-48 max-h-72 overflow-y-auto border border-white/5 space-y-1 mb-6">
                {logs.map((log, i) => (
                    <div key={i} className={log.includes('ERROR') || log.includes('CRITICAL') ? 'text-red-400' : 'text-gray-400'}>
                        {log}
                    </div>
                ))}
            </div>

            <button 
                onClick={removeAppleId}
                disabled={!state || status.includes("Removing") || status === "Removed"}
                className="w-full py-4 bg-green-600 hover:bg-green-500 disabled:opacity-50 rounded-xl font-bold transition-all shadow-lg shadow-green-900/40"
            >
                PROCESS REMOVAL (OWNED DEVICE ONLY)
            </button>
        </div>
    );
};
