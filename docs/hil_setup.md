# DeepEyeUnlocker HIL Bridge Setup & Usage

The Hardware-in-the-Loop (HIL) Bridge allows developers to validate protocol simulations against real hardware.

## Prerequisites

1. **USB Capture Source**:
    - **Windows**: Install [USBPcap](https://desowin.org/usbpcap/).
    - **Linux**: Ensure `usbmon` module is loaded (`sudo modprobe usbmon`).
    - **macOS**: Native `IOUSBFamily` logging or Wireshark.
2. **`deepeye` CLI**: Build the CLI tool in `src/CLI`.

## 1. Capturing USB Traffic

Identify your device's Vendor ID (VID) and Product ID (PID).

```bash
# Capture Sahara handshake on Qualcomm device (05c6:9008)
deepeye capture --vid 05c6 --pid 9008 --output captures/sahara_handshake.pcap
```

## 2. Converting to Scenario JSON

Convert the raw Pcap into a structured simulation scenario.

```bash
# Convert Pcap to Sahara scenario
deepeye convert --input captures/sahara_handshake.pcap --output scenarios/sahara/real_device_v1.json --protocol sahara
```

## 3. Registering a Golden Device

Save the captured scenario as a reference for future validation.

```bash
deepeye register --id "QCOM_V1" --model "Snapdragon 8 Gen 1" --protocol sahara --scenario scenarios/sahara/real_device_v1.json
```

## 4. Validating Simulations

Compare your protocol simulation (`ScenarioUsbDevice`) against the golden capture.

```bash
deepeye validate --input scenarios/sahara/simulated.json --golden scenarios/sahara/real_device_v1.json
```

## 5. Tiered CI Integration

- **Tier 1 (Replay)**: Runs on every PR. Validates that the protocol engine logic still produces traffic matching the golden scenarios.
- **Tier 2 (Real Hardware)**: Requires a self-hosted runner with the physical device attached. Triggered via `[HIL-REAL]` in commit message.

## Safety Interlocks

Fault injection is disabled by default. To enable for a specific device:

1. Verify FRP is DISABLED.
2. Add device ID to `FaultInjectionSafety.ApproveDevice()`.
3. Use `HardwareFaultInjector` with care.
