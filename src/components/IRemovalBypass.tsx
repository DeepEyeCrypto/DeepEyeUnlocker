import { useState, useEffect, useRef } from "react";
import { invoke } from "@tauri-apps/api/core";
import { listen } from "@tauri-apps/api/event";

interface IRemovalDevice {
  chip_id: string;
  chip_name: string;
  udid: string;
  ecid: string;
  serial: string;
  imei: string;
  ios_version: string;
  ios_major: number;
  model: string;
  exploit_method: string; // "checkm8" | "server_bypass"
}

interface IRemovalProgress {
  session_id: string;
  event: string;
  message: string;
  [key: string]: unknown;
}

interface IRemovalResult {
  success: boolean;
  technique: string; // "A", "B", or "C"
  message: string;
}

let _sessionCounter = 0;
function newSessionId(): string {
  _sessionCounter += 1;
  return `ir-${Date.now()}-${_sessionCounter}`;
}

const TECHNIQUE_CONFIG = {
  A: {
    label: "Technique A",
    subtitle: "checkm8 + Ramdisk",
    chips: "A7–A11",
    glowClass: "shadow-red-500/30",
    borderClass: "border-red-500/40",
    bgClass: "bg-red-500/10",
    textClass: "text-red-400",
    badgeBorder: "border-red-500",
    dotClass: "bg-red-400",
    activeClass: "bg-red-700 hover:bg-red-600 shadow-red-900/40",
  },
  B: {
    label: "Technique B",
    subtitle: "A12+ Server Bypass",
    chips: "A12+ (ECID)",
    glowClass: "shadow-blue-500/30",
    borderClass: "border-blue-500/40",
    bgClass: "bg-blue-500/10",
    textClass: "text-blue-400",
    badgeBorder: "border-blue-500",
    dotClass: "bg-blue-400",
    activeClass: "bg-blue-700 hover:bg-blue-600 shadow-blue-900/40",
  },
  C: {
    label: "Technique C",
    subtitle: "iServices Fix",
    chips: "Fake Erase",
    glowClass: "shadow-green-500/30",
    borderClass: "border-green-500/40",
    bgClass: "bg-green-500/10",
    textClass: "text-green-400",
    badgeBorder: "border-green-500",
    dotClass: "bg-green-400",
    activeClass: "bg-green-700 hover:bg-green-600 shadow-green-900/40",
  },
} as const;

type Technique = keyof typeof TECHNIQUE_CONFIG;

function resolveRoute(device: IRemovalDevice | null): Technique | null {
  if (!device) return null;
  if (device.exploit_method === "checkm8") return "A";
  if (device.exploit_method === "server_bypass") return "B";
  return null;
}

function logLineColor(log: string): string {
  if (log.includes("[ERROR]") || log.includes("[FAILED]")) return "text-red-400";
  if (log.includes("[COMPLETE]") || log.includes("[SUCCESS]")) return "text-green-400";
  if (log.includes("[DEVICE_FOUND]")) return "text-violet-300";
  if (log.includes("[STARTING]")) return "text-yellow-300";
  if (log.includes("[STEP]") || log.includes("[PROGRESS]")) return "text-blue-300";
  return "text-white/60";
}

