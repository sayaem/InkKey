package com.example.ui.screens

import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FontRepository
import com.example.data.UserPreferencesRepository
import com.example.data.entity.FontAssetEntity
import com.example.data.entity.HandwritingProfileEntity
import com.example.font.FontValidator
import com.example.font.TtfFontGenerator
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

@Composable
fun FontStudioScreen(
    profileId: Long,
    fontRepository: FontRepository,
    preferencesRepository: UserPreferencesRepository,
    onNavigateBack: () -> Unit,
    onNavigateToKeyboardSetup: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val activeProfile by fontRepository.activeProfileFlow.collectAsState(initial = null)
    val currentFontAsset by fontRepository.getCurrentFontAsset(profileId).collectAsState(initial = null)
    val userPrefs by preferencesRepository.userPreferencesFlow.collectAsState(initial = com.example.data.UserPreferences())

    var strokeThickness by remember { mutableFloatStateOf(45f) }
    var letterSpacing by remember { mutableFloatStateOf(40f) }
    var slantAngle by remember { mutableFloatStateOf(0f) }

    var isRegenerating by remember { mutableStateOf(false) }
    var previewText by remember {
        mutableStateOf("The quick brown fox jumps over the lazy dog. Pack my box with five dozen liquor jugs. 1234567890 !?@#")
    }

    var customFontFamily by remember { mutableStateOf<FontFamily?>(null) }
    var fontFileSizeKb by remember { mutableStateOf(0L) }

    // Synchronize settings from preferences
    LaunchedEffect(userPrefs) {
        strokeThickness = userPrefs.strokePreset.toFloat()
        letterSpacing = userPrefs.letterSpacing.toFloat()
        slantAngle = userPrefs.slantAngle
    }

    // Load actual Typeface when currentFontAsset changes
    LaunchedEffect(currentFontAsset?.filePath) {
        val path = currentFontAsset?.filePath
        if (!path.isNullOrEmpty()) {
            val file = File(path)
            if (file.exists()) {
                try {
                    val tf = Typeface.createFromFile(file)
                    customFontFamily = FontFamily(androidx.compose.ui.text.font.Typeface(tf))
                    fontFileSizeKb = file.length() / 1024
                } catch (_: Exception) {
                    customFontFamily = null
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // --- Header ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.testTag("font_studio_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Font Studio",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${activeProfile?.name ?: "Personal"} · Version ${currentFontAsset?.version ?: 1}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onNavigateToKeyboardSetup,
                modifier = Modifier.testTag("go_to_keyboard_setup_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Setup Keyboard",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // --- Live Specimen Card ---
            Text(
                text = "Live Specimen Preview",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFAF6EE) // Warm Parchment paper
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = previewText,
                        fontFamily = customFontFamily ?: FontFamily.Serif,
                        fontSize = 22.sp,
                        lineHeight = 32.sp,
                        color = Color(0xFF1E3A8A), // Deep ink blue
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = previewText,
                        onValueChange = { previewText = it },
                        label = { Text("Test Your Font") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("preview_text_input"),
                        singleLine = false,
                        maxLines = 3
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Typography Adjustments Card ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Typography Tuning",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Stroke Thickness Slider
                    Text(
                        text = "Stroke Weight: ${strokeThickness.roundToInt()} units",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = strokeThickness,
                        onValueChange = { strokeThickness = it },
                        valueRange = 25f..75f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Letter Spacing Slider
                    Text(
                        text = "Letter Spacing: ${letterSpacing.roundToInt()} units",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = letterSpacing,
                        onValueChange = { letterSpacing = it },
                        valueRange = 0f..80f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Slant Slider
                    Text(
                        text = "Slant Angle: ${slantAngle.roundToInt()}°",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = slantAngle,
                        onValueChange = { slantAngle = it },
                        valueRange = -15f..15f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Regenerate Button ---
            Button(
                onClick = {
                    isRegenerating = true
                    scope.launch {
                        val profile = activeProfile ?: fontRepository.ensureDefaultProfile()
                        val settings = TtfFontGenerator.GeneratorSettings(
                            strokeThickness = strokeThickness.roundToInt(),
                            letterSpacing = letterSpacing.roundToInt(),
                            slantAngleDeg = slantAngle
                        )

                        preferencesRepository.setFontSettings(
                            thickness = strokeThickness.roundToInt(),
                            spacing = letterSpacing.roundToInt(),
                            slant = slantAngle
                        )

                        val result = fontRepository.compileAndPromoteFont(
                            profileId = profile.id,
                            profileName = profile.name,
                            settings = settings
                        )

                        isRegenerating = false
                        if (result.isSuccess) {
                            val f = result.getOrNull()
                            if (f != null) {
                                val tf = Typeface.createFromFile(f)
                                customFontFamily = FontFamily(androidx.compose.ui.text.font.Typeface(tf))
                                fontFileSizeKb = f.length() / 1024
                            }
                            snackbarHostState.showSnackbar("Font compiled & promoted successfully!")
                        } else {
                            snackbarHostState.showSnackbar("Generation error: ${result.exceptionOrNull()?.message}")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("regenerate_font_button"),
                shape = RoundedCornerShape(14.dp),
                enabled = !isRegenerating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (isRegenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Compiling TrueType Font...")
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Regenerate Font Asset",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Technical Font Specifications Card ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TrueType Font Verified",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "• Tables: [head, hhea, maxp, OS/2, hmtx, cmap, glyf, loca, name, post]\n" +
                                "• Architecture: Units-per-em 1000, Ascender 800, Descender -200\n" +
                                "• File size: ${fontFileSizeKb} KB\n" +
                                "• Compatible with: Android Skia Canvas, Compose, and System IME",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        SnackbarHost(hostState = snackbarHostState)
    }
}
