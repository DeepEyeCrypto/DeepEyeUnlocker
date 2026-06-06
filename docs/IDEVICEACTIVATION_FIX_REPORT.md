# ideviceactivation Missing Binary Fix Report

## Issue Summary

**Error Message:**

```
❌ Activation failed for UDID 00008120-000924940A42201E:
   ideviceactivation exec failed: No such file or directory (os error 2)

Hint: Device may need checkm8 exploit for full bypass.
```

**Root Cause:** The `ideviceactivation` binary is not installed on the system. While `libimobiledevice` tools like `idevice_id`, `ideviceinfo`, and `idevicediagnostics` are present, `ideviceactivation` is a separate component that requires manual installation.

---

## Fix Implementation

### 1. Code Changes

**File Modified:** `src-tauri/src/commands/rebuild.rs`

#### A. Added Binary Existence Check Helper

```rust
/// Helper to check if a binary exists in PATH
fn check_tool_exists(bin: &str) -> Result<String, String> {
    let path_env = format!(
        "/usr/local/bin:/opt/homebrew/bin:{}",
        std::env::var("PATH").unwrap_or_else(|_| "/usr/bin:/bin".to_string())
    );

    let result = std::process::Command::new("which")
        .env("PATH", &path_env)
        .arg(bin)
        .output();

    match result {
        Ok(out) if out.status.success() => {
            Ok(String::from_utf8_lossy(&out.stdout).trim().to_string())
        }
        _ => Err(format!(
            "❌ Tool not found: {bin}\n\n\
             💡 Install via Homebrew:\n\
             brew install {bin}\n\n\
             Or build from source:\n\
             https://github.com/libimobiledevice/{bin}\n\n\
             Alternative: Use Finder/iTunes for activation."
        ))
    }
}
```

#### B. Updated run_binary() Function

Added pre-execution check:

```rust
pub(crate) async fn run_binary(app: &AppHandle, bin: &str, args: &[&str]) -> Result<String, String> {
    use tauri_plugin_shell::ShellExt;

    // Check if binary exists before attempting to run
    let _bin_path = check_tool_exists(bin)?;

    // ... rest of the function
}
```

#### C. Improved Activation Bypass Error Handling

The `run_activation_bypass()` function now:

1. First checks device activation state using `ideviceinfo` (always available)
2. Attempts to query state via `ideviceactivation`
3. If device is unactivated, tries to activate it
4. Provides clear, actionable error messages when `ideviceactivation` is missing

**New Error Message Example:**

```
⚠️ ideviceactivation tool not installed

Device UDID: 00008120-000924940A42201E
Current state: Unactivated

💡 To install ideviceactivation:
1. Build from source:
git clone https://github.com/libimobiledevice/libideviceactivation.git
cd libideviceactivation
./autogen.sh && make && sudo make install

2. Or use alternative methods:
• Connect device to WiFi and activate on-screen
• Use Finder (macOS Catalina+) or iTunes to activate
• For Hello screen bypass: checkm8 exploit (A7-A11 only)
```

---

## Installation Instructions

### Option 1: Build from Source (Recommended)

```bash
# Install dependencies
brew install libimobiledevice libplist openssl pkg-config autoconf automake libtool

# Clone repository
cd /tmp
git clone https://github.com/libimobiledevice/libideviceactivation.git
cd libideviceactivation

# Build and install
./autogen.sh
make
sudo make install

# Verify installation
which ideviceactivation
ideviceactivation --version
```

### Option 2: Use Alternative Methods (No Installation Required)

If you don't want to build from source, users can:

1. **Activate on Device:**
   - Connect iPhone to WiFi
   - Follow on-screen activation prompts
   - Sign in with Apple ID

2. **Use Finder/iTunes:**
   - Connect iPhone to Mac via USB
   - Open Finder (macOS Catalina+) or iTunes
   - Follow activation wizard

3. **For Hello Screen Bypass (A7-A11 chips only):**
   - Use checkm8 exploit with palera1n or checkra1n
   - Supported devices: iPhone 5s through iPhone X
   - iPhone XS and newer: NOT supported

---

## Testing

### Verify Current Installation

```bash
# Check which tools are installed
which idevice_id ideviceinfo idevicediagnostics ideviceactivation

# Expected output (ideviceactivation may be missing):
# /usr/local/bin/idevice_id
# /usr/local/bin/ideviceinfo
# /usr/local/bin/idevicediagnostics
# ideviceactivation not found
```

### Test the Fix

1. **Build the app:**

   ```bash
   cd /Users/enayat/Documents/DeepEyeUnlocker
   cargo build --release --manifest-path src-tauri/Cargo.toml
   ```

2. **Run the app and test Apple activation bypass**
   - The error message should now be clear and actionable
   - No more cryptic "No such file or directory" errors

3. **Manual test with UDID:**
   ```bash
   idevice_id -l
   # Copy UDID and test:
   ideviceinfo -u <UDID> -k ActivationState
   ```

---

## What Changed

### Before

- ❌ Cryptic error: `ideviceactivation exec failed: No such file or directory (os error 2)`
- ❌ No guidance on how to fix
- ❌ Returned error state, breaking user flow

### After

- ✅ Clear message: `⚠️ ideviceactivation tool not installed`
- ✅ Complete installation instructions provided
- ✅ Alternative methods suggested
- ✅ Returns OK state with helpful message (not error)
- ✅ Checks binary existence before execution

---

## Git Commit

```
commit 12b1c23
fix(apple): check ideviceactivation exists before invoke — show install hint if missing

- Added check_tool_exists() helper to verify binary availability
- Updated run_binary() to check tool exists before execution
- Improved run_activation_bypass() error handling with clear instructions
- Provides installation guide and alternative methods when tool missing
- Returns helpful message instead of cryptic 'No such file or directory' error
```

---

## Impact

- **User Experience:** Dramatically improved - users now know exactly what to do
- **Error Handling:** More robust - checks before execution
- **Maintainability:** Reusable `check_tool_exists()` helper for all binary calls
- **Compatibility:** Works with or without `ideviceactivation` installed

---

## Next Steps

1. **For Users:** Follow installation instructions above to install `ideviceactivation`
2. **For Developers:** Consider adding `ideviceactivation` to Homebrew formula if possible
3. **Future Enhancement:** Package `ideviceactivation` with the app bundle

---

## References

- [libideviceactivation GitHub](https://github.com/libimobiledevice/libideviceactivation)
- [libimobiledevice Project](https://libimobiledevice.org/)
- [checkm8 Exploit](https://github.com/axi0mX/ipwndfu)
