import { useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import { Cpu, Download, FileText, ShieldCheck } from "lucide-react";

type AuditDevice = {
    model?: string;
    serial?: string;
    mode?: string;
};

type AuditReportData = {
    timestamp: string;
    security_score: number;
    device?: AuditDevice;
    logs_summary: string[];
};

export default function AuditReport() {
    const [report, setReport] = useState<AuditReportData | null>(null);
    const [isGenerating, setIsGenerating] = useState(false);

    const generate = async () => {
        setIsGenerating(true);
        try {
            const res = await invoke<AuditReportData>('reporter_generate_audit');
            setReport(res);
        } catch (error: unknown) {
            console.error(error);
        } finally {
            setIsGenerating(false);
        }
    };

    return (
        <div className="max-w-4xl mx-auto py-6">
            <div className="flex items-center justify-between mb-8">
                <div>
                    <h2 className="text-2xl font-bold text-white">📊 Device Audit Report</h2>
                    <p className="text-slate-400">Generate professional forensic evidence for bypass operations</p>
                </div>
                <button 
                    onClick={generate} 
                    disabled={isGenerating}
                    className="btn primary flex items-center gap-2 px-6 py-3"
                >
                    {isGenerating ? "Gathering Evidence..." : "Generate Audit Report"}
                </button>
            </div>

            {report ? (
                <div className="glass p-8 border-white/5 space-y-8 animate-in fade-in duration-500">
                    <div className="flex justify-between items-start border-b border-white/5 pb-6">
                        <div className="flex gap-4 items-center">
                            <div className="w-16 h-16 bg-blue-500/10 rounded-2xl flex items-center justify-center border border-blue-500/20">
                                <ShieldCheck className="w-8 h-8 text-blue-400" />
                            </div>
                            <div>
                                <h3 className="text-xl font-bold uppercase tracking-wider">DeepEye Evidence Log</h3>
                                <p className="text-xs text-slate-500 font-mono">{report.timestamp}</p>
                            </div>
                        </div>
                        <div className="text-right">
                            <div className="text-3xl font-black text-blue-400">{report.security_score}%</div>
                            <div className="text-[10px] text-slate-500 uppercase font-bold">Trust Score</div>
                        </div>
                    </div>

                    <div className="grid grid-cols-2 gap-8">
                        <div className="space-y-4">
                            <h4 className="flex items-center gap-2 text-sm font-bold text-slate-300 uppercase tracking-tight">
                                <Cpu className="w-4 h-4" />
                                Hardware Identity
                            </h4>
                            <div className="bg-white/5 p-4 rounded-xl space-y-2">
                                <div className="flex justify-between text-xs">
                                    <span className="text-slate-500">Model</span>
                                    <span className="text-white font-mono">{report.device?.model || "N/A"}</span>
                                </div>
                                <div className="flex justify-between text-xs">
                                    <span className="text-slate-500">Serial</span>
                                    <span className="text-white font-mono">{report.device?.serial || "N/A"}</span>
                                </div>
                                <div className="flex justify-between text-xs">
                                    <span className="text-slate-500">Mode</span>
                                    <span className="text-blue-400 font-bold uppercase">{report.device?.mode || "N/A"}</span>
                                </div>
                            </div>
                        </div>

                        <div className="space-y-4">
                            <h4 className="flex items-center gap-2 text-sm font-bold text-slate-300 uppercase tracking-tight">
                                <FileText className="w-4 h-4" />
                                Operation Summary
                            </h4>
                            <div className="space-y-2">
                                {report.logs_summary.map((log: string, i: number) => (
                                    <div key={i} className="flex gap-3 items-center text-xs text-slate-400">
                                        <div className="w-1.5 h-1.5 rounded-full bg-blue-500"></div>
                                        {log}
                                    </div>
                                ))}
                            </div>
                        </div>
                    </div>

                    <div className="pt-6 flex justify-end gap-3">
                        <button className="btn secondary px-4 py-2 flex items-center gap-2 text-sm">
                            <Download className="w-4 h-4" />
                            Export PDF
                        </button>
                    </div>
                </div>
            ) : (
                <div className="h-[400px] border-2 border-dashed border-white/5 rounded-3xl flex flex-col items-center justify-center gap-4 text-slate-600">
                    <FileText className="w-12 h-12" />
                    <p className="text-sm">Click generate to start the audit process</p>
                </div>
            )}
        </div>
    );
}
