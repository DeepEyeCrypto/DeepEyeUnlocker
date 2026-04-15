package com.deepeye.otg.ui.screens.apple

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepeye.otg.python.PythonBridge
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

@Immutable
data class ICloudBypassUiState(
    val model: String = "",
    val chip: String = "",
    val iosVersion: String = "",
    val udid: String = "",
    val imei: String = "",
    val serial: String = "",
    val findMyEnabled: Boolean = false,
    val hasReceipt: Boolean = false,
    val wifiSsid: String = "",
    val plistInput: String = "",
    val plistOutput: String = "",
    val isLoading: Boolean = false,
    val bypassScore: BypassScore? = null,
    val bypassMethods: List<BypassMethodEntry> = emptyList(),
    val dnsConfig: DnsConfig? = null,
    val appleIdPlan: String = "",
    val error: String? = null
)

data class BypassScore(
    val scoreValue: Int,
    val difficulty: String,
    val chip: String,
    val iosMajor: Int
)

data class BypassMethodEntry(
    val methodName: String,
    val successRate: Int,
    val time: String,
    val limitation: String,
    val isFree: Boolean,
    val requirements: List<String>
)

data class DnsConfig(
    val primary: String,
    val secondary: String,
    val deepeyeDns: String,
    val instructions: List<String>
)

@HiltViewModel
class ICloudBypassViewModel @Inject constructor(
    private val pythonBridge: PythonBridge,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ICloudBypassUiState())
    val uiState: StateFlow<ICloudBypassUiState> = _uiState.asStateFlow()

    fun onModelChanged(v: String)     { _uiState.update { it.copy(model = v) } }
    fun onChipChanged(v: String)      { _uiState.update { it.copy(chip = v) } }
    fun onIosVersionChanged(v: String){ _uiState.update { it.copy(iosVersion = v) } }
    fun onFindMyChanged(v: Boolean)   { _uiState.update { it.copy(findMyEnabled = v) } }
    fun onReceiptChanged(v: Boolean)  { _uiState.update { it.copy(hasReceipt = v) } }
    fun onSsidChanged(v: String)      { _uiState.update { it.copy(wifiSsid = v) } }
    fun onPlistChanged(v: String)     { _uiState.update { it.copy(plistInput = v) } }

    fun analyzeDevice() {
        val state = _uiState.value
        viewModelScope.launch {
            val sid = UUID.randomUUID().toString()
            _uiState.update { it.copy(isLoading = true) }
            Timber.d("[ICloudVM] analyzeDevice chip=${state.chip} sid=$sid")

            val iosMajor = state.iosVersion
                .split(".").firstOrNull()?.toIntOrNull() ?: 15

            // Score + methods in parallel
            val score = pythonBridge.calculateBypassScore(
                state.chip, iosMajor,
                state.imei.isNotBlank(), state.findMyEnabled, sid
            )
            val methodsJson = pythonBridge.getBypassMethodsForDevice(
                state.chip, iosMajor,
                state.findMyEnabled, state.imei.isNotBlank(), sid
            )

            val methods = parseMethodsJson(methodsJson)
            val bypassScore = BypassScore(
                scoreValue = score.optInt("score", 5),
                difficulty = score.optString("difficulty", "Unknown"),
                chip       = state.chip,
                iosMajor   = iosMajor
            )
            _uiState.update { it.copy(
                isLoading    = false,
                bypassScore  = bypassScore,
                bypassMethods= methods
            )}
            Timber.d("[ICloudVM] analyzed score=${bypassScore.scoreValue} methods=${methods.size} sid=$sid")
        }
    }

    fun generateDnsConfig() {
        viewModelScope.launch {
            val sid = UUID.randomUUID().toString()
            val dns = pythonBridge.buildDnsBypassConfig(
                _uiState.value.wifiSsid, sid
            )
            val instructions = buildList {
                val arr = dns.optJSONArray("instructions")
                if (arr != null) {
                    for (i in 0 until arr.length()) add(arr.getString(i))
                }
            }
            _uiState.update { it.copy(dnsConfig = DnsConfig(
                primary    = dns.optString("dns_primary"),
                secondary  = dns.optString("dns_secondary"),
                deepeyeDns = dns.optString("deepeye_dns"),
                instructions = instructions
            ))}
        }
    }

    fun getAppleIdPlan() {
        val state = _uiState.value
        viewModelScope.launch {
            val sid = UUID.randomUUID().toString()
            val plan = pythonBridge.getAppleIdRemovalPlan(
                state.chip, state.hasReceipt,
                state.imei.isNotBlank(), sid
            )
            _uiState.update { it.copy(appleIdPlan = plan) }
        }
    }

    fun parsePlist() {
        viewModelScope.launch {
            val sid = UUID.randomUUID().toString()
            val result = pythonBridge.parseActivationPlist(
                _uiState.value.plistInput, sid
            )
            _uiState.update { it.copy(
                plistOutput = result.toString(2)
            )}
        }
    }

    fun generatePlist() {
        val state = _uiState.value
        viewModelScope.launch {
            val sid = UUID.randomUUID().toString()
            val plist = pythonBridge.generateActivationPlist(
                state.udid, state.imei, state.serial, sid
            )
            _uiState.update { it.copy(plistOutput = plist) }
        }
    }

    fun onMethodSelected(method: BypassMethodEntry) {
        Timber.d("[ICloudVM] method selected: ${method.methodName}")
    }

    fun copyPlistToClipboard() {
        val clipboard = context.getSystemService(
            Context.CLIPBOARD_SERVICE
        ) as ClipboardManager
        clipboard.setPrimaryClip(
            ClipData.newPlainText("PLIST", _uiState.value.plistOutput)
        )
    }

    private fun parseMethodsJson(json: String): List<BypassMethodEntry> {
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                BypassMethodEntry(
                    methodName  = obj.optString("method"),
                    successRate = obj.optInt("success_rate"),
                    time        = obj.optString("time"),
                    limitation  = obj.optString("limitation"),
                    isFree      = obj.optBoolean("free"),
                    requirements= buildList {
                        val reqs = obj.optJSONArray("requires")
                        if (reqs != null)
                            for (i in 0 until reqs.length())
                                add(reqs.getString(i))
                    }
                )
            }
        } catch (e: Exception) {
            Timber.e("[ICloudVM] parseMethodsJson: ${e.message}")
            emptyList()
        }
    }
}
