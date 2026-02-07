using System;
using System.Collections.Generic;
using IronPython.Hosting;
using Microsoft.Scripting.Hosting;
using DeepEyeUnlocker.Core;

namespace DeepEyeUnlocker.Services.Foundry
{
    /// <summary>
    /// The Foundry (v5.1): Dynamic Scripting Engine.
    /// Allows the community to write custom hardware sequences using Python.
    /// </summary>
    public class FoundryEngine
    {
        private readonly ScriptEngine _engine;
        private readonly ScriptScope _scope;

        public FoundryEngine()
        {
            _engine = Python.CreateEngine();
            _scope = _engine.CreateScope();
            
            // Expose core DeepEye utilities to Python
            _scope.SetVariable("logger", new ScriptLogger());
            _scope.SetVariable("nexus", new ScriptNexusPlaceholder());
        }

        public void ExecuteScript(string code)
        {
            Logger.Info("[FOUNDRY] Initializing script execution environment...");
            try
            {
                var source = _engine.CreateScriptSourceFromString(code);
                source.Execute(_scope);
                Logger.Success("[FOUNDRY] Script execution cycle completed.");
            }
            catch (Exception ex)
            {
                Logger.Error($"[FOUNDRY] Script Runtime Error: {ex.Message}");
            }
        }

        // Helper classes for script access
        public class ScriptLogger
        {
            public void info(string msg) => Logger.Info($"[PY-SCRIPT] {msg}");
            public void success(string msg) => Logger.Success($"[PY-SCRIPT] {msg}");
            public void error(string msg) => Logger.Error($"[PY-SCRIPT] {msg}");
        }

        public class ScriptNexusPlaceholder
        {
            public string get_status() => "Connected to Neural Nexus v5.1.0";
        }
    }
}
