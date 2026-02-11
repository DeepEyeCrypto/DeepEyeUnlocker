# FRP Service Engine Scenarios (v5.0)

This document shows how the Universal FRP Engine handles different device types and ownership states.

## Scenario 1: Samsung Galaxy S23 (Unverified)

* **Device**: Samsung S23, Qualcomm, Knox
* **Ownership**: Unknown
* **Action**: `ExecuteServiceClearAsync`
* **Engine Response**:
  * `Status`: FAILED
  * `Message`: "Operation Blocking: Ownership verification is mandatory for FRP services."
  * `Audit Log`: Recorded as `UNVERIFIED` attempt.

## Scenario 2: Xiaomi Redmi Note 11 (Verified Repair)

* **Device**: Xiaomi, Qualcomm, Standard FRP
* **Ownership**: VerifiedIndividual (Customer provided receipt)
* **Mode**: Connect in EDL (9008)
* **Action**: `ExecuteServiceClearAsync`
* **Engine Logic**:
    1. `EnsureFrpCapabilities` detects Qualcomm + Xiaomi -> Sets partition to `frp`.
    2. `QualcommEdlFrpStrategy.CanHandle` returns true.
    3. `ExecuteAsync` calls `ErasePartitionAsync("frp")`.
  * `Status`: SUCCESS
  * `Message`: "Successfully erased 'frp' partition. FRP request prevents reuse."
  * `Audit Log`: Recorded as `VERIFIED_INDIVIDUAL` success.

## Scenario 3: Enterprise Fleet (Bulk Clear)

* **Device**: Pixel 7, Google Tensor
* **Ownership**: VerifiedEnterpriseOwner
* **Mode**: Fastboot
* **Action**: `ExecuteServiceClearAsync`
* **Engine Logic**:
    1. Registry detects Google + Google -> No strategy for Tensor EDL yet.
    2. Registry falls back to `FASTBOOT_ERASE_FRP`.
  * `Status`: FAILED (Strategy Not Found) -> "No supported strategy found for Protocol: FASTBOOT..."
  * `Audit Log`: Recorded with specific failure reason.

---

## Technical Features for Technicians

- **Auto-Partition Discovery**: Registry knows if it's `frp`, `persistent`, or `config`.
* **Contextual Help**: `GetOfficialInstructions` reveals the "Right Way" for each device (e.g., eRecovery for Huawei, Smart Switch for Samsung).
* **Security Interlocks**: No data is written unless `Ownership` is set in the context.
