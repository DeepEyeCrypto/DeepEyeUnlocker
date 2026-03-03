import { Terminal, Database, ShieldAlert, Cpu, HardDrive } from "lucide-react";
import { useState, useEffect } from "react";
import { motion } from "framer-motion";

export default function DebugLogsPage() {
    const [logs, setLogs] = useState<{ id: number, time: string, level: 'INFO' | 'WARN' | 'ERROR' | 'DEBUG', message: string, source: string }[]>([]);

    useEffect(() => {
        // Mocking some system logs for now
        const initialLogs = [
            { id: 1, time: new Date().toLocaleTimeString(), level: 'INFO', message: "DeepEye Universal Core Engine initialized.", source: 'system' },
            { id: 2, time: new Date().toLocaleTimeString(), level: 'DEBUG', message: "Loaded 4 protocol stacks: Qualcomm, MTK, Samsung, UniSoc.", source: 'core::protocols' },
            { id: 3, time: new Date().toLocaleTimeString(), level: 'INFO', message: "Awaiting USB device connections via nusb...", source: 'core::usb' }
        ];

        setLogs(initialLogs as any);

        const interval = setInterval(() => {
            const tempMsgs = [
                "Polling native USB bridges...",
                "LibUSB context refreshed.",
                "Zero devices matching EDL/BROM criteria found.",
                "Waiting for interrupts."
            ];
            const msg = tempMsgs[Math.floor(Math.random() * tempMsgs.length)];

            setLogs(prev => [...prev.slice(-49), {
                id: Date.now(),
                time: new Date().toLocaleTimeString(),
                level: 'DEBUG',
                message: msg,
                source: 'core::usb::polling'
            }]);
        }, 8000);

        return () => clearInterval(interval);
    }, []);

    const getColorForLevel = (level: string) => {
        switch (level) {
            case 'INFO': return 'text-[#00C4B4]';
            case 'WARN': return 'text-yellow-400';
            case 'ERROR': return 'text-red-500';
            case 'DEBUG': return 'text-gray-400';
            default: return 'text-white';
        }
    };

    return (
        <div className="flex flex-col h-full w-full bg-[#0D0D1A] text-white p-6 relative">
            <div className="absolute inset-0 bg-cyber-grid animate-grid [mask-image:linear-gradient(to_bottom,black_10%,transparent_90%)] opacity-20 pointer-events-none z-0"></div>

            <div className="relative z-10 flex flex-col h-full w-full max-w-7xl mx-auto gap-6">

                {/* Header */}
                <div className="flex items-center justify-between shrink-0">
                    <div>
                        <h1 className="text-3xl font-bold flex items-center gap-3">
                            <Terminal className="text-purple-400" size={28} />
                            Debug Logs
                        </h1>
                        <p className="text-[#9898C4] text-sm mt-1">Real-time telemetry and Core Engine daemon activity.</p>
                    </div>
                    <div className="flex items-center gap-3">
                        <button className="px-4 py-2 rounded-lg bg-white/5 hover:bg-white/10 border border-white/10 text-sm font-medium transition-colors cursor-pointer">
                            Export Logs
                        </button>
                        <button onClick={() => setLogs([])} className="px-4 py-2 rounded-lg hover:bg-red-500/20 text-red-400 border border-red-500/20 text-sm font-medium transition-colors cursor-pointer">
                            Clear
                        </button>
                    </div>
                </div>

                {/* Sub-Metric Cards */}
                <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 shrink-0">
                    {[
                        { label: 'LibUSB Context', value: 'Active', icon: HardDrive, color: 'text-green-400' },
                        { label: 'SQLite DB', value: 'Idle', icon: Database, color: 'text-blue-400' },
                        { label: 'Memory Usage', value: '18 MB', icon: Cpu, color: 'text-purple-400' },
                        { label: 'Exceptions', value: '0', icon: ShieldAlert, color: 'text-yellow-400' }
                    ].map((stat, i) => (
                        <div key={i} className="flex items-center gap-4 bg-white/5 border border-white/10 rounded-xl p-4 backdrop-blur-sm">
                            <div className={`p-2 rounded-lg bg-black/30 ${stat.color}`}>
                                <stat.icon size={20} />
                            </div>
                            <div>
                                <div className="text-xs text-gray-500 font-bold uppercase tracking-wider">{stat.label}</div>
                                <div className="text-lg font-semibold">{stat.value}</div>
                            </div>
                        </div>
                    ))}
                </div>

                {/* Main Terminal Window */}
                <div className="flex-1 bg-[#0A0A14] border border-white/10 rounded-xl overflow-hidden flex flex-col shadow-2xl relative">
                    {/* Top Bar */}
                    <div className="h-8 bg-[#151525] border-b border-white/5 flex items-center px-4 shrink-0">
                        <div className="flex items-center gap-2">
                            <div className="w-3 h-3 rounded-full bg-red-500/80"></div>
                            <div className="w-3 h-3 rounded-full bg-yellow-500/80"></div>
                            <div className="w-3 h-3 rounded-full bg-green-500/80"></div>
                        </div>
                        <div className="mx-auto text-xs text-gray-500 font-mono">deepeye-daemon.log</div>
                    </div>

                    {/* Log Lines */}
                    <div className="flex-1 overflow-y-auto p-4 font-mono text-sm flex flex-col gap-1">
                        {logs.map((log) => (
                            <motion.div
                                initial={{ opacity: 0, x: -10 }}
                                animate={{ opacity: 1, x: 0 }}
                                key={log.id}
                                className="group flex gap-4 hover:bg-white/5 px-2 py-0.5 rounded transition-colors"
                            >
                                <span className="text-gray-600 shrink-0 w-24">[{log.time}]</span>
                                <span className={`${getColorForLevel(log.level)} font-bold shrink-0 w-14`}>{log.level}</span>
                                <span className="text-purple-400/70 shrink-0 w-40 truncate">[{log.source}]</span>
                                <span className="text-gray-300 flex-1 break-words">{log.message}</span>
                            </motion.div>
                        ))}
                    </div>
                </div>

            </div>
        </div>
    );
}
