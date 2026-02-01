using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Reflection;

namespace DeepEyeUnlocker.Core.Architecture
{
    public class PluginManager
    {
        private readonly List<IProtocolPlugin> _protocols = new();
        private readonly List<IOperationHandler> _operations = new();

        public void DiscoverPlugins(string directory)
        {
            if (!Directory.Exists(directory)) return;

            var assemblies = Directory.GetFiles(directory, "*.dll")
                .Select(file => {
                    try { return Assembly.LoadFrom(file); }
                    catch { return null; }
                })
                .Where(a => a != null);

            foreach (var assembly in assemblies!)
            {
                var protocolTypes = assembly.GetTypes()
                    .Where(t => typeof(IProtocolPlugin).IsAssignableFrom(t) && !t.IsInterface && !t.IsAbstract);
                
                foreach (var type in protocolTypes)
                {
                    if (Activator.CreateInstance(type) is IProtocolPlugin protocol)
                        _protocols.Add(protocol);
                }

                var operationTypes = assembly.GetTypes()
                    .Where(t => typeof(IOperationHandler).IsAssignableFrom(t) && !t.IsInterface && !t.IsAbstract);

                foreach (var type in operationTypes)
                {
                    if (Activator.CreateInstance(type) is IOperationHandler handler)
                        _operations.Add(handler);
                }
            }
        }

        public IProtocolPlugin? FindProtocol(string name) => 
            _protocols.FirstOrDefault(p => p.ProtocolName.Equals(name, StringComparison.OrdinalIgnoreCase));

        public IEnumerable<IOperationHandler> GetOperationsForProtocol(string protocolName) =>
            _operations.Where(o => o.TargetProtocol.Equals(protocolName, StringComparison.OrdinalIgnoreCase));

        public IEnumerable<IProtocolPlugin> LoadedProtocols => _protocols;
    }
}
