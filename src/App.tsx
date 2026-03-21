import { useState } from "react";
import { getCurrentWindow } from "@tauri-apps/api/window";
import "./styles/glass.css";
import Sidebar from "./components/Sidebar";
import DeviceBar from "./components/DeviceBar";
import Activation from "./components/pages/Activation";
import Jailbreak from "./components/pages/Jailbreak";
import Toolbox from "./components/pages/Toolbox";
import FMIPage from "./components/pages/FMI";
import PurplePage from "./components/pages/PurpleMode";
import BootFilesPage from "./components/pages/BootFiles";
import SHSHPage from "./components/pages/SHSH";
import DiagnosticsPage from "./components/pages/Diagnostics";
import RestorePage from "./components/pages/Restore";
import CveDashboard from "./components/pages/CveDashboard";
import VaultPage from "./components/pages/Vault";
import IdentityPage from "./components/pages/Identity";
import MassExtraction from "./components/pages/MassExtraction";
import AdvancedPage from "./components/pages/Advanced";

type TauriWindowHost = Window & {
  __TAURI_INTERNALS__?: unknown;
};

const hasTauriWindowRuntime = () => {
  return typeof window !== "undefined" && typeof (window as TauriWindowHost).__TAURI_INTERNALS__ !== "undefined";
};

const runWindowAction = async (action: "close" | "minimize" | "toggleMaximize") => {
  if (!hasTauriWindowRuntime()) {
    console.warn(`[AppShell] window_action_skipped action=${action} reason=tauri_runtime_unavailable`);
    return;
  }

  try {
    const appWindow = getCurrentWindow();

    switch (action) {
      case "close":
        await appWindow.close();
        break;
      case "minimize":
        await appWindow.minimize();
        break;
      case "toggleMaximize":
        await appWindow.toggleMaximize();
        break;
    }
  } catch (error) {
    const reason = error instanceof Error ? error.message : String(error);
    console.error(`[AppShell] window_action_failed action=${action} reason=${reason}`);
  }
};

const PAGES: Record<string, JSX.Element> = {
  activation: <Activation />,
  fmi:         <FMIPage />,
  jailbreak:   <Jailbreak />,
  purple:      <PurplePage />,
  bootfiles:   <BootFilesPage />,
  toolbox:     <Toolbox />,
  shsh:        <SHSHPage />,
  diagnostics: <DiagnosticsPage />,
  restore:     <RestorePage />,
  cve:         <CveDashboard />,
  vault:       <VaultPage />,
  identity:    <IdentityPage />,
  extraction:  <MassExtraction />,
  advanced:    <AdvancedPage />,
};

export default function App() {
  const [page, setPage] = useState("activation");
  const [performanceMode, setPerformanceMode] = useState(false);

  return (
    <div style={{
      display: "flex", flexDirection: "column",
      height: "100vh", padding: "0",
      background: performanceMode ? "#050507" : "radial-gradient(ellipse at 20% 20%, rgba(124,58,237,0.15) 0%, transparent 60%), #0a0a0f",
      userSelect: "none",
      transition: "background 0.5s ease"
    }}>

      {/* Custom Titlebar */}
      <div
        data-tauri-drag-region
        style={{
          height: 40, display: "flex", alignItems: "center",
          justifyContent: "space-between", padding: "0 16px",
          background: "rgba(0,0,0,0.3)",
          borderBottom: "1px solid rgba(255,255,255,0.06)",
          flexShrink: 0,
        }}
      >
        {/* Left Side: Traffic Lights & Title */}
        <div style={{ display: "flex", alignItems: "center", gap: 16 }}>
          <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
            {[
              { id: "close", fn: () => { void runWindowAction("close"); }, color: "#ff5f56" },
              { id: "minimize", fn: () => { void runWindowAction("minimize"); }, color: "#ffbd2e" },
              { id: "maximize", fn: () => { void runWindowAction("toggleMaximize"); }, color: "#27c93f" },
            ].map(b => (
              <button 
                key={b.id} 
                onClick={b.fn} 
                style={{
                  width: 12, height: 12, borderRadius: "50%", border: "1px solid rgba(0,0,0,0.15)",
                  background: b.color, cursor: "pointer", padding: 0
                }} 
              />
            ))}
          </div>

          <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
            <span style={{ fontSize: 13, fontWeight: 700, color: "#a78bfa", letterSpacing: 1 }}>
              👁️ DEEPEYE UNLOCKER
            </span>
            <div style={{ 
              display: "flex", alignItems: "center", gap: 6, 
              padding: "2px 8px", borderRadius: 12, background: "rgba(255,255,255,0.05)",
              border: "1px solid rgba(255,255,255,0.05)"
            }}>
              <div className="cursor-blink" style={{ width: 6, height: 6, borderRadius: "50%", background: "#4ade80" }} />
              <span style={{ fontSize: 9, fontWeight: 800, color: "#94a3b8" }}>MISSION READY</span>
            </div>
          </div>
        </div>

        {/* Right Side: Toggles */}
        <div style={{ display: "flex", gap: 8 }}>
          <button 
            onClick={() => setPerformanceMode(!performanceMode)}
            style={{
              padding: "4px 8px", borderRadius: 6, border: "none",
              background: performanceMode ? "#7c3aed" : "rgba(255,255,255,0.06)",
              color: "#fff", fontSize: 10, fontWeight: 800, cursor: "pointer",
              transition: "all 0.3s"
            }}
          >
            {performanceMode ? "🚀 FAST" : "💎 HQ"}
          </button>
        </div>
      </div>

      {/* Main content */}
      <div style={{ padding: 12, flex: 1, display: "flex", flexDirection: "column", overflow: "hidden", gap: 10 }}>
        <DeviceBar />
        <div style={{ display: "flex", gap: 12, flex: 1, overflow: "hidden" }}>
          <Sidebar active={page} onSelect={setPage} />
          <div className="glass" style={{
            flex: 1, padding: 24, overflowY: "auto", borderRadius: 16
          }}>
            {PAGES[page]}
          </div>
        </div>
      </div>
    </div>
  );
}
