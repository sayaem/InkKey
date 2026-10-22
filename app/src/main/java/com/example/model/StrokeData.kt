package com.example.model

/**
 * Normalized stroke point in range [0.0f, 1.0f].
 * (0, 0) is top-left, (1, 1) is bottom-right of the drawing cell.
 */
data class StrokePoint(
    val x: Float,
    val y: Float,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * A continuous stroke from touch down to touch up.
 */
data class Stroke(
    val points: List<StrokePoint>
)

/**
 * Stroke collection for a single character glyph.
 */
data class GlyphStrokes(
    val character: Char,
    val strokes: List<Stroke>
)
