import { useEffect } from 'react';
import { useLicenseStore } from '../../stores/useLicenseStore';
import { LicenseBadge } from './LicenseBadge';
import { LicenseKeyInput } from './LicenseKeyInput';
import { FeatureList } from './FeatureList';
import { LogOut, Loader2 } from 'lucide-react';

export function LicenseTab() {
  const { status, isLoading, error, loadStatus, activate, deactivate } = useLicenseStore();

  useEffect(() => {
    loadStatus();
  }, [loadStatus]);

  if (isLoading && !status) {
    return (
      <div className="p-8 flex justify-center">
        <Loader2 className="w-6 h-6 text-[#7C3AED] animate-spin" />
      </div>
    );
  }

  const isPro =
    status?.isValid && status.licenseType !== 'free' && status.licenseType !== 'expired';

  return (
    <div className="flex flex-col gap-6 animate-in fade-in duration-300">
      <div className="p-6 rounded-2xl bg-white/[0.02] border border-white/5 relative overflow-hidden">
        {isPro && (
          <div className="absolute -right-10 -top-10 w-40 h-40 bg-[#7C3AED] rounded-full blur-[80px] opacity-20 pointer-events-none" />
        )}

        <div className="flex items-start justify-between mb-8 relative z-10">
          <div>
            <h3 className="text-xl font-bold text-white mb-2">License Status</h3>
            <LicenseBadge status={status} />
          </div>
          {isPro && (
            <button
              onClick={() => deactivate()}
              disabled={isLoading}
              className="flex items-center gap-2 px-3 py-1.5 text-xs font-semibold text-red-400 hover:text-red-300 hover:bg-red-400/10 rounded-lg transition-colors"
            >
              <LogOut className="w-3.5 h-3.5" />
              Deactivate
            </button>
          )}
        </div>

        {error && (
          <div className="mb-6 p-3 bg-red-500/10 border border-red-500/20 rounded-lg text-red-400 text-sm">
            {error}
          </div>
        )}

        {!isPro ? (
          <div className="max-w-md">
            <h4 className="text-sm font-semibold text-gray-300 mb-4">Activate PRO Features</h4>
            <LicenseKeyInput onActivate={activate} isLoading={isLoading} />
            <p className="mt-4 text-xs text-gray-500">
              Need a license?{' '}
              <a href="#" className="text-[#A78BFA] hover:underline">
                Purchase one here
              </a>
              .
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-8 pt-6 border-t border-white/10">
            <div>
              <h4 className="text-sm font-bold text-gray-400 uppercase tracking-widest mb-4">
                License Details
              </h4>
              <div className="space-y-3 text-sm">
                <div className="flex justify-between">
                  <span className="text-gray-500">Seat ID</span>
                  <span className="text-gray-300 font-mono">{status?.seatId || 'N/A'}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-500">Activated</span>
                  <span className="text-gray-300">
                    {status?.activatedAt
                      ? new Date(status.activatedAt).toLocaleDateString()
                      : 'N/A'}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-500">Last Checked</span>
                  <span className="text-gray-300">
                    {status?.lastValidatedAt
                      ? new Date(status.lastValidatedAt).toLocaleDateString()
                      : 'N/A'}
                  </span>
                </div>
              </div>
            </div>

            {status?.features && <FeatureList features={status.features} />}
          </div>
        )}
      </div>
    </div>
  );
}
