import type { Platform } from "../../lib/platform";

export type NavId =
  | "dashboard"
  | "adbtools"
  | "frp"
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
  | "advanced"
  | "updater"
  | "edl"
  | "mtk"
  | "romflasher"
  | "history"
  | "mtkbrom";

type NavAvailability = "all" | "desktop" | "android";

export type NavItem = {
  id: NavId;
  icon: string;
  label: string;
  visibleOn?: NavAvailability;
  enabledOn?: NavAvailability;
  disabledHint?: string;
  badge?: string;
  androidOrder?: number;
};

export type ResolvedNavItem = NavItem & {
  disabled: boolean;
  title: string;
};

const DESKTOP_ONLY_HINT = "Available on Desktop only";

export const NAV_ITEMS: NavItem[] = [
  { id: "dashboard", icon: "H", label: "Dashboard", androidOrder: 0 },
  { id: "adbtools", icon: "AD", label: "ADB Tools", visibleOn: "android", enabledOn: "android", androidOrder: 1 },
  { id: "frp", icon: "FR", label: "FRP Bypass", androidOrder: 2 },
  { id: "edl", icon: "⚡", label: "Qualcomm EDL", androidOrder: 3 },
  { id: "mtk", icon: "MT", label: "MTK Tools", visibleOn: "android", enabledOn: "android", androidOrder: 4 },
  { id: "mtkbrom", icon: "BR", label: "MTK BROM", androidOrder: 4.5 },
  { id: "romflasher", icon: "FB", label: "Fastboot Flash", androidOrder: 5 },
  { id: "identity", icon: "I", label: "Identity", androidOrder: 6 },
  { id: "history", icon: "L", label: "History", androidOrder: 7 },
  { id: "updater", icon: "U", label: "Updater", androidOrder: 8 },
  { id: "activation", icon: "A", label: "Activation", enabledOn: "desktop", disabledHint: DESKTOP_ONLY_HINT, badge: "DESKTOP", androidOrder: 20 },
  { id: "jailbreak", icon: "J", label: "Jailbreak", enabledOn: "desktop", disabledHint: DESKTOP_ONLY_HINT, badge: "DESKTOP", androidOrder: 21 },
  { id: "toolbox", icon: "T", label: "Toolbox", enabledOn: "desktop", disabledHint: DESKTOP_ONLY_HINT, badge: "DESKTOP", androidOrder: 22 },
  { id: "fmi", icon: "F", label: "FMI", enabledOn: "desktop", disabledHint: DESKTOP_ONLY_HINT, badge: "DESKTOP", androidOrder: 23 },
  { id: "purple", icon: "P", label: "Purple", enabledOn: "desktop", disabledHint: DESKTOP_ONLY_HINT, badge: "DESKTOP", androidOrder: 24 },
  { id: "bootfiles", icon: "B", label: "BootFiles", enabledOn: "desktop", disabledHint: DESKTOP_ONLY_HINT, badge: "DESKTOP", androidOrder: 25 },
  { id: "shsh", icon: "S", label: "SHSH", enabledOn: "desktop", disabledHint: DESKTOP_ONLY_HINT, badge: "DESKTOP", androidOrder: 26 },
  { id: "diagnostics", icon: "D", label: "Diagnostics", enabledOn: "desktop", disabledHint: DESKTOP_ONLY_HINT, badge: "DESKTOP", androidOrder: 27 },
  { id: "restore", icon: "R", label: "Restore", enabledOn: "desktop", disabledHint: DESKTOP_ONLY_HINT, badge: "DESKTOP", androidOrder: 28 },
  { id: "cve", icon: "C", label: "CVE", enabledOn: "desktop", disabledHint: DESKTOP_ONLY_HINT, badge: "DESKTOP", androidOrder: 29 },
  { id: "vault", icon: "V", label: "Vault", enabledOn: "desktop", disabledHint: DESKTOP_ONLY_HINT, badge: "DESKTOP", androidOrder: 30 },
  { id: "extraction", icon: "E", label: "Extraction", enabledOn: "desktop", disabledHint: DESKTOP_ONLY_HINT, badge: "DESKTOP", androidOrder: 31 },
  { id: "advanced", icon: "X", label: "Advanced", enabledOn: "desktop", disabledHint: DESKTOP_ONLY_HINT, badge: "DESKTOP", androidOrder: 32 },
];

function isAndroidPlatform(platform: Platform | null): boolean {
  return platform === "android";
}

function isVisibleOnPlatform(item: NavItem, platform: Platform | null): boolean {
  const visibleOn = item.visibleOn ?? "all";
  if (visibleOn === "all") return true;
  if (visibleOn === "android") return isAndroidPlatform(platform);
  return !isAndroidPlatform(platform);
}

function isEnabledOnPlatform(item: NavItem, platform: Platform | null): boolean {
  const enabledOn = item.enabledOn ?? "all";
  if (enabledOn === "all") return true;
  if (enabledOn === "android") return isAndroidPlatform(platform);
  return !isAndroidPlatform(platform);
}

export function getNavItems(platform: Platform | null): ResolvedNavItem[] {
  const resolved = NAV_ITEMS
    .map((item, index) => ({ item, index }))
    .filter(({ item }) => isVisibleOnPlatform(item, platform))
    .map(({ item, index }) => {
      const disabled = !isEnabledOnPlatform(item, platform);
      return {
        ...item,
        disabled,
        title: disabled ? item.disabledHint ?? item.label : item.label,
        index,
      };
    });

  if (isAndroidPlatform(platform)) {
    resolved.sort((left, right) => {
      const leftOrder = left.androidOrder ?? Number.MAX_SAFE_INTEGER;
      const rightOrder = right.androidOrder ?? Number.MAX_SAFE_INTEGER;
      if (leftOrder !== rightOrder) {
        return leftOrder - rightOrder;
      }
      return left.index - right.index;
    });
  }

  return resolved.map(({ index: _index, ...item }) => item);
}
