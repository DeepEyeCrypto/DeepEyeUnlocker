export const FEATURE_MAP = {
  // ── ANDROID / MTK ─────────────────────────────────
  android: {
    label: "Android Tools",
    icon: "android",
    color: "#00FF44",
    tools: [
      {
        id: "mtk_brom",
        name: "MTK BROM Bypass",
        description: "MediaTek Boot ROM — SLA/DA auth bypass",
        protocol: "BROM",
        chips: ["MT6789","MT6785","MT6765","MT6768","MT6781"],
        status: "live",
        fn: "run_mtk_brom_bypass"
      },
      {
        id: "mtk_da",
        name: "MTK DA Bypass",
        description: "Download Agent bypass — skip auth",
        protocol: "DA",
        chips: ["MT6789","MT67xx"],
        status: "live",
        fn: "run_da_bypass"
      },
      {
        id: "mtk_meta",
        name: "MTK META Mode",
        description: "Factory META mode FRP bypass",
        protocol: "META",
        status: "live",
        fn: "run_meta_bypass"
      },
      {
        id: "mtk_frp_erase",
        name: "MTK FRP Erase",
        description: "Erase FRP partition via DA protocol",
        protocol: "DA",
        status: "live",
        fn: "run_frp_erase"
      },
      {
        id: "frp_adb",
        name: "ADB FRP Bypass",
        description: "FRP bypass via ADB shell commands",
        protocol: "ADB",
        status: "live",
        fn: "run_adb_frp"
      },
      {
        id: "frp_deepeye",
        name: "DeepEye FRP Agent",
        description: "Automated FRP agent injection",
        protocol: "ADB",
        status: "live",
        fn: "run_deepeye_agent"
      },
      {
        id: "pattern_bypass",
        name: "Pattern/PIN Bypass",
        description: "Remove gesture, PIN, password lock",
        protocol: "ADB",
        status: "live",
        fn: "run_pattern_bypass"
      },
      {
        id: "screen_bypass",
        name: "Screen Lock Bypass",
        description: "Bypass Android screen lock without data loss",
        protocol: "ADB",
        status: "live",
        fn: "run_screen_bypass"
      }
    ]
  },

  // ── QUALCOMM ───────────────────────────────────────
  qualcomm: {
    label: "Qualcomm Tools",
    icon: "chip",
    color: "#FF3D00",
    tools: [
      {
        id: "qcom_edl",
        name: "Qualcomm EDL Bypass",
        description: "Emergency Download Mode — Sahara+Firehose",
        protocol: "EDL",
        chips: ["SM8550","SM7450","SM6375"],
        status: "live",
        fn: "run_qcom_edl"
      },
      {
        id: "qcom_frp",
        name: "Qualcomm FRP Erase",
        description: "Erase FRP via Firehose XML commands",
        protocol: "Firehose",
        status: "live",
        fn: "run_qcom_frp_erase"
      },
      {
        id: "qcom_sahara",
        name: "Sahara Handshake",
        description: "Sahara protocol handshake + programmer upload",
        protocol: "Sahara",
        status: "live",
        fn: "run_sahara_handshake"
      }
    ]
  },

  // ── APPLE iOS ─────────────────────────────────────
  apple: {
    label: "Apple Pro Tools",
    icon: "apple",
    color: "#FFD700",
    tools: [
      {
        id: "activation_bypass",
        name: "iCloud Activation Bypass",
        description: "Bypass iCloud lock — iOS 12–16.7",
        protocol: "USB",
        status: "live",
        fn: "run_activation_bypass"
      },
      {
        id: "mdm_bypass",
        name: "MDM Profile Bypass",
        description: "Remove MDM/DEP enrollment profile",
        protocol: "USB",
        status: "live",
        fn: "run_mdm_bypass"
      },
      {
        id: "checkm8",
        name: "checkm8 Exploit",
        description: "Bootrom exploit — A5 to A11 chips",
        protocol: "DFU",
        chips: ["A7","A8","A9","A10","A11"],
        status: "live",
        fn: "run_checkm8_new"
      },
      {
        id: "dfu_force",
        name: "Force DFU Mode",
        description: "Force device into DFU mode via USB timing",
        protocol: "DFU",
        status: "live",
        fn: "run_force_dfu"
      },
      {
        id: "ipsw_flash",
        name: "IPSW Firmware Flash",
        description: "Flash iOS .ipsw firmware via DFU",
        protocol: "iTunes",
        status: "live",
        fn: "run_ipsw_flash"
      },
      {
        id: "passcode_remove",
        name: "Passcode Removal",
        description: "Remove screen passcode (A7–A11)",
        protocol: "checkm8",
        status: "live",
        fn: "run_passcode_remove"
      },
      {
        id: "device_info_ios",
        name: "iOS Device Info",
        description: "ECID, UDID, IMEI, serial, iOS version",
        protocol: "USB",
        status: "live",
        fn: "run_ios_device_info"
      },
      {
        id: "shsh_save",
        name: "SHSH Blob Saver",
        description: "Save SHSH2 blobs for downgrades",
        protocol: "TSS",
        status: "live",
        fn: "run_shsh_save"
      }
    ]
  },

  // ── SAMSUNG ────────────────────────────────────────
  samsung: {
    label: "Samsung Tools",
    icon: "samsung",
    color: "#1428A0",
    tools: [
      {
        id: "samsung_frp",
        name: "Samsung FRP Bypass",
        description: "Google account bypass for Samsung",
        protocol: "ADB",
        status: "live",
        fn: "run_samsung_frp"
      },
      {
        id: "samsung_odin",
        name: "Samsung Odin Flash",
        description: "Flash firmware via Odin/Heimdall protocol",
        protocol: "Odin",
        status: "live",
        fn: "run_odin_flash"
      },
      {
        id: "samsung_knox",
        name: "Knox Bypass",
        description: "Knox security bypass (pre-Knox 3.x)",
        protocol: "ADB",
        status: "live",
        fn: "run_knox_bypass"
      }
    ]
  }
}
