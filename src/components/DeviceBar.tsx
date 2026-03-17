export default function DeviceBar() {
  return (
    <div className="glass" style={{
      padding: "12px 20px", borderRadius: 16,
      display: "flex", alignItems: "center", justifyContent: "space-between",
      border: "1px solid rgba(255,255,255,0.08)"
    }}>
      <div style={{ display: "flex", alignItems: "center", gap: 20 }}>
        <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
          <div style={{
            width: 40, height: 40, borderRadius: 10,
            background: "linear-gradient(135deg, #a78bfa, #7c3aed)",
            display: "flex", alignItems: "center", justifyContent: "center",
            fontSize: 20
          }}>📲</div>
          <div>
            <div style={{ fontSize: 14, fontWeight: 700, color: "#e2e8f0" }}>iPhone 12 Pro</div>
            <div style={{ fontSize: 11, color: "#64748b" }}>iOS 16.7.5 · DFU Mode</div>
          </div>
        </div>

        <div style={{ height: 24, width: 1, background: "rgba(255,255,255,0.1)" }} />

        <div style={{ display: "flex", gap: 15 }}>
          <Stat label="ECID" value="0x8020" />
          <Stat label="CHIP" value="A14 Bionic" />
          <Stat label="CPID" value="0x8101" />
        </div>
      </div>

      <div style={{ display: "flex", gap: 10 }}>
        <button className="btn" style={{ fontSize: 11 }}>🔍 Detect</button>
        <button className="btn primary" style={{ fontSize: 11 }}>☁️ Sync Vault</button>
      </div>
    </div>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <div style={{ fontSize: 10, color: "#475569", fontWeight: 600 }}>{label}</div>
      <div style={{ fontSize: 12, color: "#94a3b8", fontWeight: 700 }}>{value}</div>
    </div>
  );
}
