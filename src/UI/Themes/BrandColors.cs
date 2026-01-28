using System.Drawing;

namespace DeepEyeUnlocker.UI.Themes
{
    public static class BrandColors
    {
        public static readonly Color Primary = ColorTranslator.FromHtml("#224A47");   // Dark Teal
        public static readonly Color Secondary = ColorTranslator.FromHtml("#1B3C38"); // Deeper Teal
        public static readonly Color Accent = ColorTranslator.FromHtml("#00D4FF");    // Cyan
        public static readonly Color Background = ColorTranslator.FromHtml("#122624");
        public static readonly Color Surface = ColorTranslator.FromHtml("#1D3B38");
        
        public static readonly Color Text = Color.White;
        public static readonly Color TextSecondary = Color.FromArgb(180, 180, 200);
        
        public static readonly Color Success = Color.FromArgb(81, 207, 102);
        public static readonly Color Warning = Color.FromArgb(255, 107, 107);
        public static readonly Color Info = Color.FromArgb(77, 171, 245);
    }
}
