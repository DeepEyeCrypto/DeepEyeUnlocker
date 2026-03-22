package com.deepeye.otg.intelligence

import jadx.api.JadxArgs
import jadx.api.JadxDecompiler
import timber.log.Timber
import java.io.File

/**
 * ApkAnalyzer provides automated intelligence for APK files using JADX.
 * Focus: Manifest extraction, string analysis, and sensitive key discovery.
 */
class ApkAnalyzer(private val apkFile: File) {

    private val args = JadxArgs().apply {
        inputFiles = listOf(apkFile)
        isSkipResources = false
        isSkipSources = false
        isDeobfuscationOn = false
    }

    /**
     * Extracts high-level intelligence from the APK.
     */
    fun analyze(onProgress: (String) -> Unit): Result<ApkIntelligence> {
        Timber.d("[APK_INTEL] Starting analysis for ${apkFile.name}")
        
        return try {
            JadxDecompiler(args).use { decompiler ->
                decompiler.load()
                
                onProgress("Decompiling resources...")
                val manifest = decompiler.resources.find { it.originalName == "AndroidManifest.xml" }?.content?.toString()
                
                onProgress("Scanning for sensitive keys...")
                val foundKeys = mutableMapOf<String, String>()
                
                // Simplified pattern matching for common keys
                val patterns = mapOf(
                    "Firebase API Key" to "AIza[0-9A-Za-z-_]{35}",
                    "AWS Access Key" to "AKIA[0-9A-Z]{16}",
                    "Google Maps API" to "AIza[0-9A-Za-z-_]{35}"
                )

                decompiler.classes.forEach { dexClass ->
                    val code = dexClass.code
                    patterns.forEach { (name, regex) ->
                        val match = Regex(regex).find(code)
                        if (match != null) {
                            foundKeys[name] = match.value
                            Timber.i("[APK_INTEL] Found $name in ${dexClass.fullName}")
                        }
                    }
                }

                val intel = ApkIntelligence(
                    packageName = decompiler.root.packageName ?: "unknown",
                    manifest = manifest ?: "",
                    sensitiveKeys = foundKeys
                )
                
                Timber.d("[APK_INTEL] Analysis complete for ${apkFile.name}")
                Result.success(intel)
            }
        } catch (e: Exception) {
            Timber.e(e, "[APK_INTEL] Analysis failed for ${apkFile.name}")
            Result.failure(e)
        }
    }
}

data class ApkIntelligence(
    val packageName: String,
    val manifest: String,
    val sensitiveKeys: Map<String, String>
)
