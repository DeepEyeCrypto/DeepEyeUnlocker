import { NavLink } from 'react-router-dom';
import { LayoutDashboard, Zap, ShieldAlert, Wrench, ScrollText, Settings, Cpu } from 'lucide-react';

const routes = [
  { path: '/', label: 'Dashboard', icon: LayoutDashboard },
  { path: '/modes', label: 'Activation', icon: Zap },
  { path: '/fmi', label: 'FMI / Account', icon: ShieldAlert },
  { path: '/toolbox', label: 'Toolbox', icon: Wrench },
  { path: '/edl', label: 'Qualcomm EDL', icon: Cpu },
  { path: '/logs', label: 'Logs & History', icon: ScrollText },
  { path: '/settings', label: 'Settings', icon: Settings },
];

import { useOperationSessionStore } from '../../stores/useOperationSessionStore';

export function SidebarNav() {
  const isSessionActive = useOperationSessionStore((state) => state.activeSession !== null);

  return (
    <nav className="w-64 bg-white/5 backdrop-blur-xl border-r border-white/10 flex flex-col h-full z-10 relative shadow-[4px_0_24px_rgba(0,0,0,0.2)] pt-10">
      <div className="p-6">
        <div className="flex items-center gap-3 mb-8">
          <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-[#7C3AED] to-[#A78BFA] flex items-center justify-center shadow-[0_0_15px_rgba(124,58,237,0.4)]">
            <Zap className="w-5 h-5 text-white" />
          </div>
          <h1 className="text-xl font-bold text-white tracking-wide">DeepEye</h1>
        </div>

        <div className="flex flex-col gap-2">
          {routes.map((route) => (
            <NavLink
              key={route.path}
              to={route.path}
              className={({ isActive }) => `
                flex items-center gap-3 px-4 py-3 rounded-xl font-medium transition-all duration-300 relative
                ${
                  isActive
                    ? 'bg-gradient-to-r from-white/10 to-transparent text-white border border-white/10 shadow-[inset_2px_0_0_#A78BFA]'
                    : 'text-gray-400 hover:text-white hover:bg-white/5'
                }
              `}
            >
              <route.icon className="w-5 h-5" />
              <span className="flex-1">{route.label}</span>
              {route.path === '/logs' && isSessionActive && (
                <span className="absolute right-4 w-2 h-2 rounded-full bg-blue-500 shadow-[0_0_8px_rgba(59,130,246,0.8)] animate-pulse" />
              )}
            </NavLink>
          ))}
        </div>
      </div>

      <div className="mt-auto p-6">
        <div className="p-4 rounded-xl bg-[#0B0D17]/50 border border-white/5">
          <div className="text-xs text-gray-500 uppercase tracking-wider mb-1">Status</div>
          <div className="flex items-center gap-2">
            <div className="w-2 h-2 rounded-full bg-green-500 shadow-[0_0_8px_rgba(34,197,94,0.6)]"></div>
            <span className="text-sm text-gray-300">Agent Online</span>
          </div>
        </div>
      </div>
    </nav>
  );
}
