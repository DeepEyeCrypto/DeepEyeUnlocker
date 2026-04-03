import { getCurrentWindow } from "@tauri-apps/api/window";

export function TitleBar() {
  const win = getCurrentWindow();
  return (
    <div className="titlebar" data-tauri-drag-region>
      <div className="titlebar-left" data-tauri-drag-region>
        <div className="mac-traffic-lights" onDoubleClick={(e) => e.stopPropagation()}>
          <button onClick={() => void win.close()} className="mac-btn mac-close" title="Close" />
          <button onClick={() => void win.minimize()} className="mac-btn mac-min" title="Minimize" />
          <button onClick={() => void win.toggleMaximize()} className="mac-btn mac-max" title="Maximize" />
        </div>
        <span className="titlebar-title">DeepEye Unlocker</span>
      </div>
    </div>
  );
}

