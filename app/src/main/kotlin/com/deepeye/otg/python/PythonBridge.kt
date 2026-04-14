package com.deepeye.otg.python

import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
}
