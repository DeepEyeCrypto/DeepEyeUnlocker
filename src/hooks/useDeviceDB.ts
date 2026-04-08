import { invoke } from "@tauri-apps/api/core";
import { emit } from "@tauri-apps/api/event";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import rawDeviceDatabase from "../../app/src/main/assets/device_database.json";

// --- LEGACY TYPES & DATA (from original useDeviceDB.ts) ---

export const FRP_BRAND_SELECTOR = [
  "Samsung",
  "Xiaomi",
  "OPPO",
  "Vivo",
  "Realme",
  "Motorola",
  "Huawei",
  "Nokia",
  "OnePlus",
  "Infinix",
  "Tecno",
  "Others",
] as const;

export type FrpBrandSelection = (typeof FRP_BRAND_SELECTOR)[number];
export type ChipsetFamily = "qualcomm" | "mediatek" | "unisoc" | "exynos" | "unknown";
export type ProgrammingFileType = "firehose" | "scatter" | "da";

export interface ProgrammingFile {
  brand: string;
  model: string;
  chipset: string;
  file_type: "firehose" | "scatter" | "da";
  file_path: string;
  verified: boolean;
}

export type ProgrammingLookup = {
  brand: string;
  model: string;
  chipset: string;
  chipsetFamily?: ChipsetFamily;
};

export type DeviceRecord = {
  key: string;
  brand: string;
  name: string;
  model: string;
  codename?: string;
  chipset: string;
  chipsetFamily: ChipsetFamily;
};

type RawDatabaseModel = {
  name: string;
  chipset: string;
  codename?: string;
};

type RawDatabaseBrand = {
  models: RawDatabaseModel[];
};

type RawDeviceDatabase = {
  version: string;
  totalModels: number;
  lastUpdated: string;
  brands: Record<string, RawDatabaseBrand>;
};

const deviceDatabase = rawDeviceDatabase as RawDeviceDatabase;

const BRAND_ALIASES: Record<string, string> = {
  ASUS: "Asus",
  GOOGLE: "Google",
  HUAWEI: "Huawei",
  INFINIX: "Infinix",
  LENOVO: "Lenovo",
  LG: "LG",
  MOTOROLA: "Motorola",
  NOKIA: "Nokia",
  ONEPLUS: "OnePlus",
  OPPO: "OPPO",
  REALME: "Realme",
  SAMSUNG: "Samsung",
  TECNO: "Tecno",
  VIVO: "Vivo",
  XIAOMI: "Xiaomi",
  ZTE: "ZTE",
};

const PRIORITY_BRANDS = new Set<string>(FRP_BRAND_SELECTOR.filter((brand) => brand !== "Others"));

const PROGRAMMING_TEMPLATES = [
  {
    family: "qualcomm",
    matcher: (chipset: string) => /\bSM6115\b/i.test(chipset),
    file_type: "firehose",
    file_path: "app/src/main/assets/prog/sm6115_emmc_firehose.elf",
  },
  {
    family: "qualcomm",
    matcher: (chipset: string) => /\bSM8250\b/i.test(chipset),
    file_type: "firehose",
    file_path: "app/src/main/assets/prog/sm8250_ufs_firehose.elf",
  },
  {
    family: "qualcomm",
    matcher: (chipset: string) => /\bSM8350\b/i.test(chipset),
    file_type: "firehose",
    file_path: "app/src/main/assets/prog/sm8350_emmc_firehose.elf",
  },
  {
    family: "qualcomm",
    matcher: (chipset: string) => /\bSM8350\b/i.test(chipset),
    file_type: "firehose",
    file_path: "app/src/main/assets/prog/sm8350_ufs_firehose.elf",
  },
  {
    family: "mediatek",
    matcher: () => true,
    file_type: "da",
    file_path: "app/src/main/assets/da/MTK_DA_V5.bin",
  },
  {
    family: "mediatek",
    matcher: () => true,
    file_type: "da",
    file_path: "app/src/main/assets/da/MTK_DA_V6.bin",
  },
] as const;

export function formatChipsetFamilyLabel(family: ChipsetFamily): string {
  switch (family) {
    case "qualcomm": return "Qualcomm";
    case "mediatek": return "MediaTek";
    case "unisoc": return "Unisoc";
    case "exynos": return "Exynos";
    default: return "Unknown";
  }
}

