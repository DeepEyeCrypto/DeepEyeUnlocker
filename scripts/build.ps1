# DeepEyeUnlocker Build & Package Script
$config = "Release"
$version = "1.0.0"

Write-Host "🔨 Building DeepEyeUnlocker v$version ($config)..." -ForegroundColor Cyan

# Clean previous builds
if (Test-Path "artifacts") { Remove-Item -Recurse -Force "artifacts" }
New-Item -ItemType Directory -Path "artifacts"

# Build Solution
dotnet build DeepEyeUnlocker.sln -c $config

# Publish Portable
Write-Host "📦 Publishing portable version..."
dotnet publish src/DeepEyeUnlocker.csproj -c $config -r win-x64 --self-contained true /p:PublishSingleFile=true -o "artifacts/portable"

# Copy Docs
Copy-Item "README.md" "artifacts/portable/"
Copy-Item "LICENSE" "artifacts/portable/"

Write-Host "✅ Build complete. Artifacts are in the /artifacts folder." -ForegroundColor Green
