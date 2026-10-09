package com.example.engine

import com.example.model.Diagnostic
import com.example.model.DiagnosticSeverity
import com.example.model.Language

object CodeLinter {

    fun lint(code: String, language: Language): List<Diagnostic> {
        val diagnostics = mutableListOf<Diagnostic>()
        if (code.isBlank()) return diagnostics

        val lines = code.split("\n")

        // 1. Bracket Matching Check
        checkBracketBalance(code, diagnostics)

        // 2. Line by Line Static Analysis
        var inBlockComment = false
        var hasMainFunction = false
        var hasStdio = false
        var hasIostream = false
        var hasUsedPrintf = false
        var hasUsedCout = false

        for ((index, rawLine) in lines.withIndex()) {
            val lineNum = index + 1
            val trimmed = rawLine.trim()

            // Handle multi-line comments
            if (inBlockComment) {
                if (trimmed.contains("*/")) {
                    inBlockComment = false
                }
                continue
            }
            if (trimmed.startsWith("/*")) {
                if (!trimmed.contains("*/")) {
                    inBlockComment = true
                }
                continue
            }

            // Skip empty lines and single-line comments
            if (trimmed.isEmpty() || trimmed.startsWith("//")) {
                continue
            }

            // Strip single-line comments for checks
            val lineWithoutComment = if (trimmed.contains("//")) {
                trimmed.substringBefore("//").trim()
            } else {
                trimmed
            }

            // Check Header inclusions
            if (lineWithoutComment.startsWith("#include")) {
                val includeTarget = lineWithoutComment.substringAfter("#include").trim()
                if (includeTarget.contains("stdio.h") || includeTarget.contains("cstdio")) {
                    hasStdio = true
                }
                if (includeTarget.contains("iostream")) {
                    hasIostream = true
                }

                if (!includeTarget.startsWith("<") && !includeTarget.startsWith("\"")) {
                    diagnostics.add(
                        Diagnostic(
                            line = lineNum,
                            column = 9,
                            message = "Invalid #include syntax: expected <header> or \"header\"",
                            severity = DiagnosticSeverity.ERROR,
                            rule = "preprocessor"
                        )
                    )
                } else if ((includeTarget.startsWith("<") && !includeTarget.contains(">")) ||
                    (includeTarget.startsWith("\"") && includeTarget.count { it == '"' } < 2)
                ) {
                    diagnostics.add(
                        Diagnostic(
                            line = lineNum,
                            column = lineWithoutComment.length,
                            message = "Unclosed #include directive",
                            severity = DiagnosticSeverity.ERROR,
                            rule = "preprocessor"
                        )
                    )
                }
                continue
            }

            // Check Preprocessor typo: e.g. #inclue, #defne
            if (lineWithoutComment.startsWith("#")) {
                val directive = lineWithoutComment.substring(1).trimStart().takeWhile { it.isLetter() }
                val validDirectives = listOf("include", "define", "undef", "ifdef", "ifndef", "if", "elif", "else", "endif", "pragma", "error")
                if (directive.isNotEmpty() && !validDirectives.contains(directive)) {
                    diagnostics.add(
                        Diagnostic(
                            line = lineNum,
                            column = 1,
                            message = "Unknown preprocessor directive '#$directive'",
                            severity = DiagnosticSeverity.WARNING,
                            rule = "preprocessor"
                        )
                    )
                }
                continue
            }

            // Detect main()
            if (lineWithoutComment.contains("main(") || lineWithoutComment.contains("main (")) {
                hasMainFunction = true
            }

            if (lineWithoutComment.contains("printf(")) {
                hasUsedPrintf = true
            }
            if (lineWithoutComment.contains("cout") || lineWithoutComment.contains("std::cout")) {
                hasUsedCout = true
            }

            // Check unclosed string literal on this line
            checkUnclosedLiterals(lineWithoutComment, lineNum, diagnostics)

            // Check Missing Semicolons
            checkMissingSemicolon(lineWithoutComment, lineNum, lines, index, diagnostics)

            // Student Trap: assignment in condition `if (a = b)`
            val ifAssignRegex = Regex("""\bif\s*\([^=]*[^!=<>]=[^=][^)]*\)""")
            if (ifAssignRegex.containsMatchIn(lineWithoutComment)) {
                diagnostics.add(
                    Diagnostic(
                        line = lineNum,
                        column = lineWithoutComment.indexOf("if") + 1,
                        message = "Suspicious assignment '=' in 'if' condition. Did you mean '=='?",
                        severity = DiagnosticSeverity.WARNING,
                        rule = "student_bug"
                    )
                )
            }

            // Student Trap: scanf without '&' for non-string formats
            if (lineWithoutComment.contains("scanf(")) {
                val scanfArgs = lineWithoutComment.substringAfter("scanf(").substringBefore(")")
                val parts = scanfArgs.split(",").map { it.trim() }
                if (parts.size > 1) {
                    val format = parts[0]
                    if ((format.contains("%d") || format.contains("%f") || format.contains("%lf")) && !format.contains("%s")) {
                        for (argIdx in 1 until parts.size) {
                            val arg = parts[argIdx]
                            if (arg.isNotEmpty() && !arg.startsWith("&") && !arg.contains("[") && !arg.startsWith("\"")) {
                                diagnostics.add(
                                    Diagnostic(
                                        line = lineNum,
                                        column = lineWithoutComment.indexOf(arg),
                                        message = "Possible missing '&' in scanf('$arg'). Remember to pass variable address for %d/%f.",
                                        severity = DiagnosticSeverity.WARNING,
                                        rule = "student_bug"
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Student Trap: division by literal zero
            val divZeroRegex = Regex("""/\s*0\b""")
            if (divZeroRegex.containsMatchIn(lineWithoutComment)) {
                diagnostics.add(
                    Diagnostic(
                        line = lineNum,
                        column = lineWithoutComment.indexOf("/ 0"),
                        message = "Division by zero leads to undefined behavior / crash.",
                        severity = DiagnosticSeverity.ERROR,
                        rule = "student_bug"
                    )
                )
            }
        }

        // Check header warning
        if (hasUsedPrintf && !hasStdio && language == Language.C) {
            diagnostics.add(
                Diagnostic(
                    line = 1,
                    column = 1,
                    message = "Using 'printf' without '#include <stdio.h>'. Add standard header to avoid compiler warnings.",
                    severity = DiagnosticSeverity.WARNING,
                    rule = "headers"
                )
            )
        }

        if (hasUsedCout && !hasIostream && language == Language.CPP) {
            diagnostics.add(
                Diagnostic(
                    line = 1,
                    column = 1,
                    message = "Using 'cout' without '#include <iostream>'. Add '#include <iostream>' at top.",
                    severity = DiagnosticSeverity.WARNING,
                    rule = "headers"
                )
            )
        }

        // Check entry point
        if (!hasMainFunction && lines.size > 2) {
            diagnostics.add(
                Diagnostic(
                    line = 1,
                    column = 1,
                    message = "No 'main()' function found. Execution requires an 'int main()' entry point.",
                    severity = DiagnosticSeverity.INFO,
                    rule = "entry_point"
                )
            )
        }

        return diagnostics.sortedBy { it.line }
    }

    private fun checkBracketBalance(code: String, diagnostics: MutableList<Diagnostic>) {
        data class OpenBracket(val char: Char, val line: Int, val col: Int)

        val stack = ArrayDeque<OpenBracket>()
        var line = 1
        var col = 1
        var inString = false
        var inChar = false
        var inLineComment = false
        var inBlockComment = false

        var i = 0
        while (i < code.length) {
            val c = code[i]

            if (c == '\n') {
                line++
                col = 1
                inLineComment = false
                i++
                continue
            }

            if (inLineComment) {
                col++
                i++
                continue
            }

            if (inBlockComment) {
                if (c == '*' && i + 1 < code.length && code[i + 1] == '/') {
                    inBlockComment = false
                    i += 2
                    col += 2
                    continue
                }
                col++
                i++
                continue
            }

            if (!inString && !inChar) {
                if (c == '/' && i + 1 < code.length && code[i + 1] == '/') {
                    inLineComment = true
                    i += 2
                    col += 2
                    continue
                }
                if (c == '/' && i + 1 < code.length && code[i + 1] == '*') {
                    inBlockComment = true
                    i += 2
                    col += 2
                    continue
                }
            }

            if (c == '"' && !inChar) {
                if (i == 0 || code[i - 1] != '\\') {
                    inString = !inString
                }
            } else if (c == '\'' && !inString) {
                if (i == 0 || code[i - 1] != '\\') {
                    inChar = !inChar
                }
            }

            if (!inString && !inChar) {
                when (c) {
                    '(', '{', '[' -> {
                        stack.addLast(OpenBracket(c, line, col))
                    }
                    ')' -> {
                        if (stack.isEmpty() || stack.last().char != '(') {
                            diagnostics.add(
                                Diagnostic(
                                    line = line,
                                    column = col,
                                    message = "Unmatched closing parenthesis ')'",
                                    severity = DiagnosticSeverity.ERROR,
                                    rule = "bracket"
                                )
                            )
                        } else {
                            stack.removeLast()
                        }
                    }
                    '}' -> {
                        if (stack.isEmpty() || stack.last().char != '{') {
                            diagnostics.add(
                                Diagnostic(
                                    line = line,
                                    column = col,
                                    message = "Unmatched closing brace '}'",
                                    severity = DiagnosticSeverity.ERROR,
                                    rule = "bracket"
                                )
                            )
                        } else {
                            stack.removeLast()
                        }
                    }
                    ']' -> {
                        if (stack.isEmpty() || stack.last().char != '[') {
                            diagnostics.add(
                                Diagnostic(
                                    line = line,
                                    column = col,
                                    message = "Unmatched closing bracket ']'",
                                    severity = DiagnosticSeverity.ERROR,
                                    rule = "bracket"
                                )
                            )
                        } else {
                            stack.removeLast()
                        }
                    }
                }
            }

            col++
            i++
        }

        while (stack.isNotEmpty()) {
            val unclosed = stack.removeLast()
            val expected = when (unclosed.char) {
                '(' -> "closing parenthesis ')'"
                '{' -> "closing brace '}'"
                '[' -> "closing bracket ']'"
                else -> "closing token"
            }
            diagnostics.add(
                Diagnostic(
                    line = unclosed.line,
                    column = unclosed.col,
                    message = "Unclosed '${unclosed.char}'. Missing $expected.",
                    severity = DiagnosticSeverity.ERROR,
                    rule = "bracket"
                )
            )
        }
    }

    private fun checkUnclosedLiterals(lineText: String, lineNum: Int, diagnostics: MutableList<Diagnostic>) {
        var inStr = false
        var strStart = 0
        var escaped = false

        for (i in lineText.indices) {
            val c = lineText[i]
            if (c == '\\' && inStr) {
                escaped = !escaped
                continue
            }
            if (c == '"' && !escaped) {
                inStr = !inStr
                if (inStr) strStart = i
            }
            escaped = false
        }

        if (inStr) {
            diagnostics.add(
                Diagnostic(
                    line = lineNum,
                    column = strStart + 1,
                    message = "Unclosed string literal. Missing terminating '\"' character.",
                    severity = DiagnosticSeverity.ERROR,
                    rule = "literal"
                )
            )
        }
    }

    private fun checkMissingSemicolon(
        line: String,
        lineNum: Int,
        allLines: List<String>,
        currentIndex: Int,
        diagnostics: MutableList<Diagnostic>
    ) {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return
        if (trimmed.startsWith("#")) return
        if (trimmed.endsWith("{") || trimmed.endsWith("}") || trimmed.endsWith(";") || trimmed.endsWith(":") || trimmed.endsWith(",") || trimmed.endsWith("\\")) {
            return
        }

        // Control flow constructs that don't need semicolon directly on this line
        val controlPrefixes = listOf("if", "else", "for", "while", "do", "switch", "case", "default", "struct", "class", "namespace", "template")
        val startsWithControl = controlPrefixes.any { kw ->
            trimmed == kw || trimmed.startsWith("$kw ") || trimmed.startsWith("$kw(")
        }

        // Function signature check e.g. int main(), void test(int x)
        val isFunctionHeader = trimmed.endsWith(")") && (
            trimmed.startsWith("int ") || trimmed.startsWith("void ") ||
                trimmed.startsWith("float ") || trimmed.startsWith("double ") ||
                trimmed.startsWith("char ") || trimmed.startsWith("bool ") ||
                trimmed.startsWith("auto ")
            )

        // Check if next non-empty line starts with {
        val nextNonEmpty = (currentIndex + 1 until allLines.size)
            .map { allLines[it].trim() }
            .firstOrNull { it.isNotEmpty() && !it.startsWith("//") }

        if (isFunctionHeader || (startsWithControl && nextNonEmpty?.startsWith("{") == true)) {
            return
        }

        // Check common statements that strictly require semicolons:
        // variable declarations, return, expressions, function calls, break, continue
        val isStatementCandidate = trimmed.startsWith("return") ||
            trimmed.startsWith("break") ||
            trimmed.startsWith("continue") ||
            trimmed.startsWith("typedef") ||
            trimmed.startsWith("using") ||
            trimmed.endsWith(")") || // e.g. printf("..."), obj.method()
            trimmed.matches(Regex("""^(int|char|float|double|bool|long|short|void|auto|string|vector<.*>)\s+[a-zA-Z0-9_].*""")) ||
            trimmed.contains("=") || // assignment
            trimmed.endsWith("++") || trimmed.endsWith("--")

        if (isStatementCandidate && !startsWithControl) {
            diagnostics.add(
                Diagnostic(
                    line = lineNum,
                    column = trimmed.length,
                    message = "Missing semicolon ';' at end of statement",
                    severity = DiagnosticSeverity.ERROR,
                    rule = "semicolon"
                )
            )
        }
    }
}
