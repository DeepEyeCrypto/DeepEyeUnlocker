import { HistoryFilters, DEFAULT_FILTERS } from '../../lib/session-history-utils';

interface HistoryFilterBarProps {
  filters: HistoryFilters;
  onChange: (f: HistoryFilters) => void;
}

export function HistoryFilterBar({ filters, onChange }: HistoryFilterBarProps) {
  const update = (key: keyof HistoryFilters, val: any) => {
    onChange({ ...filters, [key]: val });
  };

  return (
    <div className="flex flex-wrap items-center gap-3 p-4 bg-gray-900 border-b border-white/5">
      <div className="relative flex-1 min-w-[200px] max-w-sm">
        <input
          type="text"
          placeholder="Search by model, ID, logs..."
          className="w-full bg-black border border-gray-700 text-sm text-white rounded px-3 py-2 focus:border-blue-500 focus:outline-none placeholder-gray-500 transition-colors"
          value={filters.search}
          onChange={(e) => update('search', e.target.value)}
        />
      </div>

      <select
        value={filters.status}
        onChange={(e) => update('status', e.target.value)}
        className="bg-black border border-gray-700 text-sm text-gray-300 rounded px-3 py-2 focus:outline-none focus:border-blue-500"
      >
        <option value="all">All Statuses</option>
        <option value="completed">Completed</option>
        <option value="failed">Failed</option>
        <option value="cancelled">Cancelled</option>
      </select>

      <button
        onClick={() => onChange(DEFAULT_FILTERS)}
        className="text-xs text-gray-500 hover:text-white px-2 transition-colors"
      >
        Reset Filters
      </button>
    </div>
  );
}
