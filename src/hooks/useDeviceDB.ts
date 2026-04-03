import { useCallback, useMemo } from "react";
import rawDeviceDatabase from "../../app/src/main/assets/device_database.json";

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

type ProgrammingTemplate = {
  family: ChipsetFamily;
  matcher: (chipset: string) => boolean;
  file_type: ProgrammingFileType;
  file_path: string;
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

export interface ProgrammingFile {
  brand: string;
  model: string;
  chipset: string;
  file_type: "firehose" | "scatter" | "da";
  file_path: string;
  verified: boolean;
}

type ProgrammingLookup = {
  brand: string;
  model: string;
  chipset: string;
  chipsetFamily?: ChipsetFamily;
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

// [INFERRED] Programming file coverage is derived from assets physically present in the repository.
const PROGRAMMING_TEMPLATES: readonly ProgrammingTemplate[] = [
  {
    family: "qualcomm",
    matcher: (chipset) => /\bSM6115\b/i.test(chipset),
    file_type: "firehose",
    file_path: "app/src/main/assets/prog/sm6115_emmc_firehose.elf",
  },
  {
    family: "qualcomm",
    matcher: (chipset) => /\bSM8250\b/i.test(chipset),
    file_type: "firehose",
    file_path: "app/src/main/assets/prog/sm8250_ufs_firehose.elf",
  },
  {
    family: "qualcomm",
    matcher: (chipset) => /\bSM8350\b/i.test(chipset),
    file_type: "firehose",
    file_path: "app/src/main/assets/prog/sm8350_emmc_firehose.elf",
  },
  {
    family: "qualcomm",
    matcher: (chipset) => /\bSM8350\b/i.test(chipset),
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
    case "qualcomm":
      return "Qualcomm";
    case "mediatek":
      return "MediaTek";
    case "unisoc":
      return "Unisoc";
    case "exynos":
      return "Exynos";
    default:
      return "Unknown";
  }
}

export function detectChipsetFamily(chipset: string): ChipsetFamily {
  const normalized = chipset.trim().toLowerCase();

  if (!normalized || normalized.includes("auto-detect")) {
    return "unknown";
  }

  if (normalized.includes("exynos")) {
    return "exynos";
  }

  if (
    normalized.includes("unisoc") ||
    normalized.includes("spreadtrum") ||
    /^sc\d+/i.test(normalized) ||
    /^ums\d+/i.test(normalized) ||
    /^t\d{3}/i.test(normalized)
  ) {
    return "unisoc";
  }

  if (
    normalized.startsWith("mt") ||
    normalized.includes("mediatek") ||
    normalized.includes("dimensity") ||
    normalized.includes("helio")
  ) {
    return "mediatek";
  }

  if (
    normalized.startsWith("sm") ||
    normalized.startsWith("sdm") ||
    normalized.startsWith("msm") ||
    normalized.startsWith("qcm") ||
    normalized.includes("snapdragon") ||
    normalized.includes("qualcomm")
  ) {
    return "qualcomm";
  }

  return "unknown";
}

function normalizeBrandLabel(brand: string): string {
  const normalized = brand.trim();
  return BRAND_ALIASES[normalized.toUpperCase()] ?? normalized;
}

function buildRecordKey(brand: string, name: string, chipset: string): string {
  return `${brand}::${name}::${chipset}`.toLowerCase();
}

// [INFERRED] The curated JSON database is the authoritative source for brand/model/chipset mapping in this hub.
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
      if (brandCompare !== 0) {
        return brandCompare;
      }
      return left.name.localeCompare(right.name);
    });
}

function isOtherBrand(brand: string): boolean {
  return !PRIORITY_BRANDS.has(brand);
}

function matchesBrandSelection(record: DeviceRecord, brandSelection: FrpBrandSelection): boolean {
  if (brandSelection === "Others") {
    return isOtherBrand(record.brand);
  }
  return record.brand === brandSelection;
}

function getQueryScore(record: DeviceRecord, query: string): number {
  if (!query) {
    return 1;
  }

  const fields = [record.name, record.model, record.codename ?? "", record.chipset];
  let score = 0;

  for (const field of fields) {
    const normalizedField = field.toLowerCase();

    if (!normalizedField) {
      continue;
    }

    if (normalizedField === query) {
      score = Math.max(score, 400);
    } else if (normalizedField.startsWith(query)) {
      score = Math.max(score, 280);
    } else if (normalizedField.includes(query)) {
      score = Math.max(score, 180);
    }
  }

  return score;
}

function dedupeProgrammingFiles(files: ProgrammingFile[]): ProgrammingFile[] {
  const seen = new Set<string>();
  return files.filter((file) => {
    const key = `${file.file_type}:${file.file_path}`;
    if (seen.has(key)) {
      return false;
    }
    seen.add(key);
    return true;
  });
}

function resolveProgrammingFiles(lookup: ProgrammingLookup): ProgrammingFile[] {
  const family = lookup.chipsetFamily ?? detectChipsetFamily(lookup.chipset);
  const normalizedChipset = lookup.chipset.trim().toUpperCase();

  const matchedFiles = PROGRAMMING_TEMPLATES.filter((template) => {
    if (template.family !== family) {
      return false;
    }
    return template.matcher(normalizedChipset);
  }).map<ProgrammingFile>((template) => ({
    brand: lookup.brand,
    model: lookup.model,
    chipset: lookup.chipset,
    file_type: template.file_type,
    file_path: template.file_path,
    verified: true,
  }));

  return dedupeProgrammingFiles(matchedFiles);
}

const DEVICE_RECORDS = buildDeviceRecords(deviceDatabase);

export function useDeviceDB() {
  const records = useMemo(() => DEVICE_RECORDS, []);

  const brandsInDatabase = useMemo(() => {
    const uniqueBrands = [...new Set(records.map((record) => record.brand))];
    const prioritized = FRP_BRAND_SELECTOR.filter(
      (brand): brand is Exclude<FrpBrandSelection, "Others"> => brand !== "Others" && uniqueBrands.includes(brand),
    );
    const others = uniqueBrands.filter((brand) => isOtherBrand(brand)).sort((left, right) => left.localeCompare(right));
    return [...prioritized, ...others];
  }, [records]);

  const otherBrands = useMemo(
    () => brandsInDatabase.filter((brand) => isOtherBrand(brand)),
    [brandsInDatabase],
  );

  const getBrandModels = useCallback(
    (brandSelection: FrpBrandSelection): DeviceRecord[] =>
      records.filter((record) => matchesBrandSelection(record, brandSelection)),
    [records],
  );

  const searchModels = useCallback(
    (query: string, brandSelection: FrpBrandSelection, limit = 8): DeviceRecord[] => {
      const normalizedQuery = query.trim().toLowerCase();

      return getBrandModels(brandSelection)
        .map((record) => ({ record, score: getQueryScore(record, normalizedQuery) }))
        .filter(({ score }) => score > 0)
        .sort((left, right) => {
          if (left.score !== right.score) {
            return right.score - left.score;
          }
          return left.record.name.localeCompare(right.record.name);
        })
        .slice(0, limit)
        .map(({ record }) => record);
    },
    [getBrandModels],
  );

  const resolveModel = useCallback(
    (query: string, brandSelection: FrpBrandSelection): DeviceRecord | null => {
      const [match] = searchModels(query, brandSelection, 1);
      return match ?? null;
    },
    [searchModels],
  );

  const getProgrammingFiles = useCallback(
    (lookup: ProgrammingLookup): ProgrammingFile[] => resolveProgrammingFiles(lookup),
    [],
  );

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
