using System;
using System.Drawing;
using System.Windows.Forms;
using DeepEyeUnlocker.Core.Services.Repositories;

namespace DeepEyeUnlocker.UI
{
    public class DiagramViewerForm : Form
    {
        private readonly DiagramMetadata _metadata;
        private readonly string _imagePath;
        
        private PictureBox _pictureBox = null!;
        private Panel _instructionPanel = null!;
        private Label _titleLabel = null!;
        private Button _closeButton = null!;

        public DiagramViewerForm(DiagramMetadata metadata, string imagePath)
        {
            _metadata = metadata;
            _imagePath = imagePath;
            InitializeComponents();
        }

        private void InitializeComponents()
        {
            this.Text = $"Diagram: {_metadata.Title}";
            this.Size = new Size(800, 600);
            this.StartPosition = FormStartPosition.CenterParent;
            this.BackColor = Color.FromArgb(20, 20, 25);
            this.FormBorderStyle = FormBorderStyle.SizableToolWindow;

            _titleLabel = new Label
            {
                Text = _metadata.Title,
                Font = new Font("Segoe UI", 12, FontStyle.Bold),
                ForeColor = Color.White,
                Dock = DockStyle.Top,
                Height = 40,
                TextAlign = ContentAlignment.MiddleCenter
            };
            this.Controls.Add(_titleLabel);

            _closeButton = new Button
            {
                Text = "Close",
                Dock = DockStyle.Bottom,
                Height = 40,
                BackColor = Color.FromArgb(0, 123, 255),
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat
            };
            _closeButton.Click += (s, e) => this.Close();
            this.Controls.Add(_closeButton);

            _instructionPanel = new Panel
            {
                Dock = DockStyle.Right,
                Width = 250,
                BackColor = Color.FromArgb(30, 30, 35),
                Padding = new Padding(10)
            };
            this.Controls.Add(_instructionPanel);

            var instTitle = new Label { Text = "Instructions", ForeColor = Color.Cyan, Font = new Font("Segoe UI", 10, FontStyle.Bold), Dock = DockStyle.Top, Height = 25 };
            _instructionPanel.Controls.Add(instTitle);

            var instText = new Label
            {
                Text = string.Join("\n\n", _metadata.Instructions),
                ForeColor = Color.White,
                Font = new Font("Segoe UI", 9),
                Dock = DockStyle.Fill,
                TextAlign = ContentAlignment.TopLeft
            };
            _instructionPanel.Controls.Add(instText);

            _pictureBox = new PictureBox
            {
                Dock = DockStyle.Fill,
                SizeMode = PictureBoxSizeMode.Zoom,
                BackColor = Color.Black
            };
            
            if (System.IO.File.Exists(_imagePath))
            {
                _pictureBox.Image = Image.FromFile(_imagePath);
            }
            else
            {
                 // Mock placeholder if image missing
                 _pictureBox.Paint += (s, e) => {
                     e.Graphics.DrawString("DIAGRAM IMAGE NOT FOUND\n" + _imagePath, 
                         new Font("Consolas", 10), Brushes.Red, 10, 10);
                 };
            }

            // Draw annotations
            _pictureBox.Paint += (s, e) => {
                foreach(var ann in _metadata.Annotations)
                {
                    float px = (float)(ann.X * _pictureBox.Width / 100);
                    float py = (float)(ann.Y * _pictureBox.Height / 100);
                    
                    e.Graphics.FillEllipse(Brushes.Red, px - 5, py - 5, 10, 10);
                    e.Graphics.DrawString(ann.Label, new Font("Segoe UI", 8, FontStyle.Bold), Brushes.Yellow, px + 8, py - 8);
                }
            };

            this.Controls.Add(_pictureBox);
        }
    }
}