export default function IRemovalBypass() {
  const [device, setDevice] = useState<IRemovalDevice | null>(null);
  const [logs, setLogs] = useState<string[]>([]);
  const [status, setStatus] = useState<
    "idle" | "detecting" | "running" | "success" | "error"
  >("idle");
  const [result, setResult] = useState<IRemovalResult | null>(null);
  const sessionRef = useRef<string>(newSessionId());
  const logEndRef = useRef<HTMLDivElement | null>(null);

  // Auto-scroll the log terminal
  useEffect(() => {
    logEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [logs]);

  // Subscribe to iremoval-progress events from Tauri
  useEffect(() => {
    const unlisten = listen<IRemovalProgress>("iremoval-progress", (event) => {
      const p = event.payload;
      const line = `[${p.event.toUpperCase()}] ${p.message}`;
      setLogs((prev) => [...prev, line]);

      if (p.event === "device_found") {
        const dev = p as unknown as IRemovalDevice;
        if (dev.chip_id) setDevice(dev);
      }

      if (p.event === "complete") {
        const r = p as unknown as IRemovalResult & IRemovalProgress;
        setResult({
          success: r.success ?? false,
          technique: r.technique ?? "unknown",
          message: r.message ?? "",
        });
        setStatus((r.success ?? false) ? "success" : "error");
      }

      if (p.event === "failed") {
        const r = p as unknown as IRemovalResult & IRemovalProgress;
        setResult({
          success: false,
          technique: r.technique ?? "unknown",
          message: r.message ?? "",
        });
        setStatus("error");
      }

      if (p.event === "error") {
        setStatus("error");
      }
    });
    return () => {
      unlisten.then((fn) => fn());
    };
  }, []);

  const handleDetect = async () => {
    setStatus("detecting");
    setDevice(null);
    setResult(null);
    setLogs([]);
    sessionRef.current = newSessionId();
    try {
      const dev = await invoke<IRemovalDevice>("iremoval_detect", {
        sessionId: sessionRef.current,
      });
      setDevice(dev);
      setStatus("idle");
    } catch (e) {
      setLogs((prev) => [...prev, `[ERROR] ${e}`]);
      setStatus("error");
    }
  };

  const handleRun = async () => {
    if (!device) return;
    setStatus("running");
    setResult(null);
    try {
      const res = await invoke<IRemovalResult>("iremoval_run", {
        sessionId: sessionRef.current,
      });
      setResult(res);
      setStatus(res.success ? "success" : "error");
    } catch (e) {
      setLogs((prev) => [...prev, `[ERROR] ${e}`]);
      setStatus("error");
    }
  };

  const handleIServices = async () => {
    if (!device) return;
    setStatus("running");
    setResult(null);
    try {
      const res = await invoke<IRemovalResult>("iremoval_iservices", {
        sessionId: sessionRef.current,
      });
      setResult(res);
      setStatus(res.success ? "success" : "error");
    } catch (e) {
      setLogs((prev) => [...prev, `[ERROR] ${e}`]);
      setStatus("error");
    }
  };

  const route = resolveRoute(device);

  const statusColor = {
    idle: "text-white/40",
    detecting: "text-yellow-400",
    running: "text-blue-400",
    success: "text-green-400",
    error: "text-red-400",
  }[status];

  const isBusy = status === "detecting" || status === "running";

  return (
    <div className="p-3 sm:p-4 lg:p-6 bg-black/40 backdrop-blur-xl rounded-2xl border border-white/10 text-white font-mono text-xs space-y-5">
      {/* Header */}
      <div className="flex justify-between items-center">
        <div>
          <h2 className="text-xl font-bold tracking-tighter text-cyan-400">
            iREMOVAL BYPASS ENGINE
          </h2>
          <p className="text-white/30 text-xs mt-0.5">
            A7–A18 · checkm8 · Server Bypass · iServices Fix
          </p>
        </div>
        <div
          className={`px-3 py-1 rounded-full border text-xs sm:text-sm ${statusColor} border-current`}
        >
          {status.toUpperCase()}
        </div>
      </div>

      {/* Technique Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-2 sm:gap-3">
        {(Object.keys(TECHNIQUE_CONFIG) as Technique[]).map((key) => {
          const cfg = TECHNIQUE_CONFIG[key];
          const isActive = route === key;
          return (
            <div
              key={key}
              className={`rounded-xl border p-3 space-y-0.5 transition-all ${
                isActive
                  ? `${cfg.borderClass} ${cfg.bgClass} shadow-lg ${cfg.glowClass}`
                  : "border-white/10 bg-white/5"
              }`}
            >
              <div className="flex items-center gap-1.5">
                <span
                  className={`w-1.5 h-1.5 rounded-full ${
                    isActive ? cfg.dotClass : "bg-white/20"
                  }`}
                />
                <span
                  className={`font-bold ${
                    isActive ? cfg.textClass : "text-white/40"
                  }`}
                >
                  {cfg.label}
                </span>
              </div>
              <div
                className={`text-xs ${
                  isActive ? "text-white/80" : "text-white/30"
                }`}
              >
                {cfg.subtitle}
              </div>
              <div
                className={`text-xs ${
                  isActive ? cfg.textClass : "text-white/20"
                }`}
              >
                {cfg.chips}
              </div>
            </div>
          );
        })}
      </div>

      {/* Device Info Panel */}
      {device ? (
        <div className="bg-white/5 rounded-xl border border-white/10 p-3 sm:p-4 space-y-2">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 sm:gap-3">
            <div className="flex justify-between items-center col-span-1 sm:col-span-2">
              <span className="text-white/60">DEVICE</span>
              <span className="text-white font-bold">{device.model}</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-white/60">CHIP</span>
              <span className="text-cyan-300">
                {device.chip_name} ({device.chip_id})
              </span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-white/60">iOS</span>
              <span className="text-white">{device.ios_version}</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-white/60">SERIAL</span>
              <span className="text-white/70 text-xs">{device.serial}</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-white/60">IMEI</span>
              <span className="text-white/70 text-xs">{device.imei}</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-white/60">ECID</span>
              <span className="text-white/70 text-xs">{device.ecid}</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-white/60">METHOD</span>
              <span
                className={
                  device.exploit_method === "server_bypass"
                    ? "text-blue-400"
                    : "text-red-400"
                }
              >
                {device.exploit_method.toUpperCase()}
              </span>
            </div>
          </div>

          {/* Route Indicator */}
          {route && (
            <div className="pt-1.5">
              <div
                className={`inline-flex items-center gap-2 px-3 py-1.5 rounded-lg border text-xs sm:text-sm font-bold ${
                  TECHNIQUE_CONFIG[route].borderClass
                } ${TECHNIQUE_CONFIG[route].textClass}`}
              >
                <span
                  className={`w-1.5 h-1.5 rounded-full ${TECHNIQUE_CONFIG[route].dotClass}`}
                />
                ROUTE: {TECHNIQUE_CONFIG[route].label.toUpperCase()} —{" "}
                {TECHNIQUE_CONFIG[route].subtitle}
              </div>
            </div>
          )}
        </div>
      ) : (
        <div className="bg-white/5 rounded-xl border border-white/10 p-3 sm:p-4 text-center text-white/30">
          NO DEVICE DETECTED — CONNECT iOS DEVICE AND CLICK DETECT
        </div>
      )}

      {/* Progress Log Terminal */}
      <div className="bg-black/60 rounded-xl p-3 min-h-[120px] sm:min-h-[160px] max-h-48 sm:max-h-64 overflow-y-auto border border-white/5 space-y-0.5">
        {logs.length === 0 ? (
          <div className="text-white/20 italic">awaiting commands…</div>
        ) : (
          logs.map((log, i) => (
            <div key={i} className={logLineColor(log)}>
              {log}
            </div>
          ))
        )}
        <div ref={logEndRef} />
      </div>

      {/* Result Summary */}
      {result && (
        <div
          className={`rounded-xl border p-3 space-y-1.5 ${
            result.success
              ? "border-green-500/40 bg-green-500/10"
              : "border-red-500/40 bg-red-500/10"
          }`}
        >
          <div className="flex justify-between items-center">
            <span className="text-white/60">RESULT</span>
            <span
              className={result.success ? "text-green-400" : "text-red-400"}
            >
              {result.success ? "✓ BYPASS OK" : "✗ FAILED"}
            </span>
          </div>
          <div className="flex justify-between items-center">
            <span className="text-white/60">TECHNIQUE</span>
            <span className="text-white">
              {result.technique.toUpperCase()}
              {result.technique in TECHNIQUE_CONFIG
                ? ` — ${TECHNIQUE_CONFIG[result.technique as Technique].subtitle}`
                : ""}
            </span>
          </div>
          {result.message && (
            <div className="flex justify-between items-start gap-4">
              <span className="text-white/60 shrink-0">MSG</span>
              <span className="text-white/70 text-right">{result.message}</span>
            </div>
          )}
        </div>
      )}

      {/* Action Buttons */}
      <div className="flex flex-col gap-2">
        <button
          onClick={handleDetect}
          disabled={isBusy}
          className="w-full py-3 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl font-bold transition-all disabled:opacity-40"
        >
          {status === "detecting" ? "DETECTING…" : "DETECT DEVICE"}
        </button>
        <div className="flex flex-col sm:flex-row gap-2 sm:gap-3">
          <button
            onClick={handleRun}
            disabled={!device || isBusy}
            className={`flex-1 py-3 rounded-xl font-bold transition-all shadow-lg disabled:opacity-40 ${
              route
                ? `${TECHNIQUE_CONFIG[route].activeClass}`
                : "bg-cyan-700 hover:bg-cyan-600 shadow-cyan-900/40"
            }`}
          >
            {status === "running" ? "BYPASSING…" : "RUN FULL PIPELINE"}
          </button>
          <button
            onClick={handleIServices}
            disabled={!device || isBusy}
            className="flex-1 py-3 bg-green-800/60 hover:bg-green-700/70 border border-green-500/30 disabled:opacity-40 rounded-xl font-bold transition-all shadow-lg shadow-green-900/30"
          >
            FIX iSERVICES ONLY
          </button>
        </div>
      </div>
    </div>
  );
}
