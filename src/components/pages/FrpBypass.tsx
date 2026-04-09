import { useCallback, useEffect, useMemo, useState, type CSSProperties } from "react";
import { invoke } from "@tauri-apps/api/core";
import { listen, type UnlistenFn } from "@tauri-apps/api/event";
import {
  AlertTriangle,
  HardDriveDownload,
  Play,
  Search,
  ShieldAlert,
  ShieldCheck,
  Sparkles,
} from "lucide-react";
import { AnimatePresence, motion } from "framer-motion";
import Terminal from "../Terminal";
import { Card } from "../ui/Card";
import { LiquidButton } from "@/components/ui/liquid-glass-button";
import { ShineBorder } from "@/components/ui/shine-border";
import { cn } from "@/lib/utils";
import { EDL_COMMANDS, type EdlDeviceInfo } from "../../lib/edl";
import { getPlatform } from "../../lib/platform";
import { UNISOC_COMMANDS, type UnisocDeviceInfo } from "../../lib/unisoc";
import {
  FRP_BRAND_SELECTOR,
  formatChipsetFamilyLabel,
  useDeviceDB,
  type ChipsetFamily,
  type DeviceRecord,
  type FrpBrandSelection,
  type ProgrammingFile,
} from "../../hooks/useDeviceDB";

type PageStatus = "idle" | "running" | "success" | "error";
type RiskLevel = "LOW" | "MEDIUM" | "HIGH" | "EXTREME";
type CommandName =
  | "hydra_samsung_frp_bypass"
  | "mtk_device_info"
  | "mtk_erase_partition"
  | typeof EDL_COMMANDS.GET_GPT
  | typeof EDL_COMMANDS.ERASE_PARTITION
  | typeof UNISOC_COMMANDS.DETECT;

type FrpMethod = {
  id: string;
  title: string;
  description: string;
  successRate: string;
  requirements: string[];
  riskLevel: RiskLevel;
  partitions: string;
  routeLabel: string;
  availability: string;
  supported: boolean;
  destructive: boolean;
  command: CommandName | null;
  commandArgs?: Record<string, string>;
};

type PendingRun = {
  method: FrpMethod;
  sessionId: string;
};

type MethodFilter = "all" | "supported" | "destructive" | "safe" | "blocked";
type MethodExecutionState = "idle" | "queued" | "running" | "success" | "error";

type ChipsetOption = {
  id: ChipsetFamily;
  label: string;
  transport: string;
};

const CHIPSET_OPTIONS: readonly ChipsetOption[] = [
  { id: "qualcomm", label: "Qualcomm", transport: "EDL / Firehose" },
  { id: "mediatek", label: "MediaTek", transport: "BROM / DA" },
  { id: "unisoc", label: "Unisoc", transport: "Research Download" },
  { id: "exynos", label: "Exynos", transport: "Download Mode" },
] as const;

// [INFERRED] Only command routes physically registered in the current Rust backend are advertised here.
const BACKEND_ROUTE_AUDIT = [
  {
    label: "Samsung vendor route",
    commands: ["hydra_samsung_frp_bypass", "hydra_detect_protocol"],
  },
  {
    label: "Qualcomm generic route",
    commands: [
      EDL_COMMANDS.DETECT,
      EDL_COMMANDS.GET_GPT,
      EDL_COMMANDS.ERASE_PARTITION,
      EDL_COMMANDS.READ_PARTITION,
      EDL_COMMANDS.WRITE_PARTITION,
      EDL_COMMANDS.REBOOT,
    ],
  },
  {
    label: "MediaTek generic route",
    commands: [
      "mtk_device_info",
      "mtk_erase_partition",
      "mtk_unlock_bootloader",
      "mtk_read_partition",
      "mtk_write_partition",
      "mtk_run_command",
    ],
  },
  {
    label: "Unisoc read-only route",
    commands: [UNISOC_COMMANDS.DETECT],
  },
  {
    label: "Missing FRP routes",
    commands: [
      "No Unisoc auth / FRP erase command exposed in current build",
      "No generic Exynos FRP route outside Samsung Hydra",
    ],
  },
] as const;

const METHOD_ACCENTS: Record<RiskLevel, string[]> = {
  LOW: ["#34d399", "#22d3ee", "#60a5fa"],
  MEDIUM: ["#f59e0b", "#f97316", "#22d3ee"],
  HIGH: ["#fb7185", "#f97316", "#f59e0b"],
  EXTREME: ["#f43f5e", "#ef4444", "#f97316"],
};

const METHOD_RISK_BADGES: Record<RiskLevel, string> = {
  LOW: "border-emerald-400/25 bg-emerald-400/10 text-emerald-200",
  MEDIUM: "border-amber-400/25 bg-amber-400/10 text-amber-200",
  HIGH: "border-orange-400/25 bg-orange-400/10 text-orange-200",
  EXTREME: "border-rose-400/25 bg-rose-400/10 text-rose-200",
};

const clampStyle = (lines: number): CSSProperties => ({
  display: "-webkit-box",
  WebkitBoxOrient: "vertical",
  WebkitLineClamp: lines,
  overflow: "hidden",
});

type CompactMethodCardProps = {
  method: FrpMethod;
  isActive: boolean;
  acknowledged: boolean;
  executionState: MethodExecutionState;
  onSelect: () => void;
  onRun: () => void;
};

const METHOD_FILTER_CHIPS: Array<{ value: MethodFilter; label: string }> = [
  { value: "all", label: "All" },
  { value: "supported", label: "Ready" },
  { value: "destructive", label: "Erase" },
  { value: "safe", label: "Safe" },
  { value: "blocked", label: "Blocked" },
];

