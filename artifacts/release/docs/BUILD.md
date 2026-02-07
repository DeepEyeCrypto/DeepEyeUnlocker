# Build Instructions

## Prerequisites

- **Visual Studio 2022 Community** (or later) with ".NET desktop development" workload.
- **.NET 6.0 SDK** (LTS).
- **Git** for version control.
- (Optional) **Python 3.10+** for MediaTek engine integration (v1.0 uses a wrapper).

## How to Build

1. Clone the repository:

   ```bash
   git clone https://github.com/yourusername/deepeyeunlocker.git
   cd deepeyeunlocker
   ```

2. Restore NuGet packages:

   ```bash
   dotnet restore
   ```

3. Build the solution:

   ```bash
   dotnet build --configuration Release
   ```

4. Run the application:
   The binary will be located in `src/bin/Release/net6.0-windows/DeepEyeUnlocker.exe`.

## Running Tests

To run the automated test suite:

```bash
dotnet test
```

## Creating a Release

Run the PowerShell script to bundle the application:

```powershell
./scripts/release.ps1
```
