package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "font_assets")
data class FontAssetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long,
    val version: Int = 1,
    val filePath: String,
    val generatedAt: Long = System.currentTimeMillis(),
    val strokeThickness: Int = 45,
    val letterSpacing: Int = 40,
    val slantAngle: Float = 0f,
    val validationStatus: String = "VALID",
    val isCurrent: Boolean = true
)
