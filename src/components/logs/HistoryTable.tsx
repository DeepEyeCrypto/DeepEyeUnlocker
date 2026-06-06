import { SessionHistoryRow } from '../../lib/session-history-utils';

interface HistoryTableProps {
  rows: SessionHistoryRow[];
  onRowClick: (id: string) => void;
}

export function HistoryTable({ rows, onRowClick }: HistoryTableProps) {
  if (rows.length === 0) {
    return (
      <div className="flex-1 flex items-center justify-center text-gray-500 italic h-64">
        No sessions found matching filters.
      </div>
    );
  }

  return (
    <div className="flex-1 overflow-x-auto">
      <table className="w-full text-left text-sm text-gray-300">
        <thead className="text-xs text-gray-500 uppercase bg-gray-900/50 sticky top-0 z-10 shadow-sm">
          <tr>
            <th className="px-4 py-3 font-medium tracking-wider">Date</th>
            <th className="px-4 py-3 font-medium tracking-wider">Operation</th>
            <th className="px-4 py-3 font-medium tracking-wider">Device</th>
            <th className="px-4 py-3 font-medium tracking-wider">Duration</th>
            <th className="px-4 py-3 font-medium tracking-wider">Status</th>
            <th className="px-4 py-3 font-medium tracking-wider text-right">Logs</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-800">
          {rows.map((row) => {
            const dateStr = new Date(row.startedAt).toLocaleString('en-US', {
              month: 'short',
              day: 'numeric',
              hour: '2-digit',
              minute: '2-digit',
            });
            const opStr =
              typeof row.operationType === 'string'
                ? row.operationType
                : (row.operationType as any).customCommand;

            let statusBadge = (
              <span className="px-2 py-0.5 rounded text-xs bg-gray-500/10 text-gray-400 border border-gray-500/20">
                {row.status}
              </span>
            );
            if (row.outcome === 'success') {
              statusBadge = (
                <span className="px-2 py-0.5 rounded text-xs bg-green-500/10 text-green-400 border border-green-500/20">
                  Success
                </span>
              );
            } else if (row.outcome === 'failed' || row.status === 'failed') {
              statusBadge = (
                <span className="px-2 py-0.5 rounded text-xs bg-red-500/10 text-red-400 border border-red-500/20">
                  Failed
                </span>
              );
            }

            return (
              <tr
                key={row.sessionId}
                onClick={() => onRowClick(row.sessionId)}
                className="hover:bg-gray-800/50 cursor-pointer transition-colors group"
              >
                <td className="px-4 py-3 whitespace-nowrap text-gray-400 font-mono text-xs">
                  {dateStr}
                </td>
                <td className="px-4 py-3 font-medium text-white group-hover:text-blue-400 transition-colors">
                  {opStr}
                </td>
                <td className="px-4 py-3 text-gray-400">{row.deviceModel}</td>
                <td className="px-4 py-3 font-mono text-xs text-gray-500">
                  {row.durationMs ? `${Math.round(row.durationMs / 1000)}s` : '—'}
                </td>
                <td className="px-4 py-3 whitespace-nowrap">{statusBadge}</td>
                <td className="px-4 py-3 whitespace-nowrap text-right font-mono text-xs">
                  {row.hasErrors && <span className="text-red-400 mr-2">⚠️</span>}
                  {row.logCount} lines
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
