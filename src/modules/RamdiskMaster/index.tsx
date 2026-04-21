import React, { useState, useEffect } from 'react';
import { invoke } from '@tauri-apps/api/core';
import { listen } from '@tauri-apps/api/event';
import { open } from '@tauri-apps/plugin-dialog';

interface PwnState {
    pwned: boolean;
    cpid: string;
    model: string;
}

export const RamdiskMaster: React.FC = () => {
    const [state, setState] = useState<PwnState | null>(null);
    const [status, setStatus] = useState("Idle");
    const [logs, setLogs] = useState<string[]>([]);
    const [udid] = useState("");
    const [ramdiskPath, setRamdiskPath] = useState<string | null>(null);

    useEffect(() => {
        const unlistenProgress = listen<string>('pwn-progress', (event) => {
            setLogs((prev: string[]) => [...prev.slice(-49), `[EXPLOIT] ${event.payload}`]);
        });

        const unlistenComplete = listen<number>('pwn-complete', (event) => {
            setStatus("Task Terminated");
            setLogs((prev: string[]) => [...prev, `[INIT] Gaster finished with code: ${event.payload}`]);
            checkPwn();
        });

        return () => {
            unlistenProgress.then(f => f());
            unlistenComplete.then(f => f());
        };
    }, []);

    const checkPwn = async () => {
        try {
            setStatus("Scanning...");
            const res = await invoke<PwnState>('ios_check_pwn_state', { udid });
            setState(res);
            setLogs(prev => [...prev.slice(-49), `[INIT] Found ${res.model} (${res.cpid}). PWNED: ${res.pwned ? 'YES' : 'NO'}`]);
            setStatus("Ready");
        } catch (e) {
            setLogs(prev => [...prev.slice(-49), `[ERROR] ${e}`]);
            setStatus("Error");
        }
    };

    const triggerPwn = async () => {
        setStatus("Exploiting...");
        try {
            await invoke('ios_run_gaster_pwn');
        } catch (e) {
            setLogs(prev => [...prev.slice(-49), `[ERROR] ${e}`]);
        }
    };

    const selectRamdisk = async () => {
        const selected = await open({
            multiple: false,
            filters: [{ name: "Ramdisk Image", extensions: ["img", "dmg", "raw"] }],
        });
        if (selected) {
            const path = typeof selected === "string" ? selected : selected;
            setRamdiskPath(path as string);
            setLogs(prev => [...prev.slice(-49), `[RAMDISK] Selected: ${path}`]);
        }
    };

    const bootRamdisk = async () => {
        if (!ramdiskPath) {
            setLogs(prev => [...prev, "[ERROR] No ramdisk image selected. Use SELECT RAMDISK button first."]);
            return;
        }
        try {
            await invoke('ios_boot_ramdisk', { ramdiskPath });
            setLogs(prev => [...prev, "[SUCCESS] Ramdisk boot command sent"]);
        } catch (e) {
            setLogs(prev => [...prev, `[ERROR] ${e}`]);
        }
    };

    return (
        <div className="p-4 sm:p-6 bg-black/40 backdrop-blur-xl rounded-2xl border border-white/10 text-white font-mono text-xs">
            <div className="flex flex-wrap justify-between items-center gap-3 mb-6">
                <h2 className="text-xl font-bold tracking-tighter text-indigo-400">CHECKM8 RAMDISK MASTER</h2>
                <div className={`px-3 py-1 rounded-full border ${state?.pwned ? 'border-green-500 text-green-400' : 'border-red-500 text-red-400'}`}>
                    PWNED: {state ? (state.pwned ? 'YES' : 'NO') : 'UNKNOWN'}
                </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
                <button 
                    onClick={checkPwn}
                    className="p-4 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl transition-all flex flex-col items-center gap-2"
                >
                    <span className="text-indigo-300 font-bold italic">STEP 1</span>
                    <span>PROBE DFU STATE</span>
                </button>
                <button 
                    onClick={triggerPwn}
                    disabled={status === "Exploiting"}
                    className="p-4 bg-indigo-500/10 hover:bg-indigo-500/20 border border-indigo-500/20 rounded-xl transition-all flex flex-col items-center gap-2 text-indigo-400"
                >
                    <span className="text-indigo-400 font-bold italic">STEP 2</span>
                    <span>TRIGGER PWNDFU</span>
                </button>
                <button
                    onClick={selectRamdisk}
                    className="p-4 bg-indigo-500/10 hover:bg-indigo-500/20 border border-indigo-500/20 rounded-xl transition-all flex flex-col items-center gap-2 text-indigo-400"
                >
                    <span className="text-indigo-400 font-bold italic">STEP 3</span>
                    <span>{ramdiskPath ? "✅ RAMDISK SET" : "SELECT RAMDISK"}</span>
                </button>
            </div>

            {ramdiskPath && (
                <div className="mb-4 p-3 bg-indigo-500/5 border border-indigo-500/10 rounded-xl text-indigo-300/70 truncate">
                    💾 {ramdiskPath}
                </div>
            )}

            <div className="bg-black/60 rounded-xl p-4 min-h-64 max-h-80 overflow-y-auto border border-white/5 space-y-1 mb-6">
                {logs.map((log, i) => (
                    <div key={i} className={log.includes('ERROR') ? 'text-red-400' : 'text-gray-400'}>
                        {log}
                    </div>
                ))}
            </div>

            <button 
                onClick={bootRamdisk}
                disabled={!state?.pwned || status.includes("Exploiting") || !ramdiskPath}
                className="w-full py-4 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 rounded-xl font-bold transition-all shadow-lg shadow-indigo-900/40 uppercase tracking-widest"
            >
                BOOT FORENSIC RAMDISK
            </button>
        </div>
    );
};
