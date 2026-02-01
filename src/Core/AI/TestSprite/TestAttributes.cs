using System;

namespace DeepEyeUnlocker.Core.AI.TestSprite
{
    [AttributeUsage(AttributeTargets.Method, AllowMultiple = false)]
    public class TestSpriteLayerAttribute : Attribute
    {
        public string Layer { get; }
        public TestSpriteLayerAttribute(string layer) => Layer = layer;
    }

    public static class TestSpriteLayers
    {
        public const string Unit = "unit";
        public const string Protocol = "protocol";
        public const string Integration = "integration";
        public const string UI = "ui";
    }

    [AttributeUsage(AttributeTargets.Method)]
    public class PerformanceTestAttribute : Attribute { }

    [AttributeUsage(AttributeTargets.Method)]
    public class SecurityTestAttribute : Attribute { }
}
