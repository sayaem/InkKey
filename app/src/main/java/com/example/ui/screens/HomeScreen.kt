package com.example.ui.screens

import android.graphics.Typeface
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.FontRepository
import com.example.data.entity.GlyphEntity
import com.example.model.GlyphManifest
import java.io.File

@Composable
fun HomeScreen(
    fontRepository: FontRepository,
    onNavigateToCapture: () -> Unit,
    onNavigateToFontStudio: () -> Unit,
    onNavigateToHandwritingStudio: () -> Unit,
    onNavigateToKeyboardSetup: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val activeProfile by fontRepository.activeProfileFlow.collectAsState(initial = null)
    var savedGlyphsList by remember { mutableStateOf<List<GlyphEntity>>(emptyList()) }
    var customFontFamily by remember { mutableStateOf<FontFamily?>(null) }

    val profileId = activeProfile?.id ?: 0L
    val currentFontAsset by fontRepository.getCurrentFontAsset(profileId).collectAsState(initial = null)

    LaunchedEffect(profileId) {
        if (profileId > 0L) {
            fontRepository.getGlyphsForProfile(profileId).collect { list ->
                savedGlyphsList = list
            }
        }
    }

    LaunchedEffect(currentFontAsset?.filePath) {
        val path = currentFontAsset?.filePath
        if (!path.isNullOrEmpty()) {
            val file = File(path)
            if (file.exists()) {
                try {
                    val tf = Typeface.createFromFile(file)
                    customFontFamily = FontFamily(androidx.compose.ui.text.font.Typeface(tf))
                } catch (_: Exception) {
                    customFontFamily = null
                }
            }
        }
    }

    // Real progress calculations
    val totalRequired = GlyphManifest.requiredItems.size
    val savedRequired = savedGlyphsList.count { glyph ->
        !glyph.isSkipped && GlyphManifest.requiredItems.any { it.char.code == glyph.codePoint }
    }
    val progressPercent = if (totalRequired > 0) ((savedRequired.toFloat() / totalRequired) * 100).toInt() else 0
    val progressFraction = (savedRequired.toFloat() / totalRequired.toFloat()).coerceIn(0f, 1f)

    val profileStatus = activeProfile?.profileStatus ?: "NOT_STARTED"
    val isReady = profileStatus == "READY" || savedRequired >= totalRequired

    // Find next missing glyph
    val nextMissingGlyph = remember(savedGlyphsList) {
        GlyphManifest.requiredItems.firstOrNull { req ->
            savedGlyphsList.none { it.codePoint == req.char.code && !it.isSkipped }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // --- Top Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.ic_inkkey_icon),
                    contentDescription = "InkKey Logo",
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "InkKey",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Personal Handwriting Keyboard",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = onNavigateToSettings,
                modifier = Modifier.testTag("home_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            // --- Primary Profile Card ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_status_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = activeProfile?.name ?: "My Handwriting",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Profile Status",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Status Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        isReady -> Color(0xFFECFDF5)
                                        savedRequired > 0 -> Color(0xFFFEF3C7)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = when {
                                    isReady -> "Ready"
                                    savedRequired > 0 -> "Capture In Progress"
                                    else -> "Not Started"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = when {
                                    isReady -> Color(0xFF047857)
                                    savedRequired > 0 -> Color(0xFFB45309)
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Real font preview specimen
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFAF6EE)) // Warm Parchment
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = if (savedRequired > 0) {
                                "The quick brown fox jumps over the lazy dog."
                            } else {
                                "Start drawing your letters to generate your custom handwriting font."
                            },
                            fontFamily = customFontFamily ?: FontFamily.Serif,
                            fontSize = 18.sp,
                            lineHeight = 26.sp,
                            color = Color(0xFF1E3A8A)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Progress info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$savedRequired of $totalRequired required glyphs",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$progressPercent%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outline
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Dynamic Primary CTA
                    Button(
                        onClick = {
                            if (isReady) {
                                onNavigateToKeyboardSetup()
                            } else {
                                onNavigateToCapture()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("primary_cta_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = if (isReady) Icons.Default.Keyboard else Icons.Default.Create,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                isReady -> "Set Up System Keyboard"
                                savedRequired > 0 -> "Continue Capture (Next: '${nextMissingGlyph?.char ?: 'a'}')"
                                else -> "Start Handwriting Capture"
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- Features & Actions Grid ---
            Text(
                text = "Key Actions",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(10.dp))

            FeatureCard(
                title = "Capture Studio",
                subtitle = "Guided canvas for lowercase, uppercase, digits & punctuation",
                icon = Icons.Default.Create,
                testTag = "home_capture_card",
                onClick = onNavigateToCapture
            )

            Spacer(modifier = Modifier.height(10.dp))

            FeatureCard(
                title = "Font Studio",
                subtitle = "Tune stroke thickness, letter spacing, and regenerate TTF font",
                icon = Icons.Default.FontDownload,
                testTag = "home_font_card",
                onClick = onNavigateToFontStudio
            )

            Spacer(modifier = Modifier.height(10.dp))

            FeatureCard(
                title = "Handwriting Studio",
                subtitle = "Compose visual letters on warm paper and export as shareable PNG",
                icon = Icons.Default.Share,
                testTag = "home_export_card",
                onClick = onNavigateToHandwritingStudio
            )

            Spacer(modifier = Modifier.height(10.dp))

            FeatureCard(
                title = "Keyboard Setup & Test",
                subtitle = "Enable InkKey system input method and test typing live",
                icon = Icons.Default.Keyboard,
                testTag = "home_keyboard_setup_card",
                onClick = onNavigateToKeyboardSetup
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Offline Reassurance Badge
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "100% Offline-First · Zero Cloud Tracking · All Data On-Device",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
