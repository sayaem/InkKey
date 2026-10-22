package com.example.model

enum class GlyphCategory(val title: String) {
    LOWERCASE("Lowercase"),
    UPPERCASE("Uppercase"),
    DIGITS("Digits"),
    PUNCTUATION("Punctuation"),
    SYMBOLS("Symbols")
}

data class GlyphInfo(
    val char: Char,
    val category: GlyphCategory,
    val name: String,
    val hint: String,
    val hasDescender: Boolean = false,
    val isOptional: Boolean = false
)

object GlyphManifest {
    val items: List<GlyphInfo> = buildList {
        // Lowercase a-z
        "abcdefghijklmnopqrstuvwxyz".forEach { c ->
            val hasDescender = c in "gjpqy"
            add(
                GlyphInfo(
                    char = c,
                    category = GlyphCategory.LOWERCASE,
                    name = "Letter '$c'",
                    hint = "Lowercase $c",
                    hasDescender = hasDescender
                )
            )
        }

        // Uppercase A-Z
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ".forEach { c ->
            val hasDescender = c == 'J' || c == 'Q'
            add(
                GlyphInfo(
                    char = c,
                    category = GlyphCategory.UPPERCASE,
                    name = "Capital '$c'",
                    hint = "Uppercase $c",
                    hasDescender = hasDescender
                )
            )
        }

        // Digits 0-9
        "0123456789".forEach { c ->
            add(
                GlyphInfo(
                    char = c,
                    category = GlyphCategory.DIGITS,
                    name = "Digit '$c'",
                    hint = "Number $c"
                )
            )
        }

        // Basic Punctuation
        val punctuationList = listOf(
            Triple('.', "Period", "Full stop"),
            Triple(',', "Comma", "Comma"),
            Triple('?', "Question Mark", "Question mark"),
            Triple('!', "Exclamation Mark", "Exclamation mark"),
            Triple(':', "Colon", "Colon"),
            Triple(';', "Semicolon", "Semicolon"),
            Triple('\'', "Apostrophe", "Single quote / apostrophe"),
            Triple('"', "Quotation Mark", "Double quote"),
            Triple('-', "Hyphen", "Dash or hyphen"),
            Triple('(', "Left Parenthesis", "Open paren"),
            Triple(')', "Right Parenthesis", "Close paren")
        )
        punctuationList.forEach { (c, name, hint) ->
            val hasDescender = c in ",;"
            add(
                GlyphInfo(
                    char = c,
                    category = GlyphCategory.PUNCTUATION,
                    name = name,
                    hint = hint,
                    hasDescender = hasDescender
                )
            )
        }

        // Optional Symbols
        val symbolsList = listOf(
            Triple('&', "Ampersand", "And symbol"),
            Triple('/', "Slash", "Forward slash"),
            Triple('@', "At Sign", "At symbol"),
            Triple('+', "Plus", "Plus sign"),
            Triple('=', "Equals", "Equal sign"),
            Triple('%', "Percent", "Percentage")
        )
        symbolsList.forEach { (c, name, hint) ->
            add(
                GlyphInfo(
                    char = c,
                    category = GlyphCategory.SYMBOLS,
                    name = name,
                    hint = hint,
                    isOptional = true
                )
            )
        }
    }

    val requiredItems: List<GlyphInfo> = items.filter { !it.isOptional }

    fun findByChar(c: Char): GlyphInfo? = items.firstOrNull { it.char == c }
}
