export type NavId =
  | "dashboard"
  | "activation"
  | "jailbreak"
  | "toolbox"
  | "fmi"
  | "purple"
  | "bootfiles"
  | "shsh"
  | "diagnostics"
  | "restore"
  | "cve"
  | "vault"
  | "identity"
  | "extraction"
  | "advanced";

export type NavItem = {
  id: NavId;
  icon: string;
  label: string;
};

export const NAV_ITEMS: NavItem[] = [
  { id: "dashboard", icon: "H", label: "Dashboard" },
  { id: "activation", icon: "A", label: "Activation" },
  { id: "jailbreak", icon: "J", label: "Jailbreak" },
  { id: "toolbox", icon: "T", label: "Toolbox" },
  { id: "fmi", icon: "F", label: "FMI" },
  { id: "purple", icon: "P", label: "Purple" },
  { id: "bootfiles", icon: "B", label: "BootFiles" },
  { id: "shsh", icon: "S", label: "SHSH" },
  { id: "diagnostics", icon: "D", label: "Diagnostics" },
  { id: "restore", icon: "R", label: "Restore" },
  { id: "cve", icon: "C", label: "CVE" },
  { id: "vault", icon: "V", label: "Vault" },
  { id: "identity", icon: "I", label: "Identity" },
  { id: "extraction", icon: "E", label: "Extraction" },
  { id: "advanced", icon: "X", label: "Advanced" },
];
