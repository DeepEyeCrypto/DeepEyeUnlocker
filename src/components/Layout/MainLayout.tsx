import { useCallback, useEffect, useMemo, useRef, useState, type CSSProperties } from 'react';
import { Sidebar } from './Sidebar';
import { DeviceStatusBar } from '../device/DeviceStatusBar';
import { ExecutionConsole } from './ExecutionConsole';
import { HistoryScreen } from '../history/HistoryScreen';
import { WifiAdbScreen } from '../adb/WifiAdbScreen';
import { SignalBypassFlow } from '../ios/SignalBypassFlow';
import { safeListen } from '../../lib/tauri-utils';
import DashboardPage from '../../pages/Dashboard';
import AdbPage from '../../pages/AdbPage';
import EdlPage from '../../pages/EdlPage';
import MtkBromPage from '../../pages/MtkBromPage';
import SamsungPage from '../../pages/SamsungPage';
import SettingsPage from '../../pages/SettingsPage';
import RomManager from '../pages/RomManager';
import { FeatureRemapStudio } from '../workspace/FeatureRemapStudio';
import { useDevicePolling } from '../../hooks/useDevicePolling';
import { DEFAULT_APP_SETTINGS, loadAppSettings, saveAppSettings, type AppSettings } from '../../lib/settings';
import { checkForUpdate, type UpdateInfo, type UpdateStatus } from '../../lib/updater';
import { getPlatform, initPlatform, type Platform } from '../../lib/platform';
import {
  FEATURE_SUMMARY,
  NAVIGATION_ITEMS,
  WORKSPACE_META,
  type NavigationItem,
  type RemappedFeature,
  type WorkspaceId,
} from '../../lib/desktopWorkspace';
import './MainLayout.css';

type MetricCard = {
  label: string;
  value: string;
  meta: string;
};

function humanizeConnectionState(value: string): string {
  return value
    .replace(/([A-Z])/g, ' $1')
    .replace(/[_-]+/g, ' ')
    .replace(/\b\w/g, (character) => character.toUpperCase())
    .trim();
}

function humanizeUpdateStatus(status: UpdateStatus): string {
  switch (status) {
    case 'available':
      return 'Available';
    case 'checking':
      return 'Checking';
    case 'upToDate':
      return 'Up to date';
    case 'installing':
      return 'Installing';
    case 'error':
      return 'Error';
    default:
      return 'Idle';
  }
}

type LayoutZone = 'mobile' | 'tablet' | 'desktop';

function get_layout_zone(): LayoutZone {
  if (typeof window === 'undefined') {
    return 'desktop';
  }

  if (window.innerWidth < 640) {
    return 'mobile';
  }

  if (window.innerWidth < 1024) {
    return 'tablet';
  }

  return 'desktop';
}

function get_default_sidebar_collapsed(): boolean {
  return get_layout_zone() === 'tablet';
}

