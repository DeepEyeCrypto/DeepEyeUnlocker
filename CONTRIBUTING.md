# Contributing to DeepEyeUnlocker

This repository enforces production-grade architecture and CI gates for Android, Tauri/Rust, and frontend modules.

## Required stack and build baseline

- Android: Kotlin + Jetpack Compose + Hilt + NDK + libusb
- Desktop: Tauri v2 + Rust + React/TypeScript
- Scripts: Python 3.10+
- Java: 17

## Non-negotiable architecture constraints

### Android

- `ForegroundService` must own all USB sessions.
- All USB I/O must execute on `Dispatchers.IO`.
- Use `SupervisorJob`; never use `GlobalScope`.
- Thread a `sessionId` UUID through logs.
- For Compose state, use lifecycle-safe collectors (`collectAsStateWithLifecycle`).
- Keep business logic out of composables.

### Tauri/Rust

- Use `tauri_plugin_shell` APIs only.
- Do not use `std::process::Command`.
- Use `AppHandle` in Tauri command boundaries.
- Avoid `unwrap()` on production paths; convert to `Result<_, String>` at Tauri boundary.
- Use `app.emit()` event emission path only.

## Apple Device Rules (Stage 14)

- Apple USB operations are tool-mediated only (`irecovery` / `ideviceinfo` / `idevicerestore`).
- Never call raw USB `bulkTransfer` on Apple endpoints.
- DFU mode sends no ACK; verify DFU by PID `0x1227`.
- Pwned DFU confirmation requires interface-count `== 5` validation.

## Stability grep policy (must trend to zero)

The CI pipelines enforce these checks:

- `GlobalScope` usage in Android sources
- `std::process::Command` in Tauri Rust sources
- `window.emit` in Tauri Rust sources
- Raw `.collectAsState()` in Android UI package
- fake `delay(8xx|9xx)` patterns
- direct raw `.bulkTransfer(` outside approved extension wrappers

## Pull request checklist

- [ ] Code compiles locally for modified modules.
- [ ] Added/updated tests for behavior changes.
- [ ] No new grep-policy violations introduced.
- [ ] Kotlin UI uses lifecycle-safe state collection.
- [ ] Rust command changes preserve `Result<_, String>` at the Tauri boundary.
- [ ] Change log entry added when shipping user-visible functionality.

## CI workflows

- `.github/workflows/android.yml`: Android audit, test, and release build artifact.
- `.github/workflows/tauri.yml`: Rust audit + clippy/tests and frontend TS/Vitest checks.
- `.github/workflows/auto_release.yml`: tag-driven release notes extraction and artifact publishing.

## Commit quality bar

- Keep changes scoped and reviewable.
- Do not commit placeholders or TODO-only implementations.
- Prefer explicit error handling and deterministic logs.
