import { Button } from "./ui/Button";
import { Card } from "./ui/Card";
import { StatusBadge, type Status } from "./ui/StatusBadge";

type DeviceInfo = {
  status: Status;
  model: string;
  serial: string;
  os: string;
  mode: string;
  bootloaderStatus: string;
  carrier?: string;
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

export function DeviceCard({ device }: { device: DeviceInfo }) {
  return (
    <Card title="Connected Device" action={<StatusBadge status={device.status} />} className="device-card">
      <div className="device-grid">
        <DeviceField label="Model" value={device.model} />
        <DeviceField label="Serial" value={device.serial} mono />
        <DeviceField label="OS" value={device.os} />
        <DeviceField label="Mode" value={device.mode} highlight />
        <DeviceField label="Bootloader" value={device.bootloaderStatus} />
        <DeviceField label="Carrier" value={device.carrier ?? "-"} />
      </div>
      <div className="device-actions">
        <Button variant="primary" size="sm">Unlock</Button>
        <Button variant="secondary" size="sm">Reboot</Button>
        <Button variant="ghost" size="sm">Copy Info</Button>
      </div>
    </Card>
  );
}

