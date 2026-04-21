import { useCallback, useEffect, useMemo, useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import {
  ROM_FLASHER_COMMANDS,
  type FastbootDevice,
  type FastbootQueryResult,
  type FastbootVariable,
  type FlashResult,
} from "../../lib/rom_flasher";
import { Card } from "../ui/Card";
import { TerminalLog } from "../ui/TerminalLog";
import { ProgressStep, type ProgressItem } from "../ui/ProgressStep";

type FlashMode = "sideload" | "fastboot";
type FastbootTarget = "system" | "bootloader" | "fastboot" | "recovery";

const FASTBOOT_PARTITIONS = [
  "boot",
  "boot_a",
  "boot_b",
  "init_boot",
  "vendor_boot",
  "recovery",
  "dtbo",
  "vbmeta",
  "vbmeta_system",
  "system",
  "system_ext",
  "vendor",
  "product",
  "super",
  "userdata",
  "cache",
  "frp",
  "metadata",
] as const;

const SUMMARY_VARIABLES = [
  { label: "Product", keys: ["product"] },
  { label: "Serial", keys: ["serialno", "serial-number"] },
  { label: "Unlocked", keys: ["unlocked"] },
  { label: "Current Slot", keys: ["current-slot"] },
  { label: "Slot Count", keys: ["slot-count"] },
  { label: "Userspace", keys: ["is-userspace"] },
] as const;

export default function RomFlasherPage() {
  const [mode, setMode] = useState<FlashMode>("sideload");
  const [zipPath, setZipPath] = useState("");
  const [partition, setPartition] = useState<string>(FASTBOOT_PARTITIONS[0]);
  const [imagePath, setImagePath] = useState("");
  const [running, setRunning] = useState(false);
  const [logs, setLogs] = useState<string[]>(["[info] ROM Flasher ready"]);
  const [step, setStep] = useState(0);
  const [devices, setDevices] = useState<FastbootDevice[]>([]);
  const [selectedSerial, setSelectedSerial] = useState("");
  const [variables, setVariables] = useState<FastbootVariable[]>([]);
  const [rebootTarget, setRebootTarget] = useState<FastbootTarget>("system");
  const [refreshingDevices, setRefreshingDevices] = useState(false);
  const [queryingVariables, setQueryingVariables] = useState(false);

  const addLog = useCallback((line: string) => {
    setLogs((prev) => [...prev, line]);
  }, []);

  const requiresManualSerialSelection = devices.length > 1 && !selectedSerial.trim();

  const resolveSerial = useCallback(
    (fallbackDevices: FastbootDevice[] = devices): string | undefined => {
      const explicitSerial = selectedSerial.trim();
      if (explicitSerial) {
        return explicitSerial;
      }

      if (fallbackDevices.length === 1) {
        return fallbackDevices[0].serial;
      }

      return undefined;
    },
    [devices, selectedSerial],
  );

  const variableMap = useMemo(() => {
    return new Map(variables.map((variable) => [variable.key, variable.value]));
  }, [variables]);

  const summaryVariables = useMemo(() => {
    return SUMMARY_VARIABLES.map((entry) => {
      const value = entry.keys
        .map((key) => variableMap.get(key))
        .find((candidate): candidate is string => Boolean(candidate && candidate.trim()));

      return {
        label: entry.label,
        value: value ?? "—",
      };
    });
  }, [variableMap]);

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

  const refreshFastbootDevices = useCallback(async (): Promise<FastbootDevice[]> => {
    setRefreshingDevices(true);

    try {
      const foundDevices = await invoke<FastbootDevice[]>(ROM_FLASHER_COMMANDS.LIST_DEVICES);
      setDevices(foundDevices);
      setSelectedSerial((currentSerial) => {
        if (currentSerial && foundDevices.some((device) => device.serial === currentSerial)) {
          return currentSerial;
        }

        return foundDevices.length === 1 ? foundDevices[0].serial : "";
      });

      if (foundDevices.length === 0) {
        setVariables([]);
        addLog("[info] No fastboot devices detected");
      } else {
        addLog(`[success] Detected ${foundDevices.length} fastboot device${foundDevices.length === 1 ? "" : "s"}`);
      }

      return foundDevices;
    } catch (error: unknown) {
      addLog(`[error] ${String(error)}`);
      return [];
    } finally {
      setRefreshingDevices(false);
    }
  }, [addLog]);

  const queryFastbootVariables = useCallback(async () => {
    if (devices.length === 0) {
      addLog("[error] No fastboot device detected");
      return;
    }

    if (requiresManualSerialSelection) {
      addLog("[error] Select a fastboot serial before querying variables");
      return;
    }

    setQueryingVariables(true);
    addLog("[info] Querying fastboot getvar:all...");

    try {
      const serial = resolveSerial();
      const result = await invoke<FastbootQueryResult>(
        ROM_FLASHER_COMMANDS.GET_ALL_VARIABLES,
        serial ? { serial } : {},
      );
      setVariables(result.variables);
      addLog(`[success] Retrieved ${result.variables.length} fastboot variables`);
    } catch (error: unknown) {
      addLog(`[error] ${String(error)}`);
    } finally {
      setQueryingVariables(false);
    }
  }, [addLog, devices.length, requiresManualSerialSelection, resolveSerial]);

  useEffect(() => {
    if (mode === "fastboot") {
      void refreshFastbootDevices();
    }
  }, [mode, refreshFastbootDevices]);

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

    try {
      let foundDevices = devices;

      if (foundDevices.length === 0) {
        addLog("[info] Rebooting to bootloader...");
        try {
          const rebootMessage = await invoke<string>(ROM_FLASHER_COMMANDS.REBOOT_BOOTLOADER);
          addLog(`[success] ${rebootMessage}`);
        } catch (error: unknown) {
          addLog(`[warn] ${String(error)}`);
        }

        foundDevices = await refreshFastbootDevices();
      }

      if (foundDevices.length === 0) {
        addLog("[error] No fastboot device detected after reboot request");
        return;
      }

      if (foundDevices.length > 1 && !selectedSerial.trim()) {
        addLog("[error] Multiple fastboot devices detected — select a serial first");
        return;
      }

      setStep(1);

      addLog(`[info] Flashing ${partition} from ${imagePath}...`);
      const serial = resolveSerial(foundDevices);
      const result = await invoke<FlashResult>(ROM_FLASHER_COMMANDS.FLASH_PARTITION, {
        partition,
        image_path: imagePath,
        ...(serial ? { serial } : {}),
      });
      addLog(result.success ? `[success] ${result.message}` : `[error] ${result.message}`);
      setStep(result.success ? 2 : 1);
    } catch (e: unknown) {
      addLog(`[error] ${String(e)}`);
    } finally {
      setRunning(false);
    }
  };

  const eraseSelectedPartition = async () => {
    if (requiresManualSerialSelection) {
      addLog("[error] Select a fastboot serial before erasing a partition");
      return;
    }

    setRunning(true);
    addLog(`[info] Erasing ${partition} via fastboot...`);

    try {
      const serial = resolveSerial();
      const result = await invoke<FlashResult>(ROM_FLASHER_COMMANDS.ERASE_PARTITION, {
        partition,
        ...(serial ? { serial } : {}),
      });
      addLog(result.success ? `[success] ${result.message}` : `[error] ${result.message}`);
    } catch (error: unknown) {
      addLog(`[error] ${String(error)}`);
    } finally {
      setRunning(false);
    }
  };

  const wipeData = async () => {
    if (requiresManualSerialSelection) {
      addLog("[error] Select a fastboot serial before wiping data");
      return;
    }

    setRunning(true);
    addLog("[info] Wiping data via fastboot...");

    try {
      const serial = resolveSerial();
      const result = await invoke<string>(ROM_FLASHER_COMMANDS.WIPE_DATA, serial ? { serial } : {});
      addLog(`[success] ${result}`);
    } catch (e: unknown) {
      addLog(`[error] ${String(e)}`);
    } finally {
      setRunning(false);
    }
  };

  const rebootFastbootTarget = async () => {
    if (requiresManualSerialSelection) {
      addLog("[error] Select a fastboot serial before rebooting");
      return;
    }

    setRunning(true);
    addLog(`[info] Rebooting fastboot device to ${rebootTarget}...`);

    try {
      const serial = resolveSerial();
      const result = await invoke<string>(ROM_FLASHER_COMMANDS.REBOOT_TARGET, {
        target: rebootTarget,
        ...(serial ? { serial } : {}),
      });
      addLog(`[success] ${result}`);
    } catch (error: unknown) {
      addLog(`[error] ${String(error)}`);
    } finally {
      setRunning(false);
    }
  };

  const unlockBootloader = async () => {
    if (requiresManualSerialSelection) {
      addLog("[error] Select a fastboot serial before unlocking the bootloader");
      return;
    }

    setRunning(true);
    addLog("[info] Sending fastboot unlock sequence...");

    try {
      const serial = resolveSerial();
      const result = await invoke<FlashResult>(
        ROM_FLASHER_COMMANDS.UNLOCK_BOOTLOADER,
        serial ? { serial } : {},
      );
      addLog(result.success ? `[success] ${result.message}` : `[error] ${result.message}`);

      if (result.success) {
        await queryFastbootVariables();
      }
    } catch (error: unknown) {
      addLog(`[error] ${String(error)}`);
    } finally {
      setRunning(false);
    }
  };

  return (
    <div className="page">
      <div className="row-between">
        <div>
          <h2 className="page-title">Fastboot Protocol</h2>
          <p className="page-subtitle">Query devices, inspect getvar output, flash partitions, erase partitions, wipe data, and reboot targets</p>
        </div>
        <div className="tab-row tab-bar">
          <button
            className={`btn btn-sm tab-btn ${mode === "sideload" ? "btn-primary" : "btn-ghost"}`}
            onClick={() => { setMode("sideload"); setStep(0); }}
          >
            TWRP Sideload
          </button>
          <button
            className={`btn btn-sm tab-btn ${mode === "fastboot" ? "btn-primary" : "btn-ghost"}`}
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
        <Card title="Fastboot Control">
          <div className="stack-sm">
            <div className="row-between">
              <div>
                <div className="action-title">Device State</div>
                <span className="meta-text">
                  {devices.length === 0
                    ? "No fastboot device detected"
                    : `${devices.length} fastboot device${devices.length === 1 ? "" : "s"} detected`}
                </span>
              </div>
              <div className="action-row">
                <button
                  className="btn btn-ghost btn-sm"
                  disabled={running || refreshingDevices}
                  onClick={() => void refreshFastbootDevices()}
                >
                  {refreshingDevices ? "Refreshing..." : "Refresh Devices"}
                </button>
                <button
                  className="btn btn-ghost btn-sm"
                  disabled={running || queryingVariables || devices.length === 0 || requiresManualSerialSelection}
                  onClick={() => void queryFastbootVariables()}
                >
                  {queryingVariables ? "Reading..." : "Read getvar:all"}
                </button>
              </div>
            </div>

            {devices.length > 0 && (
              <div>
                <label className="field-label">Fastboot Serial</label>
                <select
                  className="field-input"
                  value={selectedSerial}
                  onChange={(event) => setSelectedSerial(event.target.value)}
                >
                  {devices.length > 1 && <option value="">Select a fastboot device</option>}
                  {devices.map((device) => (
                    <option key={device.serial} value={device.serial}>
                      {device.serial} — {device.state}
                    </option>
                  ))}
                </select>
              </div>
            )}

            {summaryVariables.some((entry) => entry.value !== "—") && (
              <div className="grid-two">
                {summaryVariables.map((entry) => (
                  <div key={entry.label}>
                    <label className="field-label">{entry.label}</label>
                    <input className="field-input" value={entry.value} readOnly />
                  </div>
                ))}
              </div>
            )}

            {variables.length > 0 && (
              <div>
                <label className="field-label">Parsed Fastboot Variables</label>
                <pre
                  className="field-input"
                  style={{ minHeight: 180, overflow: "auto", whiteSpace: "pre-wrap" }}
                >
                  {variables.map((variable) => `${variable.key}: ${variable.value}`).join("\n")}
                </pre>
              </div>
            )}

            <div className="grid-two">
              <div>
                <label className="field-label">Partition</label>
                <select
                  className="field-input"
                  value={partition}
                  onChange={(e) => setPartition(e.target.value)}
                >
                  {FASTBOOT_PARTITIONS.map((p) => (
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
            <div className="grid-two">
              <div>
                <label className="field-label">Reboot Target</label>
                <select
                  className="field-input"
                  value={rebootTarget}
                  onChange={(event) => setRebootTarget(event.target.value as FastbootTarget)}
                >
                  <option value="system">system</option>
                  <option value="bootloader">bootloader</option>
                  <option value="fastboot">fastboot / fastbootd</option>
                  <option value="recovery">recovery</option>
                </select>
              </div>
              <div className="action-row" style={{ alignItems: "flex-end" }}>
                <button
                  className="btn btn-ghost btn-sm"
                  disabled={running || devices.length === 0 || requiresManualSerialSelection}
                  onClick={rebootFastbootTarget}
                >
                  Reboot Target
                </button>
                <button
                  className="btn btn-danger btn-sm"
                  disabled={running || devices.length === 0 || requiresManualSerialSelection}
                  onClick={unlockBootloader}
                >
                  Unlock Bootloader
                </button>
              </div>
            </div>
            <div className="action-row">
              <button
                className="btn btn-primary btn-md"
                disabled={running || !imagePath || requiresManualSerialSelection}
                onClick={runFastbootFlash}
              >
                {running ? "Flashing..." : "Flash Partition"}
              </button>
              <button
                className="btn btn-ghost btn-sm"
                disabled={running || devices.length === 0 || requiresManualSerialSelection}
                onClick={eraseSelectedPartition}
              >
                Erase Partition
              </button>
              <button
                className="btn btn-danger btn-sm"
                disabled={running || devices.length === 0 || requiresManualSerialSelection}
                onClick={wipeData}
              >
                Wipe Data
              </button>
            </div>
            {requiresManualSerialSelection && (
              <div className="danger-note">
                <div className="danger-title">Multiple Fastboot Devices</div>
                <span className="meta-text">
                  Select a specific serial before running flash, erase, unlock, or reboot operations.
                </span>
              </div>
            )}
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
