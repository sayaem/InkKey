package com.example.font

import com.example.model.Stroke
import com.example.model.StrokePoint
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure Kotlin TrueType Font (.ttf) Compiler.
 * Generates standards-compliant OpenType/TrueType files compatible with Android Skia engine.
 */
class TtfFontGenerator {

    data class Point(val x: Int, val y: Int)

    data class Contour(val points: List<Point>)

    data class CompiledGlyph(
        val glyphId: Int,
        val charCode: Int,
        val contours: List<Contour>,
        val xMin: Int,
        val yMin: Int,
        val xMax: Int,
        val yMax: Int,
        val advanceWidth: Int,
        val leftSideBearing: Int
    )

    data class GeneratorSettings(
        val strokeThickness: Int = 45, // in font units (approx 25..80)
        val letterSpacing: Int = 40,   // additional advance width units
        val slantAngleDeg: Float = 0f  // slant in degrees (-15 to 15)
    )

    /**
     * Converts raw captured strokes into normalized font contours.
     */
    fun convertStrokesToContours(
        strokes: List<Stroke>,
        settings: GeneratorSettings
    ): List<Contour> {
        val contours = mutableListOf<Contour>()
        val halfW = (settings.strokeThickness / 2).coerceAtLeast(10)
        val slantTan = Math.tan(Math.toRadians(settings.slantAngleDeg.toDouble())).toFloat()

        for (stroke in strokes) {
            val pts = stroke.points
            if (pts.isEmpty()) continue

            // Map normalized points [0..1] to font units
            // ascender guide: y=0.20 -> 800 font units
            // baseline guide: y=0.72 -> 0 font units
            // x range: 0.15..0.85 mapped to 100..700
            val mapped = pts.map { p ->
                val fy = ((0.72f - p.y) / (0.72f - 0.20f) * 800f).roundToInt()
                val fxRaw = (100f + (p.x - 0.15f) / 0.70f * 600f).roundToInt()
                val fx = (fxRaw + fy * slantTan).roundToInt()
                Point(fx, fy)
            }

            if (mapped.size == 1) {
                // A single dot (e.g. period or dot of i)
                val c = mapped[0]
                val r = halfW
                contours.add(
                    Contour(
                        listOf(
                            Point(c.x - r, c.y - r),
                            Point(c.x - r, c.y + r),
                            Point(c.x + r, c.y + r),
                            Point(c.x + r, c.y - r)
                        )
                    )
                )
                continue
            }

            // Multiple points: create ribbon contour
            val leftSide = mutableListOf<Point>()
            val rightSide = mutableListOf<Point>()

            for (i in 0 until mapped.size - 1) {
                val p0 = mapped[i]
                val p1 = mapped[i + 1]
                val dx = (p1.x - p0.x).toFloat()
                val dy = (p1.y - p0.y).toFloat()
                val len = sqrt(dx * dx + dy * dy)
                if (len < 1f) continue

                val nx = (-dy / len * halfW).roundToInt()
                val ny = (dx / len * halfW).roundToInt()

                if (i == 0) {
                    leftSide.add(Point(p0.x + nx, p0.y + ny))
                    rightSide.add(Point(p0.x - nx, p0.y - ny))
                }
                leftSide.add(Point(p1.x + nx, p1.y + ny))
                rightSide.add(Point(p1.x - nx, p1.y - ny))
            }

            if (leftSide.size >= 2) {
                // Combine into a closed polygon contour wound clockwise
                val fullPoints = mutableListOf<Point>()
                fullPoints.addAll(leftSide)
                fullPoints.addAll(rightSide.asReversed())
                contours.add(Contour(fullPoints))
            }
        }
        return contours
    }

