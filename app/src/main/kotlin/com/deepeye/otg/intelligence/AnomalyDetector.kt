package com.deepeye.otg.intelligence

import android.content.Context
import org.tensorflow.lite.Interpreter
import timber.log.Timber
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * AnomalyDetector uses TFLite to detect device-side traps or security anomalies
 * based on protocol response patterns (latencies, byte sequences, etc.).
 */
class AnomalyDetector(private val context: Context) {

    private var interpreter: Interpreter? = null

    init {
        try {
            val modelBuffer = loadModelFile()
            interpreter = Interpreter(modelBuffer)
            Timber.i("[ANOMALY] TFLite model loaded successfully")
        } catch (e: Exception) {
            Timber.e(e, "[ANOMALY] Failed to load TFLite model")
        }
    }

    private fun loadModelFile(): ByteBuffer {
        val fileDescriptor = context.assets.openFd("models/anomaly_model.tflite")
        val inputStream = fileDescriptor.createInputStream()
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength).apply {
            order(ByteOrder.nativeOrder())
        }
    }

    /**
     * Detects anomalies from a feature vector (e.g., [latency, response_code, byte_checksum, ...])
     */
    fun detect(features: FloatArray): Result<AnomalyEvent> {
        val interp = interpreter ?: return Result.failure(IllegalStateException("Interpreter not initialized"))
        
        val input = arrayOf(features)
        val output = Array(1) { FloatArray(2) } // [Normal Prob, Anomaly Prob]

        return try {
            interp.run(input, output)
            val anomalyProb = output[0][1]
            val isAnomaly = anomalyProb > 0.8f // Threshold

            val event = AnomalyEvent(
                isAnomaly = isAnomaly,
                confidence = anomalyProb,
                description = if (isAnomaly) "High probability of device-side trap detected" else "Normal pattern"
            )
            
            if (isAnomaly) {
                Timber.w("[ANOMALY] Detected anomaly: confidence=$anomalyProb")
            }
            
            Result.success(event)
        } catch (e: Exception) {
            Timber.e(e, "[ANOMALY] Inference failed")
            Result.failure(e)
        }
    }

    fun close() {
        interpreter?.close()
    }
}

data class AnomalyEvent(
    val isAnomaly: Boolean,
    val confidence: Float,
    val description: String
)
