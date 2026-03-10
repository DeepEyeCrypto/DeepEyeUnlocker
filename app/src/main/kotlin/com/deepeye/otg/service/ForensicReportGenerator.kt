package com.deepeye.otg.service

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Stage 100.2 — Final Forensic Report Generator.
 * Generates official PDF documentation for session operations.
 */
object ForensicReportGenerator {
    private const val TAG = "DeepEye-PDF"

    fun generatePdfReport(context: Context, auditJsonPath: String): File? {
        val auditFile = File(auditJsonPath)
        if (!auditFile.exists()) return null

        val jsonContent = auditFile.readText()
        val root = try { JSONObject(jsonContent) } catch (e: Exception) { return null }

        Log.i(TAG, "Generating Fleet PDF Report: ${root.optString("report_id")}")
        
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
        var page = pdfDocument.startPage(pageInfo)
        var canvas: Canvas = page.canvas
        val paint = Paint()

        var yPos = 50f

        // 1. Header
        paint.color = Color.BLACK
        paint.textSize = 20f
        paint.isFakeBoldText = true
        canvas.drawText("DEEPEYE OTG - CONSOLIDATED FLEET AUDIT", 50f, yPos, paint)
        yPos += 30f

        paint.textSize = 10f
        paint.isFakeBoldText = false
        canvas.drawText("Generated: ${root.optString("timestamp")}", 50f, yPos, paint)
        yPos += 15f
        canvas.drawText("Fleet Report ID: ${root.optString("report_id")}", 50f, yPos, paint)
        yPos += 30f

        // 2. Multi-Device Audit Trails
        val nodes = root.optJSONArray("forensic_nodes")
        if (nodes != null) {
            for (n in 0 until nodes.length()) {
                val node = nodes.getJSONObject(n)
                val info = node.optJSONObject("device_info")
                
                // Section Header for Device
                if (yPos > 700f) {
                    pdfDocument.finishPage(page)
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    yPos = 50f
                }

                paint.textSize = 14f
                paint.isFakeBoldText = true
                paint.color = Color.BLUE
                canvas.drawText("NODE: ${node.optString("device_key")}", 50f, yPos, paint)
                yPos += 20f

                paint.textSize = 10f
                paint.isFakeBoldText = false
                paint.color = Color.BLACK
                canvas.drawText("Model: ${info?.optString("model", "Unknown")} | Chipset: ${info?.optString("chipset", "Generic")}", 60f, yPos, paint)
                yPos += 25f

                val logs = node.optJSONArray("audit_trail")
                if (logs != null) {
                    paint.textSize = 9f
                    for (l in 0 until logs.length()) {
                        if (yPos > 780f) {
                            pdfDocument.finishPage(page)
                            page = pdfDocument.startPage(pageInfo)
                            canvas = page.canvas
                            yPos = 50f
                        }
                        val log = logs.getJSONObject(l)
                        val status = log.optString("res")
                        paint.color = if (status.contains("SUCCESS") || status.contains("OK")) Color(0xFF1B5E20).toInt() else Color.RED
                        canvas.drawText("[${log.optString("ts")}] ${log.optString("op")} -> $status", 70f, yPos, paint)
                        yPos += 12f
                        paint.color = Color.GRAY
                        val hash = log.optString("sha256")
                        if (hash != "N/A") {
                            canvas.drawText("  SHA256: $hash", 70f, yPos, paint)
                            yPos += 12f
                        }
                        yPos += 5f
                    }
                }
                yPos += 15f
                paint.color = Color.LTGRAY
                canvas.drawLine(50f, yPos, 545f, yPos, paint)
                yPos += 25f
            }
        }

        // 4. Footer
        paint.color = Color.GRAY
        paint.textSize = 8f
        paint.isFakeBoldText = false
        canvas.drawText("DeepEye Forensic Engine - Secure Multi-Device Audit Documentation", 50f, 820f, paint)

        pdfDocument.finishPage(page)

        val outputFile = File(context.filesDir, "reports/ForensicAudit_${System.currentTimeMillis()}.pdf")
        outputFile.parentFile?.mkdirs()

        return try {
            pdfDocument.writeTo(FileOutputStream(outputFile))
            pdfDocument.close()
            Log.i(TAG, "Consolidated PDF saved: ${outputFile.absolutePath}")
            outputFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write PDF: ${e.message}")
            pdfDocument.close()
            null
        }
    }
}
}
