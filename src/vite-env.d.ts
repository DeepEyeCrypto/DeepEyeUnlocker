/// <reference types="vite/client" />

declare const __APP_VERSION__: string;
declare const __CARGO_PKG_VERSION__: string;

interface Window {
  __TAURI__?: Record<string, unknown>
  __TAURI_INVOKE__?: (cmd: string, args?: Record<string, unknown>) => Promise<unknown>
}

declare module "*.svg" {
  const content: string
  export default content
}
