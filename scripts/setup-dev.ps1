# DeepEyeUnlocker Dev Setup Script
Write-Host "🚀 Setting up DeepEyeUnlocker Development Environment..." -ForegroundColor Cyan

# 1. Check for .NET 8.0 SDK
if (Get-Command dotnet -ErrorAction SilentlyContinue) {
    $version = dotnet --version
    if ($version -match "^8\.") {
        Write-Host "✅ .NET SDK 8.0 is installed ($version)." -ForegroundColor Green
    }
    else {
        Write-Host "⚠️ .NET SDK found but version is $version. DeepEyeUnlocker requires .NET 8.0." -ForegroundColor Yellow
    }
}
else {
    Write-Host "❌ .NET SDK not found. Please install .NET 8.0 SDK." -ForegroundColor Red
    Write-Host "   Mac: brew install --cask dotnet-sdk" -ForegroundColor Gray
    Write-Host "   Win: winget install Microsoft.DotNet.SDK.8" -ForegroundColor Gray
}

# 2. Restore NuGet Packages
Write-Host "📦 Restoring NuGet packages..."
dotnet restore DeepEyeUnlocker.sln

# 3. Create necessary folders
$folders = @("logs", "temp", "backups")
foreach ($folder in $folders) {
    if (!(Test-Path $folder)) {
        New-Item -ItemType Directory -Path $folder
        Write-Host "📁 Created $folder directory."
    }
}

Write-Host "✨ Setup Complete. You can now open DeepEyeUnlocker.sln in Visual Studio." -ForegroundColor Cyan
