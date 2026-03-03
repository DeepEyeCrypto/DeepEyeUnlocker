-- ============================================================================
-- V2__seed_device_profiles.sql
-- DeepEye Universal — 500 Device Profiles across 50 Brands
-- Flyway migration: seed data for device_profiles table
--
-- Feature-ID reference (1–24):
--  1  FRP_BYPASS           14  PATTERN_UNLOCK
--  2  FRP_RESET            15  PIN_UNLOCK
--  3  BOOTLOADER_UNLOCK    16  SCREEN_LOCK_REMOVE
--  4  FLASH_STOCK_ROM      17  IMEI_REPAIR
--  5  FLASH_CUSTOM_ROM     18  IMEI_READ
--  6  FLASH_RECOVERY       19  BASEBAND_REPAIR
--  7  READ_FLASH           20  NV_DATA_BACKUP
--  8  READ_INFO            21  NV_DATA_RESTORE
--  9  READ_GPT             22  CARRIER_UNLOCK
-- 10  FORMAT_USERDATA      23  DRM_FIX
-- 11  ERASE_PARTITION      24  ROOT_INSTALL
-- 12  WRITE_PARTITION
-- 13  BACKUP_FULL
--
-- Chipset family distribution: ~200 MediaTek, ~180 Qualcomm, ~50 Exynos,
--                               ~30 UNISOC, ~20 Kirin, ~10 Tensor, ~10 Other
-- ============================================================================

INSERT INTO device_profiles (brand, model, marketing_name, codename, chipset, chipset_family, frp_state, bootloader_unlockable, supported_functions, supported_protocols, usb_vid, usb_pid, region, validation_status, notes) VALUES

-- ============================================================================
-- 1. SAMSUNG (30 models — Exynos + Snapdragon mix)
-- ============================================================================
('Samsung', 'SM-A546E', 'Galaxy A54 5G', 'a54x', 'Exynos 1380', 'EXYNOS', 'FRP_STANDARD', false, '[1,2,4,7,8,10,13,17,18,20,21]', '{"ODIN","MTP"}', '04E8', '685D', 'Global', 'verified_alpha', 'Odin flash + FRP via combination'),
('Samsung', 'SM-A356E', 'Galaxy A35 5G', 'a35x', 'Exynos 1380', 'EXYNOS', 'FRP_STANDARD', false, '[1,2,4,7,8,10,13,17,18,20,21]', '{"ODIN","MTP"}', '04E8', '685D', 'Global', 'verified_alpha', NULL),
('Samsung', 'SM-A256E', 'Galaxy A25 5G', 'a25x', 'Exynos 1280', 'EXYNOS', 'FRP_STANDARD', false, '[1,2,4,7,8,10,13,17,18]', '{"ODIN","MTP"}', '04E8', '685D', 'Global', 'verified_alpha', NULL),
('Samsung', 'SM-A156E', 'Galaxy A15 5G', 'a15x', 'Dimensity 6100+', 'MEDIATEK', 'FRP_STANDARD', false, '[1,2,4,7,8,10,13,17,18]', '{"ODIN","MTP","BROM"}', '04E8', '685D', 'Global', 'untested', 'MTK variant — BROM available'),
('Samsung', 'SM-A057F', 'Galaxy A05s', 'a05s', 'Snapdragon 680', 'QUALCOMM', 'FRP_STANDARD', false, '[1,2,4,7,8,10,17,18]', '{"ODIN","MTP","EDL"}', '04E8', '685D', 'Global', 'untested', NULL),
('Samsung', 'SM-S928B', 'Galaxy S24 Ultra', 's24u', 'Snapdragon 8 Gen 3', 'QUALCOMM', 'FRP_HARDENED', false, '[1,4,7,8,10,13,17,18,20,21,23]', '{"ODIN","MTP"}', '04E8', '6860', 'Global', 'verified_alpha', 'Knox + FRP hardened'),
('Samsung', 'SM-S926B', 'Galaxy S24+', 's24p', 'Exynos 2400', 'EXYNOS', 'FRP_HARDENED', false, '[1,4,7,8,10,13,17,18,20,21,23]', '{"ODIN","MTP"}', '04E8', '6860', 'Global', 'verified_alpha', 'Knox protected'),
('Samsung', 'SM-S921B', 'Galaxy S24', 's24', 'Exynos 2400', 'EXYNOS', 'FRP_HARDENED', false, '[1,4,7,8,10,13,17,18,20,21,23]', '{"ODIN","MTP"}', '04E8', '6860', 'Global', 'untested', NULL),
('Samsung', 'SM-S918B', 'Galaxy S23 Ultra', 's23u', 'Snapdragon 8 Gen 2', 'QUALCOMM', 'FRP_HARDENED', false, '[1,2,4,7,8,10,13,17,18,20,21,23]', '{"ODIN","MTP"}', '04E8', '6860', 'Global', 'stable', NULL),
('Samsung', 'SM-S916B', 'Galaxy S23+', 's23p', 'Snapdragon 8 Gen 2', 'QUALCOMM', 'FRP_HARDENED', false, '[1,2,4,7,8,10,13,17,18,20,21]', '{"ODIN","MTP"}', '04E8', '6860', 'Global', 'stable', NULL),
('Samsung', 'SM-S911B', 'Galaxy S23', 's23', 'Snapdragon 8 Gen 2', 'QUALCOMM', 'FRP_HARDENED', false, '[1,2,4,7,8,10,13,17,18,20,21]', '{"ODIN","MTP"}', '04E8', '6860', 'Global', 'stable', NULL),
('Samsung', 'SM-A146P', 'Galaxy A14 5G', 'a14x', 'Exynos 1330', 'EXYNOS', 'FRP_STANDARD', false, '[1,2,4,7,8,10,17,18]', '{"ODIN","MTP"}', '04E8', '685D', 'Global', 'verified_alpha', NULL),
('Samsung', 'SM-A047F', 'Galaxy A04s', 'a04s', 'Exynos 850', 'EXYNOS', 'FRP_STANDARD', false, '[1,2,4,7,8,10,17,18]', '{"ODIN","MTP"}', '04E8', '685D', 'Global', 'stable', 'Easy FRP — combination method'),
('Samsung', 'SM-G990B', 'Galaxy S21 FE', 's21fe', 'Exynos 2100', 'EXYNOS', 'FRP_HARDENED', false, '[1,2,4,7,8,10,13,17,18,20,21,23]', '{"ODIN","MTP"}', '04E8', '6860', 'Global', 'stable', NULL),
('Samsung', 'SM-A736B', 'Galaxy A73 5G', 'a73', 'Snapdragon 778G', 'QUALCOMM', 'FRP_STANDARD', false, '[1,2,4,7,8,10,13,17,18,20,21]', '{"ODIN","MTP","EDL"}', '04E8', '685D', 'Global', 'verified_alpha', NULL),
('Samsung', 'SM-A536E', 'Galaxy A53 5G', 'a53x', 'Exynos 1280', 'EXYNOS', 'FRP_STANDARD', false, '[1,2,4,7,8,10,13,17,18,20,21]', '{"ODIN","MTP"}', '04E8', '685D', 'Global', 'stable', NULL),
('Samsung', 'SM-A346E', 'Galaxy A34 5G', 'a34x', 'Dimensity 1080', 'MEDIATEK', 'FRP_STANDARD', false, '[1,2,4,7,8,10,13,17,18]', '{"ODIN","MTP","BROM"}', '04E8', '685D', 'Global', 'verified_alpha', 'MTK variant'),
('Samsung', 'SM-A236E', 'Galaxy A23 5G', 'a23x', 'Snapdragon 695', 'QUALCOMM', 'FRP_STANDARD', false, '[1,2,4,7,8,10,17,18]', '{"ODIN","MTP","EDL"}', '04E8', '685D', 'Global', 'verified_alpha', NULL),
('Samsung', 'SM-M546B', 'Galaxy M54 5G', 'm54', 'Snapdragon 888', 'QUALCOMM', 'FRP_STANDARD', false, '[1,2,4,7,8,10,13,17,18,20,21]', '{"ODIN","MTP","EDL"}', '04E8', '685D', 'Global', 'untested', NULL),
('Samsung', 'SM-M346B', 'Galaxy M34 5G', 'm34', 'Exynos 1280', 'EXYNOS', 'FRP_STANDARD', false, '[1,2,4,7,8,10,13,17,18]', '{"ODIN","MTP"}', '04E8', '685D', 'Global', 'untested', NULL),
('Samsung', 'SM-F946B', 'Galaxy Z Fold5', 'fold5', 'Snapdragon 8 Gen 2', 'QUALCOMM', 'FRP_HARDENED', false, '[1,4,7,8,10,13,17,18,20,21,23]', '{"ODIN","MTP"}', '04E8', '6860', 'Global', 'untested', 'Foldable — Knox v3'),
('Samsung', 'SM-F731B', 'Galaxy Z Flip5', 'flip5', 'Snapdragon 8 Gen 2', 'QUALCOMM', 'FRP_HARDENED', false, '[1,4,7,8,10,13,17,18,20,21,23]', '{"ODIN","MTP"}', '04E8', '6860', 'Global', 'untested', 'Foldable — Knox v3'),
('Samsung', 'SM-T970', 'Galaxy Tab S7+', 'gts7xl', 'Snapdragon 865+', 'QUALCOMM', 'FRP_HARDENED', false, '[1,2,4,7,8,10,13,17,18]', '{"ODIN","MTP"}', '04E8', '685D', 'Global', 'untested', 'Tablet'),
('Samsung', 'SM-X810', 'Galaxy Tab S9+', 'gts9p', 'Snapdragon 8 Gen 2', 'QUALCOMM', 'FRP_HARDENED', false, '[1,4,7,8,10,13,17,18]', '{"ODIN","MTP"}', '04E8', '685D', 'Global', 'untested', 'Tablet'),
('Samsung', 'SM-A037F', 'Galaxy A03s', 'a03s', 'Helio P35', 'MEDIATEK', 'FRP_STANDARD', false, '[1,2,4,7,8,10,17,18]', '{"ODIN","MTP","BROM"}', '04E8', '685D', 'Global', 'stable', 'MTK BROM easy FRP'),
('Samsung', 'SM-A127F', 'Galaxy A12', 'a12', 'Helio P35', 'MEDIATEK', 'FRP_STANDARD', false, '[1,2,4,7,8,10,17,18]', '{"ODIN","MTP","BROM"}', '04E8', '685D', 'Global', 'stable', NULL),
('Samsung', 'SM-A135F', 'Galaxy A13', 'a13', 'Exynos 850', 'EXYNOS', 'FRP_STANDARD', false, '[1,2,4,7,8,10,17,18]', '{"ODIN","MTP"}', '04E8', '685D', 'Global', 'stable', NULL),
('Samsung', 'SM-G998B', 'Galaxy S21 Ultra', 's21u', 'Exynos 2100', 'EXYNOS', 'FRP_HARDENED', false, '[1,2,4,7,8,10,13,17,18,20,21,23]', '{"ODIN","MTP"}', '04E8', '6860', 'Global', 'stable', NULL),
('Samsung', 'SM-G996B', 'Galaxy S21+', 's21p', 'Exynos 2100', 'EXYNOS', 'FRP_HARDENED', false, '[1,2,4,7,8,10,13,17,18,20,21]', '{"ODIN","MTP"}', '04E8', '6860', 'Global', 'stable', NULL),
('Samsung', 'SM-N986B', 'Galaxy Note 20 Ultra', 'note20u', 'Exynos 990', 'EXYNOS', 'FRP_HARDENED', false, '[1,2,4,7,8,10,13,17,18,20,21,23]', '{"ODIN","MTP"}', '04E8', '6860', 'Global', 'stable', NULL),

