package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "inkkey_preferences")

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

enum class KeyboardHeightPreset(val label: String, val rowHeightDp: Int) {
    COMPACT("Compact", 44),
    NORMAL("Normal", 50),
    TALL("Tall", 56)
}

enum class PaperPreset(val title: String) {
    WARM_PAPER("Warm Paper"),
    LINED("Lined Notebook"),
    BLANK("Clean White"),
    DARK_SLATE("Dark Slate"),
    TRANSPARENT("Transparent")
}

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val hapticsEnabled: Boolean = true,
    val keyboardHeight: KeyboardHeightPreset = KeyboardHeightPreset.NORMAL,
    val keyPreviewEnabled: Boolean = true,
    val defaultPaper: PaperPreset = PaperPreset.WARM_PAPER,
    val strokePreset: Int = 45,
    val letterSpacing: Int = 40,
    val slantAngle: Float = 0f,
    val onboardingCompleted: Boolean = false
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val KEYBOARD_HEIGHT = stringPreferencesKey("keyboard_height")
        val KEY_PREVIEW_ENABLED = booleanPreferencesKey("key_preview_enabled")
        val DEFAULT_PAPER = stringPreferencesKey("default_paper")
        val STROKE_PRESET = intPreferencesKey("stroke_preset")
        val LETTER_SPACING = intPreferencesKey("letter_spacing")
        val SLANT_ANGLE = floatPreferencesKey("slant_angle")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val themeModeStr = prefs[Keys.THEME_MODE] ?: ThemeMode.LIGHT.name
        val themeMode = try {
            ThemeMode.valueOf(themeModeStr)
        } catch (_: Exception) {
            ThemeMode.LIGHT
        }

        val heightStr = prefs[Keys.KEYBOARD_HEIGHT] ?: KeyboardHeightPreset.NORMAL.name
        val height = try {
            KeyboardHeightPreset.valueOf(heightStr)
        } catch (_: Exception) {
            KeyboardHeightPreset.NORMAL
        }

        val paperStr = prefs[Keys.DEFAULT_PAPER] ?: PaperPreset.WARM_PAPER.name
        val paper = try {
            PaperPreset.valueOf(paperStr)
        } catch (_: Exception) {
            PaperPreset.WARM_PAPER
        }

        UserPreferences(
            themeMode = themeMode,
            hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
            keyboardHeight = height,
            keyPreviewEnabled = prefs[Keys.KEY_PREVIEW_ENABLED] ?: true,
            defaultPaper = paper,
            strokePreset = prefs[Keys.STROKE_PRESET] ?: 45,
            letterSpacing = prefs[Keys.LETTER_SPACING] ?: 40,
            slantAngle = prefs[Keys.SLANT_ANGLE] ?: 0f,
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTICS_ENABLED] = enabled }
    }

    suspend fun setKeyboardHeight(preset: KeyboardHeightPreset) {
        context.dataStore.edit { it[Keys.KEYBOARD_HEIGHT] = preset.name }
    }

    suspend fun setKeyPreviewEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.KEY_PREVIEW_ENABLED] = enabled }
    }

    suspend fun setDefaultPaper(paper: PaperPreset) {
        context.dataStore.edit { it[Keys.DEFAULT_PAPER] = paper.name }
    }

    suspend fun setFontSettings(thickness: Int, spacing: Int, slant: Float) {
        context.dataStore.edit {
            it[Keys.STROKE_PRESET] = thickness
            it[Keys.LETTER_SPACING] = spacing
            it[Keys.SLANT_ANGLE] = slant
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }
}
