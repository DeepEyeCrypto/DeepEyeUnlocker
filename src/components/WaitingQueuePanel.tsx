import { formatElapsed } from "../lib/devices";

type WaitingQueuePanelProps = {
  operation: {
    id: string;
    name: string;
  } | null;
  elapsedMs: number;
  onCancel: (operationId: string) => void;
};

export function WaitingQueuePanel({
  operation,
  elapsedMs,
  onCancel,
}: WaitingQueuePanelProps) {
  if (!operation) {
    return null;
  }

  return (
    <div className="wait-queue-panel" role="status" aria-live="polite">
      <div className="wait-queue-copy">
        <div className="wait-queue-title">
          <span className="wait-queue-indicator" aria-hidden="true" />
          <span>Waiting for device...</span>
        </div>
        <div className="wait-queue-meta">
          {operation.name} queued • {formatElapsed(elapsedMs)} elapsed
        </div>
      </div>
      <button
        className="action-btn"
        type="button"
        onClick={() => onCancel(operation.id)}
      >
        Cancel Queue
      </button>
    </div>
  );
}

