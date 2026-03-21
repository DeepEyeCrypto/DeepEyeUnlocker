use std::process::Command;

fn bash(s: &str) -> Result<String, String> {
    let out = Command::new("bash").arg("-c").arg(s).output()
        .map_err(|e| e.to_string())?;
    Ok(format!("{}\n{}", String::from_utf8_lossy(&out.stdout),
        String::from_utf8_lossy(&out.stderr)))
}

#[tauri::command]
pub fn get_signed_firmwares(identifier: String) -> Result<String, String> {
    bash(&format!(
        "curl -s 'https://api.ipsw.me/v4/device/{identifier}?type=ipsw' | \
         python3 -c \"
import json,sys
data=json.load(sys.stdin)
fw=data.get('firmwares',[])
signed=[f for f in fw if f.get('signed')]
for f in signed:
    print(f'✅ iOS {{f[\\\"version\\\"]}} | Build {{f[\\\"buildid\\\"]}} | {{round(f[\\\"filesize\\\"]/1e9,2)}} GB')
print(f'\\n{{len(signed)}} version(s) currently signed')
\" 2>&1"
    ))
}

#[tauri::command]
pub fn get_all_firmwares(identifier: String) -> Result<String, String> {
    bash(&format!(
        "curl -s 'https://api.ipsw.me/v4/device/{identifier}?type=ipsw' | \
         python3 -c \"
import json,sys
data=json.load(sys.stdin)
fw=data.get('firmwares',[])
for f in fw[:30]:
    signed = '✅' if f.get('signed') else '❌'
    print(f'{{signed}} iOS {{f[\\\"version\\\"]}} | {{f[\\\"buildid\\\"]}} | {{round(f[\\\"filesize\\\"]/1e9,2)}} GB')
\" 2>&1"
    ))
}

#[tauri::command]
pub fn download_ipsw(identifier: String, build_id: String) -> Result<String, String> {
    bash(&format!(
        "URL=$(curl -s 'https://api.ipsw.me/v4/ipsw/{identifier}/{build_id}' | \
               python3 -c 'import json,sys; d=json.load(sys.stdin); print(d[\"url\"])') && \
         FNAME=$(basename \"$URL\") && \
         mkdir -p ~/DeepEyeUnlocker/ipsw && \
         echo \"Downloading: $FNAME\" && \
         curl -L --progress-bar -o ~/DeepEyeUnlocker/ipsw/$FNAME \"$URL\" && \
         echo \"✅ Saved: ~/DeepEyeUnlocker/ipsw/$FNAME\""
    ))
}

#[tauri::command]
pub fn get_download_progress() -> Result<String, String> {
    bash(
        "ls -lh ~/DeepEyeUnlocker/ipsw/*.ipsw 2>/dev/null || \
         echo 'No IPSW files found'"
    )
}

#[tauri::command]
pub fn verify_ipsw_sha1(ipsw_path: String, identifier: String, build_id: String) -> Result<String, String> {
    bash(&format!(
        "EXPECTED=$(curl -s 'https://api.ipsw.me/v4/ipsw/{identifier}/{build_id}' | \
                    python3 -c 'import json,sys; d=json.load(sys.stdin); print(d[\"sha1sum\"])') && \
         ACTUAL=$(shasum '{ipsw_path}' | awk '{{print $1}}') && \
         echo \"Expected: $EXPECTED\" && \
         echo \"Actual:   $ACTUAL\" && \
         [ \"$EXPECTED\" = \"$ACTUAL\" ] && echo '✅ SHA1 MATCH — IPSW valid' || \
         echo '❌ SHA1 MISMATCH — IPSW corrupt!'"
    ))
}
