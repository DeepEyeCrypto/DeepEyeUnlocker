import React from 'react';

interface ToolboxActionCardProps {
  title: string;
  description: string;
  icon: React.ReactNode;
  tag?: string;
  onClick: () => void;
  disabled?: boolean;
}

export function ToolboxActionCard({
  title,
  description,
  icon,
  tag,
  onClick,
  disabled,
}: ToolboxActionCardProps) {
  return (
    <div
      className={`group relative flex flex-col p-6 rounded-2xl backdrop-blur-xl bg-white/10 shadow-[0_32px_64px_rgba(0,0,0,0.4)] border border-white/10 hover:border-white/20 hover:bg-white/[0.15] transition-all duration-300 ${disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer hover:-translate-y-1'}`}
      onClick={disabled ? undefined : onClick}
    >
      <div className="absolute inset-0 bg-gradient-to-br from-white/5 to-transparent rounded-2xl pointer-events-none" />

      <div className="flex items-start justify-between mb-4 relative z-10">
        <div className="p-3 bg-white/5 rounded-xl border border-white/10 group-hover:bg-[#7C3AED]/20 group-hover:border-[#7C3AED]/50 transition-colors duration-300 shadow-inner">
          {icon}
        </div>
        {tag && (
          <span className="px-2.5 py-1 text-[10px] font-bold uppercase tracking-widest text-[#A78BFA] bg-[#7C3AED]/20 border border-[#7C3AED]/30 rounded-full">
            {tag}
          </span>
        )}
      </div>

      <div className="relative z-10">
        <h3 className="text-lg font-bold text-white mb-2 tracking-tight group-hover:text-blue-100 transition-colors">
          {title}
        </h3>
        <p className="text-sm text-gray-400 leading-relaxed group-hover:text-gray-300 transition-colors">
          {description}
        </p>
      </div>
    </div>
  );
}
