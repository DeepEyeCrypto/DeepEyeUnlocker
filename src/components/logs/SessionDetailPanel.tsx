import { OperationSession } from '../../lib/session-types';
import { SessionStepRow } from './SessionStepRow';
import { LogLineList } from './LogLineList';
import { useState } from 'react';

interface SessionDetailPanelProps {
  session: OperationSession | null;
  onClose: () => void;
}

export function SessionDetailPanel({ session, onClose }: SessionDetailPanelProps) {
  const [tab, setTab] = useState<'steps' | 'logs'>('steps');

  if (!session) return null;

  const snap = session.deviceSnapshotAtStart;
  const opStr =
    typeof session.operationType === 'string'
      ? session.operationType
      : session.operationType.customCommand;
  const isFailed = session.status === 'failed' || session.outcome === 'failed';

  return (
    <div className="absolute inset-0 bg-black/50 backdrop-blur-sm z-50 flex justify-end">
      <div className="w-full max-w-2xl bg-[#0f0f13] border-l border-white/10 h-full flex flex-col shadow-2xl animate-in slide-in-from-right duration-200">
        {/* Header */}
        <div className="px-6 py-4 border-b border-white/10 bg-white/[0.02] flex justify-between items-start">
          <div>
            <h2 className="text-xl font-bold text-white mb-1">{opStr}</h2>
            <div className="text-sm text-gray-400 font-mono">
              {snap ? `${snap.model} • iOS ${snap.osVersion} • ${snap.serial}` : 'Unknown Device'}
            </div>
            {isFailed && session.errorMessage && (
              <div className="mt-2 text-sm text-red-400 bg-red-500/10 px-3 py-2 rounded">
                <span className="font-semibold block mb-1">
                  Error: {session.errorCode || 'UNKNOWN_ERROR'}
                </span>
                {session.errorMessage}
              </div>
            )}
          </div>
          <button
            onClick={onClose}
            className="p-2 text-gray-400 hover:text-white rounded-full hover:bg-white/10 transition-colors"
          >
            ✕
          </button>
        </div>

        {/* Tabs */}
        <div className="flex border-b border-white/5 px-4">
          <button
            onClick={() => setTab('steps')}
            className={`px-4 py-3 text-sm font-medium border-b-2 transition-colors ${tab === 'steps' ? 'border-blue-500 text-white' : 'border-transparent text-gray-500 hover:text-gray-300'}`}
          >
            Pipeline Steps ({session.steps.length})
          </button>
          <button
            onClick={() => setTab('logs')}
            className={`px-4 py-3 text-sm font-medium border-b-2 transition-colors ${tab === 'logs' ? 'border-blue-500 text-white' : 'border-transparent text-gray-500 hover:text-gray-300'}`}
          >
            Raw Logs ({session.logs.length})
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-hidden relative">
          {tab === 'steps' ? (
            <div className="p-6 overflow-y-auto h-full space-y-2">
              {session.steps.map((step, idx) => (
                <SessionStepRow key={`${step.id}-${idx}`} step={step} />
              ))}
              {session.steps.length === 0 && (
                <div className="text-gray-500 italic py-4">No steps recorded.</div>
              )}
            </div>
          ) : (
            <div className="h-full flex flex-col">
              <LogLineList logs={session.logs} />
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
