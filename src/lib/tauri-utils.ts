import { invoke as tauriInvoke } from "@tauri-apps/api/core";
import { listen as tauriListen, type EventCallback, type UnlistenFn } from "@tauri-apps/api/event";

/**
 * Checks if the application is running within a Tauri environment.
 */
export const isTauri = (): boolean => {
  return typeof window !== 'undefined' && (window as any).__TAURI_INTERNALS__ !== undefined;
};

/**
 * Safe wrapper for Tauri's invoke API.
 * Returns mock data or logs a warning if not in Tauri.
 */
export async function safeInvoke<T>(cmd: string, args?: any): Promise<T> {
  if (!isTauri()) {
    console.warn(`[Tauri Mock] invoke("${cmd}") called in non-Tauri environment.`);
    
    // Mock data for common commands
    if (cmd === 'get_connected_devices') return [] as unknown as T;
    if (cmd === 'load_settings') return {} as unknown as T;
    if (cmd === 'db_list_all') return [] as unknown as T;
    
    return undefined as unknown as T;
  }
  return tauriInvoke<T>(cmd, args);
}

/**
 * Safe wrapper for Tauri's listen API.
 */
export async function safeListen<T>(event: string, handler: EventCallback<T>): Promise<UnlistenFn> {
  if (!isTauri()) {
    console.warn(`[Tauri Mock] listen("${event}") called in non-Tauri environment.`);
    return () => {};
  }
  return tauriListen<T>(event, handler);
}
