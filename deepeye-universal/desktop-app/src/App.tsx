import { useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import "./App.css";

function App() {
  const [devicePort, setDevicePort] = useState<string>("Waiting for device...");
  const [isScanning, setIsScanning] = useState(false);

  async function scanDevices() {
    setIsScanning(true);
    try {
      // Connects to Rust backend
      const res = await invoke<string[]>("scan_usb_devices");
      if (res.length > 0) {
        setDevicePort(res[0]);
      } else {
        setDevicePort("No devices found.");
      }
    } catch (e) {
      setDevicePort(`Error: ${e}`);
    } finally {
      setIsScanning(false);
    }
  }

  return (
    <div className="flex h-screen bg-deeper text-white overflow-hidden p-4 gap-4">
      {/* Sidebar - Devices Panel */}
      <aside className="w-64 glass-panel flex flex-col p-4">
        <div className="flex items-center gap-3 mb-6">
          <div className="w-8 h-8 rounded-full bg-deepaccent flex items-center justify-center font-bold">D</div>
          <h1 className="font-semibold text-lg tracking-wide">DeepEye</h1>
        </div>

        <div className="flex-1">
          <h2 className="text-xs uppercase text-gray-400 font-semibold mb-3 tracking-wider">Device Status</h2>

          <div className="glass-button p-3 flex flex-col gap-1 mb-4">
            <span className="text-sm font-medium text-gray-200">Port</span>
            <span className="text-xs text-deepaccent truncate">{devicePort}</span>
          </div>

          <button
            onClick={scanDevices}
            disabled={isScanning}
            className="w-full primary-button disabled:opacity-50"
          >
            {isScanning ? "Scanning..." : "Scan Devices"}
          </button>
        </div>

        <div className="mt-auto pt-4 border-t border-white/10">
          <div className="flex items-center justify-between text-xs text-gray-400">
            <span>Tier: </span><span className="text-deepaccent font-medium">Technician</span>
          </div>
        </div>
      </aside>

      {/* Main Workbench */}
      <main className="flex-1 flex flex-col gap-4">
        {/* Top Navbar / Brand Tabs */}
        <header className="h-14 glass-panel flex items-center px-4 gap-2">
          {['MTK Universal', 'QCOM Universal', 'Samsung', 'Unisoc'].map(tab => (
            <button key={tab} className="px-4 py-1.5 text-sm font-medium rounded-md hover:bg-white/10 transition flex-shrink-0">
              {tab}
            </button>
          ))}
        </header>

        {/* Content Area */}
        <div className="flex-1 glass-panel p-6 flex flex-col">
          <h2 className="text-xl font-semibold mb-6">Operations (MTK)</h2>

          <div className="grid grid-cols-3 gap-4">
            {['FRP Assist', 'Read Info', 'Factory Reset', 'Unlock Bootloader', 'Backup NV'].map(op => (
              <button key={op} className="glass-button py-4 px-4 flex items-center justify-center font-medium text-sm text-gray-200">
                {op}
              </button>
            ))}
          </div>

          <div className="mt-auto glass-panel border border-white/5 bg-black/40 h-48 p-4 font-mono text-xs text-gray-300 overflow-y-auto">
            <div className="text-green-400">[10:45:01] DeepEye Universal Engine v1.0.0 Started</div>
            <div>[10:45:01] Waiting for command...</div>
          </div>
        </div>
      </main>
    </div>
  );
}

export default App;
