using System.Windows.Forms;
using DeepEyeUnlocker.UI.Themes;

namespace DeepEyeUnlocker.UI.Themes
{
    public static class DarkTheme
    {
        public static void Apply(Control control)
        {
            control.BackColor = BrandColors.Primary;
            control.ForeColor = BrandColors.Text;

            foreach (Control child in control.Controls)
            {
                ApplyToControl(child);
            }
        }

        private static void ApplyToControl(Control control)
        {
            if (control is Button btn)
            {
                btn.FlatStyle = FlatStyle.Flat;
                btn.FlatAppearance.BorderSize = 0;
                btn.BackColor = BrandColors.Secondary;
                btn.ForeColor = BrandColors.Text;
                btn.Cursor = Cursors.Hand;
            }
            else if (control is Panel pnl)
            {
                pnl.BackColor = BrandColors.Surface;
            }
            else if (control is Label lbl)
            {
                lbl.ForeColor = BrandColors.Text;
            }
            else if (control is TextBox txt)
            {
                txt.BackColor = BrandColors.Surface;
                txt.ForeColor = BrandColors.Text;
                txt.BorderStyle = BorderStyle.FixedSingle;
            }
            else if (control is ListBox lb)
            {
                lb.BackColor = BrandColors.Surface;
                lb.ForeColor = BrandColors.Text;
                lb.BorderStyle = BorderStyle.None;
            }

            foreach (Control child in control.Controls)
            {
                ApplyToControl(child);
            }
        }
    }
}
