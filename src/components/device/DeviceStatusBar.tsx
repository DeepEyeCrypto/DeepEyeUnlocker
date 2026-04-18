import { useState, useEffect } from 'react';
import { invoke } from '@tauri-apps/api/core';
import { listen } from '@tauri-apps/api/event';
import './DeviceStatusBar.css';

interface DetectedDevice {
  name: string;
  mode: string;
  vendor_id: number;
  product_id: number;
  manufacturer: string | null;
  serial: string | null;
  bus: number;
  address: number;
  speed: string;
}

export function DeviceStatusBar() {
  const [device, setDevice] = useState<DetectedDevice | null>(null);

  useEffect(() => {
    // Listen for USB device events from Rust
    const unlisten = listen('device-detected', (event) => {
      setDevice(event.payload as DetectedDevice);
    });
    
    // Poll every 2 seconds for initial state or missed events
    const interval = setInterval(async () => {
      try {
        const d = await invoke<DetectedDevice | null>('get_connected_device');
        setDevice(d ?? null);
      } catch (e) {
        console.error("Failed to poll device:", e);
        setDevice(null);
      }
    }, 2000);

    return () => {
      unlisten.then(f => f());
      clearInterval(interval);
    };
  }, []);

  return (
    <div className="device-status-bar glass">
      <div className={`device-indicator ${device ? 'connected' : 'disconnected'}`}>
        <span className="blink-dot" />
        {device ? (
          <div className="device-info-row">
            <span className="device-name">{device.name}</span>
            <span className="device-badge">{device.mode}</span>
            <span className="device-ids">
              0x{device.vendor_id.toString(16).padStart(4, '0')}:0x{device.product_id.toString(16).padStart(4, '0')}
            </span>
            {device.speed && device.speed !== 'Unknown' && (
              <span className="device-speed">{device.speed}</span>
            )}
          </div>
        ) : (
          <span className="no-device">Searching for USB/OTG device...</span>
        )}
      </div>
      
      <div className="status-meta">
        <span className="system-time">{new Date().toLocaleTimeString()}</span>
      </div>
    </div>
  );
}