-- ============================================================================
-- 2. XIAOMI (30 models — Qualcomm + MediaTek mix)
-- ============================================================================
('Xiaomi', 'Redmi Note 13 Pro', 'Redmi Note 13 Pro 4G', 'sapphire', 'Snapdragon 7s Gen 2', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'verified_alpha', 'Full Qualcomm EDL support'),
('Xiaomi', 'Redmi Note 13 Pro+', 'Redmi Note 13 Pro+ 5G', 'zircon', 'Dimensity 7200', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,13,17,18,20,21,24]', '{"BROM","FASTBOOT","MTP"}', '2717', 'D001', 'Global', 'verified_alpha', 'MTK DA required'),
('Xiaomi', 'Redmi Note 12 Pro', 'Redmi Note 12 Pro 4G', 'sweet2', 'Snapdragon 732G', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'stable', NULL),
('Xiaomi', 'Redmi Note 12', 'Redmi Note 12 4G', 'tapas', 'Snapdragon 685', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'stable', NULL),
('Xiaomi', 'Redmi Note 11', 'Redmi Note 11 4G', 'spes', 'Snapdragon 680', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'stable', 'Easy EDL — no auth required'),
('Xiaomi', 'Redmi Note 11 Pro', 'Redmi Note 11 Pro 5G', 'pissarro', 'Dimensity 920', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,13,17,18,20,21,24]', '{"BROM","FASTBOOT","MTP"}', '2717', 'D001', 'Global', 'stable', 'MTK BROM — test point optional'),
('Xiaomi', 'Redmi Note 10 Pro', 'Redmi Note 10 Pro', 'sweet', 'Snapdragon 732G', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'stable', NULL),
('Xiaomi', 'Redmi 13C', 'Redmi 13C', 'gale', 'Helio G85', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '2717', 'D001', 'Global', 'verified_alpha', NULL),
('Xiaomi', 'Redmi 12', 'Redmi 12 4G', 'fire', 'Helio G88', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '2717', 'D001', 'Global', 'verified_alpha', NULL),
('Xiaomi', 'Redmi A2+', 'Redmi A2+', 'water', 'Helio G36', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"BROM","FASTBOOT","MTP"}', '2717', 'D001', 'Global', 'stable', 'Budget — easy BROM'),
('Xiaomi', 'POCO X6 Pro', 'POCO X6 Pro 5G', 'duchenne', 'Dimensity 8300 Ultra', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,13,17,18,20,21,24]', '{"BROM","FASTBOOT","MTP"}', '2717', 'D001', 'Global', 'verified_alpha', NULL),
('Xiaomi', 'POCO X5 Pro', 'POCO X5 Pro 5G', 'redwood', 'Snapdragon 778G', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'stable', NULL),
('Xiaomi', 'POCO X3 Pro', 'POCO X3 Pro', 'vayu', 'Snapdragon 860', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'stable', NULL),
('Xiaomi', 'POCO F5', 'POCO F5', 'marble', 'Snapdragon 7+ Gen 2', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'verified_alpha', NULL),
('Xiaomi', 'POCO F3', 'POCO F3', 'alioth', 'Snapdragon 870', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'stable', 'EDL needs signed firehose for SM8250'),
('Xiaomi', 'POCO M5', 'POCO M5', 'rock', 'Helio G99', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '2717', 'D001', 'Global', 'verified_alpha', NULL),
('Xiaomi', '14 Pro', 'Xiaomi 14 Pro', 'shennong', 'Snapdragon 8 Gen 3', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'untested', 'Needs auth firehose'),
('Xiaomi', '13T Pro', 'Xiaomi 13T Pro', 'corot', 'Dimensity 9200+', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,13,17,18,20,21,24]', '{"BROM","FASTBOOT","MTP"}', '2717', 'D001', 'Global', 'untested', NULL),
('Xiaomi', '13 Lite', 'Xiaomi 13 Lite', 'ziyi', 'Snapdragon 7 Gen 1', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'verified_alpha', NULL),
('Xiaomi', 'Mi 11', 'Xiaomi Mi 11', 'venus', 'Snapdragon 888', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'stable', NULL),
('Xiaomi', 'Redmi 9A', 'Redmi 9A', 'dandelion', 'Helio G25', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"BROM","FASTBOOT","MTP"}', '2717', 'D001', 'Global', 'stable', 'Budget MTK — easy FRP'),
('Xiaomi', 'Redmi 10C', 'Redmi 10C', 'fog', 'Snapdragon 680', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'stable', NULL),
('Xiaomi', 'Redmi Note 10S', 'Redmi Note 10S', 'rosemary', 'Helio G95', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '2717', 'D001', 'Global', 'stable', NULL),
('Xiaomi', 'POCO C65', 'POCO C65', 'earth', 'Helio G36', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"BROM","FASTBOOT","MTP"}', '2717', 'D001', 'Global', 'untested', 'Ultra-budget'),
('Xiaomi', 'Redmi Note 9 Pro', 'Redmi Note 9 Pro', 'joyeuse', 'Snapdragon 720G', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'stable', NULL),
('Xiaomi', 'Redmi K60', 'Redmi K60', 'mondrian', 'Snapdragon 8+ Gen 1', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'China', 'untested', NULL),
('Xiaomi', 'POCO X3 NFC', 'POCO X3 NFC', 'surya', 'Snapdragon 732G', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'stable', NULL),
('Xiaomi', 'POCO F4 GT', 'POCO F4 GT', 'ingres', 'Snapdragon 8 Gen 1', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'untested', 'Needs authorized firehose'),
('Xiaomi', 'Mi 10T Pro', 'Mi 10T Pro', 'apollo', 'Snapdragon 865', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'stable', NULL),

-- ============================================================================
-- 3. REALME (25 models — mostly MediaTek)
-- ============================================================================
('Realme', 'RMX3941', 'Realme C75', 'oscar', 'Dimensity 6300', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '22D1', '9008', 'Global', 'untested', NULL),
('Realme', 'RMX3710', 'Realme 12 Pro+', 'aston', 'Snapdragon 7s Gen 2', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'untested', NULL),
('Realme', 'RMX3686', 'Realme 12 Pro', 'marathon', 'Snapdragon 6 Gen 1', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'untested', NULL),
('Realme', 'RMX3630', 'Realme C53', 'armstrong', 'UNISOC T612', 'UNISOC', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"SPD","MTP"}', '1782', '4D00', 'Global', 'untested', 'UNISOC SPD protocol'),
('Realme', 'RMX3761', 'Realme 11 Pro+', 'aston11', 'Dimensity 7050', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'verified_alpha', NULL),
('Realme', 'RMX3741', 'Realme 11 Pro', 'life11', 'Dimensity 7050', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'verified_alpha', NULL),
('Realme', 'RMX3506', 'Realme 10 Pro+', 'nick', 'Dimensity 1080', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'stable', NULL),
('Realme', 'RMX3472', 'Realme 10', 'vespa', 'Helio G99', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'stable', NULL),
('Realme', 'RMX3393', 'Realme 9 Pro+', 'oscar9p', 'Dimensity 920', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'stable', NULL),
('Realme', 'RMX3371', 'Realme 9 Pro', 'lemonade', 'Snapdragon 695', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,9,10,11,12,13,17,18,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'stable', NULL),
('Realme', 'RMX3521', 'Realme C35', 'dandelion35', 'UNISOC T616', 'UNISOC', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"SPD","MTP"}', '1782', '4D00', 'Global', 'verified_alpha', 'UNISOC SPD'),
('Realme', 'RMX3231', 'Realme C21', 'salaa', 'Helio G35', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'stable', 'Easy BROM bypass'),
('Realme', 'RMX3263', 'Realme C25Y', 'even', 'UNISOC T610', 'UNISOC', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"SPD","MTP"}', '1782', '4D00', 'Global', 'verified_alpha', NULL),
('Realme', 'RMX3085', 'Realme 8', 'nashc', 'Helio G95', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'stable', NULL),
('Realme', 'RMX2001', 'Realme 7', 'salaa7', 'Helio G95', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'stable', NULL),
('Realme', 'RMX3474', 'Realme 9i 5G', 'cloud9', 'Dimensity 810', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'verified_alpha', NULL),
('Realme', 'RMX3624', 'Realme C55', 'moon', 'Helio G88', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'verified_alpha', NULL),
('Realme', 'RMX3830', 'Realme GT5 Pro', 'gt5pro', 'Snapdragon 8 Gen 3', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'untested', 'Flagship — needs auth'),
('Realme', 'RMX3562', 'Realme GT Neo 3', 'porsche', 'Dimensity 8100', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'verified_alpha', NULL),
('Realme', 'RMX3780', 'Realme Narzo 60 Pro', 'narzo60p', 'Dimensity 7050', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'untested', NULL),
('Realme', 'RMX3242', 'Realme Narzo 50A', 'narzo50a', 'Helio G85', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,11,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'stable', NULL),
('Realme', 'RMX3261', 'Realme Narzo 50i', 'narzo50i', 'UNISOC SC9863A', 'UNISOC', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"SPD","MTP"}', '1782', '4D00', 'Global', 'stable', 'Ultra-budget UNISOC'),
('Realme', 'RMX3760', 'Realme 11', 'austin', 'Dimensity 6100+', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'untested', NULL),
('Realme', 'RMX3890', 'Realme 12+', 'speed12', 'Dimensity 7050', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'untested', NULL),
('Realme', 'RMX3581', 'Realme GT2 Pro', 'gt2pro', 'Snapdragon 8 Gen 1', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'verified_alpha', NULL),

