import { Suspense, useState } from "react";
import { AppShell } from "./components/Layout/AppShell";
import type { NavId } from "./components/Layout/types";
import { DeviceCard } from "./components/DeviceCard";
import { TerminalLog } from "./components/ui/TerminalLog";
import { PageSkeleton } from "./components/ui/PageSkeleton";
import { useDevicePolling } from "./hooks/useDevicePolling";
import { DASHBOARD_CONFIG } from "./lib/dashboard";
import { getPlatform } from "./lib/platform";
import DashboardPage from "./pages/Dashboard";

// Lazy-loaded heavy pages
import React from "react";
const Activation = React.lazy(() => import("./components/pages/Activation.tsx"));
const Jailbreak = React.lazy(() => import("./components/pages/Jailbreak.tsx"));
const Toolbox = React.lazy(() => import("./components/pages/Toolbox.tsx"));
const FMIPage = React.lazy(() => import("./components/pages/FMI.tsx"));
const PurplePage = React.lazy(() => import("./components/pages/PurpleMode.tsx"));
const BootFilesPage = React.lazy(() => import("./components/pages/BootFiles.tsx"));
const SHSHPage = React.lazy(() => import("./components/pages/SHSH.tsx"));
const DiagnosticsPage = React.lazy(() => import("./components/pages/Diagnostics.tsx"));
const RestorePage = React.lazy(() => import("./components/pages/Restore.tsx"));
const CveDashboard = React.lazy(() => import("./components/pages/CveDashboard.tsx"));
const VaultPage = React.lazy(() => import("./components/pages/Vault.tsx"));
const IdentityPage = React.lazy(() => import("./components/pages/Identity.tsx"));
const AdbToolsPage = React.lazy(() => import("./components/pages/AdbTools.tsx"));
const FrpBypassPage = React.lazy(() => import("./components/pages/FrpBypass.tsx"));
const MassExtraction = React.lazy(() => import("./components/pages/MassExtraction.tsx"));
const AdvancedPage = React.lazy(() => import("./components/pages/Advanced.tsx"));
const UpdaterPage = React.lazy(() => import("./components/pages/Updater.tsx"));
const EdlPage = React.lazy(() => import("./components/pages/EdlMode.tsx"));
const MtkToolsPage = React.lazy(() => import("./components/pages/MtkTools.tsx"));
const RomFlasherPage = React.lazy(() => import("./components/pages/RomFlasher.tsx"));
const DeviceHistoryPage = React.lazy(() => import("./components/pages/DeviceHistory.tsx"));
const MtkBromPage = React.lazy(() => import("./pages/MtkBromPage.tsx"));

export default function App() {
  const [page, setPage] = useState<NavId>("dashboard");
  const platform = getPlatform();
  const { primaryDevice, state, error, logs, refresh } = useDevicePolling(
    DASHBOARD_CONFIG.POLLING_INTERVAL_MS,
  );

  const pages: Record<NavId, JSX.Element> = {
    dashboard: (
      <DashboardPage
        platform={platform}
        connectionState={state}
        error={error}
        device={primaryDevice}
        onRefresh={refresh}
      />
    ),
    adbtools: <Suspense fallback={<PageSkeleton />}><AdbToolsPage /></Suspense>,
    frp: <Suspense fallback={<PageSkeleton />}><FrpBypassPage /></Suspense>,
    activation: <Suspense fallback={<PageSkeleton />}><Activation /></Suspense>,
    fmi: <Suspense fallback={<PageSkeleton />}><FMIPage /></Suspense>,
    jailbreak: <Suspense fallback={<PageSkeleton />}><Jailbreak /></Suspense>,
    purple: <Suspense fallback={<PageSkeleton />}><PurplePage /></Suspense>,
    bootfiles: <Suspense fallback={<PageSkeleton />}><BootFilesPage /></Suspense>,
    toolbox: <Suspense fallback={<PageSkeleton />}><Toolbox /></Suspense>,
    shsh: <Suspense fallback={<PageSkeleton />}><SHSHPage /></Suspense>,
    diagnostics: <Suspense fallback={<PageSkeleton />}><DiagnosticsPage /></Suspense>,
    restore: <Suspense fallback={<PageSkeleton />}><RestorePage /></Suspense>,
    cve: <Suspense fallback={<PageSkeleton />}><CveDashboard /></Suspense>,
    vault: <Suspense fallback={<PageSkeleton />}><VaultPage /></Suspense>,
    identity: <Suspense fallback={<PageSkeleton />}><IdentityPage /></Suspense>,
    extraction: <Suspense fallback={<PageSkeleton />}><MassExtraction /></Suspense>,
    advanced: <Suspense fallback={<PageSkeleton />}><AdvancedPage /></Suspense>,
    updater: <Suspense fallback={<PageSkeleton />}><UpdaterPage /></Suspense>,
    edl: <Suspense fallback={<PageSkeleton />}><EdlPage /></Suspense>,
    mtk: <Suspense fallback={<PageSkeleton />}><MtkToolsPage /></Suspense>,
    romflasher: <Suspense fallback={<PageSkeleton />}><RomFlasherPage /></Suspense>,
    history: <Suspense fallback={<PageSkeleton />}><DeviceHistoryPage /></Suspense>,
    mtkbrom: <Suspense fallback={<PageSkeleton />}><MtkBromPage /></Suspense>,
  };

  return (
    <AppShell active={page} onNavigate={setPage}>
      <div className="dashboard-grid">
        <DeviceCard device={primaryDevice} />

        <div className="card">
          <div className="card-header">
            <h3 className="card-title">Operation Console</h3>
          </div>
          <div className="card-body">{pages[page]}</div>
        </div>

        <TerminalLog lines={logs} />
      </div>
    </AppShell>
  );
}
