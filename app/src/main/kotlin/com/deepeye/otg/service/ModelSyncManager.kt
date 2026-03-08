package com.deepeye.otg.service

import android.content.Context
import android.util.Log
import com.deepeye.otg.DeviceModel
import org.json.JSONArray
import java.io.File

/**
 * Stage I — Cloud Model Sync & Dynamic Assets.
 * Manages fetching, caching, and serving device model definitions.
 */
object ModelSyncManager {
    private const val TAG = "DeepEye-Sync"
    private const val FILENAME = "models_cached.json"

    private var cachedModels: List<DeviceModel> = emptyList()

    /**
     * Attempts to refresh the model database from the cloud.
     * Updates the local file if successful.
     */
    suspend fun sync(context: Context) {
        val cloudJson = CloudClient.fetchModelDatabase()
        if (cloudJson != null) {
            try {
                // Validate before saving
                val array = JSONArray(cloudJson)
                if (array.length() > 0) {
                    val file = File(context.filesDir, FILENAME)
                    file.writeText(cloudJson)
                    Log.i(TAG, "[SYNC] Successfully updated model DB: ${array.length()} devices.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "[SYNC] Validation failed: ${e.message}")
            }
        }
    }

    /**
     * Loads the model database, using the cache if available, falling back to assets.
     */
    fun load(context: Context): List<DeviceModel> {
        return try {
            val cacheFile = File(context.filesDir, FILENAME)
            val jsonString = if (cacheFile.exists()) {
                cacheFile.readText()
            } else {
                context.assets.open("models.json").bufferedReader().use { it.readText() }
            }
            
            val jsonArray = JSONArray(jsonString)
            cachedModels = (0 until jsonArray.length()).map { i ->
                val obj = jsonArray.getJSONObject(i)
                DeviceModel(
                    name = obj.getString("name"),
                    chipset = obj.getString("chipset"),
                    brand = obj.getString("brand")
                )
            }
            cachedModels
        } catch (e: Exception) {
            Log.e(TAG, "[SYNC] Load error: ${e.message}")
            emptyList()
        }
    }

    fun getAllModels() = cachedModels
}
