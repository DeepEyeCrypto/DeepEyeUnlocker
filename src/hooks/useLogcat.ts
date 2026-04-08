import { listen, type UnlistenFn } from "@tauri-apps/api/event";
import { save } from "@tauri-apps/plugin-dialog";
import { useCallback, useEffect, useMemo, useState } from "react";
import {
  clearLogcat,
  exportLogcat,
  startLogcat,
  stopLogcat,
  type LogcatEntry,
  type LogcatError,
  type LogcatStatus,
} from "../lib/logcat";

const MAX_LOGCAT_ENTRIES = 5000;
const LOGCAT_PREFERENCES_KEY = "deepeye.logcat.preferences";

type PersistedLogcatPreferences = {
  levelFilter: string;
  tagFilter: string;
  textFilter: string;
  pidFilter: string;
  autoScroll: boolean;
};

function loadPreferences(): PersistedLogcatPreferences {
  if (typeof window === "undefined") {
    return {
      levelFilter: "ALL",
      tagFilter: "",
      textFilter: "",
      pidFilter: "",
      autoScroll: true,
    };
  }

  try {
    const raw = window.localStorage.getItem(LOGCAT_PREFERENCES_KEY);
    if (!raw) {
      return {
        levelFilter: "ALL",
        tagFilter: "",
        textFilter: "",
        pidFilter: "",
        autoScroll: true,
      };
    }

    const parsed = JSON.parse(raw) as Partial<PersistedLogcatPreferences>;
    return {
      levelFilter: parsed.levelFilter ?? "ALL",
      tagFilter: parsed.tagFilter ?? "",
      textFilter: parsed.textFilter ?? "",
      pidFilter: parsed.pidFilter ?? "",
      autoScroll: parsed.autoScroll ?? true,
    };
  } catch {
    return {
      levelFilter: "ALL",
      tagFilter: "",
      textFilter: "",
      pidFilter: "",
      autoScroll: true,
    };
  }
}

function buildDefaultExportName(serial: string | null): string {
  const timestamp = new Date().toISOString().replace(/:/g, "-").replace(/\..+$/, "");
  return `deepeye-logcat-${serial ?? "device"}-${timestamp}.txt`;
}

export interface UseLogcatReturn {
  entries: LogcatEntry[];
  filteredEntries: LogcatEntry[];
  isRunning: boolean;
  selectedSerial: string | null;
  levelFilter: string;
  tagFilter: string;
  textFilter: string;
  pidFilter: string;
  autoScroll: boolean;
  paused: boolean;
  statusMessage: string;
  errorMessage: string;
  setSelectedSerial: (serial: string | null) => void;
  setLevelFilter: (value: string) => void;
  setTagFilter: (value: string) => void;
  setTextFilter: (value: string) => void;
  setPidFilter: (value: string) => void;
  setAutoScroll: (value: boolean) => void;
  setPaused: (value: boolean) => void;
  start: () => Promise<void>;
  stop: () => Promise<void>;
  clear: () => Promise<void>;
  exportToFile: () => Promise<void>;
  clearEntries: () => void;
}

