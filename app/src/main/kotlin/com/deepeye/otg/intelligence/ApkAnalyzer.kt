package com.deepeye.otg.intelligence

import jadx.api.JadxArgs
import jadx.api.JadxDecompiler
import timber.log.Timber
import java.io.File

import jadx.api.JavaClass
import jadx.api.ResourceFile
import jadx.api.ResourceType

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
    fun analyze(onProgress: (String) -> Unit): kotlin.Result<ApkIntelligence> = kotlin.runCatching {
        Timber.d("[APK_INTEL] Starting analysis for ${apkFile.name}")
        
        val decompiler = JadxDecompiler(args)
        decompiler.load()
        
        onProgress("Extracting manifest...")
        val manifestResource = decompiler.resources.find { it.type == ResourceType.MANIFEST }
        val manifest = manifestResource?.loadContent()?.toString()
        
        onProgress("Scanning for sensitive keys...")
        val foundKeys = mutableMapOf<String, String>()
        
        // Simplified pattern matching for common keys
        val patterns = mapOf(
            "Firebase API Key" to "AIza[0-9A-Za-z-_]{35}",
            "AWS Access Key" to "AKIA[0-9A-Z]{16}",
            "Google Maps API" to "AIza[0-9A-Za-z-_]{35}"
        )

        val classes: List<JavaClass> = decompiler.classes
        classes.forEach { dexClass ->
            val code = dexClass.code
            patterns.forEach { (name, regex) ->
                val match = Regex(regex).find(code)
                if (match != null) {
                    foundKeys[name] = match.value
                    Timber.i("[APK_INTEL] Found $name in ${dexClass.fullName}")
                }
            }
        }

        // Extract package name from manifest if possible
        val packageMatch = Regex("package=\"([^\"]+)\"").find(manifest ?: "")
        val packageName = packageMatch?.groupValues?.get(1) ?: "unknown"

        val intel = ApkIntelligence(
            packageName = packageName,
            manifest = manifest ?: "",
            sensitiveKeys = foundKeys
        )
        
        Timber.d("[APK_INTEL] Analysis complete for ${apkFile.name}")
        decompiler.close()
        intel
    }
}

data class ApkIntelligence(
    val packageName: String,
    val manifest: String,
    val sensitiveKeys: Map<String, String>
)
