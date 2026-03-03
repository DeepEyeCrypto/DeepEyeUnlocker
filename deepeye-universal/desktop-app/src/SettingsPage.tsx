import { Settings, Shield, Sliders, Monitor, Globe, Save, RotateCcw, Cpu } from "lucide-react";
import { useState } from "react";
import { motion } from "framer-motion";

export default function SettingsPage() {
    const [apiKey, setApiKey] = useState("sk-de-********************");
    const [autoDetect, setAutoDetect] = useState(true);
    const [performanceMode, setPerformanceMode] = useState(false);

    const categories = [
        { id: 'connection', label: 'Connection', icon: Sliders },
        { id: 'security', label: 'Security & Auth', icon: Shield },
        { id: 'appearance', label: 'Appearance', icon: Monitor },
        { id: 'general', label: 'General', icon: Globe },
    ];

    return (
        <div className="flex flex-col h-full w-full bg-[#0D0D1A] text-white p-8 relative overflow-hidden">
            <div className="absolute top-0 left-0 w-full h-full bg-cyber-grid opacity-10 pointer-events-none"></div>

            <div className="relative z-10 max-w-4xl mx-auto w-full flex flex-col gap-10">
                {/* Header */}
                <div className="flex items-center justify-between">
                    <div className="flex flex-col gap-1">
                        <h1 className="text-3xl font-black flex items-center gap-3">
                            <Settings className="text-purple-500" size={32} />
                            System Settings
                        </h1>
                        <p className="text-gray-500 text-sm">Configure your global DeepEye Universal environment</p>
                    </div>
                    <div className="flex gap-3">
                        <button className="flex items-center gap-2 px-4 py-2 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl text-xs font-bold transition-all text-gray-400">
                            <RotateCcw size={14} /> Reset
                        </button>
                        <button className="flex items-center gap-2 px-6 py-2 bg-gradient-to-r from-purple-600 to-cyan-600 hover:from-purple-500 hover:to-cyan-500 rounded-xl text-xs font-black shadow-lg shadow-purple-500/20 transition-all">
                            <Save size={14} /> Save Changes
                        </button>
                    </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
                    {/* Sidebar Tabs */}
                    <div className="flex flex-col gap-2">
                        {categories.map((cat) => (
                            <button
                                key={cat.id}
                                className={`flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium transition-all ${cat.id === 'connection' ? 'bg-white/10 text-white border border-white/10 shadow-xl' : 'text-gray-500 hover:text-gray-300 hover:bg-white/5'}`}
                            >
                                <cat.icon size={18} />
                                {cat.label}
                            </button>
                        ))}
                    </div>

                    {/* Settings Content */}
                    <div className="md:col-span-3 flex flex-col gap-6 bg-white/5 border border-white/10 rounded-2xl p-8 backdrop-blur-md">
                        {/* Section: Device Connection */}
                        <div className="flex flex-col gap-6">
                            <h3 className="text-lg font-bold flex items-center gap-2 text-cyan-400">
                                <Cpu size={20} /> USB & Connection
                            </h3>

                            <div className="flex items-center justify-between p-4 bg-black/40 border border-white/5 rounded-xl">
                                <div className="flex flex-col gap-1">
                                    <span className="text-sm font-bold text-gray-200">Auto-Detect Devices</span>
                                    <span className="text-[10px] text-gray-500 uppercase tracking-tighter">Scan USB bus continuously for target handshakes</span>
                                </div>
                                <button
                                    onClick={() => setAutoDetect(!autoDetect)}
                                    className={`w-12 h-6 rounded-full relative transition-colors ${autoDetect ? 'bg-purple-600' : 'bg-gray-700'}`}
                                >
                                    <motion.div
                                        animate={{ x: autoDetect ? 26 : 2 }}
                                        className="absolute top-1 left-0 w-4 h-4 bg-white rounded-full shadow-md"
                                    />
                                </button>
                            </div>

                            <div className="flex flex-col gap-3">
                                <label className="text-xs font-bold text-gray-500 uppercase tracking-widest pl-1">Default Protocol Override</label>
                                <select className="bg-black/60 border border-white/10 rounded-xl p-3 text-sm focus:outline-none focus:border-cyan-400/50 appearance-none cursor-pointer">
                                    <option>Hardware Dependent (Recommended)</option>
                                    <option>Qualcomm Firehose (S908 / S918)</option>
                                    <option>MediaTek Brom (Force V6)</option>
                                    <option>Samsung Loke (Odin V1)</option>
                                </select>
                            </div>
                        </div>

                        <div className="h-px bg-white/5 w-full"></div>

                        {/* Section: Security */}
                        <div className="flex flex-col gap-6">
                            <h3 className="text-lg font-bold flex items-center gap-2 text-purple-400">
                                <Shield size={20} /> Security & Credits
                            </h3>

                            <div className="flex flex-col gap-3">
                                <label className="text-xs font-bold text-gray-500 uppercase tracking-widest pl-1">Enterprise API Key</label>
                                <div className="relative group">
                                    <input
                                        type="password"
                                        value={apiKey}
                                        onChange={(e) => setApiKey(e.target.value)}
                                        className="w-full bg-black/60 border border-white/10 rounded-xl p-3 pr-12 text-sm focus:outline-none focus:border-purple-400/50 transition-all font-mono"
                                    />
                                    <button className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-600 hover:text-purple-400 transition-colors">
                                        <Monitor size={16} />
                                    </button>
                                </div>
                                <p className="text-[10px] text-gray-500 italic pl-1">Used for server-side auth (Samsung CPID / Xiaomi Auth).</p>
                            </div>
                        </div>

                        <div className="h-px bg-white/5 w-full"></div>

                        {/* Section: Performance */}
                        <div className="flex items-center justify-between p-4 bg-purple-500/5 border border-purple-500/10 rounded-xl">
                            <div className="flex flex-col gap-1">
                                <span className="text-sm font-bold text-purple-200">Ultra-Low Latency Mode</span>
                                <span className="text-[10px] text-gray-500 uppercase tracking-tighter">Bypass OS drivers where possible using NUS_B raw-stack</span>
                            </div>
                            <button
                                onClick={() => setPerformanceMode(!performanceMode)}
                                className={`w-12 h-6 rounded-full relative transition-colors ${performanceMode ? 'bg-cyan-500' : 'bg-gray-700'}`}
                            >
                                <motion.div
                                    animate={{ x: performanceMode ? 26 : 2 }}
                                    className="absolute top-1 left-0 w-4 h-4 bg-white rounded-full shadow-md"
                                />
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
