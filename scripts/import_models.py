import sqlite3
import csv
import json
import os
from datetime import datetime

DB_PATH = "src/Features/ModelDiscovery/Database/model_discovery.db"
CSV_PATH = "assets/models_import.csv"

# Ensure directory exists
os.makedirs(os.path.dirname(DB_PATH), exist_ok=True)

conn = sqlite3.connect(DB_PATH)
cursor = conn.cursor()

# Create table if not exists (Schema from SupportedModel.cs)
cursor.execute('''
CREATE TABLE IF NOT EXISTS SupportedModels (
    Id INTEGER PRIMARY KEY AUTOINCREMENT,
    Tool TEXT NOT NULL DEFAULT 'DeepEyeUnlocker',
    ToolVersion TEXT,
    Brand TEXT NOT NULL,
    MarketingName TEXT NOT NULL,
    ModelNumber TEXT,
    Codename TEXT,
    ChipsetFamily TEXT,
    ChipsetModel TEXT,
    OperationsJson TEXT NOT NULL,
    ModesJson TEXT NOT NULL,
    SourceUrl TEXT NOT NULL,
    SourceSection TEXT,
    FirstSeen TEXT NOT NULL,
    LastSeen TEXT NOT NULL
);
''')

cursor.execute('''
CREATE INDEX IF NOT EXISTS IX_SupportedModels_Brand_ModelNumber_Tool 
ON SupportedModels (Brand, ModelNumber, Tool);
''')

print(f"[Info] Database initialized at {DB_PATH}")

# Read CSV and Insert
try:
    with open(CSV_PATH, 'r', encoding='utf-8') as f:
        reader = csv.reader(f)
        count = 0
        skipped = 0
        
        for row in reader:
            if not row or row[0].startswith("Brand"):
                continue
                
            if len(row) < 2:
                skipped += 1
                continue
            
            brand = row[0].strip()
            model_name = row[1].strip()
            series = row[2].strip() if len(row) > 2 else ""
            year = row[3].strip() if len(row) > 3 else ""
            device_type = row[4].strip() if len(row) > 4 else "Unknown"

            # Construct Marketing Name
            marketing_name = f"{model_name} ({year})" if year else model_name

            # Capabilities
            ops = ["FRP", "Screen", "Factory Reset"]
            if "Flagship" in device_type:
                ops.append("Knox Guard Check")
            
            modes = ["MTP", "ADB", "Download Mode", "Fastboot"]
            
            # Insert into DB
            cursor.execute('''
                INSERT INTO SupportedModels (
                    Tool, Brand, MarketingName, ModelNumber, ChipsetFamily, 
                    OperationsJson, ModesJson, SourceUrl, FirstSeen, LastSeen
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ''', (
                "DeepEyeUnlocker", brand, marketing_name, model_name, "Auto-Detect",
                json.dumps(ops), json.dumps(modes), "Internal CSV Import",
                datetime.utcnow().isoformat(), datetime.utcnow().isoformat()
            ))
            count += 1

        conn.commit()
        print(f"[Success] Imported {count} models into the database.")
        print(f"[Info] Skipped {skipped} rows.")

except FileNotFoundError:
    print(f"[Error] CSV file not found: {CSV_PATH}")
except Exception as e:
    print(f"[Error] {str(e)}")
finally:
    conn.close()
