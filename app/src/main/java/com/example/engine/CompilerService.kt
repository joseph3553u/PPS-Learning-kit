package com.example.engine

import com.example.model.Diagnostic
import com.example.model.DiagnosticSeverity
import com.example.model.ExecutionResult
import com.example.model.ExecutionStatus
import com.example.model.Language
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class CompilerService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .build()

    private val offlineInterpreter = OfflineInterpreter()

    companion object {
        private const val PISTON_API_URL = "https://emkc.org/api/v2/piston/execute"
        private val GCC_ERROR_REGEX = Regex("""(?:\w+\.(?:c|cpp)):(\d+):(\d+):\s*(error|warning|note):\s*(.+)""")
    }

    suspend fun compileAndRun(
        code: String,
        language: Language,
        stdin: String = "",
        forceOffline: Boolean = false
    ): Pair<ExecutionResult, List<Diagnostic>> = withContext(Dispatchers.IO) {
        if (forceOffline) {
            val result = offlineInterpreter.execute(code, language, stdin)
            return@withContext Pair(result, emptyList())
        }

        val startTime = System.currentTimeMillis()
        try {
            val requestJson = JSONObject().apply {
                put("language", language.pistonLanguage)
                put("version", "*")
                val filesArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("name", language.defaultFileName)
                        put("content", code)
                    })
                }
                put("files", filesArray)
                put("stdin", stdin)
                put("compile_timeout", 10000)
                put("run_timeout", 5000)
            }

            val body = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(PISTON_API_URL)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val duration = System.currentTimeMillis() - startTime

            if (!response.isSuccessful) {
                // If remote API returns error (e.g. rate limit 429), fall back to offline
                val fallback = offlineInterpreter.execute(code, language, stdin)
                return@withContext Pair(fallback, emptyList())
            }

            val responseBody = response.body?.string().orEmpty()
            val json = JSONObject(responseBody)

            val compileObj = json.optJSONObject("compile")
            val runObj = json.optJSONObject("run")

            val compileCode = compileObj?.optInt("code", 0) ?: 0
            val compileOutput = compileObj?.optString("output", "").orEmpty()

            val runStdout = runObj?.optString("stdout", "").orEmpty()
            val runStderr = runObj?.optString("stderr", "").orEmpty()
            val runCode = runObj?.optInt("code", 0) ?: 0

            val diagnostics = parseCompilerDiagnostics(compileOutput + "\n" + runStderr)

            val status = when {
                compileCode != 0 -> ExecutionStatus.COMPILE_ERROR
                runCode != 0 -> ExecutionStatus.RUNTIME_ERROR
                else -> ExecutionStatus.SUCCESS
            }

            val combinedStdout = if (compileOutput.isNotEmpty() && compileCode != 0) {
                ""
            } else {
                runStdout
            }

            val combinedStderr = if (compileCode != 0) {
                compileOutput
            } else {
                runStderr
            }

            val result = ExecutionResult(
                status = status,
                stdout = combinedStdout,
                stderr = combinedStderr,
                compileOutput = compileOutput,
                exitCode = if (compileCode != 0) compileCode else runCode,
                durationMs = duration,
                command = language.compilerCmd,
                isOffline = false
            )

            return@withContext Pair(result, diagnostics)
        } catch (_: Exception) {
            // Network failure / Offline fallback
            val fallback = offlineInterpreter.execute(code, language, stdin)
            return@withContext Pair(fallback, emptyList())
        }
    }

    private fun parseCompilerDiagnostics(output: String): List<Diagnostic> {
        val list = mutableListOf<Diagnostic>()
        output.lines().forEach { line ->
            val match = GCC_ERROR_REGEX.find(line)
            if (match != null) {
                val lineNum = match.groupValues[1].toIntOrNull() ?: 1
                val colNum = match.groupValues[2].toIntOrNull() ?: 1
                val sevStr = match.groupValues[3].lowercase()
                val message = match.groupValues[4].trim()

                val severity = when (sevStr) {
                    "error" -> DiagnosticSeverity.ERROR
                    "warning" -> DiagnosticSeverity.WARNING
                    else -> DiagnosticSeverity.INFO
                }

                list.add(
                    Diagnostic(
                        line = lineNum,
                        column = colNum,
                        message = message,
                        severity = severity,
                        rule = "gcc"
                    )
                )
            }
        }
        return list
    }
}
