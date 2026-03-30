import { getCurrentWindow } from "@tauri-apps/api/window";

export function TitleBar() {
  const win = getCurrentWindow();
  return (
    <div className="titlebar" data-tauri-drag-region>
      <div className="titlebar-left" data-tauri-drag-region>
        <span className="titlebar-title">DeepEye Unlocker</span>
      </div>
      <div className="titlebar-controls">
        <button onClick={() => void win.minimize()} className="tb-btn tb-min" title="Minimize">
          <span>─</span>
        </button>
        <button onClick={() => void win.toggleMaximize()} className="tb-btn tb-max" title="Maximize">
          <span>□</span>
        </button>
        <button onClick={() => void win.close()} className="tb-btn tb-close" title="Close">
          <span>✕</span>
        </button>
      </div>
    </div>
  );
}

