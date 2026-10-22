package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.example.data.PaperPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object HandwritingRenderer {

    data class RenderOptions(
        val text: String,
        val paper: PaperPreset,
        val inkColorInt: Int,
        val fontSizePx: Float = 54f,
        val lineSpacingMult: Float = 1.4f,
        val horizontalMarginPx: Float = 80f,
        val topMarginPx: Float = 100f,
        val targetWidthPx: Int = 1200
    )

    suspend fun renderToPngFile(
        context: Context,
        fontFile: File?,
        options: RenderOptions
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val typeface = if (fontFile != null && fontFile.exists()) {
                Typeface.createFromFile(fontFile)
            } else {
                Typeface.SERIF
            }

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.typeface = typeface
                this.textSize = options.fontSizePx
                this.color = options.inkColorInt
            }

            val lineSpacingPx = options.fontSizePx * options.lineSpacingMult
            val maxTextWidth = options.targetWidthPx - (options.horizontalMarginPx * 2)

            // Break input into wrapped lines
            val rawParagraphs = options.text.ifBlank { "InkKey Personal Handwriting" }.split("\n")
            val wrappedLines = mutableListOf<String>()

            for (p in rawParagraphs) {
                if (p.isBlank()) {
                    wrappedLines.add("")
                    continue
                }
                val words = p.split(" ")
                var curLine = ""
                for (w in words) {
                    val candidate = if (curLine.isEmpty()) w else "$curLine $w"
                    val measure = textPaint.measureText(candidate)
                    if (measure <= maxTextWidth) {
                        curLine = candidate
                    } else {
                        if (curLine.isNotEmpty()) wrappedLines.add(curLine)
                        curLine = w
                    }
                }
                if (curLine.isNotEmpty()) wrappedLines.add(curLine)
            }

            val totalTextHeight = wrappedLines.size * lineSpacingPx
            val totalHeightPx = (options.topMarginPx * 2 + totalTextHeight).toInt().coerceAtLeast(800)

            val bitmap = Bitmap.createBitmap(options.targetWidthPx, totalHeightPx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // 1. Draw Paper Background
            when (options.paper) {
                PaperPreset.WARM_PAPER -> {
                    canvas.drawColor(AndroidColor.parseColor("#FAF6EE"))
                }
                PaperPreset.LINED -> {
                    canvas.drawColor(AndroidColor.parseColor("#FAF6EE"))
                    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = AndroidColor.parseColor("#D4E0EE")
                        strokeWidth = 2f
                    }
                    val marginPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = AndroidColor.parseColor("#FCA5A5")
                        strokeWidth = 2.5f
                    }
                    // Vertical red notebook margin
                    canvas.drawLine(options.horizontalMarginPx - 20f, 0f, options.horizontalMarginPx - 20f, totalHeightPx.toFloat(), marginPaint)

                    // Horizontal ruled lines
                    var lineY = options.topMarginPx + 15f
                    while (lineY < totalHeightPx - 40f) {
                        canvas.drawLine(0f, lineY, options.targetWidthPx.toFloat(), lineY, linePaint)
                        lineY += lineSpacingPx
                    }
                }
                PaperPreset.BLANK -> {
                    canvas.drawColor(AndroidColor.WHITE)
                }
                PaperPreset.DARK_SLATE -> {
                    canvas.drawColor(AndroidColor.parseColor("#141923"))
                }
                PaperPreset.TRANSPARENT -> {
                    canvas.drawColor(AndroidColor.TRANSPARENT)
                }
            }

            // 2. Draw Text Lines
            var textY = options.topMarginPx + options.fontSizePx
            for (line in wrappedLines) {
                if (line.isNotEmpty()) {
                    canvas.drawText(line, options.horizontalMarginPx, textY, textPaint)
                }
                textY += lineSpacingPx
            }

            // 3. Save to app cache export directory
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val outputFile = File(exportDir, "inkkey_${System.currentTimeMillis()}.png")

            FileOutputStream(outputFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            bitmap.recycle()

            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun shareImage(context: Context, imageFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Share Handwritten Note")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
