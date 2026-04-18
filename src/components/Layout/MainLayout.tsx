import { useState } from 'react';
import { Sidebar } from './Sidebar';
import { DeviceStatusBar } from '../device/DeviceStatusBar';
import { ToolCard } from '../tools/ToolCard';
import { ExecutionConsole } from './ExecutionConsole';
import { FEATURE_MAP } from '../../lib/featureMap';
import './MainLayout.css';

export function MainLayout() {
  const [activePlatform, setActivePlatform] = useState('android');
  const [activeProtocol, setActiveProtocol] = useState('all');
  const [searchQuery, setSearchQuery] = useState('');
  const [consoleLines, setConsoleLines] = useState<string[]>([]);

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
                  onRun={handleRunLogs}
                />
              ))}
            </div>
          </div>

          {/* Execution Console */}
          <ExecutionConsole lines={consoleLines} />
        </main>
      </div>
    </div>
  );
}
