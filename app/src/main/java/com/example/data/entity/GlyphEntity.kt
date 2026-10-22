package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "glyph_samples",
    indices = [
        Index(value = ["profileId", "codePoint"], unique = true)
    ]
)
data class GlyphEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long,
    val codePoint: Int,
    val charValue: String,
    val strokeDataJson: String,
    val revision: Int = 1,
    val acceptedAt: Long = System.currentTimeMillis(),
    val isSkipped: Boolean = false
)
