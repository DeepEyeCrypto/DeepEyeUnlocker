/**
 * FastbootOperations Component
 * 
 * Complete Fastboot protocol operations UI
 * Flash, erase, reboot, and bootloader management
 */

import React, { useState } from 'react';
import { open } from '@tauri-apps/plugin-dialog';
import {
  fastbootDetect,
  fastbootGetInfo,
  fastbootFlashPartition,
  fastbootErasePartition,
  fastbootReboot,
  fastbootRebootBootloader,
  fastbootRebootRecovery,
  fastbootUnlockBootloader,
  fastbootLockBootloader,
  FastbootDeviceInfo
} from '@/lib/device';

interface FastbootOperationsProps {
  onProgress?: (message: string) => void;
}

export const FastbootOperations: React.FC<FastbootOperationsProps> = ({ onProgress }) => {
  const [deviceInfo, setDeviceInfo] = useState<FastbootDeviceInfo | null>(null);
  const [isConnected, setIsConnected] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [selectedPartition, setSelectedPartition] = useState('');
  const [selectedFile, setSelectedFile] = useState<string | null>(null);

  const log = (message: string) => {
    console.log('[Fastboot]', message);
    onProgress?.(message);
  };

  const handleSelectFlashFile = async () => {
    setError(null);

    try {
      const selection = await open({
        directory: false,
        multiple: false,
        filters: [
          {
            name: 'Firmware Images',
            extensions: ['img', 'bin', 'zip']
          }
        ]
      });

      if (typeof selection === 'string') {
        setSelectedFile(selection);
        log(`Selected flash file: ${selection}`);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'File selection failed');
      log('❌ File selection error: ' + err);
    }
  };

  const handleDetect = async () => {
    setIsLoading(true);
    setError(null);
    
    try {
      const detected = await fastbootDetect();
      setIsConnected(detected);
      
      if (detected) {
        log('Fastboot device detected');
        const info = await fastbootGetInfo();
        setDeviceInfo(info);
        log(`Device: ${info.product} (${info.serial})`);
      } else {
        log('No fastboot device found');
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Detection failed');
      log('Error: ' + err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleFlash = async () => {
    if (!selectedPartition || !selectedFile) {
      setError('Please select partition and file');
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      log(`Flashing '${selectedPartition}' from ${selectedFile}...`);
      await fastbootFlashPartition(selectedPartition, selectedFile);
      log(`✅ Successfully flashed '${selectedPartition}'`);
      setSelectedPartition('');
      setSelectedFile(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Flash failed');
      log('❌ Flash error: ' + err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleErase = async (partition: string) => {
    setIsLoading(true);
    setError(null);

    try {
      log(`Erasing '${partition}'...`);
      await fastbootErasePartition(partition);
      log(`✅ Successfully erased '${partition}'`);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erase failed');
      log('❌ Erase error: ' + err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleReboot = async (mode: 'normal' | 'bootloader' | 'recovery') => {
    setIsLoading(true);
    setError(null);

    try {
      log(`Rebooting to ${mode}...`);
      
      switch (mode) {
        case 'normal':
          await fastbootReboot();
          break;
        case 'bootloader':
          await fastbootRebootBootloader();
          break;
        case 'recovery':
          await fastbootRebootRecovery();
          break;
      }
      
      log(`✅ Rebooted to ${mode}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Reboot failed');
      log('❌ Reboot error: ' + err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleUnlockBootloader = async () => {
    if (!confirm('⚠️ This will wipe all device data. Continue?')) {
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      log('Unlocking bootloader...');
      await fastbootUnlockBootloader();
      log('✅ Bootloader unlocked');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unlock failed');
      log('❌ Unlock error: ' + err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleLockBootloader = async () => {
    if (!confirm('⚠️ Lock bootloader? This may brick your device if improperly done.')) {
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      log('Locking bootloader...');
      await fastbootLockBootloader();
      log('✅ Bootloader locked');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Lock failed');
      log('❌ Lock error: ' + err);
    } finally {
      setIsLoading(false);
    }
  };

  const partitions = [
    'boot', 'recovery', 'system', 'vendor', 'userdata', 
    'cache', 'dtbo', 'vbmeta', 'logo', 'misc'
  ];

  return (
    <div className="fastboot-operations">
      <div className="panel-header">
        <h3>⚡ Fastboot Operations</h3>
        <button 
          onClick={handleDetect} 
          disabled={isLoading}
          className="detect-button"
        >
          {isLoading ? 'Working...' : 'Detect Device'}
        </button>
      </div>

      {error && (
        <div className="error-banner">
          ❌ {error}
        </div>
      )}

      {isConnected && deviceInfo && (
        <div className="device-info-panel">
          <h4>Device Information</h4>
          <div className="info-grid">
            <InfoRow label="Serial" value={deviceInfo.serial} />
            <InfoRow label="Product" value={deviceInfo.product} />
            <InfoRow label="Variant" value={deviceInfo.variant} />
            <InfoRow label="Bootloader" value={deviceInfo.bootloaderVersion} />
            <InfoRow label="Baseband" value={deviceInfo.basebandVersion} />
            <InfoRow 
              label="Secure Boot" 
              value={deviceInfo.secureBoot ? 'Yes' : 'No'} 
            />
            <InfoRow 
              label="Bootloader Status" 
              value={deviceInfo.unlocked ? '🔓 Unlocked' : '🔒 Locked'}
              highlight={deviceInfo.unlocked}
            />
          </div>
        </div>
      )}

      <div className="operations-grid">
        {/* Flash Partition */}
        <div className="operation-card">
          <h4>📦 Flash Partition</h4>
          <div className="form-group">
            <label>Partition:</label>
            <select 
              value={selectedPartition} 
              onChange={(e) => setSelectedPartition(e.target.value)}
              disabled={isLoading}
            >
              <option value="">Select partition...</option>
              {partitions.map(p => (
                <option key={p} value={p}>{p}</option>
              ))}
            </select>
          </div>
          <div className="form-group">
            <label>Image File:</label>
            <div className="file-picker-row">
              <button
                type="button"
                onClick={handleSelectFlashFile}
                disabled={isLoading}
                className="detect-button"
              >
                {selectedFile ? 'Change File' : 'Select File'}
              </button>
              <span className="selected-file-path" title={selectedFile ?? 'No file selected'}>
                {selectedFile ?? 'No file selected'}
              </span>
            </div>
          </div>
          <button 
            onClick={handleFlash}
            disabled={isLoading || !selectedPartition || !selectedFile}
            className="flash-button"
          >
            {isLoading ? 'Flashing...' : 'Flash Partition'}
          </button>
        </div>

        {/* Erase Partition */}
        <div className="operation-card">
          <h4>🗑️ Erase Partition</h4>
          <div className="partition-buttons">
            {partitions.map(partition => (
              <button
                key={partition}
                onClick={() => handleErase(partition)}
                disabled={isLoading}
                className="erase-button"
              >
                Erase {partition}
              </button>
            ))}
          </div>
        </div>

        {/* Reboot Options */}
        <div className="operation-card">
          <h4>🔄 Reboot Options</h4>
          <div className="reboot-buttons">
            <button 
              onClick={() => handleReboot('normal')}
              disabled={isLoading}
              className="reboot-button"
            >
              Reboot System
            </button>
            <button 
              onClick={() => handleReboot('bootloader')}
              disabled={isLoading}
              className="reboot-button secondary"
            >
              Reboot Bootloader
            </button>
            <button 
              onClick={() => handleReboot('recovery')}
              disabled={isLoading}
              className="reboot-button secondary"
            >
              Reboot Recovery
            </button>
          </div>
        </div>

        {/* Bootloader Management */}
        <div className="operation-card warning">
          <h4>🔐 Bootloader Management</h4>
          <div className="bootloader-actions">
            <button 
              onClick={handleUnlockBootloader}
              disabled={isLoading || deviceInfo?.unlocked}
              className="unlock-button"
            >
              🔓 Unlock Bootloader
            </button>
            <button 
              onClick={handleLockBootloader}
              disabled={isLoading || !deviceInfo?.unlocked}
              className="lock-button"
            >
              🔒 Lock Bootloader
            </button>
          </div>
          <p className="warning-text">
            ⚠️ Unlocking will wipe all device data
          </p>
        </div>
      </div>
    </div>
  );
};

interface InfoRowProps {
  label: string;
  value: string;
  highlight?: boolean;
}

const InfoRow: React.FC<InfoRowProps> = ({ label, value, highlight = false }) => (
  <div className={`info-row ${highlight ? 'highlight' : ''}`}>
    <span className="info-label">{label}:</span>
    <span className="info-value">{value}</span>
  </div>
);

export default FastbootOperations;
