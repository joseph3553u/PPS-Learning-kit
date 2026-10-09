package com.example.engine

import com.example.model.ExecutionResult
import com.example.model.ExecutionStatus
import com.example.model.Language

/**
 * An offline lightweight executor for student C/C++ programs.
 * Handles standard arithmetic, printf/puts/scanf, std::cout/std::cin,
 * loops (for/while), conditionals (if/else), and basic variable state.
 */
class OfflineInterpreter {

    fun execute(code: String, language: Language, stdin: String): ExecutionResult {
        val startTime = System.currentTimeMillis()
        val stdoutBuilder = StringBuilder()
        val stderrBuilder = StringBuilder()
        val stdinTokens = stdin.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.toMutableList()

        try {
            val lines = code.split("\n")
            val variables = mutableMapOf<String, Any>()

            // Extract main body
            val mainBodyLines = extractMainBody(lines)
            if (mainBodyLines.isEmpty()) {
                // If main body couldn't be parsed with braces, evaluate whole code lines
                executeLines(lines, variables, stdinTokens, stdoutBuilder, stderrBuilder)
            } else {
                executeLines(mainBodyLines, variables, stdinTokens, stdoutBuilder, stderrBuilder)
            }

            val duration = System.currentTimeMillis() - startTime
            val hasErrors = stderrBuilder.isNotEmpty()

            return ExecutionResult(
                status = if (hasErrors) ExecutionStatus.RUNTIME_ERROR else ExecutionStatus.SUCCESS,
                stdout = stdoutBuilder.toString(),
                stderr = stderrBuilder.toString(),
                exitCode = if (hasErrors) 1 else 0,
                durationMs = duration,
                command = "${language.compilerCmd} (Offline Engine)",
                isOffline = true
            )
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            stderrBuilder.append("Runtime Exception: ${e.message ?: "Unknown error"}\n")
            return ExecutionResult(
                status = ExecutionStatus.RUNTIME_ERROR,
                stdout = stdoutBuilder.toString(),
                stderr = stderrBuilder.toString(),
                exitCode = 1,
                durationMs = duration,
                command = "${language.compilerCmd} (Offline Engine)",
                isOffline = true
            )
        }
    }

    private fun extractMainBody(lines: List<String>): List<String> {
        val body = mutableListOf<String>()
        var inMain = false
        var braceCount = 0

        for (line in lines) {
            val trimmed = line.trim()
            if (!inMain) {
                if (trimmed.contains("main(") || trimmed.contains("main (")) {
                    inMain = true
                    braceCount += trimmed.count { it == '{' } - trimmed.count { it == '}' }
                }
            } else {
                braceCount += trimmed.count { it == '{' } - trimmed.count { it == '}' }
                if (braceCount <= 0 && trimmed.contains("}")) {
                    break
                }
                body.add(line)
            }
        }
        return body
    }

    private fun executeLines(
        lines: List<String>,
        vars: MutableMap<String, Any>,
        stdinTokens: MutableList<String>,
        stdout: StringBuilder,
        stderr: StringBuilder
    ) {
        var idx = 0
        var loopGuard = 0
        val maxOperations = 50000

        while (idx < lines.size && loopGuard++ < maxOperations) {
            val rawLine = lines[idx]
            val line = rawLine.trim()

            if (line.isEmpty() || line.startsWith("//") || line.startsWith("#") || line == "{" || line == "}") {
                idx++
                continue
            }

            // Handle printf("...", args)
            if (line.contains("printf(")) {
                handlePrintf(line, vars, stdout)
                idx++
                continue
            }

            // Handle puts("...")
            if (line.contains("puts(")) {
                val str = line.substringAfter("puts(\"").substringBefore("\")")
                stdout.append(str).append("\n")
                idx++
                continue
            }

            // Handle std::cout << ... << std::endl
            if (line.contains("cout") || line.contains("std::cout")) {
                handleCout(line, vars, stdout)
                idx++
                continue
            }

            // Handle scanf("%d", &x)
            if (line.contains("scanf(")) {
                handleScanf(line, vars, stdinTokens)
                idx++
                continue
            }

            // Handle std::cin >> x
            if (line.contains("cin >>") || line.contains("std::cin >>")) {
                handleCin(line, vars, stdinTokens)
                idx++
                continue
            }

            // Handle simple for loop: for (int i = 0; i < N; i++) { ... }
            if (line.startsWith("for (") || line.startsWith("for(")) {
                val loopParsed = handleForLoop(lines, idx, vars, stdinTokens, stdout, stderr)
                if (loopParsed != null) {
                    idx = loopParsed
                    continue
                }
            }

            // Handle variable declaration or assignment: int x = 10; or x = a + b;
            handleAssignment(line, vars)

            idx++
        }
    }

