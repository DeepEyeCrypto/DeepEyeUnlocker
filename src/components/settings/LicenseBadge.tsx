import type { LicenseStatus } from '../../lib/settings-types';
import { Shield, ShieldAlert, ShieldCheck } from 'lucide-react';

interface LicenseBadgeProps {
  status: LicenseStatus | null;
}

export function LicenseBadge({ status }: LicenseBadgeProps) {
  if (!status) return null;

  const type = status.licenseType;
  let bg = 'bg-gray-500/10 border-gray-500/20 text-gray-400';
  let icon = <Shield className="w-4 h-4" />;

  if (type === 'pro') {
    bg = 'bg-[#7C3AED]/10 border-[#7C3AED]/20 text-[#A78BFA]';
    icon = <ShieldCheck className="w-4 h-4" />;
  } else if (type === 'trial') {
    bg = 'bg-blue-500/10 border-blue-500/20 text-blue-400';
    icon = <ShieldCheck className="w-4 h-4" />;
  } else if (type === 'expired') {
    bg = 'bg-red-500/10 border-red-500/20 text-red-400';
    icon = <ShieldAlert className="w-4 h-4" />;
  }

  return (
    <div className={`inline-flex items-center gap-2 px-3 py-1.5 rounded-lg border ${bg}`}>
      {icon}
      <span className="text-xs font-bold uppercase tracking-wider">{type} License</span>
      {type === 'trial' && status.daysRemaining && (
        <span className="text-[10px] opacity-80">({status.daysRemaining} days left)</span>
      )}
    </div>
  );
}
