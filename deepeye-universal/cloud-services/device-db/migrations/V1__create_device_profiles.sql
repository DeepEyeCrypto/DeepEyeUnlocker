-- ============================================================
-- V1__create_device_profiles.sql
-- DeepEye Universal — DeviceProfile DB
-- Flyway migration | PostgreSQL 15
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TYPE chipset_type AS ENUM (
    'qualcomm', 'mediatek', 'samsung_exynos', 'kirin', 'tensor', 'unisoc', 'unknown'
);

CREATE TYPE engine_type AS ENUM (
    'qualcomm', 'mediatek', 'samsung', 'unisoc'
);

CREATE TABLE IF NOT EXISTS device_profiles (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    brand                   VARCHAR(64)  NOT NULL,
    model                   VARCHAR(128) NOT NULL,
    series                  VARCHAR(64),
    release_year            SMALLINT,
    device_type             VARCHAR(32),
    chipset                 chipset_type NOT NULL DEFAULT 'unknown',
    engine                  engine_type  NOT NULL,
    bootloader_unlockable   BOOLEAN NOT NULL DEFAULT FALSE,
    supported_functions     JSONB    NOT NULL DEFAULT '[]',
    frp_state               VARCHAR(32) NOT NULL DEFAULT 'UNKNOWN',
    notes                   TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_dp_brand         ON device_profiles (brand);
CREATE INDEX idx_dp_chipset       ON device_profiles (chipset);
CREATE INDEX idx_dp_engine        ON device_profiles (engine);
CREATE INDEX idx_dp_year          ON device_profiles (release_year);
CREATE INDEX idx_dp_brand_model   ON device_profiles (brand, model);
CREATE INDEX idx_dp_funcs_gin     ON device_profiles USING GIN (supported_functions);

-- Auto-update updated_at
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN NEW.updated_at = NOW(); RETURN NEW; END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_dp_updated_at
BEFORE UPDATE ON device_profiles
FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- Abuse / audit table
CREATE TABLE IF NOT EXISTS device_check_audit (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_hash   VARCHAR(64) NOT NULL,
    brand       VARCHAR(64),
    model       VARCHAR(128),
    serial_hash VARCHAR(64),
    operation   VARCHAR(64) NOT NULL,
    tier        SMALLINT    NOT NULL,
    result      VARCHAR(32) NOT NULL,
    checked_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_user_hash ON device_check_audit (user_hash, checked_at);
CREATE INDEX idx_audit_serial    ON device_check_audit (serial_hash);