function MethodsStatsBar({
  total,
  filtered,
  supported,
  destructive,
  programmingFiles,
}: {
  total: number;
  filtered: number;
  supported: number;
  destructive: number;
  programmingFiles: number;
}) {
  return (
    <div className="grid grid-cols-2 gap-2 xl:grid-cols-4">
      <div className="rounded-2xl border border-emerald-400/15 bg-emerald-400/8 px-3 py-2 text-emerald-100">
        <div className="flex items-center gap-1.5 text-[10px] font-semibold uppercase tracking-[0.18em] text-emerald-200/80">
          <ShieldCheck className="h-3.5 w-3.5" />
          Ready
        </div>
        <div className="mt-1 text-lg font-black leading-none">{supported}</div>
      </div>

      <div className="rounded-2xl border border-orange-400/15 bg-orange-400/8 px-3 py-2 text-orange-100">
        <div className="flex items-center gap-1.5 text-[10px] font-semibold uppercase tracking-[0.18em] text-orange-200/80">
          <AlertTriangle className="h-3.5 w-3.5" />
          Erase
        </div>
        <div className="mt-1 text-lg font-black leading-none">{destructive}</div>
      </div>

      <div className="rounded-2xl border border-cyan-400/15 bg-cyan-400/8 px-3 py-2 text-cyan-100">
        <div className="flex items-center gap-1.5 text-[10px] font-semibold uppercase tracking-[0.18em] text-cyan-200/80">
          <HardDriveDownload className="h-3.5 w-3.5" />
          Assets
        </div>
        <div className="mt-1 text-lg font-black leading-none">{programmingFiles}</div>
      </div>

      <div className="rounded-2xl border border-purple-400/15 bg-purple-400/8 px-3 py-2 text-purple-100">
        <div className="flex items-center gap-1.5 text-[10px] font-semibold uppercase tracking-[0.18em] text-purple-200/80">
          <Sparkles className="h-3.5 w-3.5" />
          Showing
        </div>
        <div className="mt-1 text-lg font-black leading-none">{filtered}/{total}</div>
      </div>
    </div>
  );
}

function CompactMethodCard({
  method,
  isActive,
  acknowledged,
  executionState,
  onSelect,
  onRun,
}: CompactMethodCardProps) {
  const SupportIcon = method.supported ? ShieldCheck : ShieldAlert;
  const runDisabled =
    executionState === "running" || !method.supported || (method.destructive && !acknowledged);
  const actionLabel =
    executionState === "queued"
      ? "Queued"
      : executionState === "running"
        ? "Running"
        : executionState === "success"
          ? "Done"
          : executionState === "error"
            ? "Retry"
            : method.supported
              ? "Run"
              : "Locked";
  const showSpinner = executionState === "running";

  return (
    <motion.div
      layout
      initial={{ opacity: 0, scale: 0.97 }}
      animate={{ opacity: 1, scale: 1 }}
      exit={{ opacity: 0, scale: 0.96 }}
      transition={{ duration: 0.18 }}
      className="h-full"
    >
      <ShineBorder
        borderRadius={20}
        borderWidth={isActive || executionState === "running" ? 2 : 1}
        duration={executionState === "running" ? 4 : 9}
        color={METHOD_ACCENTS[method.riskLevel]}
        className={cn(
          "h-full rounded-[20px] bg-transparent",
          isActive && "shadow-[0_0_0_1px_rgba(56,189,248,0.35)]",
        )}
      >
        <div
          className={cn(
            "relative flex h-full min-h-[220px] flex-col overflow-hidden rounded-[20px] border border-white/10 bg-[radial-gradient(circle_at_top,rgba(59,130,246,0.16),transparent_42%),linear-gradient(180deg,rgba(7,11,24,0.98),rgba(4,7,17,0.9))] p-3 text-white backdrop-blur-xl transition-transform duration-300",
            isActive && "border-sky-400/30",
            !method.supported && "opacity-75",
          )}
        >
          {showSpinner && (
            <div className="pointer-events-none absolute inset-0 bg-gradient-to-r from-transparent via-cyan-300/10 to-transparent animate-[shimmer_1.2s_infinite]" />
          )}

          <div className="flex items-start justify-between gap-2">
            <div className="min-w-0 flex-1">
              <p className="truncate text-[10px] font-semibold uppercase tracking-[0.22em] text-white/45">
                {method.supported ? "Ready Route" : "Research Route"}
              </p>
              <h4 className="mt-1 text-sm font-semibold leading-tight text-white">
                {method.title}
              </h4>
            </div>
            <span
              className={cn(
                "shrink-0 rounded-full border px-2 py-1 text-[10px] font-semibold tracking-[0.18em]",
                METHOD_RISK_BADGES[method.riskLevel],
              )}
            >
              {method.riskLevel}
            </span>
          </div>

          <p className="mt-2 text-[11px] leading-5 text-slate-300/85" style={clampStyle(3)}>
            {method.description}
          </p>

          <div className="mt-3 flex flex-wrap gap-1.5">
            <span className="rounded-full border border-cyan-400/20 bg-cyan-400/10 px-2 py-1 text-[10px] font-medium uppercase tracking-[0.18em] text-cyan-100">
              {method.successRate}
            </span>
            <span className="rounded-full border border-white/10 bg-white/5 px-2 py-1 text-[10px] font-medium uppercase tracking-[0.18em] text-white/70">
              {method.requirements.length} req
            </span>
            <span
              className={cn(
                "rounded-full border px-2 py-1 text-[10px] font-medium uppercase tracking-[0.18em]",
                method.destructive
                  ? "border-orange-400/25 bg-orange-400/10 text-orange-200"
                  : "border-emerald-400/25 bg-emerald-400/10 text-emerald-200",
              )}
            >
              {method.destructive ? "Erase" : "Safe"}
            </span>
            {executionState !== "idle" && (
              <span className="rounded-full border border-sky-400/25 bg-sky-400/10 px-2 py-1 text-[10px] font-medium uppercase tracking-[0.18em] text-sky-100">
                {actionLabel}
              </span>
            )}
          </div>

          <div className="mt-3 rounded-2xl border border-white/10 bg-white/5 px-3 py-2.5">
            <div className="flex items-center gap-1.5 text-[10px] font-semibold uppercase tracking-[0.18em] text-cyan-200">
              <HardDriveDownload className="h-3 w-3" />
              <span>Backend Route</span>
            </div>
            <p className="mt-1 text-[11px] leading-4 text-white/65" style={clampStyle(2)}>
              {method.routeLabel}
            </p>
          </div>

          <div className="mt-3 flex items-center gap-2 rounded-2xl border border-white/8 bg-white/5 px-3 py-2 text-[10px] font-medium uppercase tracking-[0.18em] text-white/70">
            <SupportIcon className="h-3.5 w-3.5 shrink-0" />
            <span className="truncate">
              {method.supported ? method.availability : `Blocked • ${method.availability}`}
            </span>
          </div>

          <div className="mt-auto grid grid-cols-2 gap-2 pt-3">
            <button
              className={cn(
                "rounded-xl border px-2 py-2 text-[11px] font-medium tracking-wide text-white transition-all duration-200",
                isActive
                  ? "border-sky-400/40 bg-sky-400/15 text-sky-100"
                  : "border-white/10 bg-white/5 hover:border-white/25 hover:bg-white/10",
              )}
              onClick={onSelect}
              type="button"
            >
              Select
            </button>
            <LiquidButton
              className={cn(
                "w-full !rounded-xl text-[11px] font-semibold uppercase tracking-[0.18em] text-white",
                method.destructive ? "text-orange-100" : "text-cyan-50",
              )}
              disabled={runDisabled}
              onClick={onRun}
              size="sm"
            >
              {showSpinner ? (
                <span className="h-3.5 w-3.5 animate-spin rounded-full border border-current border-t-transparent" />
              ) : (
                <Play className="h-3.5 w-3.5" />
              )}
              <span>{actionLabel}</span>
            </LiquidButton>
          </div>
        </div>
      </ShineBorder>
    </motion.div>
  );
}

