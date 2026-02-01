using System.Threading.Tasks;
using System.Windows.Forms;
using DeepEyeUnlocker.Core.Services;

namespace DeepEyeUnlocker.UI.Services
{
    public class WinFormsUserInteraction : IUserInteraction
    {
        public Task<bool> ConfirmAsync(string title, string message, bool isRisk)
        {
            var icon = isRisk ? MessageBoxIcon.Warning : MessageBoxIcon.Question;
            var buttons = MessageBoxButtons.YesNo;
            
            var result = MessageBox.Show(message, title, buttons, icon);
            return Task.FromResult(result == DialogResult.Yes);
        }
    }
}
