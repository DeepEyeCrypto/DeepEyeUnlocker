import { useState } from "react";
import { LayoutDashboard, Zap, FileTerminal, Settings, ChevronRight, Database } from "lucide-react";
import DeepEyeFeaturePage from "./DeepEyeFeaturePage";
import DebugLogsPage from "./DebugLogsPage";
import DashboardPage from "./DashboardPage";
import PartitionManagerPage from "./PartitionManagerPage";
import SettingsPage from "./SettingsPage";

type PageId = 'dashboard' | 'features' | 'logs' | 'settings' | 'partition-manager';

export default function App() {
  const [activePage, setActivePage] = useState<PageId>('features');

  const navItems: { id: PageId, label: string, icon: any }[] = [
    { id: 'dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { id: 'features', label: 'Toolbox', icon: Zap },
    { id: 'logs', label: 'Debug Logs', icon: FileTerminal },
    { id: 'partition-manager', label: 'Partition Manager', icon: Database },
    { id: 'settings', label: 'Settings', icon: Settings },
  ];

  return (
    <div className="flex h-screen w-screen bg-[#0D0D1A] overflow-hidden text-white font-sans selection:bg-purple-500/30">

      {/* Sidebar / AppShell Nav */}
      <aside className="w-[72px] hover:w-64 transition-all duration-300 group z-50 flex flex-col bg-[#111122]/95 backdrop-blur-md border-r border-white/5 shadow-2xl shrink-0">

        {/* Brand Area */}
        <div className="h-16 flex items-center justify-center shrink-0 border-b border-white/5 overflow-hidden px-4">
          <div className="flex items-center gap-3">
            <div className="bg-gradient-to-br from-purple-500 to-cyan-400 p-2 rounded-lg shrink-0">
              <Zap size={20} className="text-white drop-shadow-md" />
            </div>
            <span className="font-bold whitespace-nowrap opacity-0 group-hover:opacity-100 transition-opacity duration-300 text-transparent bg-clip-text bg-gradient-to-r from-white to-gray-400">
              DeepEye Unlkr
            </span>
          </div>
        </div>

        {/* Nav Links */}
        <nav className="flex-1 overflow-y-auto py-6 flex flex-col gap-2 px-3 scrollbar-hide">
          {navItems.map(item => {
            const isActive = activePage === item.id;
            const Icon = item.icon;
            return (
              <button
                key={item.id}
                onClick={() => setActivePage(item.id)}
                className={`
                  relative flex items-center justify-start gap-4 p-3 rounded-xl transition-all w-full outline-none
                  ${isActive
                    ? 'bg-purple-500/10 text-purple-400 shadow-[inset_0_0_0_1px_rgba(168,85,247,0.2)]'
                    : 'text-gray-400 hover:text-white hover:bg-white/5'}
                `}
              >
                {isActive && (
                  <div className="absolute left-0 top-1/2 -translate-y-1/2 w-1 h-6 bg-purple-500 rounded-r-full shadow-[0_0_8px_rgba(168,85,247,0.8)]" />
                )}
                <div className="shrink-0 flex items-center justify-center w-6">
                  <Icon size={20} className={isActive ? "drop-shadow-[0_0_8px_rgba(168,85,247,0.5)]" : ""} />
                </div>
                <span className="whitespace-nowrap font-medium text-sm opacity-0 group-hover:opacity-100 transition-opacity duration-300">
                  {item.label}
                </span>

                {isActive && (
                  <ChevronRight size={14} className="ml-auto opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
                )}
              </button>
            );
          })}
        </nav>

        {/* Footer actions area */}
        <div className="p-4 border-t border-white/5 shrink-0">
          <div className="w-10 h-10 mx-auto group-hover:w-full rounded-xl bg-black/40 border border-white/5 flex items-center justify-center overflow-hidden transition-all duration-300">
            <span className="opacity-0 group-hover:opacity-100 whitespace-nowrap text-[10px] font-bold tracking-widest text-gray-500">v0.1.0-alpha</span>
          </div>
        </div>
      </aside>

      {/* Main Content Area */}
      <main className="flex-1 relative h-full overflow-y-auto overflow-x-hidden bg-[#0D0D1A]">
        {activePage === 'dashboard' && <DashboardPage />}
        {activePage === 'features' && <DeepEyeFeaturePage />}
        {activePage === 'logs' && <DebugLogsPage />}
        {activePage === 'partition-manager' && <PartitionManagerPage />}
        {activePage === 'settings' && <SettingsPage />}
      </main>
    </div>
  );
}
