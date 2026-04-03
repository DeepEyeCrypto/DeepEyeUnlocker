import { useEffect, useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import { listen } from "@tauri-apps/api/event";
import { Card } from "../ui/Card";
import { TerminalLog } from "../ui/TerminalLog";

export default function AdbToolsPage() {
  const [deviceSerial, setDeviceSerial] = useState("");
  const [running, setRunning] = useState(false);
  const [logs, setLogs] = useState<string[]>(["[info] ADB tools ready"]);

  const addLog = (line: string) => {
    setLogs((prev) => [...prev.slice(-199), line]);
  };

  useEffect(() => {
    const unlistenLog = listen<string>("adb-log-line", (event) => {
      addLog(event.payload);
    });

    const unlistenError = listen<string>("adb-log-error", (event) => {
      addLog(`[error] ${event.payload}`);
    });

    const unlistenTerminated = listen<string>("adb-log-terminated", (event) => {
      addLog(`[info] ${event.payload}`);
      setRunning(false);
    });

    return () => {
      void unlistenLog.then((unlisten) => unlisten());
      void unlistenError.then((unlisten) => unlisten());
      void unlistenTerminated.then((unlisten) => unlisten());
    };
  }, []);

  const startLogcat = async () => {
    setRunning(true);
    setLogs(["[info] Starting ADB logcat stream..."]);

    try {
      await invoke("stream_adb_logs", {
        deviceSerial: deviceSerial.trim() || null,
      });
    } catch (error: unknown) {
      addLog(`[error] ${String(error)}`);
      setRunning(false);
    }
  };

  return (
    <div className="page">
      <div>
        <h2 className="page-title">ADB Tools</h2>
        <p className="page-subtitle">Android bridge log streaming and host-targeted serial routing</p>
      </div>

      <Card title="ADB Logcat Stream">
        <div className="stack-sm">
          <div className="grid-two">
            <div>
              <label className="field-label">Target Serial</label>
              <input
                className="field-input"
                placeholder="Optional device serial"
                value={deviceSerial}
                onChange={(event) => setDeviceSerial(event.target.value)}
              />
            </div>
            <div>
              <label className="field-label">Session Control</label>
              <button className="btn btn-primary btn-sm" disabled={running} onClick={() => void startLogcat()}>
                {running ? "Streaming..." : "Start Logcat"}
              </button>
            </div>
          </div>

          <div className="panel">
            Use a blank serial to target the default transport, or enter a specific serial for a single attached device.
          </div>
        </div>
      </Card>

      <div className="panel">
        <div className="row-between">
          <div className="action-title">ADB Console</div>
          <button className="btn btn-ghost btn-sm" onClick={() => setLogs([])}>Clear</button>
        </div>
        <TerminalLog lines={logs} />
      </div>
    </div>
  );
}
