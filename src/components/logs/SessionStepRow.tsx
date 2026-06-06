import { ProgressStep } from '../../lib/session-types';

interface SessionStepRowProps {
  step: ProgressStep;
}

export function SessionStepRow({ step }: SessionStepRowProps) {
  let statusIcon = '⏳';
  let statusColor = 'text-gray-400';

  if (step.status === 'done') {
    statusIcon = '✓';
    statusColor = 'text-green-400';
  } else if (step.status === 'failed') {
    statusIcon = '✗';
    statusColor = 'text-red-400';
  } else if (step.status === 'running') {
    statusIcon = '↻';
    statusColor = 'text-blue-400';
  }

  return (
    <div className="flex items-start space-x-3 text-sm py-2 border-b border-white/5 last:border-0">
      <div className={`mt-0.5 w-4 font-mono font-bold ${statusColor}`}>{statusIcon}</div>
      <div className="flex-1">
        <div className="font-medium text-gray-200">{step.label}</div>
        {step.detail && <div className="text-gray-500 text-xs mt-1">{step.detail}</div>}
      </div>
      <div className="text-gray-500 font-mono text-xs">
        {step.durationMs ? `${step.durationMs}ms` : '—'}
      </div>
    </div>
  );
}