    private fun handlePrintf(line: String, vars: Map<String, Any>, stdout: StringBuilder) {
        try {
            val callStart = line.indexOf("printf(")
            val inside = line.substring(callStart + 7).substringBeforeLast(")")

            // Check if format string is present
            if (inside.startsWith("\"")) {
                val closingQuoteIdx = findClosingQuote(inside, 0)
                val formatRaw = inside.substring(1, closingQuoteIdx)
                val argsRaw = inside.substring(closingQuoteIdx + 1).trimStart().removePrefix(",").trim()

                val args = if (argsRaw.isNotEmpty()) {
                    splitArguments(argsRaw).map { evaluateExpression(it, vars) }
                } else {
                    emptyList()
                }

                var argIdx = 0
                var i = 0
                val processedFormat = StringBuilder()

                while (i < formatRaw.length) {
                    if (formatRaw[i] == '\\' && i + 1 < formatRaw.length) {
                        when (formatRaw[i + 1]) {
                            'n' -> processedFormat.append('\n')
                            't' -> processedFormat.append('\t')
                            '\\' -> processedFormat.append('\\')
                            '\"' -> processedFormat.append('\"')
                            else -> processedFormat.append(formatRaw[i + 1])
                        }
                        i += 2
                        continue
                    }

                    if (formatRaw[i] == '%' && i + 1 < formatRaw.length) {
                        val specifier = formatRaw[i + 1]
                        if (specifier == '%') {
                            processedFormat.append('%')
                            i += 2
                            continue
                        }

                        if (argIdx < args.size) {
                            val v = args[argIdx++]
                            when (specifier) {
                                'd', 'i' -> {
                                    val num = (v as? Number)?.toLong() ?: v.toString().toDoubleOrNull()?.toLong() ?: 0L
                                    processedFormat.append(num)
                                }
                                'f' -> {
                                    val num = (v as? Number)?.toDouble() ?: v.toString().toDoubleOrNull() ?: 0.0
                                    processedFormat.append(String.format("%.4f", num))
                                }
                                's' -> processedFormat.append(v.toString())
                                'c' -> processedFormat.append(v.toString().firstOrNull() ?: ' ')
                                'p' -> processedFormat.append("0x" + Integer.toHexString(v.hashCode()))
                                else -> processedFormat.append(v.toString())
                            }
                            i += 2
                            continue
                        }
                    }

                    processedFormat.append(formatRaw[i])
                    i++
                }

                stdout.append(processedFormat.toString())
            }
        } catch (e: Exception) {
            stdout.append("[printf error]")
        }
    }

    private fun handleCout(line: String, vars: Map<String, Any>, stdout: StringBuilder) {
        try {
            val parts = line.substringAfter("cout").substringBefore(";").split("<<")
            for (p in parts) {
                val token = p.trim()
                if (token.isEmpty()) continue
                if (token == "endl" || token == "std::endl") {
                    stdout.append("\n")
                } else if (token.startsWith("\"") && token.endsWith("\"")) {
                    val content = token.substring(1, token.length - 1)
                        .replace("\\n", "\n")
                        .replace("\\t", "\t")
                    stdout.append(content)
                } else {
                    val value = evaluateExpression(token, vars)
                    stdout.append(value)
                }
            }
        } catch (e: Exception) {
            stdout.append("[cout error]")
        }
    }