export function useLogcat(): UseLogcatReturn {
  const preferences = useMemo(loadPreferences, []);
  const [entries, setEntries] = useState<LogcatEntry[]>([]);
  const [isRunning, setIsRunning] = useState(false);
  const [selectedSerial, setSelectedSerial] = useState<string | null>(null);
  const [levelFilter, setLevelFilter] = useState(preferences.levelFilter);
  const [tagFilter, setTagFilter] = useState(preferences.tagFilter);
  const [textFilter, setTextFilter] = useState(preferences.textFilter);
  const [pidFilter, setPidFilter] = useState(preferences.pidFilter);
  const [autoScroll, setAutoScroll] = useState(preferences.autoScroll);
  const [paused, setPaused] = useState(false);
  const [statusMessage, setStatusMessage] = useState("Logcat viewer ready");
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    if (typeof window === "undefined") {
      return;
    }

    window.localStorage.setItem(
      LOGCAT_PREFERENCES_KEY,
      JSON.stringify({
        levelFilter,
        tagFilter,
        textFilter,
        pidFilter,
        autoScroll,
      }),
    );
  }, [autoScroll, levelFilter, pidFilter, tagFilter, textFilter]);

  useEffect(() => {
    let mounted = true;
    let unlistenFns: UnlistenFn[] = [];

    const setupListeners = async () => {
      const listeners = await Promise.all([
        listen<LogcatEntry>("logcat-entry", (event) => {
          setEntries((previous) => {
            const next = [...previous, event.payload];
            return next.length > MAX_LOGCAT_ENTRIES
              ? next.slice(next.length - MAX_LOGCAT_ENTRIES)
              : next;
          });
        }),
        listen<LogcatStatus>("logcat-status", (event) => {
          setIsRunning(event.payload.running);
          setStatusMessage(event.payload.message);
          setErrorMessage("");
        }),
        listen<LogcatError>("logcat-error", (event) => {
          setErrorMessage(event.payload.message);
        }),
      ]);

      if (!mounted) {
        listeners.forEach((unlisten) => unlisten());
        return;
      }

      unlistenFns = listeners;
    };

    void setupListeners();

    return () => {
      mounted = false;
      unlistenFns.forEach((unlisten) => unlisten());
      void stopLogcat().catch(() => undefined);
    };
  }, []);

  const filteredEntries = useMemo(() => {
    const normalizedLevel = levelFilter.trim().toUpperCase();
    const normalizedTag = tagFilter.trim().toLowerCase();
    const normalizedText = textFilter.trim().toLowerCase();
    const normalizedPid = pidFilter.trim();
    const parsedPid = normalizedPid ? Number.parseInt(normalizedPid, 10) : null;

    return entries.filter((entry) => {
      if (normalizedLevel && normalizedLevel !== "ALL") {
        if (entry.level.toUpperCase() !== normalizedLevel) {
          return false;
        }
      }

      if (normalizedTag) {
        if (!entry.tag.toLowerCase().includes(normalizedTag)) {
          return false;
        }
      }

      if (normalizedText) {
        const haystack = `${entry.raw} ${entry.tag} ${entry.message}`.toLowerCase();
        if (!haystack.includes(normalizedText)) {
          return false;
        }
      }

      if (parsedPid !== null && !Number.isNaN(parsedPid)) {
        if (entry.pid !== parsedPid) {
          return false;
        }
      }

      return true;
    });
  }, [entries, levelFilter, pidFilter, tagFilter, textFilter]);

  const start = useCallback(async () => {
    setErrorMessage("");
    setStatusMessage("Starting logcat stream...");
    const normalizedLevel = levelFilter.trim().toUpperCase();
    const normalizedTag = tagFilter.trim();
    const normalizedText = textFilter.trim();
    const normalizedPid = pidFilter.trim();

    await startLogcat(selectedSerial ?? undefined, {
      level: normalizedLevel && normalizedLevel !== "ALL" ? normalizedLevel : null,
      tag: normalizedTag || null,
      keyword: normalizedText || null,
      pid: normalizedPid ? Number.parseInt(normalizedPid, 10) : null,
    });
  }, [levelFilter, pidFilter, selectedSerial, tagFilter, textFilter]);

  const stop = useCallback(async () => {
    await stopLogcat();
  }, []);

  const clear = useCallback(async () => {
    await clearLogcat(selectedSerial ?? undefined);
    setEntries([]);
    setStatusMessage("Device logcat buffer cleared");
    setErrorMessage("");
  }, [selectedSerial]);

  const exportToFile = useCallback(async () => {
    if (filteredEntries.length === 0) {
      setErrorMessage("No visible log entries available for export");
      return;
    }

    const filePath = await save({
      defaultPath: buildDefaultExportName(selectedSerial),
      filters: [{ name: "Text", extensions: ["txt"] }],
    });

    if (!filePath) {
      return;
    }

    const savedPath = await exportLogcat(filteredEntries, filePath);
    setStatusMessage(`Logcat exported to ${savedPath}`);
    setErrorMessage("");
  }, [filteredEntries, selectedSerial]);

  const clearEntries = useCallback(() => {
    setEntries([]);
    setStatusMessage("Visible log entries cleared from the viewer");
    setErrorMessage("");
  }, []);

  return {
    entries,
    filteredEntries,
    isRunning,
    selectedSerial,
    levelFilter,
    tagFilter,
    textFilter,
    pidFilter,
    autoScroll,
    paused,
    statusMessage,
    errorMessage,
    setSelectedSerial,
    setLevelFilter,
    setTagFilter,
    setTextFilter,
    setPidFilter,
    setAutoScroll,
    setPaused,
    start,
    stop,
    clear,
    exportToFile,
    clearEntries,
  };
}
