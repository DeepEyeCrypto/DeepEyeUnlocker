import { useEffect } from 'react';
import { useUpdateStore } from '../../stores/useUpdateStore';
import { useSettingsStore } from '../../stores/useSettingsStore';
import { UpdateBanner } from './UpdateBanner';
import { RefreshCw, Github, Globe, Heart } from 'lucide-react';

export function AboutTab() {
  const { info, isChecking, isInstalling, checkForUpdates, installUpdate } = useUpdateStore();
  const { settings, updateSettings } = useSettingsStore();

  useEffect(() => {
    if (settings.autoCheckUpdates && !info) {
      checkForUpdates();
    }
  }, [settings.autoCheckUpdates, info, checkForUpdates]);

  return (
    <div className="flex flex-col gap-6 animate-in fade-in duration-300">
      {/* Update Section */}
      <div className="p-6 rounded-2xl bg-white/[0.02] border border-white/5">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h3 className="text-lg font-bold text-white">Software Updates</h3>
            <p className="text-xs text-gray-500 mt-1">
              Current version: v{info?.currentVersion || '—'}
            </p>
          </div>
          <button
            onClick={checkForUpdates}
            disabled={isChecking}
            className="flex items-center gap-2 px-3 py-1.5 bg-white/5 hover:bg-white/10 text-white text-xs font-semibold rounded-lg disabled:opacity-50 transition-colors border border-white/10"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isChecking ? 'animate-spin' : ''}`} />
            Check Now
          </button>
        </div>

        <UpdateBanner info={info} onInstall={installUpdate} isInstalling={isInstalling} />

        <label className="flex items-center gap-3 mt-6 cursor-pointer">
          <input
            type="checkbox"
            checked={settings.autoCheckUpdates}
            onChange={(e) => updateSettings({ autoCheckUpdates: e.target.checked })}
            className="rounded bg-black/50 border-white/10 text-[#7C3AED] focus:ring-[#7C3AED] focus:ring-offset-black"
          />
          <span className="text-sm text-gray-300">Automatically check for updates on startup</span>
        </label>
      </div>

      {/* About Section */}
      <div className="p-6 rounded-2xl bg-white/[0.02] border border-white/5 text-center">
        <div className="w-16 h-16 bg-gradient-to-br from-[#7C3AED] to-blue-500 rounded-2xl mx-auto mb-4 flex items-center justify-center shadow-lg shadow-[#7C3AED]/20">
          <span className="text-2xl font-bold text-white">DE</span>
        </div>
        <h3 className="text-xl font-bold text-white mb-1">DeepEyeUnlocker</h3>
        <p className="text-sm text-gray-400 mb-6">
          Advanced Mobile Diagnostics & Exploitation Framework
        </p>

        <div className="flex items-center justify-center gap-4">
          <a
            href="#"
            className="p-2 bg-white/5 hover:bg-white/10 text-gray-400 hover:text-white rounded-lg transition-colors border border-white/10"
          >
            <Github className="w-5 h-5" />
          </a>
          <a
            href="#"
            className="p-2 bg-white/5 hover:bg-white/10 text-gray-400 hover:text-white rounded-lg transition-colors border border-white/10"
          >
            <Globe className="w-5 h-5" />
          </a>
          <a
            href="#"
            className="p-2 bg-white/5 hover:bg-white/10 text-gray-400 hover:text-pink-400 rounded-lg transition-colors border border-white/10"
          >
            <Heart className="w-5 h-5" />
          </a>
        </div>
      </div>
    </div>
  );
}
