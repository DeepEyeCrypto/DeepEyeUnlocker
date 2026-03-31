import { useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import { ROM_FLASHER_COMMANDS, type FlashResult } from "../../lib/rom_flasher";
import { Card } from "../ui/Card";
import { TerminalLog } from "../ui/TerminalLog";
import { ProgressStep, type ProgressItem } from "../ui/ProgressStep";

type FlashMode = "sideload" | "fastboot";

export default function RomFlasherPage() {
  const [mode, setMode] = useState<FlashMode>("sideload");
  const [zipPath, setZipPath] = useState("");
  const [partition, setPartition] = useState("boot");
  const [imagePath, setImagePath] = useState("");
  const [running, setRunning] = useState(false);
  const [logs, setLogs] = useState<string[]>(["[info] ROM Flasher ready"]);
  const [step, setStep] = useState(0);

  const addLog = (line: string) => setLogs((prev) => [...prev, line]);

  const steps: ProgressItem[] = mode === "sideload"
    ? [
        { id: "reboot", label: "Reboot Recovery", status: step >= 1 ? "done" : step === 0 && running ? "active" : "idle" },
        { id: "sideload", label: "Sideload ZIP", status: step >= 2 ? "done" : step === 1 && running ? "active" : "idle" },
        { id: "complete", label: "Complete", status: step >= 2 ? "done" : "idle" },
      ]
    : [
        { id: "reboot", label: "Reboot Bootloader", status: step >= 1 ? "done" : step === 0 && running ? "active" : "idle" },
        { id: "flash", label: "Flash Partition", status: step >= 2 ? "done" : step === 1 && running ? "active" : "idle" },
        { id: "complete", label: "Complete", status: step >= 2 ? "done" : "idle" },
      ];

  const runSideload = async () => {
    if (!zipPath) return;
    setRunning(true);
    setStep(0);

    addLog("[info] Rebooting to recovery...");
    try {
      await invoke<string>(ROM_FLASHER_COMMANDS.REBOOT_RECOVERY);
      addLog("[success] Recovery mode — enable ADB sideload on device");
      setStep(1);

      addLog(`[info] Sideloading ${zipPath}...`);
      const result = await invoke<FlashResult>(ROM_FLASHER_COMMANDS.SIDELOAD_ZIP, { zip_path: zipPath });
      addLog(result.success ? `[success] ${result.message}` : `[error] ${result.message}`);
      setStep(2);
    } catch (e: unknown) {
      addLog(`[error] ${String(e)}`);
    }
    setRunning(false);
  };

  const runFastbootFlash = async () => {
    if (!imagePath) return;
    setRunning(true);
    setStep(0);

    addLog("[info] Rebooting to bootloader...");
    try {
      await invoke<string>(ROM_FLASHER_COMMANDS.REBOOT_BOOTLOADER);
      addLog("[success] Bootloader mode entered");
      setStep(1);

      addLog(`[info] Flashing ${partition} from ${imagePath}...`);
      const result = await invoke<FlashResult>(ROM_FLASHER_COMMANDS.FLASH_PARTITION, {
        partition,
        image_path: imagePath,
      });
      addLog(result.success ? `[success] ${result.message}` : `[error] ${result.message}`);
      setStep(2);
    } catch (e: unknown) {
      addLog(`[error] ${String(e)}`);
    }
    setRunning(false);
  };

  const wipeData = async () => {
    setRunning(true);
    addLog("[info] Wiping data via fastboot...");
    try {
      const result = await invoke<string>(ROM_FLASHER_COMMANDS.WIPE_DATA);
      addLog(`[success] ${result}`);
    } catch (e: unknown) {
      addLog(`[error] ${String(e)}`);
    }
    setRunning(false);
  };

  return (
    <div className="page">
      <div className="row-between">
        <div>
          <h2 className="page-title">Custom ROM Flasher</h2>
          <p className="page-subtitle">Sideload ZIP (TWRP) or Fastboot flash partitions</p>
        </div>
        <div className="tab-row">
          <button
            className={`btn btn-sm ${mode === "sideload" ? "btn-primary" : "btn-ghost"}`}
            onClick={() => { setMode("sideload"); setStep(0); }}
          >
            TWRP Sideload
          </button>
          <button
            className={`btn btn-sm ${mode === "fastboot" ? "btn-primary" : "btn-ghost"}`}
            onClick={() => { setMode("fastboot"); setStep(0); }}
          >
            Fastboot Flash
          </button>
        </div>
      </div>

      <ProgressStep steps={steps} />

      {mode === "sideload" && (
        <Card title="TWRP Sideload">
          <div className="stack-sm">
            <div>
              <label className="field-label">ROM ZIP Path</label>
              <input
                className="field-input"
                placeholder="/path/to/rom.zip"
                value={zipPath}
                onChange={(e) => setZipPath(e.target.value)}
              />
            </div>
            <div className="action-row">
              <button
                className="btn btn-primary btn-md"
                disabled={running || !zipPath}
                onClick={runSideload}
              >
                {running ? "Flashing..." : "Start Sideload"}
              </button>
            </div>
            <div className="danger-note">
              <div className="danger-title">Before sideloading</div>
              <span className="meta-text">
                1. Boot device into TWRP recovery{"\n"}
                2. Select "Advanced" then "ADB Sideload"{"\n"}
                3. Swipe to start sideload on device{"\n"}
                4. Then click "Start Sideload" above
              </span>
            </div>
          </div>
        </Card>
      )}

      {mode === "fastboot" && (
        <Card title="Fastboot Flash">
          <div className="stack-sm">
            <div className="grid-two">
              <div>
                <label className="field-label">Partition</label>
                <select
                  className="field-input"
                  value={partition}
                  onChange={(e) => setPartition(e.target.value)}
                >
                  {["boot", "recovery", "system", "vendor", "dtbo", "vbmeta", "cache", "userdata", "product", "system_ext"].map((p) => (
                    <option key={p} value={p}>{p}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="field-label">Image Path</label>
                <input
                  className="field-input"
                  placeholder="/path/to/boot.img"
                  value={imagePath}
                  onChange={(e) => setImagePath(e.target.value)}
                />
              </div>
            </div>
            <div className="action-row">
              <button
                className="btn btn-primary btn-md"
                disabled={running || !imagePath}
                onClick={runFastbootFlash}
              >
                {running ? "Flashing..." : "Flash Partition"}
              </button>
              <button
                className="btn btn-danger btn-sm"
                disabled={running}
                onClick={wipeData}
              >
                Wipe Data
              </button>
            </div>
          </div>
        </Card>
      )}

      <div className="panel">
        <div className="row-between">
          <div className="action-title">Flash Log</div>
          <button className="btn btn-ghost btn-sm" onClick={() => setLogs([])}>Clear</button>
        </div>
        <TerminalLog lines={logs} />
      </div>
    </div>
  );
}