export function detectChipsetFamily(chipset: string): ChipsetFamily {
  const normalized = chipset.trim().toLowerCase();
  if (!normalized || normalized.includes("auto-detect")) return "unknown";
  if (normalized.includes("exynos")) return "exynos";
  if (normalized.includes("unisoc") || normalized.includes("spreadtrum") || /^sc\d+/i.test(normalized) || /^ums\d+/i.test(normalized) || /^t\d{3}/i.test(normalized)) return "unisoc";
  if (normalized.startsWith("mt") || normalized.includes("mediatek") || normalized.includes("dimensity") || normalized.includes("helio")) return "mediatek";
  if (normalized.startsWith("sm") || normalized.startsWith("sdm") || normalized.startsWith("msm") || normalized.startsWith("qcm") || normalized.includes("snapdragon") || normalized.includes("qualcomm")) return "qualcomm";
  return "unknown";
}

function normalizeBrandLabel(brand: string): string {
  const normalized = brand.trim();
  return BRAND_ALIASES[normalized.toUpperCase()] ?? normalized;
}

function buildRecordKey(brand: string, name: string, chipset: string): string {
  return `${brand}::${name}::${chipset}`.toLowerCase();
}

function buildDeviceRecords(database: RawDeviceDatabase): DeviceRecord[] {
  return Object.entries(database.brands)
    .flatMap(([brandName, brandData]) => {
      const brand = normalizeBrandLabel(brandName);
      return brandData.models.map((model) => ({
        key: buildRecordKey(brand, model.name, model.chipset),
        brand,
        name: model.name,
        model: model.name,
        codename: model.codename,
        chipset: model.chipset,
        chipsetFamily: detectChipsetFamily(model.chipset),
      }));
    })
    .sort((left, right) => {
      const brandCompare = left.brand.localeCompare(right.brand);
      if (brandCompare !== 0) return brandCompare;
      return left.name.localeCompare(right.name);
    });
}

const DEVICE_RECORDS = buildDeviceRecords(deviceDatabase);

// --- MODERN TYPES (for useDeviceDb / Tauri) ---

export interface DeviceEntry {
  brand: string;
  model: string;
  codename: string;
  soc: string;
  soc_family: "Qualcomm" | "MediaTek" | "Samsung" | "Unisoc" | "Kirin" | "Unknown";
  chipset_id: string;
  protocol: "Edl" | "MtkBrom" | "SamsungOdin" | "Adb" | "Fastboot" | "Unknown";
  vid: number | null;
  pid: number | null;
  firehose_path: string | null;
  da_file: string | null;
  notes: string | null;
}

export interface RoutingPreFill {
  firehose_path: string | null;
  da_path: string | null;
  partition_hints: string[];
}

export interface HardwareGuide {
  mode_name: string;
  button_combo: string;
  test_point: string | null;
  steps: string[];
  warning: string | null;
  danger_zone: boolean;
}

export interface RoutingResult {
  device: DeviceEntry | null;
  protocol: string;
  strategy?: string;
  route_to: string;
  confidence: number;
  pre_fill: RoutingPreFill;
  hardware_guide: HardwareGuide;
  frp_partitions: string[];
  danger_zone: boolean;
}

