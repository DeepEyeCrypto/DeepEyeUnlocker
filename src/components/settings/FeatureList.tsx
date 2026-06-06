import { Check, X } from 'lucide-react';
import type { LicenseFeatureSet } from '../../lib/settings-types';

interface FeatureListProps {
  features: LicenseFeatureSet;
}

export function FeatureList({ features }: FeatureListProps) {
  const items = [
    {
      label: `Max Devices: ${features.maxDevicesPerSession > 10 ? 'Unlimited' : features.maxDevicesPerSession}`,
      enabled: true,
    },
    { label: 'Jailbreak Tools (checkra1n, palera1n)', enabled: features.canUseJailbreakTools },
    { label: 'Boot Files (Activation/Backup)', enabled: features.canUseBootFiles },
    { label: 'FMI OFF Tools', enabled: features.canUseFmiOff },
    { label: 'Export Logs & Reports', enabled: features.canExportLogs },
    { label: 'EDL Pipeline (Qualcomm)', enabled: features.canUseEdlPipeline },
    { label: 'MTK BROM Bypass', enabled: features.canUseMtkBrom },
  ];

  return (
    <div className="flex flex-col gap-2">
      <h4 className="text-sm font-bold text-gray-400 uppercase tracking-widest mb-2">
        Available Features
      </h4>
      {items.map((item, idx) => (
        <div key={idx} className="flex items-center gap-3">
          {item.enabled ? (
            <Check className="w-4 h-4 text-green-400 shrink-0" />
          ) : (
            <X className="w-4 h-4 text-gray-600 shrink-0" />
          )}
          <span
            className={`text-sm ${item.enabled ? 'text-gray-300' : 'text-gray-600 line-through'}`}
          >
            {item.label}
          </span>
        </div>
      ))}
    </div>
  );
}
