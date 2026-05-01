import kotlinFeatureInventory from "./kotlin_feature_inventory.json";

export type WorkspaceId =
  | "control-center"
  | "feature-remap"
  | "adb-bridge"
  | "wireless-adb"
  | "firmware-lab"
  | "mtk-brom"
  | "qualcomm-edl"
  | "samsung-odin"
  | "signal-bypass"
  | "filesystem-engine"
  | "history"
  | "settings";

export type NavigationSection = "overview" | "labs" | "system";
export type RiskLevel = "SAFE" | "MODERATE" | "HIGH" | "CRITICAL";
export type AutomationMode = "direct" | "guided";

export type KotlinFeatureItem = {
  id: string;
  icon: string;
  label: string;
  tier: number;
  modes: string[];
  chipsets: string[];
  risk: RiskLevel;
  requiresAuth: boolean;
  description: string;
  successLog: string;
  warningMsg: string | null;
};

export type KotlinFeatureGroup = {
  id: string;
  title: string;
  features: KotlinFeatureItem[];
};

export type BrandFeatureSet = {
  brand: string;
  groups: KotlinFeatureGroup[];
};

export type NavigationItem = {
  id: WorkspaceId;
  label: string;
  icon: string;
  color: string;
  description: string;
  section: NavigationSection;
  eyebrow: string;
  badge?: string;
};

export type WorkspaceMeta = {
  label: string;
  icon: string;
  color: string;
  description: string;
  section: NavigationSection;
  eyebrow: string;
};

export type RemappedFeature = KotlinFeatureItem & {
  brand: string;
  groupId: string;
  groupTitle: string;
  featurePath: string;
  workspaceId: WorkspaceId;
  workspaceLabel: string;
  workspaceCandidates: WorkspaceId[];
  executionHint: string;
  commandHint: string | null;
  automation: AutomationMode;
};

const ALL_WORKSPACES: WorkspaceId[] = [
  "control-center",
  "feature-remap",
  "adb-bridge",
  "wireless-adb",
  "firmware-lab",
  "mtk-brom",
  "samsung-odin",
  "signal-bypass",
  "filesystem-engine",
  "history",
  "settings",
];

const WORKSPACE_COUNT_TEMPLATE: Record<WorkspaceId, number> = {
  "control-center": 0,
  "feature-remap": 0,
  "adb-bridge": 0,
  "wireless-adb": 0,
  "firmware-lab": 0,
  "mtk-brom": 0,
  "qualcomm-edl": 0,
  "samsung-odin": 0,
  "signal-bypass": 0,
  "filesystem-engine": 0,
  history: 0,
  settings: 0,
};

