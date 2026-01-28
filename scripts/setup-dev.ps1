# DeepEyeUnlocker Dev Setup Script
Write-Host "🚀 Setting up DeepEyeUnlocker Development Environment..." -ForegroundColor Cyan

# 1. Check for .NET 6.0 SDK
if (Get-Command dotnet -ErrorAction SilentlyContinue) {
    dotnet --version
    Write-Host "✅ .NET SDK is installed." -ForegroundColor Green
} else {
    Write-Host "❌ .NET SDK not found. Please install .NET 6.0 SDK." -ForegroundColor Red
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
