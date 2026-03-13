import React from 'react';

interface SidebarProps {
    currentTab: string;
    setTab: (tab: string) => void;
}

export const Sidebar: React.FC<SidebarProps> = ({ currentTab, setTab }) => {
    const tabs = [
        { id: 'ios-backup', label: 'IOS BACKUP', color: 'text-blue-500' },
        { id: 'adb', label: 'ADB TERMINAL', color: 'text-emerald-500' },
        { id: 'bypass-advanced', label: 'ADVANCED BYPASS', color: 'text-fuchsia-400' },
        { id: 'identity', label: 'IDENTITY AUDIT', color: 'text-blue-400' },
        { id: 'tickets', label: 'TICKET ENGINE', color: 'text-emerald-400' },
        { id: 'orchestrator', label: 'EXPLOIT CHAIN', color: 'text-indigo-500' },
        { id: 'dfu', label: 'RESTORE ENGINE', color: 'text-blue-400' },
        { id: 'activation', label: 'ACTIVATION LOCK', color: 'text-purple-400' },
        { id: 'apple-id', label: 'APPLE ID AUDIT', color: 'text-green-400' },
        { id: 'screentime', label: 'SCREEN TIME', color: 'text-amber-400' },
        { id: 'mdm', label: 'MDM POLICIES', color: 'text-cyan-400' },
        { id: 'bypass', label: 'HELLO BYPASS', color: 'text-red-400' },
        { id: 'ramdisk', label: 'RAMDISK MASTER', color: 'text-indigo-400' },
        { id: 'deep-extraction', label: 'MASS EXTRACTION', color: 'text-indigo-500' },
        { id: 'vault', label: 'DEEPVAULT EXPORT', color: 'text-blue-300' },
    ];

    return (
        <div className="w-64 h-screen bg-black/80 backdrop-blur-3xl border-r border-white/5 flex flex-col p-6 space-y-8 select-none">
            <div className="flex items-center gap-3 px-2">
                <div className="w-8 h-8 bg-blue-600 rounded-lg flex items-center justify-center font-bold text-white shadow-lg shadow-blue-900/40">D</div>
                <div className="font-bold tracking-[0.2em] text-sm text-white/90">DEEPEYE UNLOCKER</div>
            </div>

            <nav className="flex-1 space-y-1">
                {tabs.map((tab) => (
                    <button
                        key={tab.id}
                        onClick={() => setTab(tab.id)}
                        className={`w-full flex items-center px-4 py-3 rounded-xl transition-all font-mono text-[10px] tracking-widest uppercase ${
                            currentTab === tab.id 
                            ? 'bg-white/10 text-white border border-white/5 shadow-inner' 
                            : `text-gray-500 hover:bg-white/5 hover:${tab.color}`
                        }`}
                    >
                        {tab.label}
                    </button>
                ))}
            </nav>

            <div className="px-4 py-3 bg-white/5 rounded-xl border border-white/5 text-[9px] text-gray-500 font-mono">
                SESSION: <span className="text-green-400">ACTIVE</span><br/>
                STATION: <span className="text-white">macOS_FORENSIC_01</span>
            </div>
        </div>
    );
};
