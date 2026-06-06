import { useSettingsStore } from '../../stores/useSettingsStore';

export function GeneralTab() {
  const { settings, updateSettings } = useSettingsStore();

  return (
    <div className="flex flex-col gap-6 animate-in fade-in duration-300">
      {/* UI Settings */}
      <div className="p-5 rounded-2xl bg-white/[0.02] border border-white/5 space-y-4">
        <h3 className="text-lg font-bold text-white mb-2">Appearance & Behavior</h3>

        <div className="flex items-center justify-between">
          <div>
            <div className="text-sm font-semibold text-gray-200">Theme</div>
            <div className="text-xs text-gray-500">Choose your preferred visual style</div>
          </div>
          <select
            value={settings.theme}
            onChange={(e) => updateSettings({ theme: e.target.value as any })}
            className="bg-black/50 border border-white/10 rounded-lg px-3 py-1.5 text-sm text-white focus:outline-none focus:border-[#7C3AED]"
          >
            <option value="dark">Dark</option>
            <option value="light">Light</option>
            <option value="system">System</option>
          </select>
        </div>

        <div className="flex items-center justify-between">
          <div>
            <div className="text-sm font-semibold text-gray-200">Language</div>
            <div className="text-xs text-gray-500">Interface language</div>
          </div>
          <select
            value={settings.language}
            onChange={(e) => updateSettings({ language: e.target.value as any })}
            className="bg-black/50 border border-white/10 rounded-lg px-3 py-1.5 text-sm text-white focus:outline-none focus:border-[#7C3AED]"
          >
            <option value="en">English</option>
            <option value="ar">العربية (Arabic)</option>
            <option value="tr">Türkçe (Turkish)</option>
            <option value="zh">中文 (Chinese)</option>
          </select>
        </div>
      </div>

      {/* Operation Settings */}
      <div className="p-5 rounded-2xl bg-white/[0.02] border border-white/5 space-y-4">
        <h3 className="text-lg font-bold text-white mb-2">Operations & Safety</h3>

        <label className="flex items-center justify-between cursor-pointer group">
          <div>
            <div className="text-sm font-semibold text-gray-200 group-hover:text-white transition-colors">
              Confirm Dangerous Actions
            </div>
            <div className="text-xs text-gray-500">Show prompt before wiping or bypassing</div>
          </div>
          <div className="relative">
            <input
              type="checkbox"
              className="sr-only peer"
              checked={settings.confirmDangerousActions}
              onChange={(e) => updateSettings({ confirmDangerousActions: e.target.checked })}
            />
            <div className="w-11 h-6 bg-white/10 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-[#7C3AED]"></div>
          </div>
        </label>

        <label className="flex items-center justify-between cursor-pointer group">
          <div>
            <div className="text-sm font-semibold text-gray-200 group-hover:text-white transition-colors">
              Show Risk Badges
            </div>
            <div className="text-xs text-gray-500">
              Display iCloud/MDM risk warnings on dashboard
            </div>
          </div>
          <div className="relative">
            <input
              type="checkbox"
              className="sr-only peer"
              checked={settings.showRiskBadges}
              onChange={(e) => updateSettings({ showRiskBadges: e.target.checked })}
            />
            <div className="w-11 h-6 bg-white/10 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-[#7C3AED]"></div>
          </div>
        </label>

        <label className="flex items-center justify-between cursor-pointer group">
          <div>
            <div className="text-sm font-semibold text-gray-200 group-hover:text-white transition-colors">
              Send Anonymous Diagnostics
            </div>
            <div className="text-xs text-gray-500">Help improve DeepEyeUnlocker</div>
          </div>
          <div className="relative">
            <input
              type="checkbox"
              className="sr-only peer"
              checked={settings.sendAnonymousDiagnostics}
              onChange={(e) => updateSettings({ sendAnonymousDiagnostics: e.target.checked })}
            />
            <div className="w-11 h-6 bg-white/10 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-[#7C3AED]"></div>
          </div>
        </label>
      </div>
    </div>
  );
}
