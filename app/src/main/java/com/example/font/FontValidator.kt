package com.example.font

import java.io.File
import java.io.FileInputStream

data class FontValidationResult(
    val isValid: Boolean,
    val numGlyphs: Int = 0,
    val tablesFound: List<String> = emptyList(),
    val errorMessage: String? = null
)

object FontValidator {

    private val REQUIRED_TABLES = setOf(
        "head", "hhea", "maxp", "OS/2", "hmtx", "cmap", "glyf", "loca", "name", "post"
    )

    fun validateTtfFile(file: File): FontValidationResult {
        if (!file.exists() || file.length() < 128) {
            return FontValidationResult(false, errorMessage = "Font file does not exist or is too small (${file.length()} bytes)")
        }

        return try {
            FileInputStream(file).use { fis ->
                val header = ByteArray(12)
                val read = fis.read(header)
                if (read < 12) {
                    return FontValidationResult(false, errorMessage = "Incomplete font header")
                }

                val sfntVersion = ((header[0].toInt() and 0xFF) shl 24) or
                        ((header[1].toInt() and 0xFF) shl 16) or
                        ((header[2].toInt() and 0xFF) shl 8) or
                        (header[3].toInt() and 0xFF)

                if (sfntVersion != 0x00010000 && sfntVersion != 0x74727565 /* 'true' */) {
                    return FontValidationResult(false, errorMessage = "Invalid SFNT version: 0x${Integer.toHexString(sfntVersion)}")
                }

                val numTables = ((header[4].toInt() and 0xFF) shl 8) or (header[5].toInt() and 0xFF)
                val tableTags = mutableListOf<String>()

                for (i in 0 until numTables) {
                    val entry = ByteArray(16)
                    val n = fis.read(entry)
                    if (n < 16) break
                    val tag = String(entry, 0, 4, Charsets.US_ASCII)
                    tableTags.add(tag)
                }

                val missingTables = REQUIRED_TABLES - tableTags.toSet()
                if (missingTables.isNotEmpty()) {
                    return FontValidationResult(
                        isValid = false,
                        tablesFound = tableTags,
                        errorMessage = "Missing required tables: $missingTables"
                    )
                }

                FontValidationResult(
                    isValid = true,
                    tablesFound = tableTags
                )
            }
        } catch (e: Exception) {
            FontValidationResult(false, errorMessage = "Font parsing exception: ${e.message}")
        }
    }
}
