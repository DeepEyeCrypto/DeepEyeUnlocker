# Protocol Simulation TODO

This list tracks desired real-world behaviors and edge cases that still need simulation coverage in the `scenarios/` library.

## High Priority

- [x] **Firehose: Mid-transfer Disconnect**: Simulate a USB disconnect while a large partition is being read/written.
- [x] **Sahara: Malformed Hello**: Device sends a version higher than supported or invalid mode.
- [x] **MTK: Auth Required**: Simulate a device that returns an "Auth Required" status instead of BROM sync.
- [x] **FRP: Samsung persistent access**: Partial partition access simulation for Samsung-specific FRP layouts.
- [x] **FRP: Xiaomi HyperOS OTA logic**: Simulation of FRP policy enforcement after a system update.

## Medium Priority

- [x] **Firehose: CRC Mismatch**: Simulate a checksum error in an XML response.
- [x] **Generic: Latency Spike**: Measure host resilience when device responses take 1000ms+ (just below timeout).

## Low Priority

- [x] **Sahara: Memory Read Loop**: Full simulation of a memory debug session.
- [x] **MTK: Preloader to DA transition**: Multi-step simulation across protocol hand-offs.
