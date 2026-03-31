import { useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import { EDL_COMMANDS, type EdlDeviceInfo } from "../../lib/edl";
import { Card } from "../ui/Card";
import { TerminalLog } from "../ui/TerminalLog";

type EdlState = "idle" | "detecting" | "connected" | "error";

export default function EdlPage() {
  const [state, setState] = useState<EdlState>("idle");
  const [device, setDevice] = useState<EdlDeviceInfo | null>(null);
  const [partition, setPartition] = useState("boot");
  const [filePath, setFilePath] = useState("");
  const [running, setRunning] = useState(false);
  const [logs, setLogs] = useState<string[]>(["[info] EDL mode interface ready"]);

  const addLog = (line: string) => setLogs((prev) => [...prev, line]);

  const detect = async () => {
    setState("detecting");
    addLog("[info] Scanning for EDL 9008 device...");
    try {
      const info = await invoke<EdlDeviceInfo>(EDL_COMMANDS.DETECT);
      if (info.detected) {
        setDevice(info);
        setState("connected");
        addLog(`[info] Device detected: ${info.chipset} (${info.serial})`);
      } else {
        setState("idle");
        addLog("[info] No EDL device found");
      }
    } catch (e: unknown) {
      setState("error");
      addLog(`[error] ${String(e)}`);
    }
  };

  const run = async (cmd: string, args: Record<string, string> = {}) => {
    setRunning(true);
    addLog(`[info] Running: ${cmd}`);
    try {
      const result = await invoke<string>(cmd, args);
      addLog(`[success] ${result}`);
    } catch (e: unknown) {
      addLog(`[error] ${String(e)}`);
    }
    setRunning(false);
  };

  const connected = state === "connected";

  return (
    <div className="page">
      <div className="row-between">
        <div>
          <h2 className="page-title">EDL Mode (Qualcomm 9008)</h2>
          <p className="page-subtitle">Deep partition access via Emergency Download mode</p>
        </div>
        <button
          className="btn btn-primary btn-sm"
          disabled={state === "detecting"}
          onClick={detect}
        >
          {state === "detecting" ? "Scanning..." : "Detect Device"}
        </button>
      </div>

      {connected && device && (
        <Card title="EDL Device">
          <div className="device-grid">
            <div className="device-field">
              <span className="device-field-label">Chipset</span>
              <span className="device-field-value highlight">{device.chipset}</span>
            </div>
            <div className="device-field">
              <span className="device-field-label">Serial</span>
              <span className="device-field-value mono">{device.serial || "N/A"}</span>
            </div>
            <div className="device-field">
              <span className="device-field-label">Mode</span>
              <span className="device-field-value">{device.mode}</span>
            </div>
          </div>
        </Card>
      )}

      <Card title="Partition Operations">
        <div className="stack-sm">
          <div className="grid-two">
            <div>
              <label className="field-label">Partition</label>
              <select
                className="field-input"
                value={partition}
                onChange={(e) => setPartition(e.target.value)}
              >
                {["boot", "recovery", "system", "userdata", "modem", "fsg", "persist", "misc", "aboot", "sbl1", "tz", "rpm"].map((p) => (
                  <option key={p} value={p}>{p}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="field-label">File Path</label>
              <input
                className="field-input"
                placeholder="/path/to/image.img"
                value={filePath}
                onChange={(e) => setFilePath(e.target.value)}
              />
            </div>
          </div>

          <div className="action-row">
            <button
              className="btn btn-secondary btn-sm"
              disabled={!connected || running}
              onClick={() => run(EDL_COMMANDS.READ_PARTITION, { partition, output_path: filePath || `/tmp/${partition}.img` })}
            >
              Read
            </button>
            <button
              className="btn btn-primary btn-sm"
              disabled={!connected || running || !filePath}
              onClick={() => run(EDL_COMMANDS.WRITE_PARTITION, { partition, image_path: filePath })}
            >
              Write
            </button>
            <button
              className="btn btn-danger btn-sm"
              disabled={!connected || running}
              onClick={() => run(EDL_COMMANDS.ERASE_PARTITION, { partition })}
            >
              Erase
            </button>
            <button
              className="btn btn-ghost btn-sm"
              disabled={!connected || running}
              onClick={() => run(EDL_COMMANDS.GET_GPT)}
            >
              Print GPT
            </button>
            <button
              className="btn btn-ghost btn-sm"
              disabled={!connected || running}
              onClick={() => run(EDL_COMMANDS.REBOOT)}
            >
              Reboot
            </button>
          </div>
        </div>
      </Card>

      <div className="panel">
        <div className="row-between">
          <div className="action-title">EDL Log</div>
          <button className="btn btn-ghost btn-sm" onClick={() => setLogs([])}>Clear</button>
        </div>
        <TerminalLog lines={logs} />
      </div>
    </div>
  );
}
