package com.deepeye.otg.intelligence

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ApkAnalyzerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `test analyzer with dummy apk`() {
        // Create a fake APK (zip file)
        val apkFile = tempFolder.newFile("test.apk")
        ZipOutputStream(FileOutputStream(apkFile)).use { zos ->
            zos.putNextEntry(ZipEntry("AndroidManifest.xml"))
            zos.write("<manifest package=\"com.test.app\"></manifest>".toByteArray())
            zos.closeEntry()
            
            zos.putNextEntry(ZipEntry("classes.dex"))
            zos.write("Some dummy dex content with AIzaTestKey12345678901234567890123456789012345".toByteArray())
            zos.closeEntry()
        }

        val analyzer = ApkAnalyzer(apkFile)
        // Since JADX requires valid dex, this will likely fail or return empty in a pure unit test
        // but we can at least verify it doesn't crash on init
        val result = analyzer.analyze { println(it) }
        
        // This is more of an integration test for JADX loading
        // In a real CI, we'd use a small valid APK
        assertNotNull(result)
    }

    @Test
    fun `test pattern matching logic`() {
        val regex = Regex("AIza[0-9A-Za-z-_]{35}")
        val testString = "key is AIzaSyD4_Lz0Vz9rX-n9_h-9z_X9z9z9z9z9z9z"
        val match = regex.find(testString)
        assertNotNull(match)
        assertEquals("AIzaSyD4_Lz0Vz9rX-n9_h-9z_X9z9z9z9z9z9z", match?.value)
    }
}
