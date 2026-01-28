# DeepEyeUnlocker Build & Package Script
$config = "Release"
$version = "1.1.1"

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

# -------------------------------------------------------------------------
# MSI Installer Generation (Requires WiX Toolset)
# -------------------------------------------------------------------------
$isWinEnvironment = $true
if ($IsMacOS -or $IsLinux -or ($PSVersionTable.Platform -eq "Unix")) {
    $isWinEnvironment = $false
}

if (-not $isWinEnvironment) {
    Write-Host "Skipping MSI build on macOS/Linux. Please use GitHub Actions for Windows installer creation." -ForegroundColor Yellow
}
else {
    $wixCandidate = Get-Command "candle.exe" -ErrorAction SilentlyContinue
    if ($wixCandidate) {
        Write-Host "💿 WiX Toolset found. Building MSI..." -ForegroundColor Cyan
        
        $wixObj = "artifacts/installer/Product.wixobj"
        $msiOut = "artifacts/installer/DeepEyeUnlocker.msi"
        
        if (-not (Test-Path "artifacts/installer")) { New-Item -ItemType Directory -Path "artifacts/installer" }

        # Candle
        candle.exe "installer/Product.wxs" -dDeepEyeUnlocker.TargetDir=artifacts/portable/ -out $wixObj -arch x64
        
        # Light
        light.exe $wixObj -out $msiOut -b "artifacts/portable/" -ext WixUIExtension
        
        Write-Host "🎉 MSI Installer created at: $msiOut" -ForegroundColor Green
    }
    else {
        Write-Host "⚠️ WiX Toolset not found in PATH. Skipping MSI generation." -ForegroundColor Yellow
    }
}