    /**
     * Builds standard fallback strokes for characters that haven't been custom-drawn yet.
     */
    fun createDefaultStrokesForChar(c: Char): List<Stroke> {
        val strokes = mutableListOf<Stroke>()
        fun st(vararg pairs: Pair<Float, Float>): Stroke =
            Stroke(pairs.map { StrokePoint(it.first, it.second) })

        when (c) {
            'a' -> {
                strokes.add(st(0.65f to 0.48f, 0.45f to 0.45f, 0.35f to 0.58f, 0.42f to 0.70f, 0.65f to 0.70f, 0.65f to 0.46f))
                strokes.add(st(0.65f to 0.46f, 0.65f to 0.72f, 0.72f to 0.72f))
            }
            'b' -> {
                strokes.add(st(0.35f to 0.20f, 0.35f to 0.72f))
                strokes.add(st(0.35f to 0.50f, 0.55f to 0.45f, 0.65f to 0.58f, 0.55f to 0.72f, 0.35f to 0.72f))
            }
            'c' -> strokes.add(st(0.65f to 0.48f, 0.45f to 0.45f, 0.35f to 0.58f, 0.45f to 0.72f, 0.65f to 0.70f))
            'd' -> {
                strokes.add(st(0.65f to 0.20f, 0.65f to 0.72f))
                strokes.add(st(0.65f to 0.50f, 0.45f to 0.45f, 0.35f to 0.58f, 0.45f to 0.72f, 0.65f to 0.72f))
            }
            'e' -> strokes.add(st(0.35f to 0.58f, 0.65f to 0.58f, 0.62f to 0.46f, 0.45f to 0.45f, 0.35f to 0.58f, 0.45f to 0.72f, 0.65f to 0.70f))
            'f' -> {
                strokes.add(st(0.60f to 0.22f, 0.48f to 0.20f, 0.42f to 0.28f, 0.42f to 0.72f))
                strokes.add(st(0.32f to 0.45f, 0.58f to 0.45f))
            }
            'g' -> {
                strokes.add(st(0.65f to 0.48f, 0.45f to 0.45f, 0.35f to 0.58f, 0.45f to 0.72f, 0.65f to 0.72f, 0.65f to 0.46f))
                strokes.add(st(0.65f to 0.46f, 0.65f to 0.88f, 0.48f to 0.90f, 0.36f to 0.85f))
            }
            'h' -> {
                strokes.add(st(0.35f to 0.20f, 0.35f to 0.72f))
                strokes.add(st(0.35f to 0.52f, 0.50f to 0.45f, 0.65f to 0.52f, 0.65f to 0.72f))
            }
            'i' -> {
                strokes.add(st(0.50f to 0.45f, 0.50f to 0.72f))
                strokes.add(st(0.50f to 0.32f, 0.50f to 0.34f))
            }
            'j' -> {
                strokes.add(st(0.55f to 0.45f, 0.55f to 0.88f, 0.42f to 0.90f, 0.35f to 0.85f))
                strokes.add(st(0.55f to 0.32f, 0.55f to 0.34f))
            }
            'k' -> {
                strokes.add(st(0.35f to 0.20f, 0.35f to 0.72f))
                strokes.add(st(0.62f to 0.46f, 0.38f to 0.60f, 0.65f to 0.72f))
            }
            'l' -> strokes.add(st(0.48f to 0.20f, 0.48f to 0.70f, 0.56f to 0.72f))
            'm' -> {
                strokes.add(st(0.28f to 0.46f, 0.28f to 0.72f))
                strokes.add(st(0.28f to 0.52f, 0.42f to 0.45f, 0.50f to 0.52f, 0.50f to 0.72f))
                strokes.add(st(0.50f to 0.52f, 0.64f to 0.45f, 0.72f to 0.52f, 0.72f to 0.72f))
            }
            'n' -> {
                strokes.add(st(0.35f to 0.46f, 0.35f to 0.72f))
                strokes.add(st(0.35f to 0.52f, 0.50f to 0.45f, 0.65f to 0.52f, 0.65f to 0.72f))
            }
            'o' -> strokes.add(st(0.50f to 0.45f, 0.35f to 0.58f, 0.50f to 0.72f, 0.65f to 0.58f, 0.50f to 0.45f))
            'p' -> {
                strokes.add(st(0.35f to 0.46f, 0.35f to 0.90f))
                strokes.add(st(0.35f to 0.50f, 0.55f to 0.45f, 0.65f to 0.58f, 0.55f to 0.72f, 0.35f to 0.72f))
            }
            'q' -> {
                strokes.add(st(0.65f to 0.48f, 0.45f to 0.45f, 0.35f to 0.58f, 0.45f to 0.72f, 0.65f to 0.72f, 0.65f to 0.46f))
                strokes.add(st(0.65f to 0.46f, 0.65f to 0.90f, 0.72f to 0.88f))
            }
            'r' -> {
                strokes.add(st(0.38f to 0.46f, 0.38f to 0.72f))
                strokes.add(st(0.38f to 0.54f, 0.50f to 0.46f, 0.62f to 0.48f))
            }
            's' -> strokes.add(st(0.62f to 0.50f, 0.50f to 0.45f, 0.38f to 0.52f, 0.62f to 0.64f, 0.48f to 0.72f, 0.36f to 0.68f))
            't' -> {
                strokes.add(st(0.48f to 0.28f, 0.48f to 0.70f, 0.58f to 0.72f))
                strokes.add(st(0.36f to 0.45f, 0.60f to 0.45f))
            }
            'u' -> {
                strokes.add(st(0.35f to 0.46f, 0.35f to 0.68f, 0.48f to 0.72f, 0.62f to 0.68f, 0.62f to 0.46f))
                strokes.add(st(0.62f to 0.56f, 0.62f to 0.72f))
            }
            'v' -> strokes.add(st(0.32f to 0.46f, 0.50f to 0.72f, 0.68f to 0.46f))
            'w' -> strokes.add(st(0.24f to 0.46f, 0.38f to 0.72f, 0.50f to 0.52f, 0.62f to 0.72f, 0.76f to 0.46f))
            'x' -> {
                strokes.add(st(0.35f to 0.46f, 0.65f to 0.72f))
                strokes.add(st(0.65f to 0.46f, 0.35f to 0.72f))
            }
            'y' -> {
                strokes.add(st(0.35f to 0.46f, 0.48f to 0.66f, 0.65f to 0.46f))
                strokes.add(st(0.65f to 0.46f, 0.60f to 0.72f, 0.42f to 0.90f, 0.32f to 0.86f))
            }
            'z' -> strokes.add(st(0.35f to 0.46f, 0.65f to 0.46f, 0.35f to 0.72f, 0.65f to 0.72f))

            // Uppercase A-Z
            'A' -> {
                strokes.add(st(0.50f to 0.20f, 0.28f to 0.72f))
                strokes.add(st(0.50f to 0.20f, 0.72f to 0.72f))
                strokes.add(st(0.36f to 0.54f, 0.64f to 0.54f))
            }
            'B' -> {
                strokes.add(st(0.32f to 0.20f, 0.32f to 0.72f))
                strokes.add(st(0.32f to 0.20f, 0.58f to 0.20f, 0.66f to 0.32f, 0.58f to 0.45f, 0.32f to 0.45f))
                strokes.add(st(0.32f to 0.45f, 0.60f to 0.45f, 0.68f to 0.58f, 0.60f to 0.72f, 0.32f to 0.72f))
            }
            'C' -> strokes.add(st(0.70f to 0.30f, 0.50f to 0.20f, 0.32f to 0.45f, 0.50f to 0.72f, 0.70f to 0.62f))
            'D' -> {
                strokes.add(st(0.32f to 0.20f, 0.32f to 0.72f))
                strokes.add(st(0.32f to 0.20f, 0.54f to 0.20f, 0.70f to 0.45f, 0.54f to 0.72f, 0.32f to 0.72f))
            }
            'E' -> {
                strokes.add(st(0.32f to 0.20f, 0.32f to 0.72f))
                strokes.add(st(0.32f to 0.20f, 0.68f to 0.20f))
                strokes.add(st(0.32f to 0.45f, 0.60f to 0.45f))
                strokes.add(st(0.32f to 0.72f, 0.68f to 0.72f))
            }
            'F' -> {
                strokes.add(st(0.32f to 0.20f, 0.32f to 0.72f))
                strokes.add(st(0.32f to 0.20f, 0.68f to 0.20f))
                strokes.add(st(0.32f to 0.45f, 0.60f to 0.45f))
            }
            'G' -> {
                strokes.add(st(0.70f to 0.30f, 0.50f to 0.20f, 0.32f to 0.45f, 0.50f to 0.72f, 0.68f to 0.72f, 0.68f to 0.48f, 0.52f to 0.48f))
            }
            'H' -> {
                strokes.add(st(0.32f to 0.20f, 0.32f to 0.72f))
                strokes.add(st(0.68f to 0.20f, 0.68f to 0.72f))
                strokes.add(st(0.32f to 0.45f, 0.68f to 0.45f))
            }
            'I' -> {
                strokes.add(st(0.50f to 0.20f, 0.50f to 0.72f))
                strokes.add(st(0.38f to 0.20f, 0.62f to 0.20f))
                strokes.add(st(0.38f to 0.72f, 0.62f to 0.72f))
            }
            'J' -> strokes.add(st(0.62f to 0.20f, 0.62f to 0.64f, 0.50f to 0.74f, 0.35f to 0.68f))
            'K' -> {
                strokes.add(st(0.32f to 0.20f, 0.32f to 0.72f))
                strokes.add(st(0.68f to 0.20f, 0.34f to 0.48f, 0.68f to 0.72f))
            }
            'L' -> {
                strokes.add(st(0.34f to 0.20f, 0.34f to 0.72f, 0.68f to 0.72f))
            }
            'M' -> {
                strokes.add(st(0.25f to 0.72f, 0.25f to 0.20f, 0.50f to 0.54f, 0.75f to 0.20f, 0.75f to 0.72f))
            }
            'N' -> {
                strokes.add(st(0.30f to 0.72f, 0.30f to 0.20f, 0.70f to 0.72f, 0.70f to 0.20f))
            }
            'O' -> strokes.add(st(0.50f to 0.20f, 0.30f to 0.46f, 0.50f to 0.72f, 0.70f to 0.46f, 0.50f to 0.20f))
            'P' -> {
                strokes.add(st(0.32f to 0.20f, 0.32f to 0.72f))
                strokes.add(st(0.32f to 0.20f, 0.60f to 0.20f, 0.70f to 0.34f, 0.60f to 0.48f, 0.32f to 0.48f))
            }
            'Q' -> {
                strokes.add(st(0.50f to 0.20f, 0.30f to 0.46f, 0.50f to 0.72f, 0.70f to 0.46f, 0.50f to 0.20f))
                strokes.add(st(0.56f to 0.60f, 0.74f to 0.78f))
            }
            'R' -> {
                strokes.add(st(0.32f to 0.20f, 0.32f to 0.72f))
                strokes.add(st(0.32f to 0.20f, 0.60f to 0.20f, 0.70f to 0.34f, 0.60f to 0.48f, 0.32f to 0.48f))
                strokes.add(st(0.52f to 0.48f, 0.70f to 0.72f))
            }
            'S' -> strokes.add(st(0.68f to 0.30f, 0.50f to 0.20f, 0.34f to 0.32f, 0.66f to 0.58f, 0.50f to 0.72f, 0.32f to 0.62f))
            'T' -> {
                strokes.add(st(0.50f to 0.20f, 0.50f to 0.72f))
                strokes.add(st(0.28f to 0.20f, 0.72f to 0.20f))
            }
            'U' -> strokes.add(st(0.32f to 0.20f, 0.32f to 0.64f, 0.50f to 0.72f, 0.68f to 0.64f, 0.68f to 0.20f))
            'V' -> strokes.add(st(0.30f to 0.20f, 0.50f to 0.72f, 0.70f to 0.20f))
            'W' -> strokes.add(st(0.22f to 0.20f, 0.36f to 0.72f, 0.50f to 0.35f, 0.64f to 0.72f, 0.78f to 0.20f))
            'X' -> {
                strokes.add(st(0.30f to 0.20f, 0.70f to 0.72f))
                strokes.add(st(0.70f to 0.20f, 0.30f to 0.72f))
            }
            'Y' -> {
                strokes.add(st(0.30f to 0.20f, 0.50f to 0.46f, 0.70f to 0.20f))
                strokes.add(st(0.50f to 0.46f, 0.50f to 0.72f))
            }
            'Z' -> strokes.add(st(0.30f to 0.20f, 0.70f to 0.20f, 0.30f to 0.72f, 0.70f to 0.72f))

            // Digits 0-9
            '0' -> strokes.add(st(0.50f to 0.20f, 0.32f to 0.46f, 0.50f to 0.72f, 0.68f to 0.46f, 0.50f to 0.20f))
            '1' -> {
                strokes.add(st(0.38f to 0.32f, 0.50f to 0.20f, 0.50f to 0.72f))
                strokes.add(st(0.35f to 0.72f, 0.65f to 0.72f))
            }
            '2' -> strokes.add(st(0.34f to 0.30f, 0.50f to 0.20f, 0.66f to 0.32f, 0.34f to 0.72f, 0.68f to 0.72f))
            '3' -> {
                strokes.add(st(0.34f to 0.22f, 0.66f to 0.20f, 0.48f to 0.42f, 0.66f to 0.56f, 0.50f to 0.72f, 0.34f to 0.66f))
            }
            '4' -> {
                strokes.add(st(0.60f to 0.20f, 0.30f to 0.54f, 0.72f to 0.54f))
                strokes.add(st(0.60f to 0.38f, 0.60f to 0.72f))
            }
            '5' -> {
                strokes.add(st(0.65f to 0.20f, 0.36f to 0.20f, 0.34f to 0.42f, 0.62f to 0.44f, 0.66f to 0.60f, 0.50f to 0.72f, 0.34f to 0.66f))
            }
            '6' -> strokes.add(st(0.62f to 0.24f, 0.42f to 0.22f, 0.32f to 0.45f, 0.50f to 0.72f, 0.66f to 0.58f, 0.34f to 0.50f))
            '7' -> strokes.add(st(0.32f to 0.20f, 0.68f to 0.20f, 0.46f to 0.72f))
            '8' -> {
                strokes.add(st(0.50f to 0.20f, 0.36f to 0.32f, 0.50f to 0.44f, 0.66f to 0.58f, 0.50f to 0.72f, 0.34f to 0.58f, 0.50f to 0.44f, 0.64f to 0.32f, 0.50f to 0.20f))
            }
            '9' -> strokes.add(st(0.66f to 0.44f, 0.50f to 0.20f, 0.34f to 0.32f, 0.50f to 0.46f, 0.68f to 0.42f, 0.54f to 0.72f, 0.38f to 0.70f))

            // Punctuation
            '.' -> strokes.add(st(0.48f to 0.68f, 0.52f to 0.68f))
            ',' -> strokes.add(st(0.50f to 0.68f, 0.44f to 0.82f))
            '?' -> {
                strokes.add(st(0.36f to 0.32f, 0.50f to 0.20f, 0.66f to 0.30f, 0.54f to 0.42f, 0.50f to 0.52f))
                strokes.add(st(0.50f to 0.68f, 0.52f to 0.68f))
            }
            '!' -> {
                strokes.add(st(0.50f to 0.20f, 0.50f to 0.52f))
                strokes.add(st(0.50f to 0.68f, 0.52f to 0.68f))
            }
            ':' -> {
                strokes.add(st(0.50f to 0.46f, 0.52f to 0.46f))
                strokes.add(st(0.50f to 0.68f, 0.52f to 0.68f))
            }
            ';' -> {
                strokes.add(st(0.50f to 0.46f, 0.52f to 0.46f))
                strokes.add(st(0.50f to 0.68f, 0.44f to 0.82f))
            }
            '\'' -> strokes.add(st(0.50f to 0.20f, 0.46f to 0.32f))
            '"' -> {
                strokes.add(st(0.42f to 0.20f, 0.40f to 0.32f))
                strokes.add(st(0.58f to 0.20f, 0.56f to 0.32f))
            }
            '-' -> strokes.add(st(0.32f to 0.50f, 0.68f to 0.50f))
            '(' -> strokes.add(st(0.60f to 0.20f, 0.42f to 0.46f, 0.60f to 0.72f))
            ')' -> strokes.add(st(0.40f to 0.20f, 0.58f to 0.46f, 0.40f to 0.72f))

            // Symbols
            '&' -> strokes.add(st(0.65f to 0.68f, 0.38f to 0.32f, 0.48f to 0.20f, 0.58f to 0.32f, 0.34f to 0.62f, 0.44f to 0.72f, 0.66f to 0.58f))
            '/' -> strokes.add(st(0.32f to 0.72f, 0.68f to 0.20f))
            '@' -> {
                strokes.add(st(0.55f to 0.45f, 0.45f to 0.45f, 0.45f to 0.60f, 0.55f to 0.60f, 0.55f to 0.45f))
                strokes.add(st(0.55f to 0.60f, 0.66f to 0.55f, 0.65f to 0.35f, 0.50f to 0.28f, 0.35f to 0.45f, 0.35f to 0.62f, 0.50f to 0.72f, 0.68f to 0.68f))
            }
            '+' -> {
                strokes.add(st(0.50f to 0.32f, 0.50f to 0.68f))
                strokes.add(st(0.32f to 0.50f, 0.68f to 0.50f))
            }
            '=' -> {
                strokes.add(st(0.32f to 0.42f, 0.68f to 0.42f))
                strokes.add(st(0.32f to 0.58f, 0.68f to 0.58f))
            }
            '%' -> {
                strokes.add(st(0.32f to 0.70f, 0.68f to 0.22f))
                strokes.add(st(0.40f to 0.30f, 0.42f to 0.30f))
                strokes.add(st(0.60f to 0.62f, 0.62f to 0.62f))
            }
            else -> {
                // simple vertical stroke fallback
                strokes.add(st(0.50f to 0.25f, 0.50f to 0.70f))
            }
        }
        return strokes
    }