function createSessionId(): string {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return crypto.randomUUID();
  }
  return `session-${Date.now()}`;
}

function getTimestamp(): string {
  return new Date().toLocaleTimeString("en-IN", { hour12: false });
}

function getManualChipsetLabel(chipsetFamily: ChipsetFamily): string {
  switch (chipsetFamily) {
    case "qualcomm":
      return "Qualcomm";
    case "mediatek":
      return "MediaTek";
    case "exynos":
      return "Exynos";
    case "unisoc":
      return "Unisoc";
    default:
      return "Unknown";
  }
}

function formatProgrammingAvailability(files: ProgrammingFile[]): string {
  if (files.length === 0) {
    return "No indexed firehose/scatter/DA asset matched the current selection.";
  }

  return `${files.length} indexed programming file${files.length === 1 ? "" : "s"} available.`;
}

function formatUnisocProbeOutput(info: UnisocDeviceInfo): string {
  const usbAddress = `${info.vendorId || "n/a"}:${info.productId || "n/a"}`;
  const lines = [
    `[unisoc] detected=${info.detected}`,
    `[unisoc] transport=${info.transport || "Research Download"}`,
    `[unisoc] mode=${info.mode || "unknown"}`,
    `[unisoc] usb=${usbAddress}`,
    `[unisoc] guidance=${info.serviceGuidance}`,
  ];

  if (info.productName) {
    lines.push(`[unisoc] product=${info.productName}`);
  }

  if (info.locationId) {
    lines.push(`[unisoc] location=${info.locationId}`);
  }

  return lines.join("\n");
}

