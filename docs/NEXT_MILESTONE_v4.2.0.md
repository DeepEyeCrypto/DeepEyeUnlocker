# NEXT MILESTONE: v4.2.0 "The Experience & Expansion Update"

## 🎯 Vision

Bridge the gap between the v4.1.x stability releases and the v5.0.0 Enterprise overhaul by focusing on **User Experience (UX)**, **UI Aesthetics**, and completing the **Spreadtrum (SPD)** protocol engine.

---

## 🏗️ Phase 1: High-Fidelity UI/UX (Medium Priority)

**Objective**: Make the Modern UI feel premium and reactive.

- [x] **Tab Transitions**: Implement smooth Opacity/Translate animations when switching between Operational Centers.
- [x] **Adaptive Progress Rings**: Replace standard progress bars with high-fidelity circular SVG animations for hardware operations.
- [x] **Haptic Feedback (Android)**: Integrate subtle vibration feedback for successful/failed operations in the Portable Engine.
- [x] **Glassmorphism Refinement**: Update the `Liquid Glass` theme with better blur performance for lower-end Windows machines.

## 📡 Phase 2: Protocol Expansion (SPD Heritage)

**Objective**: Move Spreadtrum/Unisoc support from "Simulated" to "Functional".

- [x] **SPD FDL Parser**: Implement the binary parser for `.pac` files.
- [x] **SPD Diag Mode**: Finalize the NVRAM read/write commands for SC9863A and SC7731E chipsets.
- [x] **SPD Handshake Fix**: Resolve the COM-port baud rate switching issue during FDL1 loading.

## 📱 Phase 3: Mobile UX Optimization

**Objective**: Tailor the interface specifically for the Android OTG experience.

- [x] **Compact Layout**: Auto-switch to a simplified, touch-friendly UI when `isMobile: true`.
- [x] **Large Touch Targets**: Ensure all action buttons (FRP Bypass, Wipe, Backup) follow mobile accessibility standards.
- [x] **OTG Speed Boost**: Implement bulk-transfer chunking for faster transmission.
- [x] **USB Hotplug Optimization**: Improve the JNI bridge response time when a device is attached via OTG cable.

## 🛠️ Phase 4: Technical Debt & Polish

- [x] **MSI Installer**: Finalize the WiX bootstrap for Windows.
- [x] **PDF Reporting**: Add full styling to the Fleet HQ export engine.
- [x] **Localization**: Complete the Hindi translation for the Expert Mode panels.

---
**Target Release**: April 2026
**Branch**: `feat/v4.2.0-ux-spd`
