# Implementation Plan: Driver Manager v4.0

This plan outlines the steps to upgrade DeepEyeUnlocker's driver management system from a "Generic" approach to a "Chimera-Class" smart installer, as defined in `docs/windows_driver_architecture.md`.

## Phase 1: Infrastructure Upgrades

- [ ] **Smart Architecture Detection**: Implement a helper to detect WOW64 (32-bit app on 64-bit OS) and Native 64-bit kernels.
- [ ] **LibUSB Filter Manager**: Create a service to manage the registration of `libusb-win32` as an Upper Filter for specific VID/PIDs (MTK BROM, Qualcomm EDL).
- [ ] **Enhanced Conflict Purging**: Update `DriverConflictManager` to use `pnputil.exe` for forced removal of legacy drivers.

## Phase 2: Core Logic Unification

- [ ] **Unify Driver Managers**: Combine `DriverManager` and `DriverInstaller` into a single, robust `SmartDriverInstaller`.
- [ ] **SetupAPI Integration**: Ensure all INF-based drivers are installed via `SetupCopyOEMInf` for proper Driver Store registration.
- [ ] **Digital Signature Validation**: Implement basic catalog check before installation.

## Phase 3: UI Integration

- [ ] **Driver Pro Panel**: Connect the new `SmartDriverInstaller` to the `DriverProPanel` UI.
- [ ] **Progress Reporting**: Add detailed progress feedback for each stage (Detection -> Purging -> Installation -> Filtering).

## Phase 4: Validation

- [ ] **Test Scenarios**: Verify BROM disconnect prevention and driver conflict resolution.
