# USB Abstraction Overview

This document summarizes the current state of the USB transport abstraction and hardware simulation in DeepEyeUnlocker.

## 1. Core Abstractions (`IUsbDevice`)

The `IUsbDevice` interface decouples protocol logic from specific USB libraries (`LibUsbDotNet`).

- **Path**: [IUsbInterfaces.cs](file:///Users/enayat/Documents/DeepEyeUnlocker/src/Protocols/Usb/IUsbInterfaces.cs)
- **Interface**:

```csharp
public interface IUsbDevice : IDisposable
{
    IUsbEndpointReader OpenEndpointReader(ReadEndpointID readEndpointID);
    IUsbEndpointWriter OpenEndpointWriter(WriteEndpointID writeEndpointID);
    bool IsOpen { get; }
}
```

## 2. Hardware Simulation (`MockUsbDevice`)

The `MockUsbDevice` provides a thread-safe environment for simulating device behavior in unit tests.

- **Path**: [MockUsbDevice.cs](file:///Users/enayat/Documents/DeepEyeUnlocker/tests/Mocks/MockUsbDevice.cs)
- **Mechanism**: Uses `ConcurrentQueue<byte[]>` for `InboundPackets` (simulating device → host) and `OutboundPackets` (simulating host → device).
- **Utility**: `EnqueuePacket<T>(T packet)` allows tests to push structured packets into the inbound stream.

## 3. Protocol Composition

Protocol engines (Sahara, Firehose, MTK) are constructed with an `IUsbDevice` dependency.

- **Sahara**: `SaharaProtocol(IUsbDevice device)`
- **MTK**: `MTKPreloader(IUsbDevice device)`, `MTKDAProtocol(IUsbDevice device)`

## 4. Current Test Pattern

Tests currently follow a manual "Arrange-Act-Assert" pattern:

1. Instantiate `MockUsbDevice`.
2. `EnqueuePacket` to simulate the device's first response.
3. Call protocol method (e.g., `ProcessHelloAsync`).
4. Assert on `OutboundPackets` to verify the host's response.

### Example: [SaharaProtocolTests.cs](file:///Users/enayat/Documents/DeepEyeUnlocker/tests/Protocols/Qualcomm/SaharaProtocolTests.cs)

```csharp
var mockDevice = new MockUsbDevice();
mockDevice.EnqueuePacket(new SaharaHelloPacket { ... });
var result = await protocol.ProcessHelloAsync();
Assert.True(result);
Assert.Equal(SaharaCommand.HelloResponse, mockDevice.OutboundPackets.First()[0]);
```
