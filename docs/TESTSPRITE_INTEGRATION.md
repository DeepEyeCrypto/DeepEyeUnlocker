# TestSprite AI Integration: Autonomous Testing & Self-Healing

DeepEyeUnlocker now integrates **TestSprite AI** as its primary autonomous testing and validation layer, transforming the project's quality assurance into a self-healing, AI-driven ecosystem.

## Core Components

### 1. [MCP Bridge](file:///Users/enayat/Documents/DeepEyeUnlocker/src/DeepEyeUnlocker.MCP/Program.cs)

A dedicated Model Context Protocol (MCP) server that exposes internal tools to TestSprite:

- `discover_tests`: Automatically scans the codebase for testable protocols and operation handlers.
- `execute_protocol_scenario`: Runs AI-generated tests against the **ScenarioUsbDevice** simulation.
- `get_coverage_report`: Provides real-time feedback on protocol path coverage.

### 2. [Autonomous Test Generation](file:///Users/enayat/Documents/DeepEyeUnlocker/src/Core/AI/TestSprite/TestPrompts.cs)

Pre-defined natural language templates allow TestSprite to:

- Generate **Protocol Tests** for handshake logic and timeout handling.
- Generate **Operation Tests** that verify safety checks (FRP, Prerequisites).
- Generate **Integration Tests** for full cross-platform device workflows.

### 3. [Self-Healing Architecture](file:///Users/enayat/Documents/DeepEyeUnlocker/src/Core/AI/TestSprite/Healers/ScenarioSchemaHealer.cs)

Tests stay green even as the architecture evolves:

- **Scenario Healer**: Automatically corrects JSON scenarios when the DSL schema changes.
- **Signature Healer**: Updates test methods when plugin interfaces or method signatures are refactored.

### 4. [AI-Driven Debugging](file:///Users/enayat/Documents/DeepEyeUnlocker/src/Core/AI/TestSprite/AiDebuggingEngine.cs)

When a test fails, TestSprite provides:

- **Root Cause Analysis**: Categorizes bugs (Race conditions, logic errors, timing drifts).
- **Fix Suggestions**: Generates specific code snippets and patches to resolve the issue.

### 5. [CI/CD Automation](file:///Users/enayat/Documents/DeepEyeUnlocker/.github/workflows/testsprite.yml)

Fully automated testing pipeline in GitHub Actions that performs autonomous discovery, execution, and self-healing on every PR.

## Usage

In your IDE (Cursor/VS Code) with the TestSprite MCP enabled:

- `TestSprite, generate tests for QualcommV2Plugin`
- `TestSprite, why did the MTK Preloader test fail?`
- `TestSprite, heal all scenarios for the new v2.1 schema`

## Multi-Layer Coverage

Tests are categorized using **[specialized attributes](file:///Users/enayat/Documents/DeepEyeUnlocker/src/Core/AI/TestSprite/TestAttributes.cs)**:

- `@TestSpriteLayer("unit")`: Parser logic.
- `@TestSpriteLayer("protocol")`: Simulation scenarios.
- `@TestSpriteLayer("integration")`: UI and Orchestrator.
- `@PerformanceTest`: Latency and throughput.
- `@SecurityTest`: FRP and data sanitization.
