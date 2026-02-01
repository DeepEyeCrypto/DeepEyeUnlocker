using System.Windows;

namespace DeepEye.UI.Modern
{
    public partial class App : Application
    {
        protected override void OnStartup(StartupEventArgs e)
        {
            base.OnStartup(e);
            Infrastructure.GlobalExceptionHandler.Initialize();
        }
    }
}
