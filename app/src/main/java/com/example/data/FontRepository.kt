package com.example.data

import android.content.Context
import android.graphics.Typeface
import com.example.data.entity.FontAssetEntity
import com.example.data.entity.GlyphEntity
import com.example.data.entity.HandwritingProfileEntity
import com.example.font.FontValidator
import com.example.font.TtfFontGenerator
import com.example.model.GlyphManifest
import com.example.model.Stroke
import com.example.model.StrokeSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.File

class FontRepository(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getInstance(context)
) {
    private val profileDao = database.profileDao()
    private val glyphDao = database.glyphDao()
    private val fontAssetDao = database.fontAssetDao()
    private val fontGenerator = TtfFontGenerator()

    val activeProfileFlow: Flow<HandwritingProfileEntity?> = profileDao.getActiveProfile()

    suspend fun ensureDefaultProfile(): HandwritingProfileEntity = withContext(Dispatchers.IO) {
        val existing = profileDao.getActiveProfileOnce()
        if (existing != null) return@withContext existing

        val profile = HandwritingProfileEntity(
            name = "My Handwriting",
            profileStatus = "NOT_STARTED",
            isActive = true
        )
        val profileId = profileDao.insertProfile(profile)
        val created = profile.copy(id = profileId)

        // Generate initial baseline font asset so keyboard and preview work immediately
        compileAndPromoteFont(
            profileId = profileId,
            profileName = created.name,
            settings = TtfFontGenerator.GeneratorSettings()
        )

        created
    }

    suspend fun getProfileById(id: Long): HandwritingProfileEntity? = withContext(Dispatchers.IO) {
        profileDao.getProfileById(id)
    }

    suspend fun createProfile(name: String): HandwritingProfileEntity = withContext(Dispatchers.IO) {
        profileDao.deactivateAll()
        val newProfile = HandwritingProfileEntity(
            name = name.ifBlank { "Personal Script" },
            profileStatus = "NOT_STARTED",
            isActive = true
        )
        val id = profileDao.insertProfile(newProfile)
        val saved = newProfile.copy(id = id)
        compileAndPromoteFont(id, saved.name, TtfFontGenerator.GeneratorSettings())
        saved
    }

    suspend fun switchActiveProfile(id: Long) = withContext(Dispatchers.IO) {
        profileDao.deactivateAll()
        profileDao.setActiveProfile(id)
    }

    fun getGlyphsForProfile(profileId: Long): Flow<List<GlyphEntity>> =
        glyphDao.getGlyphsForProfile(profileId)

    suspend fun getSavedGlyphsMap(profileId: Long): Map<Char, List<Stroke>> = withContext(Dispatchers.IO) {
        val entities = glyphDao.getGlyphsForProfileOnce(profileId)
        val map = mutableMapOf<Char, List<Stroke>>()
        for (e in entities) {
            if (!e.isSkipped && e.charValue.isNotEmpty()) {
                val strokes = StrokeSerializer.deserialize(e.strokeDataJson)
                if (strokes.isNotEmpty()) {
                    map[e.charValue[0]] = strokes
                }
            }
        }
        map
    }

    suspend fun saveGlyph(
        profileId: Long,
        char: Char,
        strokes: List<Stroke>,
        isSkipped: Boolean = false
    ) = withContext(Dispatchers.IO) {
        val existing = glyphDao.getGlyph(profileId, char.code)
        val rev = (existing?.revision ?: 0) + 1
        val json = StrokeSerializer.serialize(strokes)

        val entity = GlyphEntity(
            id = existing?.id ?: 0,
            profileId = profileId,
            codePoint = char.code,
            charValue = char.toString(),
            strokeDataJson = json,
            revision = rev,
            acceptedAt = System.currentTimeMillis(),
            isSkipped = isSkipped
        )
        glyphDao.insertOrUpdateGlyph(entity)

        // Update profile status
        val savedCount = glyphDao.getGlyphsForProfileOnce(profileId).count { !it.isSkipped }
        val requiredCount = GlyphManifest.requiredItems.size
        val status = if (savedCount >= requiredCount) "READY" else "IN_PROGRESS"
        val profile = profileDao.getProfileById(profileId)
        if (profile != null) {
            profileDao.updateProfile(profile.copy(profileStatus = status, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun deleteGlyph(profileId: Long, char: Char) = withContext(Dispatchers.IO) {
        glyphDao.deleteGlyph(profileId, char.code)
    }

    fun getCurrentFontAsset(profileId: Long): Flow<FontAssetEntity?> =
        fontAssetDao.getCurrentFontForProfile(profileId)

    suspend fun getCurrentFontAssetOnce(profileId: Long): FontAssetEntity? = withContext(Dispatchers.IO) {
        fontAssetDao.getCurrentFontForProfileOnce(profileId)
    }

    /**
     * Atomically compiles and validates candidate TTF.
     * Preserves last-known-good font if compilation or validation fails.
     */
    suspend fun compileAndPromoteFont(
        profileId: Long,
        profileName: String,
        settings: TtfFontGenerator.GeneratorSettings
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val glyphsMap = getSavedGlyphsMap(profileId)
            val fontsDir = File(context.filesDir, "fonts").apply { mkdirs() }
            val tempFile = File(fontsDir, "candidate_${profileId}_${System.currentTimeMillis()}.ttf")

            val compileResult = fontGenerator.compileTtf(
                capturedStrokes = glyphsMap,
                outputFile = tempFile,
                fontFamilyName = profileName,
                settings = settings
            )

            if (compileResult.isFailure) {
                tempFile.delete()
                return@withContext Result.failure(
                    compileResult.exceptionOrNull() ?: Exception("Unknown font compilation failure")
                )
            }

            // Validate generated font structure
            val validation = FontValidator.validateTtfFile(tempFile)
            if (!validation.isValid) {
                tempFile.delete()
                return@withContext Result.failure(
                    Exception(validation.errorMessage ?: "Generated font failed validation")
                )
            }

            // Determine new version
            val currentAsset = fontAssetDao.getCurrentFontForProfileOnce(profileId)
            val nextVersion = (currentAsset?.version ?: 0) + 1
            val targetFile = File(fontsDir, "font_${profileId}_v${nextVersion}.ttf")

            // Atomic rename / promotion
            if (tempFile.renameTo(targetFile) || copyAndReplace(tempFile, targetFile)) {
                tempFile.delete()

                fontAssetDao.clearCurrentFlagForProfile(profileId)
                val newAsset = FontAssetEntity(
                    profileId = profileId,
                    version = nextVersion,
                    filePath = targetFile.absolutePath,
                    generatedAt = System.currentTimeMillis(),
                    strokeThickness = settings.strokeThickness,
                    letterSpacing = settings.letterSpacing,
                    slantAngle = settings.slantAngleDeg,
                    validationStatus = "VALID",
                    isCurrent = true
                )
                fontAssetDao.insertFontAsset(newAsset)

                val profile = profileDao.getProfileById(profileId)
                if (profile != null) {
                    profileDao.updateProfile(
                        profile.copy(
                            profileStatus = "READY",
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }

                Result.success(targetFile)
            } else {
                tempFile.delete()
                Result.failure(Exception("Could not promote font candidate to target file"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun copyAndReplace(src: File, dst: File): Boolean {
        return try {
            src.inputStream().use { input ->
                dst.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun loadCurrentTypeface(profileId: Long): Typeface? = withContext(Dispatchers.IO) {
        val asset = fontAssetDao.getCurrentFontForProfileOnce(profileId) ?: return@withContext null
        val file = File(asset.filePath)
        if (file.exists()) {
            try {
                Typeface.createFromFile(file)
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }

    suspend fun deleteProfile(profileId: Long) = withContext(Dispatchers.IO) {
        val fonts = fontAssetDao.getAllFontsForProfile(profileId).firstOrNull() ?: emptyList()
        for (f in fonts) {
            File(f.filePath).delete()
        }
        fontAssetDao.deleteFontsForProfile(profileId)
        glyphDao.deleteGlyphsForProfile(profileId)
        profileDao.deleteProfile(profileId)

        // If active profile was deleted, activate another one or create default
        val remaining = profileDao.getActiveProfileOnce()
        if (remaining == null) {
            ensureDefaultProfile()
        }
    }

    suspend fun deleteAllData() = withContext(Dispatchers.IO) {
        val fontsDir = File(context.filesDir, "fonts")
        fontsDir.deleteRecursively()
        fontAssetDao.deleteAllFonts()
        glyphDao.deleteAllGlyphs()
        profileDao.deleteAllProfiles()
        ensureDefaultProfile()
    }
}
