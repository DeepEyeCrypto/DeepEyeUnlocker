using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;

namespace DeepEyeUnlocker.Core.Diagnostics
{
    /// <summary>
    /// A simple, structure-aware coverage registry for protocol fuzzing and verification.
    /// Tracks which code paths (branches, states, error handlers) are exercised.
    /// </summary>
    public static class ProtocolCoverage
    {
        private static readonly ConcurrentDictionary<string, int> _hits = new ConcurrentDictionary<string, int>();
        private static bool _isEnabled = false;

        public static void Enable() => _isEnabled = true;
        public static void Disable() => _isEnabled = false;
        public static void Reset() => _hits.Clear();

        /// <summary>
        /// Records a hit on a specific code path.
        /// </summary>
        /// <param name="path">A unique string representing the code path (e.g., "Sahara_Hello_InsufficientData").</param>
        public static void Hit(string path)
        {
            if (!_isEnabled) return;
            _hits.AddOrUpdate(path, 1, (key, old) => old + 1);
        }

        /// <summary>
        /// Gets the current hit counts for all paths.
        /// </summary>
        public static IReadOnlyDictionary<string, int> GetResults() => _hits.ToDictionary(k => k.Key, v => v.Value);

        /// <summary>
        /// Gets the total number of unique paths hit.
        /// </summary>
        public static int UniquePathCount => _hits.Count;

        /// <summary>
        /// Checks if a specific path was hit.
        /// </summary>
        public static bool WasHit(string path) => _hits.ContainsKey(path);
    }
}
