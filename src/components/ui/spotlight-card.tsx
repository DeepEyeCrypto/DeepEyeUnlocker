import React, { ReactNode } from 'react';

interface GlowCardProps {
  children: ReactNode;
  className?: string;
  glowColor?: 'blue' | 'purple' | 'green' | 'red' | 'orange';
  size?: 'sm' | 'md' | 'lg';
  width?: string | number;
  height?: string | number;
  customSize?: boolean;
}

const sizeMap = {
  sm: 'w-48 h-64',
  md: 'w-64 h-80',
  lg: 'w-80 h-96',
};

const GlowCard: React.FC<GlowCardProps> = ({
  children,
  className = '',
  glowColor = 'blue',
  size = 'md',
  width,
  height,
  customSize = false,
}) => {
  const getSizeClasses = () => {
    if (customSize) return '';
    return sizeMap[size];
  };

  const getInlineStyles = () => {
    const baseStyles: React.CSSProperties = {};
    if (width !== undefined) {
      baseStyles.width = typeof width === 'number' ? `${width}px` : width;
    }
    if (height !== undefined) {
      baseStyles.height = typeof height === 'number' ? `${height}px` : height;
    }
    return baseStyles;
  };

  // Map glowColor to the new CSS variables
  const getGlowClass = () => {
    switch (glowColor) {
      case 'blue':
        return 'hover:border-cyan-400/50 hover:shadow-[0_0_20px_rgba(0,212,255,0.2)]';
      case 'purple':
        return 'hover:border-purple-400/50 hover:shadow-[0_0_20px_rgba(176,0,255,0.2)]';
      case 'green':
        return 'hover:border-green-400/50 hover:shadow-[0_0_20px_rgba(0,255,102,0.2)]';
      case 'red':
        return 'hover:border-red-400/50 hover:shadow-[0_0_20px_rgba(255,0,0,0.2)]';
      case 'orange':
        return 'hover:border-orange-400/50 hover:shadow-[0_0_20px_rgba(255,106,0,0.2)]';
      default:
        return 'glass-hover';
    }
  };

  return (
    <div
      style={getInlineStyles()}
      className={`
        glass
        glass-hover
        ${getGlowClass()}
        ${getSizeClasses()}
        ${!customSize ? 'aspect-[3/4]' : ''}
        relative 
        grid 
        grid-rows-[1fr_auto] 
        p-4 
        gap-4 
        overflow-hidden
        max-w-full
        ${className}
      `}
    >
      {children}
    </div>
  );
};

export { GlowCard };
export type { GlowCardProps };