    /**
     * Compiles a TTF file from user-captured strokes (with automatic fallbacks for untouched glyphs).
     */
    fun compileTtf(
        capturedStrokes: Map<Char, List<Stroke>>,
        outputFile: File,
        fontFamilyName: String = "InkKey Personal",
        settings: GeneratorSettings = GeneratorSettings()
    ): Result<File> {
        return try {
            val supportedChars = mutableListOf<Char>()
            // Always space
            supportedChars.add(' ')
            // All printable ASCII 33..126
            for (code in 33..126) {
                supportedChars.add(code.toChar())
            }

            // Build compiled glyphs
            val compiledGlyphs = mutableListOf<CompiledGlyph>()

            // Glyph 0: .notdef (simple box with a hole)
            val notDefContours = listOf(
                Contour(listOf(Point(100, 0), Point(100, 700), Point(500, 700), Point(500, 0))),
                Contour(listOf(Point(160, 60), Point(440, 60), Point(440, 640), Point(160, 640)))
            )
            compiledGlyphs.add(
                CompiledGlyph(
                    glyphId = 0,
                    charCode = 0,
                    contours = notDefContours,
                    xMin = 100,
                    yMin = 0,
                    xMax = 500,
                    yMax = 700,
                    advanceWidth = 600,
                    leftSideBearing = 100
                )
            )

            // Build remaining glyphs
            var nextGlyphId = 1
            for (char in supportedChars) {
                if (char == ' ') {
                    compiledGlyphs.add(
                        CompiledGlyph(
                            glyphId = nextGlyphId++,
                            charCode = 32,
                            contours = emptyList(),
                            xMin = 0,
                            yMin = 0,
                            xMax = 0,
                            yMax = 0,
                            advanceWidth = (350 + settings.letterSpacing).coerceAtLeast(200),
                            leftSideBearing = 0
                        )
                    )
                    continue
                }

                val strokes = capturedStrokes[char] ?: createDefaultStrokesForChar(char)
                val contours = convertStrokesToContours(strokes, settings)

                if (contours.isEmpty()) {
                    compiledGlyphs.add(
                        CompiledGlyph(
                            glyphId = nextGlyphId++,
                            charCode = char.code,
                            contours = emptyList(),
                            xMin = 0,
                            yMin = 0,
                            xMax = 0,
                            yMax = 0,
                            advanceWidth = (450 + settings.letterSpacing).coerceAtLeast(200),
                            leftSideBearing = 0
                        )
                    )
                } else {
                    var xMin = Int.MAX_VALUE
                    var yMin = Int.MAX_VALUE
                    var xMax = Int.MIN_VALUE
                    var yMax = Int.MIN_VALUE

                    for (c in contours) {
                        for (p in c.points) {
                            if (p.x < xMin) xMin = p.x
                            if (p.y < yMin) yMin = p.y
                            if (p.x > xMax) xMax = p.x
                            if (p.y > yMax) yMax = p.y
                        }
                    }

                    val lsb = xMin.coerceAtLeast(0)
                    val advance = max(xMax + 80 + settings.letterSpacing, 400)

                    compiledGlyphs.add(
                        CompiledGlyph(
                            glyphId = nextGlyphId++,
                            charCode = char.code,
                            contours = contours,
                            xMin = xMin,
                            yMin = yMin,
                            xMax = xMax,
                            yMax = yMax,
                            advanceWidth = advance,
                            leftSideBearing = lsb
                        )
                    )
                }
            }

            // Build binary TTF tables
            val ttfBytes = assembleTtf(compiledGlyphs, fontFamilyName)

            // Ensure parent directory exists
            outputFile.parentFile?.mkdirs()
            FileOutputStream(outputFile).use { it.write(ttfBytes) }

            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun assembleTtf(glyphs: List<CompiledGlyph>, familyName: String): ByteArray {
        val numGlyphs = glyphs.size

        // Build individual tables
        val glyfAndLoca = buildGlyfAndLoca(glyphs)
        val glyfTable = glyfAndLoca.first
        val locaTable = glyfAndLoca.second
        val cmapTable = buildCmap(glyphs)
        val hmtxTable = buildHmtx(glyphs)
        val maxpTable = buildMaxp(glyphs)
        val nameTable = buildName(familyName)
        val os2Table = buildOs2(glyphs)
        val postTable = buildPost()
        val hheaTable = buildHhea(glyphs)

        // Head table needs font bounds
        var fontXMin = 0
        var fontYMin = -200
        var fontXMax = 800
        var fontYMax = 800
        for (g in glyphs) {
            if (g.contours.isNotEmpty()) {
                fontXMin = min(fontXMin, g.xMin)
                fontYMin = min(fontYMin, g.yMin)
                fontXMax = max(fontXMax, g.xMax)
                fontYMax = max(fontYMax, g.yMax)
            }
        }
        val headTable = buildHead(fontXMin, fontYMin, fontXMax, fontYMax)

        // Tables sorted alphabetically by tag:
        // 1. "OS/2"
        // 2. "cmap"
        // 3. "glyf"
        // 4. "head"
        // 5. "hhea"
        // 6. "hmtx"
        // 7. "loca"
        // 8. "maxp"
        // 9. "name"
        // 10. "post"
        val tables = listOf(
            "OS/2" to os2Table,
            "cmap" to cmapTable,
            "glyf" to glyfTable,
            "head" to headTable,
            "hhea" to hheaTable,
            "hmtx" to hmtxTable,
            "loca" to locaTable,
            "maxp" to maxpTable,
            "name" to nameTable,
            "post" to postTable
        )

        val numTables = tables.size
        var k = 0
        while ((1 shl (k + 1)) <= numTables) k++
        val searchRange = (1 shl k) * 16
        val entrySelector = k
        val rangeShift = numTables * 16 - searchRange

        val bos = ByteArrayOutputStream()
        val dos = DataOutputStream(bos)

        // Offset table header
        dos.writeInt(0x00010000) // sfntVersion
        dos.writeShort(numTables)
        dos.writeShort(searchRange)
        dos.writeShort(entrySelector)
        dos.writeShort(rangeShift)

        // Table directory starts at byte offset 12 + numTables * 16
        var currentOffset = 12 + numTables * 16

        val paddedTables = mutableListOf<ByteArray>()
        val tableOffsets = mutableListOf<Int>()

        for ((_, data) in tables) {
            tableOffsets.add(currentOffset)
            val padding = (4 - (data.size % 4)) % 4
            val paddedData = if (padding > 0) {
                val p = ByteArray(data.size + padding)
                System.arraycopy(data, 0, p, 0, data.size)
                p
            } else {
                data
            }
            paddedTables.add(paddedData)
            currentOffset += paddedData.size
        }

        // Write Table Directory
        var headDirectoryEntryOffset = -1
        for (i in tables.indices) {
            val (tag, rawData) = tables[i]
            val tagBytes = tag.toByteArray(Charsets.US_ASCII)
            dos.write(tagBytes)
            dos.writeInt(calculateChecksum(rawData).toInt())
            dos.writeInt(tableOffsets[i])
            dos.writeInt(rawData.size)

            if (tag == "head") {
                headDirectoryEntryOffset = 12 + i * 16
            }
        }

        // Write table bodies
        for (paddedData in paddedTables) {
            dos.write(paddedData)
        }
        dos.flush()

        val fullFont = bos.toByteArray()

        // Calculate whole font checksum and patch head.checkSumAdjustment at (headOffset + 8)
        val headOffset = tableOffsets[tables.indexOfFirst { it.first == "head" }]
        val wholeFontChecksum = calculateChecksum(fullFont)
        val adjustment = (0xB1B0AFBAL - wholeFontChecksum) and 0xFFFFFFFFL

        fullFont[headOffset + 8] = ((adjustment ushr 24) and 0xFF).toByte()
        fullFont[headOffset + 9] = ((adjustment ushr 16) and 0xFF).toByte()
        fullFont[headOffset + 10] = ((adjustment ushr 8) and 0xFF).toByte()
        fullFont[headOffset + 11] = (adjustment and 0xFF).toByte()

        return fullFont
    }

    private fun calculateChecksum(data: ByteArray): Long {
        var sum = 0L
        val len = data.size
        var i = 0
        while (i < len) {
            val b0 = (data[i].toInt() and 0xFF).toLong()
            val b1 = if (i + 1 < len) (data[i + 1].toInt() and 0xFF).toLong() else 0L
            val b2 = if (i + 2 < len) (data[i + 2].toInt() and 0xFF).toLong() else 0L
            val b3 = if (i + 3 < len) (data[i + 3].toInt() and 0xFF).toLong() else 0L
            val v = (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3
            sum = (sum + v) and 0xFFFFFFFFL
            i += 4
        }
        return sum
    }

    private fun buildGlyfAndLoca(glyphs: List<CompiledGlyph>): Pair<ByteArray, ByteArray> {
        val glyfBos = ByteArrayOutputStream()
        val locaOffsets = mutableListOf<Int>()

        for (g in glyphs) {
            locaOffsets.add(glyfBos.size())

            if (g.contours.isEmpty()) {
                // Empty glyph (e.g. space), no data in glyf
                continue
            }

            val gBos = ByteArrayOutputStream()
            val gDos = DataOutputStream(gBos)

            val numContours = g.contours.size
            gDos.writeShort(numContours)
            gDos.writeShort(g.xMin)
            gDos.writeShort(g.yMin)
            gDos.writeShort(g.xMax)
            gDos.writeShort(g.yMax)

            // endPtsOfContours
            var ptIndex = 0
            for (c in g.contours) {
                ptIndex += c.points.size
                gDos.writeShort(ptIndex - 1)
            }

            // instructionLength
            gDos.writeShort(0)

            // Flatten all points
            val allPoints = g.contours.flatMap { it.points }
            val numPoints = allPoints.size

            // Flags: 0x01 = ON_CURVE, with 16-bit signed x and y deltas
            for (i in 0 until numPoints) {
                gDos.writeByte(0x01)
            }

            // X deltas (signed 16-bit)
            var prevX = 0
            for (p in allPoints) {
                val dx = p.x - prevX
                gDos.writeShort(dx)
                prevX = p.x
            }

            // Y deltas (signed 16-bit)
            var prevY = 0
            for (p in allPoints) {
                val dy = p.y - prevY
                gDos.writeShort(dy)
                prevY = p.y
            }

            gDos.flush()
            val glyphBytes = gBos.toByteArray()
            glyfBos.write(glyphBytes)

            // 2-byte alignment inside glyf table
            if (glyphBytes.size % 2 != 0) {
                glyfBos.write(0)
            }
        }

        // Final offset in loca
        locaOffsets.add(glyfBos.size())

        // Build 32-bit loca table
        val locaBos = ByteArrayOutputStream()
        val locaDos = DataOutputStream(locaBos)
        for (offset in locaOffsets) {
            locaDos.writeInt(offset)
        }
        locaDos.flush()

        return Pair(glyfBos.toByteArray(), locaBos.toByteArray())
    }

    private fun buildCmap(glyphs: List<CompiledGlyph>): ByteArray {
        val bos = ByteArrayOutputStream()
        val dos = DataOutputStream(bos)

        // Cmap Header
        dos.writeShort(0) // version
        dos.writeShort(1) // numTables = 1

        // Encoding record: Windows Unicode BMP (Platform 3, Encoding 1)
        dos.writeShort(3)  // platformID
        dos.writeShort(1)  // encodingID
        dos.writeInt(12)   // offset to subtable from beginning of cmap

        // Build sorted list of (charCode, glyphId)
        val charGlyphs = glyphs.filter { it.charCode > 0 }.sortedBy { it.charCode }

        // Segments: group contiguous character ranges
        val segments = mutableListOf<Segment>()
        if (charGlyphs.isNotEmpty()) {
            var curStart = charGlyphs[0].charCode
            var curEnd = curStart
            var curStartGlyph = charGlyphs[0].glyphId

            for (i in 1 until charGlyphs.size) {
                val cg = charGlyphs[i]
                if (cg.charCode == curEnd + 1 && cg.glyphId == curStartGlyph + (cg.charCode - curStart)) {
                    curEnd = cg.charCode
                } else {
                    segments.add(Segment(curStart, curEnd, (curStartGlyph - curStart) and 0xFFFF))
                    curStart = cg.charCode
                    curEnd = curStart
                    curStartGlyph = cg.glyphId
                }
            }
            segments.add(Segment(curStart, curEnd, (curStartGlyph - curStart) and 0xFFFF))
        }

        // Final mandatory segment [0xFFFF, 0xFFFF] mapping to 0 (.notdef)
        segments.add(Segment(0xFFFF, 0xFFFF, 1))

        val segCount = segments.size
        val segCountX2 = segCount * 2
        var k = 0
        while ((1 shl (k + 1)) <= segCount) k++
        val searchRange = 2 * (1 shl k)
        val entrySelector = k
        val rangeShift = segCountX2 - searchRange

        val subtableLength = 16 + segCount * 8

        // Subtable format 4
        dos.writeShort(4) // format
        dos.writeShort(subtableLength)
        dos.writeShort(0) // language
        dos.writeShort(segCountX2)
        dos.writeShort(searchRange)
        dos.writeShort(entrySelector)
        dos.writeShort(rangeShift)

        // endCode[segCount]
        for (s in segments) dos.writeShort(s.endCode)
        dos.writeShort(0) // reservedPad
        // startCode[segCount]
        for (s in segments) dos.writeShort(s.startCode)
        // idDelta[segCount]
        for (s in segments) dos.writeShort(s.idDelta)
        // idRangeOffset[segCount] (all zeros since idDelta handles direct mapping)
        for (s in segments) dos.writeShort(0)

        dos.flush()
        return bos.toByteArray()
    }

    private data class Segment(val startCode: Int, val endCode: Int, val idDelta: Int)

    private fun buildHmtx(glyphs: List<CompiledGlyph>): ByteArray {
        val bos = ByteArrayOutputStream()
        val dos = DataOutputStream(bos)
        for (g in glyphs) {
            dos.writeShort(g.advanceWidth)
            dos.writeShort(g.leftSideBearing)
        }
        dos.flush()
        return bos.toByteArray()
    }

    private fun buildHhea(glyphs: List<CompiledGlyph>): ByteArray {
        val bos = ByteArrayOutputStream()
        val dos = DataOutputStream(bos)

        val maxAdvance = glyphs.maxOfOrNull { it.advanceWidth } ?: 600

        dos.writeInt(0x00010000) // version 1.0
        dos.writeShort(800)      // ascender
        dos.writeShort(-200)     // descender
        dos.writeShort(200)      // lineGap
        dos.writeShort(maxAdvance) // advanceWidthMax
        dos.writeShort(0)        // minLeftSideBearing
        dos.writeShort(0)        // minRightSideBearing
        dos.writeShort(maxAdvance) // xMaxExtent
        dos.writeShort(1)        // caretSlopeRise
        dos.writeShort(0)        // caretSlopeRun
        dos.writeShort(0)        // caretOffset
        dos.writeShort(0)        // reserved1
        dos.writeShort(0)        // reserved2
        dos.writeShort(0)        // reserved3
        dos.writeShort(0)        // reserved4
        dos.writeShort(0)        // metricDataFormat
        dos.writeShort(glyphs.size) // numberOfHMetrics

        dos.flush()
        return bos.toByteArray()
    }

    private fun buildHead(xMin: Int, yMin: Int, xMax: Int, yMax: Int): ByteArray {
        val bos = ByteArrayOutputStream()
        val dos = DataOutputStream(bos)

        dos.writeInt(0x00010000) // major 1, minor 0
        dos.writeInt(0x00010000) // fontRevision
        dos.writeInt(0)          // checkSumAdjustment (patched later)
        dos.writeInt(0x5F0F3CF5) // magicNumber
        dos.writeShort(0x000B)   // flags (baseline y=0, lsb x=0, etc.)
        dos.writeShort(1000)     // unitsPerEm
        dos.writeLong(0L)        // created
        dos.writeLong(0L)        // modified
        dos.writeShort(xMin)
        dos.writeShort(yMin)
        dos.writeShort(xMax)
        dos.writeShort(yMax)
        dos.writeShort(0)        // macStyle
        dos.writeShort(8)        // lowestRecPPEM
        dos.writeShort(2)        // fontDirectionHint
        dos.writeShort(1)        // indexToLocFormat: 1 = 32-bit offsets in loca
        dos.writeShort(0)        // glyphDataFormat

        dos.flush()
        return bos.toByteArray()
    }

    private fun buildMaxp(glyphs: List<CompiledGlyph>): ByteArray {
        val bos = ByteArrayOutputStream()
        val dos = DataOutputStream(bos)

        var maxPoints = 4
        var maxContours = 2
        for (g in glyphs) {
            val totalPts = g.contours.sumOf { it.points.size }
            if (totalPts > maxPoints) maxPoints = totalPts
            if (g.contours.size > maxContours) maxContours = g.contours.size
        }

        dos.writeInt(0x00010000) // version 1.0
        dos.writeShort(glyphs.size) // numGlyphs
        dos.writeShort(maxPoints)
        dos.writeShort(maxContours)
        dos.writeShort(0) // maxCompositePoints
        dos.writeShort(0) // maxCompositeContours
        dos.writeShort(1) // maxZones
        dos.writeShort(0) // maxTwilightPoints
        dos.writeShort(0) // maxStorage
        dos.writeShort(0) // maxFunctionDefs
        dos.writeShort(0) // maxInstructionDefs
        dos.writeShort(0) // maxStackElements
        dos.writeShort(0) // maxSizeOfInstructions
        dos.writeShort(0) // maxComponentElements
        dos.writeShort(0) // maxComponentDepth

        dos.flush()
        return bos.toByteArray()
    }

    private fun buildName(familyName: String): ByteArray {
        val bos = ByteArrayOutputStream()
        val dos = DataOutputStream(bos)

        // Name records:
        // 1: Family
        // 2: Subfamily
        // 3: Unique ID
        // 4: Full Name
        // 5: Version
        // 6: PostScript
        val records = listOf(
            1 to familyName,
            2 to "Regular",
            3 to "InkKey:$familyName:1.0",
            4 to "$familyName Regular",
            5 to "Version 1.000",
            6 to familyName.replace("\\s+".toRegex(), "-")
        )

        val stringBytesList = records.map { (_, text) ->
            text.toByteArray(Charsets.UTF_16BE)
        }

        val numRecords = records.size
        val stringOffset = 6 + numRecords * 12

        dos.writeShort(0) // format = 0
        dos.writeShort(numRecords)
        dos.writeShort(stringOffset)

        var curOffset = 0
        for (i in records.indices) {
            val nameId = records[i].first
            val bytes = stringBytesList[i]
            dos.writeShort(3)      // platformID = 3 (Windows)
            dos.writeShort(1)      // encodingID = 1 (Unicode BMP)
            dos.writeShort(0x0409) // languageID = English US
            dos.writeShort(nameId)
            dos.writeShort(bytes.size)
            dos.writeShort(curOffset)
            curOffset += bytes.size
        }

        for (bytes in stringBytesList) {
            dos.write(bytes)
        }

        dos.flush()
        return bos.toByteArray()
    }

    private fun buildOs2(glyphs: List<CompiledGlyph>): ByteArray {
        val bos = ByteArrayOutputStream()
        val dos = DataOutputStream(bos)

        val maxAdvance = glyphs.maxOfOrNull { it.advanceWidth } ?: 600

        dos.writeShort(3)     // version = 3
        dos.writeShort(500)   // xAvgCharWidth
        dos.writeShort(400)   // usWeightClass (Normal)
        dos.writeShort(5)     // usWidthClass (Medium)
        dos.writeShort(0)     // fsType (0 = installable embedding)
        dos.writeShort(650)   // ySubscriptXSize
        dos.writeShort(600)   // ySubscriptYSize
        dos.writeShort(0)     // ySubscriptXOffset
        dos.writeShort(75)    // ySubscriptYOffset
        dos.writeShort(650)   // ySuperscriptXSize
        dos.writeShort(600)   // ySuperscriptYSize
        dos.writeShort(0)     // ySuperscriptXOffset
        dos.writeShort(350)   // ySuperscriptYOffset
        dos.writeShort(50)    // yStrikeoutSize
        dos.writeShort(300)   // yStrikeoutPosition
        dos.writeShort(0)     // sFamilyClass
        // Panose (10 bytes)
        dos.write(byteArrayOf(2, 0, 5, 3, 0, 0, 0, 0, 0, 0))
        // Unicode ranges
        dos.writeInt(0x00000001) // Basic Latin (bit 0)
        dos.writeInt(0)
        dos.writeInt(0)
        dos.writeInt(0)
        // achVendID: "INKK"
        dos.write("INKK".toByteArray(Charsets.US_ASCII))
        dos.writeShort(0x0040) // fsSelection: Regular
        dos.writeShort(32)     // usFirstCharIndex
        dos.writeShort(126)    // usLastCharIndex
        dos.writeShort(800)    // sTypoAscender
        dos.writeShort(-200)   // sTypoDescender
        dos.writeShort(200)    // sTypoLineGap
        dos.writeShort(1000)   // usWinAscent
        dos.writeShort(200)    // usWinDescent
        dos.writeInt(0x00000001) // ulCodePageRange1: 1252 Latin 1
        dos.writeInt(0)        // ulCodePageRange2
        dos.writeShort(500)    // sxHeight
        dos.writeShort(700)    // sCapHeight
        dos.writeShort(0)      // usDefaultChar
        dos.writeShort(32)     // usBreakChar
        dos.writeShort(1)      // usMaxContext

        dos.flush()
        return bos.toByteArray()
    }

    private fun buildPost(): ByteArray {
        val bos = ByteArrayOutputStream()
        val dos = DataOutputStream(bos)

        dos.writeInt(0x00030000) // format 3.0 (no glyph names)
        dos.writeInt(0)          // italicAngle = 0
        dos.writeShort(-100)     // underlinePosition
        dos.writeShort(50)       // underlineThickness
        dos.writeInt(0)          // isFixedPitch = false
        dos.writeInt(0)          // minMemType42
        dos.writeInt(0)          // maxMemType42
        dos.writeInt(0)          // minMemType1
        dos.writeInt(0)          // maxMemType1

        dos.flush()
        return bos.toByteArray()
    }
}
