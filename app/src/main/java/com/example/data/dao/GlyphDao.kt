package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.GlyphEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GlyphDao {
    @Query("SELECT * FROM glyph_samples WHERE profileId = :profileId")
    fun getGlyphsForProfile(profileId: Long): Flow<List<GlyphEntity>>

    @Query("SELECT * FROM glyph_samples WHERE profileId = :profileId")
    suspend fun getGlyphsForProfileOnce(profileId: Long): List<GlyphEntity>

    @Query("SELECT * FROM glyph_samples WHERE profileId = :profileId AND codePoint = :codePoint LIMIT 1")
    suspend fun getGlyph(profileId: Long, codePoint: Int): GlyphEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateGlyph(glyph: GlyphEntity): Long

    @Query("SELECT COUNT(*) FROM glyph_samples WHERE profileId = :profileId AND isSkipped = 0")
    fun getSavedCountForProfile(profileId: Long): Flow<Int>

    @Query("DELETE FROM glyph_samples WHERE profileId = :profileId AND codePoint = :codePoint")
    suspend fun deleteGlyph(profileId: Long, codePoint: Int)

    @Query("DELETE FROM glyph_samples WHERE profileId = :profileId")
    suspend fun deleteGlyphsForProfile(profileId: Long)

    @Query("DELETE FROM glyph_samples")
    suspend fun deleteAllGlyphs()
}
