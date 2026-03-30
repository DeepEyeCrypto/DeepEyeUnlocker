export const DASHBOARD_CONFIG = {
  POLLING_INTERVAL_MS: 2000,
} as const;

export const DASHBOARD_COMMANDS = {
  POLL_ORCHESTRATOR: "ios_poll_orchestrator",
  DEVICE_IDENTITY: "ios_device_identity",
  CHECK_FMI: "ios_fmi_state",
  CHECK_SHSH: "check_signed_versions",
  CHECK_ACTIVATION: "ios_check_activation_state",
  RESTORE_LATEST: "restore_latest",
} as const;

export type DashboardState = "idle" | "scanning" | "connected" | "error" | "unsupported";

export type PlatformTab = "android" | "apple" | "tools";

export type DetectedDeviceInfo = {
  model: string;
  serial: string;
  os: string;
  mode: string;
  bootloaderStatus: string;
  carrier?: string;
};

