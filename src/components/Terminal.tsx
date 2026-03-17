interface Props {
  output: string;
  status: "idle" | "running" | "success" | "error";
}

export default function Terminal({ output, status }: Props) {
  if (status === "idle" && !output) return null;

  return (
    <div className="glass" style={{
      marginTop: 20, padding: 12, borderRadius: 12,
      background: "rgba(0,0,0,0.6)", border: "1px solid rgba(255,255,255,0.08)",
      maxHeight: 250, overflowY: "auto",
    }}>
      <div style={{
        display: "flex", justifyContent: "space-between",
        alignItems: "center", marginBottom: 10, paddingBottom: 8,
        borderBottom: "1px solid rgba(255,255,255,0.05)"
      }}>
        <div style={{ fontSize: 11, fontWeight: 700, color: "#94a3b8", display: "flex", alignItems: "center", gap: 6 }}>
          <span style={{
            width: 8, height: 8, borderRadius: "50%",
            background: status === "running" ? "#fbbf24" : status === "success" ? "#4ade80" : status === "error" ? "#f87171" : "#64748b"
          }} />
          TERMINAL LOG
        </div>
        <button
          onClick={() => {}} // TODO: Add clear log
          style={{
            background: "none", border: "none", color: "#475569",
            fontSize: 10, cursor: "pointer", fontWeight: 600
          }}
        >
          CLEAR
        </button>
      </div>
      <pre style={{
        margin: 0, fontSize: 11, color: "#d1d5db", whiteSpace: "pre-wrap",
        fontFamily: "'Fira Code', 'JetBrains Mono', monospace", lineHeight: 1.5
      }}>
        {output || "Waiting for execution..."}
        {status === "running" && <span className="cursor-blink">_</span>}
      </pre>
    </div>
  );
}
