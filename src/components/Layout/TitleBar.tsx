import { getCurrentWindow } from "@tauri-apps/api/window";
import { getPlatform } from "../../lib/platform";

export function TitleBar() {
  const win = getCurrentWindow();
  const platform = getPlatform();
  
  // macOS has native traffic lights, only show minimal drag region
  if (platform === "macos") {
    return (
      <div className="titlebar titlebar-macos" data-tauri-drag-region>
        <span className="titlebar-title titlebar-macos-title">DeepEye Unlocker</span>
      </div>
    );
  }
  
  // Windows/Linux: show custom titlebar with window controls
  return (
    <div className="titlebar titlebar-windows" data-tauri-drag-region>
      <div className="titlebar-left" data-tauri-drag-region>
        <div className="window-controls">
          <button 
            onClick={() => win.close().catch(() => {})} 
            className="window-btn window-close" 
            title="Close" 
            aria-label="Close window"
          />
          <button 
            onClick={() => win.minimize().catch(() => {})} 
            className="window-btn window-minimize" 
            title="Minimize" 
            aria-label="Minimize window"
          />
          <button 
            onClick={() => win.toggleMaximize().catch(() => {})} 
            className="window-btn window-maximize" 
            title="Maximize" 
            aria-label="Maximize window"
          />
        </div>
        <span className="titlebar-title">DeepEye Unlocker</span>
      </div>
    </div>
  );
}

