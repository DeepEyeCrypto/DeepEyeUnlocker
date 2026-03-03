-- ============================================================================
-- V1__create_device_profiles.sql
-- DeepEye Universal — Device Profile Schema (PostgreSQL 15+)
-- Flyway migration: creates the primary device_profiles table + indexes
-- ============================================================================

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

CREATE TYPE frp_state_enum AS ENUM (
    'NO_FRP',            -- device has no Google FRP mechanism
    'FRP_STANDARD',      -- standard Google FRP, bypassable
    'FRP_HARDENED',      -- vendor-hardened FRP (Knox, realme Shield, etc.)
    'FRP_UNKNOWN'        -- not yet classified
);

CREATE TYPE chipset_family_enum AS ENUM (
    'QUALCOMM',
    'MEDIATEK',
    'EXYNOS',
    'UNISOC',
    'KIRIN',
    'TENSOR',
    'SNAPDRAGON',   -- alias kept for historical data
    'OTHER'
);

CREATE TABLE device_profiles (
    id              BIGSERIAL       PRIMARY KEY,

    -- Identity
    brand           VARCHAR(64)     NOT NULL,
    model           VARCHAR(128)    NOT NULL,
    marketing_name  VARCHAR(128),
    codename        VARCHAR(64),

    -- Hardware
    chipset         VARCHAR(128)    NOT NULL,       -- e.g. "MT6789", "SM8550"
    chipset_family  chipset_family_enum NOT NULL DEFAULT 'OTHER',
    cpu_arch        VARCHAR(16)     NOT NULL DEFAULT 'ARM64',

    -- Security / FRP
    frp_state       frp_state_enum  NOT NULL DEFAULT 'FRP_UNKNOWN',
    bootloader_unlockable BOOLEAN   NOT NULL DEFAULT FALSE,

    -- Capabilities (JSONB array of integer feature IDs 1–24)
    supported_functions JSONB       NOT NULL DEFAULT '[]'::jsonb,

    -- Protocol metadata
    supported_protocols TEXT[]      DEFAULT '{}',   -- e.g. {'EDL','BROM','FASTBOOT'}
    usb_vid         VARCHAR(8),
    usb_pid         VARCHAR(8),

    -- Audit
    region          VARCHAR(32)     DEFAULT 'Global',
    validation_status VARCHAR(24)   DEFAULT 'untested',
    notes           TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),

    -- Constraints
    CONSTRAINT uq_brand_model UNIQUE (brand, model)
);

-- Performance indexes
CREATE INDEX idx_dp_brand           ON device_profiles (brand);
CREATE INDEX idx_dp_chipset_family  ON device_profiles (chipset_family);
CREATE INDEX idx_dp_frp_state       ON device_profiles (frp_state);
CREATE INDEX idx_dp_bootloader      ON device_profiles (bootloader_unlockable);
CREATE INDEX idx_dp_functions_gin   ON device_profiles USING gin (supported_functions);

-- Full-text search on brand + model + marketing_name
CREATE INDEX idx_dp_search ON device_profiles USING gin (
    to_tsvector('simple', coalesce(brand,'') || ' ' || coalesce(model,'') || ' ' || coalesce(marketing_name,''))
);

-- Auto-update updated_at on row change
CREATE OR REPLACE FUNCTION update_device_profiles_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_device_profiles_updated
    BEFORE UPDATE ON device_profiles
    FOR EACH ROW
    EXECUTE FUNCTION update_device_profiles_timestamp();

COMMENT ON TABLE  device_profiles IS 'Master device compatibility database — 50 brands × 500 models';
COMMENT ON COLUMN device_profiles.supported_functions IS 'JSONB array of feature IDs 1–24 (see migration header)';
