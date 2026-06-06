package com.deepeye.otg.service

import android.util.Log
import com.deepeye.otg.BuildConfig
import com.deepeye.otg.network.NetworkClient
import com.deepeye.otg.network.TrafficTagRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

/**
 * Handles OTA update checks via GitHub Releases API.
 */
object UpdateManager {
    private const val TAG = "DeepEye-Update"
    private const val GITHUB_API = "https://api.github.com/repos/DeepEyeCrypto/DeepEyeUnlocker/releases/latest"

    data class UpdateInfo(
        val hasUpdate: Boolean,
        val latestVersion: String,
        val downloadUrl: String? = null,
        val releaseNotes: String? = null
    )

    private val _updateState = MutableStateFlow<UpdateInfo?>(null)
    val updateState: StateFlow<UpdateInfo?> = _updateState

    suspend fun checkForUpdates(): UpdateInfo = withContext(Dispatchers.IO) {
        try {
            val client = NetworkClient.getClient(TrafficTagRegistry.TAG_UPDATE)
            val request = Request.Builder()
                .url(GITHUB_API)
                .header("Accept", "application/vnd.github.v3+json")
                .build()
                
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                if (body.isEmpty()) {
                    return@withContext UpdateInfo(false, BuildConfig.VERSION_NAME)
                }
                
                val json = JSONObject(body)
                val latestTag = json.getString("tag_name").removePrefix("v")
                val currentTag = BuildConfig.VERSION_NAME

                val hasUpdate = isNewer(latestTag, currentTag)
                val info = UpdateInfo(
                    hasUpdate = hasUpdate,
                    latestVersion = latestTag,
                    downloadUrl = json.getJSONArray("assets").optJSONObject(0)?.getString("browser_download_url"),
                    releaseNotes = json.optString("body")
                )
                
                Log.i(TAG, "[UPDATE] Latest: $latestTag | Current: $currentTag | Update available: $hasUpdate")
                _updateState.value = info
                info
            } else {
                UpdateInfo(false, BuildConfig.VERSION_NAME)
            }
        } catch (e: Exception) {
            Log.e(TAG, "[UPDATE] Failed to check for updates: ${e.message}")
            UpdateInfo(false, BuildConfig.VERSION_NAME)
        }
    }

    /**
     * Simple version comparison (e.g. 2026.18 vs 2026.17)
     */
    private fun isNewer(latest: String, current: String): Boolean {
        return try {
            val latestParts = latest.split(".").map { it.toInt() }
            val currentParts = current.split(".").map { it.toInt() }
            
            for (i in 0 until minOf(latestParts.size, currentParts.size)) {
                if (latestParts[i] > currentParts[i]) return true
                if (latestParts[i] < currentParts[i]) return false
            }
            latestParts.size > currentParts.size
        } catch (e: Exception) {
            latest != current // Fallback to string comparison
        }
    }
}
