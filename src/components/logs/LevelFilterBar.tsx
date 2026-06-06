import { LogLevel } from '../../lib/session-types';

interface LevelFilterBarProps {
  levels: LogLevel[];
  activeLevels: Set<LogLevel>;
  onToggle: (level: LogLevel) => void;
}

const levelColors: Record<LogLevel, string> = {
  info: 'bg-blue-500/10 text-blue-400 border-blue-500/20 hover:bg-blue-500/20',
  warn: 'bg-yellow-500/10 text-yellow-400 border-yellow-500/20 hover:bg-yellow-500/20',
  error: 'bg-red-500/10 text-red-400 border-red-500/20 hover:bg-red-500/20',
  success: 'bg-green-500/10 text-green-400 border-green-500/20 hover:bg-green-500/20',
  debug: 'bg-gray-500/10 text-gray-400 border-gray-500/20 hover:bg-gray-500/20',
};

export function LevelFilterBar({ levels, activeLevels, onToggle }: LevelFilterBarProps) {
  return (
    <div className="flex items-center space-x-2 py-2 px-4 bg-gray-900 border-b border-white/5">
      <span className="text-xs text-gray-500 mr-2 font-mono">Filters:</span>
      {levels.map((lvl) => {
        const isActive = activeLevels.has(lvl);
        const colorClass = isActive
          ? levelColors[lvl]
          : 'bg-transparent text-gray-600 border-gray-800 hover:text-gray-400';

        return (
          <button
            key={lvl}
            onClick={() => onToggle(lvl)}
            className={`px-3 py-1 text-xs font-mono rounded border transition-colors ${colorClass}`}
          >
            {lvl.toUpperCase()}
          </button>
        );
      })}
    </div>
  );
}
