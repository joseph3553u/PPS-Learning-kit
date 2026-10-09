package com.example.viewmodel

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.CodeLinter
import com.example.engine.CompilerService
import com.example.model.AppTheme
import com.example.model.CodeTemplate
import com.example.model.Diagnostic
import com.example.model.DiagnosticSeverity
import com.example.model.ExecutionResult
import com.example.model.ExecutionStatus
import com.example.model.Language
import com.example.model.TemplateLibrary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class BottomTab {
    TERMINAL,
    PROBLEMS,
    TEMPLATES,
    CHEAT_SHEET
}

data class EditorUiState(
    val language: Language = Language.C,
    val codeValue: TextFieldValue = TextFieldValue(TemplateLibrary.templates[0].code),
    val diagnostics: List<Diagnostic> = emptyList(),
    val executionResult: ExecutionResult? = null,
    val isCompiling: Boolean = false,
    val stdin: String = "",
    val activeTab: BottomTab = BottomTab.TERMINAL,
    val fontSizeSp: Int = 14,
    val theme: AppTheme = AppTheme.CATPPUCCIN_DARK,
    val forceOffline: Boolean = false,
    val isTerminalExpanded: Boolean = false,
    val errorCount: Int = 0,
    val warningCount: Int = 0
)

class EditorViewModel(
    private val compilerService: CompilerService = CompilerService()
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val undoStack = mutableListOf<TextFieldValue>()
    private val redoStack = mutableListOf<TextFieldValue>()
    private var lintJob: Job? = null

    init {
        runLint(_uiState.value.codeValue.text, _uiState.value.language)
    }

    fun onCodeChange(newValue: TextFieldValue) {
        val oldText = _uiState.value.codeValue.text
        if (oldText != newValue.text) {
            undoStack.add(_uiState.value.codeValue)
            if (undoStack.size > 50) undoStack.removeAt(0)
            redoStack.clear()
        }

        _uiState.value = _uiState.value.copy(codeValue = newValue)

        // Debounced Real-time linting
        lintJob?.cancel()
        lintJob = viewModelScope.launch {
            delay(250)
            runLint(newValue.text, _uiState.value.language)
        }
    }

    private fun runLint(code: String, language: Language) {
        val list = CodeLinter.lint(code, language)
        val errors = list.count { it.severity == DiagnosticSeverity.ERROR }
        val warnings = list.count { it.severity == DiagnosticSeverity.WARNING }
        _uiState.value = _uiState.value.copy(
            diagnostics = list,
            errorCount = errors,
            warningCount = warnings
        )
    }

    fun setLanguage(newLanguage: Language) {
        if (_uiState.value.language == newLanguage) return
        val defaultTemplate = TemplateLibrary.templates.firstOrNull { it.language == newLanguage }
        val newCode = defaultTemplate?.code ?: ""
        val newStdin = defaultTemplate?.sampleInput ?: ""

        undoStack.clear()
        redoStack.clear()

        _uiState.value = _uiState.value.copy(
            language = newLanguage,
            codeValue = TextFieldValue(newCode, selection = TextRange(newCode.length)),
            stdin = newStdin,
            executionResult = null
        )
        runLint(newCode, newLanguage)
    }

    fun setStdin(newStdin: String) {
        _uiState.value = _uiState.value.copy(stdin = newStdin)
    }

    fun setActiveTab(tab: BottomTab) {
        _uiState.value = _uiState.value.copy(activeTab = tab)
    }

    fun setFontSize(size: Int) {
        _uiState.value = _uiState.value.copy(fontSizeSp = size)
    }

    fun setTheme(theme: AppTheme) {
        _uiState.value = _uiState.value.copy(theme = theme)
    }

    fun toggleOfflineMode() {
        _uiState.value = _uiState.value.copy(forceOffline = !_uiState.value.forceOffline)
    }

    fun toggleTerminalExpanded() {
        _uiState.value = _uiState.value.copy(isTerminalExpanded = !_uiState.value.isTerminalExpanded)
    }

    fun applyTemplate(template: CodeTemplate) {
        undoStack.add(_uiState.value.codeValue)
        _uiState.value = _uiState.value.copy(
            language = template.language,
            codeValue = TextFieldValue(template.code, selection = TextRange(template.code.length)),
            stdin = template.sampleInput,
            activeTab = BottomTab.TERMINAL
        )
        runLint(template.code, template.language)
    }

    fun compileAndRun() {
        if (_uiState.value.isCompiling) return
        val currentCode = _uiState.value.codeValue.text
        val currentLang = _uiState.value.language
        val currentStdin = _uiState.value.stdin
        val offline = _uiState.value.forceOffline

        _uiState.value = _uiState.value.copy(
            isCompiling = true,
            activeTab = BottomTab.TERMINAL
        )

        viewModelScope.launch {
            val (result, compilerDiags) = compilerService.compileAndRun(
                code = currentCode,
                language = currentLang,
                stdin = currentStdin,
                forceOffline = offline
            )

            // Merge compiler diagnostics with static diagnostics
            val existing = _uiState.value.diagnostics.filter { it.rule != "gcc" }
            val merged = (existing + compilerDiags).sortedBy { it.line }
            val errors = merged.count { it.severity == DiagnosticSeverity.ERROR }
            val warnings = merged.count { it.severity == DiagnosticSeverity.WARNING }

            _uiState.value = _uiState.value.copy(
                executionResult = result,
                isCompiling = false,
                diagnostics = merged,
                errorCount = errors,
                warningCount = warnings
            )
        }
    }

    fun clearTerminal() {
        _uiState.value = _uiState.value.copy(executionResult = null)
    }

    fun jumpToDiagnostic(diag: Diagnostic) {
        val lines = _uiState.value.codeValue.text.split("\n")
        val targetLine = (diag.line - 1).coerceIn(0, (lines.size - 1).coerceAtLeast(0))
        var offset = 0
        for (i in 0 until targetLine) {
            offset += lines[i].length + 1
        }
        val colOffset = (diag.column - 1).coerceIn(0, lines.getOrNull(targetLine)?.length ?: 0)
        val finalOffset = (offset + colOffset).coerceIn(0, _uiState.value.codeValue.text.length)

        _uiState.value = _uiState.value.copy(
            codeValue = _uiState.value.codeValue.copy(selection = TextRange(finalOffset))
        )
    }

    fun insertSnippet(snippet: String, cursorAdvance: Int = snippet.length) {
        val current = _uiState.value.codeValue
        val text = current.text
        val sel = current.selection

        val newText = text.replaceRange(sel.start, sel.end, snippet)
        val newCursor = sel.start + cursorAdvance

        undoStack.add(current)
        redoStack.clear()

        val nextValue = TextFieldValue(newText, selection = TextRange(newCursor))
        _uiState.value = _uiState.value.copy(codeValue = nextValue)
        runLint(newText, _uiState.value.language)
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val prev = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(_uiState.value.codeValue)
            _uiState.value = _uiState.value.copy(codeValue = prev)
            runLint(prev.text, _uiState.value.language)
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(_uiState.value.codeValue)
            _uiState.value = _uiState.value.copy(codeValue = next)
            runLint(next.text, _uiState.value.language)
        }
    }

    fun formatCode() {
        val raw = _uiState.value.codeValue.text
        val formatted = autoFormat(raw)
        if (formatted != raw) {
            undoStack.add(_uiState.value.codeValue)
            redoStack.clear()
            val nextValue = TextFieldValue(formatted, selection = TextRange(formatted.length))
            _uiState.value = _uiState.value.copy(codeValue = nextValue)
            runLint(formatted, _uiState.value.language)
        }
    }

    private fun autoFormat(code: String): String {
        val lines = code.split("\n")
        val result = mutableListOf<String>()
        var indentLevel = 0

        for (rawLine in lines) {
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) {
                result.add("")
                continue
            }

            // Adjust unindent before printing if line starts with closing brace
            var closingCount = 0
            var i = 0
            while (i < trimmed.length && trimmed[i] == '}') {
                closingCount++
                i++
            }
            val effectiveIndent = (indentLevel - closingCount).coerceAtLeast(0)
            val indentSpaces = "    ".repeat(effectiveIndent)

            result.add(indentSpaces + trimmed)

            // Update indent for next lines
            val opens = trimmed.count { it == '{' }
            val closes = trimmed.count { it == '}' }
            indentLevel = (indentLevel + opens - closes).coerceAtLeast(0)
        }
        return result.joinToString("\n")
    }
}
