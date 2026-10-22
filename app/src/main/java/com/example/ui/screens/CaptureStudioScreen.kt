package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke as CanvasStroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FontRepository
import com.example.data.entity.GlyphEntity
import com.example.model.GlyphCategory
import com.example.model.GlyphInfo
import com.example.model.GlyphManifest
import com.example.model.Stroke
import com.example.model.StrokePoint
import com.example.model.StrokeSerializer
import kotlinx.coroutines.launch

@Composable
fun CaptureStudioScreen(
    profileId: Long,
    fontRepository: FontRepository,
    onNavigateBack: () -> Unit,
    onNavigateToFontStudio: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // State
    var selectedCategory by remember { mutableStateOf(GlyphCategory.LOWERCASE) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var savedGlyphsList by remember { mutableStateOf<List<GlyphEntity>>(emptyList()) }

    // Canvas strokes
    val currentStrokes = remember { mutableStateListOf<Stroke>() }
    val redoStack = remember { mutableStateListOf<Stroke>() }
    var activeStrokePoints by remember { mutableStateOf<List<StrokePoint>>(emptyList()) }
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    val categoryGlyphs = remember(selectedCategory) {
        GlyphManifest.items.filter { it.category == selectedCategory }
    }

    val currentGlyph = categoryGlyphs.getOrElse(currentIndex) { categoryGlyphs.first() }

    // Fetch saved glyphs for profile
    LaunchedEffect(profileId) {
        fontRepository.getGlyphsForProfile(profileId).collect { list ->
            savedGlyphsList = list
        }
    }

    // Load strokes when switching glyphs
    LaunchedEffect(currentGlyph.char, savedGlyphsList) {
        val existing = savedGlyphsList.find { it.codePoint == currentGlyph.char.code }
        currentStrokes.clear()
        redoStack.clear()
        activeStrokePoints = emptyList()
        if (existing != null && existing.strokeDataJson.isNotEmpty()) {
            val deserialized = StrokeSerializer.deserialize(existing.strokeDataJson)
            currentStrokes.addAll(deserialized)
        }
        hasUnsavedChanges = false
    }

    // Back handler for unsaved changes
    BackHandler {
        if (hasUnsavedChanges && currentStrokes.isNotEmpty()) {
            showExitDialog = true
        } else {
            onNavigateBack()
        }
    }

    // Progress calculation
    val totalRequired = GlyphManifest.requiredItems.size
    val savedRequiredCount = savedGlyphsList.count { glyph ->
        !glyph.isSkipped && GlyphManifest.requiredItems.any { it.char.code == glyph.codePoint }
    }
    val progressFraction = (savedRequiredCount.toFloat() / totalRequired.toFloat()).coerceIn(0f, 1f)

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
                onClick = {
                    if (hasUnsavedChanges && currentStrokes.isNotEmpty()) {
                        showExitDialog = true
                    } else {
                        onNavigateBack()
                    }
                },
                modifier = Modifier.testTag("capture_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Capture Studio",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "$savedRequiredCount of $totalRequired required glyphs",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onNavigateToFontStudio,
                modifier = Modifier.testTag("go_to_font_studio_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open Font Studio",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        LinearProgressIndicator(
            progress = { progressFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outline
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- Category Chips ---
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(GlyphCategory.entries.toTypedArray()) { cat ->
                    val selected = cat == selectedCategory
                    FilterChip(
                        selected = selected,
                        onClick = {
                            if (hasUnsavedChanges && currentStrokes.isNotEmpty()) {
                                showExitDialog = true
                            } else {
                                selectedCategory = cat
                                currentIndex = 0
                            }
                        },
                        label = { Text(cat.title, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // --- Horizontal Character Strip ---
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(categoryGlyphs.indices.toList()) { idx ->
                    val item = categoryGlyphs[idx]
                    val isSelected = idx == currentIndex
                    val isSaved = savedGlyphsList.any { it.codePoint == item.char.code && !it.isSkipped }

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    isSaved -> MaterialTheme.colorScheme.primaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                            .border(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                if (hasUnsavedChanges && currentStrokes.isNotEmpty()) {
                                    showExitDialog = true
                                } else {
                                    currentIndex = idx
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.char.toString(),
                            fontSize = 18.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                isSaved -> MaterialTheme.colorScheme.onPrimaryContainer
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- Drawing Target Info ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = currentGlyph.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${selectedCategory.title} · ${currentIndex + 1} of ${categoryGlyphs.size}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Reference watermark pill
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = currentGlyph.char.toString(),
                        fontSize = 28.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // --- The Guided Handwriting Drawing Canvas ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFAF6EE) // Warm Parchment drawing cell
                ),
                border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Canvas drawing and guideline layers
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(currentGlyph.char) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        hasUnsavedChanges = true
                                        val normX = (offset.x / size.width).coerceIn(0f, 1f)
                                        val normY = (offset.y / size.height).coerceIn(0f, 1f)
                                        activeStrokePoints = listOf(StrokePoint(normX, normY))
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        val normX = (change.position.x / size.width).coerceIn(0f, 1f)
                                        val normY = (change.position.y / size.height).coerceIn(0f, 1f)
                                        activeStrokePoints = activeStrokePoints + StrokePoint(normX, normY)
                                    },
                                    onDragEnd = {
                                        if (activeStrokePoints.isNotEmpty()) {
                                            currentStrokes.add(Stroke(activeStrokePoints))
                                            redoStack.clear()
                                            activeStrokePoints = emptyList()
                                        }
                                    },
                                    onDragCancel = {
                                        activeStrokePoints = emptyList()
                                    }
                                )
                            }
                    ) {
                        val w = size.width
                        val h = size.height

                        // Guidelines:
                        val ascenderY = h * 0.20f
                        val xHeightY = h * 0.44f
                        val baselineY = h * 0.72f
                        val descenderY = h * 0.90f
                        val leftX = w * 0.15f
                        val rightX = w * 0.85f

                        val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)

                        // Ascender line (dashed light slate)
                        drawLine(
                            color = Color(0xFFCBD5E1),
                            start = Offset(0f, ascenderY),
                            end = Offset(w, ascenderY),
                            strokeWidth = 1.5f,
                            pathEffect = dashedEffect
                        )

                        // X-height line (dashed light slate)
                        drawLine(
                            color = Color(0xFFCBD5E1),
                            start = Offset(0f, xHeightY),
                            end = Offset(w, xHeightY),
                            strokeWidth = 1.5f,
                            pathEffect = dashedEffect
                        )

                        // Baseline line (solid prominent ink line)
                        drawLine(
                            color = Color(0xFF94A3B8),
                            start = Offset(0f, baselineY),
                            end = Offset(w, baselineY),
                            strokeWidth = 2.5f
                        )

                        // Descender line (dashed)
                        if (currentGlyph.hasDescender) {
                            drawLine(
                                color = Color(0xFFE2E8F0),
                                start = Offset(0f, descenderY),
                                end = Offset(w, descenderY),
                                strokeWidth = 1.5f,
                                pathEffect = dashedEffect
                            )
                        }

                        // Side boundary markers
                        drawLine(
                            color = Color(0xFFF1F5F9),
                            start = Offset(leftX, 0f),
                            end = Offset(leftX, h),
                            strokeWidth = 1f,
                            pathEffect = dashedEffect
                        )
                        drawLine(
                            color = Color(0xFFF1F5F9),
                            start = Offset(rightX, 0f),
                            end = Offset(rightX, h),
                            strokeWidth = 1f,
                            pathEffect = dashedEffect
                        )

                        // Draw completed strokes
                        val inkColor = Color(0xFF1E3A8A)
                        val strokeWidthPx = 10f

                        for (stroke in currentStrokes) {
                            if (stroke.points.isEmpty()) continue
                            if (stroke.points.size == 1) {
                                val p = stroke.points[0]
                                drawCircle(
                                    color = inkColor,
                                    radius = strokeWidthPx / 2f,
                                    center = Offset(p.x * w, p.y * h)
                                )
                            } else {
                                val path = Path()
                                val first = stroke.points[0]
                                path.moveTo(first.x * w, first.y * h)
                                for (i in 1 until stroke.points.size) {
                                    val pt = stroke.points[i]
                                    path.lineTo(pt.x * w, pt.y * h)
                                }
                                drawPath(
                                    path = path,
                                    color = inkColor,
                                    style = CanvasStroke(
                                        width = strokeWidthPx,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                        }

                        // Draw currently active dragging stroke
                        if (activeStrokePoints.isNotEmpty()) {
                            val path = Path()
                            val first = activeStrokePoints[0]
                            path.moveTo(first.x * w, first.y * h)
                            for (i in 1 until activeStrokePoints.size) {
                                val pt = activeStrokePoints[i]
                                path.lineTo(pt.x * w, pt.y * h)
                            }
                            drawPath(
                                path = path,
                                color = inkColor,
                                style = CanvasStroke(
                                    width = strokeWidthPx,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }

                    // Watermark / guideline labels
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("ascender", fontSize = 10.sp, color = Color(0xFF94A3B8))
                        Text("baseline", fontSize = 10.sp, color = Color(0xFF64748B))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- Canvas Action Buttons (Undo, Redo, Clear) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (currentStrokes.isNotEmpty()) {
                            hasUnsavedChanges = true
                            val last = currentStrokes.removeAt(currentStrokes.lastIndex)
                            redoStack.add(last)
                        }
                    },
                    enabled = currentStrokes.isNotEmpty(),
                    modifier = Modifier.testTag("undo_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (currentStrokes.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                    )
                }

                IconButton(
                    onClick = {
                        if (redoStack.isNotEmpty()) {
                            hasUnsavedChanges = true
                            val last = redoStack.removeAt(redoStack.lastIndex)
                            currentStrokes.add(last)
                        }
                    },
                    enabled = redoStack.isNotEmpty(),
                    modifier = Modifier.testTag("redo_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tint = if (redoStack.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                    )
                }

                IconButton(
                    onClick = {
                        if (currentStrokes.isNotEmpty()) {
                            showClearConfirmDialog = true
                        }
                    },
                    enabled = currentStrokes.isNotEmpty(),
                    modifier = Modifier.testTag("clear_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear Strokes",
                        tint = if (currentStrokes.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                    )
                }

                // Skip button (optional)
                TextButton(
                    onClick = {
                        scope.launch {
                            fontRepository.saveGlyph(profileId, currentGlyph.char, emptyList(), isSkipped = true)
                            hasUnsavedChanges = false
                            if (currentIndex < categoryGlyphs.lastIndex) {
                                currentIndex++
                            }
                        }
                    },
                    modifier = Modifier.testTag("skip_glyph_button")
                ) {
                    Text("Skip", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- Primary Save & Next Action ---
            Button(
                onClick = {
                    if (currentStrokes.isEmpty()) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Please draw the letter '${currentGlyph.char}' before saving.")
                        }
                        return@Button
                    }

                    scope.launch {
                        fontRepository.saveGlyph(
                            profileId = profileId,
                            char = currentGlyph.char,
                            strokes = currentStrokes.toList(),
                            isSkipped = false
                        )
                        hasUnsavedChanges = false

                        snackbarHostState.showSnackbar("Saved '${currentGlyph.char}'")

                        // Advance to next glyph in category
                        if (currentIndex < categoryGlyphs.lastIndex) {
                            currentIndex++
                        } else {
                            // Find next incomplete category
                            val nextCat = GlyphCategory.entries.find { cat ->
                                val items = GlyphManifest.items.filter { it.category == cat }
                                items.any { item -> savedGlyphsList.none { it.codePoint == item.char.code && !it.isSkipped } }
                            }
                            if (nextCat != null) {
                                selectedCategory = nextCat
                                currentIndex = 0
                            } else {
                                snackbarHostState.showSnackbar("All category glyphs captured! Opening Font Studio...")
                                onNavigateToFontStudio()
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_and_next_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (currentIndex == categoryGlyphs.lastIndex) "Save & Finish Category" else "Save & Next Glyph",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        SnackbarHost(hostState = snackbarHostState)
    }

    // --- Unsaved Changes Dialog ---
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Unsaved Strokes") },
            text = { Text("You have unsaved handwriting strokes on this canvas. Do you want to discard them?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        hasUnsavedChanges = false
                        onNavigateBack()
                    }
                ) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Keep Drawing")
                }
            }
        )
    }

    // --- Clear Confirmation Dialog ---
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear Canvas") },
            text = { Text("Clear all drawn strokes for '${currentGlyph.char}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        currentStrokes.clear()
                        redoStack.clear()
                        hasUnsavedChanges = true
                        showClearConfirmDialog = false
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
