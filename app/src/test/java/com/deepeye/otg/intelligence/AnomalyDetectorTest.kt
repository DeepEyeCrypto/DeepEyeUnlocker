package com.deepeye.otg.intelligence

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.content.res.AssetManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.*
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.tensorflow.lite.Interpreter
import java.io.File
import java.lang.reflect.Field

class AnomalyDetectorTest {

    @Mock lateinit var context: Context
    @Mock lateinit var assetManager: AssetManager
    @Mock lateinit var interpreter: Interpreter
    @Mock lateinit var afd: AssetFileDescriptor

    private lateinit var detector: AnomalyDetector

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        `when`(context.assets).thenReturn(assetManager)
        
        // Mock loadModelFile to avoid native crashes in unit test
        `when`(assetManager.openFd(anyString())).thenReturn(afd)
        
        // Use reflection to set the private interpreter field since init might fail without a real model
        detector = AnomalyDetector(context)
        val field: Field = AnomalyDetector::class.java.getDeclaredField("interpreter")
        field.isAccessible = true
        field.set(detector, interpreter)
    }

    @Test
    fun `test detect normal`() {
        // Mock interpreter.run(input, output)
        // input is floatArrayOf(lat, resp, ...)
        // output is Array(1) { floatArrayOf(0.9f, 0.1f) } -> Normal
        
        doAnswer { invocation ->
            val output = invocation.getArgument<Array<FloatArray>>(1)
            output[0][0] = 0.9f
            output[0][1] = 0.1f
            null
        }.`when`(interpreter).run(any(), any())

        val result = detector.detect(floatArrayOf(50f, 0f, 100f))
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull()!!.isAnomaly)
        assertEquals(0.1f, result.getOrNull()!!.confidence)
    }

    @Test
    fun `test detect anomaly`() {
        // output is Array(1) { floatArrayOf(0.1f, 0.9f) } -> Anomaly
        doAnswer { invocation ->
            val output = invocation.getArgument<Array<FloatArray>>(1)
            output[0][0] = 0.1f
            output[0][1] = 0.9f
            null
        }.`when`(interpreter).run(any(), any())

        val result = detector.detect(floatArrayOf(500f, 1f, 999f))
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.isAnomaly)
        assertEquals(0.9f, result.getOrNull()!!.confidence)
    }
}
