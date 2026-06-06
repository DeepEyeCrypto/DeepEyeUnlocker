import { useState } from 'react';
import { GeneralTab } from '../components/settings/GeneralTab';
import { LicenseTab } from '../components/settings/LicenseTab';
import { AboutTab } from '../components/settings/AboutTab';
import { Settings, Shield, Info } from 'lucide-react';

export function SettingsScreen() {
  const [activeTab, setActiveTab] = useState<'general' | 'license' | 'about'>('general');

  const tabs = [
    { id: 'general', label: 'General Settings', icon: Settings },
    { id: 'license', label: 'License & Access', icon: Shield },
    { id: 'about', label: 'Updates & About', icon: Info },
  ] as const;

  return (
    <div className="flex flex-col gap-8 animate-in fade-in slide-in-from-bottom-4 duration-500 pb-12">
      {/* Header */}
      <div className="flex flex-col gap-2">
        <h2 className="text-3xl font-bold text-white tracking-tight">Settings</h2>
        <p className="text-gray-400">Configure desktop bridge and application preferences.</p>
      </div>

      {/* Tabs */}
      <div className="flex items-center gap-2 border-b border-white/10">
        {tabs.map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            className={`flex items-center gap-2 px-4 py-3 text-sm font-semibold border-b-2 transition-colors ${
              activeTab === tab.id
                ? 'border-[#7C3AED] text-white'
                : 'border-transparent text-gray-500 hover:text-gray-300'
            }`}
          >
            <tab.icon className="w-4 h-4" />
            {tab.label}
          </button>
        ))}
      </div>

      {/* Content */}
      <div className="min-h-[400px]">
        {activeTab === 'general' && <GeneralTab />}
        {activeTab === 'license' && <LicenseTab />}
        {activeTab === 'about' && <AboutTab />}
      </div>
    </div>
  );
}
