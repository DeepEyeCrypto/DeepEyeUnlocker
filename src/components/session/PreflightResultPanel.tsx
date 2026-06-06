import React from 'react';
import { PreflightResult } from '../../lib/session-types';
import { AlertTriangle, XCircle, CheckCircle } from 'lucide-react';

interface Props {
  preflight: PreflightResult | null;
}

export const PreflightResultPanel: React.FC<Props> = ({ preflight }) => {
  if (!preflight) return null;

  return (
    <div className="bg-surface p-4 rounded-xl border border-white/5 space-y-4">
      <div className="flex items-center gap-2">
        {preflight.passed ? (
          <CheckCircle className="w-5 h-5 text-green-500" />
        ) : (
          <XCircle className="w-5 h-5 text-red-500" />
        )}
        <h3 className="font-semibold text-white">Preflight Checks</h3>
      </div>

      <div className="space-y-2">
        {preflight.checks.map((check, idx) => (
          <div
            key={idx}
            className="flex items-center justify-between p-2 rounded-lg bg-black/20 border border-white/5"
          >
            <span className="text-sm text-gray-300">{check.message}</span>
            {check.passed ? (
              <span className="text-xs bg-green-500/20 text-green-400 px-2 py-1 rounded">
                Passed
              </span>
            ) : (
              <span
                className={`text-xs px-2 py-1 rounded ${check.required ? 'bg-red-500/20 text-red-400' : 'bg-yellow-500/20 text-yellow-400'}`}
              >
                {check.required ? 'Failed' : 'Warning'}
              </span>
            )}
          </div>
        ))}
      </div>

      {preflight.blockingIssues.length > 0 && (
        <div className="p-3 bg-red-500/10 border border-red-500/20 rounded-lg flex items-start gap-3">
          <XCircle className="w-5 h-5 text-red-400 shrink-0 mt-0.5" />
          <div>
            <h4 className="text-sm font-medium text-red-400">Blocking Issues</h4>
            <ul className="list-disc list-inside text-sm text-red-300/80 mt-1">
              {preflight.blockingIssues.map((issue, idx) => (
                <li key={idx}>{issue}</li>
              ))}
            </ul>
          </div>
        </div>
      )}

      {preflight.warnings.length > 0 && (
        <div className="p-3 bg-yellow-500/10 border border-yellow-500/20 rounded-lg flex items-start gap-3">
          <AlertTriangle className="w-5 h-5 text-yellow-400 shrink-0 mt-0.5" />
          <div>
            <h4 className="text-sm font-medium text-yellow-400">Warnings</h4>
            <ul className="list-disc list-inside text-sm text-yellow-300/80 mt-1">
              {preflight.warnings.map((warn, idx) => (
                <li key={idx}>{warn}</li>
              ))}
            </ul>
          </div>
        </div>
      )}
    </div>
  );
};
