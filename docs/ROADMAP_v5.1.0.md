# ROADMAP: v5.1.0 "The Fleet Commander Update"

## 🎯 Vision

Evolve the **Neural Nexus** from a data-sharing bridge into a **remote orchestration platform**. v5.1.0 focuses on granular control over large device fleets and military-grade data protection.

---

## 🛰️ Phase 1: Remote Orchestration (Sentinel Bridge)

**Objective**: Control remote Sentinel instances via the Nexus.

- [x] **Nexus Command Relay**: Trigger operations (Unlock/Flash) on a remote machine from the master dashboard (SentinelBridgeClient live).
- [ ] **Fleet-Wide Broadcast**: Push firmware updates to all connected Sentinels simultaneously.
- [ ] **Remote Shell**: Secure UART/ADB terminal access via the Nexus Web Portal.

## 🔐 Phase 2: Secure Vault & Entropy

**Objective**: Advanced data protection for backups and logs.

- [x] **Secure Vault**: Implement AES-256-GCM encryption for all partition dumps (VaultEngine live).
- [ ] **Zero-Knowledge Architecture**: The Nexus server cannot read the partition data; keys stay on the hardware.
- [ ] **Wipe-on-Detection**: Automatic device wipe logic if unauthorized intrusion (TAMPER) is detected in the BROM environment.

## 🛠️ Phase 3: The Foundry (Community Plugins)

**Objective**: Empower the community with a scriptable engine.

- [x] **IronPython Bridge**: Create a scripting environment where users can write their own protocol extensions (FoundryEngine live).
- [ ] **Nexus Marketplace**: A secure, verified repository for community-contributed "Foundry Scripts".
- [ ] **Visual Script Builder**: A node-based UI for creating complex hardware sequences without code.

## 📊 Phase 4: Fleet Analytics Pro

**Objective**: Enterprise-level insights.

- [x] **Success Heatmaps**: Visual representation of bypass success rates per chipset/manufacturer.
- [x] **Latency Monitoring**: Track USB throughput and handshake timings across the fleet to identify hardware bottlenecks.
- [x] **Automated CVE Alerting**: Direct push notifications when a connected device type is identified as vulnerable in the global NVD.

---
**Target Release**: Early 2027
**Status**: Planning / Brainstorming
