import React, { useState } from 'react';
import { Sidebar } from './components/Sidebar';
import { DfuRestore } from './modules/DfuRestore';
import { ActivationLock } from './modules/ActivationLock';
import { AppleIdRemoval } from './modules/AppleIdRemoval';
import { ScreenTimeCrack } from './modules/ScreenTimeCrack';
import { MdmAnalysis } from './modules/MdmAnalysis';
import { BypassEngine } from './modules/BypassEngine';
import { BypassAdvanced } from './modules/BypassAdvanced';
import { IdentityForensics } from './modules/IdentityForensics';
import { TicketEngine } from './modules/TicketEngine';
import { ExploitOrchestrator } from './modules/ExploitOrchestrator';
import { RamdiskMaster } from './modules/RamdiskMaster';
import { DeepVaultExport } from './modules/DeepVaultExport';
import { DeepExtraction } from './modules/DeepExtraction';
import IOSBackupLayer from './modules/IOSBackup';
import AdbTerminal from './modules/AdbTerminal';

const App: React.FC = () => {
  const [activeTab, setActiveTab] = useState('dfu');

  const renderContent = () => {
    switch (activeTab) {
      case 'ios-backup': return <IOSBackupLayer />;
      case 'adb': return <AdbTerminal />;
      case 'bypass-advanced': return <BypassAdvanced />;
      case 'identity': return <IdentityForensics />;
      case 'tickets': return <TicketEngine />;
      case 'orchestrator': return <ExploitOrchestrator />;
      case 'dfu': return <DfuRestore />;
      case 'activation': return <ActivationLock />;
      case 'apple-id': return <AppleIdRemoval />;
      case 'screentime': return <ScreenTimeCrack />;
      case 'mdm': return <MdmAnalysis />;
      case 'bypass': return <BypassEngine />;
      case 'ramdisk': return <RamdiskMaster />;
      case 'deep-extraction': return <DeepExtraction />;
      case 'vault': return <DeepVaultExport />;
      default: return <DfuRestore />;
    }
  };

  return (
    <div className="flex h-screen bg-[#020205] text-white overflow-hidden font-sans selection:bg-blue-500/30">
      <Sidebar currentTab={activeTab} setTab={setActiveTab} />
      
      <main className="flex-1 flex flex-col relative overflow-hidden">
        {/* Background Ambient Glow */}
        <div className="absolute top-[-10%] right-[-10%] w-[50%] h-[50%] bg-blue-900/10 blur-[120px] rounded-full pointer-events-none" />
        <div className="absolute bottom-[-10%] left-[-10%] w-[40%] h-[40%] bg-indigo-900/10 blur-[100px] rounded-full pointer-events-none" />

        <div className="flex-1 overflow-y-auto p-12 relative z-10 custom-scrollbar">
          <div className="max-w-5xl mx-auto space-y-12">
            {/* Nav Context Breadcrumb */}
            <div className="flex items-center gap-2 text-[10px] uppercase tracking-[0.3em] text-gray-500 font-mono">
                <span className="hover:text-blue-400 cursor-pointer transition-colors" onClick={() => setActiveTab('dfu')}>CORE</span>
                <span>/</span>
                <span className="text-gray-300">{activeTab}</span>
            </div>

            <div className="animate-in fade-in slide-in-from-bottom-4 duration-700">
                {renderContent()}
            </div>
          </div>
        </div>

        <footer className="h-10 px-8 flex items-center bg-black/60 border-t border-white/5 text-[9px] text-gray-500 font-mono uppercase tracking-widest z-20">
            <div className="flex items-center gap-4">
                <div className="flex items-center gap-2">
                    <div className="w-1.5 h-1.5 rounded-full bg-green-500 animate-pulse" />
                    SYSTEM ENGINE ONLINE
                </div>
                <div className="w-px h-3 bg-white/10" />
                <div>BUILD 2026.25_PRO</div>
            </div>
            <div className="ml-auto flex gap-6">
                <span>TAURI HOST: DARWIN_ARM64</span>
                <span className="text-blue-400/60">© DEEPEYE SECURITY RESEARCH</span>
            </div>
        </footer>
      </main>

      <style>{`
        .custom-scrollbar::-webkit-scrollbar {
          width: 4px;
        }
        .custom-scrollbar::-webkit-scrollbar-track {
          background: transparent;
        }
        .custom-scrollbar::-webkit-scrollbar-thumb {
          background: rgba(255, 255, 255, 0.05);
          border-radius: 10px;
        }
        .custom-scrollbar::-webkit-scrollbar-thumb:hover {
          background: rgba(255, 255, 255, 0.1);
        }
      `}</style>
    </div>
  );
};

export default App;
