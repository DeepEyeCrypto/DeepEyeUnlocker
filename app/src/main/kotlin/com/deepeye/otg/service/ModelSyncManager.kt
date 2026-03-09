package com.deepeye.otg.service

import android.content.Context
import android.util.Log
import com.deepeye.otg.DeviceModel
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Stage I — Cloud Model Sync & Dynamic Assets.
 * Manages fetching, caching, and serving device model definitions.
 */
object ModelSyncManager {
    private const val TAG = "DeepEye-Sync"
    private const val FILENAME = "models_cached.json"
    private const val PREFS_FILE = "de_sync_prefs"
    private const val KEY_VERSION = "model_db_version"

    private var cachedModels: List<DeviceModel> = emptyList()

    /**
     * High-assurance sync: Version-aware Delta updates.
     */
    suspend fun sync(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val currentVersion = prefs.getLong(KEY_VERSION, 0L)

        Log.d(TAG, "[SYNC] Starting sync check (current_v=$currentVersion)")
        val response = CloudClient.fetchModelDatabase(currentVersion)

        when {
            response == "NOT_MODIFIED" -> {
                Log.i(TAG, "[SYNC] Cache is current (304). No update needed.")
            }
            response != null -> {
                try {
                    val root = JSONObject(response)
                    val newVersion = root.optLong("version", currentVersion + 1)
                    val data = root.getJSONArray("models")
                    
                    // Verify data integrity
                    if (data.length() > 0) {
                        val file = File(context.filesDir, FILENAME)
                        file.writeText(data.toString())
                        
                        prefs.edit().putLong(KEY_VERSION, newVersion).apply()
                        Log.i(TAG, "[SYNC] UPDATED: v$newVersion, ${data.length()} models synced.")
                        
                        // Hot-reload
                        load(context)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "[SYNC] Delta sync parsing error: ${e.message}")
                }
            }
            else -> Log.w(TAG, "[SYNC] Sync failed. Retaining current DB.")
        }
    }

    /**
     * Loads the model database, favoring cached updates over bundled assets.
     */
    fun load(context: Context): List<DeviceModel> {
        return try {
            val cacheFile = File(context.filesDir, FILENAME)
            val jsonString = if (cacheFile.exists()) {
                Log.d(TAG, "[SYNC] Loading from cache...")
                cacheFile.readText()
            } else {
                Log.d(TAG, "[SYNC] Initializing from bundled assets...")
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
            Log.i(TAG, "[SYNC] Session DB ready: ${cachedModels.size} models.")
            cachedModels
        } catch (e: Exception) {
            Log.e(TAG, "[SYNC] Load error: ${e.message}")
            emptyList()
        }
    }

    fun getAllModels() = cachedModels
}
