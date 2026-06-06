import { useState } from 'react';
import { LogsTopBar } from '../components/logs/LogsTopBar';
import { LiveConsoleTab } from '../components/logs/LiveConsoleTab';
import { SessionHistoryTab } from '../components/logs/SessionHistoryTab';

export function LogsScreen() {
  const [currentTab, setCurrentTab] = useState<'live' | 'history'>('live');

  return (
    <div className="flex flex-col h-full space-y-4">
      <LogsTopBar currentTab={currentTab} onTabChange={setCurrentTab} />

      <div className="flex-1 overflow-hidden">
        {currentTab === 'live' ? <LiveConsoleTab /> : <SessionHistoryTab />}
      </div>
    </div>
  );
}
