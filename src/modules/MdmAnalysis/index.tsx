import React, { useState } from 'react';
import { invoke } from '@tauri-apps/api/core';

interface MdmState {
    enrolled: boolean;
    org_name?: string;
    server_url?: string;
    restrictions: string[];
    removal_path: string;
}

export const MdmAnalysis: React.FC = () => {
    const [udid, setUdid] = useState("");
    const [state, setState] = useState<MdmState | null>(null);
    const [status, setStatus] = useState("Idle");
    const [logs, setLogs] = useState<string[]>([]);

    const checkMdm = async () => {
        try {
            setStatus("Querying...");
            const res = await invoke<MdmState>('ios_mdm_state', { udid });
            setState(res);
            setLogs(prev => [...prev, `[INIT] MDM Status: ${res.enrolled ? 'ENROLLED' : 'CLEAN'}`]);
            if (res.org_name) setLogs(prev => [...prev, `[INIT] Org: ${res.org_name}`]);
            setStatus("Ready");
        } catch (e) {
            setLogs(prev => [...prev, `[ERROR] ${e}`]);
            setStatus("Error");
        }
    };

    const removeMdm = async () => {
        if (!state) return;
        setStatus("Removing...");
        try {
            const success = await invoke<boolean>('ios_remove_mdm', { udid });
            if (success) {
                setLogs(prev => [...prev, `[SUCCESS] MDM profile removal triggered.`]);
                setStatus("Complete");
            }
        } catch (e) {
            setLogs(prev => [...prev, `[ERROR] ${e}`]);
            setStatus("Error");
        }
    };

    return (
        <div className="p-6 bg-black/40 backdrop-blur-xl rounded-2xl border border-white/10 text-white font-mono text-xs">
            <div className="flex justify-between items-center mb-6">
                <h2 className="text-xl font-bold tracking-tighter text-cyan-400">MDM POLICY & PROFILE AUDIT</h2>
                <div className={`px-3 py-1 rounded-full border ${state?.enrolled ? 'border-red-500 text-red-400' : 'border-green-500 text-green-400'}`}>
                    ENROLLMENT: {state ? (state.enrolled ? 'ACTIVE' : 'NONE') : 'UNKNOWN'}
                </div>
            </div>

            <div className="flex gap-2 mb-6">
                <input 
                    type="text" 
                    placeholder="Enter Device UDID"
                    value={udid}
                    onChange={(e) => setUdid(e.target.value)}
                    className="flex-1 bg-white/5 border border-white/10 p-3 rounded-xl outline-none focus:border-cyan-500 transition-all font-mono"
                />
                <button 
                    onClick={checkMdm}
                    className="px-6 py-3 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl transition-all font-bold"
                >
                    AUDIT
                </button>
            </div>

            {state && (
                <div className="grid grid-cols-2 gap-4 mb-6">
                    <div className="p-4 bg-white/5 border border-white/10 rounded-xl">
                        <div className="text-[10px] text-gray-500 uppercase mb-1">Organization</div>
                        <div className="text-sm font-bold truncate">{state.org_name || 'N/A'}</div>
                    </div>
                    <div className="p-4 bg-white/5 border border-white/10 rounded-xl">
                        <div className="text-[10px] text-gray-500 uppercase mb-1">Logic Path</div>
                        <div className="text-sm font-bold text-cyan-300">{state.removal_path}</div>
                    </div>
                </div>
            )}

            <div className="bg-black/60 rounded-xl p-4 h-48 overflow-y-auto border border-white/5 space-y-1 mb-6">
                {logs.map((log, i) => (
                    <div key={i} className={log.includes('ERROR') ? 'text-red-400' : 'text-gray-400'}>
                        {log}
                    </div>
                ))}
            </div>

            <button 
                onClick={removeMdm}
                disabled={!state || !state.enrolled || status.includes("Removing")}
                className="w-full py-4 bg-cyan-600 hover:bg-cyan-500 disabled:opacity-50 rounded-xl font-bold transition-all shadow-lg shadow-cyan-900/40"
            >
                REMOVE ENROLLMENT PROFILE
            </button>
        </div>
    );
};