// --- LEGACY HOOK ---
export function useDeviceDB() {
  const records = useMemo(() => DEVICE_RECORDS, []);
  
  const brandsInDatabase = useMemo(() => {
    const uniqueBrands = [...new Set(records.map((r) => r.brand))];
    const prioritized = FRP_BRAND_SELECTOR.filter((b): b is Exclude<FrpBrandSelection, "Others"> => b !== "Others" && uniqueBrands.includes(b));
    const others = uniqueBrands.filter((b) => !PRIORITY_BRANDS.has(b)).sort();
    return [...prioritized, ...others];
  }, [records]);

  const otherBrands = useMemo(() => brandsInDatabase.filter(b => !PRIORITY_BRANDS.has(b)), [brandsInDatabase]);

  const getBrandModels = useCallback((brand: FrpBrandSelection) => 
    records.filter(r => brand === "Others" ? !PRIORITY_BRANDS.has(r.brand) : r.brand === brand), [records]);

  const searchModels = useCallback((query: string, brand: FrpBrandSelection, limit = 8) => {
    const q = query.trim().toLowerCase();
    return getBrandModels(brand)
      .map(record => {
        let score = 0;
        const fields = [record.name, record.model, record.codename ?? "", record.chipset];
        for (const f of fields) {
          const nf = f.toLowerCase();
          if (nf === q) score = Math.max(score, 400);
          else if (nf.startsWith(q)) score = Math.max(score, 280);
          else if (nf.includes(q)) score = Math.max(score, 180);
        }
        return { record, score };
      })
      .filter(x => x.score > 0)
      .sort((a, b) => b.score - a.score || a.record.name.localeCompare(b.record.name))
      .slice(0, limit)
      .map(x => x.record);
  }, [getBrandModels]);

  const resolveModel = useCallback((query: string, brand: FrpBrandSelection) => searchModels(query, brand, 1)[0] ?? null, [searchModels]);

  const getProgrammingFiles = useCallback((lookup: ProgrammingLookup): ProgrammingFile[] => {
    const family = lookup.chipsetFamily ?? detectChipsetFamily(lookup.chipset);
    const norm = lookup.chipset.trim().toUpperCase();
    return PROGRAMMING_TEMPLATES
      .filter(t => t.family === family && t.matcher(norm))
      .map(t => ({ brand: lookup.brand, model: lookup.model, chipset: lookup.chipset, file_type: t.file_type, file_path: t.file_path, verified: true }));
  }, []);

  return {
    databaseVersion: deviceDatabase.version,
    totalModels: deviceDatabase.totalModels,
    lastUpdated: deviceDatabase.lastUpdated,
    records,
    brands: FRP_BRAND_SELECTOR,
    brandsInDatabase,
    otherBrands,
    getBrandModels,
    searchModels,
    resolveModel,
    getProgrammingFiles,
    detectChipsetFamily,
    formatChipsetFamilyLabel,
  };
}

// --- MODERN HOOK (Tauri-backed) ---
export function useDeviceDb() {
  const [results, setResults] = useState<DeviceEntry[]>([]);
  const [allDevices, setAllDevices] = useState<DeviceEntry[]>([]);
  const [selectedDevice, setSelectedDevice] = useState<DeviceEntry | null>(null);
  const [routingResult, setRoutingResult] = useState<RoutingResult | null>(null);
  const [isSearching, setIsSearching] = useState(false);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    invoke<DeviceEntry[]>("db_list_all").then(setAllDevices).catch(console.error);
  }, []);

  const search = useCallback((query: string) => {
    if (debounceRef.current) clearTimeout(debounceRef.current);
    if (!query.trim()) { setResults([]); return; }
    debounceRef.current = setTimeout(async () => {
      setIsSearching(true);
      try {
        const res = await invoke<DeviceEntry[]>("db_search_devices", { query });
        setResults(res);
      } catch (e) {
        console.error(e);
      } finally {
        setIsSearching(false);
      }
    }, 300);
  }, []);

  const searchDevices = useCallback(async (query: string): Promise<DeviceEntry[]> => {
    if (!query.trim()) {
      setResults([]);
      return [];
    }

    setIsSearching(true);
    try {
      const res = await invoke<DeviceEntry[]>("db_search_devices", { query });
      setResults(res);
      return res;
    } catch (error) {
      console.error(error);
      return [];
    } finally {
      setIsSearching(false);
    }
  }, []);

  const lookupModel = useCallback(async (model: string) => {
    const entry = await invoke<DeviceEntry | null>("db_lookup_model", { model });
    setSelectedDevice(entry);
  }, []);

  const autoRoute = useCallback(async (model: string): Promise<RoutingResult> => {
    const result = await invoke<RoutingResult>("db_auto_route", { model });
    setRoutingResult(result);
    await emit("navigate-to-protocol", {
      route: result.route_to,
      device: result.device,
      pre_fill: result.pre_fill,
    });
    return result;
  }, []);

  const lookupVidPid = useCallback(async (vid: number, pid: number) => {
    const entry = await invoke<DeviceEntry | null>("db_lookup_vid_pid", { vid, pid });
    setSelectedDevice(entry ?? null);
    if (entry) {
      const result = await invoke<RoutingResult>("db_auto_route", { model: entry.model });
      setRoutingResult(result);
    }
  }, []);

  return {
    results,
    allDevices,
    selectedDevice,
    routingResult,
    isSearching,
    search,
    searchDevices,
    lookupModel,
    autoRoute,
    lookupVidPid,
  };
}
