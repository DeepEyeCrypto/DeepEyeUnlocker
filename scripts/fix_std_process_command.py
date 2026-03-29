#!/usr/bin/env python3
"""
Script to replace std::process::Command with tauri_plugin_shell in Rust files.
Assumes a common pattern: a helper function named `bash`, `run_bash`, or `afc` that uses Command::new("bash").
"""

import os
import re
import sys
from pathlib import Path

# List of files to process (relative to workspace root)
FILES = [
    "src-tauri/src/crash_logs.rs",
    "src-tauri/src/cve.rs",
    "src-tauri/src/developer.rs",
    "src-tauri/src/diagnostics.rs",
    "src-tauri/src/frida.rs",
    "src-tauri/src/identity.rs",
    "src-tauri/src/ipsw_dl.rs",
    "src-tauri/src/nonce.rs",
    "src-tauri/src/purple.rs",
    "src-tauri/src/restore.rs",
    "src-tauri/src/shsh.rs",
    "src-tauri/src/sideloader.rs",
    "src-tauri/src/toolbox.rs",
    "src-tauri/src/vault.rs",
]

def process_file(filepath: str):
    print(f"Processing {filepath}...")
    with open(filepath, 'r') as f:
        content = f.read()
    
    # Check if file already uses tauri_plugin_shell
    if "tauri_plugin_shell::ShellExt" in content:
        print(f"  Already has tauri_plugin_shell import, skipping.")
        return False
    
    # 1. Replace import
    # Remove "use std::process::Command;"
    # Add "use tauri::AppHandle;" and "use tauri_plugin_shell::ShellExt;" if not present
    lines = content.split('\n')
    new_lines = []
    import_added = False
    for line in lines:
        if line.strip() == "use std::process::Command;":
            # Skip this line
            continue
        if "use tauri::AppHandle;" in line:
            import_added = True
        new_lines.append(line)
    
    # Insert imports after the last "use" statement or at the top
    if not import_added:
        # Find position to insert
        insert_idx = 0
        for i, line in enumerate(new_lines):
            if line.startswith("use "):
                insert_idx = i + 1
            elif line.startswith("mod ") or line.startswith("fn ") or line.startswith("pub ") or line.startswith("//"):
                # Stop at first non-use line
                break
        # Insert
        new_lines.insert(insert_idx, "use tauri::AppHandle;")
        new_lines.insert(insert_idx + 1, "use tauri_plugin_shell::ShellExt;")
    
    content = '\n'.join(new_lines)
    
    # 2. Replace helper functions
    # Pattern: fn helper_name(s: &str) -> Result<String, String> { ... Command::new("bash") ... }
    # We'll replace with async version that takes AppHandle
    # This is a simple regex; might need adjustment per file
    # We'll do a more generic replacement: replace the whole helper
    # Let's find the helper function definition
    helper_pattern = r'fn\s+(\w+)\s*\(\s*([^)]*)\s*\)\s*->\s*Result<String,\s*String>\s*\{[^}]*Command::new\("bash"\)[^}]*\}'
    match = re.search(helper_pattern, content, re.DOTALL)
    if match:
        helper_name = match.group(1)
        params = match.group(2)
        print(f"  Found helper function: {helper_name}")
        # Replace with async version
        new_helper = f"""async fn {helper_name}(app: &AppHandle, s: &str) -> Result<String, String> {{
    let output = app
        .shell()
        .command("bash")
        .args(["-c", s])
        .output()
        .await
        .map_err(|e| e.to_string())?;
    
    Ok(format!("{{}}\\n{{}}", 
        String::from_utf8_lossy(&output.stdout),
        String::from_utf8_lossy(&output.stderr)))
}}"""
        content = content[:match.start()] + new_helper + content[match.end():]
    
    # 3. Update command functions that call the helper
    # Pattern: #[tauri::command] pub fn func_name(...) -> Result<String, String> { helper_name(...) }
    # We need to:
    # - Add app: AppHandle as first parameter
    # - Make function async
    # - Change call to helper_name(&app, ...).await
    # This is complex; we'll do a simpler approach: manually update each file later
    # For now, we'll just mark that manual review is needed
    
    # Write back
    with open(filepath, 'w') as f:
        f.write(content)
    
    print(f"  Updated {filepath} (requires manual review of command functions)")
    return True

def main():
    workspace_root = Path(__file__).parent.parent
    updated_count = 0
    for rel_path in FILES:
        filepath = workspace_root / rel_path
        if not filepath.exists():
            print(f"File not found: {filepath}")
            continue
        if process_file(str(filepath)):
            updated_count += 1
    
    print(f"\nUpdated {updated_count} files.")
    print("NOTE: Command functions need manual update to add AppHandle parameter and make async.")
    print("Refer to already updated files (extraction.rs, afc.rs, backup.rs) for pattern.")

if __name__ == "__main__":
    main()