export type ButtonType = 'volumeDown' | 'side' | 'home' | 'sleepWake';

interface Props {
  variant: 'modern' | 'seven' | 'legacy';
  highlightedButtons: ButtonType[];
}

export function DfuPhoneSvg({ variant, highlightedButtons }: Props) {
  const isHighlighted = (b: ButtonType) => highlightedButtons.includes(b);

  const buttonColor = (b: ButtonType) => (isHighlighted(b) ? '#F87171' : '#374151'); // red-400 / gray-700
  const buttonOpacity = (b: ButtonType) => (isHighlighted(b) ? 1 : 0.5);

  return (
    <div className="w-32 h-64 relative flex items-center justify-center">
      <svg
        viewBox="0 0 100 200"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        className="w-full h-full drop-shadow-lg"
      >
        {/* Phone Body */}
        <rect
          x="10"
          y="10"
          width="80"
          height="180"
          rx="16"
          stroke="#4B5563"
          strokeWidth="2"
          fill="#111827"
        />

        {/* Screen */}
        <rect x="14" y="14" width="72" height="172" rx="12" fill="#030712" />

        {/* Buttons based on variant */}

        {/* Volume Up - never highlighted in DFU, but exists on all */}
        <rect x="6" y="50" width="4" height="16" rx="2" fill="#374151" opacity={0.5} />

        {/* Volume Down */}
        {(variant === 'modern' || variant === 'seven') && (
          <rect
            x="6"
            y="74"
            width="4"
            height="16"
            rx="2"
            fill={buttonColor('volumeDown')}
            opacity={buttonOpacity('volumeDown')}
          />
        )}

        {/* Side Button (Modern/Seven) vs Sleep/Wake (Legacy on top, but often we just draw it on side for simplicity, let's draw side button) */}
        {variant === 'modern' ? (
          <rect
            x="90"
            y="60"
            width="4"
            height="24"
            rx="2"
            fill={buttonColor('side')}
            opacity={buttonOpacity('side')}
          />
        ) : variant === 'seven' ? (
          <rect
            x="90"
            y="60"
            width="4"
            height="24"
            rx="2"
            fill={buttonColor('sleepWake')}
            opacity={buttonOpacity('sleepWake')}
          />
        ) : (
          /* Legacy Top Button */
          <rect
            x="70"
            y="6"
            width="16"
            height="4"
            rx="2"
            fill={buttonColor('sleepWake')}
            opacity={buttonOpacity('sleepWake')}
          />
        )}

        {/* Home Button (Legacy only) */}
        {variant === 'legacy' && (
          <circle
            cx="50"
            cy="170"
            r="8"
            stroke={buttonColor('home')}
            strokeWidth="2"
            fill="transparent"
            opacity={buttonOpacity('home')}
          />
        )}
      </svg>
    </div>
  );
}
