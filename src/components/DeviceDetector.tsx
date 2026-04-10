/**
 * DeviceDetector Component
 * 
 * Real-time USB device detection with protocol classification
 * Displays connected devices with mode-specific styling and actions
 */

import React, { useState, useEffect, useCallback } from 'react';
import { 
  scanDevices, 
  autoConnectDevice, 
  DeviceScanResult,
  DeviceConnectionStatus,
  getDeviceModeName,
  getDeviceModeColor,
  isFlashMode,
  isMtkDevice,
  isQualcommDevice
} from '@/lib/device';

interface DeviceDetectorProps {
  onDeviceConnected?: (device: DeviceConnectionStatus) => void;
  autoScan?: boolean;
  scanInterval?: number;
}

export const DeviceDetector: React.FC<DeviceDetectorProps> = ({
  onDeviceConnected,
  autoScan = true,
  scanInterval = 3000
}) => {
  const [scanResult, setScanResult] = useState<DeviceScanResult | null>(null);
  const [connectedDevice, setConnectedDevice] = useState<DeviceConnectionStatus | null>(null);
  const [isScanning, setIsScanning] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleScan = useCallback(async () => {
    setIsScanning(true);
    setError(null);
    
    try {
      const result = await scanDevices();
      setScanResult(result);
      
      // Auto-connect if device found
      if (result.hasSupported && result.devices.length > 0) {
        const connected = await autoConnectDevice();
        setConnectedDevice(connected);
        onDeviceConnected?.(connected);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Scan failed');
    } finally {
      setIsScanning(false);
    }
  }, [onDeviceConnected]);

  // Auto-scan on mount
  useEffect(() => {
    handleScan();
    
    if (autoScan) {
      const interval = setInterval(handleScan, scanInterval);
      return () => clearInterval(interval);
    }
  }, [autoScan, handleScan, scanInterval]);

  return (
    <div className="device-detector">
      <div className="detector-header">
        <h3>🔌 Device Detection</h3>
        <button 
          onClick={handleScan} 
          disabled={isScanning}
          className="scan-button"
        >
          {isScanning ? 'Scanning...' : 'Scan Devices'}
        </button>
      </div>

      {error && (
        <div className="error-banner">
          ❌ {error}
        </div>
      )}

      {connectedDevice && (
        <DeviceCard device={connectedDevice} />
      )}

      {scanResult && !connectedDevice && (
        <div className="device-list">
          <h4>Found {scanResult.count} device(s)</h4>
          {scanResult.devices.map((status, idx) => (
            <DeviceCard key={idx} device={status} compact />
          ))}
        </div>
      )}

      {!scanResult && !isScanning && (
        <div className="empty-state">
          <p>No devices scanned yet</p>
          <button onClick={handleScan}>Start Scan</button>
        </div>
      )}
    </div>
  );
};

interface DeviceCardProps {
  device: DeviceConnectionStatus;
  compact?: boolean;
}

const DeviceCard: React.FC<DeviceCardProps> = ({ device, compact = false }) => {
  const { device: deviceInfo, protocol } = device;
  
  if (!deviceInfo) return null;

  const modeColor = getDeviceModeColor(deviceInfo.mode);
  const modeName = getDeviceModeName(deviceInfo.mode);
  const isFlash = isFlashMode(deviceInfo.mode);
  const isMtk = isMtkDevice(deviceInfo.mode);
  const isQcom = isQualcommDevice(deviceInfo.mode);

  if (compact) {
    return (
      <div className="device-card-compact" style={{ borderLeftColor: modeColor }}>
        <div className="device-icon" style={{ backgroundColor: modeColor }}>
          {isMtk ? '📱' : isQcom ? '⚡' : '🔧'}
        </div>
        <div className="device-info">
          <div className="device-mode">{modeName}</div>
          <div className="device-vid-pid">
            VID: {deviceInfo.vid.toString(16).toUpperCase().padStart(4, '0')} | 
            PID: {deviceInfo.pid.toString(16).toUpperCase().padStart(4, '0')}
          </div>
        </div>
        <div className="device-status">
          {device.connected ? '✅' : '❌'}
        </div>
      </div>
    );
  }

  return (
    <div className="device-card" style={{ borderLeft: `4px solid ${modeColor}` }}>
      <div className="card-header">
        <div className="device-title">
          <span className="device-icon-large" style={{ backgroundColor: modeColor }}>
            {isMtk ? '📱' : isQcom ? '⚡' : '🔧'}
          </span>
          <div>
            <h4>{modeName}</h4>
            <span className="protocol-badge">{protocol}</span>
          </div>
        </div>
        <div className="connection-status">
          {device.connected ? (
            <span className="status-connected">✅ Connected</span>
          ) : (
            <span className="status-disconnected">❌ Disconnected</span>
          )}
        </div>
      </div>

      <div className="card-body">
        <div className="info-grid">
          <InfoItem label="VID" value={`0x${deviceInfo.vid.toString(16).toUpperCase().padStart(4, '0')}`} />
          <InfoItem label="PID" value={`0x${deviceInfo.pid.toString(16).toUpperCase().padStart(4, '0')}`} />
          <InfoItem label="Bus" value={deviceInfo.bus.toString()} />
          <InfoItem label="Address" value={deviceInfo.address.toString()} />
          {deviceInfo.serial && <InfoItem label="Serial" value={deviceInfo.serial} />}
          {deviceInfo.chipset && <InfoItem label="Chipset" value={deviceInfo.chipset} />}
        </div>

        {isFlash && (
          <div className="flash-mode-banner">
            ⚠️ Device is in flash/download mode
          </div>
        )}

        {deviceInfo.manufacturer && (
          <div className="device-manufacturer">
            Manufacturer: {deviceInfo.manufacturer}
          </div>
        )}

        {deviceInfo.product && (
          <div className="device-product">
            Product: {deviceInfo.product}
          </div>
        )}
      </div>

      <div className="card-footer">
        <div className="detected-time">
          Detected: {new Date(deviceInfo.detectedAt).toLocaleTimeString()}
        </div>
        {device.connected && (
          <div className="available-actions">
            {isMtk && <ActionBadge text="MTK Operations" />}
            {isQcom && <ActionBadge text="EDL Operations" />}
            {deviceInfo.mode === 'Fastboot' && <ActionBadge text="Fastboot Operations" />}
          </div>
        )}
      </div>
    </div>
  );
};

const InfoItem: React.FC<{ label: string; value: string }> = ({ label, value }) => (
  <div className="info-item">
    <span className="info-label">{label}:</span>
    <span className="info-value">{value}</span>
  </div>
);

const ActionBadge: React.FC<{ text: string }> = ({ text }) => (
  <span className="action-badge">{text}</span>
);

export default DeviceDetector;
