using System;
using System.Collections.Generic;
using System.IO;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core.Diagnostics;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Core.Simulation;
using DeepEyeUnlocker.Protocols.Qualcomm;
using DeepEyeUnlocker.Tests.Mocks;

namespace DeepEyeUnlocker.Tests.Protocols.Fuzzing
{
    public class FuzzResult
    {
        public int Iteration { get; set; }
        public int Seed { get; set; }
        public bool Crashed { get; set; }
        public Exception? Exception { get; set; }
        public string? FailurePoint { get; set; }
    }

    public class ProtocolFuzzHarness
    {
        public async Task<List<FuzzResult>> FuzzSaharaAsync(ProtocolScenario scenario, int iterations, int baseSeed = 42)
        {
            var results = new List<FuzzResult>();
            ProtocolCoverage.Enable();

            for (int i = 0; i < iterations; i++)
            {
                int seed = baseSeed + i;
                var mutator = new ProtocolMutator(seed);
                var fuzzResult = new FuzzResult { Iteration = i, Seed = seed };

                try
                {
                    using var usb = new ScenarioUsbDevice(scenario);
                    usb.MutationHook = (data) => mutator.Mutate(data);
                    
                    var protocol = new SaharaProtocol(usb);
                    bool success = await protocol.ProcessHelloAsync();
                    
                    // If it returned false, that's a graceful failure (EXPECTED for fuzzed data)
                    // We only care about Crashes (Exceptions)
                }
                catch (Exception ex)
                {
                    fuzzResult.Crashed = true;
                    fuzzResult.Exception = ex;
                    fuzzResult.FailurePoint = ex.StackTrace?.Split('\n')[0];
                    results.Add(fuzzResult);
                }
            }

            return results;
        }

        public async Task<List<FuzzResult>> FuzzFirehoseAsync(ProtocolScenario scenario, int iterations, int baseSeed = 42)
        {
            var results = new List<FuzzResult>();
            ProtocolCoverage.Enable();

            for (int i = 0; i < iterations; i++)
            {
                int seed = baseSeed + i;
                var mutator = new ProtocolMutator(seed);
                var fuzzResult = new FuzzResult { Iteration = i, Seed = seed };

                try
                {
                    using var usb = new ScenarioUsbDevice(scenario);
                    usb.MutationHook = (data) => mutator.Mutate(data);
                    
                    var protocol = new FirehoseProtocol(usb);
                    await protocol.ConfigureAsync();
                }
                catch (Exception ex)
                {
                    fuzzResult.Crashed = true;
                    fuzzResult.Exception = ex;
                    fuzzResult.FailurePoint = ex.StackTrace?.Split('\n')[0];
                    results.Add(fuzzResult);
                }
            }

            return results;
        }
    }
}
