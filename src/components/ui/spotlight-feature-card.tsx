import { GlowCard } from './spotlight-card';
import { ReactNode } from 'react';

interface SpotlightFeatureCardProps {
  icon: ReactNode;
  title: string;
  description: string;
  onClick?: () => void;
  disabled?: boolean;
  glowColor?: 'blue' | 'purple' | 'green' | 'red' | 'orange';
  badge?: string;
  className?: string;
}

export function SpotlightFeatureCard({
  icon,
  title,
  description,
  onClick,
  disabled = false,
  glowColor = 'blue',
  badge,
  className = ''
}: SpotlightFeatureCardProps) {
  return (
    <button
      type="button"
      onClick={disabled ? undefined : onClick}
      disabled={disabled}
      aria-disabled={disabled}
      className={`tool-card w-full min-w-0 text-left transition-transform ${disabled ? 'cursor-not-allowed opacity-60' : 'cursor-pointer hover:scale-[1.02]'} ${className}`}
    >
      <GlowCard 
        glowColor={glowColor}
        customSize={true}
        className="tool-card h-full min-h-[120px] sm:min-h-[160px] md:min-h-[180px]"
      >
        <div className="flex flex-col gap-3" style={{ position: 'relative', zIndex: 20 }}>
          {/* Icon */}
          <div className="w-12 h-12 rounded-[0px] bg-white/10 flex items-center justify-center border border-white/10" style={{ backdropFilter: 'none', WebkitBackdropFilter: 'none' }}>
            {icon}
          </div>

          {/* Content */}
          <div className="flex-1">
            <div className="flex items-start justify-between gap-2 mb-2">
              <h3 className="text-white font-bold text-lg leading-tight">{title}</h3>
              {badge && (
                <span className="px-2 py-0.5 text-xs font-bold rounded-[0px] border border-white/20 bg-white/10 text-white whitespace-nowrap uppercase tracking-wider">
                  {badge}
                </span>
              )}
            </div>
            <p className="text-white/70 text-sm leading-relaxed">{description}</p>
          </div>
        </div>
      </GlowCard>
    </button>
  );
}
