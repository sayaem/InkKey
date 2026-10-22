package com.example.keyboard

import android.view.HapticFeedbackConstants
import android.view.inputmethod.EditorInfo
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

interface KeyboardActionListener {
    fun onCommitText(text: String)
    fun onDelete()
    fun onPerformAction(actionId: Int)
    fun onSwitchInputMethod()
    fun onHideKeyboard()
    fun onOpenHandwritingStudio()
}

@Composable
fun InkKeyKeyboardView(
    profileName: String,
    imeOptions: Int,
    hapticsEnabled: Boolean,
    keyHeightDp: Int,
    isDarkTheme: Boolean,
    listener: KeyboardActionListener
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    var currentPage by remember { mutableStateOf(KeyboardPage.ALPHA) }
    var shiftState by remember { mutableStateOf(ShiftState.OFF) }
    var lastShiftTapTime by remember { mutableStateOf(0L) }

    fun vibrate() {
        if (hapticsEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    // Color Palette: Editorial warm paper / deep midnight ink
    val bgColor = if (isDarkTheme) Color(0xFF141923) else Color(0xFFF7F5F0)
    val barColor = if (isDarkTheme) Color(0xFF1A2232) else Color(0xFFEBE6DC)
    val keyBgNormal = if (isDarkTheme) Color(0xFF232D3F) else Color(0xFFFFFFFF)
    val keyBgFunction = if (isDarkTheme) Color(0xFF1E2638) else Color(0xFFE2DDD2)
    val keyBgPressed = if (isDarkTheme) Color(0xFF334155) else Color(0xFFDDD7CB)
    val textColor = if (isDarkTheme) Color(0xFFF1F5F9) else Color(0xFF1E293B)
    val subtextColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
    val accentColor = Color(0xFF1E3A8A)
    val borderColor = if (isDarkTheme) Color(0xFF334155) else Color(0xFFD6CFC4)

    val rowHeight = keyHeightDp.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .navigationBarsPadding()
    ) {
        // --- Top Accessory Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(barColor)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        vibrate()
                        listener.onOpenHandwritingStudio()
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Create,
                    contentDescription = "Handwriting Studio",
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = profileName.ifBlank { "InkKey Handwriting" },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        vibrate()
                        listener.onSwitchInputMethod()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Switch Input Method",
                        tint = subtextColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = {
                        vibrate()
                        listener.onHideKeyboard()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Dismiss Keyboard",
                        tint = subtextColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // --- Keyboard Rows ---
        when (currentPage) {
            KeyboardPage.ALPHA -> {
                val isUpper = shiftState != ShiftState.OFF

                // Row 1
                KeyRow(height = rowHeight) {
                    for (letter in KeyboardLayouts.alphaRow1) {
                        val display = if (isUpper) letter.uppercase() else letter
                        RegularKey(
                            label = display,
                            textColor = textColor,
                            normalBg = keyBgNormal,
                            pressedBg = keyBgPressed,
                            borderColor = borderColor,
                            modifier = Modifier.weight(1f)
                        ) {
                            vibrate()
                            listener.onCommitText(display)
                            if (shiftState == ShiftState.ONESHOT) {
                                shiftState = ShiftState.OFF
                            }
                        }
                    }
                }

                // Row 2
                KeyRow(height = rowHeight) {
                    Spacer(modifier = Modifier.weight(0.5f))
                    for (letter in KeyboardLayouts.alphaRow2) {
                        val display = if (isUpper) letter.uppercase() else letter
                        RegularKey(
                            label = display,
                            textColor = textColor,
                            normalBg = keyBgNormal,
                            pressedBg = keyBgPressed,
                            borderColor = borderColor,
                            modifier = Modifier.weight(1f)
                        ) {
                            vibrate()
                            listener.onCommitText(display)
                            if (shiftState == ShiftState.ONESHOT) {
                                shiftState = ShiftState.OFF
                            }
                        }
                    }
                    Spacer(modifier = Modifier.weight(0.5f))
                }

                // Row 3 (Shift, letters, Backspace)
                KeyRow(height = rowHeight) {
                    // Shift Key
                    val shiftActive = shiftState != ShiftState.OFF
                    val shiftBg = if (shiftActive) accentColor else keyBgFunction
                    val shiftTint = if (shiftActive) Color.White else textColor
                    val shiftLabel = when (shiftState) {
                        ShiftState.CAPSLOCK -> "⇪"
                        ShiftState.ONESHOT -> "⇧"
                        ShiftState.OFF -> "⇧"
                    }

                    FunctionKey(
                        label = shiftLabel,
                        textColor = shiftTint,
                        bg = shiftBg,
                        borderColor = borderColor,
                        modifier = Modifier.weight(1.5f)
                    ) {
                        vibrate()
                        val now = System.currentTimeMillis()
                        shiftState = when {
                            now - lastShiftTapTime < 350 -> ShiftState.CAPSLOCK
                            shiftState == ShiftState.OFF -> ShiftState.ONESHOT
                            else -> ShiftState.OFF
                        }
                        lastShiftTapTime = now
                    }

                    for (letter in KeyboardLayouts.alphaRow3) {
                        val display = if (isUpper) letter.uppercase() else letter
                        RegularKey(
                            label = display,
                            textColor = textColor,
                            normalBg = keyBgNormal,
                            pressedBg = keyBgPressed,
                            borderColor = borderColor,
                            modifier = Modifier.weight(1f)
                        ) {
                            vibrate()
                            listener.onCommitText(display)
                            if (shiftState == ShiftState.ONESHOT) {
                                shiftState = ShiftState.OFF
                            }
                        }
                    }

                    // Backspace Key with repeat
                    BackspaceKey(
                        keyBg = keyBgFunction,
                        textColor = textColor,
                        borderColor = borderColor,
                        modifier = Modifier.weight(1.5f),
                        onDelete = {
                            vibrate()
                            listener.onDelete()
                        }
                    )
                }

                // Row 4 (?123, Comma, Space, Period, Action)
                KeyRow(height = rowHeight) {
                    FunctionKey(
                        label = "?123",
                        textColor = textColor,
                        bg = keyBgFunction,
                        borderColor = borderColor,
                        modifier = Modifier.weight(1.4f)
                    ) {
                        vibrate()
                        currentPage = KeyboardPage.NUMBERS
                    }

                    RegularKey(
                        label = ",",
                        textColor = textColor,
                        normalBg = keyBgNormal,
                        pressedBg = keyBgPressed,
                        borderColor = borderColor,
                        modifier = Modifier.weight(1f)
                    ) {
                        vibrate()
                        listener.onCommitText(",")
                    }

                    // Spacebar
                    RegularKey(
                        label = profileName.ifBlank { "space" },
                        textColor = subtextColor,
                        normalBg = keyBgNormal,
                        pressedBg = keyBgPressed,
                        borderColor = borderColor,
                        isSpace = true,
                        modifier = Modifier.weight(4.2f)
                    ) {
                        vibrate()
                        listener.onCommitText(" ")
                    }

                    RegularKey(
                        label = ".",
                        textColor = textColor,
                        normalBg = keyBgNormal,
                        pressedBg = keyBgPressed,
                        borderColor = borderColor,
                        modifier = Modifier.weight(1f)
                    ) {
                        vibrate()
                        listener.onCommitText(".")
                    }

                    ActionKey(
                        imeOptions = imeOptions,
                        accentColor = accentColor,
                        modifier = Modifier.weight(1.4f)
                    ) { actionId ->
                        vibrate()
                        listener.onPerformAction(actionId)
                    }
                }
            }

            KeyboardPage.NUMBERS, KeyboardPage.SYMBOLS -> {
                val isSymbols = currentPage == KeyboardPage.SYMBOLS
                val row1 = if (isSymbols) KeyboardLayouts.symRow1 else KeyboardLayouts.numRow1
                val row2 = if (isSymbols) KeyboardLayouts.symRow2 else KeyboardLayouts.numRow2
                val row3 = if (isSymbols) KeyboardLayouts.symRow3 else KeyboardLayouts.numRow3

                // Row 1
                KeyRow(height = rowHeight) {
                    for (char in row1) {
                        RegularKey(
                            label = char,
                            textColor = textColor,
                            normalBg = keyBgNormal,
                            pressedBg = keyBgPressed,
                            borderColor = borderColor,
                            modifier = Modifier.weight(1f)
                        ) {
                            vibrate()
                            listener.onCommitText(char)
                        }
                    }
                }

                // Row 2
                KeyRow(height = rowHeight) {
                    for (char in row2) {
                        RegularKey(
                            label = char,
                            textColor = textColor,
                            normalBg = keyBgNormal,
                            pressedBg = keyBgPressed,
                            borderColor = borderColor,
                            modifier = Modifier.weight(1f)
                        ) {
                            vibrate()
                            listener.onCommitText(char)
                        }
                    }
                }

                // Row 3
                KeyRow(height = rowHeight) {
                    FunctionKey(
                        label = if (isSymbols) "123" else "=\\<",
                        textColor = textColor,
                        bg = keyBgFunction,
                        borderColor = borderColor,
                        modifier = Modifier.weight(1.5f)
                    ) {
                        vibrate()
                        currentPage = if (isSymbols) KeyboardPage.NUMBERS else KeyboardPage.SYMBOLS
                    }

                    for (char in row3) {
                        RegularKey(
                            label = char,
                            textColor = textColor,
                            normalBg = keyBgNormal,
                            pressedBg = keyBgPressed,
                            borderColor = borderColor,
                            modifier = Modifier.weight(1f)
                        ) {
                            vibrate()
                            listener.onCommitText(char)
                        }
                    }

                    BackspaceKey(
                        keyBg = keyBgFunction,
                        textColor = textColor,
                        borderColor = borderColor,
                        modifier = Modifier.weight(1.5f),
                        onDelete = {
                            vibrate()
                            listener.onDelete()
                        }
                    )
                }

                // Row 4
                KeyRow(height = rowHeight) {
                    FunctionKey(
                        label = "ABC",
                        textColor = textColor,
                        bg = keyBgFunction,
                        borderColor = borderColor,
                        modifier = Modifier.weight(1.4f)
                    ) {
                        vibrate()
                        currentPage = KeyboardPage.ALPHA
                    }

                    RegularKey(
                        label = ",",
                        textColor = textColor,
                        normalBg = keyBgNormal,
                        pressedBg = keyBgPressed,
                        borderColor = borderColor,
                        modifier = Modifier.weight(1f)
                    ) {
                        vibrate()
                        listener.onCommitText(",")
                    }

                    RegularKey(
                        label = "space",
                        textColor = subtextColor,
                        normalBg = keyBgNormal,
                        pressedBg = keyBgPressed,
                        borderColor = borderColor,
                        isSpace = true,
                        modifier = Modifier.weight(4.2f)
                    ) {
                        vibrate()
                        listener.onCommitText(" ")
                    }

                    RegularKey(
                        label = ".",
                        textColor = textColor,
                        normalBg = keyBgNormal,
                        pressedBg = keyBgPressed,
                        borderColor = borderColor,
                        modifier = Modifier.weight(1f)
                    ) {
                        vibrate()
                        listener.onCommitText(".")
                    }

                    ActionKey(
                        imeOptions = imeOptions,
                        accentColor = accentColor,
                        modifier = Modifier.weight(1.4f)
                    ) { actionId ->
                        vibrate()
                        listener.onPerformAction(actionId)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
private fun KeyRow(
    height: androidx.compose.ui.unit.Dp,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .padding(horizontal = 3.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
    }
}

@Composable
private fun RegularKey(
    label: String,
    textColor: Color,
    normalBg: Color,
    pressedBg: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
    isSpace: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val bgColor by animateColorAsState(targetValue = if (isPressed) pressedBg else normalBg, label = "bg")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(7.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(7.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = if (isSpace) 12.sp else 19.sp,
            fontWeight = if (isSpace) FontWeight.Normal else FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FunctionKey(
    label: String,
    textColor: Color,
    bg: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(7.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(7.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun BackspaceKey(
    keyBg: Color,
    textColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
    onDelete: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var repeatJob by remember { mutableStateOf<Job?>(null) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(7.dp))
            .background(keyBg)
            .border(1.dp, borderColor, RoundedCornerShape(7.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onDelete()
                        repeatJob = scope.launch {
                            delay(400)
                            while (isActive) {
                                onDelete()
                                delay(60)
                            }
                        }
                        tryAwaitRelease()
                        repeatJob?.cancel()
                    }
                )
            }
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Backspace",
            tint = textColor,
            modifier = Modifier.size(19.dp)
        )
    }
}

@Composable
private fun ActionKey(
    imeOptions: Int,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onAction: (Int) -> Unit
) {
    val action = imeOptions and EditorInfo.IME_MASK_ACTION
    val (label, icon) = when (action) {
        EditorInfo.IME_ACTION_SEARCH -> Pair("Search", Icons.Default.Search)
        EditorInfo.IME_ACTION_SEND -> Pair("Send", Icons.AutoMirrored.Filled.Send)
        EditorInfo.IME_ACTION_DONE -> Pair("Done", Icons.Default.Check)
        EditorInfo.IME_ACTION_GO -> Pair("Go", null)
        EditorInfo.IME_ACTION_NEXT -> Pair("Next", null)
        else -> Pair("↵", null)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(7.dp))
            .background(accentColor)
            .clickable { onAction(action) }
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        } else {
            Text(
                text = label,
                color = Color.White,
                fontSize = if (label == "↵") 18.sp else 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
