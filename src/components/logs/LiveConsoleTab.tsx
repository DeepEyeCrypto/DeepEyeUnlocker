import { useState, useMemo } from 'react';
import { useOperationSessionStore } from '../../stores/useOperationSessionStore';
import { LogLevel } from '../../lib/session-types';
import { LevelFilterBar } from './LevelFilterBar';
import { LogLineList } from './LogLineList';

const ALL_LEVELS: LogLevel[] = ['debug', 'info', 'success', 'warn', 'error'];

export function LiveConsoleTab() {
  const activeSession = useOperationSessionStore((state) => state.activeSession);
  const [activeLevels, setActiveLevels] = useState<Set<LogLevel>>(
    new Set(['info', 'success', 'warn', 'error']),
  );

  const handleToggleLevel = (lvl: LogLevel) => {
    setActiveLevels((prev) => {
      const next = new Set(prev);
      if (next.has(lvl)) next.delete(lvl);
      else next.add(lvl);
      return next;
    });
  };

  const logs = activeSession?.logs ?? [];

  const filteredLogs = useMemo(() => {
    return logs.filter((l) => activeLevels.has(l.level));
  }, [logs, activeLevels]);

  return (
    <div className="flex flex-col h-full bg-[#0a0a0a] rounded-lg border border-white/10 overflow-hidden">
      <div className="flex items-center justify-between px-4 py-3 border-b border-white/5 bg-white/5">
        <h3 className="font-semibold text-white">Live Console</h3>
        {activeSession && (
          <div className="text-xs font-mono text-gray-400">
            Session:{' '}
            <span className="text-blue-400">
              {typeof activeSession.operationType === 'string'
                ? activeSession.operationType
                : activeSession.operationType.customCommand}
            </span>
          </div>
        )}
      </div>

      <LevelFilterBar
        levels={ALL_LEVELS}
        activeLevels={activeLevels}
        onToggle={handleToggleLevel}
      />

      <LogLineList logs={filteredLogs} />
    </div>
  );
}
