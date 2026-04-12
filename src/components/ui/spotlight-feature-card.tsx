import { GlowCard } from './spotlight-card';
import { ReactNode } from 'react';

interface SpotlightFeatureCardProps {
  icon: ReactNode;
  title: string;
  description: string;
  onClick?: () => void;
  glowColor?: 'blue' | 'purple' | 'green' | 'red' | 'orange';
  badge?: string;
  className?: string;
}

export function SpotlightFeatureCard({
  icon,
  title,
  description,
  onClick,
  glowColor = 'blue',
  badge,
  className = ''
}: SpotlightFeatureCardProps) {
  return (
    <div onClick={onClick} className={`cursor-pointer transition-transform hover:scale-[1.02] ${className}`}>
      <GlowCard 
        glowColor={glowColor}
        customSize={true}
        className="h-full min-h-[180px]"
      >
        <div className="flex flex-col gap-3">
          {/* Icon */}
          <div className="w-12 h-12 rounded-xl bg-white/10 backdrop-blur-sm flex items-center justify-center">
            {icon}
          </div>

          {/* Content */}
          <div className="flex-1">
            <div className="flex items-start justify-between gap-2 mb-2">
              <h3 className="text-white font-bold text-lg leading-tight">{title}</h3>
              {badge && (
                <span className="px-2 py-0.5 text-xs font-bold rounded-full bg-white/20 text-white whitespace-nowrap">
                  {badge}
                </span>
              )}
            </div>
            <p className="text-white/70 text-sm leading-relaxed">{description}</p>
          </div>
        </div>
      </GlowCard>
    </div>
  );
}
