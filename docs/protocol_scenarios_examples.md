# Protocol Scenario Examples

These examples demonstrate how to use the Scenario DSL to describe device interactions.

## 1. Sahara Handshake Success

Describes a standard successful Sahara "Hello" exchange.

```json
{
  "name": "sahara_hello_success",
  "protocol": "sahara",
  "description": "Standard startup handshake for Qualcomm devices in EDL mode.",
  "steps": [
    {
      "direction": "device_to_host",
      "label": "device_hello",
      "data_hex": "010000003000000002000000010000000010000000000000000000000000000000000000000000000000000000000000"
    },
    {
      "direction": "host_to_device",
      "label": "host_hello_resp",
      "data_hex": "020000003000000002000000010000000010000000000000000000000000000000000000000000000000000000000000"
    }
  ],
  "expectations": {
    "max_duration_ms": 1000
  }
}
```

## 2. Firehose Configuration Success

Demonstrates XML-based Firehose command exchange.

```json
{
  "name": "firehose_open_success",
  "protocol": "firehose",
  "description": "Initialize Firehose programmer session.",
  "steps": [
    {
      "direction": "host_to_device",
      "label": "configure_req",
      "data_hex": "3c3f786d6c2076657273696f6e3d22312e3022203f3e3c646174613e3c636f6e666967757265204d656d6f72794e616d653d22656d6d632220566572626f73653d22302220416c7761797356616c69646174653d223022204d61785061796c6f616453697a65546f546172676574496e42797465733d223130343835373622202f3e3c2f646174613e"
    },
    {
      "direction": "device_to_host",
      "label": "configure_resp",
      "data_hex": "3c3f786d6c2076657273696f6e3d22312e3022203f3e3c646174613e3c726573706f6e73652076616c75653d2241434b22202f3e3c2f646174613e"
    }
  ],
  "expectations": {
    "max_duration_ms": 2000
  }
}
```

## 3. Timeout Example

Describes a scenario where the device is intentionally silent.

```json
{
  "name": "sahara_hello_timeout",
  "protocol": "sahara",
  "description": "Device fails to send HELLO within the expected window.",
  "steps": [
    {
      "direction": "device_to_host",
      "label": "silent_device",
      "data_hex": "",
      "delay_ms": 5000
    }
  ],
  "expectations": {
    "max_duration_ms": 6000
  }
}
```
