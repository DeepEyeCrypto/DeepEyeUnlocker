import { useDeviceStatusStore } from '../../stores/useDeviceStatusStore';
import { useOperationSessionStore } from '../../stores/useOperationSessionStore';
import { Activity } from 'lucide-react';

export function DiagnosticsCard() {
  const device = useDeviceStatusStore((s) => s.currentDevice);
  const startSession = useOperationSessionStore((s) => s.startSession);
  const isSessionActive = useOperationSessionStore((s) => s.activeSession !== null);

  const handleRefresh = () => {
    if (!isSessionActive) {
      startSession('DeviceCheck');
    }
  };

  if (!device) {
    return (
      <div className="flex flex-col p-6 rounded-2xl bg-white/5 border border-white/10 opacity-50">
        <div className="flex items-center gap-3 mb-4">
          <Activity className="w-5 h-5 text-gray-400" />
          <h3 className="text-lg font-bold text-gray-400">Diagnostics</h3>
        </div>
        <p className="text-sm text-gray-500">No device connected for diagnostics.</p>
      </div>
    );
  }

  return (
    <div className="flex flex-col p-6 rounded-2xl bg-white/5 border border-white/10 backdrop-blur-xl shadow-sm">
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-3">
          <div className="p-2 bg-blue-500/20 rounded-lg">
            <Activity className="w-5 h-5 text-blue-400" />
          </div>
          <h3 className="text-lg font-bold text-white">Device Diagnostics</h3>
        </div>
        <span className="px-2.5 py-1 text-[10px] font-bold tracking-widest text-green-400 bg-green-500/10 border border-green-500/20 rounded-full uppercase">
          Safe
        </span>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        <div className="flex flex-col">
          <span className="text-xs text-gray-500 uppercase font-semibold mb-1">Model</span>
          <span className="text-sm text-gray-200">{device.model || 'Unknown'}</span>
        </div>
        <div className="flex flex-col">
          <span className="text-xs text-gray-500 uppercase font-semibold mb-1">OS Version</span>
          <span className="text-sm text-gray-200">iOS {device.osVersion || 'Unknown'}</span>
        </div>
        <div className="flex flex-col">
          <span className="text-xs text-gray-500 uppercase font-semibold mb-1">Serial</span>
          <span className="text-sm text-gray-200">{device.serial || 'Unknown'}</span>
        </div>
        <div className="flex flex-col">
          <span className="text-xs text-gray-500 uppercase font-semibold mb-1">Mode</span>
          <span className="text-sm text-gray-200 capitalize">{device.mode}</span>
        </div>
      </div>

      <button
        onClick={handleRefresh}
        disabled={isSessionActive}
        className="self-start px-4 py-2 bg-white/10 hover:bg-white/15 text-sm text-white font-semibold rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
      >
        {isSessionActive ? 'Session Active...' : 'Refresh Data →'}
      </button>
    </div>
  );
}
