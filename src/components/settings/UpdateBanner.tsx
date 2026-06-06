import { Download, CheckCircle, RefreshCw } from 'lucide-react';
import type { UpdateInfo } from '../../lib/settings-types';

interface UpdateBannerProps {
  info: UpdateInfo | null;
  onInstall: () => Promise<void>;
  isInstalling: boolean;
}

export function UpdateBanner({ info, onInstall, isInstalling }: UpdateBannerProps) {
  if (!info) return null;

  if (info.updateAvailable) {
    return (
      <div className="p-4 rounded-xl bg-blue-500/10 border border-blue-500/20 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h4 className="text-sm font-bold text-blue-400 mb-1">
            Update Available: v{info.latestVersion}
          </h4>
          <p className="text-xs text-gray-400 max-w-md">{info.releaseNotes}</p>
        </div>
        <button
          onClick={onInstall}
          disabled={isInstalling}
          className="shrink-0 flex items-center gap-2 px-4 py-2 bg-blue-500 hover:bg-blue-600 text-white text-sm font-semibold rounded-lg disabled:opacity-50 transition-colors"
        >
          {isInstalling ? (
            <RefreshCw className="w-4 h-4 animate-spin" />
          ) : (
            <Download className="w-4 h-4" />
          )}
          {isInstalling ? 'Installing...' : 'Install Update'}
        </button>
      </div>
    );
  }

  return (
    <div className="p-4 rounded-xl bg-green-500/5 border border-green-500/10 flex items-center gap-3">
      <CheckCircle className="w-5 h-5 text-green-400" />
      <div>
        <h4 className="text-sm font-bold text-green-400">Up to Date</h4>
        <p className="text-xs text-gray-500">
          You are running the latest version (v{info.currentVersion})
        </p>
      </div>
    </div>
  );
}
