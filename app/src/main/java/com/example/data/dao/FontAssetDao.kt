package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.FontAssetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FontAssetDao {
    @Query("SELECT * FROM font_assets WHERE profileId = :profileId AND isCurrent = 1 LIMIT 1")
    fun getCurrentFontForProfile(profileId: Long): Flow<FontAssetEntity?>

    @Query("SELECT * FROM font_assets WHERE profileId = :profileId AND isCurrent = 1 LIMIT 1")
    suspend fun getCurrentFontForProfileOnce(profileId: Long): FontAssetEntity?

    @Query("SELECT * FROM font_assets WHERE profileId = :profileId ORDER BY version DESC")
    fun getAllFontsForProfile(profileId: Long): Flow<List<FontAssetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFontAsset(fontAsset: FontAssetEntity): Long

    @Query("UPDATE font_assets SET isCurrent = 0 WHERE profileId = :profileId")
    suspend fun clearCurrentFlagForProfile(profileId: Long)

    @Query("UPDATE font_assets SET isCurrent = 1 WHERE id = :fontAssetId")
    suspend fun setCurrentFont(fontAssetId: Long)

    @Query("DELETE FROM font_assets WHERE profileId = :profileId")
    suspend fun deleteFontsForProfile(profileId: Long)

    @Query("DELETE FROM font_assets")
    suspend fun deleteAllFonts()
}
