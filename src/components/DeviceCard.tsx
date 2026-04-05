import { Card } from "./ui/Card";
import { StatusBadge, type Status } from "./ui/StatusBadge";

type DeviceInfo = {
  model: string;
  serial: string;
  os: string;
  mode: string;
  bootloaderStatus: string;
  carrier?: string | null;
  source?: string | null;
};

type FieldProps = {
  label: string;
  value: string;
  mono?: boolean;
  highlight?: boolean;
};

function DeviceField({ label, value, mono, highlight }: FieldProps) {
  return (
    <div className="device-field">
      <span className="device-field-label">{label}</span>
      <span className={`device-field-value ${mono ? "mono" : ""} ${highlight ? "highlight" : ""}`}>
        {value}
      </span>
    </div>
  );
}

function deriveStatus(device: DeviceInfo | null): Status {
  if (!device) {
    return "pending";
  }
  if (device.mode.toLowerCase().includes("recovery") || device.mode.toLowerCase().includes("dfu")) {
    return "recovery";
  }
  return "connected";
}

export function DeviceCard({ device }: { device: DeviceInfo | null }) {
  const status = deriveStatus(device);

  const handleCopyInfo = async () => {
    if (!device) {
      return;
    }

    const payload = [
      `Model: ${device.model}`,
      `Serial: ${device.serial}`,
      `OS: ${device.os}`,
      `Mode: ${device.mode}`,
      `Bootloader: ${device.bootloaderStatus}`,
      `Carrier: ${device.carrier ?? "-"}`,
      `Source: ${device.source ?? "unknown"}`,
    ].join("\n");

    await navigator.clipboard.writeText(payload);
  };

  if (!device) {
    return (
      <Card title="Connected Device" action={<StatusBadge status={status} />} className="device-card">
        <div className="device-empty-state">
          <div className="device-empty-pulse" aria-hidden="true" />
          <div className="device-empty-title">No device connected</div>
          <div className="device-empty-copy">
            Connect a device over USB. Queued operations will start automatically once detection succeeds.
          </div>
        </div>
      </Card>
    );
  }

  return (
    <Card title="Connected Device" action={<StatusBadge status={status} />} className="device-card">
      <div className="device-grid">
        <DeviceField label="Model" value={device.model} />
        <DeviceField label="Serial" value={device.serial} mono />
        <DeviceField label="OS" value={device.os} />
        <DeviceField label="Mode" value={device.mode} highlight />
        <DeviceField label="Bootloader" value={device.bootloaderStatus} />
        <DeviceField label="Carrier" value={device.carrier ?? "-"} />
        <DeviceField label="Source" value={device.source ?? "unknown"} />
      </div>
      <div className="device-actions">
        <button className="action-btn" type="button" onClick={() => void handleCopyInfo()}>
          Copy Info
        </button>
      </div>
    </Card>
  );
}
