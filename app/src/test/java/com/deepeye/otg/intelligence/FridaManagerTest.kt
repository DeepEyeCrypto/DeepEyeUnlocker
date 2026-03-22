package com.deepeye.otg.intelligence

import android.content.Context
import android.content.res.AssetManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.io.ByteArrayInputStream

class FridaManagerTest {

    @Mock lateinit var context: Context
    @Mock lateinit var assetManager: AssetManager

    private lateinit var manager: FridaManager

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        `when`(context.assets).thenReturn(assetManager)
        manager = FridaManager(context)
    }

    @Test
    fun `test list hooks`() {
        `when`(assetManager.list("frida/hooks")).thenReturn(arrayOf("ssl.js", "root.js"))
        val hooks = manager.listAvailableHooks()
        assertEquals(2, hooks.size)
        assertTrue(hooks.contains("ssl.js"))
    }

    @Test
    fun `test deploy hooks merging`() {
        val sslContent = "console.log('ssl')"
        val rootContent = "console.log('root')"
        
        `when`(assetManager.open("frida/hooks/ssl.js")).thenReturn(ByteArrayInputStream(sslContent.toByteArray()))
        `when`(assetManager.open("frida/hooks/root.js")).thenReturn(ByteArrayInputStream(rootContent.toByteArray()))

        val result = manager.deployHooks("com.test.app", listOf("ssl.js", "root.js")) { println(it) }
        assertTrue(result.isSuccess)
    }
}
