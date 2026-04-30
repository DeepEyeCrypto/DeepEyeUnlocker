use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ProgrammerEntry {
    pub device: String,
    pub chipset: String,
    pub programmer_name: String,
    pub sha256: String,
    pub notes: String,
}

pub fn get_programmer_db() -> Vec<ProgrammerEntry> {
    vec![
        // Xiaomi
        ProgrammerEntry {
            device: "Xiaomi Redmi Note 11".into(),
            chipset: "SM6225 (Snapdragon 680)".into(),
            programmer_name: "prog_firehose_ddr_sm6225.elf".into(),
            sha256: "ea69...".into(),
            notes: "Place in ~/.deepeye/programmers/".into(),
        },
        ProgrammerEntry {
            device: "Xiaomi Poco X3 Pro".into(),
            chipset: "SM8150 (Snapdragon 860)".into(),
            programmer_name: "prog_firehose_ddr_sm8150.elf".into(),
            sha256: "b2c1...".into(),
            notes: "Requires specific auth if bootloader locked".into(),
        },
        ProgrammerEntry {
            device: "Xiaomi Redmi Note 10 Pro".into(),
            chipset: "SM7150 (Snapdragon 732G)".into(),
            programmer_name: "prog_firehose_ddr_sm7150.elf".into(),
            sha256: "5d01...".into(),
            notes: "Vayu/Bhima variant".into(),
        },
        // Samsung
        ProgrammerEntry {
            device: "Samsung Galaxy A52".into(),
            chipset: "SM7125 (Snapdragon 720G)".into(),
            programmer_name: "prog_firehose_sm7125_samsung.elf".into(),
            sha256: "acc...".into(),
            notes: "EDL mode via test points only".into(),
        },
        ProgrammerEntry {
            device: "Samsung Galaxy S20 FE (G780G)".into(),
            chipset: "SM8250 (Snapdragon 865)".into(),
            programmer_name: "prog_firehose_sm8250_samsung.elf".into(),
            sha256: "fb2...".into(),
            notes: "Standard Samsung firehose".into(),
        },
        // Oppo / Vivo
        ProgrammerEntry {
            device: "Oppo A53".into(),
            chipset: "SM4250 (Snapdragon 460)".into(),
            programmer_name: "prog_firehose_ddr_sm4250.elf".into(),
            sha256: "cc1...".into(),
            notes: "Standard loader".into(),
        },
        ProgrammerEntry {
            device: "Vivo V20".into(),
            chipset: "SM7125 (Snapdragon 720G)".into(),
            programmer_name: "prog_firehose_ddr_sm7125_vivo.elf".into(),
            sha256: "da2...".into(),
            notes: "V2021 variant".into(),
        },
        // Realme
        ProgrammerEntry {
            device: "Realme 8 Pro".into(),
            chipset: "SM7125 (Snapdragon 720G)".into(),
            programmer_name: "prog_firehose_ddr_sm7125_realme.elf".into(),
            sha256: "331...".into(),
            notes: "RMX3081".into(),
        },
        ProgrammerEntry {
            device: "Realme C15 (Qualcomm Edition)".into(),
            chipset: "SM4250 (Snapdragon 460)".into(),
            programmer_name: "prog_firehose_ddr_sm4250.elf".into(),
            sha256: "cc1...".into(),
            notes: "RMX2195".into(),
        },
        // Others (Generic Labeled)
        ProgrammerEntry {
            device: "Generic SDM660".into(),
            chipset: "Snapdragon 660".into(),
            programmer_name: "prog_firehose_ddr_sdm660.elf".into(),
            sha256: "991...".into(),
            notes: "Supports many Nokia/Xiaomi mid-range".into(),
        },
        ProgrammerEntry {
            device: "Generic MSM8953".into(),
            chipset: "Snapdragon 625".into(),
            programmer_name: "prog_firehose_ddr_msm8953.elf".into(),
            sha256: "112...".into(),
            notes: "Legacy support for huge range of devices".into(),
        },
        ProgrammerEntry {
            device: "Generic MSM8937".into(),
            chipset: "Snapdragon 430/435".into(),
            programmer_name: "prog_firehose_ddr_msm8937.elf".into(),
            sha256: "882...".into(),
            notes: "Compatible with Redmi 3S/4/Note 4X".into(),
        },
        // Additional popular devices (v1.2.0)
        ProgrammerEntry {
            device: "Samsung Galaxy A32 5G".into(),
            chipset: "SM7125 (Snapdragon 750G)".into(),
            programmer_name: "prog_firehose_ddr_sm7125.elf".into(),
            sha256: "".into(),
            notes: "Requires EDL mode via test point".into(),
        },
        ProgrammerEntry {
            device: "OnePlus Nord".into(),
            chipset: "SM8250 (Snapdragon 865)".into(),
            programmer_name: "prog_firehose_sm8250_oneplus.elf".into(),
            sha256: "".into(),
            notes: "EDL via depth charge test point".into(),
        },
        ProgrammerEntry {
            device: "Redmi Note 10 Pro".into(),
            chipset: "SM7150 (Snapdragon 732G)".into(),
            programmer_name: "prog_firehose_ddr_sm7150.elf".into(),
            sha256: "5d01...".into(),
            notes: "Sweet/Sweetin variant".into(),
        },
        ProgrammerEntry {
            device: "Moto G Power (2021)".into(),
            chipset: "SM6115 (Snapdragon 662)".into(),
            programmer_name: "prog_firehose_ddr_sm6115.elf".into(),
            sha256: "".into(),
            notes: "Requires EDL cable/modem test point".into(),
        },
        ProgrammerEntry {
            device: "Samsung Galaxy M31".into(),
            chipset: "SM7125 (Snapdragon 720G)".into(),
            programmer_name: "prog_firehose_sm7125_samsung.elf".into(),
            sha256: "".into(),
            notes: "Standard Samsung EDL protocol".into(),
        },
        ProgrammerEntry {
            device: "Poco X3 NFC".into(),
            chipset: "SM7150 (Snapdragon 732G)".into(),
            programmer_name: "prog_firehose_ddr_sm7150.elf".into(),
            sha256: "".into(),
            notes: "Surya/Karna variant".into(),
        },
        ProgrammerEntry {
            device: "Realme GT Neo 2".into(),
            chipset: "SM8350 (Snapdragon 870)".into(),
            programmer_name: "prog_firehose_sm8350_realme.elf".into(),
            sha256: "".into(),
            notes: "Requires deep test point access".into(),
        },
        ProgrammerEntry {
            device: "Xiaomi Mi 11 Lite".into(),
            chipset: "SM7350 (Snapdragon 780G)".into(),
            programmer_name: "prog_firehose_sm7350_xiaomi.elf".into(),
            sha256: "".into(),
            notes: "Rena/Courbet variant".into(),
        },
    ]
}

// Tauri commands for frontend integration

#[tauri::command]
pub async fn get_edl_programmers() -> Vec<ProgrammerEntry> {
    get_programmer_db()
}

#[tauri::command]
pub async fn load_edl_programmer(path: String) -> Result<String, String> {
    let metadata = std::fs::metadata(&path).map_err(|e| format!("Cannot read file: {e}"))?;
    Ok(format!(
        "✅ Programmer loaded!\nFile: {path}\nSize: {} KB",
        metadata.len() / 1024
    ))
}
