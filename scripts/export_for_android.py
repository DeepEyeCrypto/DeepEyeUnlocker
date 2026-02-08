import sqlite3
import json
import os

DB_PATH = "src/Features/ModelDiscovery/Database/model_discovery.db"
OUTPUT_PATH = "portable/android/app/src/main/assets/models.json"

# Ensure output directory exists
os.makedirs(os.path.dirname(OUTPUT_PATH), exist_ok=True)

conn = sqlite3.connect(DB_PATH)
cursor = conn.cursor()

# Read models
cursor.execute("SELECT Brand, MarketingName, ModelNumber, ChipsetFamily, OperationsJson FROM SupportedModels")
rows = cursor.fetchall()

models = []
for row in rows:
    brand, name, code, chipset, ops = row
    
    # Parse Operations JSON
    try:
        operations = json.loads(ops)
    except:
        operations = []

    models.append({
        "brand": brand,
        "name": name,
        "model": code,
        "chipset": chipset,
        "features": operations
    })

# Write JSON
with open(OUTPUT_PATH, 'w', encoding='utf-8') as f:
    json.dump(models, f, indent=2)

print(f"[Success] Exported {len(models)} models to {OUTPUT_PATH}")
conn.close()
