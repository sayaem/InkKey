package com.example.model

import org.json.JSONArray
import org.json.JSONObject

object StrokeSerializer {

    fun serialize(strokes: List<Stroke>): String {
        val rootArray = JSONArray()
        for (stroke in strokes) {
            val strokeArray = JSONArray()
            for (p in stroke.points) {
                val ptObj = JSONObject()
                ptObj.put("x", p.x.toDouble())
                ptObj.put("y", p.y.toDouble())
                strokeArray.put(ptObj)
            }
            rootArray.put(strokeArray)
        }
        return rootArray.toString()
    }

    fun deserialize(json: String): List<Stroke> {
        val strokes = mutableListOf<Stroke>()
        if (json.isBlank()) return strokes
        try {
            val rootArray = JSONArray(json)
            for (i in 0 until rootArray.length()) {
                val strokeArray = rootArray.getJSONArray(i)
                val points = mutableListOf<StrokePoint>()
                for (j in 0 until strokeArray.length()) {
                    val ptObj = strokeArray.getJSONObject(j)
                    val x = ptObj.optDouble("x", 0.0).toFloat()
                    val y = ptObj.optDouble("y", 0.0).toFloat()
                    points.add(StrokePoint(x, y))
                }
                if (points.isNotEmpty()) {
                    strokes.add(Stroke(points))
                }
            }
        } catch (_: Exception) {
            // Return whatever parsed safely
        }
        return strokes
    }
}
