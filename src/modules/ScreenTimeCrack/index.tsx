import React, { useState, useEffect } from 'react';
import { invoke } from '@tauri-apps/api/core';
import { listen } from '@tauri-apps/api/event';

interface HashInfo {
    version: string;
    algorithm: string;
    iterations: number;
    salt: string;
    hash: string;
}

export const ScreenTimeCrack: React.FC = () => {
    const [path, setPath] = useState("");
    const [state, setState] = useState<HashInfo | null>(null);
    const [status, setStatus] = useState("Idle");
    const [logs, setLogs] = useState<string[]>([]);

    useEffect(() => {
        const unlistenProgress = listen<string>('screentime-progress', (event) => {
            setLogs((prev: string[]) => [...prev.slice(-49), `[HASHCAT] ${event.payload}`]);
        });

        const unlistenDone = listen<number>('screentime-done', (event) => {
            setStatus("Complete");
            setLogs((prev: string[]) => [...prev, `[INIT] Search finished with code: ${event.payload}`]);
        });

        return () => {
            unlistenProgress.then(f => f());
            unlistenDone.then(f => f());
        };
    }, []);

    const extractHash = async () => {
        try {
            setStatus("Extracting...");
            const res = await invoke<HashInfo>('ios_extract_screentime_hash', { backupPath: path });
            setState(res);
            setLogs(prev => [...prev, `[INIT] Found ${res.version} hash (${res.algorithm})`]);
            setStatus("Ready");
        } catch (e) {
            setLogs(prev => [...prev, `[ERROR] ${e}`]);
            setStatus("Error");
        }
    };

    const startCrack = async () => {
        if (!state) return;
        setStatus("Cracking...");
        try {
            await invoke('ios_run_screentime_crack', { 
                backupPath: path,
                wordlist: "/usr/local/share/wordlists/rockyou.txt",
                rules: "/usr/local/share/hashcat/rules/best64.rule"
            });
        } catch (e) {
            setLogs(prev => [...prev, `[ERROR] ${e}`]);
        }
    };

    return (
        <div className="p-6 bg-black/40 backdrop-blur-xl rounded-2xl border border-white/10 text-white font-mono text-xs">
            <div className="flex justify-between items-center mb-6">
                <h2 className="text-xl font-bold tracking-tighter text-amber-400">SCREEN TIME SECURITY ANALYTICS</h2>
                <div className={`px-3 py-1 rounded-full border border-white/20 uppercase`}>
                    STATUS: {status}
                </div>
            </div>

            <div className="flex gap-2 mb-6">
                <input 
                    type="text" 
                    placeholder="Decrypted Backup Path"
                    value={path}
                    onChange={(e) => setPath(e.target.value)}
                    className="flex-1 bg-white/5 border border-white/10 p-3 rounded-xl outline-none focus:border-amber-500 transition-all font-mono"
                />
                <button 
                    onClick={extractHash}
                    className="px-6 py-3 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl transition-all font-bold"
                >
                    SCAN
                </button>
            </div>

            {state && (
                <div className="bg-white/5 border border-white/10 rounded-xl p-4 mb-6 space-y-2 overflow-x-hidden">
                    <div className="flex justify-between">
                        <span className="text-gray-500">VERSION</span>
                        <span>{state.version}</span>
                    </div>
                    <div className="flex justify-between">
                        <span className="text-gray-500">SALT (HEX)</span>
                        <span className="text-[10px] break-all ml-4 text-right">{state.salt}</span>
                    </div>
                    <div className="flex justify-between">
                        <span className="text-gray-500">HASH</span>
                        <span className="text-[10px] break-all ml-4 text-right">{state.hash}</span>
                    </div>
                </div>
            )}

            <div className="bg-black/60 rounded-xl p-4 min-h-48 max-h-72 overflow-y-auto border border-white/5 space-y-1 mb-6">
                {logs.map((log, i) => (
                    <div key={i} className={log.includes('ERROR') ? 'text-red-400' : 'text-gray-400'}>
                        {log}
                    </div>
                ))}
            </div>

            <button 
                onClick={startCrack}
                disabled={!state || status.includes("Cracking")}
                className="w-full py-4 bg-amber-600 hover:bg-amber-500 disabled:opacity-50 rounded-xl font-bold transition-all shadow-lg shadow-amber-900/40"
            >
                START EXHAUSTIVE PIN SEARCH
            </button>
        </div>
    );
};
