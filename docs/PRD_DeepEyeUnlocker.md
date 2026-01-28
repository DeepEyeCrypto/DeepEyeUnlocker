# Product Requirements Document (PRD) - DeepEyeUnlocker

## 1. Executive Summary

DeepEyeUnlocker is a professional-grade, open-source mobile repair tool designed to perform sensitive operations like Bootloader Unlocking, FRP (Factory Reset Protection) Bypassing, and Firmware Flashing across Qualcomm, MediaTek, and Samsung devices.

## 2. Core Operational Pillars

The tool is built on three main operational pillars that must be tested for absolute reliability:

1. **Security Bypass**: Bypassing FRP and Mi accounts using chipset-level exploits (EDL/BROM).
2. **Firmware Integrity**: Parsing complex vendor manifests (XML/Scatter/TAR) and writing them to storage without corruption.
3. **Partition Safety**: Preventing the accidental erasure of critical device identification data (EFS, NVRAM, Persist).

## 3. Critical Workflows

### 3.1 Device Detection

- Must detect devices in ADB, Fastboot, EDL, BROM, and Download Mode.
- Must reactively update the UI when a device is plugged/unplugged.

### 3.2 Flash Operations

- Must validate firmware file paths before initialization.
- Must support partial flashing (selecting specific partitions).
- **Hard Constraint**: Must force-deselect "Critical" partitions when "Safeguard Mode" is enabled.

### 3.3 Bootloader Unlock

- Must implement a multi-step confirmation wizard to prevent accidental data loss.
- Must select the correct exploit strategy based on detected hardware ID.

## 4. Error Handling Requirements

- Any protocol timeout must result in a safe disconnection of the hardware handle.
- Failures during partition writes must be logged with high granularity (offset, length, error code).
- UI must remain responsive during 1GB+ partition transfers.

## 5. Testing Scope

- **Logic Level**: 100% coverage on path parsing, partition filtering, and strategy selection.
- **Hardware Level**: Mocked USB/CLI responses for CI/CD environments.
- **Security**: Verification of "Zygisk/Shamiko" detection logic in the Cloak Center.
