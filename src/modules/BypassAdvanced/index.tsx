import React, { useState, useEffect } from 'react';
import { invoke } from '@tauri-apps/api/core';
import { listen } from '@tauri-apps/api/event';

interface ActivationTypeMatrix {
    device_udid: string;
    chip_generation: string;
    ios_version: string;
    imei_present: bool;
    imei_valid: bool;
    is_meid_cdma: bool;
    eligible_types: string[];
    recommended_type: string;
    temp_test_viable: bool;
}

interface BypassStep {
    step_num: number;
    instruction: string;
}

interface BypassProgress {
    pct: number;
    current_phase: string;
}

export const BypassAdvanced: React.FC = () => {
    const [udid, setUdid] = useState("");
    const [matrix, setMatrix] = useState<ActivationTypeMatrix | null>(null);
    const [status, setStatus] = useState("Idle");
    const [logs, setLogs] = useState<string[]>([]);
    const [progress, setProgress] = useState(0);
    const [stepInfo, setStepInfo] = useState<BypassStep | null>(null);
    const [phase, setPhase] = useState("");

    useEffect(() => {
        const unlistenStep = listen<BypassStep>('bypass-step', (event) => setStepInfo(event.payload));
        const unlistenProgress = listen<BypassProgress>('bypass-progress', (event) => {
            setProgress(event.payload.pct);
            setPhase(event.payload.current_phase);
        });
        const unlistenComplete = listen<any>('bypass-complete', () => setStatus("Operation Success"));

        return () => {
            unlistenStep.then(f => f());
            unlistenProgress.then(f => f());
            unlistenComplete.then(f => f());
        };
    }, []);

    const checkMatrix = async () => {
        try {
            setStatus("Probing...");
            const res = await invoke<ActivationTypeMatrix>('ios_activation_type_check', { udid });
            setMatrix(res);
            setStatus("Ready");
            setLogs(prev => [...prev, `[IDENT] iOS ${res.ios_version} | Chip ${res.chip_generation} | Recommended: ${res.recommended_type}`]);
        } catch (e) {
            setLogs(prev => [...prev, `[ERROR] ${e}`]);
            setStatus("Error");
        }
    };

    const runBypass = async (type: string) => {
        setStatus("In-Progress");
        try {
            await invoke('ios_untethered_bypass', { udid, activationType: type });
        } catch (e) {
            setLogs(prev => [...prev, `[ERROR] ${e}`]);
        }
    };

    const runTempTest = async () => {
        setStatus("Testing...");
        try {
            const res = await invoke<any>('ios_temp_activation', { udid });
            setLogs(prev => [...prev, `[TEST] Pre-validation result: ${res.activated ? 'PASSED' : 'FAILED'}`]);
            setStatus("Ready");
        } catch (e) {
            setLogs(prev => [...prev, `[ERROR] ${e}`]);
        }
    };

    return (
        <div className="p-6 bg-black/40 backdrop-blur-xl rounded-2xl border border-white/10 text-white font-mono text-xs">
            <div className="flex justify-between items-center mb-6">
                <h2 className="text-xl font-bold tracking-tighter text-fuchsia-400 italic">UNTETHERED BYPASS RESEARCH (F3arRa1n Flow)</h2>
                <div className="flex gap-2">
                    <button onClick={runTempTest} className="px-3 py-1 bg-white/5 border border-white/10 rounded-lg text-[8px] uppercase font-bold hover:bg-white/10 transition-all">Run Pre-Check</button>
                    <div className="px-3 py-1 rounded-full border border-white/20 uppercase text-[9px] bg-black/40">Status: {status}</div>
                </div>
            </div>

            <div className="flex gap-2 mb-8">
                <input 
                    type="text" 
                    placeholder="Enter Reference UDID"
                    value={udid}
                    onChange={(e) => setUdid(e.target.value)}
                    className="flex-1 bg-white/5 border border-white/10 p-3 rounded-xl outline-none focus:border-fuchsia-500 transition-all"
                />
                <button 
                    onClick={checkMatrix}
                    className="px-6 py-3 bg-fuchsia-600/20 text-fuchsia-400 border border-fuchsia-600/40 rounded-xl hover:bg-fuchsia-600 hover:text-white transition-all font-bold"
                >
                    AUTO-DETECT
                </button>
            </div>

            {matrix && (
                <div className="mb-8 animate-in fade-in slide-in-from-top-2 duration-500">
                    <div className="text-[10px] font-bold text-gray-500 mb-4 uppercase tracking-[0.2em] flex items-center gap-2">
                        <div className="w-1 h-1 bg-fuchsia-500 rounded-full" />
                        Eligibility Matrix
                    </div>
                    <div className="grid grid-cols-1 gap-2">
                        {matrix.eligible_types.map((type) => (
                            <div key={type} className="p-4 bg-white/5 border border-white/10 rounded-xl flex justify-between items-center group hover:bg-white/10 transition-all">
                                <div className="flex items-center gap-4">
                                    <div className="text-fuchsia-400 font-bold">{type}</div>
                                    {matrix.recommended_type === type && (
                                        <span className="text-[7px] bg-fuchsia-500 text-white px-1.5 py-0.5 rounded font-black">RECOMMENDED</span>
                                    )}
                                </div>
                                <button 
                                    onClick={() => runBypass(type)}
                                    className="px-4 py-2 bg-white/10 border border-white/10 rounded-lg text-[9px] font-bold hover:bg-fuchsia-600 hover:border-fuchsia-400 transition-all"
                                >
                                    ACTIVATE
                                </button>
                            </div>
                        ))}
                    </div>
                </div>
            )}

            {status === "In-Progress" && (
                <div className="mb-8 p-6 bg-fuchsia-900/10 border border-fuchsia-500/20 rounded-2xl animate-pulse">
                    <div className="flex justify-between items-end mb-3">
                        <div className="space-y-1">
                            <div className="text-[8px] text-fuchsia-400 font-black uppercase">Stage {stepInfo?.step_num}/6</div>
                            <div className="text-xl font-bold tracking-tight text-white">{phase}</div>
                        </div>
                        <div className="text-2xl font-black text-fuchsia-500">{progress}%</div>
                    </div>
                    <div className="h-2 w-full bg-white/5 rounded-full overflow-hidden border border-white/10 mb-2">
                        <div className="h-full bg-fuchsia-500 transition-all duration-700" style={{ width: `${progress}%` }} />
                    </div>
                    <div className="text-[10px] text-gray-400 font-mono tracking-tighter italic">{stepInfo?.instruction}</div>
                </div>
            )}

            <div className="bg-black/60 rounded-xl p-4 h-32 overflow-y-auto border border-white/5 space-y-1 custom-scrollbar">
                {logs.map((log, i) => <div key={i} className="text-gray-500 truncate">{log}</div>)}
            </div>
        </div>
    );
};
