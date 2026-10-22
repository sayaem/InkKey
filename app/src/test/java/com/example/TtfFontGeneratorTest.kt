package com.example

import com.example.font.FontValidator
import com.example.font.TtfFontGenerator
import com.example.model.Stroke
import com.example.model.StrokePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class TtfFontGeneratorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testCompileTtf_withSampleGlyphs_generatesValidTtf() {
        val generator = TtfFontGenerator()
        val customStrokes = mutableMapOf<Char, List<Stroke>>()

        // Sample letter 'A'
        customStrokes['A'] = listOf(
            Stroke(listOf(StrokePoint(0.5f, 0.2f), StrokePoint(0.25f, 0.72f))),
            Stroke(listOf(StrokePoint(0.5f, 0.2f), StrokePoint(0.75f, 0.72f))),
            Stroke(listOf(StrokePoint(0.35f, 0.5f), StrokePoint(0.65f, 0.5f)))
        )

        // Sample letter 'b'
        customStrokes['b'] = listOf(
            Stroke(listOf(StrokePoint(0.35f, 0.20f), StrokePoint(0.35f, 0.72f))),
            Stroke(listOf(StrokePoint(0.35f, 0.50f), StrokePoint(0.65f, 0.45f), StrokePoint(0.65f, 0.72f), StrokePoint(0.35f, 0.72f)))
        )

        val outputFile = File(tempFolder.root, "test_handwriting.ttf")
        val result = generator.compileTtf(
            capturedStrokes = customStrokes,
            outputFile = outputFile,
            fontFamilyName = "InkKey Test"
        )

        assertTrue("TTF compilation must succeed: ${result.exceptionOrNull()?.message}", result.isSuccess)
        assertTrue("Output file must exist", outputFile.exists())
        assertTrue("Output file must have non-trivial size (${outputFile.length()} bytes)", outputFile.length() > 500)

        val validation = FontValidator.validateTtfFile(outputFile)
        assertTrue("Validation should pass: ${validation.errorMessage}", validation.isValid)
        assertTrue("Must contain glyf table", validation.tablesFound.contains("glyf"))
        assertTrue("Must contain head table", validation.tablesFound.contains("head"))
        assertTrue("Must contain cmap table", validation.tablesFound.contains("cmap"))
        assertTrue("Must contain name table", validation.tablesFound.contains("name"))
    }
}
