# MTK Download Agent (DA) Binaries

Place the following files here:

## Required (AllInOne strategy — 2 files cover ALL chips)

| File | Covers | Source |
|------|--------|--------|
| `MTK_AllInOne_DA.bin` | All Helio chips (MT6739–MT6785) | SP Flash Tool V5 |
| `MTK_AllInOne_DA_V6.bin` | All Dimensity chips (MT6833–MT6991) | SP Flash Tool V6 |

## Download Instructions

1. Go to https://spflashtools.com/category/flash-tool
2. Download SP Flash Tool V5 (legacy) and V6 (latest)
3. Extract the installer/archive
4. Copy `MTK_AllInOne_DA.bin` from V5 → here
5. Copy `MTK_AllInOne_DA_V6.bin` from V6 → here

## Validate

```bash
python3 scripts/validate_da.py app/src/main/assets/da/*.bin
```

## Optional Per-Chip Files (same bytes, different names — for debugging)

| File | Chip | Source |
|------|------|--------|
| `mt6835t_da.bin` | Dimensity 6300 (Realme 14x) | SP Flash Tool V6 AllInOne |
| `mt6769_da.bin` | Helio G85 (Samsung A14/A15 5G MTK) | SP Flash Tool V5 AllInOne |
| `mt6765_da.bin` | Helio G35 (Samsung A06/Infinix) | SP Flash Tool V5 AllInOne |

Per-chip files are NOT required — `MTK_AllInOne_DA_V6.bin` / `MTK_AllInOne_DA.bin` handle all chips automatically.
