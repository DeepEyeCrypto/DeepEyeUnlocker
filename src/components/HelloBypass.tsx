import { useState, useEffect, useRef } from "react";
import { invoke } from "@tauri-apps/api/core";
import { listen } from "@tauri-apps/api/event";

interface HelloBypassDevice {
  chip_id: string;
  chip_name: string;
  udid: string;
  ios_version: string;
  ios_build: string;
  ios_major: number;
  model: string;
  serial: string;
  exploit_method: string; // "checkm8" | "server_bypass"
}

interface HelloBypassProgress {
  session_id: string;
  event: string;
  msg: string;
  [key: string]: unknown;
}

interface HelloBypassResult {
  success: boolean;
  method: string;
  notes: string[];
}

let _sessionCounter = 0;
function newSessionId(): string {
  _sessionCounter += 1;
  return `hb-${Date.now()}-${_sessionCounter}`;
}

export default function HelloBypass() {
  const [device, setDevice] = useState<HelloBypassDevice | null>(null);
  const [logs, setLogs] = useState<string[]>([]);
  const [status, setStatus] = useState<
    "idle" | "detecting" | "running" | "success" | "error"
  >("idle");
  const [result, setResult] = useState<HelloBypassResult | null>(null);
  const sessionRef = useRef<string>(newSessionId());
  const logEndRef = useRef<HTMLDivElement | null>(null);

  // Auto-scroll the log terminal
  useEffect(() => {
    logEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [logs]);

  // Subscribe to hello-bypass-progress events from Tauri
  useEffect(() => {
    const unlisten = listen<HelloBypassProgress>(
      "hello-bypass-progress",
      (event) => {
        const p = event.payload;
        const line = `[${p.event.toUpperCase()}] ${p.msg}`;
        setLogs((prev) => [...prev, line]);

        if (p.event === "device_found") {
          // The extra fields carry the device struct fields
          const dev = p as unknown as HelloBypassDevice;
          if (dev.chip_id) setDevice(dev);
        }

        if (p.event === "complete") {
          const r = p as unknown as HelloBypassResult & HelloBypassProgress;
          setResult({
            success: r.success ?? false,
            method: r.method ?? "unknown",
            notes: Array.isArray(r.notes) ? r.notes : [],
          });
          setStatus(r.success ? "success" : "error");
        }

        if (p.event === "error") {
          setStatus("error");
        }
      }
    );
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
      const dev = await invoke<HelloBypassDevice>("hello_bypass_detect", {
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
      const res = await invoke<HelloBypassResult>("hello_bypass_run", {
        sessionId: sessionRef.current,
      });
      setResult(res);
      setStatus(res.success ? "success" : "error");
    } catch (e) {
      setLogs((prev) => [...prev, `[ERROR] ${e}`]);
      setStatus("error");
    }
  };

  const routeLabel =
    device?.exploit_method === "server_bypass"
      ? "A12+ SERVER BYPASS"
      : device?.exploit_method === "checkm8"
      ? "A7–A11 CHECKM8"
      : null;

  const routeColor =
    device?.exploit_method === "server_bypass"
      ? "border-green-500 text-green-400"
      : device?.exploit_method === "checkm8"
      ? "border-red-500 text-red-400"
      : "border-white/20 text-white/40";

  const statusColor = {
    idle: "text-white/40",
    detecting: "text-yellow-400",
    running: "text-blue-400",
    success: "text-green-400",
    error: "text-red-400",
  }[status];

  return (
    <div className="p-6 bg-black/40 backdrop-blur-xl rounded-2xl border border-white/10 text-white font-mono text-xs space-y-5">
      {/* Header */}
      <div className="flex justify-between items-center">
        <h2 className="text-xl font-bold tracking-tighter text-violet-400">
          HELLO BYPASS ENGINE v2
        </h2>
        <div className={`px-3 py-1 rounded-full border text-[10px] ${statusColor} border-current`}>
          {status.toUpperCase()}
        </div>
      </div>

      {/* Device Info Panel */}
      {device ? (
        <div className="bg-white/5 rounded-xl border border-white/10 p-4 space-y-2">
          <div className="flex justify-between items-center">
            <span className="text-white/60">DEVICE</span>
            <span className="text-white font-bold">{device.model}</span>
          </div>
          <div className="flex justify-between items-center">
            <span className="text-white/60">CHIP</span>
            <span className="text-violet-300">
              {device.chip_name} ({device.chip_id})
            </span>
          </div>
          <div className="flex justify-between items-center">
            <span className="text-white/60">iOS</span>
            <span className="text-white">
              {device.ios_version} ({device.ios_build})
            </span>
          </div>
          <div className="flex justify-between items-center">
            <span className="text-white/60">SERIAL</span>
            <span className="text-white/70 text-[10px]">{device.serial}</span>
          </div>
          <div className="flex justify-between items-center">
            <span className="text-white/60">UDID</span>
            <span className="text-white/50 text-[10px] truncate max-w-[200px]">
              {device.udid}
            </span>
          </div>

          {/* Bypass Route Badge */}
          {routeLabel && (
            <div className="pt-2">
              <div
                className={`inline-flex items-center gap-2 px-3 py-1.5 rounded-lg border text-[10px] font-bold ${routeColor}`}
              >
                <span
                  className={`w-1.5 h-1.5 rounded-full bg-current`}
                />
                {routeLabel}
              </div>
            </div>
          )}
        </div>
      ) : (
        <div className="bg-white/5 rounded-xl border border-white/10 p-4 text-center text-white/30">
          NO DEVICE DETECTED — CONNECT iOS DEVICE AND CLICK DETECT
        </div>
      )}

      {/* Progress Log Terminal */}
      <div className="bg-black/60 rounded-xl p-3 min-h-[160px] max-h-64 overflow-y-auto border border-white/5 space-y-0.5">
        {logs.length === 0 ? (
          <div className="text-white/20 italic">awaiting commands…</div>
        ) : (
          logs.map((log, i) => (
            <div
              key={i}
              className={
                log.includes("[ERROR]")
                  ? "text-red-400"
                  : log.includes("[COMPLETE]") || log.includes("[SUCCESS]")
                  ? "text-green-400"
                  : log.includes("[DEVICE_FOUND]")
                  ? "text-violet-300"
                  : "text-white/60"
              }
            >
              {log}
            </div>
          ))
        )}
        <div ref={logEndRef} />
      </div>

      {/* Result Summary */}
      {result && (
        <div
          className={`rounded-xl border p-3 space-y-1 ${
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
            <span className="text-white/60">METHOD</span>
            <span className="text-white">{result.method.toUpperCase()}</span>
          </div>
          {result.notes.length > 0 && (
            <ul className="text-white/50 space-y-0.5 pt-1 list-disc list-inside">
              {result.notes.map((note, i) => (
                <li key={i}>{note}</li>
              ))}
            </ul>
          )}
        </div>
      )}

      {/* Action Buttons */}
      <div className="flex gap-3">
        <button
          onClick={handleDetect}
          disabled={status === "detecting" || status === "running"}
          className="flex-1 py-3 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl font-bold transition-all disabled:opacity-40"
        >
          {status === "detecting" ? "DETECTING…" : "DETECT DEVICE"}
        </button>
        <button
          onClick={handleRun}
          disabled={!device || status === "running" || status === "detecting"}
          className="flex-1 py-3 bg-violet-700 hover:bg-violet-600 disabled:opacity-40 rounded-xl font-bold transition-all shadow-lg shadow-violet-900/40"
        >
          {status === "running" ? "BYPASSING…" : "RUN BYPASS"}
        </button>
      </div>
    </div>
  );
}
