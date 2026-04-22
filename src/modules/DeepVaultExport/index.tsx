import React, { useState } from 'react';
import { invoke } from '@tauri-apps/api/core';
import { open } from '@tauri-apps/plugin-dialog';

interface VaultResult {
    success: boolean;
    vault_path?: string;
    error?: string;
}

export const DeepVaultExport: React.FC = () => {
    const [udid, setUdid] = useState("");
    const [status, setStatus] = useState("Idle");
    const [logs, setLogs] = useState<string[]>([]);
    const [selectedFiles, setSelectedFiles] = useState<string[]>([]);

    const selectArtifacts = async () => {
        const selected = await open({
            multiple: true,
            filters: [
                { name: "All Forensic Files", extensions: ["plist", "db", "sqlite", "json", "bin", "img", "tar", "gz", "zip"] },
            ],
        });
        if (selected) {
            const paths = Array.isArray(selected) ? selected : [selected];
            setSelectedFiles(paths as string[]);
            setLogs(prev => [...prev, `[FILES] ${paths.length} artifact(s) selected for vault packaging`]);
        }
    };

    const createVault = async () => {
        if (selectedFiles.length === 0) {
            setLogs(prev => [...prev, "[ERROR] No artifacts selected. Use SELECT ARTIFACTS button first."]);
            return;
        }
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
                files: selectedFiles
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

                <button
                    onClick={selectArtifacts}
                    className="w-full p-4 bg-blue-500/10 hover:bg-blue-500/20 border border-blue-500/20 rounded-xl transition-all text-blue-400 font-bold"
                >
                    {selectedFiles.length > 0
                        ? `✅ ${selectedFiles.length} ARTIFACT(S) SELECTED`
                        : "SELECT FORENSIC ARTIFACTS"}
                </button>

                {selectedFiles.length > 0 && (
                    <div className="p-3 bg-blue-500/5 border border-blue-500/10 rounded-xl text-blue-200/60 max-h-24 overflow-y-auto space-y-1">
                        {selectedFiles.map((f, i) => (
                            <div key={i} className="truncate">📄 {f.split('/').pop()}</div>
                        ))}
                    </div>
                )}
                
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
                disabled={!udid || status.includes("Compiling") || selectedFiles.length === 0}
                className="w-full py-4 bg-blue-600 hover:bg-blue-500 disabled:opacity-50 rounded-xl font-bold transition-all shadow-lg shadow-blue-900/40 uppercase tracking-widest"
            >
                COMPILE UNIFIED FORENSIC VAULT
            </button>
        </div>
    );
};

