import { invoke } from '@tauri-apps/api/core';
import { exportLogBundle } from '../../lib/log-export';
import { useOperationSessionStore } from '../../stores/useOperationSessionStore';
import { useState } from 'react';

interface LogsTopBarProps {
  currentTab: 'live' | 'history';
  onTabChange: (tab: 'live' | 'history') => void;
}

export function LogsTopBar({ currentTab, onTabChange }: LogsTopBarProps) {
  const sessionHistory = useOperationSessionStore((state) => state.sessionHistory);
  const activeSession = useOperationSessionStore((state) => state.activeSession);
  const fetchHistory = useOperationSessionStore((state) => state.fetchHistory);
  const [isExporting, setIsExporting] = useState(false);

  const handleExport = (format: 'txt' | 'json') => {
    setIsExporting(true);
    try {
      const toExport = activeSession ? [activeSession, ...sessionHistory] : sessionHistory;
      if (toExport.length === 0) {
        alert('No sessions to export.');
        return;
      }
      exportLogBundle(toExport, format);
    } finally {
      setIsExporting(false);
    }
  };

  const handleClearHistory = async () => {
    if (
      !window.confirm('Are you sure you want to clear all session history? This cannot be undone.')
    )
      return;
    try {
      await invoke('clear_session_history');
      await fetchHistory();
    } catch (e) {
      console.error('Failed to clear history:', e);
      alert('Failed to clear history');
    }
  };

  return (
    <div className="flex items-center justify-between mb-6">
      <div className="flex items-center space-x-6">
        <h1 className="text-2xl font-bold tracking-tight text-white flex items-center">
          Logs & History
        </h1>
        <div className="flex bg-gray-900 rounded-lg p-1 border border-white/5">
          <button
            onClick={() => onTabChange('live')}
            className={`px-4 py-1.5 text-sm font-medium rounded-md transition-all ${
              currentTab === 'live'
                ? 'bg-blue-500 text-white shadow-sm'
                : 'text-gray-400 hover:text-white hover:bg-white/5'
            }`}
          >
            Live Console
          </button>
          <button
            onClick={() => onTabChange('history')}
            className={`px-4 py-1.5 text-sm font-medium rounded-md transition-all ${
              currentTab === 'history'
                ? 'bg-blue-500 text-white shadow-sm'
                : 'text-gray-400 hover:text-white hover:bg-white/5'
            }`}
          >
            Session History
          </button>
        </div>
      </div>

      <div className="flex items-center space-x-3">
        <div className="relative group">
          <button
            disabled={isExporting}
            className="px-4 py-2 bg-gray-800 hover:bg-gray-700 text-white text-sm font-medium rounded-lg transition-colors border border-white/10 flex items-center disabled:opacity-50"
          >
            <span className="mr-2">📥</span>
            Export Logs
          </button>

          {/* Dropdown for export formats */}
          <div className="absolute right-0 mt-2 w-36 bg-gray-900 border border-white/10 rounded-lg shadow-xl opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all z-50">
            <div className="p-1">
              <button
                onClick={() => handleExport('txt')}
                className="w-full text-left px-3 py-2 text-sm text-gray-300 hover:text-white hover:bg-gray-800 rounded-md transition-colors"
              >
                Export as TXT
              </button>
              <button
                onClick={() => handleExport('json')}
                className="w-full text-left px-3 py-2 text-sm text-gray-300 hover:text-white hover:bg-gray-800 rounded-md transition-colors"
              >
                Export as JSON
              </button>
            </div>
          </div>
        </div>

        {currentTab === 'history' && (
          <button
            onClick={handleClearHistory}
            className="px-4 py-2 bg-red-500/10 hover:bg-red-500/20 text-red-400 border border-red-500/20 text-sm font-medium rounded-lg transition-colors"
          >
            Clear History
          </button>
        )}
      </div>
    </div>
  );
}
