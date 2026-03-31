import { useEffect, useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import { HISTORY_COMMANDS, type DeviceHistoryEntry } from "../../lib/device_history";
import { Card } from "../ui/Card";

export default function DeviceHistoryPage() {
  const [entries, setEntries] = useState<DeviceHistoryEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState("");

  const load = async () => {
    setLoading(true);
    try {
      const data = await invoke<DeviceHistoryEntry[]>(HISTORY_COMMANDS.GET_ENTRIES, { limit: 200 });
      setEntries(data);
    } catch {
      setEntries([]);
    }
    setLoading(false);
  };

  useEffect(() => {
    void load();
  }, []);

  const clearHistory = async () => {
    try {
      await invoke<string>(HISTORY_COMMANDS.CLEAR);
      setEntries([]);
    } catch { /* ignore */ }
  };

  const deleteEntry = async (id: string) => {
    try {
      await invoke<string>(HISTORY_COMMANDS.DELETE_ENTRY, { entry_id: id });
      setEntries((prev) => prev.filter((e) => e.id !== id));
    } catch { /* ignore */ }
  };

  const exportJson = async () => {
    try {
      const json = await invoke<string>(HISTORY_COMMANDS.EXPORT_JSON);
      await navigator.clipboard.writeText(json);
    } catch { /* ignore */ }
  };

  const filtered = filter
    ? entries.filter(
        (e) =>
          e.model.toLowerCase().includes(filter.toLowerCase()) ||
          e.serial.toLowerCase().includes(filter.toLowerCase()) ||
          e.action.toLowerCase().includes(filter.toLowerCase()) ||
          e.platform.toLowerCase().includes(filter.toLowerCase())
      )
    : entries;

  const formatTime = (ts: string) => {
    try {
      const d = new Date(ts);
      return d.toLocaleString();
    } catch {
      return ts;
    }
  };

  return (
    <div className="page">
      <div className="row-between">
        <h2 className="page-title">Device History</h2>
        <div className="action-row">
          <button className="btn btn-ghost btn-sm" onClick={exportJson}>
            Copy JSON
          </button>
          <button className="btn btn-danger btn-sm" onClick={clearHistory}>
            Clear All
          </button>
        </div>
      </div>

      <div>
        <input
          className="field-input"
          placeholder="Filter by model, serial, action, platform..."
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
        />
      </div>

      {loading && <div className="panel pulse-panel">Loading history...</div>}

      {!loading && filtered.length === 0 && (
        <div className="panel">
          <p className="muted">No history entries found.</p>
        </div>
      )}

      {!loading && filtered.length > 0 && (
        <div className="stack-sm">
          <span className="meta-text">{filtered.length} entries</span>
          {filtered.map((entry) => (
            <Card key={entry.id}>
              <div className="row-between">
                <div className="device-grid" style={{ flex: 1 }}>
                  <div className="device-field">
                    <span className="device-field-label">Model</span>
                    <span className="device-field-value">{entry.model}</span>
                  </div>
                  <div className="device-field">
                    <span className="device-field-label">Serial</span>
                    <span className="device-field-value mono">{entry.serial}</span>
                  </div>
                  <div className="device-field">
                    <span className="device-field-label">Action</span>
                    <span className="device-field-value highlight">{entry.action}</span>
                  </div>
                  <div className="device-field">
                    <span className="device-field-label">Result</span>
                    <span className="device-field-value">{entry.result}</span>
                  </div>
                  <div className="device-field">
                    <span className="device-field-label">Platform</span>
                    <span className="device-field-value">{entry.platform}</span>
                  </div>
                  <div className="device-field">
                    <span className="device-field-label">Time</span>
                    <span className="device-field-value">{formatTime(entry.timestamp)}</span>
                  </div>
                </div>
                <button
                  className="btn btn-ghost btn-sm"
                  onClick={() => deleteEntry(entry.id)}
                  title="Delete entry"
                >
                  X
                </button>
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
