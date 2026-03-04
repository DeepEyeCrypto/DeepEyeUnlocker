# DeepEyeUnlocker v5.5.1 — "The Device DB" 🗄️

**Release Date:** 2026-03-04  
**Focus:** PostgreSQL device-profile database, 1 164-model seed, Kotlin Exposed ORM, Compose session panel refactor

---

## 💎 Key Highlights

- **PostgreSQL Device-Profile Schema** — Production-ready Flyway migration with UUID primary keys, `chipset_type` / `engine_type` ENUMs, audit table, GIN index on supported functions, and auto-update trigger.
- **1 164 Device Models Seeded** — 27 brands covering Samsung (138), Xiaomi (133), Infinix (117), Realme (71), Motorola (70), Huawei (66), Honor (60), OPPO (59), Vivo (59), OnePlus (39), Google (40), Nokia (38), Lava (41), Micromax (37), Tecno (33), ASUS (26), CAT (23), Karbonn (23), LG (18), BlackBerry (15), HTC (15), RedMagic (13), ZTE (13), Fairphone (7), Nothing (7), Razer (2), and Essential (1).
- **Kotlin Exposed ORM Aligned** — `UUIDTable` with `ChipsetType`, `EngineType` enums, `DeviceCheckAudit` entity + DTO, all 24 feature-ID mappings.
- **Compose Session Panel** — New Jetpack Compose session panel UI and `UsbConnectionController` refactor (carried from v5.5.1-wip).

---

## 🔧 Technical Changes

### V1__create_device_profiles.sql (Flyway)
- `uuid-ossp` extension for UUID v4 generation
- `chipset_type` ENUM: qualcomm, mediatek, samsung_exynos, kirin, tensor, unisoc, unknown
- `engine_type` ENUM: qualcomm, mediatek, samsung, unisoc
- `device_profiles` table — UUID PK, brand, model, series, release_year, device_type, chipset, engine, bootloader_unlockable, supported_functions (JSONB), frp_state (VARCHAR 32), notes, timestamps
- `device_check_audit` table — UUID PK, user_hash, brand, model, serial_hash, operation, tier, result, checked_at
- Indexes: brand, chipset, engine, year, brand+model composite, GIN on supported_functions, audit user_hash+checked_at, audit serial_hash
- `update_updated_at()` trigger on device_profiles

### V2__seed_device_profiles.sql (Flyway)
- 1 164 INSERT statements across 27 brands
- Realistic series, release years (2010–2025), device types, chipset/engine mappings
- Feature-set profiles: QC flagship (17 IDs), MTK full (15), Samsung flagship (18), Samsung budget (10), Huawei (15), Xiaomi (15), budget generic (6/7)

### DeviceProfile.kt (Kotlin Exposed ORM)
- `DeviceProfiles` object extends `UUIDTable` (was `LongIdTable`)
- New columns: `series`, `releaseYear`, `deviceType`, `engine`
- Removed: `marketingName`, `codename`, `cpuArch`, `supportedProtocols`, `usbVid`, `usbPid`, `region`, `validationStatus`
- `ChipsetType` enum replaces `ChipsetFamily`
- `EngineType` enum (new)
- `DeviceCheckAudits` table + `DeviceCheckAudit` entity
- `DeviceProfileDto` — UUID string ID, new fields
- `DeviceCheckAuditDto` — full audit DTO

### Android (carried from wip)
- Compose session panel UI
- `UsbConnectionController` refactor (8 files)

---

## 📊 Stats

| Metric | Value |
|---|---|
| Brands seeded | 27 |
| Models seeded | 1 164 |
| Feature IDs | 24 |
| Flyway migrations | 2 (V1 schema + V2 seed) |
| Files changed (DB) | 3 |
| Lines added | 1 420 |

---

## 🚀 Upgrade Notes

```bash
# Apply Flyway migrations
flyway -url=jdbc:postgresql://localhost/deepeye \
       -user=deepeye -password=secret \
       -locations=filesystem:db/migrations \
       migrate
```

No breaking changes to the Android app or Windows desktop client.
