# DeepEyeUnlocker v1.1.1 - Core Model Refactoring & Type Unification

We are pleased to announce the release of DeepEyeUnlocker v1.1.1. This release focuses on internal structural improvements, type safety, and architectural consistency to provide a more stable foundation for future multi-protocol updates.

## Key Changes

### 1. Unified FRP Model System

- **Single Source of Truth**: Unified `FrpStatus` and related entities into `Core.Models.LockFrpModels`.
- **Standardized Enums**: introduced `FrpLockStatus` with explicit states (`Locked`, `Unlocked`, `PartiallyCleared`, `Error`).
- **consistent Logic**: Updated detection and bypass logic across `FrpBypassManager`, `LockFrpDiagnosticsManager`, and UI panels to use the new unified models.

### 2. Standardized Partition Management

- **Canonical Model**: Established `PartitionInfo` in `Core.Models` as the universal representation of storage partitions.
- **Protocol Consistency**: Removed local duplicates in `PartitionTableParser` and protocol engines.
- **Property Renaming**: Standardized property names to `SizeInBytes` and `StartLba` across all layers to eliminate ambiguity.

### 3. Modernized Device Context Architecture

- **Legacy Removal**: Deprecated and removed the inconsistent `Device` class.
- **Enhanced Context**: Refactored the entire operation pipeline to use `DeviceContext`, providing better metadata management and brand identification.
- **Robust Integration**: Updated `DeviceManager` and `MainForm` to use the new context model for discovery and execution.

### 4. Interface & Engine Refinement

- **Consistent Contracts**: Updated `IProtocolEngine` and `Operation` base classes to use unified models.
- **Improved Progress Tracking**: Standardized progress reporting using `IProgress<ProgressUpdate>` across all asynchronous operations.

## Technical Debt Resolved

- Eliminated redundant definitions of common data structures.
- Improved type safety for cross-protocol operations.
- Simplified UI-to-Engine communication patterns.

## Installation & Usage

Refer to the [USER_MANUAL.md](USER_MANUAL.md) and [BUILD.md](BUILD.md) for instructions on setting up and running the latest version.

---
*DeepEyeUnlocker - Open Source, Community Driven.*
