import { useCurrentDevice, useDeviceConnection } from '../../hooks/useDevice';
import { Smartphone, Shield, Wifi, Hash, RefreshCw, AlertTriangle } from 'lucide-react';

export function DeviceStatusBar() {
  const device = useCurrentDevice();
  const { connectionState, isScanning, refresh } = useDeviceConnection();

  if (connectionState === 'disconnected' || !device) {
    return (
      <div className="h-12 bg-[#0B0D17]/90 border-t border-white/10 backdrop-blur-xl flex items-center justify-between px-6 relative z-10 shadow-[0_-4px_24px_rgba(0,0,0,0.2)]">
        <div className="flex items-center gap-2 text-gray-500">
          <div className="w-2 h-2 rounded-full bg-gray-500"></div>
          <span className="text-sm font-medium">Waiting for device connection...</span>
        </div>
        <button
          onClick={() => void refresh()}
          disabled={isScanning}
          className="p-1 hover:bg-white/5 rounded text-gray-400 hover:text-white transition-colors disabled:opacity-50"
          title="Manual refresh"
        >
          <RefreshCw className={`w-4 h-4 ${isScanning ? 'animate-spin' : ''}`} />
        </button>
      </div>
    );
  }

  if (connectionState === 'detecting') {
    return (
      <div className="h-12 bg-[#0B0D17]/90 border-t border-white/10 backdrop-blur-xl flex items-center justify-between px-6 relative z-10 shadow-[0_-4px_24px_rgba(0,0,0,0.2)] animate-pulse">
        <div className="flex items-center gap-2 text-[#A78BFA]">
          <div className="w-2 h-2 rounded-full bg-[#A78BFA] animate-ping"></div>
          <span className="text-sm font-medium">Detecting device profile...</span>
        </div>
      </div>
    );
  }

  if (connectionState === 'unstable') {
    return (
      <div className="h-12 bg-yellow-500/10 border-t border-yellow-500/30 backdrop-blur-xl flex items-center justify-between px-6 relative z-10 shadow-[0_-4px_24px_rgba(0,0,0,0.2)]">
        <div className="flex items-center gap-2 text-yellow-400">
          <AlertTriangle className="w-4 h-4" />
          <span className="text-sm font-medium">Connection unstable wiggles detected...</span>
        </div>
      </div>
    );
  }

  return (
    <div className="h-12 bg-[#0B0D17]/80 border-t border-white/10 backdrop-blur-xl flex items-center justify-between px-6 relative z-10 shadow-[0_-4px_24px_rgba(0,0,0,0.2)]">
      <div className="flex items-center gap-6">
        {/* Connection Status */}
        <div className="flex items-center gap-2">
          <div className="w-2 h-2 rounded-full bg-green-500 shadow-[0_0_8px_rgba(34,197,94,0.6)] animate-pulse"></div>
          <span className="text-sm font-medium text-green-400">Connected</span>
        </div>

        {/* Separator */}
        <div className="w-px h-4 bg-white/10"></div>

        {/* Device Model */}
        <div className="flex items-center gap-2 text-gray-300">
          <Smartphone className="w-4 h-4 text-gray-400" />
          <span className="text-sm font-medium">
            {device.model} ({device.platform.toUpperCase()})
          </span>
        </div>

        {/* UDID/Identifier */}
        <div className="flex items-center gap-2 text-gray-300">
          <Hash className="w-4 h-4 text-gray-400" />
          <span className="text-sm font-mono text-gray-400" title={device.id}>
            {device.id.substring(0, 16)}...
          </span>
        </div>
      </div>

      <div className="flex items-center gap-4">
        {/* Mode / Connection Type */}
        <div className="flex items-center gap-2 bg-white/5 px-3 py-1 rounded-md border border-white/10">
          <Wifi className="w-4 h-4 text-[#A78BFA]" />
          <span className="text-xs font-semibold text-[#A78BFA] uppercase tracking-wider">
            {device.mode}
          </span>
        </div>

        {/* Safety Indicator */}
        <div className="flex items-center gap-2">
          <Shield className="w-4 h-4 text-gray-400" />
          <span className="text-xs font-medium text-gray-400 uppercase">
            {device.isSupported ? 'Supported' : 'Unsupported'}
          </span>
        </div>

        {/* Manual scan refresh */}
        <button
          onClick={() => void refresh()}
          disabled={isScanning}
          className="p-1 hover:bg-white/5 rounded text-gray-400 hover:text-white transition-colors disabled:opacity-50"
          title="Force refresh"
        >
          <RefreshCw className={`w-4 h-4 ${isScanning ? 'animate-spin' : ''}`} />
        </button>
      </div>
    </div>
  );
}
