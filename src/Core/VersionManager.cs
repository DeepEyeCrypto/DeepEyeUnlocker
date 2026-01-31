using System;
using System.Reflection;

namespace DeepEyeUnlocker.Core
{
    public static class VersionManager
    {
        public static string AppVersion => Assembly.GetExecutingAssembly().GetName().Version?.ToString() ?? "3.0.0";
        
        public static string BuildIdentifier
        {
            get
            {
                var attr = Assembly.GetExecutingAssembly().GetCustomAttribute<AssemblyInformationalVersionAttribute>();
                if (attr != null && attr.InformationalVersion.Contains("+"))
                {
                    // Extracts the commit hash if following semantic versioning + commit (e.g., 3.0.0+abc123)
                    return attr.InformationalVersion.Split('+')[1];
                }
                return "local-dev";
            }
        }

        public static string FullVersionDisplay => $"v{AppVersion} ({BuildIdentifier})";
    }
}
