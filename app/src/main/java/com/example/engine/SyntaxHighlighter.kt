package com.example.engine

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import com.example.model.Diagnostic
import com.example.model.DiagnosticSeverity
import com.example.model.Language

object SyntaxColors {
    // Cyber / Dark IDE theme palette
    val Keyword = Color(0xFFC792EA)       // Vibrant Purple / Violet
    val Type = Color(0xFF4FC1FF)          // Electric Cyan / Light Blue
    val Preprocessor = Color(0xFFFF9E64)  // Coral / Deep Orange
    val Header = Color(0xFFFFCB6B)        // Gold / Amber
    val String = Color(0xFF98C379)        // Spring Emerald Green
    val FormatSpec = Color(0xFFFFE082)    // Bright Gold for %d, \n
    val Char = Color(0xFFA8E6CF)          // Mint Green
    val Number = Color(0xFFFFB86C)        // Warm Peach / Orange
    val Comment = Color(0xFF7F848E)       // Muted Slate Grey
    val Function = Color(0xFF61AFEF)      // Vibrant Sky Blue
    val Operator = Color(0xFF89DDFF)      // Pale Cyan
    val ErrorSquiggle = Color(0xFFFF5252) // Neon Red
    val WarningSquiggle = Color(0xFFFFB74D) // Amber Yellow
    val ErrorBackground = Color(0x33FF5252)
    val WarningBackground = Color(0x22FFB74D)
}

