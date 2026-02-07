using System;
using System.Collections.Generic;
using System.Windows;
using System.Windows.Media;
using System.Windows.Threading;
using System.Windows.Controls;

namespace DeepEye.UI.Modern.Infrastructure.Controls
{
    public class ParticleCanvas : Canvas
    {
        private class Particle
        {
            public double X { get; set; }
            public double Y { get; set; }
            public double VelocityX { get; set; }
            public double VelocityY { get; set; }
            public double Size { get; set; }
            public double Opacity { get; set; }
        }

        private List<Particle> _particles = new();
        private Random _random = new();
        private DispatcherTimer _timer;
        private DateTime _lastUpdate;

        public static readonly DependencyProperty ParticleColorProperty =
            DependencyProperty.Register("ParticleColor", typeof(Brush), typeof(ParticleCanvas), new PropertyMetadata(new SolidColorBrush(Color.FromArgb(50, 0, 255, 255))));

        public Brush ParticleColor
        {
            get => (Brush)GetValue(ParticleColorProperty);
            set => SetValue(ParticleColorProperty, value);
        }

        public ParticleCanvas()
        {
            _timer = new DispatcherTimer(DispatcherPriority.Render);
            _timer.Interval = TimeSpan.FromMilliseconds(16);
            _timer.Tick += OnTick;
            Loaded += (s, e) => {
                InitializeParticles();
                _timer.Start();
                _lastUpdate = DateTime.Now;
            };
            Unloaded += (s, e) => _timer.Stop();
        }

        private void InitializeParticles()
        {
            _particles.Clear();
            for (int i = 0; i < 40; i++)
            {
                _particles.Add(CreateParticle());
            }
        }

        private Particle CreateParticle()
        {
            return new Particle
            {
                X = _random.NextDouble() * ActualWidth,
                Y = _random.NextDouble() * ActualHeight,
                VelocityX = (_random.NextDouble() - 0.5) * 0.5,
                VelocityY = (_random.NextDouble() - 0.5) * 0.5,
                Size = _random.NextDouble() * 3 + 1,
                Opacity = _random.NextDouble() * 0.5 + 0.1
            };
        }

        private void OnTick(object sender, EventArgs e)
        {
            double elapsed = (DateTime.Now - _lastUpdate).TotalSeconds;
            _lastUpdate = DateTime.Now;

            foreach (var p in _particles)
            {
                p.X += p.VelocityX;
                p.Y += p.VelocityY;

                if (p.X < 0) p.X = ActualWidth;
                if (p.X > ActualWidth) p.X = 0;
                if (p.Y < 0) p.Y = ActualHeight;
                if (p.Y > ActualHeight) p.Y = 0;
            }

            InvalidateVisual();
        }

        protected override void OnRender(DrawingContext dc)
        {
            base.OnRender(dc);
            foreach (var p in _particles)
            {
                dc.DrawEllipse(ParticleColor, null, new Point(p.X, p.Y), p.Size, p.Size);
            }
            
            // Draw faint connections (Neural Nexus Lines)
            for (int i = 0; i < _particles.Count; i++)
            {
                for (int j = i + 1; j < _particles.Count; j++)
                {
                    double dist = Math.Sqrt(Math.Pow(_particles[i].X - _particles[j].X, 2) + Math.Pow(_particles[i].Y - _particles[j].Y, 2));
                    if (dist < 100)
                    {
                        double opacity = (1 - (dist / 100)) * 0.2;
                        var pen = new Pen(new SolidColorBrush(Color.FromArgb((byte)(opacity * 255), 0, 255, 255)), 0.5);
                        dc.DrawLine(pen, new Point(_particles[i].X, _particles[i].Y), new Point(_particles[j].X, _particles[j].Y));
                    }
                }
            }
        }
    }
}
