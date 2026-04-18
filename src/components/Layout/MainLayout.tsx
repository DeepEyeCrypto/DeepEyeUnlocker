import { useState } from 'react';
import { Sidebar } from './Sidebar';
import { DeviceStatusBar } from '../device/DeviceStatusBar';
import { ToolCard } from '../tools/ToolCard';
import { ExecutionConsole } from './ExecutionConsole';
import { FEATURE_MAP } from '../../lib/featureMap';
import { HistoryScreen } from '../history/HistoryScreen';
import { WifiAdbScreen } from '../adb/WifiAdbScreen';
import { DeviceInfoDashboard } from '../device/DeviceInfoDashboard';
import { useEffect } from 'react';
import { listen } from '@tauri-apps/api/event';
import { invoke } from '@tauri-apps/api/core';
import './MainLayout.css';

export function MainLayout() {
  const [activePlatform, setActivePlatform] = useState('android');
  const [activeProtocol, setActiveProtocol] = useState('all');
  const [searchQuery, setSearchQuery] = useState('');
  const [consoleLines, setConsoleLines] = useState<string[]>([]);
  const [selectedTool, setSelectedTool] = useState<any>(null);
  const [updateInfo, setUpdateInfo] = useState<any>(null);
  const [deviceInfo, setDeviceInfo] = useState<any>(null);

  useEffect(() => {
    // Feature 2: Auto-detect listener
    const unlisten = listen('device-profile-detected', (event) => {
      const platform = (event.payload as string).toLowerCase();
      console.log(`[Auto-Detect] Switching to ${platform}`);
      
      // Auto-switch platform
      if (platform === 'mtk') setActivePlatform('android');
      else setActivePlatform(platform);
      
      setActiveProtocol('all');
      setConsoleLines(prev => [...prev, `[System] Auto-switched to ${platform.toUpperCase()} platform`]);
    });

    return () => {
      unlisten.then(f => f());
    };
  }, []);

  useEffect(() => {
    // Feature 5: Update Checker
    const checkUpdates = async () => {
      try {
        const info = await invoke('check_for_updates');
        setUpdateInfo(info);
      } catch (e) {
        console.error("Update check failed", e);
      }
    };
    checkUpdates();
  }, []);

  const platformData = (FEATURE_MAP as any)[activePlatform];
  
  const filteredTools = platformData.tools.filter((tool: any) => {
    const matchSearch = tool.name.toLowerCase().includes(searchQuery.toLowerCase())
      || tool.description.toLowerCase().includes(searchQuery.toLowerCase());
    const matchProtocol = activeProtocol === 'all' || tool.protocol === activeProtocol;
    return matchSearch && matchProtocol;
  });

  const protocols = ['all', ...new Set(platformData.tools.map((t: any) => t.protocol)) as Set<string>];

  const handleRunLogs = (logs: string[]) => {
    setConsoleLines(prev => [...prev, ...logs]);
  };

  return (
    <div className="app-layout">
      <DeviceStatusBar />
      
      <div className="app-body">
        <Sidebar active={activePlatform} onSelect={(id) => {
          setActivePlatform(id);
          setActiveProtocol('all');
        }} />
        
        <main className="main-content">
          {activePlatform === 'history' ? (
            <HistoryScreen />
          ) : activePlatform === 'settings' ? (
            <div className="p-8">
              <h1 className="text-2xl font-bold mb-4">Settings</h1>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="p-6 glass-card rounded-3xl border border-white/10">
                  <h3 className="text-lg font-bold mb-4 flex items-center gap-2">
                    <span className="p-2 bg-indigo-500/20 rounded-lg">✨</span> System Update
                  </h3>
                  {updateInfo ? (
                    <div>
                        <div className="flex items-center gap-2 mb-2">
                            <span className="text-sm text-gray-400">Current: {updateInfo.current_version}</span>
                            <span className="text-sm text-gray-400">Latest: {updateInfo.latest_version}</span>
                        </div>
                        {updateInfo.update_available ? (
                            <div className="p-4 bg-green-500/10 border border-green-500/20 rounded-2xl">
                                <p className="text-green-400 text-sm font-bold">🚀 New version available!</p>
                                <button 
                                    onClick={() => window.open(updateInfo.download_url)}
                                    className="mt-3 px-4 py-2 bg-green-600 hover:bg-green-500 text-white text-xs font-bold rounded-xl transition-all"
                                >
                                    Download Now
                                </button>
                            </div>
                        ) : (
                            <p className="text-xs text-gray-500 italic">DeepEye is up to date.</p>
                        )}
                    </div>
                  ) : (
                    <p className="text-sm text-gray-400 animate-pulse">Checking for updates...</p>
                  )}
                </div>
                
                <div className="p-6 glass-card rounded-3xl border border-white/10">
                  <h3 className="text-lg font-bold mb-4">Data Management</h3>
                  <p className="text-sm text-gray-400 mb-4">v1.1.0 Persistence Layer Active ✅</p>
                  <p className="text-xs text-gray-500 italic">Path: ~/.deepeye/settings.json</p>
                </div>
              </div>
            </div>
          ) : (
            <>
              {selectedTool && selectedTool.id === 'wireless_adb' ? (
                 <div className="p-2">
                   <button 
                     onClick={() => setSelectedTool(null)}
                     className="mb-4 px-4 py-2 bg-white/5 border border-white/10 rounded-xl text-xs font-bold text-gray-400 hover:text-white"
                   >
                     ← Back to Tools
                   </button>
                   <WifiAdbScreen />
                 </div>
              ) : selectedTool && selectedTool.id.includes('info') ? (
                  <div className="p-2">
                     <button 
                       onClick={() => setSelectedTool(null)}
                       className="mb-4 px-4 py-2 bg-white/5 border border-white/10 rounded-xl text-xs font-bold text-gray-400 hover:text-white"
                     >
                       ← Back to Tools
                     </button>
                     {deviceInfo ? (
                        <DeviceInfoDashboard info={deviceInfo} />
                     ) : (
                        <div className="p-12 text-center text-gray-500 animate-pulse">Fetching detailed device metrics...</div>
                     )}
                   </div>
              ) : (
                <>
                  {/* Platform Header */}
              <div className="platform-header"
                style={{ borderColor: platformData.color }}>
                <div className="header-info">
                  <h1 className="platform-title">{platformData.label}</h1>
                  <span className="tool-count">
                    {platformData.tools.length} TOOLS AVAILABLE
                  </span>
                </div>
                
                <div className="search-wrapper">
                  <input
                    className="search-input"
                    placeholder="Search tools..."
                    value={searchQuery}
                    onChange={e => setSearchQuery(e.target.value)}
                  />
                  <span className="search-icon">🔍</span>
                </div>
              </div>

              {/* Protocol Filters */}
              <div className="filter-bar">
                <div className="protocol-tabs">
                  {protocols.map(p => (
                    <button
                      key={p}
                      className={`proto-tab ${activeProtocol === p ? 'active' : ''}`}
                      onClick={() => setActiveProtocol(p)}
                    >
                      {p.toUpperCase()}
                    </button>
                  ))}
                </div>
              </div>

              {/* Tool Grid */}
              <div className="tools-grid-container">
                <div className="tools-grid">
                  {filteredTools.map((tool: any) => (
                    <ToolCard
                      key={tool.id}
                      tool={tool}
                      platform={activePlatform}
                      onRun={async (logs) => {
                        handleRunLogs(logs);
                        
                        // v1.2.0 Branch: If info tool, fetch and show dashboard
                        if (tool.id.includes('info')) {
                             setSelectedTool(tool);
                             try {
                                const serialResult = await invoke<string>("run_binary", { bin: "adb", args: ["get-serialno"] });
                                const serial: string = serialResult;
                                const info = await invoke("adb_get_full_info", { serial: serial.trim() });
                                setDeviceInfo(info);
                             } catch (e) {
                                handleRunLogs([`❌ Dashboard Error: ${e}`]);
                             }
                             return;
                        }

                        // v1.2.0 Branch: If Wireless ADB, switch view
                        if (tool.id === 'wireless_adb') {
                             setSelectedTool(tool);
                             return;
                        }

                        // Record to History
                        invoke("add_history_entry", {
                          deviceName: "Connected Device",
                          chipset: activePlatform.toUpperCase(),
                          toolName: tool.name,
                          result: logs.join('\n')
                        }).catch(console.error);
                      }}
                    />
                  ))}
                </div>
              </div>
            </>
          )}

          {/* Execution Console */}
          <ExecutionConsole lines={consoleLines} />
          </>
          )}
        </main>
      </div>
    </div>
  );
}
