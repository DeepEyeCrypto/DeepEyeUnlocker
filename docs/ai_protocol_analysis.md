# ProtocolAutoAnalyzer (PAA): AI-Powered Reverse Engineering

The ProtocolAutoAnalyzer (PAA) is an autonomous engine designed to accelerate the support for new mobile chipsets by automatically discovering protocol structures from USB captures.

## How it Works

1. **Feature Extraction**: The engine analyzes raw `.pcap` files, calculating byte entropy, timing jitter, and clustering packets by size to identify different protocol phases (Handshake, Command, Bulk Data).
2. **AI Reasoning**: Statistical features and packet hex previews are fed into a Large Language Model (GPT-4, Claude, or local Llama3). The LLM infers packet headers, command IDs, and the underlying state machine.
3. **DSL Synthesis**: The inferred structure is converted into DeepEyeUnlocker **Scenario DSL** (JSON).
4. **Self-Healing Loop**: The generated scenario is validated against the HIL Bridge simulation. If failures occur (e.g., timing mismatches), the error logs are fed back to the AI for automatic refinement.

## Usage

### Analyzing a Capture

To analyze an unknown protocol capture:

```bash
deepeye analyze --input unknown_chipset.pcap --output scenarios/auto/new_proto.json
```

### Validating the Result

Run the generated scenario through the validator:

```bash
deepeye validate --input scenarios/auto/new_proto.json --golden references/known_good.json
```

## Safety & Accuracy

- **Human-in-the-Loop**: All AI-generated scenarios are marked as "AI Generated" and should be reviewed before use on critical hardware.
- **Redaction**: The synthesizer automatically redacts potential PII (IMEIs, serial numbers) from generated data strings.
- **Confidence Scoring**: The LLM provides a confidence score; scenarios with <0.9 confidence require manual intervention.
