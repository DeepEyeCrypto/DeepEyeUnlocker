import json
import os
import zipfile
import time
from datetime import datetime

def create_deepvault_v2(export_dir, metadata, artifacts):
    """
    Creates a DeepVault v2 (.deepvault) archive.
    metadata: dict containing device info, scan results, etc.
    artifacts: list of file paths to include (hashes, logs, etc.)
    """
    try:
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        vault_name = f"DeepVault_V2_{metadata.get('udid', 'unknown')}_{timestamp}.deepvault"
        vault_path = os.path.join(export_dir, vault_name)
        
        with zipfile.ZipFile(vault_path, 'w', zipfile.ZIP_DEFLATED) as vault:
            # Write structured report
            report_data = {
                "version": "2.0.0",
                "creation_time": datetime.now().isoformat(),
                "device_info": metadata,
                "analysis_summary": {
                    "activation_locked": metadata.get("activation_locked"),
                    "fmi_enabled": metadata.get("fmi_enabled"),
                    "mdm_enrolled": metadata.get("mdm_enrolled"),
                }
            }
            vault.writestr("manifest.json", json.dumps(report_data, indent=4))
            
            # Add artifacts
            for art_path in artifacts:
                if os.path.exists(art_path):
                    vault.write(art_path, os.path.basename(art_path))
        
        return {"success": True, "vault_path": vault_path}
    except Exception as e:
        return {"success": False, "error": str(e)}

if __name__ == "__main__":
    # Internal usage via CLI mostly
    pass
