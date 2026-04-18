import { useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import { Wifi, Link, Unlink, Key, Monitor, Terminal } from "lucide-react";

export const WifiAdbScreen = () => {
  const [ip, setIp] = useState("192.168.1.");
  const [port, setPort] = useState("5555");
  const [pairCode, setPairCode] = useState("");
  const [logs, setLogs] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);

  const addLog = (msg: string) => setLogs(prev => [`[${new Date().toLocaleTimeString()}] ${msg}`, ...prev]);

  const handlePair = async () => {
    setLoading(true);
    addLog(`Attempting to pair with ${ip}:${port}...`);
    try {
      const res = await invoke("pair_wifi_adb", { ip, port: port, pairingCode: pairCode });
      addLog(String(res));
    } catch (e) {
      addLog(`❌ Pairing Error: ${e}`);
    }
    setLoading(false);
  };

  const handleConnect = async () => {
    setLoading(true);
    addLog(`Connecting to ${ip}:${port}...`);
    try {
      const res = await invoke("connect_wifi_adb", { ip, port: port });
      addLog(String(res));
    } catch (e) {
      addLog(`❌ Connection Error: ${e}`);
    }
    setLoading(false);
  };

  const handleDisconnect = async () => {
    setLoading(true);
    try {
      const res = await invoke("disconnect_wifi_adb", { ip, port: port });
      addLog(String(res));
    } catch (e) {
      addLog(`❌ Disconnect Error: ${e}`);
    }
    setLoading(false);
  };

  const handleEnableTcpIp = async () => {
     addLog("Switching USB device to TCPIP mode...");
     try {
       const res = await invoke("enable_adb_wifi_mode");
       addLog(String(res));
     } catch (e) {
       addLog(`❌ Error: ${e}`);
     }
  };

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 p-6">
      {/* Left side: Controls */}
      <div className="flex flex-col gap-6">
        <div className="p-6 bg-white/5 border border-white/10 rounded-3xl backdrop-blur-xl">
          <div className="flex items-center gap-3 mb-6">
            <div className="p-2 bg-indigo-500/20 rounded-lg">
              <Wifi className="w-6 h-6 text-indigo-400" />
            </div>
            <h2 className="text-xl font-bold text-white">Wireless ADB (Android 11+)</h2>
          </div>

          <div className="space-y-4">
            <div className="grid grid-cols-3 gap-3">
              <div className="col-span-2">
                <label className="text-xs text-gray-400 font-bold mb-1 block">IP ADDRESS</label>
                <input 
                  value={ip} 
                  onChange={e => setIp(e.target.value)}
                  className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-2 text-white outline-none focus:border-indigo-500/50"
                  placeholder="192.168.1.10"
                />
              </div>
              <div>
                <label className="text-xs text-gray-400 font-bold mb-1 block">PORT</label>
                <input 
                  value={port} 
                  onChange={e => setPort(e.target.value)}
                  className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-2 text-white outline-none focus:border-indigo-500/50"
                  placeholder="5555"
                />
              </div>
            </div>

            <div>
              <label className="text-xs text-gray-400 font-bold mb-1 block">PAIRING CODE</label>
              <div className="relative">
                <Key className="absolute left-3 top-2.5 w-4 h-4 text-gray-500" />
                <input 
                  value={pairCode} 
                  onChange={e => setPairCode(e.target.value)}
                  className="w-full bg-white/5 border border-white/10 rounded-xl pl-10 pr-4 py-2 text-white outline-none focus:border-indigo-500/50"
                  placeholder="Enter 6-digit code"
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3 pt-4">
              <button 
                onClick={handlePair}
                disabled={loading}
                className="flex items-center justify-center gap-2 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 text-white font-bold py-3 rounded-2xl transition-all shadow-lg shadow-indigo-600/20"
              >
                <Link className="w-5 h-5" /> Pair Device
              </button>
              <button 
                onClick={handleConnect}
                disabled={loading}
                className="flex items-center justify-center gap-2 bg-green-600 hover:bg-green-500 disabled:opacity-50 text-white font-bold py-3 rounded-2xl transition-all shadow-lg shadow-green-600/20"
              >
                <Terminal className="w-5 h-5" /> Connect
              </button>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <button 
                onClick={handleDisconnect}
                disabled={loading}
                className="flex items-center justify-center gap-2 bg-white/5 hover:bg-white/10 border border-white/10 text-white font-bold py-3 rounded-2xl transition-all"
              >
                <Unlink className="w-5 h-5" /> Disconnect
              </button>
              <button 
                onClick={handleEnableTcpIp}
                disabled={loading}
                className="flex items-center justify-center gap-2 bg-amber-600/20 hover:bg-amber-600/30 border border-amber-600/30 text-amber-400 font-bold py-3 rounded-2xl transition-all"
              >
                <Monitor className="w-5 h-5" /> Enable WiFi (USB)
              </button>
            </div>
          </div>
        </div>

        <div className="p-4 bg-amber-500/10 border border-amber-500/20 rounded-2xl">
          <p className="text-xs text-amber-300 leading-relaxed">
            <strong>Pro Tip:</strong> Ensure BOTH the device and your computer are on the same WiFi network. For Android 11+, go to <strong>Developer Options {">"} Wireless Debugging</strong> to find your IP, Port, and Pairing Code.
          </p>
        </div>
      </div>

      {/* Right side: Logs */}
      <div className="flex flex-col bg-black/40 border border-white/10 rounded-3xl overflow-hidden backdrop-blur-xl">
        <div className="flex items-center justify-between px-6 py-4 border-b border-white/10 bg-white/5">
          <div className="flex items-center gap-2">
            <Terminal className="w-4 h-4 text-indigo-400" />
            <span className="text-xs font-bold text-white uppercase tracking-widest">Wireless Console</span>
          </div>
          <button 
            onClick={() => setLogs([])}
            className="text-[10px] text-gray-500 hover:text-white uppercase font-bold"
          >
            Clear logs
          </button>
        </div>
        <div className="flex-1 p-6 overflow-y-auto font-mono text-sm space-y-2 max-h-[500px]">
          {logs.length === 0 ? (
             <div className="h-full flex items-center justify-center text-gray-600 italic">
               Ready to connect...
             </div>
          ) : (
            logs.map((log, i) => (
              <div key={i} className={`p-2 rounded ${log.includes("✅") ? 'bg-green-500/5 text-green-300' : log.includes("❌") ? 'bg-red-500/5 text-red-300' : 'text-gray-300'}`}>
                {log}
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
};