export default function FrpBypassPage() {
  const platform = getPlatform();
  const desktopExecutionEnabled = platform !== "ios";

  const {
    databaseVersion,
    totalModels,
    lastUpdated,
    otherBrands,
    getBrandModels,
    searchModels,
    resolveModel,
    getProgrammingFiles,
  } = useDeviceDB();

  const [status, setStatus] = useState<PageStatus>("idle");
  const [acknowledged, setAcknowledged] = useState(false);
  const [brand, setBrand] = useState<FrpBrandSelection>("Samsung");
  const [chipsetFamily, setChipsetFamily] = useState<ChipsetFamily>("qualcomm");
  const [modelQuery, setModelQuery] = useState("");
  const [selectedModelKey, setSelectedModelKey] = useState<string | null>(null);
  const [activeMethodId, setActiveMethodId] = useState<string | null>(null);
  const [detectedTransport, setDetectedTransport] = useState("Awaiting auto-detect");
  const [detecting, setDetecting] = useState(false);
  const [pendingRun, setPendingRun] = useState<PendingRun | null>(null);
  const [methodSearchQuery, setMethodSearchQuery] = useState("");
  const [methodFilter, setMethodFilter] = useState<MethodFilter>("all");
  const [methodGridCols, setMethodGridCols] = useState<3 | 4>(3);
  const [methodExecutionStates, setMethodExecutionStates] = useState<Record<string, MethodExecutionState>>({});
  const [logs, setLogs] = useState<string[]>([
    "[info] Multi-brand FRP hub initialized",
    `[info] Loaded device DB v${databaseVersion} (${totalModels.toLocaleString()} models)` ,
  ]);

  const setMethodExecutionState = useCallback((methodId: string, nextState: MethodExecutionState) => {
    setMethodExecutionStates((previous) => ({
      ...previous,
      [methodId]: nextState,
    }));
  }, []);

  const addLog = useCallback((line: string) => {
    setLogs((previous) => [...previous, `[${getTimestamp()}] ${line}`]);
  }, []);

  const appendCommandOutput = useCallback((output: string) => {
    const lines = output
      .split(/\r?\n/)
      .map((line) => line.trimEnd())
      .filter((line) => line.length > 0);

    if (lines.length === 0) {
      return;
    }

    setLogs((previous) => [...previous, ...lines]);
  }, []);

  // [INFERRED] Unisoc integration is intentionally read-only: the page may detect Research Download transport and surface service guidance, but it does not expose auth or erase flows.
  const appendUnisocProbe = useCallback(
    (info: UnisocDeviceInfo) => {
      appendCommandOutput(formatUnisocProbeOutput(info));
      if (info.detected) {
        setDetectedTransport(`${info.productName} detected via ${info.transport}`);
        setChipsetFamily("unisoc");
        addLog(`[info] Unisoc transport detected: ${info.productName} (${info.vendorId}:${info.productId})`);
      } else {
        addLog(`[info] ${info.serviceGuidance}`);
      }
    },
    [addLog, appendCommandOutput],
  );

  // [INFERRED] MTK client commands already emit streamed events from the Rust backend, so the frontend subscribes when available.
  useEffect(() => {
    let disposed = false;
    const unlisteners: UnlistenFn[] = [];

    const registerListeners = async () => {
      try {
        unlisteners.push(
          await listen<string>("mtk://stdout", (event) => {
            if (!disposed) {
              appendCommandOutput(`[mtk] ${event.payload}`);
            }
          }),
        );
        unlisteners.push(
          await listen<string>("mtk://stderr", (event) => {
            if (!disposed) {
              appendCommandOutput(`[mtk stderr] ${event.payload}`);
            }
          }),
        );
        unlisteners.push(
          await listen<string>("mtk://error", (event) => {
            if (!disposed) {
              addLog(`[error] ${event.payload}`);
            }
          }),
        );
        unlisteners.push(
          await listen<number>("mtk://exit", (event) => {
            if (!disposed) {
              addLog(`[info] MTK session exited with code ${event.payload}`);
            }
          }),
        );
      } catch {
        // Ignore listener bootstrap failures outside a Tauri event context.
      }
    };

    void registerListeners();

    return () => {
      disposed = true;
      for (const unlisten of unlisteners) {
        unlisten();
      }
    };
  }, [addLog, appendCommandOutput]);

  useEffect(() => {
    let disposed = false;
    let unlisten: UnlistenFn | null = null;

    const registerBypassProgress = async () => {
      try {
        unlisten = await listen<{ id?: string; status?: string; detail?: string }>(
          "bypass-progress",
          (event) => {
            if (disposed) {
              return;
            }

            const payload = event.payload;
            if (payload.id && payload.status) {
              const mappedState: MethodExecutionState =
                payload.status === "running"
                  ? "running"
                  : payload.status === "success"
                    ? "success"
                    : payload.status === "queued"
                      ? "queued"
                      : "error";
              setMethodExecutionState(payload.id, mappedState);
            }

            if (payload.id || payload.detail) {
              addLog(`[bypass] ${payload.id ?? "route"} ${payload.status ?? "state"}${payload.detail ? ` • ${payload.detail}` : ""}`);
            }
          },
        );
      } catch {
        // Ignore listener bootstrap failures outside a Tauri event context.
      }
    };

    void registerBypassProgress();

    return () => {
      disposed = true;
      unlisten?.();
    };
  }, [addLog, setMethodExecutionState]);

  const brandModels = useMemo(() => getBrandModels(brand), [brand, getBrandModels]);

  const selectedModel = useMemo(
    () => brandModels.find((record) => record.key === selectedModelKey) ?? null,
    [brandModels, selectedModelKey],
  );

  const searchResults = useMemo(
    () => searchModels(modelQuery, brand, modelQuery.trim() ? 8 : 6),
    [brand, modelQuery, searchModels],
  );

  const effectiveBrand = selectedModel?.brand ?? (brand === "Others" ? "Other brands" : brand);
  const selectedModelLabel = selectedModel?.name ?? (modelQuery.trim() || "Manual selection");
  const selectedChipsetLabel = selectedModel?.chipset ?? getManualChipsetLabel(chipsetFamily);

  const programmingFiles = useMemo(
    () =>
      getProgrammingFiles({
        brand: effectiveBrand,
        model: selectedModelLabel,
        chipset: selectedChipsetLabel,
        chipsetFamily: selectedModel?.chipsetFamily ?? chipsetFamily,
      }),
    [chipsetFamily, effectiveBrand, getProgrammingFiles, selectedChipsetLabel, selectedModel?.chipsetFamily, selectedModelLabel],
  );

  // [INFERRED] Routing is constrained to commands confirmed to exist in the current Rust command registry.
  const methods = useMemo<FrpMethod[]>(() => {
    const hasFirehose = programmingFiles.some((file) => file.file_type === "firehose");
    const hasDa = programmingFiles.some((file) => file.file_type === "da");
    const firehoseRequirement = hasFirehose
      ? "Indexed firehose asset is available for this chipset."
      : "No bundled firehose asset matched this chipset; external programmer tooling may be required.";
    const daRequirement = hasDa
      ? "Bundled MTK DA assets are indexed for this route."
      : "No bundled MTK DA asset matched this selection.";

    if (effectiveBrand === "Samsung" && (chipsetFamily === "qualcomm" || chipsetFamily === "exynos")) {
      return [
        {
          id: "samsung-adb-sideload",
          title: "ADB Sideload Method",
          description: "Runs the existing Hydra Samsung recovery flow using the adb_sideload route.",
          successRate: "Hydra CLI-backed vendor workflow",
          requirements: [
            "Samsung recovery with ADB sideload available",
            "Vendor FRP payload present on host",
          ],
          riskLevel: "HIGH",
          partitions: "userdata, cache, FRP-related Samsung recovery payload targets",
          routeLabel: "hydra_samsung_frp_bypass → adb_sideload",
          availability: "Registered Rust command in current backend.",
          supported: desktopExecutionEnabled,
          destructive: true,
          command: "hydra_samsung_frp_bypass",
          commandArgs: { method: "adb_sideload" },
        },
        {
          id: "samsung-odin-flash",
          title: "ODIN Flash Method",
          description: "Runs the existing Hydra Samsung Download Mode flow using the odin_flash route.",
          successRate: "Hydra CLI-backed vendor workflow",
          requirements: [
            "Samsung Download Mode / ODIN connectivity",
            "Vendor FRP package approved for the target device",
          ],
          riskLevel: "HIGH",
          partitions: "userdata, cache, vendor FRP/reset package targets",
          routeLabel: "hydra_samsung_frp_bypass → odin_flash",
          availability: "Registered Rust command in current backend.",
          supported: desktopExecutionEnabled,
          destructive: true,
          command: "hydra_samsung_frp_bypass",
          commandArgs: { method: "odin_flash" },
        },
      ];
    }

    if (chipsetFamily === "qualcomm") {
      return [
        {
          id: "qualcomm-gpt-probe",
          title: `${effectiveBrand} GPT Probe`,
          description: "Read the GPT through the existing EDL route before any FRP write or erase action.",
          successRate: "Read-only transport validation",
          requirements: ["Device in Qualcomm EDL 9008", firehoseRequirement],
          riskLevel: "LOW",
          partitions: "none (read-only)",
          routeLabel: EDL_COMMANDS.GET_GPT,
          availability: firehoseRequirement,
          supported: desktopExecutionEnabled,
          destructive: false,
          command: EDL_COMMANDS.GET_GPT,
        },
        {
          id: "qualcomm-frp-erase",
          title: `${effectiveBrand} FRP Partition Erase`,
          description: "Uses the existing edl_erase_partition command against the FRP partition after GPT validation.",
          successRate: "Partition-layout dependent",
          requirements: [
            "Device in Qualcomm EDL 9008",
            "Confirm that the GPT exposes an frp partition",
            firehoseRequirement,
          ],
          riskLevel: "HIGH",
          partitions: "frp",
          routeLabel: `${EDL_COMMANDS.ERASE_PARTITION}(partition=frp)`,
          availability: "Executable with current Qualcomm EDL command set.",
          supported: desktopExecutionEnabled,
          destructive: true,
          command: EDL_COMMANDS.ERASE_PARTITION,
          commandArgs: { partition: "frp" },
        },
      ];
    }

    if (chipsetFamily === "mediatek") {
      return [
        {
          id: "mtk-gpt-probe",
          title: `${effectiveBrand} BROM / DA Probe`,
          description: "Reads MediaTek partition information through the existing mtk_device_info route.",
          successRate: "Read-only transport validation",
          requirements: ["Device in MediaTek BROM / DA mode", daRequirement],
          riskLevel: "LOW",
          partitions: "none (read-only)",
          routeLabel: "mtk_device_info",
          availability: daRequirement,
          supported: desktopExecutionEnabled,
          destructive: false,
          command: "mtk_device_info",
        },
        {
          id: "mtk-frp-erase",
          title: `${effectiveBrand} FRP Partition Erase`,
          description: "Uses the existing mtk_erase_partition route to erase the FRP partition.",
          successRate: "Partition-layout dependent",
          requirements: [
            "Device in MediaTek BROM / DA mode",
            "Confirm that the target exposes an frp partition",
            daRequirement,
          ],
          riskLevel: "HIGH",
          partitions: "frp",
          routeLabel: "mtk_erase_partition(partition=frp)",
          availability: "Executable with current MediaTek command set.",
          supported: desktopExecutionEnabled,
          destructive: true,
          command: "mtk_erase_partition",
          commandArgs: { partition: "frp" },
        },
      ];
    }

    if (chipsetFamily === "unisoc") {
      return [
        {
          id: "unisoc-rdm-probe",
          title: "Research Download Probe",
          description: "Performs read-only host-side detection for Unisoc / Spreadtrum Research Download transport and returns service guidance.",
          successRate: "Read-only transport validation",
          requirements: [
            "Desktop Tauri runtime available",
            "macOS host USB enumeration",
            "Device connected in Unisoc Research Download mode (VID 0x1782 / PID 0x4D00)",
          ],
          riskLevel: "LOW",
          partitions: "none (read-only)",
          routeLabel: UNISOC_COMMANDS.DETECT,
          availability: "Registered Rust command for safe detection only.",
          supported: desktopExecutionEnabled,
          destructive: false,
          command: UNISOC_COMMANDS.DETECT,
        },
      ];
    }

    return [
      {
        id: "exynos-unavailable",
        title: "Exynos Route",
        description:
          effectiveBrand === "Samsung"
            ? "Select Samsung with Qualcomm or Exynos to use the available Hydra vendor route."
            : "Current Rust backend does not expose a generic Exynos FRP command outside Samsung Hydra.",
        successRate: "Unavailable in current build",
        requirements: ["Use a supported Samsung Hydra route or add a dedicated Exynos backend command"],
        riskLevel: "MEDIUM",
        partitions: "n/a",
        routeLabel: "No registered generic Exynos command",
        availability: "Unsupported until a dedicated Exynos backend route is added.",
        supported: false,
        destructive: false,
        command: null,
      },
    ];
  }, [chipsetFamily, desktopExecutionEnabled, effectiveBrand, programmingFiles]);

  const activeMethod = useMemo(
    () => methods.find((method) => method.id === activeMethodId) ?? null,
    [activeMethodId, methods],
  );

  const filteredMethods = useMemo(() => {
    const normalizedQuery = methodSearchQuery.trim().toLowerCase();

    return methods.filter((method) => {
      const matchesQuery =
        normalizedQuery.length === 0 ||
        method.title.toLowerCase().includes(normalizedQuery) ||
        method.description.toLowerCase().includes(normalizedQuery) ||
        method.routeLabel.toLowerCase().includes(normalizedQuery) ||
        method.requirements.some((requirement) => requirement.toLowerCase().includes(normalizedQuery));

      const matchesFilter =
        methodFilter === "all"
          ? true
          : methodFilter === "supported"
            ? method.supported
            : methodFilter === "destructive"
              ? method.destructive
              : methodFilter === "safe"
                ? !method.destructive
                : !method.supported;

      return matchesQuery && matchesFilter;
    });
  }, [methodFilter, methodSearchQuery, methods]);

  const methodStats = useMemo(
    () => ({
      supported: methods.filter((method) => method.supported).length,
      destructive: methods.filter((method) => method.destructive).length,
      total: methods.length,
      filtered: filteredMethods.length,
    }),
    [filteredMethods.length, methods],
  );

  useEffect(() => {
    const fallbackMethod = methods.find((method) => method.supported) ?? methods[0] ?? null;
    if (fallbackMethod && !methods.some((method) => method.id === activeMethodId)) {
      setActiveMethodId(fallbackMethod.id);
    }
    if (!fallbackMethod && activeMethodId !== null) {
      setActiveMethodId(null);
    }
  }, [activeMethodId, methods]);

  useEffect(() => {
    setSelectedModelKey(null);
    setModelQuery("");
    setActiveMethodId(null);
  }, [brand]);

  const output = useMemo(() => logs.join("\n"), [logs]);

  const selectModel = useCallback(
    (record: DeviceRecord) => {
      setSelectedModelKey(record.key);
      setModelQuery(record.name);
      if (record.chipsetFamily !== "unknown") {
        setChipsetFamily(record.chipsetFamily);
      }
      addLog(`[info] Model selected from DB: ${record.brand} ${record.name} (${record.chipset})`);
    },
    [addLog],
  );

  const searchFromDb = useCallback(() => {
    if (!modelQuery.trim()) {
      addLog("[warn] Enter a model or codename before searching the DB.");
      return;
    }

    const match = resolveModel(modelQuery, brand);
    if (!match) {
      addLog(`[warn] No DB match found for \"${modelQuery.trim()}\" under ${brand}.`);
      return;
    }

    selectModel(match);
  }, [addLog, brand, modelQuery, resolveModel, selectModel]);

  // [INFERRED] The current backend exposes live probe commands for Qualcomm EDL, MediaTek, and a read-only Unisoc transport detector.
  const autoDetectTransport = useCallback(async () => {
    if (!desktopExecutionEnabled) {
      addLog("[warn] Desktop Tauri runtime is required for FRP transport probes.");
      return;
    }

    setDetecting(true);
    addLog("[info] Probing Qualcomm EDL route...");

    try {
      const edlInfo = await invoke<EdlDeviceInfo>(EDL_COMMANDS.DETECT);
      if (edlInfo.detected) {
        setDetectedTransport(`${edlInfo.chipset} detected via ${edlInfo.mode}`);
        setChipsetFamily("qualcomm");
        addLog(`[info] Qualcomm transport detected: ${edlInfo.chipset} (${edlInfo.serial || "no serial"})`);
        setDetecting(false);
        return;
      }
      addLog("[info] No Qualcomm EDL device detected.");
    } catch (error: unknown) {
      addLog(`[warn] Qualcomm probe failed: ${String(error)}`);
    }

    addLog("[info] Probing MediaTek BROM / DA route...");

    try {
      const info = await invoke<string>("mtk_device_info");
      if (info.trim()) {
        setDetectedTransport("MediaTek BROM / DA transport detected");
        setChipsetFamily("mediatek");
        addLog("[info] MediaTek transport detected via mtk_device_info.");
        appendCommandOutput(info);
        setDetecting(false);
        return;
      }
    } catch (error: unknown) {
      addLog(`[warn] MediaTek probe failed: ${String(error)}`);
    }

    addLog("[info] Probing Unisoc Research Download route...");

    try {
      const info = await invoke<UnisocDeviceInfo>(UNISOC_COMMANDS.DETECT);
      appendUnisocProbe(info);
      if (info.detected) {
        setDetecting(false);
        return;
      }
      addLog("[info] No Unisoc Research Download device detected.");
    } catch (error: unknown) {
      addLog(`[warn] Unisoc probe failed: ${String(error)}`);
    }

    setDetectedTransport("No live Qualcomm EDL, MediaTek BROM, or Unisoc RDM route detected");
    addLog("[warn] No live Qualcomm EDL, MediaTek BROM, or Unisoc Research Download transport was detected. Exynos must be selected manually in the current build.");
    setDetecting(false);
  }, [addLog, appendCommandOutput, appendUnisocProbe, desktopExecutionEnabled]);

  const executeMethod = useCallback(
    async (method: FrpMethod, sessionId: string) => {
      if (!method.command) {
        addLog(`[warn] ${method.title} is not executable in the current build.`);
        setMethodExecutionState(method.id, "error");
        return;
      }

      setStatus("running");
      setMethodExecutionState(method.id, "running");
      addLog(`[session] [SESSION_START] op=${method.title} sessionId=${sessionId} ts=${Date.now()}`);
      addLog(`[route] ${method.routeLabel}`);
      if (method.commandArgs) {
        addLog(`[args] ${JSON.stringify(method.commandArgs)}`);
      }

      try {
        if (method.command === UNISOC_COMMANDS.DETECT) {
          const info = await invoke<UnisocDeviceInfo>(method.command, method.commandArgs);
          appendUnisocProbe(info);
        } else {
          const result = await invoke<string>(method.command, method.commandArgs);
          if (result.trim()) {
            appendCommandOutput(result);
          }
        }
        addLog(`[session] [SESSION_END] sessionId=${sessionId} result=success`);
        setMethodExecutionState(method.id, "success");
        setStatus("success");
      } catch (error: unknown) {
        addLog(`[error] ${String(error)}`);
        addLog(`[session] [SESSION_END] sessionId=${sessionId} result=error`);
        setMethodExecutionState(method.id, "error");
        setStatus("error");
      }
    },
    [addLog, appendCommandOutput, appendUnisocProbe, setMethodExecutionState],
  );

  const queueMethod = useCallback(
    (method: FrpMethod) => {
      setActiveMethodId(method.id);

      if (!desktopExecutionEnabled) {
        addLog("[warn] Desktop Tauri runtime is required to execute FRP operations.");
        return;
      }

      if (!method.supported || !method.command) {
        addLog(`[warn] ${method.availability}`);
        return;
      }

      if (method.destructive && !acknowledged) {
        addLog("[warn] Acknowledge the safety gate before launching a destructive FRP method.");
        return;
      }

      const sessionId = createSessionId();
      if (method.destructive) {
        setMethodExecutionState(method.id, "queued");
        setPendingRun({ method, sessionId });
        return;
      }

      void executeMethod(method, sessionId);
    },
    [acknowledged, addLog, desktopExecutionEnabled, executeMethod, setMethodExecutionState],
  );

  const confirmPendingRun = useCallback(() => {
    if (!pendingRun) {
      return;
    }

    const { method, sessionId } = pendingRun;
    setPendingRun(null);
    void executeMethod(method, sessionId);
  }, [executeMethod, pendingRun]);

  const clearConsole = useCallback(() => {
    setLogs(["[info] Operation console cleared"]);
    setMethodExecutionStates({});
    setStatus("idle");
  }, []);

  const activeRiskLevel = activeMethod?.riskLevel ?? "LOW";
  const requiresBackupHint = /efs|modemst1|modemst2|persist|nvram/i.test(activeMethod?.partitions ?? "");

  return (
    <div className="page">
      <div className="row-between" style={{ alignItems: "flex-start", gap: 16 }}>
        <div>
          <h2 className="page-title">FRP Bypass</h2>
          <p className="page-subtitle">
            Multi-brand FRP hub wired to discovered Samsung Hydra, Qualcomm EDL, MediaTek MTK, and read-only Unisoc backend routes.
          </p>
        </div>
        <button
          className="btn btn-secondary btn-sm"
          disabled={!desktopExecutionEnabled || detecting || status === "running"}
          onClick={() => void autoDetectTransport()}
        >
          {detecting ? "Detecting..." : "Auto-detect"}
        </button>
      </div>

      <div className="danger-note">
        HIGH RISK — FRP operations can permanently change reset state, erase the FRP partition, or invoke vendor recovery packages depending on the selected route.
      </div>

      {!desktopExecutionEnabled && (
        <div className="panel">
          Desktop Tauri runtime required — FRP routing relies on Rust commands registered under the desktop backend.
        </div>
      )}

      <Card title="Device Context">
        <div className="device-grid">
          <div className="device-field">
            <span className="device-field-label">Device detected</span>
            <span className="device-field-value">{detectedTransport}</span>
          </div>
          <div className="device-field">
            <span className="device-field-label">Brand</span>
            <span className="device-field-value">{effectiveBrand}</span>
          </div>
          <div className="device-field">
            <span className="device-field-label">Model</span>
            <span className="device-field-value">{selectedModelLabel}</span>
          </div>
          <div className="device-field">
            <span className="device-field-label">Chipset route</span>
            <span className="device-field-value">{formatChipsetFamilyLabel(chipsetFamily)}</span>
          </div>
          <div className="device-field">
            <span className="device-field-label">DB coverage</span>
            <span className="device-field-value">v{databaseVersion} • {totalModels.toLocaleString()} models</span>
          </div>
          <div className="device-field">
            <span className="device-field-label">DB updated</span>
            <span className="device-field-value">{lastUpdated}</span>
          </div>
          <div className="device-field">
            <span className="device-field-label">Programming files</span>
            <span className="device-field-value">{formatProgrammingAvailability(programmingFiles)}</span>
          </div>
        </div>
      </Card>

      <Card title="Backend Coverage">
        <div className="frp-route-list">
          {BACKEND_ROUTE_AUDIT.map((entry) => (
            <div key={entry.label} className="frp-route-row">
              <div>
                <div className="action-title">{entry.label}</div>
                <div className="page-subtitle">Actual commands discovered during backend audit</div>
              </div>
              <div className="frp-meta-list" style={{ maxWidth: 520 }}>
                {entry.commands.map((command) => (
                  <span key={command}>{command}</span>
                ))}
              </div>
            </div>
          ))}
        </div>
      </Card>

      <Card title="Brand Selector">
        <div className="stack-sm">
          <div className="frp-brand-grid">
            {FRP_BRAND_SELECTOR.map((brandOption) => (
              <button
                key={brandOption}
                className={`frp-selector ${brand === brandOption ? "is-active" : ""}`}
                onClick={() => setBrand(brandOption)}
                type="button"
              >
                <span className="frp-selector-label">{brandOption}</span>
                <span className="frp-selector-meta">
                  {brandOption === "Others"
                    ? `${otherBrands.length} additional brands`
                    : `${getBrandModels(brandOption).length} indexed models`}
                </span>
              </button>
            ))}
          </div>
          <div className="panel">
            Others includes: {otherBrands.join(", ")}
          </div>
        </div>
      </Card>

      <Card title="Chipset">
        <div className="frp-chipset-grid">
          {CHIPSET_OPTIONS.map((option) => (
            <button
              key={option.id}
              className={`frp-chipset-option ${chipsetFamily === option.id ? "is-active" : ""}`}
              onClick={() => setChipsetFamily(option.id)}
              type="button"
            >
              <div className="action-title">{option.label}</div>
              <div className="page-subtitle">{option.transport}</div>
            </button>
          ))}
        </div>
      </Card>

      <Card title="Model Lookup">
        <div className="stack-sm">
          <div className="grid-two">
            <div>
              <label className="field-label">Model</label>
              <input
                className="field-input"
                placeholder="Enter model, codename, or chipset"
                value={modelQuery}
                onChange={(event) => {
                  setModelQuery(event.target.value);
                  setSelectedModelKey(null);
                }}
              />
            </div>
            <div>
              <label className="field-label">Selected DB record</label>
              <input className="field-input" value={selectedModel ? `${selectedModel.brand} • ${selectedModel.chipset}` : "No exact DB record selected"} readOnly />
            </div>
          </div>

          <div className="action-row">
            <button className="btn btn-secondary btn-sm" onClick={searchFromDb} type="button">
              Search from DB
            </button>
            <button
              className="btn btn-ghost btn-sm"
              disabled={!desktopExecutionEnabled || detecting || status === "running"}
              onClick={() => void autoDetectTransport()}
              type="button"
            >
              {detecting ? "Detecting..." : "Auto-detect"}
            </button>
          </div>

          <div className="frp-search-list">
            {searchResults.map((record) => (
              <button
                key={record.key}
                className={`frp-search-result ${selectedModel?.key === record.key ? "is-active" : ""}`}
                onClick={() => selectModel(record)}
                type="button"
              >
                <div className="row-between" style={{ alignItems: "flex-start", gap: 12 }}>
                  <div>
                    <div className="action-title">{record.name}</div>
                    <div className="page-subtitle">
                      {record.brand} • {record.chipset}
                      {record.codename ? ` • ${record.codename}` : ""}
                    </div>
                  </div>
                  <span className="frp-risk-badge" data-level="LOW">
                    {formatChipsetFamilyLabel(record.chipsetFamily)}
                  </span>
                </div>
              </button>
            ))}
          </div>
        </div>
      </Card>

      <Card title="Programming Files">
        <div className="stack-sm">
          {programmingFiles.length > 0 ? (
            <div className="frp-programming-list">
              {programmingFiles.map((file) => (
                <div key={`${file.file_type}:${file.file_path}`} className="frp-programming-row">
                  <div className="frp-programming-meta">
                    <span className="action-title">{file.file_type.toUpperCase()}</span>
                    <span className="page-subtitle">{file.brand} • {file.model} • {file.chipset}</span>
                    <span className="frp-programming-path">{file.file_path}</span>
                  </div>
                  <span className="frp-risk-badge" data-level={file.verified ? "LOW" : "MEDIUM"}>
                    {file.verified ? "Verified" : "Unverified"}
                  </span>
                </div>
              ))}
            </div>
          ) : (
            <div className="panel">No compatible programming file is indexed for the current selection. Scatter files were not found in the audited assets.</div>
          )}
        </div>
      </Card>

      <Card title="Safety Gate">
        <div className="stack-sm">
          <div className="panel">Data loss warning: {activeMethod?.partitions ?? "Select a method to view affected partitions."}</div>
          <div className="frp-console-meta">
            <span className="frp-risk-badge" data-level={activeRiskLevel}>{activeRiskLevel} RISK</span>
            <span className="frp-status-chip">Active route: {activeMethod?.routeLabel ?? "No method selected"}</span>
          </div>
          <div className="panel">Recommend backing up EFS before proceeding when a workflow may touch modemst1, modemst2, persist, or nvram.</div>
          {requiresBackupHint && (
            <div className="panel">Backup hint: this route may touch EFS-adjacent partitions. Capture a backup before proceeding.</div>
          )}
          <label className="field-label" style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <input type="checkbox" checked={acknowledged} onChange={(event) => setAcknowledged(event.target.checked)} />
            I understand FRP workflows may erase the partitions shown above and can permanently change device reset state.
          </label>
        </div>
      </Card>

      <Card
        title="Compatible Methods"
        action={
          <div className="flex items-center gap-2">
            <span className="frp-status-chip">{filteredMethods.length} shown</span>
            <button
              className="btn btn-ghost btn-sm"
              onClick={() => setMethodGridCols((previous) => (previous === 3 ? 4 : 3))}
              type="button"
            >
              {methodGridCols}×
            </button>
          </div>
        }
      >
        <div className="stack-sm">
          <MethodsStatsBar
            destructive={methodStats.destructive}
            filtered={methodStats.filtered}
            programmingFiles={programmingFiles.length}
            supported={methodStats.supported}
            total={methodStats.total}
          />

          <div className="flex flex-col gap-2 xl:flex-row xl:items-center">
            <label className="relative block flex-1">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-white/25" />
              <input
                className="field-input !pl-9"
                onChange={(event) => setMethodSearchQuery(event.target.value)}
                placeholder="Search route, description, requirement"
                value={methodSearchQuery}
              />
            </label>

            <div className="flex gap-2 overflow-x-auto pb-1 scrollbar-none">
              {METHOD_FILTER_CHIPS.map((chip) => (
                <button
                  key={chip.value}
                  className={cn(
                    "shrink-0 rounded-full border px-3 py-1.5 text-[10px] font-semibold uppercase tracking-[0.18em] transition-colors",
                    methodFilter === chip.value
                      ? "border-cyan-400/30 bg-cyan-400/12 text-cyan-100"
                      : "border-white/10 bg-white/5 text-white/55 hover:border-white/20 hover:text-white/80",
                  )}
                  onClick={() => setMethodFilter(chip.value)}
                  type="button"
                >
                  {chip.label}
                </button>
              ))}
            </div>
          </div>

          <p className="text-[10px] uppercase tracking-[0.18em] text-white/35">
            {filteredMethods.length} route{filteredMethods.length === 1 ? "" : "s"} match the current filter
          </p>

          {detecting ? (
            <div className={cn(
              "grid gap-3",
              methodGridCols === 4 ? "grid-cols-2 md:grid-cols-3 xl:grid-cols-4" : "grid-cols-2 md:grid-cols-3",
            )}>
              {Array.from({ length: methodGridCols === 4 ? 8 : 6 }).map((_, index) => (
                <div
                  key={`method-skeleton-${index}`}
                  className="min-h-[220px] rounded-[20px] border border-white/10 bg-white/5 animate-pulse"
                />
              ))}
            </div>
          ) : filteredMethods.length === 0 ? (
            <div className="rounded-2xl border border-white/10 bg-white/5 px-4 py-8 text-center text-sm text-white/55">
              No routes match the current method filter.
            </div>
          ) : (
            <motion.div
              layout
              className={cn(
                "grid gap-3",
                methodGridCols === 4 ? "grid-cols-2 md:grid-cols-3 xl:grid-cols-4" : "grid-cols-2 md:grid-cols-3",
              )}
            >
              <AnimatePresence mode="popLayout">
                {filteredMethods.map((method) => (
                  <CompactMethodCard
                    key={method.id}
                    acknowledged={acknowledged}
                    executionState={methodExecutionStates[method.id] ?? "idle"}
                    isActive={activeMethod?.id === method.id}
                    method={method}
                    onRun={() => queueMethod(method)}
                    onSelect={() => setActiveMethodId(method.id)}
                  />
                ))}
              </AnimatePresence>
            </motion.div>
          )}
        </div>
      </Card>

      <Card title="Execute" action={<span className="frp-status-chip">Selected: {activeMethod?.title ?? "None"}</span>}>
        <div className="stack-sm">
          <div className="panel">Method routing is limited to commands audited from the current Rust backend. Unsupported brands and chipsets remain visible but non-executable.</div>
          <LiquidButton
            className={cn(
              "w-full !rounded-2xl text-sm font-semibold uppercase tracking-[0.2em] text-white",
              activeMethod?.destructive ? "text-orange-100" : "text-cyan-50",
            )}
            disabled={
              status === "running" ||
              !activeMethod ||
              !activeMethod.supported ||
              (activeMethod.destructive && !acknowledged)
            }
            onClick={() => activeMethod && queueMethod(activeMethod)}
            size="lg"
          >
            <Play className="h-4 w-4" />
            <span>{activeMethod?.destructive ? "Start Bypass" : "Run Selected Route"}</span>
          </LiquidButton>
        </div>
      </Card>

      <Card title="Operation Console" action={<button className="btn btn-ghost btn-sm" onClick={clearConsole} type="button">Clear</button>}>
        <Terminal output={output} status={status} />
      </Card>

      {pendingRun && (
        <div className="frp-dialog-backdrop">
          <div className="frp-dialog">
            <span className="frp-risk-badge" data-level={pendingRun.method.riskLevel}>{pendingRun.method.riskLevel} RISK</span>
            <div className="frp-dialog-title">Confirm FRP operation</div>
            <div className="frp-dialog-copy">Operation: {pendingRun.method.title}</div>
            <div className="frp-dialog-copy">Affected partitions: {pendingRun.method.partitions}</div>
            <div className="frp-dialog-copy">Backend route: {pendingRun.method.routeLabel}</div>
            <div className="frp-dialog-copy">Session ID: {pendingRun.sessionId}</div>
            <div className="panel">This action cannot be undone once the backend command starts.</div>
            <div className="action-row">
              <button
                className="btn btn-secondary btn-sm"
                onClick={() => {
                  setMethodExecutionState(pendingRun.method.id, "idle");
                  setPendingRun(null);
                }}
                type="button"
              >
                Cancel
              </button>
              <button className="btn btn-danger btn-sm" onClick={confirmPendingRun} type="button">
                Confirm and Run
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
