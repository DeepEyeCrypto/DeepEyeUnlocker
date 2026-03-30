type Status = "connected" | "disconnected" | "pending" | "error" | "recovery";

const STATUS_MAP: Record<Status, { label: string; className: string; pulse?: boolean }> = {
  connected: { label: "Connected", className: "status-connected" },
  disconnected: { label: "Disconnected", className: "status-disconnected" },
  pending: { label: "Pending...", className: "status-pending", pulse: true },
  error: { label: "Error", className: "status-error" },
  recovery: { label: "Recovery", className: "status-recovery" },
};

export function StatusBadge({ status }: { status: Status }) {
  const s = STATUS_MAP[status];
  return (
    <span className={`status-badge ${s.className}`}>
      <span className={`status-dot ${s.pulse ? "pulse" : ""}`} />
      {s.label}
    </span>
  );
}

export type { Status };
