# Implementation Plan - Phase: ORCHESTRATION

This phase focuses on building the modern WPF shell, implementing the Liquid Glass design system, and bridging the existing backend core to the new UI.

## 1. Project Restructuring

- Create `src/DeepEye.UI.Modern` (WPF Project).
- Ensure existing protocols and operations are accessible (refactor to a Shared/Core library if needed).

## 2. Design System (The "Liquid Glass" Foundation)

- **Colors**: Define the DeepEye palette (Obsidian, Cyan, Hazard Orange).
- **Styles**: Create global XAML ResourceDictionaries for:
  - `DeepEye.Buttons.xaml`: The glowing glass buttons.
  - `DeepEye.Typography.xaml`: Modern font stacks (Inter/Outfit).
  - `DeepEye.Panels.xaml`: Backgrounds, borders, and glass effects.

## 3. The Core Shell (WPF)

- **MainWindow.xaml**: The main container with:
  - Branded Title Bar.
  - OEM/Chipset Ribbon (Samsung, Xiaomi, etc.).
  - Sidebar Navigation (SideNav).
  - Content Area (where Centers are loaded).
  - Status & Risk Meter (Bottom).

## 4. Center Orchestration

- Implement `DeviceInformationCenter` View/ViewModel.
- Implement `LockFRP_Center` View/ViewModel (The primary "UnlockTool" style grid).
- Implement `LoggerCenter` (The docked terminal).

## 5. Backend Bridge

- Connect `DeviceManager` (WMI Listener) to the WPF Shell.
- Ensure the progress bars from `Operation` track correctly in the Hazard Bar.

---

## Execution Tasks

### Task 1: Initialize Modern UI Project [DONE]

- Create `src/DeepEye.UI.Modern/DeepEye.UI.Modern.csproj`.
- Set up App.xaml and MainWindow.xaml.

### Task 2: Implement Theme Engine [DONE]

- Create `src/DeepEye.UI.Modern/Themes/DeepEyeTheme.xaml`.
- Define colors and control templates.

### Task 3: Build Navigation & Layout [DONE]

- Implement the SideNav and TopRibbon.
- Set up the View/ViewModel mapping.

### Task 4: Port Device Detection [DONE]

- Connect the existing `DeviceManager` to a global `AppViewModel`.
