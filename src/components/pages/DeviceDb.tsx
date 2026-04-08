import { Database, Search, ArrowRight, Zap } from "lucide-react";
import { useState } from "react";
import { useDeviceDb } from "../../hooks/useDeviceDB";
import type { DeviceEntry } from "../../hooks/useDeviceDB";
import { emit } from "@tauri-apps/api/event";
import "../../styles/device-db.css";

const PROTOCOL_META: Record<string, { label: string; color: string; emoji: string }> = {
  Edl:         { label: "EDL",     color: "#a855f7", emoji: "🟣" },
  MtkBrom:     { label: "MTK",     color: "#22c55e", emoji: "🟢" },
  SamsungOdin: { label: "Samsung", color: "#3b82f6", emoji: "🔵" },
  Adb:         { label: "ADB",     color: "#60a5fa", emoji: "🔷" },
  Fastboot:    { label: "Fastboot",color: "#f59e0b", emoji: "⚡" },
  Unknown:     { label: "Unknown", color: "#6b7280", emoji: "❓" },
};

function ProtocolBadge({ protocol }: { protocol: string }) {
  const meta = PROTOCOL_META[protocol] ?? PROTOCOL_META.Unknown;
  return (
    <span
      className="db-badge"
      style={{ borderColor: meta.color, color: meta.color }}
    >
      {meta.emoji} {meta.label}
    </span>
  );
}

function DeviceCard({
  device,
  onAutoRoute,
}: {
  device: DeviceEntry;
  onAutoRoute: (model: string) => void;
}) {
  return (
    <div className="db-device-card">
      <div className="db-device-header">
        <div>
          <div className="db-device-name">
            {device.brand} {device.model}
            <span className="db-codename">({device.codename})</span>
          </div>
          <div className="db-device-soc">
            {device.soc} · {device.soc_family} · <ProtocolBadge protocol={device.protocol} />
          </div>
          {device.notes && (
            <div className="db-device-notes">ℹ {device.notes}</div>
          )}
        </div>
        <button
          className="db-route-btn"
          onClick={() => onAutoRoute(device.model)}
        >
          Auto-Route <ArrowRight size={14} />
        </button>
      </div>
    </div>
  );
}

export default function DeviceDbPage() {
  const {
    results,
    allDevices,
    routingResult,
    isSearching,
    search,
    autoRoute,
  } = useDeviceDb();

  const [query, setQuery] = useState("");

  const handleSearch = (val: string) => {
    setQuery(val);
    search(val);
  };

  const handleAutoRoute = async (model: string) => {
    await autoRoute(model);
  };

  const displayList = query.trim() ? results : allDevices.slice(0, 20);

  return (
    <div className="db-page">
      <div className="db-header">
        <div className="db-title">
          <Database size={22} className="db-icon" />
          <h1>DEVICE DATABASE</h1>
        </div>
        <span className="db-count">{allDevices.length} devices</span>
      </div>

      {/* Search */}
      <div className="db-search-row">
        <Search size={16} className="db-search-icon" />
        <input
          className="db-search-input"
          placeholder="Search model, codename, brand, SoC..."
          value={query}
          onChange={(e) => handleSearch(e.target.value)}
        />
        {isSearching && <span className="db-searching">...</span>}
      </div>

      {/* Results */}
      <div className="db-results">
        {displayList.length === 0 && query && !isSearching && (
          <div className="db-empty">No devices found for "{query}"</div>
        )}
        {displayList.map((d, i) => (
          <DeviceCard key={i} device={d} onAutoRoute={handleAutoRoute} />
        ))}
      </div>

      {/* Auto-Route Result */}
      {routingResult && (
        <div className="db-routing-card">
          <h2>Auto-Route Result</h2>
          <div className="db-routing-grid">
            <div>
              <span>Device</span>
              <strong>
                {routingResult.device
                  ? `${routingResult.device.brand} ${routingResult.device.model}`
                  : "Unknown (heuristic)"}
              </strong>
            </div>
            <div>
              <span>Protocol</span>
              <strong>
                <ProtocolBadge protocol={routingResult.protocol} />
              </strong>
            </div>
            <div>
              <span>Route</span>
              <strong className="db-route-path">{routingResult.route_to}</strong>
            </div>
            <div>
              <span>Confidence</span>
              <strong>
                <span
                  className="db-confidence"
                  style={{
                    color:
                      routingResult.confidence >= 80
                        ? "#22c55e"
                        : routingResult.confidence >= 50
                        ? "#f59e0b"
                        : "#ef4444",
                  }}
                >
                  {routingResult.confidence}%
                </span>
              </strong>
            </div>
            {routingResult.pre_fill.firehose_path && (
              <div>
                <span>Firehose</span>
                <strong>{routingResult.pre_fill.firehose_path}</strong>
              </div>
            )}
            {routingResult.pre_fill.da_path && (
              <div>
                <span>DA File</span>
                <strong>{routingResult.pre_fill.da_path}</strong>
              </div>
            )}
          </div>
          <button
            className="db-go-btn"
            onClick={() => {
              emit("navigate-to-protocol", {
                route: routingResult.route_to,
                device: routingResult.device,
                pre_fill: routingResult.pre_fill,
              });
            }}
          >
            <Zap size={14} />
            Go to {routingResult.route_to.replace("/", "").toUpperCase()} Page
          </button>
        </div>
      )}

      {/* Legend */}
      <div className="db-legend">
        {Object.entries(PROTOCOL_META)
          .filter(([k]) => k !== "Unknown")
          .map(([key, meta]) => (
            <span key={key} style={{ color: meta.color }} className="db-legend-item">
              {meta.emoji} {meta.label}
            </span>
          ))}
      </div>
    </div>
  );
}
