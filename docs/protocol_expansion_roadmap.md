# V2 Protocol Expansion Roadmap

Targeting 100% coverage of the "Big Service" chipsets.

## 1. Samsung (Protocol: Odin/Loke)

- **Interface**: `ISamsungProtocol` inheriting from `IProtocolPlugin`.
- **Key Files**: PIT (Partition Information Table) parsing logic.
- **Challenge**: Seamless transition from Download Mode to Factory Binary mode.
- **Goal**: Full firmware flashing and FRP reset for 2020-2025 models.

## 2. Spreadtrum / Unisoc (Protocol: SPD-Diag)

- **Interface**: `ISpdProtocol`.
- **Handshake**: SC65xx/SC98xx specific FDL1/FDL2 downloader logic.
- **Challenge**: Precision timing for the "NIBBLE" handshake over UART-over-USB.
- **Goal**: Read/Write NVRAM, IMEI repair (where legal/repair-focused).

## 3. Apple (Protocol: Checkm8/DFU)

- **Interface**: `IAppleProtocol`.
- **Integration**: `libusb` native calls for the `checkm8` heap spray exploit.
- **Challenge**: macOS/Linux only for the exploit phase; Windows limited.
- **Goal**: DFU diagnostic mode, Boot SSH (PongoOS integration), and Data Recovery.

## 4. Huawei (Protocol: Kirin-Boot)

- **Interface**: `IKirinProtocol`.
- **Logic**: USB-to-UART bridging for Kirin 980/990/9000 bootrom access.
- **Challenge**: Proprietary RSA authentication for newer security levels.
- **Goal**: Partition access and XML-based flashing.

---

## Technical Dependencies Matrix

| Protocol | Auth Pattern | Transport | CI/CD Runner |
|----------|--------------|-----------|--------------|
| Qualcomm | XML/Firehose | USB Bulk | ANY |
| Samsung | Loke/PIT | USB Control | Win/macOS |
| Apple | Checkm8 | USB Control/Bulk | macOS/Linux |
| SPD | FDL/Bin | Serial/USB | ANY |