export const WORKSPACE_META: Record<WorkspaceId, WorkspaceMeta> = {
  "control-center": {
    label: "Control Center",
    icon: "🧭",
    color: "#38bdf8",
    description:
      "Live USB intelligence, Apple probe workflows, and desktop device state in one control surface.",
    section: "overview",
    eyebrow: "Live orchestration",
  },
  "feature-remap": {
    label: "Kotlin Feature Remap",
    icon: "🧬",
    color: "#a855f7",
    description:
      "All imported Kotlin brand features mapped onto desktop labs with risk, mode, and execution guidance.",
    section: "overview",
    eyebrow: "Cross-platform integration",
  },
  "adb-bridge": {
    label: "ADB Bridge",
    icon: "📱",
    color: "#22c55e",
    description:
      "ADB shell, FRP erase, package deployment, reboot control, and Android diagnostics.",
    section: "labs",
    eyebrow: "Android lab",
  },
  "wireless-adb": {
    label: "Wireless Debugging",
    icon: "📶",
    color: "#14b8a6",
    description:
      "Pair Android 11+ devices over Wi-Fi and bridge the desktop stack without USB tethering.",
    section: "labs",
    eyebrow: "Android lab",
  },
  "firmware-lab": {
    label: "Firmware Lab",
    icon: "🧱",
    color: "#f59e0b",
    description:
      "Fastboot and flash queue analysis, ROM validation, partition planning, and firmware staging.",
    section: "labs",
    eyebrow: "Firmware orchestration",
  },
  "mtk-brom": {
    label: "MTK BROM Lab",
    icon: "🔌",
    color: "#06b6d4",
    description:
      "BROM handshake, SLA bypass, DA upload, IMEI flows, FRP erase, and partition access.",
    section: "labs",
    eyebrow: "MediaTek research",
  },
  "qualcomm-edl": {
    label: "Qualcomm EDL Lab",
    icon: "⚡",
    color: "#f97316",
    description:
      "Sahara and Firehose workflows, storage probe, GPT access, FRP erase, and raw partition IO.",
    section: "labs",
    eyebrow: "Qualcomm research",
  },
  "samsung-odin": {
    label: "Samsung Odin Lab",
    icon: "💠",
    color: "#2563eb",
    description:
      "Download mode handshake, PIT parsing, FRP erase, partition flash, and reboot staging.",
    section: "labs",
    eyebrow: "Samsung research",
  },
  "signal-bypass": {
    label: "Signal Bypass Pipeline",
    icon: "📡",
    color: "#e879f9",
    description:
      "A12+ multi-stage carrier, baseband, and IMEI verification flow surfaced directly in desktop UX.",
    section: "labs",
    eyebrow: "Apple advanced flow",
  },
  "filesystem-engine": {
    label: "Filesystem Exploitation",
    icon: "🗄️",
    color: "#ef4444",
    description:
      "Advanced SSH tunneling, R/W mounting, Setup.app patching, and untethered persistence.",
    section: "labs",
    eyebrow: "Apple core exploits",
  },
  history: {
    label: "Session History",
    icon: "🕒",
    color: "#94a3b8",
    description:
      "Audit prior runs, export desktop operation history, and inspect result quality over time.",
    section: "system",
    eyebrow: "Observability",
  },
  settings: {
    label: "Settings",
    icon: "⚙️",
    color: "#e2e8f0",
    description:
      "Desktop bridge configuration, update checks, and USB polling and logging preferences.",
    section: "system",
    eyebrow: "Environment",
  },
};

export const NAVIGATION_ITEMS: NavigationItem[] = ALL_WORKSPACES.map((workspaceId) => ({
  id: workspaceId,
  label: WORKSPACE_META[workspaceId].label,
  icon: WORKSPACE_META[workspaceId].icon,
  color: WORKSPACE_META[workspaceId].color,
  description: WORKSPACE_META[workspaceId].description,
  section: WORKSPACE_META[workspaceId].section,
  eyebrow: WORKSPACE_META[workspaceId].eyebrow,
}));

export const KOTLIN_BRAND_FEATURES = kotlinFeatureInventory as BrandFeatureSet[];

function hasMode(feature: KotlinFeatureItem, ...modes: string[]): boolean {
  return modes.some((mode) => feature.modes.includes(mode));
}

function hasChipset(feature: KotlinFeatureItem, ...chipsets: string[]): boolean {
  return chipsets.some((chipset) => feature.chipsets.includes(chipset));
}

function uniqueWorkspaceList(items: WorkspaceId[]): WorkspaceId[] {
  return [...new Set(items)];
}

function featureText(groupTitle: string, feature: KotlinFeatureItem): string {
  return `${feature.id} ${feature.label} ${feature.description} ${groupTitle}`.toLowerCase();
}

