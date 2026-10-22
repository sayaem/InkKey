package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "handwriting_profiles")
data class HandwritingProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val captureVersion: Int = 1,
    val profileStatus: String = "NOT_STARTED" // NOT_STARTED, IN_PROGRESS, READY, NEEDS_REPAIR
)
