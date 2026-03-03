import { LayoutDashboard, Target, Zap, Clock, ShieldCheck, ChevronRight } from "lucide-react";
import { useState, useEffect } from "react";
import { motion } from "framer-motion";

export default function DashboardPage() {
    const [mounted, setMounted] = useState(false);
    useEffect(() => setMounted(true), []);

    const stats = [
        { label: "Total Operations", value: "1,294", icon: Zap, color: "text-purple-400" },
        { label: "Devices Serviced", value: "753", icon: Target, color: "text-cyan-400" },
        { label: "Success Rate", value: "99.8%", icon: ShieldCheck, color: "text-[#00C4B4]" },
        { label: "Uptime", value: "14d 6h", icon: Clock, color: "text-orange-400" }
    ];

    const recentJobs = [
        { id: 1, brand: "Samsung", model: "SM-G998B", action: "IMEI Fix", time: "2 min ago", status: "Success" },
        { id: 2, brand: "Xiaomi", model: "Redmi Note 13", action: "Flash Firmware", time: "15 min ago", status: "Success" },
        { id: 3, brand: "Infinix", model: "X6811", action: "FRP Bypass", time: "1 hour ago", status: "Success" },
        { id: 4, brand: "OPPO", model: "CPH2211", action: "Backup Security", time: "3 hours ago", status: "Success" }
    ];

    return (
        <div className="flex flex-col h-full w-full bg-[#0D0D1A] text-white p-8 relative overflow-y-auto overflow-x-hidden scrollbar-hide">
            <div className="absolute inset-0 bg-cyber-grid animate-grid [mask-image:linear-gradient(to_bottom,black_10%,transparent_90%)] opacity-20 pointer-events-none z-0"></div>

            <div className="relative z-10 w-full max-w-7xl mx-auto flex flex-col gap-10">
                {/* Brand Hero */}
                <div className="flex flex-col gap-2">
                    <motion.h1
                        initial={{ opacity: 0, y: -20 }}
                        animate={{ opacity: 1, y: 0 }}
                        className="text-4xl font-extrabold flex items-center gap-3 bg-gradient-to-r from-white to-gray-500 bg-clip-text text-transparent"
                    >
                        <LayoutDashboard className="text-purple-500" size={36} />
                        Analytics Dashboard
                    </motion.h1>
                    <p className="text-[#9898C4] text-lg max-w-2xl">
                        Universal Multi-platform Service Suite for Modern Android Devices
                    </p>
                </div>

                {/* Stats Grid */}
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                    {stats.map((stat, i) => (
                        <motion.div
                            key={i}
                            initial={{ opacity: 0, scale: 0.95 }}
                            animate={{ opacity: 1, scale: 1 }}
                            transition={{ delay: i * 0.1 }}
                            className="bg-white/5 border border-white/10 rounded-2xl p-6 backdrop-blur-xl group hover:bg-white/10 transition-all duration-300"
                        >
                            <div className="flex justify-between items-start mb-4">
                                <div className={`p-3 rounded-xl bg-black/40 ${stat.color}`}>
                                    <stat.icon size={24} />
                                </div>
                                <span className="text-[10px] font-bold text-gray-500 uppercase tracking-widest bg-white/5 px-2 py-1 rounded">Live</span>
                            </div>
                            <div className="text-3xl font-black mb-1">{mounted ? stat.value : "0"}</div>
                            <div className="text-xs font-bold text-gray-400 uppercase tracking-wider">{stat.label}</div>
                        </motion.div>
                    ))}
                </div>

                {/* Main Viewport Split */}
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 pb-12">
                    {/* Left: Recent Activity */}
                    <div className="lg:col-span-2 flex flex-col gap-4">
                        <div className="flex items-center justify-between">
                            <h2 className="text-xl font-bold flex items-center gap-2">
                                Recent Operations
                            </h2>
                            <button className="text-sm text-purple-400 hover:text-purple-300 transition-colors">View All Logs &rarr;</button>
                        </div>
                        <div className="bg-white/5 border border-white/10 rounded-2xl overflow-hidden backdrop-blur-sm">
                            <table className="w-full text-left border-collapse">
                                <thead className="bg-white/5 border-b border-white/10">
                                    <tr>
                                        <th className="px-6 py-4 text-xs font-bold text-gray-500 uppercase tracking-wider">Device</th>
                                        <th className="px-6 py-4 text-xs font-bold text-gray-500 uppercase tracking-wider">Action</th>
                                        <th className="px-6 py-4 text-xs font-bold text-gray-500 uppercase tracking-wider">Time</th>
                                        <th className="px-6 py-4 text-xs font-bold text-gray-500 uppercase tracking-wider text-right">Status</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-white/5">
                                    {recentJobs.map((job) => (
                                        <tr key={job.id} className="group hover:bg-white/5 transition-colors">
                                            <td className="px-6 py-4">
                                                <div className="flex flex-col">
                                                    <span className="font-semibold text-white">{job.brand}</span>
                                                    <span className="text-[10px] text-gray-500">{job.model}</span>
                                                </div>
                                            </td>
                                            <td className="px-6 py-4 text-sm text-gray-300">
                                                {job.action}
                                            </td>
                                            <td className="px-6 py-4 text-sm text-gray-500 font-mono">
                                                {job.time}
                                            </td>
                                            <td className="px-6 py-4 text-right">
                                                <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[10px] font-bold uppercase bg-[#00C4B4]/10 text-[#00C4B4] border border-[#00C4B4]/20">
                                                    <div className="w-1.5 h-1.5 rounded-full bg-[#00C4B4]" />
                                                    {job.status}
                                                </span>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    </div>

                    {/* Right: Quick Launch / Status */}
                    <div className="flex flex-col gap-6">
                        <h2 className="text-xl font-bold">Protocol Health</h2>
                        <div className="space-y-4">
                            {[
                                { name: "Qualcomm Sahara", state: "Ready", load: 12 },
                                { name: "MediaTek BROM", state: "Ready", load: 45 },
                                { name: "Samsung Odin", state: "Active", load: 88 }
                            ].map((p, i) => (
                                <div key={i} className="bg-white/5 border border-white/10 p-5 rounded-2xl flex flex-col gap-3">
                                    <div className="flex justify-between items-center">
                                        <span className="font-semibold">{p.name}</span>
                                        <span className="text-[10px] font-bold text-green-400 bg-green-400/10 px-2 py-0.5 rounded border border-green-400/20">{p.state}</span>
                                    </div>
                                    <div className="w-full h-1.5 bg-black/40 rounded-full overflow-hidden border border-white/5">
                                        <motion.div
                                            initial={{ width: 0 }}
                                            animate={{ width: `${p.load}%` }}
                                            className="h-full bg-gradient-to-r from-purple-500 to-cyan-400"
                                        />
                                    </div>
                                    <span className="text-[10px] text-gray-500 font-mono">Bus Load: {p.load}%</span>
                                </div>
                            ))}
                        </div>

                        {/* Upgrade/Version Banner */}
                        <div className="mt-auto bg-gradient-to-br from-purple-600/20 to-cyan-600/20 border border-purple-500/20 p-6 rounded-2xl relative overflow-hidden group">
                            <div className="absolute -right-8 -bottom-8 opacity-10 rotate-12 transition-transform group-hover:rotate-0">
                                <ShieldCheck size={120} />
                            </div>
                            <h3 className="text-lg font-bold text-white mb-2">DeepEye Enterprise</h3>
                            <p className="text-xs text-gray-400 mb-4 leading-relaxed">
                                Unlock CPID / Server-auth for modern Android 15 Samsung variants.
                            </p>
                            <button className="w-full py-2 bg-gradient-to-r from-purple-500 to-cyan-500 rounded-xl text-xs font-bold shadow-lg shadow-purple-500/20 flex items-center justify-center gap-2">
                                Check Tokens <ChevronRight size={14} />
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