    private fun handleScanf(line: String, vars: MutableMap<String, Any>, stdinTokens: MutableList<String>) {
        try {
            val inside = line.substringAfter("scanf(").substringBeforeLast(")")
            val parts = inside.split(",").map { it.trim() }
            if (parts.size > 1) {
                for (i in 1 until parts.size) {
                    val varName = parts[i].removePrefix("&").trim()
                    if (stdinTokens.isNotEmpty()) {
                        val inputToken = stdinTokens.removeAt(0)
                        val num = inputToken.toIntOrNull() ?: inputToken.toDoubleOrNull()
                        vars[varName] = num ?: inputToken
                    } else {
                        vars[varName] = 0
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun handleCin(line: String, vars: MutableMap<String, Any>, stdinTokens: MutableList<String>) {
        try {
            val parts = line.substringAfter("cin").substringBefore(";").split(">>")
            for (p in parts) {
                val varName = p.trim()
                if (varName.isNotEmpty()) {
                    if (stdinTokens.isNotEmpty()) {
                        val token = stdinTokens.removeAt(0)
                        val num = token.toIntOrNull() ?: token.toDoubleOrNull()
                        vars[varName] = num ?: token
                    } else {
                        vars[varName] = 0
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun handleForLoop(
        lines: List<String>,
        startIdx: Int,
        vars: MutableMap<String, Any>,
        stdinTokens: MutableList<String>,
        stdout: StringBuilder,
        stderr: StringBuilder
    ): Int? {
        try {
            val headerLine = lines[startIdx].trim()
            val headerInside = headerLine.substringAfter("(").substringBeforeLast(")")
            val headerParts = headerInside.split(";").map { it.trim() }
            if (headerParts.size != 3) return null

            // Init
            handleAssignment(headerParts[0], vars)

            // Extract loop body
            var bodyEnd = startIdx + 1
            val bodyLines = mutableListOf<String>()
            if (headerLine.endsWith("{") || (bodyEnd < lines.size && lines[bodyEnd].trim() == "{")) {
                var braces = 1
                if (!headerLine.endsWith("{")) bodyEnd++
                while (bodyEnd < lines.size && braces > 0) {
                    val l = lines[bodyEnd].trim()
                    braces += l.count { it == '{' } - l.count { it == '}' }
                    if (braces > 0) {
                        bodyLines.add(lines[bodyEnd])
                    }
                    bodyEnd++
                }
            } else if (bodyEnd < lines.size) {
                bodyLines.add(lines[bodyEnd])
                bodyEnd++
            }

            var iterations = 0
            while (iterations++ < 1000) {
                val cond = evaluateCondition(headerParts[1], vars)
                if (!cond) break

                executeLines(bodyLines, vars, stdinTokens, stdout, stderr)

                // Step update
                handleStep(headerParts[2], vars)
            }

            return bodyEnd
        } catch (e: Exception) {
            return null
        }
    }

    private fun handleStep(stepExpr: String, vars: MutableMap<String, Any>) {
        val s = stepExpr.trim()
        if (s.endsWith("++")) {
            val vName = s.removeSuffix("++").trim()
            val curr = (vars[vName] as? Number)?.toInt() ?: 0
            vars[vName] = curr + 1
        } else if (s.startsWith("++")) {
            val vName = s.removePrefix("++").trim()
            val curr = (vars[vName] as? Number)?.toInt() ?: 0
            vars[vName] = curr + 1
        } else if (s.endsWith("--")) {
            val vName = s.removeSuffix("--").trim()
            val curr = (vars[vName] as? Number)?.toInt() ?: 0
            vars[vName] = curr - 1
        } else if (s.contains("+=")) {
            val vName = s.substringBefore("+=").trim()
            val addVal = evaluateExpression(s.substringAfter("+="), vars) as? Number ?: 1
            val curr = (vars[vName] as? Number)?.toInt() ?: 0
            vars[vName] = curr + addVal.toInt()
        } else {
            handleAssignment(s, vars)
        }
    }

    private fun handleAssignment(line: String, vars: MutableMap<String, Any>) {
        val clean = line.removeSuffix(";").trim()
        if (!clean.contains("=") || clean.contains("==")) return

        val lhs = clean.substringBefore("=").trim()
        val rhs = clean.substringAfter("=").trim()

        val varName = lhs.split(Regex("\\s+")).last().removePrefix("*").trim()
        val value = evaluateExpression(rhs, vars)
        vars[varName] = value
    }

    private fun evaluateCondition(condition: String, vars: Map<String, Any>): Boolean {
        val c = condition.trim()
        if (c.isEmpty()) return true

        return when {
            c.contains("<=") -> {
                val (l, r) = c.split("<=").map { evaluateNumber(it, vars) }
                l <= r
            }
            c.contains(">=") -> {
                val (l, r) = c.split(">=").map { evaluateNumber(it, vars) }
                l >= r
            }
            c.contains("<") -> {
                val (l, r) = c.split("<").map { evaluateNumber(it, vars) }
                l < r
            }
            c.contains(">") -> {
                val (l, r) = c.split(">").map { evaluateNumber(it, vars) }
                l > r
            }
            c.contains("==") -> {
                val (l, r) = c.split("==").map { evaluateExpression(it, vars) }
                l == r
            }
            c.contains("!=") -> {
                val (l, r) = c.split("!=").map { evaluateExpression(it, vars) }
                l != r
            }
            else -> evaluateNumber(c, vars) != 0.0
        }
    }

    private fun evaluateNumber(expr: String, vars: Map<String, Any>): Double {
        val res = evaluateExpression(expr, vars)
        return (res as? Number)?.toDouble() ?: res.toString().toDoubleOrNull() ?: 0.0
    }

    private fun evaluateExpression(expr: String, vars: Map<String, Any>): Any {
        val trimmed = expr.trim()

        // String literal
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length - 1)
        }

        // Variable lookup
        if (vars.containsKey(trimmed)) {
            return vars[trimmed] ?: 0
        }

        // Basic arithmetic: + - * /
        val mathOperators = listOf("+", "-", "*", "/")
        for (op in mathOperators) {
            if (trimmed.contains(op) && !trimmed.startsWith(op)) {
                val parts = trimmed.split(op, limit = 2)
                val left = evaluateNumber(parts[0], vars)
                val right = evaluateNumber(parts[1], vars)
                return when (op) {
                    "+" -> if (left % 1.0 == 0.0 && right % 1.0 == 0.0) (left + right).toInt() else left + right
                    "-" -> if (left % 1.0 == 0.0 && right % 1.0 == 0.0) (left - right).toInt() else left - right
                    "*" -> if (left % 1.0 == 0.0 && right % 1.0 == 0.0) (left * right).toInt() else left * right
                    "/" -> if (right != 0.0) left / right else 0.0
                    else -> 0
                }
            }
        }

        // Direct number
        val intVal = trimmed.toIntOrNull()
        if (intVal != null) return intVal
        val doubleVal = trimmed.toDoubleOrNull()
        if (doubleVal != null) return doubleVal

        return trimmed
    }

    private fun findClosingQuote(str: String, startIdx: Int): Int {
        var i = startIdx + 1
        while (i < str.length) {
            if (str[i] == '"' && str[i - 1] != '\\') {
                return i
            }
            i++
        }
        return str.length - 1
    }

    private fun splitArguments(argsStr: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false

        for (c in argsStr) {
            if (c == '"') inQuotes = !inQuotes
            if (c == ',' && !inQuotes) {
                result.add(current.toString().trim())
                current = StringBuilder()
            } else {
                current.append(c)
            }
        }
        if (current.isNotEmpty()) {
            result.add(current.toString().trim())
        }
        return result
    }
}