function scoreWorkspace(
  brand: string,
  groupTitle: string,
  feature: KotlinFeatureItem,
  workspaceId: WorkspaceId,
): number {
  const text = featureText(groupTitle, feature);
  const isFrp = /frp/.test(text);
  const isFirmware =
    /firmware|flash|partition|bootloader|factory reset|demo to retail|read backup|backup fw/.test(
      text,
    );
  const isScreen = /screen lock|pattern|lock/.test(text);
  const isImei = /imei|nv|efs/.test(text);
  const isAuth = /auth|sla|testpoint|meta|preloader|brom/.test(text);

  switch (workspaceId) {
    case "samsung-odin": {
      let score = 0;
      if (brand === "Samsung") score += 120;
      if (hasMode(feature, "ODIN", "ISP")) score += 55;
      if (isFrp) score += 10;
      return score;
    }
    case "mtk-brom": {
      let score = 0;
      if (hasMode(feature, "META", "PRELOADER", "BROM")) score += 100;
      if (hasChipset(feature, "MTK")) score += 35;
      if (isAuth) score += 25;
      if (isImei) score += 15;
      return score;
    }
    case "qualcomm-edl": {
      let score = 0;
      if (hasMode(feature, "EDL", "TESTPOINT")) score += 90;
      if (hasChipset(feature, "QUALCOMM")) score += 35;
      if (isAuth || isFrp) score += 15;
      return score;
    }
    case "firmware-lab": {
      let score = 0;
      if (hasMode(feature, "FASTBOOT")) score += 45;
      if (isFirmware) score += 80;
      if (/unlock|relock/.test(text)) score += 20;
      return score;
    }
    case "adb-bridge": {
      let score = 0;
      if (hasMode(feature, "ADB")) score += 70;
      if (hasMode(feature, "DIAG")) score += 35;
      if (isFrp || isScreen) score += 30;
      if (/device info|dump/.test(text)) score += 10;
      return score;
    }
    case "control-center":
      return 1;
    default:
      return 0;
  }
}

function buildWorkspaceCandidates(
  brand: string,
  groupTitle: string,
  feature: KotlinFeatureItem,
): WorkspaceId[] {
  const text = featureText(groupTitle, feature);
  const candidates: WorkspaceId[] = [];

  if (brand === "Samsung" || hasMode(feature, "ODIN", "ISP")) {
    candidates.push("samsung-odin");
  }

  if (
    hasMode(feature, "META", "PRELOADER", "BROM") ||
    (hasChipset(feature, "MTK") && /meta|preloader|brom|sla|imei|auth|frp/.test(text))
  ) {
    candidates.push("mtk-brom");
  }

  if (hasMode(feature, "EDL", "TESTPOINT")) {
    candidates.push("qualcomm-edl");
  }

  if (
    hasMode(feature, "FASTBOOT") ||
    /firmware|flash|partition|bootloader|factory reset|demo to retail|read backup|backup fw/.test(
      text,
    )
  ) {
    candidates.push("firmware-lab");
  }

  if (hasMode(feature, "ADB", "DIAG") || /device info|dump/.test(text)) {
    candidates.push("adb-bridge");
  }

  const uniqueCandidates = uniqueWorkspaceList(candidates);
  if (uniqueCandidates.length === 0) {
    return ["control-center"];
  }

  return [...uniqueCandidates].sort((left, right) => {
    return scoreWorkspace(brand, groupTitle, feature, right) - scoreWorkspace(brand, groupTitle, feature, left);
  });
}

function buildCommandHint(feature: KotlinFeatureItem, workspaceId: WorkspaceId): string | null {
  const text = `${feature.id} ${feature.label}`.toLowerCase();
  const isFrp = /frp/.test(text);
  const isImei = /imei|nv|efs/.test(text);
  const isFirmware = /firmware|flash|partition/.test(text);
  const isUnlock = /unlock|relock/.test(text);
  const isAuth = /auth|sla|meta|preloader|brom/.test(text);

  switch (workspaceId) {
    case "adb-bridge":
      if (isFrp) return "adb_erase_frp_partition";
      if (isImei) return "adb_get_full_info";
      if (isFirmware || isUnlock) return "adb_reboot_device";
      return "adb_shell_command";
    case "mtk-brom":
      if (isFrp) return "mtk_erase_frp";
      if (isImei && /repair|write/.test(text)) return "mtk_write_imei";
      if (isImei) return "mtk_read_imei";
      if (isFirmware) return "mtk_da_write_partition";
      if (isAuth) return "mtk_bypass_sla";
      return "mtk_handshake_and_identify";
    case "qualcomm-edl":
      if (isFrp) return "edl_erase_partition";
      if (isFirmware) return "edl_write_partition";
      return "edl_sahara_handshake";
    case "firmware-lab":
      if (/read|backup/.test(text)) return "rom_build_flash_plan";
      if (isFirmware) return "rom_flash_partition";
      if (isUnlock) return "fastboot_unlock_bootloader";
      return "fastboot_get_all_variables";
    case "samsung-odin":
      if (isFrp) return "samsung_do_erase_frp_cmd";
      if (isFirmware) return "samsung_flash_part_cmd";
      return "samsung_do_handshake_cmd";
    default:
      return null;
  }
}

