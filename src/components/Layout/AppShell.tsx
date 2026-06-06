import { useEffect } from 'react';
import { Outlet } from 'react-router-dom';
import { SidebarNav } from './SidebarNav';
import { DeviceStatusBar } from './DeviceStatusBar';
import { useAppStore } from '../../stores/useAppStore';
import { useDeviceStatusStore } from '../../stores/useDeviceStatusStore';
import { useOperationSessionStore } from '../../stores/useOperationSessionStore';
import { listen } from '@tauri-apps/api/event';
import { OperationSessionOverlay } from '../ui/OperationSessionOverlay';

export function AppShell() {
  const setDevice = useAppStore((state) => state.setDevice);
  const appendLog = useAppStore((state) => state.appendLog);

  useEffect(() => {
    let unsubscribeDeviceStore: (() => void) | undefined;
    let unsubscribeSessionStore: (() => void) | undefined;

    const init = async () => {
      unsubscribeDeviceStore = await useDeviceStatusStore.getState().bootstrap();
      unsubscribeSessionStore = await useOperationSessionStore.getState().bootstrap();
    };
    init();

    // Sync useDeviceStatusStore with useAppStore's device field for backward compatibility
    const unsubscribeSync = useDeviceStatusStore.subscribe((state) => {
      const snap = state.currentDevice;
      if (snap) {
        setDevice({
          id: snap.id,
          model: snap.model,
          serial: snap.serial,
          os: snap.platform === 'ios' ? 'iOS' : 'Android',
          mode: snap.mode.toUpperCase(),
          bootloaderStatus: 'Unknown',
          carrier: undefined,
          source: snap.platform,
        });
      } else {
        setDevice(null);
      }
    });

    // Listen to rust logs if they emit them
    const unlisten = listen<string>('log_event', (event) => {
      appendLog('info', event.payload);
    });

    return () => {
      if (unsubscribeDeviceStore) {
        unsubscribeDeviceStore();
      }
      if (unsubscribeSessionStore) {
        unsubscribeSessionStore();
      }
      unsubscribeSync();
      unlisten.then((f) => f());
    };
  }, [setDevice, appendLog]);

  return (
    <div className="flex h-screen w-screen overflow-hidden bg-[#0B0D17] text-white">
      {/* Background Ambience */}
      <div className="absolute inset-0 z-0 overflow-hidden pointer-events-none">
        <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] rounded-full bg-[#7C3AED]/20 blur-[120px]" />
        <div className="absolute bottom-[-10%] right-[-10%] w-[30%] h-[30%] rounded-full bg-[#3B82F6]/10 blur-[100px]" />
      </div>

      {/* Tauri Drag Region (transparent titlebar) */}
      <div
        data-tauri-drag-region
        className="absolute top-0 left-0 right-0 h-10 z-0 bg-transparent select-none"
      />

      <SidebarNav />

      <div className="flex flex-col flex-1 relative z-10">
        <main className="flex-1 overflow-y-auto overflow-x-hidden p-8 pt-12">
          <Outlet />
        </main>

        <DeviceStatusBar />
      </div>

      {/* Live Operation Overlay */}
      <OperationSessionOverlay />
    </div>
  );
}
