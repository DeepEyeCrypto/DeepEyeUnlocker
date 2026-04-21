import React, { useState } from 'react';
import { invoke } from '@tauri-apps/api/core';

interface ExtractionResult {
    name: string;
    remote: string;
    success: boolean;
    local?: string;
}

interface MassExtractionReport {
    success: boolean;
    results: ExtractionResult[];
    message: string;
}

export const DeepExtraction: React.FC = () => {
    const [status, setStatus] = useState("Idle");
    const [report, setReport] = useState<MassExtractionReport | null>(null);
    const [logs, setLogs] = useState<string[]>([]);
    const [savePath, setSavePath] = useState("/Users/enayat/Documents/DeepEyeExtraction");

    const mountRamdisk = async () => {
        setStatus("Mounting...");
        try {
            const res = await invoke<any>('ios_mount_ramdisk');
            if (res.success) {
                setLogs(prev => [...prev, `[SUCCESS] ${res.message}`]);
                setStatus("Ready");
            } else {
                setLogs(prev => [...prev, `[ERROR] Failed to mount: ${JSON.stringify(res.errors)}`]);
                setStatus("Mount Error");
            }
        } catch (e) {
            setLogs(prev => [...prev, `[CRITICAL] ${e}`]);
            setStatus("Error");
        }
    };

    const runMassExtraction = async () => {
        setStatus("Extracting...");
        setReport(null);
        try {
            const res = await invoke<MassExtractionReport>('ios_mass_extract', { savePath });
            setReport(res);
            setLogs(prev => [...prev, `[SUCCESS] ${res.message}`]);
            setStatus("Complete");
        } catch (e) {
            setLogs(prev => [...prev, `[CRITICAL] ${e}`]);
            setStatus("Extraction Failed");
        }
    };

    return (
        <div className="p-8 bg-black/40 backdrop-blur-2xl rounded-3xl border border-white/5 text-white font-mono text-xs animate-in fade-in duration-700">
            <div className="flex justify-between items-center mb-10">
                <div className="flex items-center gap-4">
                    <div className="w-10 h-10 bg-indigo-600/20 rounded-xl flex items-center justify-center border border-indigo-500/30">
                        <div className="w-4 h-4 bg-indigo-500 rounded-sm animate-pulse" />
                    </div>
                    <div>
                        <h2 className="text-2xl font-black tracking-tighter text-white uppercase italic">DeepExtraction v2</h2>
                        <div className="text-[8px] text-gray-500 tracking-[0.4em] uppercase">Mass Artifact Extraction Engine</div>
                    </div>
                </div>
                <div className="flex gap-3">
                    <div className={`px-4 py-2 rounded-full border ${status.includes('Error') ? 'border-red-500/50 bg-red-500/10 text-red-400' : 'border-indigo-500/30 bg-indigo-500/5 text-indigo-400'} text-[9px] font-black uppercase tracking-widest`}>
                        STATE: {status}
                    </div>
                </div>
            </div>

            <div className="grid grid-cols-1 xl:grid-cols-3 gap-6 mb-10">
                <div className="xl:col-span-2 space-y-4">
                    <div className="text-[9px] text-gray-500 font-bold uppercase tracking-[0.2em]">Extraction Parameters</div>
                    <div className="flex flex-wrap gap-2">
                        <input 
                            type="text" 
                            value={savePath}
                            onChange={(e) => setSavePath(e.target.value)}
                            className="flex-1 bg-white/5 border border-white/10 p-4 rounded-2xl outline-none focus:border-indigo-500 focus:bg-white/[0.08] transition-all text-[10px]"
                            placeholder="Saving directory path..."
                        />
                    </div>
                </div>
                <div className="flex flex-col justify-end gap-2">
                    <button 
                        onClick={mountRamdisk}
                        className="w-full py-4 bg-white/5 border border-white/10 rounded-2xl hover:bg-white/10 active:scale-95 transition-all text-[10px] uppercase font-black"
                    >
                        Mount Data
                    </button>
                    <button 
                        onClick={runMassExtraction}
                        disabled={status === "Extracting..."}
                        className="w-full py-4 bg-indigo-600/20 border border-indigo-500/40 text-indigo-400 rounded-2xl hover:bg-indigo-600 hover:text-white active:scale-95 transition-all text-[10px] uppercase font-black shadow-lg shadow-indigo-900/20"
                    >
                        Initiate Mass Extraction
                    </button>
                </div>
            </div>

            {report && (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-8">
                    {report.results.map((res, i) => (
                        <div key={i} className="p-4 bg-white/5 border border-white/5 rounded-2xl flex items-center justify-between group hover:bg-white/[0.08] transition-all">
                            <div className="flex items-center gap-4">
                                <div className={`w-1.5 h-1.5 rounded-full ${res.success ? 'bg-green-500 shadow-[0_0_8px_rgba(34,197,94,0.6)]' : 'bg-red-500 animate-pulse'}`} />
                                <div>
                                    <div className="font-bold text-[10px] text-white/90">{res.name}</div>
                                    <div className="text-[8px] text-gray-600 truncate max-w-[180px]">{res.remote}</div>
                                </div>
                            </div>
                            {res.success && (
                                <div className="text-[7px] bg-green-500/20 text-green-400 px-2 py-1 rounded font-black tracking-tighter">SAVED</div>
                            )}
                        </div>
                    ))}
                </div>
            )}

            <div className="mt-auto">
                <div className="text-[9px] text-gray-600 font-bold uppercase tracking-[0.2em] mb-3">Live Session Logs</div>
                <div className="bg-black/80 rounded-2xl p-6 min-h-48 max-h-80 overflow-y-auto border border-white/5 space-y-2 custom-scrollbar shadow-inner">
                    {logs.map((log, i) => (
                        <div key={i} className="flex gap-4 items-start">
                            <span className="text-gray-700 text-[8px] font-bold mt-0.5">[{i}]</span>
                            <span className={`text-[10px] leading-relaxed ${log.includes('ERROR') || log.includes('CRITICAL') ? 'text-red-400' : 'text-gray-400'}`}>{log}</span>
                        </div>
                    ))}
                    <div className="h-1" />
                </div>
            </div>
        </div>
    );
};