function buildExecutionHint(
  feature: KotlinFeatureItem,
  workspaceId: WorkspaceId,
  workspaceCandidates: WorkspaceId[],
  commandHint: string | null,
): string {
  const modeSummary = feature.modes.length > 0 ? feature.modes.join(" / ") : "device guided";
  const chipsetSummary = feature.chipsets.length > 0 ? ` · ${feature.chipsets.join(", ")}` : "";
  const alternateWorkspaces = workspaceCandidates
    .filter((candidate) => candidate !== workspaceId)
    .map((candidate) => WORKSPACE_META[candidate].label)
    .join(" • ");

  if (commandHint) {
    return `Primary desktop route: ${WORKSPACE_META[workspaceId].label}. Command bridge: ${commandHint}. Modes: ${modeSummary}${chipsetSummary}.`;
  }

  if (alternateWorkspaces) {
    return `Primary desktop route: ${WORKSPACE_META[workspaceId].label}. Alternate labs: ${alternateWorkspaces}. Modes: ${modeSummary}${chipsetSummary}.`;
  }

  return `Primary desktop route: ${WORKSPACE_META[workspaceId].label}. Modes: ${modeSummary}${chipsetSummary}.`;
}

export const FEATURE_REMAPPINGS: RemappedFeature[] = KOTLIN_BRAND_FEATURES.flatMap((brandSet) =>
  brandSet.groups.flatMap((group) =>
    group.features.map((feature) => {
      const workspaceCandidates = buildWorkspaceCandidates(brandSet.brand, group.title, feature);
      const workspaceId = workspaceCandidates[0];
      const commandHint = buildCommandHint(feature, workspaceId);

      return {
        ...feature,
        brand: brandSet.brand,
        groupId: group.id,
        groupTitle: group.title,
        featurePath: `${brandSet.brand}:${group.id}:${feature.id}`,
        workspaceId,
        workspaceLabel: WORKSPACE_META[workspaceId].label,
        workspaceCandidates,
        commandHint,
        executionHint: buildExecutionHint(feature, workspaceId, workspaceCandidates, commandHint),
        automation: commandHint ? "direct" : "guided",
      };
    }),
  ),
);

export const BRAND_OPTIONS = ["All", ...KOTLIN_BRAND_FEATURES.map((brandSet) => brandSet.brand)];

export const FEATURE_SUMMARY = {
  totalBrands: KOTLIN_BRAND_FEATURES.length,
  totalGroups: KOTLIN_BRAND_FEATURES.reduce((sum, brandSet) => sum + brandSet.groups.length, 0),
  totalFeatures: FEATURE_REMAPPINGS.length,
  authRequiredCount: FEATURE_REMAPPINGS.filter((feature) => feature.requiresAuth).length,
  criticalCount: FEATURE_REMAPPINGS.filter((feature) => feature.risk === "CRITICAL").length,
  workspaceCounts: FEATURE_REMAPPINGS.reduce<Record<WorkspaceId, number>>((counts, feature) => {
    counts[feature.workspaceId] += 1;
    return counts;
  }, { ...WORKSPACE_COUNT_TEMPLATE }),
};