class SyntaxHighlighter(
    private val language: Language,
    private val diagnostics: List<Diagnostic> = emptyList()
) : VisualTransformation {

    companion object {
        private val C_KEYWORDS = setOf(
            "auto", "break", "case", "const", "continue", "default", "do",
            "else", "enum", "extern", "for", "goto", "if", "inline", "register",
            "restrict", "return", "sizeof", "static", "struct", "switch",
            "typedef", "union", "volatile", "while", "_Bool", "_Complex", "_Imaginary"
        )

        private val CPP_EXTRA_KEYWORDS = setOf(
            "alignas", "alignof", "and", "and_eq", "asm", "bitand", "bitor",
            "catch", "class", "compl", "concept", "consteval", "constexpr",
            "constinit", "const_cast", "co_await", "co_return", "co_yield",
            "decltype", "delete", "dynamic_cast", "explicit", "export",
            "false", "friend", "mutable", "namespace", "new", "noexcept",
            "not", "not_eq", "nullptr", "operator", "or", "or_eq",
            "override", "private", "protected", "public", "reinterpret_cast",
            "requires", "static_assert", "static_cast", "template", "this",
            "thread_local", "throw", "true", "try", "typeid", "typename",
            "using", "virtual", "xor", "xor_eq", "NULL"
        )

        private val PRIMITIVE_TYPES = setOf(
            "int", "char", "float", "double", "void", "bool", "long", "short",
            "signed", "unsigned", "size_t", "ssize_t", "int8_t", "int16_t",
            "int32_t", "int64_t", "uint8_t", "uint16_t", "uint32_t", "uint64_t",
            "intptr_t", "uintptr_t", "ptrdiff_t", "FILE", "string", "vector",
            "map", "set", "unordered_map", "unordered_set", "pair", "tuple",
            "queue", "stack", "deque", "priority_queue", "unique_ptr", "shared_ptr", "weak_ptr"
        )

        private val PREPROCESSOR_DIRECTIVES = setOf(
            "include", "define", "undef", "ifdef", "ifndef", "if", "elif",
            "else", "endif", "pragma", "error", "warning", "line"
        )

        private val FORMAT_SPEC_REGEX = Regex("%[-+ #0]*[0-9]*(\\.[0-9]+)?[hlLzjt]*[diuoxXfFeEgGaAcspn%]|\\\\[nrtbfav'\"?\\\\0]")
    }

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val builder = AnnotatedString.Builder(raw)
        highlightCode(raw, builder)
        applyDiagnostics(raw, builder)
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }

    private fun highlightCode(raw: String, builder: AnnotatedString.Builder) {
        val len = raw.length
        var i = 0

        val allKeywords = if (language == Language.CPP) {
            C_KEYWORDS + CPP_EXTRA_KEYWORDS
        } else {
            C_KEYWORDS
        }

        while (i < len) {
            val c = raw[i]

            // Line comment: //
            if (c == '/' && i + 1 < len && raw[i + 1] == '/') {
                val end = raw.indexOf('\n', i).let { if (it == -1) len else it }
                builder.addStyle(
                    SpanStyle(color = SyntaxColors.Comment, fontStyle = FontStyle.Italic),
                    i, end
                )
                i = end
                continue
            }

            // Block comment: /* ... */
            if (c == '/' && i + 1 < len && raw[i + 1] == '*') {
                val end = raw.indexOf("*/", i + 2).let { if (it == -1) len else it + 2 }
                builder.addStyle(
                    SpanStyle(color = SyntaxColors.Comment, fontStyle = FontStyle.Italic),
                    i, end
                )
                i = end
                continue
            }

            // Preprocessor directive starting with #
            if (c == '#') {
                val lineEnd = raw.indexOf('\n', i).let { if (it == -1) len else it }
                var p = i + 1
                while (p < lineEnd && raw[p].isWhitespace()) p++
                val wordStart = p
                while (p < lineEnd && raw[p].isLetter()) p++
                val directive = raw.substring(wordStart, p)

                if (PREPROCESSOR_DIRECTIVES.contains(directive)) {
                    builder.addStyle(
                        SpanStyle(color = SyntaxColors.Preprocessor, fontWeight = FontWeight.Bold),
                        i, p
                    )

                    // If #include, highlight the header path <stdio.h> or "myheader.h"
                    if (directive == "include") {
                        val headerAngleStart = raw.indexOf('<', p)
                        if (headerAngleStart != -1 && headerAngleStart < lineEnd) {
                            val headerAngleEnd = raw.indexOf('>', headerAngleStart)
                            if (headerAngleEnd != -1 && headerAngleEnd <= lineEnd) {
                                builder.addStyle(
                                    SpanStyle(color = SyntaxColors.Header, fontWeight = FontWeight.Medium),
                                    headerAngleStart, headerAngleEnd + 1
                                )
                            }
                        }
                    }
                } else {
                    builder.addStyle(SpanStyle(color = SyntaxColors.Preprocessor), i, p)
                }
                i = p
                continue
            }

            // String literal: "..."
            if (c == '"') {
                var end = i + 1
                while (end < len && raw[end] != '"') {
                    if (raw[end] == '\\' && end + 1 < len) {
                        end += 2
                    } else if (raw[end] == '\n') {
                        break
                    } else {
                        end++
                    }
                }
                val stringEnd = if (end < len && raw[end] == '"') end + 1 else end
                builder.addStyle(SpanStyle(color = SyntaxColors.String), i, stringEnd)

                // Highlight %d, %s, \n format specifiers inside string
                val strContent = raw.substring(i, stringEnd)
                FORMAT_SPEC_REGEX.findAll(strContent).forEach { match ->
                    val absStart = i + match.range.first
                    val absEnd = i + match.range.last + 1
                    builder.addStyle(
                        SpanStyle(color = SyntaxColors.FormatSpec, fontWeight = FontWeight.Bold),
                        absStart, absEnd
                    )
                }
                i = stringEnd
                continue
            }

            // Char literal: '.'
            if (c == '\'') {
                var end = i + 1
                while (end < len && raw[end] != '\'') {
                    if (raw[end] == '\\' && end + 1 < len) {
                        end += 2
                    } else if (raw[end] == '\n') {
                        break
                    } else {
                        end++
                    }
                }
                val charEnd = if (end < len && raw[end] == '\'') end + 1 else end
                builder.addStyle(SpanStyle(color = SyntaxColors.Char), i, charEnd)
                i = charEnd
                continue
            }

            // Numbers: Hex, Float, Integer
            if (c.isDigit() || (c == '.' && i + 1 < len && raw[i + 1].isDigit())) {
                var end = i
                if (c == '0' && end + 1 < len && (raw[end + 1] == 'x' || raw[end + 1] == 'X')) {
                    end += 2
                    while (end < len && (raw[end].isDigit() || raw[end] in 'a'..'f' || raw[end] in 'A'..'F')) end++
                } else {
                    while (end < len && (raw[end].isDigit() || raw[end] == '.' || raw[end] == 'f' || raw[end] == 'F' || raw[end] == 'u' || raw[end] == 'U' || raw[end] == 'l' || raw[end] == 'L')) {
                        end++
                    }
                }
                builder.addStyle(SpanStyle(color = SyntaxColors.Number), i, end)
                i = end
                continue
            }

            // Words: Keywords, Types, Function calls, Identifiers
            if (c.isLetter() || c == '_') {
                var end = i
                while (end < len && (raw[end].isLetterOrDigit() || raw[end] == '_')) end++
                val word = raw.substring(i, end)

                when {
                    PRIMITIVE_TYPES.contains(word) -> {
                        builder.addStyle(
                            SpanStyle(color = SyntaxColors.Type, fontWeight = FontWeight.SemiBold),
                            i, end
                        )
                    }
                    allKeywords.contains(word) -> {
                        builder.addStyle(
                            SpanStyle(color = SyntaxColors.Keyword, fontWeight = FontWeight.Bold),
                            i, end
                        )
                    }
                    else -> {
                        // Check if followed by '(' (Function invocation / declaration)
                        var lookAhead = end
                        while (lookAhead < len && raw[lookAhead].isWhitespace() && raw[lookAhead] != '\n') {
                            lookAhead++
                        }
                        if (lookAhead < len && raw[lookAhead] == '(') {
                            builder.addStyle(
                                SpanStyle(color = SyntaxColors.Function, fontWeight = FontWeight.Medium),
                                i, end
                            )
                        }
                    }
                }
                i = end
                continue
            }

            // Operators & Punctuation
            if (c in "+-*/%=<>!&|^~?:.") {
                var end = i + 1
                if (end < len && raw[end] in "+-*/%=<>!&|^~?:.") end++
                if (end < len && raw[end] in "+-*/%=<>!&|^~?:.") end++
                builder.addStyle(SpanStyle(color = SyntaxColors.Operator), i, end)
                i = end
                continue
            }

            i++
        }
    }

    private fun applyDiagnostics(raw: String, builder: AnnotatedString.Builder) {
        if (diagnostics.isEmpty()) return

        val lines = raw.split('\n')
        val lineOffsets = mutableListOf<Int>()
        var curr = 0
        for (line in lines) {
            lineOffsets.add(curr)
            curr += line.length + 1
        }

        diagnostics.forEach { diag ->
            val zeroLine = diag.line - 1
            if (zeroLine in lines.indices) {
                val lineStart = lineOffsets[zeroLine]
                val lineLen = lines[zeroLine].length
                if (lineLen > 0) {
                    val errorStart = lineStart
                    val errorEnd = lineStart + lineLen

                    val color = when (diag.severity) {
                        DiagnosticSeverity.ERROR -> SyntaxColors.ErrorSquiggle
                        DiagnosticSeverity.WARNING -> SyntaxColors.WarningSquiggle
                        DiagnosticSeverity.INFO -> Color(0xFF64B5F6)
                    }
                    val bg = when (diag.severity) {
                        DiagnosticSeverity.ERROR -> SyntaxColors.ErrorBackground
                        DiagnosticSeverity.WARNING -> SyntaxColors.WarningBackground
                        DiagnosticSeverity.INFO -> Color(0x1564B5F6)
                    }

                    builder.addStyle(
                        SpanStyle(
                            textDecoration = TextDecoration.Underline,
                            color = color,
                            background = bg
                        ),
                        errorStart,
                        errorEnd
                    )
                }
            }
        }
    }
}
