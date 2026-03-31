import { useMemo, useState } from "react";
import { AppShell } from "./components/Layout/AppShell";
import type { NavId } from "./components/Layout/types";
import { DeviceCard } from "./components/DeviceCard";
import { TerminalLog } from "./components/ui/TerminalLog";
import DashboardPage from "./pages/Dashboard";
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
import UpdaterPage from "./components/pages/Updater";
import EdlPage from "./components/pages/EdlMode";
import RomFlasherPage from "./components/pages/RomFlasher";
import DeviceHistoryPage from "./components/pages/DeviceHistory";

const PAGES: Record<NavId, JSX.Element> = {
  dashboard: <DashboardPage />,
  activation: <Activation />,
  fmi: <FMIPage />,
  jailbreak: <Jailbreak />,
  purple: <PurplePage />,
  bootfiles: <BootFilesPage />,
  toolbox: <Toolbox />,
  shsh: <SHSHPage />,
  diagnostics: <DiagnosticsPage />,
  restore: <RestorePage />,
  cve: <CveDashboard />,
  vault: <VaultPage />,
  identity: <IdentityPage />,
  extraction: <MassExtraction />,
  advanced: <AdvancedPage />,
  updater: <UpdaterPage />,
  edl: <EdlPage />,
  romflasher: <RomFlasherPage />,
  history: <DeviceHistoryPage />,
};

export default function App() {
  const [page, setPage] = useState<NavId>("dashboard");
  const logs = useMemo(
    () => [
      "[info] USB transport initialized",
      "Device handshake complete",
      "Mode detected: Apple Recovery",
      "Ready for operation dispatch",
    ],
    [],
  );

  return (
    <AppShell active={page} onNavigate={setPage}>
      <div className="dashboard-grid">
        <DeviceCard
          device={{
            status: "connected",
            model: "DeepEye Testbed",
            serial: "DEEPEYE-OTG-001",
            os: "Android 14 / iOS 17",
            mode: "Recovery",
            bootloaderStatus: "Unlocked",
            carrier: "Research Lab",
          }}
        />

        <div className="card">
          <div className="card-header">
            <h3 className="card-title">Operation Console</h3>
          </div>
          <div className="card-body">{PAGES[page]}</div>
        </div>

        <TerminalLog lines={logs} />
      </div>
    </AppShell>
  );
}