export function MainLayout() {
  const [activeWorkspace, setActiveWorkspace] = useState<WorkspaceId>('control-center');
  const [sidebarCollapsed, setSidebarCollapsed] = useState<boolean>(() => get_default_sidebar_collapsed());
  const [platform, setPlatform] = useState<Platform | null>(getPlatform());
  const [settings, setSettings] = useState<AppSettings>(() => loadAppSettings());
  const [consoleLines, setConsoleLines] = useState<string[]>([
    '[system] DeepEye integrated desktop workspace initialized',
  ]);
  const [updateStatus, setUpdateStatus] = useState<UpdateStatus>('idle');
  const [updateInfo, setUpdateInfo] = useState<UpdateInfo | null>(null);
  const [updateMessage, setUpdateMessage] = useState('Update channel idle.');
  const deviceLogIndexRef = useRef(0);
  const layoutZoneRef = useRef<LayoutZone>(get_layout_zone());

  const {
    devices,
    primaryDevice,
    state: connectionState,
    error,
    logs,
    refresh,
  } = useDevicePolling(settings.usbDetectIntervalMs);

  const appendConsole = useCallback((line: string) => {
    const timestamp = new Date().toLocaleTimeString();
    const entry = `[${timestamp}] ${line}`;

    setConsoleLines((previous) => {
      if (previous[previous.length - 1] === entry) {
        return previous;
      }

      return [...previous.slice(-299), entry];
    });
  }, []);

  const clearConsole = useCallback(() => {
    setConsoleLines([]);
  }, []);

  const handleSettingsChange = useCallback((next: Partial<AppSettings>) => {
    setSettings((previous) => (Object.keys(next).length === 0 ? DEFAULT_APP_SETTINGS : { ...previous, ...next }));
    appendConsole(
      Object.keys(next).length === 0
        ? '[settings] Desktop bridge settings restored to defaults.'
        : '[settings] Desktop bridge settings updated.',
    );
  }, [appendConsole]);

  const handleCheckForUpdates = useCallback(async () => {
    setUpdateStatus('checking');
    setUpdateMessage('Checking for desktop updates...');
    appendConsole('[updater] Checking for desktop updates.');

    try {
      const nextUpdate = await checkForUpdate();
      if (nextUpdate) {
        setUpdateInfo(nextUpdate);
        setUpdateStatus('available');
        setUpdateMessage(`Update v${nextUpdate.version} available.`);
        appendConsole(`[updater] Update v${nextUpdate.version} available.`);
        return;
      }

      setUpdateInfo(null);
      setUpdateStatus('upToDate');
      setUpdateMessage('DeepEye is up to date.');
      appendConsole('[updater] No newer release found.');
    } catch (updateError: unknown) {
      const message = String(updateError);
      setUpdateStatus('error');
      setUpdateMessage(message);
      appendConsole(`[updater] ${message}`);
    }
  }, [appendConsole]);

  useEffect(() => {
    void initPlatform()
      .then((detectedPlatform) => setPlatform(detectedPlatform))
      .catch(() => setPlatform(getPlatform()));
  }, []);

  useEffect(() => {
    if (typeof window === 'undefined') {
      return;
    }

    const sync_layout_zone = () => {
      const next_zone = get_layout_zone();

      if (next_zone === layoutZoneRef.current) {
        return;
      }

      layoutZoneRef.current = next_zone;
      setSidebarCollapsed(next_zone === 'tablet');
    };

    window.addEventListener('resize', sync_layout_zone);
    sync_layout_zone();

    return () => {
      window.removeEventListener('resize', sync_layout_zone);
    };
  }, []);

  useEffect(() => {
    saveAppSettings(settings);
  }, [settings]);

  useEffect(() => {
    if (logs.length <= deviceLogIndexRef.current) {
      return;
    }

    const nextLogs = logs.slice(deviceLogIndexRef.current);
    deviceLogIndexRef.current = logs.length;
    nextLogs.forEach((line) => appendConsole(line));
  }, [appendConsole, logs]);

  useEffect(() => {
    appendConsole(`[workspace] ${WORKSPACE_META[activeWorkspace].label} ready.`);
  }, [activeWorkspace, appendConsole]);

  useEffect(() => {
    let disposed = false;
    let stopBackendLog: (() => void) | undefined;
    let stopProfileDetect: (() => void) | undefined;

    void safeListen<string>('log', (event) => {
      appendConsole(String(event.payload));
    }).then((unlisten) => {
      if (disposed) {
        unlisten();
        return;
      }
      stopBackendLog = unlisten;
    });

    void safeListen<string>('device-profile-detected', (event) => {
      const payload = String(event.payload).toLowerCase();
      const suggestedWorkspace: WorkspaceId = payload.includes('samsung')
        ? 'samsung-odin'
        : payload.includes('qualcomm') || payload.includes('edl')
          ? 'qualcomm-edl'
          : payload.includes('mtk')
            ? 'mtk-brom'
            : 'control-center';

      appendConsole(
        `[detect] ${payload.toUpperCase()} profile detected. Suggested lab: ${WORKSPACE_META[suggestedWorkspace].label}.`,
      );
    }).then((unlisten) => {
      if (disposed) {
        unlisten();
        return;
      }
      stopProfileDetect = unlisten;
    });

    return () => {
      disposed = true;
      stopBackendLog?.();
      stopProfileDetect?.();
    };
  }, [appendConsole]);

  const navigationItems = useMemo<NavigationItem[]>(() => {
    const dynamicBadges: Partial<Record<WorkspaceId, string>> = {
      'control-center': devices.length > 0 ? String(devices.length) : undefined,
      'feature-remap': String(FEATURE_SUMMARY.totalFeatures),
      'adb-bridge': String(FEATURE_SUMMARY.workspaceCounts['adb-bridge']),
      'firmware-lab': String(FEATURE_SUMMARY.workspaceCounts['firmware-lab']),
      'mtk-brom': String(FEATURE_SUMMARY.workspaceCounts['mtk-brom']),
      'qualcomm-edl': String(FEATURE_SUMMARY.workspaceCounts['qualcomm-edl']),
      'samsung-odin': String(FEATURE_SUMMARY.workspaceCounts['samsung-odin']),
      'wireless-adb': 'ADB',
      'signal-bypass': 'A12+',
      settings: updateInfo ? `v${updateInfo.version}` : undefined,
    };

    return NAVIGATION_ITEMS.map((item) => ({
      ...item,
      badge: dynamicBadges[item.id] ?? item.badge,
    }));
  }, [devices.length, updateInfo]);

  const metricCards = useMemo<MetricCard[]>(() => {
    const activeWorkspaceRemaps = FEATURE_SUMMARY.workspaceCounts[activeWorkspace] || FEATURE_SUMMARY.totalFeatures;

    return [
      {
        label: 'Live devices',
        value: String(devices.length),
        meta: primaryDevice ? primaryDevice.model : 'USB watcher active',
      },
      {
        label: 'Connection state',
        value: humanizeConnectionState(connectionState),
        meta: error || primaryDevice?.mode || 'Waiting for link',
      },
      {
        label: 'Mapped features',
        value: String(activeWorkspaceRemaps),
        meta:
          activeWorkspace === 'feature-remap'
            ? `${FEATURE_SUMMARY.totalBrands} imported brand catalogs`
            : `${WORKSPACE_META[activeWorkspace].label} target set`,
      },
      {
        label: 'Updates',
        value: updateInfo ? `v${updateInfo.version}` : humanizeUpdateStatus(updateStatus),
        meta: updateMessage,
      },
    ];
  }, [activeWorkspace, connectionState, devices.length, error, primaryDevice, updateInfo, updateMessage, updateStatus]);

  const handleOpenWorkspace = useCallback((workspaceId: WorkspaceId, feature: RemappedFeature) => {
    appendConsole(
      `[remap] ${feature.brand} • ${feature.label} → ${WORKSPACE_META[workspaceId].label}${feature.commandHint ? ` (${feature.commandHint})` : ''}`,
    );
    setActiveWorkspace(workspaceId);
  }, [appendConsole]);

  const workspaceMeta = WORKSPACE_META[activeWorkspace];
  const appBodyStyle = useMemo<CSSProperties>(
    () =>
      ({
        '--sidebar-current-width': sidebarCollapsed ? 'var(--sidebar-collapsed)' : 'var(--sidebar-width)',
      }) as CSSProperties,
    [sidebarCollapsed],
  );

  const renderWorkspace = () => {
    switch (activeWorkspace) {
      case 'control-center':
        return (
          <DashboardPage
            platform={platform}
            connectionState={connectionState}
            error={error}
            device={primaryDevice}
            onRefresh={refresh}
          />
        );
      case 'feature-remap':
        return <FeatureRemapStudio onOpenWorkspace={handleOpenWorkspace} />;
      case 'adb-bridge':
        return <AdbPage />;
      case 'wireless-adb':
        return <WifiAdbScreen />;
      case 'firmware-lab':
        return <RomManager />;
      case 'mtk-brom':
        return <MtkBromPage />;
      case 'qualcomm-edl':
        return <EdlPage />;
      case 'samsung-odin':
        return <SamsungPage />;
      case 'signal-bypass':
        return <SignalBypassFlow onClose={() => setActiveWorkspace('control-center')} />;
      case 'history':
        return <HistoryScreen />;
      case 'settings':
        return (
          <SettingsPage
            settings={settings}
            onSettingsChange={handleSettingsChange}
            onCheckForUpdates={handleCheckForUpdates}
            updateStatus={updateStatus}
            updateInfo={updateInfo}
            updateMessage={updateMessage}
          />
        );
      default:
        return null;
    }
  };

  return (
    <div className="app-layout main-layout">
      <DeviceStatusBar />
      
      <div className="app-body" style={appBodyStyle}>
        <Sidebar
          active={activeWorkspace}
          collapsed={sidebarCollapsed}
          items={navigationItems}
          connectedCount={devices.length}
          featureCount={FEATURE_SUMMARY.totalFeatures}
          onSelect={setActiveWorkspace}
          onToggleCollapsed={() => setSidebarCollapsed((previous) => !previous)}
        />
        
        <main className="main-content">
          <div className="main-scroll-region">
            <div className="workspace-shell">
              <section className="workspace-header glass-card" style={{ '--workspace-accent': workspaceMeta.color } as CSSProperties}>
                <div className="workspace-header__copy">
                  <span className="workspace-eyebrow">{workspaceMeta.eyebrow}</span>
                  <h1 className="workspace-title">
                    <span className="workspace-title__icon" aria-hidden="true">{workspaceMeta.icon}</span>
                    {workspaceMeta.label}
                  </h1>
                  <p className="workspace-description">{workspaceMeta.description}</p>
                </div>

                <div className="workspace-header__status">
                  <div className={`workspace-status workspace-status--${connectionState}`}>
                    {humanizeConnectionState(connectionState)}
                  </div>
                  <div className="workspace-status__meta">
                    {primaryDevice
                      ? `${primaryDevice.model} · ${primaryDevice.mode} · ${primaryDevice.source}`
                      : error || 'No device linked'}
                  </div>
                </div>
              </section>

              <section className="workspace-metric-grid metrics-row">
                {metricCards.map((metric) => (
                  <article key={metric.label} className="workspace-metric-card metric-card glass-card">
                    <span className="workspace-metric-card__label">{metric.label}</span>
                    <strong className="workspace-metric-card__value">{metric.value}</strong>
                    <span className="workspace-metric-card__meta">{metric.meta}</span>
                  </article>
                ))}
              </section>

              <section className="workspace-content">
                {renderWorkspace()}
              </section>
            </div>
          </div>
        </main>

        <ExecutionConsole lines={consoleLines} onClear={clearConsole} title="DESKTOP OPERATIONS BUS" />
      </div>
    </div>
  );
}
