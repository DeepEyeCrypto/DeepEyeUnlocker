#!/bin/bash
# Linux Installation Script for DeepEyeUnlocker

echo "🚀 Installing DeepEyeUnlocker for Linux..."

# 1. Install .NET 6.0 Runtime if missing
if ! command -v dotnet &> /dev/null; then
    echo "📦 Installing .NET 6.0 Runtime..."
    sudo apt-get update && sudo apt-get install -y dotnet-sdk-6.0
fi

# 2. Setup Udev Rules
echo "🔧 Configuring USB permissions..."
sudo cp 51-deepeye.rules /etc/udev/rules.d/
sudo udevadm control --reload-rules
sudo udevadm trigger

# 3. Create Application Directory
echo "📁 Setting up /opt/DeepEyeUnlocker..."
sudo mkdir -p /opt/DeepEyeUnlocker
sudo cp -r ../../src/bin/Release/net6.0/* /opt/DeepEyeUnlocker/

# 4. Install Desktop Shortcut
echo "🖥️ Adding to Application Menu..."
sudo cp deepeyeunlocker.desktop /usr/share/applications/

echo "✨ Installation Complete. You can find DeepEyeUnlocker in your app menu."