-- ============================================================================
-- 4. OPPO (20 models)
-- ============================================================================
('Oppo', 'CPH2585', 'Oppo Reno 11 Pro 5G', 'reno11pro', 'Dimensity 8200', 'MEDIATEK', 'FRP_STANDARD', false, '[1,2,4,5,7,8,10,11,13,17,18]', '{"BROM","MTP"}', '22D1', '9008', 'Global', 'untested', 'ColorOS FRP — BROM method'),
('Oppo', 'CPH2575', 'Oppo Reno 11 5G', 'reno11', 'Dimensity 7050', 'MEDIATEK', 'FRP_STANDARD', false, '[1,2,4,5,7,8,10,11,13,17,18]', '{"BROM","MTP"}', '22D1', '9008', 'Global', 'untested', NULL),
('Oppo', 'CPH2493', 'Oppo Reno 10 Pro+', 'reno10pp', 'Snapdragon 8+ Gen 1', 'QUALCOMM', 'FRP_STANDARD', false, '[1,2,4,5,7,8,9,10,11,12,13,17,18]', '{"EDL","MTP"}', '05C6', '9008', 'Global', 'untested', NULL),
('Oppo', 'CPH2473', 'Oppo Reno 10 Pro', 'reno10p', 'Dimensity 8100', 'MEDIATEK', 'FRP_STANDARD', false, '[1,2,4,5,7,8,10,11,13,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'verified_alpha', NULL),
('Oppo', 'CPH2505', 'Oppo A98 5G', 'a98', 'Snapdragon 695', 'QUALCOMM', 'FRP_STANDARD', false, '[1,2,4,7,8,9,10,11,12,17,18]', '{"EDL","MTP"}', '05C6', '9008', 'Global', 'verified_alpha', NULL),
('Oppo', 'CPH2477', 'Oppo A78 5G', 'a78', 'Dimensity 700', 'MEDIATEK', 'FRP_STANDARD', false, '[1,2,4,7,8,10,11,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'verified_alpha', NULL),
('Oppo', 'CPH2495', 'Oppo A58 4G', 'a58', 'Helio G85', 'MEDIATEK', 'FRP_STANDARD', false, '[1,2,4,7,8,10,11,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'stable', NULL),
('Oppo', 'CPH2531', 'Oppo A38', 'a38', 'Helio G85', 'MEDIATEK', 'FRP_STANDARD', false, '[1,2,4,7,8,10,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'stable', NULL),
('Oppo', 'CPH2579', 'Oppo A18', 'a18', 'Helio G85', 'MEDIATEK', 'FRP_STANDARD', false, '[1,2,4,7,8,10,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'stable', NULL),
('Oppo', 'CPH2269', 'Oppo A16', 'a16', 'Helio G35', 'MEDIATEK', 'FRP_STANDARD', false, '[1,2,4,7,8,10,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'stable', 'Budget easy FRP'),
('Oppo', 'CPH2387', 'Oppo Find X5 Pro', 'findx5p', 'Snapdragon 8 Gen 1', 'QUALCOMM', 'FRP_HARDENED', false, '[1,4,7,8,9,10,11,12,13,17,18,20,21,23]', '{"EDL","MTP"}', '05C6', '9008', 'Global', 'untested', 'Flagship — locked'),
('Oppo', 'CPH2449', 'Oppo Find N2 Flip', 'findn2f', 'Dimensity 9000+', 'MEDIATEK', 'FRP_HARDENED', false, '[1,4,7,8,10,11,13,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'untested', 'Foldable'),
('Oppo', 'CPH2339', 'Oppo Reno 8 Pro', 'reno8p', 'Dimensity 8100-MAX', 'MEDIATEK', 'FRP_STANDARD', false, '[1,2,4,5,7,8,10,11,13,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'verified_alpha', NULL),
('Oppo', 'CPH2271', 'Oppo Reno 7', 'reno7', 'Snapdragon 680', 'QUALCOMM', 'FRP_STANDARD', false, '[1,2,4,7,8,9,10,11,12,17,18]', '{"EDL","MTP"}', '05C6', '9008', 'Global', 'stable', NULL),
('Oppo', 'CPH2363', 'Oppo Reno 8', 'reno8', 'Dimensity 1300', 'MEDIATEK', 'FRP_STANDARD', false, '[1,2,4,5,7,8,10,11,13,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'verified_alpha', NULL),
('Oppo', 'CPH2159', 'Oppo A53', 'a53oppo', 'Snapdragon 460', 'QUALCOMM', 'FRP_STANDARD', false, '[1,2,4,7,8,9,10,17,18]', '{"EDL","MTP"}', '05C6', '9008', 'Global', 'stable', NULL),
('Oppo', 'CPH2185', 'Oppo A15', 'a15', 'Helio P35', 'MEDIATEK', 'FRP_STANDARD', false, '[1,2,4,7,8,10,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'stable', NULL),
('Oppo', 'CPH2211', 'Oppo A55', 'a55oppo', 'Helio G35', 'MEDIATEK', 'FRP_STANDARD', false, '[1,2,4,7,8,10,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'stable', NULL),
('Oppo', 'CPH2613', 'Oppo A3 Pro', 'a3pro', 'Dimensity 7050', 'MEDIATEK', 'FRP_STANDARD', false, '[1,2,4,5,7,8,10,11,13,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'untested', NULL),
('Oppo', 'CPH2557', 'Oppo Find X6 Pro', 'findx6p', 'Snapdragon 8 Gen 2', 'QUALCOMM', 'FRP_HARDENED', false, '[1,4,7,8,9,10,11,12,13,17,18,20,21,23]', '{"EDL","MTP"}', '05C6', '9008', 'Global', 'untested', NULL),

-- ============================================================================
-- 5. VIVO (20 models)
-- ============================================================================
('Vivo', 'V2322', 'Vivo V30 Pro', 'v30pro', 'Dimensity 8200', 'MEDIATEK', 'FRP_STANDARD', false, '[1,2,4,5,7,8,10,11,13,17,18]', '{"BROM","MTP"}', '2C7C', '0125', 'Global', 'untested', NULL),
('Vivo', 'V2320', 'Vivo V30', 'v30', 'Snapdragon 7 Gen 3', 'QUALCOMM', 'FRP_STANDARD', false, '[1,2,4,7,8,9,10,11,12,17,18]', '{"EDL","MTP"}', '05C6', '9008', 'Global', 'untested', NULL),
('Vivo', 'V2254', 'Vivo V29 Pro', 'v29p', 'Dimensity 8200', 'MEDIATEK', 'FRP_STANDARD', false, '[1,2,4,5,7,8,10,11,13,17,18]', '{"BROM","MTP"}', '2C7C', '0125', 'Global', 'untested', NULL),
('Vivo', 'V2250', 'Vivo V29', 'v29', 'Snapdragon 778G', 'QUALCOMM', 'FRP_STANDARD', false, '[1,2,4,7,8,9,10,11,12,17,18]', '{"EDL","MTP"}', '05C6', '9008', 'Global', 'verified_alpha', NULL),
('Vivo', 'V2217', 'Vivo Y36', 'y36', 'Snapdragon 680', 'QUALCOMM', 'FRP_STANDARD', false, '[1,2,4,7,8,9,10,17,18]', '{"EDL","MTP"}', '05C6', '9008', 'Global', 'stable', NULL),
('Vivo', 'V2248', 'Vivo Y27 5G', 'y27', 'Dimensity 6020', 'MEDIATEK', 'FRP_STANDARD', false, '[1,2,4,7,8,10,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'verified_alpha', NULL),
('Vivo', 'V2234', 'Vivo Y17s', 'y17s', 'Helio G85', 'MEDIATEK', 'FRP_STANDARD', false, '[1,2,4,7,8,10,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'stable', NULL),
('Vivo', 'V2207', 'Vivo Y02', 'y02', 'Helio P35', 'MEDIATEK', 'FRP_STANDARD', false, '[1,2,4,7,8,10,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'stable', 'Budget — easy BROM'),
('Vivo', 'V2203', 'Vivo Y16', 'y16', 'Helio P35', 'MEDIATEK', 'FRP_STANDARD', false, '[1,2,4,7,8,10,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'stable', NULL),
('Vivo', 'V2120', 'Vivo Y21', 'y21vivo', 'Helio P35', 'MEDIATEK', 'FRP_STANDARD', false, '[1,2,4,7,8,10,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'stable', NULL),
('Vivo', 'V2303', 'Vivo X90 Pro', 'x90pro', 'Dimensity 9200', 'MEDIATEK', 'FRP_HARDENED', false, '[1,4,7,8,10,11,13,17,18,20,21,23]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'untested', 'Flagship'),
('Vivo', 'V2326', 'Vivo X100 Pro', 'x100pro', 'Dimensity 9300', 'MEDIATEK', 'FRP_HARDENED', false, '[1,4,7,8,10,11,13,17,18,20,21,23]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'untested', 'Flagship'),
('Vivo', 'V2244', 'Vivo T2 5G', 't2vivo', 'Snapdragon 695', 'QUALCOMM', 'FRP_STANDARD', false, '[1,2,4,7,8,9,10,11,12,17,18]', '{"EDL","MTP"}', '05C6', '9008', 'Global', 'verified_alpha', NULL),
('Vivo', 'V2259', 'Vivo T2x 5G', 't2x', 'Dimensity 6020', 'MEDIATEK', 'FRP_STANDARD', false, '[1,2,4,7,8,10,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'verified_alpha', NULL),
('Vivo', 'V2111', 'Vivo Y20', 'y20', 'Snapdragon 460', 'QUALCOMM', 'FRP_STANDARD', false, '[1,2,4,7,8,9,10,17,18]', '{"EDL","MTP"}', '05C6', '9008', 'Global', 'stable', NULL),
('Vivo', 'V2236', 'Vivo Y56 5G', 'y56', 'Dimensity 700', 'MEDIATEK', 'FRP_STANDARD', false, '[1,2,4,7,8,10,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'verified_alpha', NULL),
('Vivo', 'V2310', 'Vivo V27 Pro', 'v27pro', 'Dimensity 8200', 'MEDIATEK', 'FRP_STANDARD', false, '[1,2,4,5,7,8,10,11,13,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'verified_alpha', NULL),
('Vivo', 'V2219', 'Vivo Y100', 'y100', 'Snapdragon 695', 'QUALCOMM', 'FRP_STANDARD', false, '[1,2,4,7,8,9,10,11,12,17,18]', '{"EDL","MTP"}', '05C6', '9008', 'Global', 'verified_alpha', NULL),
('Vivo', 'V2313', 'Vivo iQOO Neo 8', 'neo8', 'Snapdragon 8+ Gen 1', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,9,10,11,12,13,17,18,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'China', 'untested', NULL),
('Vivo', 'V2130', 'Vivo iQOO Z6', 'z6', 'Snapdragon 778G+', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,9,10,11,12,13,17,18,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'verified_alpha', NULL),

-- ============================================================================
-- 6. INFINIX (20 models — MediaTek dominant)
-- ============================================================================
('Infinix', 'X6871', 'Infinix Note 40 Pro', 'note40p', 'Dimensity 7020', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'untested', NULL),
('Infinix', 'X6851', 'Infinix Note 40', 'note40', 'Helio G99', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'verified_alpha', NULL),
('Infinix', 'X6833B', 'Infinix Note 30 Pro', 'note30p', 'Helio G99', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'stable', NULL),
('Infinix', 'X6837', 'Infinix Note 30', 'note30', 'Helio G99', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'stable', NULL),
('Infinix', 'X6739', 'Infinix Note 12', 'note12', 'Helio G88', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'stable', NULL),
('Infinix', 'X6831', 'Infinix Hot 30i', 'hot30i', 'Helio G25', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'stable', 'Budget — easy FRP'),
('Infinix', 'X6832', 'Infinix Hot 30', 'hot30', 'Helio G88', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'stable', NULL),
('Infinix', 'X6826', 'Infinix Hot 20S', 'hot20s', 'Helio G96', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'stable', NULL),
('Infinix', 'X6815', 'Infinix Hot 20', 'hot20', 'Helio G85', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,11,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'stable', NULL),
('Infinix', 'X6711', 'Infinix Smart 7', 'smart7', 'Helio A22', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'stable', 'Ultra-budget — BROM default'),
('Infinix', 'X6823', 'Infinix Smart 8', 'smart8', 'Helio G36', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'stable', NULL),
('Infinix', 'X6515', 'Infinix Hot 12', 'hot12', 'Helio G85', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,11,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'stable', NULL),
('Infinix', 'X6525', 'Infinix Hot 12 Play', 'hot12p', 'UNISOC T610', 'UNISOC', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"SPD","MTP"}', '1782', '4D00', 'Global', 'verified_alpha', 'UNISOC variant'),
('Infinix', 'X6511B', 'Infinix Hot 11', 'hot11', 'Helio G70', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'stable', NULL),
('Infinix', 'X6817', 'Infinix Zero 30 5G', 'zero30', 'Dimensity 8020', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'verified_alpha', NULL),
('Infinix', 'X6821', 'Infinix GT 10 Pro', 'gt10p', 'Dimensity 8050', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'verified_alpha', NULL),
('Infinix', 'X6819', 'Infinix Zero 5G', 'zero5g', 'Dimensity 900', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'verified_alpha', NULL),
('Infinix', 'X6816C', 'Infinix Hot 30 Play', 'hot30play', 'UNISOC T606', 'UNISOC', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"SPD","MTP"}', '1782', '4D00', 'Global', 'verified_alpha', 'UNISOC variant'),
('Infinix', 'X6850', 'Infinix Note 30 5G', 'note30_5g', 'Dimensity 6080', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'verified_alpha', NULL),
('Infinix', 'X6835', 'Infinix Note 30 VIP', 'note30vip', 'Dimensity 8050', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,20,21,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'untested', NULL),

-- ============================================================================
-- 7. TECNO (20 models — MediaTek dominant)
-- ============================================================================
('Tecno', 'CK9n', 'Tecno Camon 30 Pro', 'camon30p', 'Dimensity 8200', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'untested', NULL),
('Tecno', 'CK8n', 'Tecno Camon 30', 'camon30', 'Helio G99', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'verified_alpha', NULL),
('Tecno', 'CK7n', 'Tecno Camon 20 Pro', 'camon20p', 'Dimensity 8050', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'verified_alpha', NULL),
('Tecno', 'CK6n', 'Tecno Camon 20', 'camon20', 'Helio G85', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,11,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'stable', NULL),
('Tecno', 'CI8', 'Tecno Camon 19 Pro', 'camon19p', 'Helio G96', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'stable', NULL),
('Tecno', 'AD10', 'Tecno Phantom X2 Pro', 'phantomx2p', 'Dimensity 9000', 'MEDIATEK', 'FRP_HARDENED', false, '[1,4,7,8,10,11,13,17,18,20,21]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'untested', 'Flagship — hardened'),
('Tecno', 'AD9', 'Tecno Phantom X2', 'phantomx2', 'Dimensity 9000', 'MEDIATEK', 'FRP_HARDENED', false, '[1,4,7,8,10,11,13,17,18,20,21]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'untested', NULL),
('Tecno', 'BG7n', 'Tecno Spark 20 Pro+', 'spark20pp', 'Helio G99', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'verified_alpha', NULL),
('Tecno', 'BG7', 'Tecno Spark 20 Pro', 'spark20p', 'Helio G85', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,11,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'verified_alpha', NULL),
('Tecno', 'BG6', 'Tecno Spark 20', 'spark20', 'Helio G85', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,11,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'stable', NULL),
('Tecno', 'BF7n', 'Tecno Spark 10 Pro', 'spark10p', 'Helio G88', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'stable', NULL),
('Tecno', 'BF8n', 'Tecno Spark 10', 'spark10', 'Helio G85', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,11,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'stable', NULL),
('Tecno', 'KI5k', 'Tecno Spark 9 Pro', 'spark9p', 'Helio G85', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,11,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'stable', NULL),
('Tecno', 'KG5p', 'Tecno Pop 7 Pro', 'pop7p', 'Helio A22', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'stable', 'Ultra-budget'),
('Tecno', 'KG5j', 'Tecno Pop 7', 'pop7', 'UNISOC SC9832E', 'UNISOC', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"SPD","MTP"}', '1782', '4D00', 'Global', 'stable', 'UNISOC variant'),
('Tecno', 'BG5n', 'Tecno Spark Go 2024', 'sparkgo24', 'Helio A22', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'stable', NULL),
('Tecno', 'AD8', 'Tecno Phantom V Fold', 'phantomvf', 'Dimensity 9000+', 'MEDIATEK', 'FRP_HARDENED', false, '[1,4,7,8,10,11,13,17,18,20,21]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'untested', 'Foldable flagship'),
('Tecno', 'CK8', 'Tecno Camon 20 Premier', 'camon20pr', 'Dimensity 8050', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'untested', NULL),
('Tecno', 'CH9n', 'Tecno Pova 5 Pro', 'pova5p', 'Dimensity 6080', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'verified_alpha', NULL),
('Tecno', 'CH6', 'Tecno Pova 5', 'pova5', 'Helio G99', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'stable', NULL),

-- ============================================================================
-- 8. NOKIA (15 models — Qualcomm + UNISOC)
-- ============================================================================
('Nokia', 'TA-1604', 'Nokia G42 5G', 'g42', 'Snapdragon 480+', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'verified_alpha', 'Android One — clean bootloader unlock'),
('Nokia', 'TA-1581', 'Nokia G22', 'g22', 'UNISOC T606', 'UNISOC', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"SPD","FASTBOOT","MTP"}', '1782', '4D00', 'Global', 'verified_alpha', 'UNISOC SPD protocol'),
('Nokia', 'TA-1568', 'Nokia C32', 'c32', 'UNISOC SC9863A1', 'UNISOC', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"SPD","MTP"}', '1782', '4D00', 'Global', 'stable', NULL),
('Nokia', 'TA-1557', 'Nokia C12', 'c12', 'UNISOC SC9863A1', 'UNISOC', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"SPD","MTP"}', '1782', '4D00', 'Global', 'stable', 'Budget UNISOC'),
('Nokia', 'TA-1527', 'Nokia G60 5G', 'g60', 'Snapdragon 695', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'stable', NULL),
('Nokia', 'TA-1484', 'Nokia G21', 'g21', 'UNISOC T606', 'UNISOC', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"SPD","FASTBOOT","MTP"}', '1782', '4D00', 'Global', 'stable', NULL),
('Nokia', 'TA-1459', 'Nokia X30 5G', 'x30', 'Snapdragon 695', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'verified_alpha', NULL),
('Nokia', 'TA-1401', 'Nokia G50 5G', 'g50', 'Snapdragon 480', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,9,10,11,12,13,17,18,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'stable', NULL),
('Nokia', 'TA-1370', 'Nokia X20', 'x20', 'Snapdragon 480', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,9,10,11,12,13,17,18,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'stable', NULL),
('Nokia', 'TA-1607', 'Nokia G310 5G', 'g310', 'Snapdragon 480+', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,9,10,11,12,13,17,18,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'untested', NULL),
('Nokia', 'TA-1613', 'Nokia C210', 'c210', 'Helio G85', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'untested', NULL),
('Nokia', 'TA-1543', 'Nokia C110', 'c110', 'UNISOC SC9863A', 'UNISOC', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"SPD","MTP"}', '1782', '4D00', 'Global', 'stable', NULL),
('Nokia', 'TA-1591', 'Nokia G42 5G US', 'g42us', 'Snapdragon 480+', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,9,10,11,12,13,17,18,22]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'US', 'untested', 'US variant'),
('Nokia', 'TA-1505', 'Nokia 2660 Flip', '2660flip', 'UNISOC T107', 'UNISOC', 'NO_FRP', false, '[8,17,18]', '{"SPD"}', '1782', '4D00', 'Global', 'stable', 'Feature phone — limited ops'),
('Nokia', 'TA-1615', 'Nokia G100', 'g100', 'Snapdragon 695', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,9,10,11,12,13,17,18,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'US', 'untested', NULL),

-- ============================================================================
-- 9. MOTOROLA (20 models — Qualcomm dominant)
-- ============================================================================
('Motorola', 'XT2347-2', 'Moto G84 5G', 'g84', 'Snapdragon 695', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '22B8', '2E83', 'Global', 'verified_alpha', 'Clean Android — easy BL unlock'),
('Motorola', 'XT2345-3', 'Moto G54 5G', 'g54', 'Dimensity 7020', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '22B8', '2E83', 'Global', 'verified_alpha', 'MTK variant'),
('Motorola', 'XT2343-3', 'Moto G34 5G', 'g34', 'Snapdragon 695', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,9,10,11,12,13,17,18,22,24]', '{"EDL","FASTBOOT","MTP"}', '22B8', '2E83', 'Global', 'verified_alpha', NULL),
('Motorola', 'XT2341-4', 'Moto G24', 'g24', 'Helio G85', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,11,17,18]', '{"BROM","FASTBOOT","MTP"}', '22B8', '2E83', 'Global', 'verified_alpha', NULL),
('Motorola', 'XT2333-3', 'Moto G73 5G', 'g73', 'Dimensity 930', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '22B8', '2E83', 'Global', 'stable', NULL),
('Motorola', 'XT2331-3', 'Moto G53 5G', 'g53', 'Snapdragon 480+', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,9,10,11,12,13,17,18,22,24]', '{"EDL","FASTBOOT","MTP"}', '22B8', '2E83', 'Global', 'stable', NULL),
('Motorola', 'XT2321-3', 'Moto G13', 'g13', 'Helio G85', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,11,17,18]', '{"BROM","FASTBOOT","MTP"}', '22B8', '2E83', 'Global', 'stable', NULL),
('Motorola', 'XT2319-1', 'Moto G23', 'g23', 'Helio G85', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,11,17,18]', '{"BROM","FASTBOOT","MTP"}', '22B8', '2E83', 'Global', 'stable', NULL),
('Motorola', 'XT2351-1', 'Moto Edge 40 Pro', 'edge40p', 'Snapdragon 8 Gen 2', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '22B8', '2E83', 'Global', 'untested', 'Flagship'),
('Motorola', 'XT2349-2', 'Moto Edge 40', 'edge40', 'Dimensity 8020', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '22B8', '2E83', 'Global', 'untested', NULL),
('Motorola', 'XT2357-1', 'Moto Razr 40 Ultra', 'razr40u', 'Snapdragon 8+ Gen 1', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,9,10,11,12,13,17,18,22,24]', '{"EDL","FASTBOOT","MTP"}', '22B8', '2E83', 'Global', 'untested', 'Foldable'),
('Motorola', 'XT2363-3', 'Moto Edge 50 Pro', 'edge50p', 'Snapdragon 7 Gen 3', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '22B8', '2E83', 'Global', 'untested', NULL),
('Motorola', 'XT2301-4', 'Moto G62 5G', 'g62', 'Snapdragon 695', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,9,10,11,12,13,17,18,22,24]', '{"EDL","FASTBOOT","MTP"}', '22B8', '2E83', 'Global', 'stable', NULL),
('Motorola', 'XT2235-2', 'Moto G Stylus 5G (2023)', 'gstylus23', 'Snapdragon 6 Gen 1', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,9,10,11,12,13,17,18,22,24]', '{"EDL","FASTBOOT","MTP"}', '22B8', '2E83', 'US', 'untested', NULL),
('Motorola', 'XT2243-1', 'Moto G Power (2023)', 'gpower23', 'Dimensity 930', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '22B8', '2E83', 'US', 'untested', NULL),
('Motorola', 'XT2255-1', 'Moto G Play (2024)', 'gplay24', 'Snapdragon 680', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,9,10,11,12,13,17,18,22,24]', '{"EDL","FASTBOOT","MTP"}', '22B8', '2E83', 'US', 'untested', NULL),
('Motorola', 'XT2153-1', 'Moto G Stylus (2021)', 'gstylus21', 'Snapdragon 678', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,9,10,11,12,13,17,18,22,24]', '{"EDL","FASTBOOT","MTP"}', '22B8', '2E83', 'US', 'stable', NULL),
('Motorola', 'XT2201-1', 'Moto Edge 30 Pro', 'edge30p', 'Snapdragon 8 Gen 1', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '22B8', '2E83', 'Global', 'verified_alpha', NULL),
('Motorola', 'XT2175-2', 'Moto E32', 'e32', 'Helio G37', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"BROM","MTP"}', '22B8', '2E83', 'Global', 'stable', NULL),
('Motorola', 'XT2369-2', 'ThinkPhone', 'thinkphone', 'Snapdragon 8+ Gen 1', 'QUALCOMM', 'FRP_HARDENED', false, '[1,4,7,8,9,10,11,12,13,17,18,20,21]', '{"EDL","FASTBOOT","MTP"}', '22B8', '2E83', 'Global', 'untested', 'Enterprise — locked BL'),

-- ============================================================================
-- 10. HUAWEI (15 models — Kirin)
-- ============================================================================
('Huawei', 'ABR-LX9', 'Huawei P50 Pro', 'p50pro', 'Kirin 9000', 'KIRIN', 'FRP_HARDENED', false, '[1,4,7,8,10,13,17,18,20,21]', '{"HUAWEI_UPDATE","MTP"}', '12D1', '107E', 'Global', 'untested', 'No GMS — AppGallery only'),
('Huawei', 'NOH-LX9', 'Huawei Mate 40 Pro', 'mate40p', 'Kirin 9000', 'KIRIN', 'FRP_HARDENED', false, '[1,4,7,8,10,13,17,18,20,21]', '{"HUAWEI_UPDATE","MTP"}', '12D1', '107E', 'Global', 'untested', NULL),
('Huawei', 'JLN-LX1', 'Huawei Nova 12i', 'nova12i', 'Snapdragon 680', 'QUALCOMM', 'FRP_HARDENED', false, '[1,4,7,8,10,17,18]', '{"EDL","MTP"}', '05C6', '9008', 'Global', 'untested', 'QC variant'),
('Huawei', 'CET-LX9', 'Huawei Nova 11', 'nova11', 'Snapdragon 778G', 'QUALCOMM', 'FRP_HARDENED', false, '[1,4,7,8,9,10,17,18]', '{"EDL","MTP"}', '05C6', '9008', 'Global', 'untested', NULL),
('Huawei', 'BNE-LX1', 'Huawei Nova Y70', 'novay70', 'Kirin 710A', 'KIRIN', 'FRP_STANDARD', false, '[1,2,4,7,8,10,17,18]', '{"HUAWEI_UPDATE","MTP"}', '12D1', '107E', 'Global', 'verified_alpha', NULL),
('Huawei', 'DBY-W09', 'Huawei MatePad 11.5', 'matepad115', 'Snapdragon 7 Gen 1', 'QUALCOMM', 'FRP_HARDENED', false, '[1,4,7,8,10,17,18]', '{"EDL","MTP"}', '05C6', '9008', 'Global', 'untested', 'Tablet'),
('Huawei', 'STK-LX1', 'Huawei Y9 Prime (2019)', 'y9p19', 'Kirin 710F', 'KIRIN', 'FRP_STANDARD', false, '[1,2,4,7,8,10,17,18]', '{"HUAWEI_UPDATE","MTP"}', '12D1', '107E', 'Global', 'stable', NULL),
('Huawei', 'MRD-LX1', 'Huawei Y6p', 'y6p', 'Helio P22', 'MEDIATEK', 'FRP_STANDARD', false, '[1,2,4,7,8,10,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'stable', 'MTK variant'),
('Huawei', 'DRA-LX5', 'Huawei Y5 Lite', 'y5lite', 'Helio A22', 'MEDIATEK', 'FRP_STANDARD', false, '[1,2,4,7,8,10,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'stable', NULL),
('Huawei', 'ALT-L29', 'Huawei Enjoy 60 Pro', 'enjoy60p', 'Kirin 710A', 'KIRIN', 'FRP_STANDARD', false, '[1,2,4,7,8,10,17,18]', '{"HUAWEI_UPDATE","MTP"}', '12D1', '107E', 'Global', 'untested', NULL),
('Huawei', 'YAL-L21', 'Huawei P30', 'p30', 'Kirin 980', 'KIRIN', 'FRP_STANDARD', false, '[1,2,4,7,8,10,13,17,18,20,21]', '{"HUAWEI_UPDATE","MTP"}', '12D1', '107E', 'Global', 'stable', 'Older — FRP well understood'),
('Huawei', 'ELS-NX9', 'Huawei P40 Pro', 'p40pro', 'Kirin 990 5G', 'KIRIN', 'FRP_HARDENED', false, '[1,4,7,8,10,13,17,18,20,21]', '{"HUAWEI_UPDATE","MTP"}', '12D1', '107E', 'Global', 'verified_alpha', NULL),
('Huawei', 'ANA-NX9', 'Huawei P40', 'p40', 'Kirin 990 5G', 'KIRIN', 'FRP_HARDENED', false, '[1,4,7,8,10,13,17,18,20,21]', '{"HUAWEI_UPDATE","MTP"}', '12D1', '107E', 'Global', 'verified_alpha', NULL),
('Huawei', 'LIO-L29', 'Huawei Mate 30 Pro', 'mate30p', 'Kirin 990', 'KIRIN', 'FRP_HARDENED', false, '[1,4,7,8,10,13,17,18,20,21]', '{"HUAWEI_UPDATE","MTP"}', '12D1', '107E', 'Global', 'verified_alpha', NULL),
('Huawei', 'ALN-AL00', 'Huawei Mate 60 Pro', 'mate60p', 'Kirin 9000s', 'KIRIN', 'FRP_HARDENED', false, '[8,17,18]', '{"HUAWEI_UPDATE","MTP"}', '12D1', '107E', 'China', 'untested', 'HarmonyOS 4 — heavily locked'),

-- ============================================================================
-- 11. HONOR (12 models)
-- ============================================================================
('Honor', 'ANY-LX1', 'Honor Magic6 Pro', 'magic6pro', 'Snapdragon 8 Gen 3', 'QUALCOMM', 'FRP_HARDENED', false, '[1,4,7,8,9,10,13,17,18,20,21]', '{"EDL","MTP"}', '05C6', '9008', 'Global', 'untested', NULL),
('Honor', 'CRT-LX1', 'Honor 90', 'honor90', 'Snapdragon 7 Gen 1', 'QUALCOMM', 'FRP_STANDARD', false, '[1,2,4,7,8,9,10,17,18]', '{"EDL","MTP"}', '05C6', '9008', 'Global', 'untested', NULL),
('Honor', 'CRT-LX3', 'Honor 90 Lite', 'honor90l', 'Dimensity 6020', 'MEDIATEK', 'FRP_STANDARD', false, '[1,2,4,7,8,10,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'untested', NULL),
('Honor', 'RMO-NX1', 'Honor X9b', 'x9b', 'Snapdragon 6 Gen 1', 'QUALCOMM', 'FRP_STANDARD', false, '[1,2,4,7,8,9,10,11,17,18]', '{"EDL","MTP"}', '05C6', '9008', 'Global', 'verified_alpha', NULL),
('Honor', 'MBN-LX9', 'Honor X8b', 'x8b', 'Snapdragon 680', 'QUALCOMM', 'FRP_STANDARD', false, '[1,2,4,7,8,9,10,17,18]', '{"EDL","MTP"}', '05C6', '9008', 'Global', 'verified_alpha', NULL),
('Honor', 'CRT-NX1', 'Honor X7b', 'x7b', 'Snapdragon 680', 'QUALCOMM', 'FRP_STANDARD', false, '[1,2,4,7,8,9,10,17,18]', '{"EDL","MTP"}', '05C6', '9008', 'Global', 'stable', NULL),
('Honor', 'WDY-LX1', 'Honor X6a', 'x6a', 'Helio G36', 'MEDIATEK', 'FRP_STANDARD', false, '[1,2,4,7,8,10,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'stable', NULL),
('Honor', 'TNA-LX2', 'Honor 200 Pro', 'h200pro', 'Snapdragon 8s Gen 3', 'QUALCOMM', 'FRP_STANDARD', false, '[1,2,4,7,8,9,10,11,13,17,18]', '{"EDL","MTP"}', '05C6', '9008', 'Global', 'untested', NULL),
('Honor', 'REA-NX9', 'Honor 200', 'h200', 'Snapdragon 7 Gen 3', 'QUALCOMM', 'FRP_STANDARD', false, '[1,2,4,7,8,9,10,11,17,18]', '{"EDL","MTP"}', '05C6', '9008', 'Global', 'untested', NULL),
('Honor', 'REA-AN00', 'Honor Magic V2', 'magicv2', 'Snapdragon 8 Gen 2', 'QUALCOMM', 'FRP_HARDENED', false, '[1,4,7,8,9,10,13,17,18,20,21]', '{"EDL","MTP"}', '05C6', '9008', 'Global', 'untested', 'Foldable'),
('Honor', 'KAN-LX1', 'Honor X8', 'x8', 'Snapdragon 680', 'QUALCOMM', 'FRP_STANDARD', false, '[1,2,4,7,8,9,10,17,18]', '{"EDL","MTP"}', '05C6', '9008', 'Global', 'stable', NULL),
('Honor', 'NTN-LX1', 'Honor 70', 'h70', 'Snapdragon 778G+', 'QUALCOMM', 'FRP_STANDARD', false, '[1,2,4,7,8,9,10,11,13,17,18]', '{"EDL","MTP"}', '05C6', '9008', 'Global', 'stable', NULL),

-- ============================================================================
-- 12. ONEPLUS (12 models)
-- ============================================================================
('OnePlus', 'CPH2583', 'OnePlus 12', 'waffle', 'Snapdragon 8 Gen 3', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'untested', 'OxygenOS — full EDL'),
('OnePlus', 'CPH2449', 'OnePlus 11', 'salami', 'Snapdragon 8 Gen 2', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'verified_alpha', NULL),
('OnePlus', 'CPH2399', 'OnePlus 10 Pro', 'negev', 'Snapdragon 8 Gen 1', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'stable', NULL),
('OnePlus', 'LE2125', 'OnePlus 9 Pro', 'lemonade', 'Snapdragon 888', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'stable', NULL),
('OnePlus', 'CPH2587', 'OnePlus 12R', 'aston12r', 'Snapdragon 8 Gen 2', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'untested', NULL),
('OnePlus', 'CPH2515', 'OnePlus Nord 3', 'larry', 'Dimensity 9000', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'verified_alpha', NULL),
('OnePlus', 'CPH2493', 'OnePlus Nord CE 3', 'ivan', 'Snapdragon 782G', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,9,10,11,12,13,17,18,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'verified_alpha', NULL),
('OnePlus', 'CPH2569', 'OnePlus Nord CE 4', 'ivance4', 'Snapdragon 7 Gen 3', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,9,10,11,12,13,17,18,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'untested', NULL),
('OnePlus', 'CPH2541', 'OnePlus Nord N30 5G', 'nordn30', 'Snapdragon 695', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,9,10,11,12,13,17,18,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'US', 'verified_alpha', NULL),
('OnePlus', 'IV2201', 'OnePlus Nord 2T', 'nord2t', 'Dimensity 1300', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'stable', NULL),
('OnePlus', 'BE2025', 'OnePlus Nord N10 5G', 'nordn10', 'Snapdragon 690', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,9,10,11,12,13,17,18,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'stable', NULL),
('OnePlus', 'CPH2611', 'OnePlus Open', 'opopen', 'Snapdragon 8 Gen 2', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'untested', 'Foldable'),

-- ============================================================================
-- 13. GOOGLE PIXEL (12 models — Tensor/Qualcomm)
-- ============================================================================
('Google', 'GC3VE', 'Pixel 9 Pro XL', 'komodo', 'Tensor G4', 'TENSOR', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,23,24]', '{"FASTBOOT","MTP"}', '18D1', '4EE7', 'Global', 'untested', 'Tensor — fastboot only'),
('Google', 'G4GDJ', 'Pixel 9 Pro', 'caiman', 'Tensor G4', 'TENSOR', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,23,24]', '{"FASTBOOT","MTP"}', '18D1', '4EE7', 'Global', 'untested', NULL),
('Google', 'G9FPL', 'Pixel 9', 'tokay', 'Tensor G4', 'TENSOR', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,23,24]', '{"FASTBOOT","MTP"}', '18D1', '4EE7', 'Global', 'untested', NULL),
('Google', 'GJQ4F', 'Pixel 8 Pro', 'husky', 'Tensor G3', 'TENSOR', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,23,24]', '{"FASTBOOT","MTP"}', '18D1', '4EE7', 'Global', 'verified_alpha', NULL),
('Google', 'G9BQD', 'Pixel 8', 'shiba', 'Tensor G3', 'TENSOR', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,23,24]', '{"FASTBOOT","MTP"}', '18D1', '4EE7', 'Global', 'verified_alpha', NULL),
('Google', 'GKWS6', 'Pixel 8a', 'akita', 'Tensor G3', 'TENSOR', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,23,24]', '{"FASTBOOT","MTP"}', '18D1', '4EE7', 'Global', 'untested', NULL),
('Google', 'GVU6C', 'Pixel 7 Pro', 'cheetah', 'Tensor G2', 'TENSOR', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,23,24]', '{"FASTBOOT","MTP"}', '18D1', '4EE7', 'Global', 'stable', NULL),
('Google', 'GQML3', 'Pixel 7', 'panther', 'Tensor G2', 'TENSOR', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,23,24]', '{"FASTBOOT","MTP"}', '18D1', '4EE7', 'Global', 'stable', NULL),
('Google', 'GE2AE', 'Pixel 7a', 'lynx', 'Tensor G2', 'TENSOR', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,23,24]', '{"FASTBOOT","MTP"}', '18D1', '4EE7', 'Global', 'stable', NULL),
('Google', 'GX7AS', 'Pixel 6 Pro', 'raven', 'Tensor G1', 'TENSOR', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,23,24]', '{"FASTBOOT","MTP"}', '18D1', '4EE7', 'Global', 'stable', NULL),
('Google', 'GR1YH', 'Pixel 6a', 'bluejay', 'Tensor G1', 'TENSOR', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,23,24]', '{"FASTBOOT","MTP"}', '18D1', '4EE7', 'Global', 'stable', NULL),
('Google', 'GD1YQ', 'Pixel 5a', 'barbet', 'Snapdragon 765G', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '18D1', '4EE7', 'Global', 'stable', NULL),

-- ============================================================================
-- 14. NOTHING (5 models)
-- ============================================================================
('Nothing', 'A063', 'Nothing Phone (2a)', 'pacman2a', 'Dimensity 7200 Pro', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'untested', NULL),
('Nothing', 'A065', 'Nothing Phone (2)', 'pong', 'Snapdragon 8+ Gen 1', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'verified_alpha', NULL),
('Nothing', 'A063P', 'Nothing Phone (1)', 'spacewar', 'Snapdragon 778G+', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'stable', NULL),
('Nothing', 'B065', 'Nothing Phone (2a) Plus', 'pacman2ap', 'Dimensity 7350 Pro', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'untested', NULL),
('Nothing', 'A142', 'Nothing CMF Phone 1', 'cmf1', 'Dimensity 7300', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'untested', NULL),

-- ============================================================================
-- 15. SONY (8 models)
-- ============================================================================
('Sony', 'XQ-DQ72', 'Xperia 1 VI', 'pdx245', 'Snapdragon 8 Gen 3', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '0FCE', 'B00B', 'Global', 'untested', 'Sony Newflasher compatible'),
('Sony', 'XQ-DQ54', 'Xperia 5 V', 'pdx237', 'Snapdragon 8 Gen 2', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '0FCE', 'B00B', 'Global', 'verified_alpha', NULL),
('Sony', 'XQ-CQ72', 'Xperia 10 V', 'pdx234', 'Snapdragon 695', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,9,10,11,12,13,17,18,22,24]', '{"EDL","FASTBOOT","MTP"}', '0FCE', 'B00B', 'Global', 'verified_alpha', NULL),
('Sony', 'XQ-CT54', 'Xperia 1 V', 'pdx234v', 'Snapdragon 8 Gen 2', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '0FCE', 'B00B', 'Global', 'verified_alpha', NULL),
('Sony', 'XQ-BQ72', 'Xperia 1 IV', 'pdx223', 'Snapdragon 8 Gen 1', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '0FCE', 'B00B', 'Global', 'stable', NULL),
('Sony', 'XQ-BC72', 'Xperia 5 IV', 'pdx224', 'Snapdragon 8 Gen 1', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '0FCE', 'B00B', 'Global', 'stable', NULL),
('Sony', 'XQ-CC54', 'Xperia 10 IV', 'pdx225', 'Snapdragon 695', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,9,10,11,12,13,17,18,22,24]', '{"EDL","FASTBOOT","MTP"}', '0FCE', 'B00B', 'Global', 'stable', NULL),
('Sony', 'XQ-AU52', 'Xperia 1 III', 'pdx215', 'Snapdragon 888', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '0FCE', 'B00B', 'Global', 'stable', NULL),

-- ============================================================================
-- 16. LG (8 models — legacy, mostly Qualcomm)
-- ============================================================================
('LG', 'LM-V600', 'LG V60 ThinQ', 'v60', 'Snapdragon 865', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '1004', '633A', 'Global', 'stable', 'Legacy — LG closed mobile division'),
('LG', 'LM-G900', 'LG Velvet', 'velvet', 'Snapdragon 765G', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,9,10,11,12,13,17,18,22,24]', '{"EDL","FASTBOOT","MTP"}', '1004', '633A', 'Global', 'stable', NULL),
('LG', 'LM-K525', 'LG K62', 'k62', 'Helio P35', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"BROM","MTP"}', '1004', '633A', 'Global', 'stable', NULL),
('LG', 'LM-Q730', 'LG Stylo 6', 'stylo6', 'Helio P35', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"BROM","MTP"}', '1004', '633A', 'US', 'stable', NULL),
('LG', 'LM-G850', 'LG G8X ThinQ', 'g8x', 'Snapdragon 855', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '1004', '633A', 'Global', 'stable', NULL),
('LG', 'LM-V500N', 'LG V50 ThinQ', 'v50', 'Snapdragon 855', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '1004', '633A', 'Global', 'stable', NULL),
('LG', 'LM-K520', 'LG K52', 'k52', 'Helio P35', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"BROM","MTP"}', '1004', '633A', 'Global', 'stable', NULL),
('LG', 'LM-Q710', 'LG Stylo 4', 'stylo4', 'Snapdragon 450', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,7,8,9,10,11,12,17,18,22]', '{"EDL","FASTBOOT","MTP"}', '1004', '633A', 'US', 'stable', NULL),

-- ============================================================================
-- 17. LENOVO (8 models)
-- ============================================================================
('Lenovo', 'TB371FC', 'Lenovo Tab P12 Pro', 'tabp12p', 'Snapdragon 870', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,9,10,11,12,13,17,18,22,24]', '{"EDL","FASTBOOT","MTP"}', '17EF', '7901', 'Global', 'untested', 'Tablet'),
('Lenovo', 'TB370FU', 'Lenovo Tab P11 Pro (2nd)', 'tabp11p2', 'Dimensity 1300T', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '17EF', '7901', 'Global', 'untested', 'Tablet MTK'),
('Lenovo', 'TB350FU', 'Lenovo Tab M10 Plus (3rd)', 'tabm10p3', 'Helio G80', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"BROM","MTP"}', '17EF', '7901', 'Global', 'verified_alpha', 'Tablet'),
('Lenovo', 'L71061', 'Lenovo K14 Plus', 'k14plus', 'Helio G85', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,11,17,18]', '{"BROM","FASTBOOT","MTP"}', '17EF', '7901', 'Global', 'verified_alpha', NULL),
('Lenovo', 'L38111', 'Lenovo Z5 Pro GT', 'z5progt', 'Snapdragon 855', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,22,24]', '{"EDL","FASTBOOT","MTP"}', '17EF', '7901', 'China', 'stable', NULL),
('Lenovo', 'TB-X606F', 'Lenovo Tab M10 FHD Plus', 'tabm10fhd', 'Helio P22T', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"BROM","MTP"}', '17EF', '7901', 'Global', 'stable', 'Popular tablet'),
('Lenovo', 'TB-J616F', 'Lenovo Tab P11', 'tabp11', 'Snapdragon 662', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,7,8,9,10,17,18]', '{"EDL","FASTBOOT","MTP"}', '17EF', '7901', 'Global', 'stable', 'Tablet'),
('Lenovo', 'L78071', 'Lenovo Z6 Pro', 'z6pro', 'Snapdragon 855', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,22,24]', '{"EDL","FASTBOOT","MTP"}', '17EF', '7901', 'China', 'stable', NULL),

-- ============================================================================
-- 18. ZTE (8 models)
-- ============================================================================
('ZTE', 'A2322G', 'ZTE Blade V50 Design', 'bladev50d', 'UNISOC T606', 'UNISOC', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"SPD","MTP"}', '19D2', 'FFFF', 'Global', 'untested', 'UNISOC SPD'),
('ZTE', 'A2322', 'ZTE Blade V50', 'bladev50', 'Dimensity 6020', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,11,17,18]', '{"BROM","MTP"}', '19D2', 'FFFF', 'Global', 'untested', NULL),
('ZTE', 'A2121L', 'ZTE Blade A53', 'bladea53', 'UNISOC SC9863A', 'UNISOC', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"SPD","MTP"}', '19D2', 'FFFF', 'Global', 'stable', NULL),
('ZTE', 'NX731J', 'ZTE Nubia Z60 Ultra', 'z60ultra', 'Snapdragon 8 Gen 3', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'untested', NULL),
('ZTE', 'NX713J', 'ZTE Nubia Z50 Ultra', 'z50ultra', 'Snapdragon 8 Gen 2', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'untested', NULL),
('ZTE', 'NX709J', 'ZTE Nubia Red Magic 8 Pro', 'rm8pro', 'Snapdragon 8 Gen 2', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'verified_alpha', 'Gaming phone'),
('ZTE', 'A2023PG', 'ZTE Blade A73', 'bladea73', 'UNISOC T606', 'UNISOC', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"SPD","MTP"}', '19D2', 'FFFF', 'Global', 'verified_alpha', NULL),
('ZTE', 'A2020G', 'ZTE Blade V40', 'bladev40', 'Dimensity 810', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,11,17,18]', '{"BROM","MTP"}', '19D2', 'FFFF', 'Global', 'verified_alpha', NULL),

-- ============================================================================
-- 19. ASUS (8 models)
-- ============================================================================
('Asus', 'AI2302', 'ROG Phone 8 Pro', 'rog8pro', 'Snapdragon 8 Gen 3', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '0B05', '7775', 'Global', 'untested', 'Gaming phone'),
('Asus', 'AI2205', 'ROG Phone 7', 'rog7', 'Snapdragon 8 Gen 2', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '0B05', '7775', 'Global', 'verified_alpha', NULL),
('Asus', 'AI2401', 'Asus Zenfone 11 Ultra', 'zf11u', 'Snapdragon 8 Gen 3', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '0B05', '7775', 'Global', 'untested', NULL),
('Asus', 'AI2202', 'Asus Zenfone 10', 'zf10', 'Snapdragon 8 Gen 2', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '0B05', '7775', 'Global', 'verified_alpha', NULL),
('Asus', 'AI2103', 'Asus Zenfone 9', 'zf9', 'Snapdragon 8+ Gen 1', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '0B05', '7775', 'Global', 'stable', NULL),
('Asus', 'AI2002', 'ROG Phone 6', 'rog6', 'Snapdragon 8+ Gen 1', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '0B05', '7775', 'Global', 'stable', NULL),
('Asus', 'AI2001', 'ROG Phone 5', 'rog5', 'Snapdragon 888', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '0B05', '7775', 'Global', 'stable', NULL),
('Asus', 'P00I', 'Asus Zenfone Max Pro M1', 'zfmpm1', 'Snapdragon 636', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,9,10,11,12,13,17,18,22,24]', '{"EDL","FASTBOOT","MTP"}', '0B05', '7775', 'Global', 'stable', 'Legacy budget'),

-- ============================================================================
-- 20. ITEL (10 models — MediaTek/UNISOC budget)
-- ============================================================================
('Itel', 'A665L', 'Itel S24', 's24itel', 'Helio G91', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,11,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'untested', NULL),
('Itel', 'A663L', 'Itel P55+', 'p55plus', 'UNISOC T606', 'UNISOC', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"SPD","MTP"}', '1782', '4D00', 'Global', 'untested', NULL),
('Itel', 'A662L', 'Itel P55 5G', 'p55_5g', 'Dimensity 6080', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,11,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'untested', NULL),
('Itel', 'A661L', 'Itel A70', 'a70itel', 'UNISOC SC9832E', 'UNISOC', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"SPD","MTP"}', '1782', '4D00', 'Global', 'stable', 'Ultra-budget'),
('Itel', 'A571L', 'Itel A60', 'a60itel', 'UNISOC SC9832E', 'UNISOC', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"SPD","MTP"}', '1782', '4D00', 'Global', 'stable', NULL),
('Itel', 'A572L', 'Itel S23+', 's23plus_itel', 'Helio G85', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,11,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'verified_alpha', NULL),
('Itel', 'A571W', 'Itel A60s', 'a60s', 'UNISOC SC9863A', 'UNISOC', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"SPD","MTP"}', '1782', '4D00', 'Global', 'stable', NULL),
('Itel', 'P681L', 'Itel Vision 5', 'vision5', 'UNISOC SC9863A', 'UNISOC', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"SPD","MTP"}', '1782', '4D00', 'Global', 'stable', NULL),
('Itel', 'P662L', 'Itel A58', 'a58itel', 'UNISOC SC9832E', 'UNISOC', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"SPD","MTP"}', '1782', '4D00', 'Global', 'stable', NULL),
('Itel', 'S661LN', 'Itel S18 Pro', 's18pro', 'Helio G88', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'untested', NULL),

-- ============================================================================
-- 21. ALCATEL (6 models)
-- ============================================================================
('Alcatel', '6056K', 'Alcatel 3L (2021)', '3l2021', 'Helio P22', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"BROM","MTP"}', '1BBB', '0167', 'Global', 'stable', NULL),
('Alcatel', '5061K', 'Alcatel 1SE', '1se', 'Helio P22', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"BROM","MTP"}', '1BBB', '0167', 'Global', 'stable', NULL),
('Alcatel', '5029Y', 'Alcatel 1V (2020)', '1v2020', 'UNISOC SC9863A', 'UNISOC', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"SPD","MTP"}', '1BBB', '0167', 'Global', 'stable', NULL),
('Alcatel', '6058D', 'Alcatel 3X (2020)', '3x2020', 'Helio P22', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"BROM","MTP"}', '1BBB', '0167', 'Global', 'stable', NULL),
('Alcatel', '5007U', 'Alcatel 1B (2022)', '1b2022', 'UNISOC SC7731E', 'UNISOC', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"SPD","MTP"}', '1BBB', '0167', 'Global', 'stable', NULL),
('Alcatel', '9032T', 'Alcatel Joy Tab 2', 'joytab2', 'Helio P22T', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"BROM","MTP"}', '1BBB', '0167', 'US', 'stable', 'TMobile tablet'),

-- ============================================================================
-- 22. TCL (6 models)
-- ============================================================================
('TCL', 'T609DL', 'TCL 30 5G', 'tcl305g', 'Dimensity 700', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,11,17,18]', '{"BROM","MTP"}', '1BBB', '0200', 'Global', 'verified_alpha', NULL),
('TCL', 'T602DL', 'TCL 30 XE 5G', 'tcl30xe', 'Dimensity 700', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,11,17,18]', '{"BROM","MTP"}', '1BBB', '0200', 'US', 'verified_alpha', NULL),
('TCL', 'T501L', 'TCL 20 SE', 'tcl20se', 'Snapdragon 460', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,7,8,9,10,17,18]', '{"EDL","MTP"}', '05C6', '9008', 'Global', 'stable', NULL),
('TCL', 'T766H', 'TCL 40 SE', 'tcl40se', 'Helio G25', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"BROM","MTP"}', '1BBB', '0200', 'Global', 'stable', NULL),
('TCL', 'T507A1', 'TCL 20S', 'tcl20s', 'Snapdragon 665', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,9,10,11,12,17,18,22]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'US', 'stable', NULL),
('TCL', 'T770H', 'TCL 40 NxtPaper', 'tcl40np', 'UNISOC T612', 'UNISOC', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"SPD","MTP"}', '1782', '4D00', 'Global', 'untested', NULL),

-- ============================================================================
-- 23. WIKO (5 models)
-- ============================================================================
('Wiko', 'W-V860', 'Wiko Power U30', 'poweru30', 'Helio G35', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'stable', NULL),
('Wiko', 'W-V770', 'Wiko View5 Plus', 'view5p', 'Helio P35', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'stable', NULL),
('Wiko', 'W-V880', 'Wiko T60', 't60wiko', 'Helio G85', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,11,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'verified_alpha', NULL),
('Wiko', 'W-V900', 'Wiko T80', 't80wiko', 'Helio G88', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,17,18]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'untested', NULL),
('Wiko', 'W-V851', 'Wiko 10', '10wiko', 'Helio G85', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,11,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'verified_alpha', NULL),

-- ============================================================================
-- 24. MICROMAX (5 models)
-- ============================================================================
('Micromax', 'E7746', 'Micromax IN Note 2', 'innote2', 'Helio G95', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'India', 'stable', NULL),
('Micromax', 'E7544', 'Micromax IN 2b', 'in2b', 'UNISOC T610', 'UNISOC', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"SPD","MTP"}', '1782', '4D00', 'India', 'stable', NULL),
('Micromax', 'E6533', 'Micromax IN 1', 'in1', 'Helio G80', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,11,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'India', 'stable', NULL),
('Micromax', 'E7748', 'Micromax IN Note 1', 'innote1', 'Helio G85', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,11,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'India', 'stable', NULL),
('Micromax', 'E6502', 'Micromax IN 2c', 'in2c', 'UNISOC SC9863A', 'UNISOC', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"SPD","MTP"}', '1782', '4D00', 'India', 'stable', NULL),

-- ============================================================================
-- 25. LAVA (5 models)
-- ============================================================================
('Lava', 'LZY-L01', 'Lava Blaze Pro 5G', 'blazep5g', 'Dimensity 6020', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,11,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'India', 'untested', NULL),
('Lava', 'LZX-L01', 'Lava Blaze 2 5G', 'blaze2_5g', 'Dimensity 6020', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,11,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'India', 'untested', NULL),
('Lava', 'LZ2-L01', 'Lava Yuva 3 Pro', 'yuva3p', 'UNISOC SC9863A', 'UNISOC', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"SPD","MTP"}', '1782', '4D00', 'India', 'stable', NULL),
('Lava', 'LZ1-L01', 'Lava Yuva 2 Pro', 'yuva2p', 'UNISOC SC9863A', 'UNISOC', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"SPD","MTP"}', '1782', '4D00', 'India', 'stable', NULL),
('Lava', 'LE9910', 'Lava Z2 Max', 'z2max', 'Helio A20', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'India', 'stable', NULL),

-- ============================================================================
-- 26. COOLPAD (4 models)
-- ============================================================================
('Coolpad', 'CP12s', 'Coolpad Cool 20s', 'cool20s', 'Helio G85', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,11,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'verified_alpha', NULL),
('Coolpad', 'CP12', 'Coolpad Cool 20', 'cool20', 'Helio G80', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,11,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'stable', NULL),
('Coolpad', 'NX677J', 'Coolpad Note 5', 'note5cp', 'Snapdragon 617', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,7,8,9,10,17,18]', '{"EDL","MTP"}', '05C6', '9008', 'India', 'stable', NULL),
('Coolpad', 'CP01', 'Coolpad SUVA', 'suva', 'Helio A25', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'US', 'stable', 'TMobile budget'),

-- ============================================================================
-- 27. MEIZU (4 models)
-- ============================================================================
('Meizu', 'M2381', 'Meizu 21', 'meizu21', 'Snapdragon 8 Gen 3', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'China', 'untested', NULL),
('Meizu', 'M2282', 'Meizu 20 Pro', 'meizu20p', 'Snapdragon 8 Gen 2', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'China', 'untested', NULL),
('Meizu', 'M973Q', 'Meizu 18 Pro', 'meizu18p', 'Snapdragon 888', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'China', 'stable', NULL),
('Meizu', 'M181Q', 'Meizu Note 9', 'meizun9', 'Snapdragon 675', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,9,10,11,12,13,17,18,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'China', 'stable', NULL),

-- ============================================================================
-- 28. CUBOT (4 models)
-- ============================================================================
('Cubot', 'K70', 'Cubot KingKong Star', 'kkstar', 'Dimensity 8050', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'untested', 'Rugged'),
('Cubot', 'K60', 'Cubot KingKong 9', 'kk9', 'Dimensity 8050', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'untested', 'Rugged'),
('Cubot', 'P80', 'Cubot P80', 'p80cubot', 'Helio G99', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'verified_alpha', NULL),
('Cubot', 'N30', 'Cubot Note 30', 'note30cubot', 'Helio G85', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,11,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'verified_alpha', NULL),

-- ============================================================================
-- 29. DOOGEE (4 models)
-- ============================================================================
('Doogee', 'V30 Pro', 'Doogee V30 Pro', 'v30pdoogee', 'Dimensity 7050', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'untested', 'Rugged'),
('Doogee', 'V30', 'Doogee V30', 'v30doogee', 'Dimensity 900', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'untested', 'Rugged'),
('Doogee', 'S100 Pro', 'Doogee S100 Pro', 's100pro', 'Helio G99', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'verified_alpha', 'Rugged'),
('Doogee', 'N50 Pro', 'Doogee N50 Pro', 'n50pro', 'Helio G99', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'verified_alpha', NULL),

-- ============================================================================
-- 30. OUKITEL (4 models)
-- ============================================================================
('Oukitel', 'WP30 Pro', 'Oukitel WP30 Pro', 'wp30pro', 'Dimensity 8050', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'untested', 'Rugged'),
('Oukitel', 'C36', 'Oukitel C36', 'c36oukitel', 'UNISOC T606', 'UNISOC', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"SPD","MTP"}', '1782', '4D00', 'Global', 'untested', NULL),
('Oukitel', 'WP28', 'Oukitel WP28', 'wp28', 'Helio G85', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,11,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'verified_alpha', 'Rugged'),
('Oukitel', 'WP21 Ultra', 'Oukitel WP21 Ultra', 'wp21u', 'Dimensity 8050', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'untested', 'Rugged'),

-- ============================================================================
-- 31. ULEFONE (4 models)
-- ============================================================================
('Ulefone', 'Armor 24', 'Ulefone Armor 24', 'armor24', 'Dimensity 6300', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,11,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'untested', 'Rugged'),
('Ulefone', 'Armor 23 Ultra', 'Ulefone Armor 23 Ultra', 'armor23u', 'Dimensity 8050', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'untested', 'Rugged'),
('Ulefone', 'Power Armor 19T', 'Ulefone Power Armor 19T', 'pa19t', 'Helio G99', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'verified_alpha', 'Rugged + thermal camera'),
('Ulefone', 'Note 16 Pro', 'Ulefone Note 16 Pro', 'note16p', 'Helio G85', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,11,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'verified_alpha', NULL),

-- ============================================================================
-- 32. BLACKVIEW (4 models)
-- ============================================================================
('Blackview', 'BV9300 Pro', 'Blackview BV9300 Pro', 'bv9300p', 'Dimensity 7050', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'untested', 'Rugged'),
('Blackview', 'BV8900', 'Blackview BV8900', 'bv8900', 'Helio G99', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'verified_alpha', 'Rugged + thermal'),
('Blackview', 'A200 Pro', 'Blackview A200 Pro', 'a200pro', 'Helio G99', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'verified_alpha', NULL),
('Blackview', 'Tab 18', 'Blackview Tab 18', 'tab18bv', 'Dimensity 7050', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'untested', 'Tablet'),

-- ============================================================================
-- 33. UMIDIGI (4 models)
-- ============================================================================
('Umidigi', 'A15', 'Umidigi A15', 'a15umidigi', 'UNISOC T616', 'UNISOC', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"SPD","MTP"}', '1782', '4D00', 'Global', 'untested', NULL),
('Umidigi', 'G5 Mecha', 'Umidigi G5 Mecha', 'g5mecha', 'Dimensity 6300', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,11,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'untested', NULL),
('Umidigi', 'C1 Max', 'Umidigi C1 Max', 'c1max', 'UNISOC T610', 'UNISOC', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"SPD","MTP"}', '1782', '4D00', 'Global', 'verified_alpha', NULL),
('Umidigi', 'Power 7 Max', 'Umidigi Power 7 Max', 'power7max', 'Helio G99', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'verified_alpha', NULL),

-- ============================================================================
-- 34. SHARP (3 models)
-- ============================================================================
('Sharp', 'SH-M24', 'Sharp Aquos Sense8', 'sense8', 'Snapdragon 6 Gen 1', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,9,10,11,12,13,17,18,22]', '{"EDL","FASTBOOT","MTP"}', '04DD', 'A252', 'Japan', 'untested', NULL),
('Sharp', 'SH-M22', 'Sharp Aquos Wish3', 'wish3', 'Dimensity 700', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"BROM","MTP"}', '04DD', 'A252', 'Japan', 'untested', NULL),
('Sharp', 'SH-M19', 'Sharp Aquos Sense7', 'sense7', 'Snapdragon 695', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,9,10,11,12,13,17,18,22]', '{"EDL","FASTBOOT","MTP"}', '04DD', 'A252', 'Japan', 'verified_alpha', NULL),

-- ============================================================================
-- 35. FAIRPHONE (3 models)
-- ============================================================================
('Fairphone', 'FP5', 'Fairphone 5', 'fp5', 'Snapdragon QCM6490', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'EU', 'verified_alpha', 'Modular — easy repair'),
('Fairphone', 'FP4', 'Fairphone 4', 'fp4', 'Snapdragon 750G', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'EU', 'stable', NULL),
('Fairphone', 'FP3', 'Fairphone 3+', 'fp3p', 'Snapdragon 632', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,9,10,11,12,13,17,18,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'EU', 'stable', NULL),

-- ============================================================================
-- 36. OPPO (sub-brand: ONEPLUS — already covered above)
-- 37. TRANSSION (parent of Infinix/Tecno/Itel — already covered)
-- ============================================================================

-- ============================================================================
-- 38. HTC (4 models)
-- ============================================================================
('HTC', 'A104', 'HTC U23 Pro', 'u23pro', 'Snapdragon 7 Gen 1', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,9,10,11,12,13,17,18,22,24]', '{"EDL","FASTBOOT","MTP"}', '0BB4', '0F87', 'Global', 'untested', NULL),
('HTC', 'A103', 'HTC U23', 'u23', 'Snapdragon 7 Gen 1', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,9,10,11,12,13,17,18,22,24]', '{"EDL","FASTBOOT","MTP"}', '0BB4', '0F87', 'Global', 'untested', NULL),
('HTC', '2QBY100', 'HTC Desire 22 Pro', 'desire22p', 'Snapdragon 695', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,9,10,11,12,13,17,18,22,24]', '{"EDL","FASTBOOT","MTP"}', '0BB4', '0F87', 'Global', 'verified_alpha', NULL),
('HTC', '2Q4D100', 'HTC Wildfire E3', 'wfe3', 'Helio P22', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"BROM","MTP"}', '0BB4', '0F87', 'Global', 'stable', NULL),

-- ============================================================================
-- 39. CAT (3 models — rugged)
-- ============================================================================
('Cat', 'S75', 'Cat S75', 's75cat', 'Dimensity 930', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'verified_alpha', 'Rugged IP69'),
('Cat', 'S62 Pro', 'Cat S62 Pro', 's62pro', 'Snapdragon 660', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,9,10,11,12,17,18,22]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'stable', 'Rugged + FLIR'),
('Cat', 'S42 H+', 'Cat S42 H+', 's42h', 'Helio A20', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'stable', 'Rugged budget'),

-- ============================================================================
-- 40. KYOCERA (3 models)
-- ============================================================================
('Kyocera', 'KYV49', 'Kyocera Torque 5G', 'torque5g', 'Snapdragon 765', 'QUALCOMM', 'FRP_STANDARD', false, '[1,2,4,7,8,9,10,17,18]', '{"EDL","MTP"}', '0482', '0A3E', 'Japan', 'untested', 'Rugged — Japan carrier locked'),
('Kyocera', 'KYV48', 'Kyocera BASIO4', 'basio4', 'Helio A25', 'MEDIATEK', 'FRP_STANDARD', false, '[1,2,4,7,8,10,17,18]', '{"BROM","MTP"}', '0482', '0A3E', 'Japan', 'untested', 'Senior phone'),
('Kyocera', 'KC-S303', 'Kyocera DuraForce Ultra 5G', 'duraforce', 'Snapdragon 765G', 'QUALCOMM', 'FRP_STANDARD', false, '[1,2,4,7,8,9,10,17,18]', '{"EDL","MTP"}', '0482', '0A3E', 'US', 'verified_alpha', 'Rugged enterprise'),

-- ============================================================================
-- 41. PANASONIC (2 models)
-- ============================================================================
('Panasonic', 'KX-TU160', 'Panasonic KX-TU160', 'tu160', 'UNISOC SC7731E', 'UNISOC', 'NO_FRP', false, '[8,17,18]', '{"SPD"}', '1782', '4D00', 'EU', 'stable', 'Feature phone'),
('Panasonic', 'P110', 'Panasonic Eluga I7', 'elugai7', 'Helio A22', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'India', 'stable', NULL),

-- ============================================================================
-- 42. BLU (4 models)
-- ============================================================================
('BLU', 'B152DL', 'BLU View 4', 'view4blu', 'Helio A22', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'US', 'stable', NULL),
('BLU', 'B131DL', 'BLU View 3', 'view3blu', 'Helio A25', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'US', 'stable', NULL),
('BLU', 'G0670WW', 'BLU G91 Pro', 'g91pro', 'Helio G95', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'US', 'verified_alpha', NULL),
('BLU', 'B130DL', 'BLU G53', 'g53blu', 'UNISOC SC9863A', 'UNISOC', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"SPD","MTP"}', '1782', '4D00', 'US', 'stable', NULL),

-- ============================================================================
-- 43. WALTON (3 models — Bangladesh)
-- ============================================================================
('Walton', 'PRIMO-S10', 'Walton Primo S10', 'primos10', 'Helio G99', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Bangladesh', 'untested', NULL),
('Walton', 'PRIMO-HM7', 'Walton Primo HM7', 'primohm7', 'Helio G85', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,11,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Bangladesh', 'untested', NULL),
('Walton', 'PRIMO-E12', 'Walton Primo E12', 'primoe12', 'UNISOC SC9832E', 'UNISOC', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"SPD","MTP"}', '1782', '4D00', 'Bangladesh', 'untested', NULL),

-- ============================================================================
-- 44. SYMPHONY (3 models — Bangladesh)
-- ============================================================================
('Symphony', 'Z50', 'Symphony Z50', 'z50sym', 'Helio G88', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Bangladesh', 'untested', NULL),
('Symphony', 'ATOM', 'Symphony Atom', 'atomsym', 'UNISOC SC9863A', 'UNISOC', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"SPD","MTP"}', '1782', '4D00', 'Bangladesh', 'untested', NULL),
('Symphony', 'Z45', 'Symphony Z45', 'z45sym', 'Helio G85', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,11,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Bangladesh', 'untested', NULL),

-- ============================================================================
-- 45. OPPO (sub-brand: REALME — already covered above)
-- ============================================================================

-- ============================================================================
-- 46. POCO (sub-brand of Xiaomi — adding 5 more unique models)
-- ============================================================================
('Poco', 'POCO M6 Pro', 'POCO M6 Pro 5G', 'm6pro', 'Snapdragon 4 Gen 2', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,9,10,11,12,13,17,18,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'untested', NULL),
('Poco', 'POCO C55', 'POCO C55', 'c55poco', 'Helio G85', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,11,17,18]', '{"BROM","FASTBOOT","MTP"}', '2717', 'D001', 'Global', 'verified_alpha', NULL),
('Poco', 'POCO M4 Pro 5G', 'POCO M4 Pro 5G', 'm4pro5g', 'Dimensity 810', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '2717', 'D001', 'Global', 'stable', NULL),
('Poco', 'POCO X5', 'POCO X5 5G', 'x5poco', 'Snapdragon 695', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,9,10,11,12,13,17,18,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'stable', NULL),
('Poco', 'POCO F6 Pro', 'POCO F6 Pro', 'f6pro', 'Snapdragon 8s Gen 3', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'untested', NULL),

-- ============================================================================
-- 47. REDMI (sub-brand of Xiaomi — adding 5 more unique models)
-- ============================================================================
('Redmi', 'Redmi 14C', 'Redmi 14C', '14credmi', 'Helio G81 Ultra', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,11,17,18]', '{"BROM","FASTBOOT","MTP"}', '2717', 'D001', 'Global', 'untested', NULL),
('Redmi', 'Redmi Note 14 Pro', 'Redmi Note 14 Pro 4G', 'note14pro', 'Helio G99 Ultra', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '2717', 'D001', 'Global', 'untested', NULL),
('Redmi', 'Redmi 13', 'Redmi 13 4G', '13redmi', 'Helio G99 Ultra', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '2717', 'D001', 'Global', 'untested', NULL),
('Redmi', 'Redmi 12C', 'Redmi 12C', '12credmi', 'Helio G85', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,11,17,18]', '{"BROM","FASTBOOT","MTP"}', '2717', 'D001', 'Global', 'stable', NULL),
('Redmi', 'Redmi A3', 'Redmi A3', 'a3redmi', 'Helio G36', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"BROM","FASTBOOT","MTP"}', '2717', 'D001', 'Global', 'stable', NULL),

-- ============================================================================
-- 48. GIONEE (3 models)
-- ============================================================================
('Gionee', 'P15 Pro', 'Gionee P15 Pro', 'p15pro', 'Helio G85', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,11,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'untested', NULL),
('Gionee', 'F8 Neo', 'Gionee F8 Neo', 'f8neo', 'UNISOC SC9863A', 'UNISOC', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"SPD","MTP"}', '1782', '4D00', 'Global', 'untested', NULL),
('Gionee', 'Max Pro', 'Gionee Max Pro', 'maxpro', 'UNISOC T310', 'UNISOC', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"SPD","MTP"}', '1782', '4D00', 'India', 'stable', NULL),

-- ============================================================================
-- 49. KARBONN (3 models — India)
-- ============================================================================
('Karbonn', 'Titanium S12', 'Karbonn Titanium S12', 'titans12', 'Helio A22', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'India', 'stable', NULL),
('Karbonn', 'X21', 'Karbonn X21', 'x21karb', 'UNISOC SC9832E', 'UNISOC', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"SPD","MTP"}', '1782', '4D00', 'India', 'stable', NULL),
('Karbonn', 'Titanium S9+', 'Karbonn Titanium S9+', 'titans9p', 'Helio A25', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'India', 'stable', NULL),

-- ============================================================================
-- 50. IVOOMI (3 models — India/Africa)
-- ============================================================================
('iVOOMi', 'Z1', 'iVOOMi Z1', 'z1ivoomi', 'UNISOC SC9832E', 'UNISOC', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"SPD","MTP"}', '1782', '4D00', 'India', 'stable', NULL),
('iVOOMi', 'i2 Lite', 'iVOOMi i2 Lite', 'i2lite', 'Helio A22', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'India', 'stable', NULL),
('iVOOMi', 'V5', 'iVOOMi V5', 'v5ivoomi', 'UNISOC SC7731E', 'UNISOC', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"SPD","MTP"}', '1782', '4D00', 'India', 'stable', NULL)

-- ============================================================================
-- ADDITIONAL MODELS (89 more to reach 500 total)
-- ============================================================================

-- Samsung additions (10 more = 40 total)
,('Samsung', 'SM-A055F', 'Galaxy A05', 'a05', 'Helio G85', 'MEDIATEK', 'FRP_STANDARD', false, '[1,2,4,7,8,10,17,18]', '{"ODIN","MTP","BROM"}', '04E8', '685D', 'Global', 'untested', NULL)
,('Samsung', 'SM-A245F', 'Galaxy A24', 'a24', 'Helio G99', 'MEDIATEK', 'FRP_STANDARD', false, '[1,2,4,7,8,10,13,17,18]', '{"ODIN","MTP","BROM"}', '04E8', '685D', 'Global', 'verified_alpha', NULL)
,('Samsung', 'SM-A146B', 'Galaxy A14', 'a14', 'Exynos 850', 'EXYNOS', 'FRP_STANDARD', false, '[1,2,4,7,8,10,17,18]', '{"ODIN","MTP"}', '04E8', '685D', 'Global', 'stable', NULL)
,('Samsung', 'SM-A336E', 'Galaxy A33 5G', 'a33x', 'Exynos 1280', 'EXYNOS', 'FRP_STANDARD', false, '[1,2,4,7,8,10,13,17,18,20,21]', '{"ODIN","MTP"}', '04E8', '685D', 'Global', 'stable', NULL)
,('Samsung', 'SM-M236B', 'Galaxy M23 5G', 'm23', 'Snapdragon 750G', 'QUALCOMM', 'FRP_STANDARD', false, '[1,2,4,7,8,10,13,17,18]', '{"ODIN","MTP","EDL"}', '04E8', '685D', 'Global', 'verified_alpha', NULL)
,('Samsung', 'SM-G991B', 'Galaxy S21', 's21', 'Exynos 2100', 'EXYNOS', 'FRP_HARDENED', false, '[1,2,4,7,8,10,13,17,18,20,21,23]', '{"ODIN","MTP"}', '04E8', '6860', 'Global', 'stable', NULL)
,('Samsung', 'SM-G781B', 'Galaxy S20 FE', 's20fe', 'Snapdragon 865', 'QUALCOMM', 'FRP_HARDENED', false, '[1,2,4,7,8,10,13,17,18,20,21,23]', '{"ODIN","MTP"}', '04E8', '6860', 'Global', 'stable', NULL)
,('Samsung', 'SM-A528B', 'Galaxy A52s 5G', 'a52s', 'Snapdragon 778G', 'QUALCOMM', 'FRP_STANDARD', false, '[1,2,4,7,8,10,13,17,18,20,21]', '{"ODIN","MTP","EDL"}', '04E8', '685D', 'Global', 'stable', NULL)
,('Samsung', 'SM-G980F', 'Galaxy S20', 's20', 'Exynos 990', 'EXYNOS', 'FRP_HARDENED', false, '[1,2,4,7,8,10,13,17,18,20,21,23]', '{"ODIN","MTP"}', '04E8', '6860', 'Global', 'stable', NULL)
,('Samsung', 'SM-A226B', 'Galaxy A22 5G', 'a22_5g', 'Dimensity 700', 'MEDIATEK', 'FRP_STANDARD', false, '[1,2,4,7,8,10,17,18]', '{"ODIN","MTP","BROM"}', '04E8', '685D', 'Global', 'verified_alpha', NULL)

-- Xiaomi additions (6 more = 35+ total with sub-brands)
,('Xiaomi', 'Redmi Note 14', 'Redmi Note 14 4G', 'note14', 'Helio G99 Ultra', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '2717', 'D001', 'Global', 'untested', NULL)
,('Xiaomi', 'Redmi Note 14 Pro+ 5G', 'Redmi Note 14 Pro+ 5G', 'note14pp5g', 'Snapdragon 7s Gen 3', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'untested', NULL)
,('Xiaomi', '14T', 'Xiaomi 14T', '14t', 'Dimensity 8300 Ultra', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '2717', 'D001', 'Global', 'untested', NULL)
,('Xiaomi', '14', 'Xiaomi 14', 'houji', 'Snapdragon 8 Gen 3', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'untested', NULL)
,('Xiaomi', 'Mix Fold 3', 'Xiaomi Mix Fold 3', 'mixfold3', 'Snapdragon 8 Gen 2', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'China', 'untested', 'Foldable')
,('Xiaomi', 'Redmi K70 Pro', 'Redmi K70 Pro', 'manet', 'Snapdragon 8 Gen 3', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'China', 'untested', NULL)

-- Realme additions (5 more = 30 total)
,('Realme', 'RMX3910', 'Realme C67', 'c67realme', 'Snapdragon 685', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,9,10,11,12,17,18,22]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'untested', NULL)
,('Realme', 'RMX3930', 'Realme 13 Pro+', '13proplus', 'Snapdragon 7s Gen 2', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,9,10,11,12,13,17,18,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'untested', NULL)
,('Realme', 'RMX3842', 'Realme 13 Pro', '13pro', 'Snapdragon 7s Gen 2', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,9,10,11,12,13,17,18,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'untested', NULL)
,('Realme', 'RMX3700', 'Realme Narzo 70 Pro', 'narzo70p', 'Dimensity 7050', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'untested', NULL)
,('Realme', 'RMX3999', 'Realme GT6', 'gt6', 'Snapdragon 8s Gen 3', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'untested', NULL)

-- Oppo additions (5 more = 25 total)
,('Oppo', 'CPH2625', 'Oppo Reno 12 Pro 5G', 'reno12pro', 'Dimensity 7300', 'MEDIATEK', 'FRP_STANDARD', false, '[1,2,4,5,7,8,10,11,13,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'untested', NULL)
,('Oppo', 'CPH2617', 'Oppo Reno 12 5G', 'reno12', 'Dimensity 7300', 'MEDIATEK', 'FRP_STANDARD', false, '[1,2,4,5,7,8,10,11,13,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'untested', NULL)
,('Oppo', 'CPH2591', 'Oppo A79 5G', 'a79', 'Dimensity 6020', 'MEDIATEK', 'FRP_STANDARD', false, '[1,2,4,7,8,10,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'untested', NULL)
,('Oppo', 'CPH2565', 'Oppo A2 Pro', 'a2pro', 'Dimensity 7050', 'MEDIATEK', 'FRP_STANDARD', false, '[1,2,4,5,7,8,10,11,13,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'China', 'untested', NULL)
,('Oppo', 'CPH2609', 'Oppo Find X7', 'findx7', 'Dimensity 9300', 'MEDIATEK', 'FRP_HARDENED', false, '[1,4,7,8,10,11,13,17,18,20,21,23]', '{"BROM","MTP"}', '0E8D', '2000', 'China', 'untested', 'Flagship MTK')

-- Vivo additions (5 more = 25 total)
,('Vivo', 'V2338', 'Vivo V40 Pro', 'v40pro', 'Dimensity 9200+', 'MEDIATEK', 'FRP_STANDARD', false, '[1,2,4,5,7,8,10,11,13,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'untested', NULL)
,('Vivo', 'V2335', 'Vivo V40', 'v40', 'Snapdragon 7 Gen 3', 'QUALCOMM', 'FRP_STANDARD', false, '[1,2,4,7,8,9,10,11,12,17,18]', '{"EDL","MTP"}', '05C6', '9008', 'Global', 'untested', NULL)
,('Vivo', 'V2348', 'Vivo Y28 5G', 'y28', 'Dimensity 6020', 'MEDIATEK', 'FRP_STANDARD', false, '[1,2,4,7,8,10,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'untested', NULL)
,('Vivo', 'V2325', 'Vivo X100', 'x100', 'Dimensity 9300', 'MEDIATEK', 'FRP_HARDENED', false, '[1,4,7,8,10,11,13,17,18,20,21,23]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'untested', NULL)
,('Vivo', 'V2246', 'Vivo T3 5G', 't3vivo', 'Dimensity 7200', 'MEDIATEK', 'FRP_STANDARD', false, '[1,2,4,7,8,10,11,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'untested', NULL)

-- Honor additions (3 more = 15 total)
,('Honor', 'PGT-AN00', 'Honor Magic6 RSR', 'magic6rsr', 'Snapdragon 8 Gen 3', 'QUALCOMM', 'FRP_HARDENED', false, '[1,4,7,8,9,10,13,17,18,20,21]', '{"EDL","MTP"}', '05C6', '9008', 'Global', 'untested', 'Porsche Design')
,('Honor', 'ALI-NX1', 'Honor X9a', 'x9a', 'Snapdragon 695', 'QUALCOMM', 'FRP_STANDARD', false, '[1,2,4,7,8,9,10,11,17,18]', '{"EDL","MTP"}', '05C6', '9008', 'Global', 'verified_alpha', NULL)
,('Honor', 'FNE-NX9', 'Honor 100 Pro', 'h100pro', 'Snapdragon 8 Gen 2', 'QUALCOMM', 'FRP_STANDARD', false, '[1,2,4,7,8,9,10,11,13,17,18,20,21]', '{"EDL","MTP"}', '05C6', '9008', 'Global', 'untested', NULL)

-- Motorola additions (5 more = 25 total)
,('Motorola', 'XT2365-3', 'Moto G85 5G', 'g85', 'Snapdragon 6s Gen 3', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,9,10,11,12,13,17,18,22,24]', '{"EDL","FASTBOOT","MTP"}', '22B8', '2E83', 'Global', 'untested', NULL)
,('Motorola', 'XT2361-3', 'Moto G45 5G', 'g45', 'Snapdragon 6s Gen 3', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,9,10,11,12,13,17,18,22,24]', '{"EDL","FASTBOOT","MTP"}', '22B8', '2E83', 'Global', 'untested', NULL)
,('Motorola', 'XT2363-6', 'Moto Edge 50 Ultra', 'edge50u', 'Snapdragon 8s Gen 3', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '22B8', '2E83', 'Global', 'untested', 'Flagship')
,('Motorola', 'XT2371-3', 'Moto Razr 50 Ultra', 'razr50u', 'Snapdragon 8s Gen 3', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,9,10,11,12,13,17,18,22,24]', '{"EDL","FASTBOOT","MTP"}', '22B8', '2E83', 'Global', 'untested', 'Foldable')
,('Motorola', 'XT2253-1', 'Moto E13', 'e13', 'UNISOC T606', 'UNISOC', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"SPD","FASTBOOT","MTP"}', '22B8', '2E83', 'Global', 'verified_alpha', 'UNISOC budget')

-- OnePlus additions (3 more = 15 total)
,('OnePlus', 'CPH2609', 'OnePlus Nord 4', 'nord4', 'Snapdragon 7+ Gen 3', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,9,10,11,12,13,17,18,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'untested', NULL)
,('OnePlus', 'CPH2635', 'OnePlus 13', 'op13', 'Snapdragon 8 Elite', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'untested', 'Latest flagship')
,('OnePlus', 'CPH2591', 'OnePlus Nord CE 4 Lite', 'nordce4l', 'Snapdragon 695', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,9,10,11,12,13,17,18,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'untested', NULL)

-- Infinix additions (5 more = 25 total)
,('Infinix', 'X6875', 'Infinix Note 40 Pro+', 'note40pp', 'Dimensity 8100', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,20,21,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'untested', NULL)
,('Infinix', 'X6841', 'Infinix Hot 40 Pro', 'hot40pro', 'Helio G99', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'untested', NULL)
,('Infinix', 'X6843', 'Infinix Hot 40', 'hot40', 'Helio G88', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,17,18]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'untested', NULL)
,('Infinix', 'X6711B', 'Infinix Smart 7 HD', 'smart7hd', 'UNISOC SC9832E', 'UNISOC', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"SPD","MTP"}', '1782', '4D00', 'Global', 'stable', 'Ultra-budget UNISOC')
,('Infinix', 'X6831B', 'Infinix Hot 30i NFC', 'hot30infc', 'Helio G37', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'stable', NULL)

-- Tecno additions (5 more = 25 total)
,('Tecno', 'CK9', 'Tecno Camon 30 Premier', 'camon30pr', 'Dimensity 8200', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,20,21,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'untested', NULL)
,('Tecno', 'BG8n', 'Tecno Spark 20 Pro 5G', 'spark20p5g', 'Dimensity 6080', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'untested', NULL)
,('Tecno', 'BG5', 'Tecno Spark Go 2023', 'sparkgo23', 'Helio A22', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'stable', NULL)
,('Tecno', 'CH7n', 'Tecno Pova 6 Pro', 'pova6p', 'Dimensity 6300', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'untested', NULL)
,('Tecno', 'CI6', 'Tecno Camon 19', 'camon19', 'Helio G85', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,11,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'stable', NULL)

-- Nokia additions (5 more = 20 total)
,('Nokia', 'TA-1620', 'Nokia G60 5G (2024)', 'g60_24', 'Snapdragon 695', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,9,10,11,12,13,17,18,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'untested', NULL)
,('Nokia', 'TA-1625', 'Nokia C300', 'c300', 'UNISOC T606', 'UNISOC', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"SPD","MTP"}', '1782', '4D00', 'US', 'untested', NULL)
,('Nokia', 'TA-1430', 'Nokia G20', 'g20nokia', 'Helio G35', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'stable', NULL)
,('Nokia', 'TA-1392', 'Nokia X10', 'x10', 'Snapdragon 480', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,9,10,11,12,13,17,18,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'stable', NULL)
,('Nokia', 'TA-1594', 'Nokia C22', 'c22nokia', 'UNISOC SC9863A', 'UNISOC', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"SPD","MTP"}', '1782', '4D00', 'Global', 'stable', NULL)

-- Huawei additions (5 more = 20 total)
,('Huawei', 'OCE-AN50', 'Huawei Mate 50 Pro', 'mate50p', 'Snapdragon 8+ Gen 1', 'QUALCOMM', 'FRP_HARDENED', false, '[1,4,7,8,9,10,13,17,18,20,21]', '{"EDL","MTP"}', '05C6', '9008', 'China', 'untested', 'No GMS')
,('Huawei', 'CET-LX3', 'Huawei Nova 11 SE', 'nova11se', 'Snapdragon 680', 'QUALCOMM', 'FRP_STANDARD', false, '[1,2,4,7,8,9,10,17,18]', '{"EDL","MTP"}', '05C6', '9008', 'Global', 'untested', NULL)
,('Huawei', 'MAR-LX1A', 'Huawei P30 Lite', 'p30lite', 'Kirin 710', 'KIRIN', 'FRP_STANDARD', false, '[1,2,4,7,8,10,13,17,18,20,21]', '{"HUAWEI_UPDATE","MTP"}', '12D1', '107E', 'Global', 'stable', 'Very popular — well understood FRP')
,('Huawei', 'JEF-NX9', 'Huawei P50', 'p50', 'Snapdragon 888', 'QUALCOMM', 'FRP_HARDENED', false, '[1,4,7,8,9,10,13,17,18,20,21]', '{"EDL","MTP"}', '05C6', '9008', 'Global', 'untested', NULL)
,('Huawei', 'BAL-AL80', 'Huawei Nova 13 Pro', 'nova13p', 'Kirin 8000', 'KIRIN', 'FRP_HARDENED', false, '[8,17,18]', '{"HUAWEI_UPDATE","MTP"}', '12D1', '107E', 'China', 'untested', 'HarmonyOS — very locked')

-- Google additions (3 more = 15 total)
,('Google', 'GFE4J', 'Pixel Fold', 'felix', 'Tensor G2', 'TENSOR', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,23,24]', '{"FASTBOOT","MTP"}', '18D1', '4EE7', 'Global', 'verified_alpha', 'Foldable')
,('Google', 'G1MNW', 'Pixel 9 Pro Fold', 'comet', 'Tensor G4', 'TENSOR', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,23,24]', '{"FASTBOOT","MTP"}', '18D1', '4EE7', 'Global', 'untested', 'Foldable')
,('Google', 'GPQ72', 'Pixel Tablet', 'tangorpro', 'Tensor G2', 'TENSOR', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,23]', '{"FASTBOOT","MTP"}', '18D1', '4EE7', 'Global', 'verified_alpha', 'Tablet')

-- Itel additions (5 more = 15 total)
,('Itel', 'A665LN', 'Itel S24 Pro', 's24proitel', 'Helio G99', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'untested', NULL)
,('Itel', 'A666L', 'Itel RS4', 'rs4itel', 'Dimensity 7300', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,13,17,18,24]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'Global', 'untested', NULL)
,('Itel', 'A661LN', 'Itel A70 Pro', 'a70pro_itel', 'UNISOC SC9863A', 'UNISOC', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"SPD","MTP"}', '1782', '4D00', 'Global', 'untested', NULL)
,('Itel', 'P682L', 'Itel Vision 5 Plus', 'vision5p', 'UNISOC T606', 'UNISOC', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"SPD","MTP"}', '1782', '4D00', 'Global', 'verified_alpha', NULL)
,('Itel', 'A572LN', 'Itel S23 Pro', 's23pro_itel', 'Helio G88', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,17,18]', '{"BROM","MTP"}', '0E8D', '2000', 'Global', 'untested', NULL)

-- ZTE additions (2 more = 10 total)
,('ZTE', 'NX735J', 'ZTE Nubia Z70 Ultra', 'z70ultra', 'Snapdragon 8 Elite', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '05C6', '9008', 'Global', 'untested', NULL)
,('ZTE', 'A2121G', 'ZTE Blade A33 Plus', 'bladea33p', 'UNISOC SC7731E', 'UNISOC', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"SPD","MTP"}', '19D2', 'FFFF', 'Global', 'stable', 'Ultra-budget')

-- Sony additions (2 more = 10 total)
,('Sony', 'XQ-ES72', 'Xperia 1 VII', 'pdx261', 'Snapdragon 8 Elite', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '0FCE', 'B00B', 'Global', 'untested', NULL)
,('Sony', 'XQ-DQ44', 'Xperia 10 VI', 'pdx262', 'Snapdragon 6 Gen 1', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,9,10,11,12,13,17,18,22,24]', '{"EDL","FASTBOOT","MTP"}', '0FCE', 'B00B', 'Global', 'untested', NULL)

-- Asus additions (2 more = 10 total)
,('Asus', 'AI2401-E', 'ROG Phone 9 Pro', 'rog9pro', 'Snapdragon 8 Elite', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '0B05', '7775', 'Global', 'untested', 'Gaming flagship')
,('Asus', 'AI2302-E', 'ROG Phone 8', 'rog8', 'Snapdragon 8 Gen 3', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,6,7,8,9,10,11,12,13,17,18,20,21,22,24]', '{"EDL","FASTBOOT","MTP"}', '0B05', '7775', 'Global', 'untested', NULL)

-- LG additions (2 more = 10 total)
,('LG', 'LM-F100', 'LG Wing', 'wing', 'Snapdragon 765G', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,9,10,11,12,13,17,18,22,24]', '{"EDL","FASTBOOT","MTP"}', '1004', '633A', 'Global', 'stable', 'Swivel design')
,('LG', 'LM-Q920N', 'LG Q31', 'q31', 'Helio P22', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"BROM","MTP"}', '1004', '633A', 'Global', 'stable', NULL)

-- Lenovo additions (2 more = 10 total)
,('Lenovo', 'L71091', 'Lenovo K14 Note', 'k14note', 'Helio G88', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,17,18]', '{"BROM","FASTBOOT","MTP"}', '17EF', '7901', 'Global', 'untested', NULL)
,('Lenovo', 'TB-X6C6F', 'Lenovo Tab M9', 'tabm9', 'Helio G80', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"BROM","MTP"}', '17EF', '7901', 'Global', 'verified_alpha', 'Tablet')

-- TCL additions (4 more = 10 total)
,('TCL', 'T771A', 'TCL 50 XL 5G', 'tcl50xl', 'Dimensity 6300', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,11,17,18]', '{"BROM","MTP"}', '1BBB', '0200', 'US', 'untested', NULL)
,('TCL', 'T772B', 'TCL 50 SE', 'tcl50se', 'Helio G85', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"BROM","MTP"}', '1BBB', '0200', 'Global', 'untested', NULL)
,('TCL', 'T610DL', 'TCL 40 R 5G', 'tcl40r', 'Dimensity 6100+', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,11,17,18]', '{"BROM","MTP"}', '1BBB', '0200', 'Global', 'untested', NULL)
,('TCL', 'T506K', 'TCL 305', 'tcl305', 'Helio A22', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"BROM","MTP"}', '1BBB', '0200', 'Global', 'stable', NULL)

-- Alcatel additions (4 more = 10 total)
,('Alcatel', '6062W', 'Alcatel 3V (2019)', '3v2019', 'Snapdragon 439', 'QUALCOMM', 'FRP_STANDARD', true, '[1,2,3,4,7,8,9,10,17,18]', '{"EDL","MTP"}', '05C6', '9008', 'US', 'stable', NULL)
,('Alcatel', '5052W', 'Alcatel 3V (2018)', '3v2018', 'Helio P22', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"BROM","MTP"}', '1BBB', '0167', 'US', 'stable', NULL)
,('Alcatel', '5002R', 'Alcatel 1B (2020)', '1b2020', 'UNISOC SC7731E', 'UNISOC', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"SPD","MTP"}', '1782', '4D00', 'Global', 'stable', NULL)
,('Alcatel', '5033A', 'Alcatel 1', '1alc', 'Helio A22', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,7,8,10,17,18]', '{"BROM","MTP"}', '1BBB', '0167', 'Global', 'stable', NULL)

-- BLU addition (1 more = 5 total)
,('BLU', 'G0710WW', 'BLU G93', 'g93blu', 'Helio G88', 'MEDIATEK', 'FRP_STANDARD', true, '[1,2,3,4,5,7,8,10,11,17,18]', '{"BROM","FASTBOOT","MTP"}', '0E8D', '2000', 'US', 'untested', NULL)

-- End of seed: 500 models across 50 brands
;
