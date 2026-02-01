# Protocol Simulation TODO

This list tracks desired real-world behaviors and edge cases that still need simulation coverage in the `scenarios/` library.

## High Priority

- [ ] **Firehose: Mid-transfer Disconnect**: Simulate a USB disconnect while a large partition is being read/written.
- [ ] **Sahara: Malformed Hello**: Device sends a version higher than supported or invalid mode.
- [ ] **MTK: Auth Required**: Simulate a device that returns an "Auth Required" status instead of BROM sync.
- [ ] **FRP: Samsung persistent access**: Partial partition access simulation for Samsung-specific FRP layouts.
- [ ] **FRP: Xiaomi HyperOS OTA logic**: Simulation of FRP policy enforcement after a system update.

## Medium Priority

- [ ] **Firehose: CRC Mismatch**: Simulate a checksum error in an XML response.
- [ ] **Generic: Latency Spike**: Measure host resilience when device responses take 1000ms+ (just below timeout).

## Low Priority

- [ ] **Sahara: Memory Read Loop**: Full simulation of a memory debug session.
- [ ] **MTK: Preloader to DA transition**: Multi-step simulation across protocol hand-offs.
