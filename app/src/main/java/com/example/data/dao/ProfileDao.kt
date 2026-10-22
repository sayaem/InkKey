package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.HandwritingProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM handwriting_profiles ORDER BY updatedAt DESC")
    fun getAllProfiles(): Flow<List<HandwritingProfileEntity>>

    @Query("SELECT * FROM handwriting_profiles WHERE isActive = 1 LIMIT 1")
    fun getActiveProfile(): Flow<HandwritingProfileEntity?>

    @Query("SELECT * FROM handwriting_profiles WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveProfileOnce(): HandwritingProfileEntity?

    @Query("SELECT * FROM handwriting_profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: Long): HandwritingProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: HandwritingProfileEntity): Long

    @Update
    suspend fun updateProfile(profile: HandwritingProfileEntity)

    @Query("UPDATE handwriting_profiles SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE handwriting_profiles SET isActive = 1 WHERE id = :id")
    suspend fun setActiveProfile(id: Long)

    @Query("DELETE FROM handwriting_profiles WHERE id = :id")
    suspend fun deleteProfile(id: Long)

    @Query("DELETE FROM handwriting_profiles")
    suspend fun deleteAllProfiles()
}
