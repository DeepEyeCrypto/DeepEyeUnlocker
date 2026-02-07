using System.Windows;

namespace DeepEye.UI.Modern
{
    public partial class MainWindow : Window
    {
        public MainWindow()
        {
            InitializeComponent();
            DataContext = new ViewModels.MainViewModel();
            
            this.Loaded += (s, e) => {
                Infrastructure.WindowCompositionHelper.EnableBlur(this);
            };
        }

        // Add logic for window dragging if needed since WindowStyle="None"
        protected override void OnMouseLeftButtonDown(System.Windows.Input.MouseButtonEventArgs e)
        {
            base.OnMouseLeftButtonDown(e);
            this.DragMove();
        }
    }
}
