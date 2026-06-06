import { useNavigate } from 'react-router-dom';
import { useCurrentDevice, useDeviceConnection } from '../hooks/useDevice';
import { useOperationSessionStore } from '../stores/useOperationSessionStore';
import { useAppStore } from '../stores/useAppStore';
import { useLicenseStore } from '../stores/useLicenseStore';
import { OperationType } from '../lib/session-types';
import {
  Smartphone,
  Wifi,
  WifiOff,
  Shield,
  ShieldAlert,
  Zap,
  Activity,
  Terminal,
  Clock,
  Hash,
  Cpu,
  AlertTriangle,
  CheckCircle2,
  XCircle,
  Loader2,
  RefreshCw,
  Signal,
  Key,
  Wrench,
} from 'lucide-react';
import { PreflightResultPanel } from '../components/session/PreflightResultPanel';

export function DashboardScreen() {
  const device = useCurrentDevice();
  const { connectionState, isScanning, refresh } = useDeviceConnection();
  const activeSession = useOperationSessionStore((state) => state.activeSession);
  const startSession = useOperationSessionStore((state) => state.startSession);
  const isStarting = useOperationSessionStore((state) => state.isStarting);
  const logs = useAppStore((state) => state.logs);
  const licenseStatus = useLicenseStore((state) => state.status);
  const licenseType = licenseStatus?.licenseType.toUpperCase() || 'FREE';
  const navigate = useNavigate();

  const handleQuickAction = async (opType: OperationType) => {
    try {
      await startSession(opType);
    } catch (err) {
      console.error(err);
    }
  };

  const recentLogs = logs.slice(-6);

  return (
    <div className="flex flex-col gap-8 animate-in fade-in slide-in-from-bottom-4 duration-500">
      {/* Header */}
      <div className="flex flex-col gap-2">
        <h2 className="text-3xl font-bold text-white tracking-tight">Dashboard</h2>
        <p className="text-gray-400">Real-time system overview and device command center.</p>
      </div>

      {/* Top Stat Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        {/* Connection Status Card */}
        <div
          className={`relative p-5 rounded-2xl border backdrop-blur-xl overflow-hidden transition-all duration-500 ${
            connectionState === 'connected'
              ? 'bg-green-500/5 border-green-500/20 shadow-[0_0_30px_rgba(34,197,94,0.08)]'
              : connectionState === 'detecting'
                ? 'bg-blue-500/5 border-blue-500/20 shadow-[0_0_30px_rgba(59,130,246,0.08)]'
                : connectionState === 'unstable'
                  ? 'bg-yellow-500/5 border-yellow-500/20'
                  : 'bg-white/[0.03] border-white/10'
          }`}
        >
          <div
            className="absolute top-0 right-0 w-24 h-24 rounded-full blur-[60px] pointer-events-none opacity-30 -translate-y-1/2 translate-x-1/2"
            style={{
              background:
                connectionState === 'connected'
                  ? '#22c55e'
                  : connectionState === 'detecting'
                    ? '#3b82f6'
                    : '#6b7280',
            }}
          />
          <div className="flex items-center gap-3 mb-3">
            {connectionState === 'connected' ? (
              <div className="p-2 bg-green-500/15 rounded-lg">
                <Wifi className="w-5 h-5 text-green-400" />
              </div>
            ) : connectionState === 'detecting' ? (
              <div className="p-2 bg-blue-500/15 rounded-lg">
                <Loader2 className="w-5 h-5 text-blue-400 animate-spin" />
              </div>
            ) : connectionState === 'unstable' ? (
              <div className="p-2 bg-yellow-500/15 rounded-lg">
                <AlertTriangle className="w-5 h-5 text-yellow-400" />
              </div>
            ) : (
              <div className="p-2 bg-white/5 rounded-lg">
                <WifiOff className="w-5 h-5 text-gray-500" />
              </div>
            )}
            <span className="text-xs font-bold uppercase tracking-widest text-gray-400">
              Connection
            </span>
          </div>
          <div className="flex items-center justify-between">
            <span
              className={`text-2xl font-bold tracking-tight ${
                connectionState === 'connected'
                  ? 'text-green-400'
                  : connectionState === 'detecting'
                    ? 'text-blue-400'
                    : connectionState === 'unstable'
                      ? 'text-yellow-400'
                      : 'text-gray-500'
              }`}
            >
              {connectionState === 'connected'
                ? 'Online'
                : connectionState === 'detecting'
                  ? 'Scanning'
                  : connectionState === 'unstable'
                    ? 'Unstable'
                    : 'Offline'}
            </span>
            <button
              onClick={() => void refresh()}
              disabled={isScanning}
              className="p-1.5 hover:bg-white/5 rounded-lg text-gray-400 hover:text-white transition-colors disabled:opacity-40"
              title="Force refresh"
            >
              <RefreshCw className={`w-4 h-4 ${isScanning ? 'animate-spin' : ''}`} />
            </button>
          </div>
        </div>

        {/* License Card */}
        <div className="relative p-5 rounded-2xl bg-[#7C3AED]/5 border border-[#7C3AED]/20 backdrop-blur-xl overflow-hidden shadow-[0_0_30px_rgba(124,58,237,0.06)]">
          <div className="absolute top-0 right-0 w-24 h-24 bg-[#7C3AED] rounded-full blur-[60px] pointer-events-none opacity-20 -translate-y-1/2 translate-x-1/2" />
          <div className="flex items-center gap-3 mb-3">
            <div className="p-2 bg-[#7C3AED]/15 rounded-lg">
              <Shield className="w-5 h-5 text-[#A78BFA]" />
            </div>
            <span className="text-xs font-bold uppercase tracking-widest text-gray-400">
              License
            </span>
          </div>
          <span className="text-2xl font-bold text-[#A78BFA] tracking-tight">{licenseType}</span>
        </div>

        {/* Session Card */}
        <div
          className={`relative p-5 rounded-2xl border backdrop-blur-xl overflow-hidden transition-all duration-500 ${
            activeSession
              ? 'bg-blue-500/5 border-blue-500/20 shadow-[0_0_30px_rgba(59,130,246,0.08)]'
              : 'bg-white/[0.03] border-white/10'
          }`}
        >
          <div className="absolute top-0 right-0 w-24 h-24 bg-blue-500 rounded-full blur-[60px] pointer-events-none opacity-10 -translate-y-1/2 translate-x-1/2" />
          <div className="flex items-center gap-3 mb-3">
            <div className={`p-2 rounded-lg ${activeSession ? 'bg-blue-500/15' : 'bg-white/5'}`}>
              <Activity
                className={`w-5 h-5 ${activeSession ? 'text-blue-400 animate-pulse' : 'text-gray-500'}`}
              />
            </div>
            <span className="text-xs font-bold uppercase tracking-widest text-gray-400">
              Session
            </span>
          </div>
          {activeSession ? (
            <div className="flex items-center gap-2">
              <span className="text-lg font-bold text-blue-400 tracking-tight">
                {typeof activeSession.operationType === 'string'
                  ? activeSession.operationType.replace(/([A-Z])/g, ' $1').trim()
                  : (activeSession.operationType as any).customCommand}
              </span>
              <span
                className={`px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider ${
                  activeSession.status === 'running'
                    ? 'bg-blue-500/20 text-blue-400 animate-pulse'
                    : activeSession.status === 'completed'
                      ? 'bg-green-500/20 text-green-400'
                      : activeSession.status === 'failed'
                        ? 'bg-red-500/20 text-red-400'
                        : 'bg-gray-500/20 text-gray-400'
                }`}
              >
                {activeSession.status}
              </span>
            </div>
          ) : (
            <span className="text-2xl font-bold text-gray-500 tracking-tight">Idle</span>
          )}
        </div>
      </div>

      {/* Device Info Panel */}
      {device && connectionState === 'connected' && (
        <div className="relative p-6 rounded-2xl bg-white/[0.03] border border-white/10 backdrop-blur-xl overflow-hidden shadow-[0_16px_48px_rgba(0,0,0,0.15)] animate-in fade-in slide-in-from-bottom-2 duration-300">
          <div className="absolute top-0 left-0 w-full h-px bg-gradient-to-r from-transparent via-[#7C3AED]/40 to-transparent" />

          <div className="flex items-center gap-3 mb-5">
            <div className="p-2.5 bg-[#7C3AED]/10 rounded-xl border border-[#7C3AED]/20">
              <Smartphone className="w-5 h-5 text-[#A78BFA]" />
            </div>
            <div>
              <h3 className="text-lg font-bold text-white tracking-tight">{device.model}</h3>
              <span className="text-xs text-gray-500 font-mono select-all">{device.id}</span>
            </div>
            <div className="ml-auto flex items-center gap-2">
              <span
                className={`px-3 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider border ${
                  device.isSupported
                    ? 'bg-green-500/10 text-green-400 border-green-500/20'
                    : 'bg-red-500/10 text-red-400 border-red-500/20'
                }`}
              >
                {device.isSupported ? 'Supported' : 'Unsupported'}
              </span>
              <span className="px-3 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider bg-[#7C3AED]/10 text-[#A78BFA] border border-[#7C3AED]/20">
                {device.mode}
              </span>
            </div>
          </div>

          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div className="flex items-center gap-2.5 p-3 rounded-xl bg-white/[0.03] border border-white/5">
              <Cpu className="w-4 h-4 text-gray-400 flex-shrink-0" />
              <div>
                <div className="text-[10px] text-gray-500 uppercase tracking-wider">Platform</div>
                <div className="text-sm font-semibold text-white">
                  {device.platform.toUpperCase()}
                </div>
              </div>
            </div>
            <div className="flex items-center gap-2.5 p-3 rounded-xl bg-white/[0.03] border border-white/5">
              <Hash className="w-4 h-4 text-gray-400 flex-shrink-0" />
              <div>
                <div className="text-[10px] text-gray-500 uppercase tracking-wider">Serial</div>
                <div className="text-sm font-semibold text-white font-mono">
                  {device.serial || '—'}
                </div>
              </div>
            </div>
            <div className="flex items-center gap-2.5 p-3 rounded-xl bg-white/[0.03] border border-white/5">
              <Shield className="w-4 h-4 text-gray-400 flex-shrink-0" />
              <div>
                <div className="text-[10px] text-gray-500 uppercase tracking-wider">iOS</div>
                <div className="text-sm font-semibold text-white">{device.osVersion || '—'}</div>
              </div>
            </div>
            <div className="flex items-center gap-2.5 p-3 rounded-xl bg-white/[0.03] border border-white/5">
              <Zap className="w-4 h-4 text-gray-400 flex-shrink-0" />
              <div>
                <div className="text-[10px] text-gray-500 uppercase tracking-wider">Chipset</div>
                <div className="text-sm font-semibold text-white">{device.chipset || '—'}</div>
              </div>
            </div>
          </div>

          {/* Risk Flags */}
          {device.riskFlags.length > 0 && (
            <div className="mt-4 flex flex-wrap gap-2">
              {device.riskFlags.map((flag) => (
                <span
                  key={flag}
                  className="flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-yellow-500/10 border border-yellow-500/20 text-[10px] font-bold uppercase tracking-wider text-yellow-400"
                >
                  <AlertTriangle className="w-3 h-3" />
                  {flag.replace(/([A-Z])/g, ' $1').trim()}
                </span>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Disconnected state */}
      {connectionState === 'disconnected' && (
        <div className="relative p-8 rounded-2xl bg-white/[0.02] border border-dashed border-white/10 backdrop-blur-xl text-center animate-in fade-in duration-500">
          <WifiOff className="w-10 h-10 text-gray-600 mx-auto mb-4" />
          <h3 className="text-lg font-semibold text-gray-400 mb-2">No Device Connected</h3>
          <p className="text-sm text-gray-500 max-w-md mx-auto mb-5">
            Connect an iOS or Android device via USB to begin diagnostics, activation bypasses, and
            exploit chains.
          </p>
          <button
            onClick={() => void refresh()}
            disabled={isScanning}
            className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-[#7C3AED] hover:bg-[#6D28D9] text-white text-sm font-semibold transition shadow-lg shadow-[#7C3AED]/20 disabled:opacity-50"
          >
            <RefreshCw className={`w-4 h-4 ${isScanning ? 'animate-spin' : ''}`} />
            Scan for Devices
          </button>
        </div>
      )}

      {/* Quick Actions */}
      {connectionState === 'connected' && (
        <div className="animate-in fade-in slide-in-from-bottom-2 duration-500 delay-150">
          <h3 className="text-sm font-bold uppercase tracking-widest text-gray-400 mb-4">
            Quick Actions
          </h3>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {[
              {
                icon: Signal,
                label: 'Hello Bypass',
                type: 'HelloActivation' as OperationType,
                color: 'text-purple-400',
                bg: 'bg-purple-500/10 hover:bg-purple-500/20 border-purple-500/20',
              },
              {
                icon: ShieldAlert,
                label: 'Exit Recovery',
                type: 'RecoveryExit' as OperationType,
                color: 'text-green-400',
                bg: 'bg-green-500/10 hover:bg-green-500/20 border-green-500/20',
              },
              {
                icon: Wrench,
                label: 'DFU Assist',
                type: 'DfuAssist' as OperationType,
                color: 'text-blue-400',
                bg: 'bg-blue-500/10 hover:bg-blue-500/20 border-blue-500/20',
              },
              {
                icon: Key,
                label: 'Reboot',
                type: 'Reboot' as OperationType,
                color: 'text-orange-400',
                bg: 'bg-orange-500/10 hover:bg-orange-500/20 border-orange-500/20',
              },
            ].map((action) => (
              <button
                key={action.label}
                onClick={() => handleQuickAction(action.type)}
                disabled={isStarting || !!activeSession}
                className={`group flex items-center gap-3 p-4 rounded-xl border backdrop-blur-md transition-all duration-300 ${action.bg} disabled:opacity-40 disabled:cursor-not-allowed hover:-translate-y-0.5`}
              >
                <action.icon className={`w-5 h-5 ${action.color}`} />
                <span className="text-sm font-semibold text-white">{action.label}</span>
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Active Session Steps (compact inline) */}
      {activeSession && activeSession.steps.length > 0 && (
        <div className="p-5 rounded-2xl bg-white/[0.03] border border-white/10 backdrop-blur-xl animate-in fade-in slide-in-from-bottom-2 duration-300">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-bold uppercase tracking-widest text-gray-400">
              Active Pipeline
            </h3>
            <span
              className={`px-2.5 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider ${
                activeSession.status === 'running'
                  ? 'bg-blue-500/20 text-blue-400 animate-pulse'
                  : activeSession.status === 'completed'
                    ? 'bg-green-500/20 text-green-400'
                    : activeSession.status === 'failed'
                      ? 'bg-red-500/20 text-red-400'
                      : 'bg-gray-500/20 text-gray-400'
              }`}
            >
              {activeSession.status}
            </span>
          </div>

          <div className="flex items-center gap-3">
            {activeSession.steps.map((step, idx) => (
              <div key={step.id} className="flex items-center gap-2">
                {step.status === 'done' ? (
                  <CheckCircle2 className="w-5 h-5 text-green-400" />
                ) : step.status === 'running' ? (
                  <Loader2 className="w-5 h-5 text-blue-400 animate-spin" />
                ) : step.status === 'failed' ? (
                  <XCircle className="w-5 h-5 text-red-400" />
                ) : (
                  <div className="w-5 h-5 rounded-full border border-white/20" />
                )}
                <span
                  className={`text-xs font-medium ${
                    step.status === 'done'
                      ? 'text-green-400'
                      : step.status === 'running'
                        ? 'text-blue-400'
                        : step.status === 'failed'
                          ? 'text-red-400'
                          : 'text-gray-500'
                  }`}
                >
                  {step.label}
                </span>
                {idx < activeSession.steps.length - 1 && (
                  <div
                    className={`w-8 h-px ${step.status === 'done' ? 'bg-green-500/40' : 'bg-white/10'}`}
                  />
                )}
              </div>
            ))}
          </div>

          {activeSession.preflight && (
            <div className="mt-4 border-t border-white/10 pt-4">
              <PreflightResultPanel preflight={activeSession.preflight} />
            </div>
          )}
        </div>
      )}

      {/* Recent Activity Feed */}
      <div className="animate-in fade-in slide-in-from-bottom-2 duration-500 delay-300">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-sm font-bold uppercase tracking-widest text-gray-400">
            Recent Activity
          </h3>
          <div className="flex items-center gap-1.5 text-gray-500">
            <Clock className="w-3.5 h-3.5" />
            <span className="text-[10px] font-semibold uppercase tracking-wider">
              {logs.length} entries
            </span>
          </div>
        </div>

        <div className="rounded-2xl bg-[#04060C]/60 border border-white/5 overflow-hidden shadow-[inset_0_2px_12px_rgba(0,0,0,0.4)]">
          <div className="p-4 flex items-center gap-2 border-b border-white/5 bg-white/[0.02]">
            <Terminal className="w-4 h-4 text-gray-500" />
            <span className="text-xs font-semibold text-gray-400 uppercase tracking-wider">
              Live Feed
            </span>
          </div>
          <div className="p-4 font-mono text-xs leading-relaxed max-h-48 overflow-y-auto">
            {recentLogs.length === 0 ? (
              <div className="text-gray-600 italic">Waiting for events...</div>
            ) : (
              recentLogs.map((log) => (
                <div key={log.id} className="flex gap-3 mb-1.5">
                  <span className="text-gray-600 select-none shrink-0">
                    [{new Date(log.timestamp).toLocaleTimeString()}]
                  </span>
                  <span
                    className={`${
                      log.level === 'error'
                        ? 'text-red-400 font-semibold'
                        : log.level === 'warn'
                          ? 'text-yellow-400'
                          : log.level === 'success'
                            ? 'text-green-400'
                            : 'text-gray-300'
                    }`}
                  >
                    {log.message}
                  </span>
                </div>
              ))
            )}
          </div>
          <button
            onClick={() => navigate('/logs')}
            className="w-full text-center p-3 text-xs font-semibold text-blue-400 hover:text-blue-300 hover:bg-white/5 transition-colors border-t border-white/5"
          >
            View full logs →
          </button>
        </div>
      </div>
    </div>
  );
}
