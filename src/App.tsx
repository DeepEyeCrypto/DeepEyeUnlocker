import { listen } from "@tauri-apps/api/event";
import { Suspense, useCallback, useEffect, useState } from "react";
import { AppShell } from "./components/Layout/AppShell";
import type { NavId } from "./components/Layout/types";
import { DeviceStatusBar } from "./components/DeviceStatusBar";
import { DeviceCard } from "./components/DeviceCard";
import { UpdateBanner } from "./components/UpdateBanner";
import { TerminalLog } from "./components/ui/TerminalLog";
import { PageSkeleton } from "./components/ui/PageSkeleton";
import { useDevicePolling } from "./hooks/useDevicePolling";
import { DASHBOARD_CONFIG } from "./lib/dashboard";
import { getPlatform } from "./lib/platform";
import {
  DEFAULT_APP_SETTINGS,
  loadAppSettings,
  saveAppSettings,
  type AppSettings,
} from "./lib/settings";
import {
  checkForUpdate,
  installUpdate,
  type UpdateInfo,
  type UpdateStatus,
} from "./lib/updater";
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
const FrpBypassPage = React.lazy(() => import("./components/pages/FrpBypass.tsx"));
const MassExtraction = React.lazy(() => import("./components/pages/MassExtraction.tsx"));
const AdvancedPage = React.lazy(() => import("./components/pages/Advanced.tsx"));
const UpdaterPage = React.lazy(() => import("./components/pages/Updater.tsx"));
const EdlPage = React.lazy(() => import("./pages/EdlPage.tsx"));
const MtkToolsPage = React.lazy(() => import("./components/pages/MtkTools.tsx"));
const RomFlasherPage = React.lazy(() => import("./components/pages/RomFlasher.tsx"));
const RomManagerPage = React.lazy(() => import("./components/pages/RomManager.tsx"));
const DeviceHistoryPage = React.lazy(() => import("./components/pages/DeviceHistory.tsx"));
const MtkBromPage = React.lazy(() => import("./pages/MtkBromPage.tsx"));
const AdbPage = React.lazy(() => import("./pages/AdbPage.tsx"));
const SamsungPage = React.lazy(() => import("./pages/SamsungPage.tsx"));
const DeviceDbPage = React.lazy(() => import("./components/pages/DeviceDb.tsx"));
const GuidedFrpPage = React.lazy(() => import("./components/pages/GuidedFRP.tsx"));
const LogcatViewerPage = React.lazy(() => import("./components/pages/LogcatViewer.tsx"));
const SettingsPage = React.lazy(() => import("./pages/SettingsPage.tsx"));

const PAGE_PATHS: Record<NavId, string> = {
  dashboard: "/",
  adbtools: "/adb",
  logcat: "/logcat",
  samsung: "/samsung",
  frp: "/frp",
  guidedfrp: "/guided-frp",
  activation: "/activation",
  fmi: "/fmi",
  jailbreak: "/jailbreak",
  purple: "/purple",
  bootfiles: "/bootfiles",
  toolbox: "/toolbox",
  shsh: "/shsh",
  diagnostics: "/diagnostics",
  restore: "/restore",
  cve: "/cve",
  vault: "/vault",
  identity: "/identity",
  extraction: "/extraction",
  advanced: "/advanced",
  updater: "/updater",
  edl: "/edl",
  mtk: "/mtk",
  romflasher: "/romflasher",
  rommanager: "/rom-manager",
  history: "/history",
  mtkbrom: "/mtk-brom",
  devicedb: "/device-db",
  settings: "/settings",
};

function normalizePathname(pathname: string): string {
  if (pathname === "/index.html") {
    return "/";
  }

  if (pathname.length > 1 && pathname.endsWith("/")) {
    return pathname.slice(0, -1);
  }

  return pathname;
}

function getPageFromPath(pathname: string): NavId {
  const normalized = normalizePathname(pathname);
  const entry = Object.entries(PAGE_PATHS).find(([, path]) => path === normalized);
  return (entry?.[0] as NavId | undefined) ?? "dashboard";
}

