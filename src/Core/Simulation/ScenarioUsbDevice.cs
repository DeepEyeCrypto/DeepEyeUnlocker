using System;
using System.Collections.Generic;
using System.Linq;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Protocols.Usb;
using LibUsbDotNet.Main;

namespace DeepEyeUnlocker.Core.Simulation
{
    public class ReplayLogEntry
    {
        public DateTime Timestamp { get; } = DateTime.Now;
        public int StepIndex { get; set; }
        public string Label { get; set; } = string.Empty;
        public string Message { get; set; } = string.Empty;
        public bool IsError { get; set; }
    }

    public class ScenarioReplayResult
    {
        public bool IsSuccessful { get; set; }
        public bool IsError { get; set; }
        public List<ReplayLogEntry> Logs { get; } = new();
        public string ErrorMessage { get; set; } = string.Empty;
        public int LastStepIndex { get; set; }
        public string FailureReason { get; set; } = string.Empty;
    }

    public class ScenarioUsbDevice : IUsbDevice
    {
        private readonly ProtocolScenario _scenario;
        private int _currentStepIndex = 0;
        private readonly ScenarioReplayResult _result = new();

        public ScenarioUsbDevice(ProtocolScenario scenario)
        {
            _scenario = scenario;
        }

        public ProtocolScenario Scenario => _scenario;
        public ScenarioReplayResult Result => _result;
        public bool IsOpen => true;

        public IUsbEndpointReader OpenEndpointReader(ReadEndpointID readEndpointID) => new ScenarioEndpointReader(this);
        public IUsbEndpointWriter OpenEndpointWriter(WriteEndpointID writeEndpointID) => new ScenarioEndpointWriter(this);

        public Func<byte[], byte[]>? MutationHook { get; set; }

        public void Dispose()
        {
            if (_currentStepIndex < _scenario.Steps.Count && !_result.IsError)
            {
                _result.IsError = true;
                _result.FailureReason = "PrematureEnd";
                _result.ErrorMessage = $"Scenario ended prematurely at step {_currentStepIndex}/{_scenario.Steps.Count}. Missing expected step: {_scenario.Steps[_currentStepIndex].Label}";
                _result.LastStepIndex = _currentStepIndex;
            }
            else if (!_result.IsError)
            {
                _result.IsSuccessful = true;
            }
        }

        private void Log(string msg, bool isError = false, string? reason = null)
        {
            var entry = new ReplayLogEntry 
            { 
                StepIndex = _currentStepIndex, 
                Label = _currentStepIndex < _scenario.Steps.Count ? _scenario.Steps[_currentStepIndex].Label : "END",
                Message = msg, 
                IsError = isError 
            };
            _result.Logs.Add(entry);
            if (isError)
            {
                _result.IsError = true;
                _result.ErrorMessage = msg;
                _result.FailureReason = reason ?? "ProtocolMismatch";
                _result.LastStepIndex = _currentStepIndex;
            }
        }

        private class ScenarioEndpointReader : IUsbEndpointReader
        {
            private readonly ScenarioUsbDevice _parent;
            public ScenarioEndpointReader(ScenarioUsbDevice parent) => _parent = parent;

            public ErrorCode Read(byte[] buffer, int timeout, out int bytesRead)
            {
                bytesRead = 0;
                if (_parent._currentStepIndex >= _parent._scenario.Steps.Count)
                {
                    _parent.Log("Read called but no more steps in scenario", true);
                    return ErrorCode.None;
                }

                var step = _parent._scenario.Steps[_parent._currentStepIndex];
                if (step.Direction != StepDirection.DeviceToHost)
                {
                    _parent.Log($"Unexpected Read: current step expectation is {step.Direction}", true);
                    return ErrorCode.None;
                }

                if (step.DelayMs > 0) System.Threading.Thread.Sleep(step.DelayMs);

                if (step.Action == StepAction.Disconnect)
                {
                    _parent.Log("Simulated Disconnect in Reader", true, "Disconnected");
                    return ErrorCode.DeviceNotFound;
                }

                if (step.Action == StepAction.Timeout)
                {
                    _parent.Log("Simulated Timeout in Reader");
                    return ErrorCode.IoTimedOut;
                }

                var data = step.GetData();
                if (_parent.MutationHook != null)
                {
                    data = _parent.MutationHook(data);
                }

                if (data.Length > 0)
                {
                    Array.Copy(data, buffer, Math.Min(data.Length, buffer.Length));
                    bytesRead = Math.Min(data.Length, buffer.Length);
                    _parent.Log($"Read {bytesRead} bytes from device simulation: {step.Label} (Mutated: {_parent.MutationHook != null})");
                }
                else
                {
                    _parent.Log($"Simulated silence/timeout: {step.Label}");
                }

                _parent._currentStepIndex++;
                return ErrorCode.None;
            }
        }

        private class ScenarioEndpointWriter : IUsbEndpointWriter
        {
            private readonly ScenarioUsbDevice _parent;
            public ScenarioEndpointWriter(ScenarioUsbDevice parent) => _parent = parent;

            public ErrorCode Write(byte[] buffer, int timeout, out int bytesWritten) 
                => Write(buffer, 0, buffer.Length, timeout, out bytesWritten);

            public ErrorCode Write(byte[] buffer, int offset, int count, int timeout, out int bytesWritten)
            {
                bytesWritten = 0;
                if (_parent._currentStepIndex >= _parent._scenario.Steps.Count)
                {
                    _parent.Log("Write called but no more steps in scenario", true);
                    return ErrorCode.None;
                }

                var step = _parent._scenario.Steps[_parent._currentStepIndex];
                if (step.Direction != StepDirection.HostToDevice)
                {
                    _parent.Log($"Unexpected Write: current step expectation is {step.Direction}", true);
                    return ErrorCode.None;
                }

                if (step.Action == StepAction.Disconnect)
                {
                    _parent.Log("Simulated Disconnect in Writer", true, "Disconnected");
                    return ErrorCode.DeviceNotFound;
                }

                var expectedData = step.GetData();
                var actualData = buffer.Skip(offset).Take(count).ToArray();

                if (expectedData.Length != actualData.Length)
                {
                    _parent.Log($"Length mismatch in step {step.Label}. Expected {expectedData.Length} bytes, Got {actualData.Length} bytes", true, "LengthMismatch");
                }
                else if (!expectedData.SequenceEqual(actualData))
                {
                    _parent.Log($"Data mismatch in step {step.Label}. Expected {System.Convert.ToHexString(expectedData)}, Got {System.Convert.ToHexString(actualData)}", true, "DataMismatch");
                }
                else
                {
                    _parent.Log($"Validated Write matching step: {step.Label}");
                    bytesWritten = count;
                }

                _parent._currentStepIndex++;
                return ErrorCode.None;
            }
        }
    }
}
