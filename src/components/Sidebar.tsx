const SECTIONS = [
  { id: "activation",  label: "Activation",    icon: "🔓" },
  { id: "vault",       label: "Cloud Vault",   icon: "☁️" },
  { id: "identity",    label: "Identity",      icon: "🆔" },
  { id: "fmi",         label: "FMI / iCloud",  icon: "☁️" },
  { id: "jailbreak",   label: "Jailbreak",     icon: "⚡" },
  { id: "purple",      label: "Purple Mode",   icon: "🟣" },
  { id: "bootfiles",   label: "Boot Files",    icon: "📦" },
  { id: "extraction",  label: "Forensics",     icon: "📂" },
  { id: "cve",         label: "CVE Intel",     icon: "🧠" },
  { id: "toolbox",     label: "Toolbox",       icon: "🛠️" },
  { id: "shsh",        label: "SHSH Blobs",    icon: "💾" },
  { id: "diagnostics", label: "Diagnostics",   icon: "🔬" },
  { id: "restore",     label: "Restore / IPSW",icon: "🔄" },
  { id: "advanced",    label: "Advanced",       icon: "🚀" },
];

interface Props { active: string; onSelect: (id: string) => void; }

export default function Sidebar({ active, onSelect }: Props) {
  return (
    <div className="glass" style={{
      width: 200, height: "100%", padding: "16px 10px",
      display: "flex", flexDirection: "column", gap: 4,
      borderRadius: 16, flexShrink: 0,
    }}>
      <div style={{ padding: "0 8px 16px", borderBottom: "1px solid rgba(255,255,255,0.08)", marginBottom: 4 }}>
        <div style={{ fontSize: 15, fontWeight: 700, color: "#a78bfa" }}>👁️ DeepEye</div>
        <div style={{ fontSize: 10, color: "#475569" }}>Unlocker v1.0 · All Features</div>
      </div>

      {SECTIONS.map(s => (
        <button key={s.id} onClick={() => onSelect(s.id)} style={{
          display: "flex", alignItems: "center", gap: 10,
          padding: "9px 12px", borderRadius: 10, border: "none",
          background: active === s.id
            ? "linear-gradient(135deg, rgba(124,58,237,0.4), rgba(37,99,235,0.3))"
            : "transparent",
          color: active === s.id ? "#e2e8f0" : "#64748b",
          cursor: "pointer", fontSize: 12.5, fontWeight: 500,
          transition: "all 0.2s", textAlign: "left",
          borderLeft: active === s.id ? "2px solid #a78bfa" : "2px solid transparent",
        }}>
          <span>{s.icon}</span><span>{s.label}</span>
        </button>
      ))}

      {/* Version footer */}
      <div style={{ marginTop: "auto", padding: "12px 8px 0", borderTop: "1px solid rgba(255,255,255,0.06)" }}>
        <div style={{ fontSize: 10, color: "#334155" }}>F3arRa1n Compatible</div>
        <div style={{ fontSize: 10, color: "#334155" }}>No Tiers · No Credits</div>
      </div>
    </div>
  );
}
