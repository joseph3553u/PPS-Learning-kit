package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.SyntaxHighlighter
import com.example.model.Diagnostic
import com.example.model.DiagnosticSeverity
import com.example.model.Language
import com.example.ui.theme.IdeGutter
import com.example.ui.theme.IdeLineNumber

@Composable
fun EditorView(
    codeValue: TextFieldValue,
    onCodeChange: (TextFieldValue) -> Unit,
    language: Language,
    diagnostics: List<Diagnostic>,
    fontSizeSp: Int,
    onDiagnosticClick: (Diagnostic) -> Unit,
    modifier: Modifier = Modifier
) {
    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()

    val lines = remember(codeValue.text) {
        val split = codeValue.text.split("\n")
        if (split.isEmpty()) listOf("") else split
    }

    val diagnosticsByLine = remember(diagnostics) {
        diagnostics.groupBy { it.line }
    }

    val visualTransformation = remember(language, diagnostics) {
        SyntaxHighlighter(language, diagnostics)
    }

    val lineHeightSp = (fontSizeSp * 1.45).sp

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Line number and diagnostic markers gutter
        Column(
            modifier = Modifier
                .width(52.dp)
                .fillMaxHeight()
                .background(IdeGutter)
                .verticalScroll(verticalScroll)
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.End
        ) {
            lines.indices.forEach { lineIndex ->
                val lineNum = lineIndex + 1
                val diagsOnLine = diagnosticsByLine[lineNum]

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Diagnostic indicator dot
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .padding(end = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!diagsOnLine.isNullOrEmpty()) {
                            val highestSeverity = when {
                                diagsOnLine.any { it.severity == DiagnosticSeverity.ERROR } -> DiagnosticSeverity.ERROR
                                diagsOnLine.any { it.severity == DiagnosticSeverity.WARNING } -> DiagnosticSeverity.WARNING
                                else -> DiagnosticSeverity.INFO
                            }

                            val dotColor = when (highestSeverity) {
                                DiagnosticSeverity.ERROR -> Color(0xFFFF5252)
                                DiagnosticSeverity.WARNING -> Color(0xFFFFB74D)
                                DiagnosticSeverity.INFO -> Color(0xFF64B5F6)
                            }

                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(dotColor, CircleShape)
                                    .clickable { onDiagnosticClick(diagsOnLine.first()) }
                            )
                        }
                    }

                    // Line number text
                    Text(
                        text = "$lineNum",
                        color = if (diagsOnLine != null) Color(0xFFFF8A80) else IdeLineNumber,
                        fontSize = (fontSizeSp - 2).coerceAtLeast(10).sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (diagsOnLine != null) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.End,
                        lineHeight = lineHeightSp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Code content editor
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(verticalScroll)
                .horizontalScroll(horizontalScroll)
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            BasicTextField(
                value = codeValue,
                onValueChange = onCodeChange,
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(min = 600.dp)
                    .testTag("code_editor_field"),
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = fontSizeSp.sp,
                    lineHeight = lineHeightSp,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                visualTransformation = visualTransformation,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Ascii
                ),
                singleLine = false
            )
        }
    }
}
