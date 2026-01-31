using CommunityToolkit.Mvvm.ComponentModel;

namespace DeepEye.UI.Modern.ViewModels
{
    public abstract class CenterViewModelBase : ObservableObject
    {
        public abstract string Title { get; }
    }
}