export default function App() {
  const [page, setPage] = useState<NavId>(() => {
    if (typeof window === "undefined") {
      return "dashboard";
    }

    return getPageFromPath(window.location.pathname);
  });
  const [settings, setSettings] = useState<AppSettings>(() => ({
    ...DEFAULT_APP_SETTINGS,
    ...loadAppSettings(),
  }));
  const [updateStatus, setUpdateStatus] = useState<UpdateStatus>("idle");
  const [updateInfo, setUpdateInfo] = useState<UpdateInfo | null>(null);
  const [updateMessage, setUpdateMessage] = useState("");
  const [updateDismissed, setUpdateDismissed] = useState(false);
  const platform = getPlatform();
  const { primaryDevice, state, error, logs, refresh } = useDevicePolling(
    settings.usbDetectIntervalMs ?? DASHBOARD_CONFIG.POLLING_INTERVAL_MS,
  );

  useEffect(() => {
    saveAppSettings(settings);
  }, [settings]);

  const navigateTo = useCallback((nextPage: NavId, routeState?: any) => {
    setPage(nextPage);

    if (typeof window === "undefined") {
      return;
    }

    const nextPath = PAGE_PATHS[nextPage];
    if (normalizePathname(window.location.pathname) !== nextPath) {
      window.history.pushState({ page: nextPage, ...routeState }, "", nextPath);
    }
  }, []);

  useEffect(() => {
    const unlisten = listen("navigate-to-protocol", (event: any) => {
      const { route, pre_fill } = event.payload;
      if (route) {
        // Find NavId from path
        const navId = Object.entries(PAGE_PATHS).find(([_, path]) => path === route)?.[0] as NavId;
        if (navId) {
          navigateTo(navId, { autoRouted: true, preFill: pre_fill });
        }
      }
    });

    return () => {
      unlisten.then((fn) => fn());
    };
  }, [navigateTo]);

  useEffect(() => {
    if (typeof window === "undefined") {
      return undefined;
    }

    const handlePopState = () => {
      setPage(getPageFromPath(window.location.pathname));
    };

    window.addEventListener("popstate", handlePopState);
    return () => {
      window.removeEventListener("popstate", handlePopState);
    };
  }, []);

  useEffect(() => {
    if (typeof window === "undefined") {
      return;
    }

    const nextPath = PAGE_PATHS[page];
    if (normalizePathname(window.location.pathname) !== nextPath) {
      window.history.replaceState({ page }, "", nextPath);
    }
  }, [page]);

  const handleSettingsChange = useCallback((next: Partial<AppSettings>) => {
    setSettings((current) => ({
      ...current,
      ...next,
    }));
  }, []);

  const handleCheckForUpdates = useCallback(async () => {
    setUpdateStatus("checking");
    setUpdateMessage("");

    try {
      const nextUpdate = await checkForUpdate();
      if (nextUpdate) {
        setUpdateInfo(nextUpdate);
        setUpdateDismissed(false);
        setUpdateStatus("available");
        return;
      }

      setUpdateInfo(null);
      setUpdateStatus("upToDate");
      setUpdateMessage("DeepEyeUnlocker is already on the latest release.");
    } catch (checkError) {
      setUpdateStatus("error");
      setUpdateMessage(String(checkError));
    }
  }, []);

  const handleInstallUpdate = useCallback(async () => {
    setUpdateStatus("installing");
    setUpdateMessage("");

    try {
      await installUpdate();
    } catch (installError) {
      setUpdateStatus("error");
      setUpdateMessage(String(installError));
    }
  }, []);

  useEffect(() => {
    void handleCheckForUpdates();
  }, [handleCheckForUpdates]);

  const showUpdateBanner =
    updateInfo !== null &&
    !updateDismissed &&
    (updateStatus === "available" || updateStatus === "installing");
  
  useEffect(() => {
    let unlisten: any;
    
    async function setupListener() {
      unlisten = await listen("navigate-to-protocol", (event: any) => {
        const { route, preFill, device } = event.payload;
        console.log("Auto-routing to:", route, preFill);
        
        // Find NavId from path
        const navId = Object.entries(PAGE_PATHS).find(([, path]) => path === route)?.[0] as NavId;
        if (navId) {
          navigateTo(navId, { autoRouted: true, preFill, device });
        }
      });
    }

    setupListener();
    return () => { if (unlisten) unlisten(); };
  }, [navigateTo]);

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
    adbtools: <Suspense fallback={<PageSkeleton />}><AdbPage /></Suspense>,
    logcat: <Suspense fallback={<PageSkeleton />}><LogcatViewerPage /></Suspense>,
    samsung: <Suspense fallback={<PageSkeleton />}><SamsungPage /></Suspense>,
    frp: <Suspense fallback={<PageSkeleton />}><FrpBypassPage /></Suspense>,
    guidedfrp: <Suspense fallback={<PageSkeleton />}><GuidedFrpPage /></Suspense>,
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
    updater: (
      <Suspense fallback={<PageSkeleton />}>
        <UpdaterPage
          status={updateStatus}
          update={updateInfo}
          message={updateMessage}
          onCheck={handleCheckForUpdates}
          onInstall={handleInstallUpdate}
        />
      </Suspense>
    ),
    edl: <Suspense fallback={<PageSkeleton />}><EdlPage /></Suspense>,
    mtk: <Suspense fallback={<PageSkeleton />}><MtkToolsPage /></Suspense>,
    romflasher: <Suspense fallback={<PageSkeleton />}><RomFlasherPage /></Suspense>,
    rommanager: <Suspense fallback={<PageSkeleton />}><RomManagerPage /></Suspense>,
    history: <Suspense fallback={<PageSkeleton />}><DeviceHistoryPage /></Suspense>,
    mtkbrom: <Suspense fallback={<PageSkeleton />}><MtkBromPage /></Suspense>,
    devicedb: <Suspense fallback={<PageSkeleton />}><DeviceDbPage /></Suspense>,
    settings: (
      <Suspense fallback={<PageSkeleton />}>
        <SettingsPage
          settings={settings}
          onSettingsChange={handleSettingsChange}
          onCheckForUpdates={handleCheckForUpdates}
          updateStatus={updateStatus}
          updateInfo={updateInfo}
          updateMessage={updateMessage}
        />
      </Suspense>
    ),
  };

  return (
    <>
      {showUpdateBanner && updateInfo && (
        <UpdateBanner
          update={updateInfo}
          installing={updateStatus === "installing"}
          onInstall={handleInstallUpdate}
          onDismiss={() => setUpdateDismissed(true)}
        />
      )}

      <AppShell active={page} onNavigate={navigateTo}>
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

      <DeviceStatusBar onNavigate={navigateTo} />
    </>
  );
}
