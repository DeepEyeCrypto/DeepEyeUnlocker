import { platform as tauriPlatform } from "@tauri-apps/plugin-os";

export type Platform = "macos" | "windows" | "linux" | "android" | "ios";

let cachedPlatform: Platform | null = null;

function fallbackPlatform(): Platform {
  const ua = typeof navigator !== "undefined" ? navigator.userAgent.toLowerCase() : "";
  if (ua.includes("android")) return "android";
  if (ua.includes("iphone") || ua.includes("ipad") || ua.includes("ipod")) return "ios";
  if (ua.includes("win")) return "windows";
  if (ua.includes("mac")) return "macos";
  return "linux";
}

export async function initPlatform(): Promise<Platform> {
  if (cachedPlatform) return cachedPlatform;
  try {
    const p = (await tauriPlatform()) as Platform;
    cachedPlatform = p;
  } catch {
    cachedPlatform = fallbackPlatform();
  }
  document.documentElement.setAttribute("data-platform", cachedPlatform);
  return cachedPlatform;
}

export function getPlatform(): Platform | null {
  return cachedPlatform;
}

export function isMobile(): boolean {
  return cachedPlatform === "android" || cachedPlatform === "ios";
}

export function isDesktop(): boolean {
  return cachedPlatform === "macos" || cachedPlatform === "windows" || cachedPlatform === "linux";
}

