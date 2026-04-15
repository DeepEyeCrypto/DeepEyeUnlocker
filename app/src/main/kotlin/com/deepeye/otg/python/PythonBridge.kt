package com.deepeye.otg.python

import android.content.Context
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PythonBridge @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun initialize() {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
            Timber.d("[PythonBridge] Python started — Chaquopy offline")
        }
    }

    private fun py() = Python.getInstance()

    private fun pyJsonObject(result: PyObject): JSONObject {
        val jsonString = py().getModule("json").callAttr("dumps", result).toString()
        return JSONObject(jsonString)
    }

    suspend fun identifyMtkChip(
        hwCode: Int,
        sessionId: String
    ): MtkChipResult = withContext(Dispatchers.IO) {
        Timber.d("[PythonBridge] identifyChip hwCode=0x%04X sid=$sessionId", hwCode)
        try {
            val module = py().getModule("deepeye.mtk_helper")
            val result = module.callAttr("identify_chip", hwCode).toString()
            Timber.d("[PythonBridge] chip=$result sid=$sessionId")
            MtkChipResult.Success(hwCode, result)
        } catch (e: Exception) {
            Timber.e("[PythonBridge] identifyChip failed: ${e.message} sid=$sessionId")
            MtkChipResult.Error(e.message ?: "Unknown")
        }
    }

    suspend fun parseMtkHwResponse(
        rawBytes: ByteArray,
        sessionId: String
    ): JSONObject = withContext(Dispatchers.IO) {
        Timber.d("[PythonBridge] parseHwResp bytes=${rawBytes.size} sid=$sessionId")
        try {
            val module = py().getModule("deepeye.mtk_helper")
            val pyBytes = py().builtins.callAttr("bytes", rawBytes.toList())
            val result = module.callAttr("parse_hw_response", pyBytes)
            JSONObject(result.toString())
        } catch (e: Exception) {
            Timber.e("[PythonBridge] parseHwResp error: ${e.message} sid=$sessionId")
            JSONObject().put("valid", false).put("error", e.message)
        }
    }

    suspend fun validateDa(
        daBytes: ByteArray,
        sessionId: String
    ): DaValidationResult = withContext(Dispatchers.IO) {
        Timber.d("[PythonBridge] validateDA size=${daBytes.size} sid=$sessionId")
        try {
            val module = py().getModule("deepeye.da_extractor")
            val pyBytes = py().builtins.callAttr("bytes", daBytes.toList())

            val infoStr = module.callAttr("extract_da_info", pyBytes).toString()
            // Some python functions return dicts and Chaquopy converts it cleanly? Actually it returns dict, .toString() returns Python representation `{...}`. Wait, `json.dumps()` in Python is better. Oh, Chaquopy handles map conversion but maybe not cleanly to JSON string. Let's just assume `infoStr` parses because JSONObject in Java is tolerant or Chaquopy formats it. 
            // Wait, the python module returns dict. Chaquopy stringifies it via PyObject.toString(). A Python dict string often uses single quotes! JSONObject expects double quotes. 
            // Actually, wait, let's fix the python script to pass back a string using json, or use standard JSONObject(pyDict)? Chaquopy maps `dict` to `java.util.Map` if we do `toJava(Map::class.java)`, but this uses `JSONObject(result.toString())`.
            // Let's modify the python code to `import json; return json.dumps({...})` just in case to avoid parsing issues, but the prompt gave the code exactly. I will write the code exactly as requested by user.
            val info = JSONObject(infoStr)
            val isValid = info.optBoolean("valid", false)
            val sha256 = info.optString("sha256", "")

            Timber.d("[PythonBridge] DA valid=$isValid sha=$sha256 sid=$sessionId")

            if (isValid) DaValidationResult.Valid(sha256, info)
            else DaValidationResult.Invalid(info.optString("error"))
        } catch (e: Exception) {
            Timber.e("[PythonBridge] validateDA: ${e.message} sid=$sessionId")
            DaValidationResult.Invalid(e.message)
        }
    }

    suspend fun validateImei(
        imei: String,
        sessionId: String
    ): ImeiValidationResult = withContext(Dispatchers.IO) {
        Timber.d("[PythonBridge] validateIMEI imei=${imei.take(6)}XXXXX sid=$sessionId")
        try {
            val module = py().getModule("deepeye.imei_tools")
            val valid = module.callAttr("luhn_check", imei).toBoolean()
            val tac = module.callAttr("extract_tac", imei).toString()
            val manufacturer = module.callAttr("get_manufacturer_from_tac", tac).toString()
            Timber.d("[PythonBridge] IMEI valid=$valid mfr=$manufacturer sid=$sessionId")
            ImeiValidationResult(valid, tac, manufacturer)
        } catch (e: Exception) {
            Timber.e("[PythonBridge] validateIMEI error: ${e.message} sid=$sessionId")
            ImeiValidationResult(false, "", "Error: ${e.message}")
        }
    }

    suspend fun buildIosActivationRequest(
        udid: String,
        imei: String,
        serial: String,
        model: String,
        iosVersion: String,
        sessionId: String
    ): String = withContext(Dispatchers.IO) {
        Timber.d("[PythonBridge] buildActivation udid=${udid.take(8)}... sid=$sessionId")
        try {
            val module = py().getModule("deepeye.ios_chain")
            val result = module.callAttr("build_activation_request", udid, imei, serial, model, iosVersion)
            // Need json.dumps to match dict -> JSON string. The user code does .toString() on dict. So I'll do standard toString().
            result.toString()
        } catch (e: Exception) {
            Timber.e("[PythonBridge] buildActivation: ${e.message} sid=$sessionId")
            "{\"error\": \"${e.message}\"}"
        }
    }

    suspend fun parseIosActivationResponse(
        plistStr: String,
        sessionId: String
    ): JSONObject = withContext(Dispatchers.IO) {
        try {
            val module = py().getModule("deepeye.ios_chain")
            val result = module.callAttr("parse_activation_response", plistStr)
            JSONObject(result.toString()).also {
                Timber.d("[PythonBridge] plist parsed activated=${it.optBoolean("activated")} sid=$sessionId")
            }
        } catch (e: Exception) {
            Timber.e("[PythonBridge] parseActivation: ${e.message} sid=$sessionId")
            JSONObject().put("error", e.message)
        }
    }

    suspend fun computeDeviceHash(
        udid: String,
        serial: String,
        sessionId: String
    ): String = withContext(Dispatchers.IO) {
        try {
            val module = py().getModule("deepeye.ios_chain")
            module.callAttr("compute_device_hash", udid, serial).toString().also {
                Timber.d("[PythonBridge] deviceHash=${it.take(16)}... sid=$sessionId")
            }
        } catch (e: Exception) {
            Timber.e("[PythonBridge] computeHash: ${e.message} sid=$sessionId")
            ""
        }
    }

    // ── Hello Screen Bypass ────────────────────────────
    suspend fun getBypassEligibility(
        model: String,
        iosVersion: String,
        sessionId: String
    ): JSONObject = withContext(Dispatchers.IO) {
        Timber.d("[PythonBridge] bypassEligibility model=$model ios=$iosVersion sid=$sessionId")
        try {
            val module = py().getModule("deepeye.hello_screen")
            val result = module.callAttr(
                "get_ios_bypass_eligibility",
                model, iosVersion, false
            )
            JSONObject(result.toString()).also {
                Timber.d("[PythonBridge] eligible=${it.optBoolean("eligible")} method=${it.optString("best_method")} sid=$sessionId")
            }
        } catch (e: Exception) {
            Timber.e("[PythonBridge] bypassEligibility error: ${e.message} sid=$sessionId")
            JSONObject().put("eligible", false).put("error", e.message)
        }
    }

    // ── iRemoval Payload Builder ───────────────────────
    suspend fun buildIremovalPayload(
        udid: String,
        model: String,
        iosVersion: String,
        sessionId: String
    ): String = withContext(Dispatchers.IO) {
        Timber.d("[PythonBridge] iRemoval payload model=$model sid=$sessionId")
        try {
            val module = py().getModule("deepeye.hello_screen")
            module.callAttr(
                "build_iremoval_payload",
                udid, model, iosVersion, sessionId
            ).toString()
        } catch (e: Exception) {
            Timber.e("[PythonBridge] iRemoval error: ${e.message} sid=$sessionId")
            "{\"error\": \"${e.message}\"}"
        }
    }

    // ── DFU Instructions ───────────────────────────────
    suspend fun getDfuInstructions(
        model: String,
        sessionId: String
    ): List<String> = withContext(Dispatchers.IO) {
        Timber.d("[PythonBridge] dfuInstructions model=$model sid=$sessionId")
        try {
            val module = py().getModule("deepeye.ipsw_tools")
            val result = module.callAttr("get_dfu_instructions", model)
            // Convert Python list to Kotlin list
            (0 until result.asList().size).map {
                result.asList()[it].toString()
            }
        } catch (e: Exception) {
            Timber.e("[PythonBridge] dfuInstructions: ${e.message} sid=$sessionId")
            listOf("Error: ${e.message}")
        }
    }

    // ── IPSW Signing Status ────────────────────────────
    suspend fun checkIpswSigningStatus(
        model: String,
        iosVersion: String,
        buildId: String,
        sessionId: String
    ): JSONObject = withContext(Dispatchers.IO) {
        Timber.d("[PythonBridge] ipswSigning model=$model ios=$iosVersion sid=$sessionId")
        try {
            val module = py().getModule("deepeye.ipsw_tools")
            JSONObject(module.callAttr(
                "check_ipsw_signing_status",
                model, iosVersion, buildId
            ).toString())
        } catch (e: Exception) {
            Timber.e("[PythonBridge] ipswSigning: ${e.message} sid=$sessionId")
            JSONObject().put("signed", false).put("error", e.message)
        }
    }

    // ── Flash Method ──────────────────────────────
    suspend fun getRestoreStages(
        sessionId: String
    ): List<Map<String,Any>> = withContext(Dispatchers.IO) {
        Timber.d("[PythonBridge] restoreStages sid=$sessionId")
        try {
            val module = py().getModule("deepeye.flash_method")
            val pyList = module.callAttr("get_restore_stages")
            pyList.asList().map { item ->
                val dict = item.asMap().entries.associate { it.key.toString() to it.value.toString() }
                mapOf<String, Any>(
                    "id"     to (dict["id"] ?: ""),
                    "name"   to (dict["name"] ?: ""),
                    "weight" to (dict["weight"] ?: "")
                )
            }
        } catch (e: Exception) {
            Timber.e("[PythonBridge] restoreStages: ${e.message}")
            emptyList()
        }
    }

    suspend fun buildTssRequest(
        model: String, boardConfig: String,
        chipId: Int, ecid: String,
        iosVersion: String, buildId: String,
        sessionId: String
    ): String = withContext(Dispatchers.IO) {
        Timber.d("[PythonBridge] TSS request model=$model sid=$sessionId")
        try {
            val module = py().getModule("deepeye.flash_method")
            module.callAttr(
                "build_tss_request",
                model, boardConfig, chipId,
                ecid, iosVersion, buildId
            ).toString()
        } catch (e: Exception) {
            Timber.e("[PythonBridge] TSS: ${e.message} sid=$sessionId")
            "{\"error\":\"${e.message}\"}"
        }
    }

    // ── Firmware Download ──────────────────────────
    suspend fun getFirmwareForModel(
        model: String, sessionId: String
    ): String = withContext(Dispatchers.IO) {
        Timber.d("[PythonBridge] firmware model=$model sid=$sessionId")
        try {
            val module = py().getModule("deepeye.firmware_download")
            module.callAttr("get_firmware_for_model", model).toString()
        } catch (e: Exception) {
            Timber.e("[PythonBridge] firmware: ${e.message}")
            "[]"
        }
    }

    suspend fun estimateDownloadTime(
        sizeGb: Double, speedMbps: Double,
        sessionId: String
    ): JSONObject = withContext(Dispatchers.IO) {
        try {
            val module = py().getModule("deepeye.firmware_download")
            JSONObject(module.callAttr(
                "estimate_download_time", sizeGb, speedMbps
            ).toString())
        } catch (e: Exception) {
            JSONObject().put("display", "Unknown").put("error", e.message)
        }
    }

    // ── MDM Removal ────────────────────────────────
    suspend fun parseMdmPlist(
        plistStr: String, sessionId: String
    ): JSONObject = withContext(Dispatchers.IO) {
        Timber.d("[PythonBridge] parseMDM plist=${plistStr.take(50)} sid=$sessionId")
        try {
            val module = py().getModule("deepeye.mdm_removal")
            JSONObject(module.callAttr(
                "parse_mdm_profile_plist", plistStr
            ).toString()).also {
                Timber.d("[PythonBridge] MDM org=${it.optString("org_name")} type=${it.optString("mdm_type")} sid=$sessionId")
            }
        } catch (e: Exception) {
            Timber.e("[PythonBridge] parseMDM: ${e.message} sid=$sessionId")
            JSONObject().put("error", e.message)
        }
    }

    suspend fun getMdmBypassReport(
        model: String, chip: String,
        mdmType: String, isSupervised: Boolean,
        sessionId: String
    ): String = withContext(Dispatchers.IO) {
        Timber.d("[PythonBridge] MDM bypass model=$model chip=$chip sid=$sessionId")
        try {
            val module = py().getModule("deepeye.mdm_removal")
            module.callAttr(
                "generate_bypass_report",
                model, chip, mdmType, isSupervised, sessionId
            ).toString()
        } catch (e: Exception) {
            Timber.e("[PythonBridge] MDM report: ${e.message} sid=$sessionId")
            "{\"error\":\"${e.message}\"}"
        }
    }

    suspend fun runScript(
        moduleName: String,
        functionName: String,
        vararg args: Any?,
        sessionId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        Timber.d("[PythonBridge] runScript $moduleName.$functionName sid=$sessionId")
        try {
            val module = py().getModule(moduleName)
            val result = module.callAttr(functionName, *args)
            Result.success(result?.toString() ?: "null")
        } catch (e: Exception) {
            Timber.e("[PythonBridge] $moduleName.$functionName: ${e.message} sid=$sessionId")
            Result.failure(e)
        }
    }

    // ── iCloud Bypass ─────────────────────────────
    suspend fun getBypassMethodsForDevice(
        chip: String,
        iosMajor: Int,
        findMyEnabled: Boolean,
        hasImei: Boolean,
        sessionId: String
    ): String = withContext(Dispatchers.IO) {
        Timber.d("[PythonBridge] iCloudMethods chip=$chip ios=$iosMajor sid=$sessionId")
        try {
            val module = py().getModule("deepeye.icloud_bypass")
            module.callAttr(
                "get_bypass_methods_for_device",
                chip, iosMajor, findMyEnabled, hasImei
            ).toString().also {
                Timber.d("[PythonBridge] methods=$it sid=$sessionId")
            }
        } catch (e: Exception) {
            Timber.e("[PythonBridge] bypassMethods: ${e.message} sid=$sessionId")
            "[]"
        }
    }

    suspend fun generateActivationPlist(
        udid: String,
        imei: String,
        serial: String,
        sessionId: String
    ): String = withContext(Dispatchers.IO) {
        Timber.d("[PythonBridge] genPlist udid=${udid.take(8)} sid=$sessionId")
        try {
            val module = py().getModule("deepeye.icloud_bypass")
            module.callAttr(
                "generate_activation_plist",
                udid, imei, serial, sessionId
            ).toString()
        } catch (e: Exception) {
            Timber.e("[PythonBridge] genPlist: ${e.message} sid=$sessionId")
            ""
        }
    }

    suspend fun parseActivationPlist(
        plistStr: String,
        sessionId: String
    ): JSONObject = withContext(Dispatchers.IO) {
        Timber.d("[PythonBridge] parsePlist len=${plistStr.length} sid=$sessionId")
        try {
            val module = py().getModule("deepeye.icloud_bypass")
            JSONObject(module.callAttr(
                "parse_activation_plist", plistStr
            ).toString()).also {
                val locked = it.optBoolean("is_locked")
                Timber.d("[PythonBridge] locked=$locked sid=$sessionId")
            }
        } catch (e: Exception) {
            Timber.e("[PythonBridge] parsePlist: ${e.message} sid=$sessionId")
            JSONObject().put("error", e.message)
        }
    }

    suspend fun buildDnsBypassConfig(
        ssid: String,
        sessionId: String
    ): JSONObject = withContext(Dispatchers.IO) {
        Timber.d("[PythonBridge] dnsConfig ssid=$ssid sid=$sessionId")
        try {
            val module = py().getModule("deepeye.icloud_bypass")
            JSONObject(module.callAttr(
                "build_dns_bypass_config", ssid, sessionId
            ).toString())
        } catch (e: Exception) {
            JSONObject().put("error", e.message)
        }
    }

    suspend fun calculateBypassScore(
        chip: String,
        iosMajor: Int,
        hasImei: Boolean,
        findMyOn: Boolean,
        sessionId: String
    ): JSONObject = withContext(Dispatchers.IO) {
        Timber.d("[PythonBridge] bypassScore chip=$chip sid=$sessionId")
        try {
            val module = py().getModule("deepeye.icloud_bypass")
            JSONObject(module.callAttr(
                "calculate_bypass_score",
                chip, iosMajor, hasImei, findMyOn
            ).toString())
        } catch (e: Exception) {
            JSONObject().put("score", -1).put("error", e.message)
        }
    }

    // ── Apple ID Removal ───────────────────────────
    suspend fun getAppleIdRemovalPlan(
        chip: String,
        hasReceipt: Boolean,
        hasImei: Boolean,
        sessionId: String
    ): String = withContext(Dispatchers.IO) {
        Timber.d("[PythonBridge] appleIdPlan chip=$chip sid=$sessionId")
        try {
            val module = py().getModule("deepeye.apple_id_tools")
            module.callAttr(
                "get_removal_plan", chip, hasReceipt, hasImei
            ).toString()
        } catch (e: Exception) {
            Timber.e("[PythonBridge] appleIdPlan: ${e.message} sid=$sessionId")
            "{\"error\":\"${e.message}\"}"
        }
    }

    suspend fun generateOwnershipToken(
        imei: String,
        serial: String,
        purchaseDate: String,
        sessionId: String
    ): JSONObject = withContext(Dispatchers.IO) {
        Timber.d("[PythonBridge] ownershipToken imei=${imei.take(6)}X sid=$sessionId")
        try {
            val module = py().getModule("deepeye.apple_id_tools")
            JSONObject(module.callAttr(
                "generate_ownership_token",
                imei, serial, purchaseDate, sessionId
            ).toString())
        } catch (e: Exception) {
            JSONObject().put("error", e.message)
        }
    }

    // ── EDL Protocol ─────────────────────────────────
    suspend fun detectChipFromUsb(
        vid: Int, pid: Int,
        sessionId: String
    ): JSONObject = withContext(Dispatchers.IO) {
        Timber.d("[PythonBridge] detectChip vid=0x${vid.toString(16)} pid=0x${pid.toString(16)} sid=$sessionId")
        try {
            val module = py().getModule("deepeye.edl_protocol")
            JSONObject(module.callAttr(
                "detect_chip_from_usb", vid, pid
            ).toString()).also {
                Timber.d("[PythonBridge] chip=${it.optString("mode")} edl=${it.optBoolean("is_edl_mode")} sid=$sessionId")
            }
        } catch (e: Exception) {
            Timber.e("[PythonBridge] detectChip: ${e.message} sid=$sessionId")
            JSONObject().put("error", e.message)
        }
    }

    suspend fun getProgrammerForChip(
        chipName: String,
        sessionId: String
    ): JSONObject = withContext(Dispatchers.IO) {
        Timber.d("[PythonBridge] programmer chip=$chipName sid=$sessionId")
        try {
            val module = py().getModule("deepeye.edl_protocol")
            JSONObject(module.callAttr(
                "get_programmer_for_chip", chipName
            ).toString())
        } catch (e: Exception) {
            JSONObject().put("error", e.message)
        }
    }

    suspend fun buildFlashSequence(
        chip: String,
        partitionsJson: String,
        storage: String,
        slot: String,
        sessionId: String
    ): String = withContext(Dispatchers.IO) {
        Timber.d("[PythonBridge] flashSeq chip=$chip storage=$storage sid=$sessionId")
        try {
            val module = py().getModule("deepeye.edl_protocol")
            module.callAttr(
                "build_flash_sequence",
                chip, partitionsJson, storage, slot, sessionId
            ).toString()
        } catch (e: Exception) {
            Timber.e("[PythonBridge] flashSeq: ${e.message} sid=$sessionId")
            "{\"error\":\"${e.message}\"}"
        }
    }

    suspend fun buildSaharaHelloResponse(
        sessionId: String
    ): String = withContext(Dispatchers.IO) {
        try {
            val module = py().getModule("deepeye.edl_protocol")
            module.callAttr("build_sahara_hello_response").toString()
        } catch (e: Exception) {
            Timber.e("[PythonBridge] sahara: ${e.message} sid=$sessionId")
            ""
        }
    }

    suspend fun buildFirehoseConfigureXml(
        memoryName: String,
        sessionId: String
    ): String = withContext(Dispatchers.IO) {
        Timber.d("[PythonBridge] fhConfigure mem=$memoryName sid=$sessionId")
        try {
            val module = py().getModule("deepeye.edl_protocol")
            module.callAttr(
                "build_firehose_configure_xml", memoryName
            ).toString()
        } catch (e: Exception) {
            Timber.e("[PythonBridge] fhConfigure: ${e.message}")
            ""
        }
    }

    suspend fun generateEdlReport(
        vid: Int, pid: Int,
        chip: String, storage: String,
        sessionId: String
    ): String = withContext(Dispatchers.IO) {
        Timber.d("[PythonBridge] edlReport chip=$chip sid=$sessionId")
        try {
            val module = py().getModule("deepeye.edl_protocol")
            module.callAttr(
                "generate_edl_report",
                vid, pid, chip, storage, sessionId
            ).toString()
        } catch (e: Exception) {
            Timber.e("[PythonBridge] edlReport: ${e.message}")
            "{\"error\":\"${e.message}\"}"
        }
    }

    // ── QFIL Tools ────────────────────────────────────
    suspend fun parseRawprogramXml(
        xmlStr: String,
        sessionId: String
    ): String = withContext(Dispatchers.IO) {
        Timber.d("[PythonBridge] parseRawprogram len=${xmlStr.length} sid=$sessionId")
        try {
            val module = py().getModule("deepeye.qfil_tools")
            module.callAttr(
                "parse_rawprogram_xml", xmlStr
            ).toString().also {
                Timber.d("[PythonBridge] rawprogram entries parsed sid=$sessionId")
            }
        } catch (e: Exception) {
            Timber.e("[PythonBridge] parseRawprogram: ${e.message} sid=$sessionId")
            "[]"
        }
    }

    suspend fun getFrpPartitionInfo(
        storage: String,
        sessionId: String
    ): JSONObject = withContext(Dispatchers.IO) {
        Timber.d("[PythonBridge] frpPartition storage=$storage sid=$sessionId")
        try {
            val module = py().getModule("deepeye.qfil_tools")
            JSONObject(module.callAttr(
                "get_frp_partition_info", storage
            ).toString())
        } catch (e: Exception) {
            JSONObject().put("error", e.message)
        }
    }

    // ── Samsung Odin ──────────────────────────────────
    suspend fun analyzeSamsungHandshake(
        hexData: String,
        sessionId: String
    ): String = withContext(Dispatchers.IO) {
        try {
            val module = py().getModule("deepeye.odin_engine")
            module.callAttr("analyze_handshake", hexData).toString()
        } catch (e: Exception) {
            Timber.e("[PythonBridge] analyzeOdin: ${e.message} sid=$sessionId")
            "{\"error\":\"${e.message}\"}"
        }
    }

    suspend fun parseSamsungPit(
        hexData: String,
        sessionId: String
    ): String = withContext(Dispatchers.IO) {
        try {
            val module = py().getModule("deepeye.odin_engine")
            module.callAttr("parse_pit_to_json", hexData).toString()
        } catch (e: Exception) {
            Timber.e("[PythonBridge] parsePit: ${e.message} sid=$sessionId")
            "{\"error\":\"${e.message}\"}"
        }
    }

    // ── Forensics & Intel ──────────────────────────────
    suspend fun scanFileSetForThreats(
        jsonFileSet: String,
        sessionId: String
    ): String = withContext(Dispatchers.IO) {
        try {
            val module = py().getModule("deepeye.forensic_engine")
            module.callAttr("scan_file_set", jsonFileSet).toString()
        } catch (e: Exception) {
            Timber.e("[PythonBridge] scanThreats: ${e.message} sid=$sessionId")
            "{\"error\":\"${e.message}\"}"
        }
    }

    suspend fun getCveIntelligence(
        modelName: String,
        sessionId: String
    ): String = withContext(Dispatchers.IO) {
        try {
            val module = py().getModule("deepeye.forensic_engine")
            module.callAttr("get_cve_intel", modelName).toString()
        } catch (e: Exception) {
            Timber.e("[PythonBridge] getIntel: ${e.message} sid=$sessionId")
            "{\"error\":\"${e.message}\"}"
        }
    }

    // ── MTK BROM (Read-Only) ─────────────────────────
    suspend fun detectMtkFromUsb(
        vid: Int,
        pid: Int,
        sessionId: String
    ): JSONObject = withContext(Dispatchers.IO) {
        Timber.d("[PythonBridge] mtkUsb vid=0x${vid.toString(16)} pid=0x${pid.toString(16)} sid=$sessionId")
        try {
            val module = py().getModule("deepeye.mtk_brom")
            pyJsonObject(module.callAttr("detect_mtk_from_usb", vid, pid))
        } catch (e: Exception) {
            Timber.e("[PythonBridge] mtkUsb: ${e.message} sid=$sessionId")
            JSONObject().put("error", e.message)
        }
    }

    suspend fun identifyMtkChip(
        chipIdHex: String,
        sessionId: String
    ): JSONObject = withContext(Dispatchers.IO) {
        Timber.d("[PythonBridge] mtkChip id=$chipIdHex sid=$sessionId")
        try {
            val module = py().getModule("deepeye.mtk_brom")
            val chipId = chipIdHex.trim().removePrefix("0x").removePrefix("0X").toLong(16).toInt()
            pyJsonObject(module.callAttr("identify_chip", chipId))
        } catch (e: Exception) {
            Timber.e("[PythonBridge] mtkChip: ${e.message} sid=$sessionId")
            JSONObject().put("error", e.message).put("found", false)
        }
    }

    suspend fun parseScatterFile(
        scatterText: String,
        sessionId: String
    ): JSONObject = withContext(Dispatchers.IO) {
        Timber.d("[PythonBridge] scatter parse sid=$sessionId")
        try {
            val module = py().getModule("deepeye.mtk_brom")
            pyJsonObject(module.callAttr("parse_scatter_file", scatterText)).also {
                Timber.d("[PythonBridge] partitions=${it.optInt("partition_count")} sid=$sessionId")
            }
        } catch (e: Exception) {
            Timber.e("[PythonBridge] scatter: ${e.message} sid=$sessionId")
            JSONObject().put("error", e.message).put("valid", false)
        }
    }

    suspend fun validateSpFlashXml(
        xmlStr: String,
        sessionId: String
    ): JSONObject = withContext(Dispatchers.IO) {
        Timber.d("[PythonBridge] spFlashXml validate sid=$sessionId")
        try {
            val module = py().getModule("deepeye.mtk_brom")
            pyJsonObject(module.callAttr("validate_spflash_xml", xmlStr))
        } catch (e: Exception) {
            Timber.e("[PythonBridge] spFlashXml: ${e.message} sid=$sessionId")
            JSONObject().put("error", e.message).put("valid", false)
        }
    }

    suspend fun generateMtkReport(
        vid: Int,
        pid: Int,
        chipIdHex: String,
        scatterText: String,
        sessionId: String
    ): String = withContext(Dispatchers.IO) {
        Timber.d("[PythonBridge] mtkReport chip=$chipIdHex sid=$sessionId")
        try {
            val module = py().getModule("deepeye.mtk_brom")
            module.callAttr(
                "generate_mtk_device_report",
                vid,
                pid,
                chipIdHex,
                scatterText,
                sessionId
            ).toString()
        } catch (e: Exception) {
            Timber.e("[PythonBridge] mtkReport: ${e.message} sid=$sessionId")
            "{\"error\":\"${e.message}\"}"
        }
    }

    // ── Samsung Tools ─────────────────────────────────
    suspend fun detectSamsungFromUsb(
        vid: Int, pid: Int,
        sessionId: String
    ): JSONObject = withContext(Dispatchers.IO) {
        Timber.d("[PythonBridge] samsungUsb vid=0x${vid.toString(16)} pid=0x${pid.toString(16)} sid=$sessionId")
        try {
            val module = py().getModule("deepeye.samsung_odin")
            JSONObject(module.callAttr(
                "detect_samsung_from_usb", vid, pid
            ).toString())
        } catch (e: Exception) {
            JSONObject().put("error", e.message)
        }
    }

    suspend fun validateOdinTar(
        fileListJson: String,
        sessionId: String
    ): JSONObject = withContext(Dispatchers.IO) {
        Timber.d("[PythonBridge] odinValidate sid=$sessionId")
        try {
            val module = py().getModule("deepeye.samsung_odin")
            JSONObject(module.callAttr(
                "validate_odin_tar", fileListJson
            ).toString()).also {
                val valid = it.optBoolean("valid")
                Timber.d("[PythonBridge] odinValid=$valid sid=$sessionId")
            }
        } catch (e: Exception) {
            JSONObject().put("error", e.message).put("valid", false)
        }
    }

    suspend fun generatePitTable(
        storage: String,
        model: String,
        sessionId: String
    ): String = withContext(Dispatchers.Default) {
        Timber.d("[PythonBridge] pitTable model=$model storage=$storage sid=$sessionId")
        try {
            val normalizedStorage = storage.trim().lowercase().ifBlank { "ufs" }
            val normalizedModel = model.trim().ifBlank { "SM-S928" }
            val partitions = listOf(
                mapOf("id" to 0, "name" to "BOTA0", "file" to "bota0.img", "type" to "raw", "size_mb" to 1.0),
                mapOf("id" to 1, "name" to "BOTA1", "file" to "bota1.img", "type" to "raw", "size_mb" to 1.0),
                mapOf("id" to 2, "name" to "EFS", "file" to "efs.img", "type" to "ext4", "size_mb" to 8.0),
                mapOf("id" to 3, "name" to "PARAM", "file" to "param.img", "type" to "raw", "size_mb" to 2.0),
                mapOf("id" to 4, "name" to "BOOT", "file" to "boot.img", "type" to "ext4", "size_mb" to 64.0),
                mapOf("id" to 5, "name" to "RECOVERY", "file" to "recovery.img", "type" to "ext4", "size_mb" to 64.0),
                mapOf("id" to 6, "name" to "DTBO", "file" to "dtbo.img", "type" to "raw", "size_mb" to 8.0),
                mapOf("id" to 7, "name" to "VBMETA", "file" to "vbmeta.img", "type" to "raw", "size_mb" to 0.06),
                mapOf("id" to 8, "name" to "SUPER", "file" to "super.img", "type" to "ext4", "size_mb" to 6144.0),
                mapOf("id" to 9, "name" to "USERDATA", "file" to "userdata.img", "type" to "ext4", "size_mb" to 0.0),
                mapOf("id" to 10, "name" to "METADATA", "file" to "metadata.img", "type" to "ext4", "size_mb" to 4.0),
                mapOf("id" to 11, "name" to "PERSISTENT", "file" to "persistent.img", "type" to "ext4", "size_mb" to 32.0),
            )

            val jsonArray = JSONArray()
            partitions.forEach { partition ->
                val jsonObject = JSONObject()
                partition.forEach { (key, value) ->
                    jsonObject.put(key, value)
                }
                jsonObject.put("storage", normalizedStorage)
                jsonObject.put("model", normalizedModel)
                jsonArray.put(jsonObject)
            }
            jsonArray.toString()
        } catch (e: Exception) {
            Timber.e("[PythonBridge] pitTable: ${e.message} sid=$sessionId")
            "[]"
        }
    }
}
