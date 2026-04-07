import { useEffect, useRef, useState } from 'react';
import { open } from '@tauri-apps/plugin-dialog';
import { useEdl } from '../hooks/useEdl';
import { DeviceSelector } from '../components/DeviceSelector';
import { LiquidMetalButton } from '../components/ui/liquid-metal-button';
import type { DeviceEntry } from '../lib/devices';
import '../styles/edl.css';

export default function EdlPage() {
  const {
    deviceInfo, saharaInfo, storageInfo,
    edlStatus, error,
    detect, saharaHandshake, uploadProgrammer,
    configure, erasePartition, readPartition,
    writePartition, getStorageInfo, reboot
  } = useEdl();

  const [selectedDevice, setSelectedDevice] = useState<DeviceEntry | null>(null);
  const [log, setLog] = useState<string[]>(['> Waiting for Qualcomm EDL device...']);
  const [partitionInput, setPartitionInput] = useState('');
  const [sectorsInput, setSectorsInput] = useState('128');
  const [programmerName, setProgrammerName] = useState('');
  const [flashFile, setFlashFile] = useState('');
  const logRef = useRef<HTMLDivElement>(null);

  const addLog = (msg: string) => {
    setLog(prev => [...prev.slice(-49), `[${new Date().toLocaleTimeString()}] ${msg}`]);
  };

  useEffect(() => {
    if (edlStatus !== 'idle') {
      addLog(`Status: ${edlStatus}`);
    }
  }, [edlStatus]);

  useEffect(() => {
    if (error) {
      addLog(`[ERROR] ${error}`);
    }
  }, [error]);

  useEffect(() => {
    logRef.current?.scrollTo(0, logRef.current.scrollHeight);
  }, [log]);

  useEffect(() => {
    if (edlStatus === 'programmer_ready') {
      addLog('> Programmer uploaded successfully. Configuring Firehose (1MB payload)...');
      configure(1048576, 4096).catch(console.error);
    }
  }, [edlStatus, configure]);

  const handlePickProgrammer = async () => {
    try {
      const path = await open({
        filters: [{
          name: 'Firehose Programmer',
          extensions: ['elf', 'mbn', 'bin']
        }]
      });
      if (path && typeof path === 'string') {
        const filename = path.split(/[\\/]/).pop() || path;
        setProgrammerName(filename);
        addLog(`> Selected programmer: ${filename}`);
        await uploadProgrammer(path);
      }
    } catch (e) {
      addLog(`[ERROR] ${String(e)}`);
    }
  };

  const handlePickFlashFile = async () => {
    try {
      const path = await open({
        filters: [{
          name: 'Flash Images',
          extensions: ['img', 'bin', 'raw']
        }]
      });
      if (path && typeof path === 'string') {
        const filename = path.split(/[\\/]/).pop() || path;
        setFlashFile(path);
        addLog(`> Selected flash file: ${filename}`);
      }
    } catch (e) {
      addLog(`[ERROR] ${String(e)}`);
    }
  };

  const isStep1Done = deviceInfo !== null;
  const isStep2Done = saharaInfo !== null;
  const isStep3Done = edlStatus === 'programmer_ready' || storageInfo !== null;

  return (
    <div className="page page-enter edl-page">
      <DeviceSelector 
        selectedDevice={selectedDevice} 
        onSelect={setSelectedDevice}
        filterSupport={['edl_only', 'partial']}
      />

      <div className="edl-header">
        <div className="edl-header-info">
          <h2 className="edl-title">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2" />
            </svg>
            QUALCOMM EDL
          </h2>
          <p className="edl-subtitle">
            {deviceInfo 
              ? `VID: 0x${deviceInfo.vid.toString(16).padStart(4, '0')} PID: 0x${deviceInfo.pid.toString(16).padStart(4, '0')}`
              : 'Connect device to read IDs'}
            {saharaInfo && ` · Sahara v${saharaInfo.version}`}
            {storageInfo && ` · ${storageInfo.storage_type}`}
          </p>
        </div>

        <div className={`status-badge status-${edlStatus}`}>
          {edlStatus.replace('_', ' ')}
        </div>
      </div>

      <div className="pipeline">
        <div className={`pipeline-step ${isStep1Done ? 'done' : (edlStatus === 'detecting' ? 'active' : '')}`}>
          <span className="step-number">Step 1</span>
          <span className="step-action">Detect Device</span>
          <LiquidMetalButton
            label="Detect Device"
            onClick={detect}
            disabled={edlStatus === 'detecting'}
          />
        </div>

        <div className={`pipeline-step ${isStep2Done ? 'done' : (edlStatus === 'sahara_handshake' ? 'active' : '')}`}>
          <span className="step-number">Step 2</span>
          <span className="step-action">Sahara Protocol</span>
          <button 
            className="edl-detect-btn"
            disabled={!isStep2Done}
            onClick={() => void saharaHandshake()}
          >
            Handshake
          </button>
        </div>

        <div className={`pipeline-step ${isStep3Done ? 'done' : (edlStatus === 'uploading_programmer' ? 'active' : '')}`}>
          <span className="step-number">Step 3</span>
          <span className="step-action">Firehose Deploy</span>
          <button 
            className="btn btn-primary btn-sm" 
            disabled={!isStep2Done}
            style={{ marginTop: 'auto' }}
            onClick={() => void handlePickProgrammer()}
          >
            {programmerName ? programmerName : 'Select .elf / .mbn'}
          </button>
        </div>
      </div>

      <div className="edl-grid">
        <div className="card glass">
          <h3 style={{ margin: '0 0 1rem 0', fontSize: '1rem' }}>DEVICE INFO</h3>
          <table className="info-table">
            <tbody>
              <tr>
                <td>VID/PID</td>
                <td>
                  {deviceInfo 
                    ? `0x${deviceInfo.vid.toString(16).padStart(4, '0')}:0x${deviceInfo.pid.toString(16).padStart(4, '0')}`
                    : 'Wait...'}
                </td>
              </tr>
              <tr>
                <td>Serial</td>
                <td>{deviceInfo?.serial || 'N/A'}</td>
              </tr>
              <tr>
                <td>Sahara Version</td>
                <td>{saharaInfo ? `v${saharaInfo.version}` : 'N/A'}</td>
              </tr>
              <tr>
                <td>Storage Type</td>
                <td>{storageInfo?.storage_type || 'N/A'}</td>
              </tr>
              <tr>
                <td>Total Blocks</td>
                <td>{storageInfo ? storageInfo.total_blocks : 'N/A'}</td>
              </tr>
              <tr>
                <td>Block Size</td>
                <td>{storageInfo ? `${storageInfo.block_size} B` : 'N/A'}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div className="card glass">
          <h3 style={{ margin: '0 0 1rem 0', fontSize: '1rem' }}>QUICK OPERATIONS</h3>
          <div className="ops-grid">
            <button className="btn btn-ghost" disabled={!isStep3Done} onClick={() => void erasePartition('frp')}>
              Erase FRP
            </button>
            <button className="btn btn-ghost" disabled={!isStep3Done} onClick={() => void erasePartition('userdata')}>
              Erase Userdata
            </button>
            <button className="btn btn-ghost" disabled={!isStep3Done} onClick={() => void getStorageInfo()}>
              Get Storage Info
            </button>
            <button className="btn btn-ghost" disabled={!isStep3Done} onClick={() => void reboot('reset')}>
              Reboot Normal
            </button>
            <button className="btn btn-ghost" disabled={!isStep3Done} onClick={() => void reboot('edl')}>
              Reboot EDL
            </button>
          </div>
        </div>
      </div>

      <div className="partition-bar">
        <div style={{ flex: 1 }}>
          <label style={{ display: 'block', fontSize: '0.875rem', color: 'var(--text-2)', marginBottom: '0.5rem' }}>
            Partition Name
          </label>
          <input 
            type="text" 
            className="input glass-input" 
            placeholder="e.g. boot"
            value={partitionInput}
            onChange={(e) => setPartitionInput(e.target.value)}
            style={{ width: '100%' }}
          />
        </div>
        <div style={{ width: '150px' }}>
          <label style={{ display: 'block', fontSize: '0.875rem', color: 'var(--text-2)', marginBottom: '0.5rem' }}>
            Sectors
          </label>
          <input 
            type="number" 
            className="input glass-input" 
            value={sectorsInput}
            onChange={(e) => setSectorsInput(e.target.value)}
            style={{ width: '100%' }}
          />
        </div>
        <button 
          className="btn btn-primary" 
          disabled={!isStep3Done || !partitionInput}
          onClick={() => void readPartition(partitionInput, parseInt(sectorsInput, 10) || 128, `/tmp/${partitionInput}.img`)}
        >
          Read →
        </button>
        <button 
          className="btn btn-primary" 
          disabled={!isStep3Done || !partitionInput}
          onClick={() => void writePartition(partitionInput, `/tmp/${partitionInput}.img`)}
        >
          Write ←
        </button>
        <button 
          className="btn btn-danger" 
          disabled={!isStep3Done || !partitionInput}
          onClick={() => void erasePartition(partitionInput)}
        >
          Erase ✕
        </button>
        <div style={{ width: '200px' }}>
          <label style={{ display: 'block', fontSize: '0.875rem', color: 'var(--text-2)', marginBottom: '0.5rem' }}>
            Flash File
          </label>
          <input 
            type="text" 
            className="input glass-input" 
            placeholder="Select file..."
            value={flashFile.split(/[\\/]/).pop() || ''}
            readOnly
            style={{ width: '100%' }}
          />
        </div>
        <button 
          className="btn btn-secondary" 
          disabled={!isStep3Done}
          onClick={() => void handlePickFlashFile()}
        >
          Select File
        </button>
        <button 
          className="btn btn-success" 
          disabled={!isStep3Done || !flashFile}
          onClick={() => void writePartition('all', flashFile)}
        >
          Flash All
        </button>
      </div>

      <div className="terminal-box" ref={logRef}>
        {log.map((line, idx) => (
          <div key={idx} className="terminal-line">{line}</div>
        ))}
      </div>
    </div>
  );
}
