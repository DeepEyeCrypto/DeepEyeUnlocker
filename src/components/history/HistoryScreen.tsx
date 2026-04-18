import { useEffect, useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import { Trash2, Download, Search, RefreshCw, XCircle, CheckCircle2 } from "lucide-react";

interface HistoryEntry {
  id: number;
  timestamp: string;
  device_name: string;
  chipset: string;
  tool_name: string;
  result: string;
}

export function HistoryScreen() {
  const [history, setHistory] = useState<HistoryEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState("");

  const fetchHistory = async () => {
    setLoading(true);
    try {
      const data = await invoke<HistoryEntry[]>("get_history");
      setHistory(data);
    } catch (err) {
      console.error("Failed to fetch history:", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchHistory();
  }, []);

  const handleClear = async () => {
    if (confirm("Are you sure you want to clear all history?")) {
      await invoke("clear_history");
      fetchHistory();
    }
  };

  const handleExport = async () => {
    try {
      const path = "deepeye_history_export.csv";
      await invoke("export_history_csv", { path });
      alert(`Exported to ${path}`);
    } catch (err) {
      alert(`Export failed: ${err}`);
    }
  };

  const filteredHistory = history.filter(
    (entry) =>
      entry.device_name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      entry.tool_name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      entry.chipset.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="history-screen p-6 animate-in fade-in duration-500">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold text-white tracking-tight">Operation History</h1>
          <p className="text-gray-400 text-sm">Review logs of your device operations</p>
        </div>
        <div className="flex gap-3">
          <button
            onClick={fetchHistory}
            className="flex items-center gap-2 px-4 py-2 bg-white/5 hover:bg-white/10 rounded-lg text-sm transition-all"
          >
            <RefreshCw size={14} className={loading ? "animate-spin" : ""} />
            Refresh
          </button>
          <button
            onClick={handleExport}
            className="flex items-center gap-2 px-4 py-2 bg-blue-500/20 hover:bg-blue-500/30 text-blue-400 rounded-lg text-sm transition-all border border-blue-500/30"
          >
            <Download size={14} />
            Export CSV
          </button>
          <button
            onClick={handleClear}
            className="flex items-center gap-2 px-4 py-2 bg-red-500/10 hover:bg-red-500/20 text-red-500 rounded-lg text-sm transition-all border border-red-500/20"
          >
            <Trash2 size={14} />
            Clear All
          </button>
        </div>
      </div>

      <div className="relative mb-6">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500" size={18} />
        <input
          type="text"
          placeholder="Search by device, tool, or chipset..."
          className="w-full bg-white/5 border border-white/10 rounded-xl py-3 pl-12 pr-4 text-white focus:outline-none focus:ring-2 focus:ring-blue-500/50 transition-all font-medium"
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
        />
      </div>

      <div className="glass-card overflow-hidden rounded-2xl border border-white/10">
        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="bg-white/5 text-gray-400 text-xs font-semibold uppercase tracking-wider">
                <th className="px-6 py-4">Status</th>
                <th className="px-6 py-4">Timestamp</th>
                <th className="px-6 py-4">Device</th>
                <th className="px-6 py-4">Chipset</th>
                <th className="px-6 py-4">Tool Used</th>
                <th className="px-6 py-4 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5">
              {filteredHistory.map((entry) => (
                <tr key={entry.id} className="hover:bg-white/5 transition-colors group">
                  <td className="px-6 py-4">
                    {entry.result.toLowerCase().includes("fail") || entry.result.toLowerCase().includes("err") ? (
                      <div className="flex items-center gap-2 text-red-400">
                        <XCircle size={16} />
                        <span className="text-xs font-medium">Failed</span>
                      </div>
                    ) : (
                      <div className="flex items-center gap-2 text-green-400">
                        <CheckCircle2 size={16} />
                        <span className="text-xs font-medium">Success</span>
                      </div>
                    )}
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-300 font-mono">{entry.timestamp}</td>
                  <td className="px-6 py-4 text-sm font-semibold text-white">{entry.device_name}</td>
                  <td className="px-6 py-4">
                    <span className="px-2 py-1 bg-white/5 rounded text-[10px] font-bold text-blue-300 uppercase letter-spacing-1">
                      {entry.chipset}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-300">{entry.tool_name}</td>
                  <td className="px-6 py-4 text-right">
                    <button 
                      className="opacity-0 group-hover:opacity-100 text-xs text-blue-400 hover:underline transition-opacity"
                      onClick={() => alert(`Full Log:\n${entry.result}`)}
                    >
                      View Logs
                    </button>
                  </td>
                </tr>
              ))}
              {filteredHistory.length === 0 && !loading && (
                <tr>
                  <td colSpan={6} className="px-6 py-12 text-center text-gray-500 font-medium">
                    No history records found.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
