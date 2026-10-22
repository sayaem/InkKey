package com.example.keyboard

enum class KeyboardPage {
    ALPHA,
    NUMBERS,
    SYMBOLS
}

enum class ShiftState {
    OFF,
    ONESHOT,
    CAPSLOCK
}

object KeyboardLayouts {
    val alphaRow1 = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
    val alphaRow2 = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
    val alphaRow3 = listOf("z", "x", "c", "v", "b", "n", "m")

    val numRow1 = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    val numRow2 = listOf("-", "/", ":", ";", "(", ")", "$", "&", "@", "\"")
    val numRow3 = listOf(".", ",", "?", "!", "'")

    val symRow1 = listOf("[", "]", "{", "}", "#", "%", "^", "*", "+", "=")
    val symRow2 = listOf("_", "\\", "|", "~", "<", ">", "€", "£", "¥", "•")
    val symRow3 = listOf(".", ",", "?", "!", "'")
}
