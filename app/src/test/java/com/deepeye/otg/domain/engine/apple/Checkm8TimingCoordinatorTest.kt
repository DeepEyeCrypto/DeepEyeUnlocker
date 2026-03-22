package com.deepeye.otg.domain.engine.apple

import android.content.Context
import android.content.res.AssetManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.io.ByteArrayInputStream

class Checkm8TimingCoordinatorTest {

    @Mock lateinit var context: Context
    @Mock lateinit var assetManager: AssetManager

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        `when`(context.assets).thenReturn(assetManager)
    }

    @Test
    fun `test load profiles from JSON`() {
        val jsonString = """
            [
              {
                "chipId": 35168,
                "name": "A7",
                "heapAddress": 6442450944,
                "payloadAddress": 6442516480,
                "sprayCount": 1024,
                "holeSize": 4096
              }
            ]
        """.trimIndent()
        
        `when`(assetManager.open("apple/checkm8_profiles.json")).thenReturn(ByteArrayInputStream(jsonString.toByteArray()))

        val profiles = Checkm8Profile.loadAll(context)
        assertEquals(1, profiles.size)
        assertEquals("A7", profiles[0].name)
        assertEquals(0x8960, profiles[0].chipId)
    }

    @Test
    fun `test heap spray progress`() {
        val profile = Checkm8Profile(0x8960, "A7", 0x180000000, 0x180010000, 1000, 0x1000)
        val coordinator = Checkm8TimingCoordinator(profile)
        
        val progressUpdates = mutableListOf<Int>()
        val result = coordinator.performHeapSpray { progressUpdates.add(it) }
        
        assertTrue(result.isSuccess)
        assertTrue(progressUpdates.contains(10))
        assertTrue(progressUpdates.contains(100))
    }

    @Test
    fun `test grooming success`() {
        val profile = Checkm8Profile(0x8960, "A7", 0x180000000, 0x180010000, 1024, 0x1000)
        val coordinator = Checkm8TimingCoordinator(profile)
        
        val result = coordinator.performGrooming()
        assertTrue(result.isSuccess)
    }
}
