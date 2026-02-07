using System;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using System.Windows.Shapes;

namespace DeepEye.UI.Modern.Infrastructure.Controls
{
    public class CircularProgressBar : ProgressBar
    {
        static CircularProgressBar()
        {
            DefaultStyleKeyProperty.OverrideMetadata(typeof(CircularProgressBar), new FrameworkPropertyMetadata(typeof(CircularProgressBar)));
        }

        public override void OnApplyTemplate()
        {
            base.OnApplyTemplate();
            ValueChanged += CircularProgressBar_ValueChanged;
            UpdateArc();
        }

        private void CircularProgressBar_ValueChanged(object sender, RoutedPropertyChangedEventArgs<double> e)
        {
            UpdateArc();
        }

        private void UpdateArc()
        {
            var arc = GetTemplateChild("Arc") as ArcSegment;
            if (arc == null) return;

            double angle = (Value - Minimum) / (Maximum - Minimum) * 360;
            if (angle >= 360) angle = 359.99;

            double radius = 50; // Based on design in XAML
            double angleRad = (angle - 90) * Math.PI / 180.0;

            double x = 50 + radius * Math.Cos(angleRad);
            double y = 50 + radius * Math.Sin(angleRad);

            arc.Point = new Point(x, y);
            arc.IsLargeArc = angle > 180;
        }
    }
}
