package com.example.ui.screens

import android.graphics.Typeface
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FontRepository
import com.example.data.PaperPreset
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

@Composable
fun HandwritingStudioScreen(
    profileId: Long,
    fontRepository: FontRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current

    val currentFontAsset by fontRepository.getCurrentFontAsset(profileId).collectAsState(initial = null)
    var customFontFamily by remember { mutableStateOf<FontFamily?>(null) }
    var fontFileRef by remember { mutableStateOf<File?>(null) }

    var inputText by remember {
        mutableStateOf("Dearest friend,\nI am writing to you in my own personal handwriting from InkKey. It feels so wonderful to keep the character of real writing on screen!")
    }

    var selectedPaper by remember { mutableStateOf(PaperPreset.WARM_PAPER) }
    var selectedInkInt by remember { mutableIntStateOf(0xFF1E3A8A.toInt()) } // Midnight Fountain Blue
    var fontSizeSp by remember { mutableFloatStateOf(20f) }
    var isExporting by remember { mutableStateOf(false) }

    LaunchedEffect(currentFontAsset?.filePath) {
        val path = currentFontAsset?.filePath
        if (!path.isNullOrEmpty()) {
            val f = File(path)
            if (f.exists()) {
                fontFileRef = f
                try {
                    val tf = Typeface.createFromFile(f)
                    customFontFamily = FontFamily(androidx.compose.ui.text.font.Typeface(tf))
                } catch (_: Exception) {
                    customFontFamily = null
                }
            }
        }
    }

    val inkChoices = remember {
        listOf(
            Triple("Midnight Blue", Color(0xFF1E3A8A), 0xFF1E3A8A.toInt()),
            Triple("Classic Black", Color(0xFF1E293B), 0xFF1E293B.toInt()),
            Triple("Sepia Ink", Color(0xFF5C3D2E), 0xFF5C3D2E.toInt()),
            Triple("Emerald", Color(0xFF065F46), 0xFF065F46.toInt()),
            Triple("Crimson", Color(0xFF991B1B), 0xFF991B1B.toInt())
        )
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
                modifier = Modifier.testTag("export_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Text(
                text = "Handwriting Studio",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = MaterialTheme.colorScheme.onBackground
            )

            IconButton(
                onClick = {
                    val text = clipboardManager.getText()?.text
                    if (!text.isNullOrBlank()) {
                        inputText = text
                        scope.launch { snackbarHostState.showSnackbar("Pasted from clipboard") }
                    }
                },
                modifier = Modifier.testTag("paste_text_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ContentPaste,
                    contentDescription = "Paste",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // --- Live Paper Canvas Preview ---
            Text(
                text = "Live Handwriting Note",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("handwriting_note_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (selectedPaper) {
                        PaperPreset.WARM_PAPER -> Color(0xFFFAF6EE)
                        PaperPreset.LINED -> Color(0xFFFAF6EE)
                        PaperPreset.BLANK -> Color(0xFFFFFFFF)
                        PaperPreset.DARK_SLATE -> Color(0xFF141923)
                        PaperPreset.TRANSPARENT -> Color(0xFFF1EFEA)
                    }
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Box(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = inputText.ifBlank { "Type or compose a message below..." },
                        fontFamily = customFontFamily ?: FontFamily.Serif,
                        fontSize = fontSizeSp.sp,
                        lineHeight = (fontSizeSp * 1.5f).sp,
                        color = Color(selectedInkInt),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Multiline Editor ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Note Composer",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${inputText.length} chars",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            if (inputText.isNotEmpty()) {
                                Text(
                                    text = "Clear",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable { inputText = "" }
                                        .padding(4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("composer_text_input"),
                        minLines = 3,
                        maxLines = 8,
                        placeholder = { Text("Write anything to render into handwriting...") }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Preset Quote Buttons
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            OutlinedButton(
                                onClick = {
                                    inputText = "Nothing is impossible, the word itself says 'I'm possible'!\n— Audrey Hepburn"
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Quote", fontSize = 12.sp)
                            }
                        }
                        item {
                            OutlinedButton(
                                onClick = {
                                    inputText = "Hi there! Just wanted to send you a warm handwritten greeting. Hope you're having an amazing day!"
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Greeting", fontSize = 12.sp)
                            }
                        }
                        item {
                            OutlinedButton(
                                onClick = {
                                    inputText = "Shopping List:\n• Fresh organic apples\n• Whole grain sourdough\n• Almond milk\n• Ground roasted coffee"
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Checklist", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Paper & Ink Controls ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Paper Surface",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(PaperPreset.entries.toTypedArray()) { paper ->
                            FilterChip(
                                selected = selectedPaper == paper,
                                onClick = { selectedPaper = paper },
                                label = { Text(paper.title, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Ink Color",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        for ((name, color, intVal) in inkChoices) {
                            val isSelected = selectedInkInt == intVal
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        if (isSelected) 3.dp else 1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                        CircleShape
                                    )
                                    .clickable { selectedInkInt = intVal }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Font Size: ${fontSizeSp.roundToInt()} pt",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Slider(
                        value = fontSizeSp,
                        onValueChange = { fontSizeSp = it },
                        valueRange = 14f..32f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Share PNG Action ---
            Button(
                onClick = {
                    if (inputText.isBlank()) {
                        scope.launch { snackbarHostState.showSnackbar("Please enter some text to export.") }
                        return@Button
                    }
                    isExporting = true
                    scope.launch {
                        val options = HandwritingRenderer.RenderOptions(
                            text = inputText,
                            paper = selectedPaper,
                            inkColorInt = selectedInkInt,
                            fontSizePx = fontSizeSp * 2.5f,
                            targetWidthPx = 1200
                        )

                        val renderResult = HandwritingRenderer.renderToPngFile(
                            context = context,
                            fontFile = fontFileRef,
                            options = options
                        )

                        isExporting = false
                        if (renderResult.isSuccess) {
                            val pngFile = renderResult.getOrThrow()
                            HandwritingRenderer.shareImage(context, pngFile)
                        } else {
                            snackbarHostState.showSnackbar("Export error: ${renderResult.exceptionOrNull()?.message}")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("export_and_share_button"),
                shape = RoundedCornerShape(14.dp),
                enabled = !isExporting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Rendering Handwritten Image...")
                } else {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Share Handwritten Image (PNG)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        SnackbarHost(hostState = snackbarHostState)
    }
}
