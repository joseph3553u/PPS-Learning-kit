package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ExecutionResult
import com.example.model.ExecutionStatus
import com.example.model.Language
import com.example.ui.theme.TerminalBackground
import com.example.ui.theme.TerminalError
import com.example.ui.theme.TerminalPrompt
import com.example.ui.theme.TerminalText
import com.example.ui.theme.TerminalWarning

@Composable
fun TerminalView(
    executionResult: ExecutionResult?,
    isCompiling: Boolean,
    language: Language,
    stdin: String,
    onStdinChange: (String) -> Unit,
    onClear: () -> Unit,
    onRunAgain: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()
    var showStdinInput by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBackground)
            .border(1.dp, Color(0xFF282A36))
    ) {
        // Terminal Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF181825))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Command prompt indicator
                Text(
                    text = "$",
                    color = TerminalPrompt,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                Text(
                    text = executionResult?.command ?: language.compilerCmd,
                    color = Color(0xFFA6ADC8),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }

            // Status badges and action icons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isCompiling) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Running...",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                } else if (executionResult != null) {
                    val statusColor = if (executionResult.status == ExecutionStatus.SUCCESS) {
                        Color(0xFFA6E3A1)
                    } else {
                        TerminalError
                    }

                    Surface(
                        color = statusColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "code: ${executionResult.exitCode} (${executionResult.durationMs}ms)",
                            color = statusColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (executionResult.isOffline) {
                        Surface(
                            color = TerminalWarning.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "OFFLINE",
                                color = TerminalWarning,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Stdin toggle button
                IconButton(
                    onClick = { showStdinInput = !showStdinInput },
                    modifier = Modifier.size(28.dp).testTag("btn_terminal_stdin")
                ) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = "Configure STDIN input",
                        tint = if (stdin.isNotEmpty()) MaterialTheme.colorScheme.primary else Color(0xFF6C7086),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Copy output button
                IconButton(
                    onClick = {
                        val fullOutput = buildString {
                            if (executionResult?.compileOutput?.isNotEmpty() == true) {
                                appendLine(executionResult.compileOutput)
                            }
                            if (executionResult?.stdout?.isNotEmpty() == true) {
                                appendLine(executionResult.stdout)
                            }
                            if (executionResult?.stderr?.isNotEmpty() == true) {
                                appendLine(executionResult.stderr)
                            }
                        }
                        if (fullOutput.isNotEmpty()) {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Terminal Output", fullOutput))
                            Toast.makeText(context, "Terminal output copied!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.size(28.dp).testTag("btn_terminal_copy")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy terminal output",
                        tint = Color(0xFFA6ADC8),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Clear button
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(28.dp).testTag("btn_terminal_clear")
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear terminal output",
                        tint = Color(0xFFA6ADC8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // STDIN Input Drawer (if open)
        if (showStdinInput) {
            Surface(
                color = Color(0xFF1E1E2E),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                shape = RoundedCornerShape(6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "stdin:",
                        color = PrimaryPrompt,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = stdin,
                        onValueChange = onStdinChange,
                        modifier = Modifier.weight(1f).height(46.dp).testTag("stdin_input_field"),
                        placeholder = {
                            Text("e.g. 42 100 hello (for scanf/cin)", fontSize = 11.sp, color = Color(0xFF6C7086))
                        },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = TerminalText
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color(0xFF45475A)
                        ),
                        singleLine = true
                    )

                    IconButton(
                        onClick = onRunAgain,
                        modifier = Modifier.size(32.dp).testTag("btn_run_with_stdin")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Run with STDIN",
                            tint = Color(0xFFA6E3A1),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Terminal Content Body
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .verticalScroll(verticalScroll)
                .horizontalScroll(horizontalScroll)
        ) {
            SelectionContainer {
                Column {
                    if (isCompiling) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "➜ Compiling ${language.displayName}...",
                                color = TerminalPrompt,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            )
                        }
                    } else if (executionResult == null) {
                        // Empty terminal intro message
                        Text(
                            text = "C & C++ Interactive Terminal",
                            color = Color(0xFF89B4FA),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "GCC 10.2 / C++17 sandboxed environment ready.\nPress 'Run' above to compile and execute your program.",
                            color = Color(0xFF6C7086),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                        if (stdin.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Configured STDIN: $stdin",
                                color = TerminalWarning,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                    } else {
                        // Compiler Output (warnings / compile errors)
                        if (executionResult.compileOutput.isNotEmpty() && executionResult.status == ExecutionStatus.COMPILE_ERROR) {
                            Text(
                                text = "--- COMPILATION ERRORS ---",
                                color = TerminalError,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = executionResult.compileOutput,
                                color = TerminalError,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }

                        // Standard Output (STDOUT)
                        if (executionResult.stdout.isNotEmpty()) {
                            Text(
                                text = executionResult.stdout,
                                color = Color(0xFFA6E3A1),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }

                        // Standard Error (STDERR)
                        if (executionResult.stderr.isNotEmpty() && executionResult.status != ExecutionStatus.COMPILE_ERROR) {
                            Text(
                                text = executionResult.stderr,
                                color = TerminalError,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }

                        if (executionResult.stdout.isEmpty() && executionResult.stderr.isEmpty() && executionResult.compileOutput.isEmpty()) {
                            Text(
                                text = "[Program executed with no console output]",
                                color = Color(0xFF6C7086),
                                fontFamily = FontFamily.Monospace,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "--- Process finished with exit code ${executionResult.exitCode} (${executionResult.durationMs} ms) ---",
                            color = if (executionResult.exitCode == 0) Color(0xFF6C7086) else TerminalError,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

private val PrimaryPrompt = Color(0xFFA6E3A1)
