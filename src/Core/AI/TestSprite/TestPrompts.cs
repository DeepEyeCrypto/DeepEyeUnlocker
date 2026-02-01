namespace DeepEyeUnlocker.Core.AI.TestSprite
{
    public static class TestPrompts
    {
        public const string ProtocolTest = @"
Test the {Protocol} protocol implementation in DeepEyeUnlocker.

Context:
- Protocol plugin: {ProtocolPluginClass}
- Scenario DSL schema: docs/protocol_scenarios.schema.json
- Existing scenarios: scenarios/{protocol_lower}/

Generate tests for:
1. Successful handshake and connection
2. Timeout handling
3. Malformed packet rejection
4. State transition validation

Use the execute_protocol_scenario tool to run tests against ScenarioUsbDevice.
Report coverage gaps and suggest new scenarios if needed.
";

        public const string OperationTest = @"
Test the {Operation} operation handler in DeepEyeUnlocker.

Context:
- Handler class: {HandlerClass}
- Protocol: {Protocol}
- Safety requirements: docs/safety_framework.md

Generate tests for:
1. Happy path execution
2. FRP state validation (should block if FRP locked)
3. Prerequisite validation
4. Error handling and rollback

Verify that ExecuteAsync respects all safety checks.
";

        public const string IntegrationTest = @"
Test end-to-end workflow: {WorkflowDescription}

Steps:
1. Device auto-detection (DeviceDiscoveryEngine)
2. Protocol selection (PluginManager)
3. Operation execution (OperationOrchestrator)
4. Progress reporting and cancellation

Simulate using ScenarioUsbDevice with golden scenarios.
Test cross-platform behavior (Windows, Linux, macOS).
";
    }
}
