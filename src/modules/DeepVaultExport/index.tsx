import React, { useState } from 'react';
import { invoke } from '@tauri-apps/api/core';

interface VaultResult {
    success: boolean;
    vault_path?: string;
    error?: string;
}

export const DeepVaultExport: React.FC = () => {
    const [udid, setUdid] = useState("");
    const [status, setStatus] = useState("Idle");
    const [logs, setLogs] = useState<string[]>([]);

    const createVault = async () => {
        setStatus("Compiling...");
        setLogs(prev => [...prev, "[INIT] Collecting forensic artifacts from volatile memory..."]);
        
        try {
            const request = {
                dir: "/Users/enayat/Documents/DeepEyeUnlocker/exports",
                meta: {
                    udid: udid,
                    investigator: "DeepEye_Lead",
                    case_id: "DF-2026-X"
                },
                files: [] // placeholder for actual extracted files
            };
            
            const res = await invoke<VaultResult>('ios_create_deepvault', { request });
            if (res.success) {
                setLogs(prev => [...prev, `[SUCCESS] Vault created at: ${res.vault_path}`]);
                setStatus("Complete");
            } else {
                setLogs(prev => [...prev, `[ERROR] ${res.error}`]);
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
                <h2 className="text-xl font-bold tracking-tighter text-blue-400">DEEPVAULT V2 FORENSIC EXPORT</h2>
            </div>

            <div className="space-y-4 mb-6">
                <input 
                    type="text" 
                    placeholder="Reference UDID"
                    value={udid}
                    onChange={(e) => setUdid(e.target.value)}
                    className="w-full bg-white/5 border border-white/10 p-4 rounded-xl outline-none focus:border-blue-500 transition-all font-mono"
                />
                
                <div className="p-4 bg-blue-500/5 border border-blue-500/10 rounded-xl text-blue-200/60 leading-relaxed">
                    Generating a DeepVault v2 package will encrypt all extracted hashes, activation records, 
                    and device snapshots into a single (.deepvault) archive for cloud synchronization.
                </div>
            </div>

            <div className="bg-black/60 rounded-xl p-4 h-32 overflow-y-auto border border-white/5 space-y-1 mb-6">
                {logs.map((log, i) => (
                    <div key={i} className={log.includes('ERROR') ? 'text-red-400' : 'text-gray-400'}>
                        {log}
                    </div>
                ))}
            </div>

            <button 
                onClick={createVault}
                disabled={!udid || status.includes("Compiling")}
                className="w-full py-4 bg-blue-600 hover:bg-blue-500 disabled:opacity-50 rounded-xl font-bold transition-all shadow-lg shadow-blue-900/40 uppercase tracking-widest"
            >
                COMPILE UNIFIED FORENSIC VAULT
            </button>
        </div>
    );
};
