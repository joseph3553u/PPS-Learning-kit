package com.example.model

enum class Language(
    val id: String,
    val displayName: String,
    val fileExtension: String,
    val compilerCmd: String,
    val pistonLanguage: String,
    val defaultFileName: String
) {
    C(
        id = "c",
        displayName = "C (GCC)",
        fileExtension = ".c",
        compilerCmd = "gcc main.c -Wall -O2 -o main && ./main",
        pistonLanguage = "c",
        defaultFileName = "main.c"
    ),
    CPP(
        id = "cpp",
        displayName = "C++ (G++ 17)",
        fileExtension = ".cpp",
        compilerCmd = "g++ -std=c++17 main.cpp -Wall -O2 -o main && ./main",
        pistonLanguage = "c++",
        defaultFileName = "main.cpp"
    )
}

enum class DiagnosticSeverity {
    ERROR,
    WARNING,
    INFO
}

data class Diagnostic(
    val line: Int,
    val column: Int = 1,
    val message: String,
    val severity: DiagnosticSeverity = DiagnosticSeverity.ERROR,
    val rule: String = "syntax"
)

enum class ExecutionStatus {
    SUCCESS,
    COMPILE_ERROR,
    RUNTIME_ERROR,
    TIMEOUT,
    OFFLINE_EXECUTED
}

data class ExecutionResult(
    val status: ExecutionStatus,
    val stdout: String,
    val stderr: String = "",
    val compileOutput: String = "",
    val exitCode: Int = 0,
    val durationMs: Long = 0,
    val command: String = "",
    val isOffline: Boolean = false
)

enum class AppTheme(val displayName: String) {
    CATPPUCCIN_DARK("Catppuccin Mocha"),
    MONOKAI("Monokai Pro"),
    DRACULA("Dracula Night"),
    LIGHT_STUDIO("Light Studio")
}
