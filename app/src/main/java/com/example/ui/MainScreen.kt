package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Language
import com.example.ui.theme.IdeBackground
import com.example.ui.theme.IdeSurface
import com.example.viewmodel.BottomTab
import com.example.viewmodel.EditorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var showSettingsDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (state.language == Language.C) "C" else "C++",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "C/C++ Studio",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = state.language.defaultFileName,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFA6ADC8)
                            )
                        }
                    }
                },
                actions = {
                    // Language switcher toggle pill
                    Surface(
                        color = Color(0xFF313244),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LanguagePill(
                                text = "C",
                                selected = state.language == Language.C,
                                onClick = { viewModel.setLanguage(Language.C) },
                                testTag = "pill_lang_c"
                            )
                            LanguagePill(
                                text = "C++",
                                selected = state.language == Language.CPP,
                                onClick = { viewModel.setLanguage(Language.CPP) },
                                testTag = "pill_lang_cpp"
                            )
                        }
                    }

                    // Settings Icon
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("btn_open_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color(0xFFA6ADC8)
                        )
                    }

                    // Compile & Run Button
                    Button(
                        onClick = { viewModel.compileAndRun() },
                        enabled = !state.isCompiling,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFA6E3A1),
                            contentColor = Color(0xFF11111B)
                        ),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("btn_compile_run")
                    ) {
                        if (state.isCompiling) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF11111B)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Run",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Run",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = IdeSurface)
            )
        },
        containerColor = IdeBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.ime)
        ) {
            // Main Editor Section (weights dynamically based on terminal expansion)
            val editorWeight = if (state.isTerminalExpanded) 0.8f else 1.3f

            Box(
                modifier = Modifier
                    .weight(editorWeight)
                    .fillMaxWidth()
            ) {
                EditorView(
                    codeValue = state.codeValue,
                    onCodeChange = { viewModel.onCodeChange(it) },
                    language = state.language,
                    diagnostics = state.diagnostics,
                    fontSizeSp = state.fontSizeSp,
                    onDiagnosticClick = { diag ->
                        viewModel.setActiveTab(BottomTab.PROBLEMS)
                        viewModel.jumpToDiagnostic(diag)
                    }
                )
            }

            // Quick Code Toolbar above Bottom Panel / Keyboard
            QuickKeyToolbar(
                language = state.language,
                onInsert = { snippet, cursorAdvance ->
                    viewModel.insertSnippet(snippet, cursorAdvance)
                },
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() },
                onFormat = { viewModel.formatCode() }
            )

            // Bottom Panel Section (Terminal / Problems / Templates / CheatSheet)
            Column(
                modifier = Modifier
                    .weight(if (state.isTerminalExpanded) 1.5f else 1.0f)
                    .fillMaxWidth()
                    .background(Color(0xFF181825))
            ) {
                // Tab Bar
                TabRow(
                    selectedTabIndex = state.activeTab.ordinal,
                    containerColor = Color(0xFF181825),
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[state.activeTab.ordinal]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    Tab(
                        selected = state.activeTab == BottomTab.TERMINAL,
                        onClick = { viewModel.setActiveTab(BottomTab.TERMINAL) },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Terminal,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text("Terminal", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        },
                        modifier = Modifier.testTag("tab_terminal")
                    )

                    Tab(
                        selected = state.activeTab == BottomTab.PROBLEMS,
                        onClick = { viewModel.setActiveTab(BottomTab.PROBLEMS) },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (state.errorCount > 0) Color(0xFFFF5252) else Color(0xFFA6ADC8),
                                    modifier = Modifier.size(15.dp)
                                )
                                Text("Problems", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                if (state.errorCount + state.warningCount > 0) {
                                    Surface(
                                        color = if (state.errorCount > 0) Color(0xFFFF5252) else Color(0xFFFFB74D),
                                        shape = CircleShape,
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${state.errorCount + state.warningCount}",
                                                color = Color(0xFF11111B),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.testTag("tab_problems")
                    )

                    Tab(
                        selected = state.activeTab == BottomTab.TEMPLATES,
                        onClick = { viewModel.setActiveTab(BottomTab.TEMPLATES) },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Code,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text("Templates", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        },
                        modifier = Modifier.testTag("tab_templates")
                    )

                    Tab(
                        selected = state.activeTab == BottomTab.CHEAT_SHEET,
                        onClick = { viewModel.setActiveTab(BottomTab.CHEAT_SHEET) },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HelpOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text("Cheat Sheet", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        },
                        modifier = Modifier.testTag("tab_cheatsheet")
                    )

                    // Expand / Collapse terminal icon
                    IconButton(
                        onClick = { viewModel.toggleTerminalExpanded() },
                        modifier = Modifier.size(36.dp).testTag("btn_toggle_terminal_height")
                    ) {
                        Icon(
                            imageVector = if (state.isTerminalExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                            contentDescription = "Expand/Collapse Terminal",
                            tint = Color(0xFFA6ADC8)
                        )
                    }
                }

                // Tab Content Area
                Box(modifier = Modifier.fillMaxSize()) {
                    when (state.activeTab) {
                        BottomTab.TERMINAL -> {
                            TerminalView(
                                executionResult = state.executionResult,
                                isCompiling = state.isCompiling,
                                language = state.language,
                                stdin = state.stdin,
                                onStdinChange = { viewModel.setStdin(it) },
                                onClear = { viewModel.clearTerminal() },
                                onRunAgain = { viewModel.compileAndRun() }
                            )
                        }
                        BottomTab.PROBLEMS -> {
                            DiagnosticsView(
                                diagnostics = state.diagnostics,
                                onDiagnosticClick = { diag ->
                                    viewModel.jumpToDiagnostic(diag)
                                }
                            )
                        }
                        BottomTab.TEMPLATES -> {
                            TemplatesView(
                                currentLanguage = state.language,
                                onSelectTemplate = { template ->
                                    viewModel.applyTemplate(template)
                                }
                            )
                        }
                        BottomTab.CHEAT_SHEET -> {
                            CheatSheetView()
                        }
                    }
                }
            }
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(
            fontSizeSp = state.fontSizeSp,
            onFontSizeChange = { viewModel.setFontSize(it) },
            forceOffline = state.forceOffline,
            onToggleOffline = { viewModel.toggleOfflineMode() },
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
private fun LanguagePill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        modifier = Modifier.testTag(testTag)
    ) {
        Text(
            text = text,
            color = if (selected) Color(0xFF11111B) else Color(0xFFA6ADC8),
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
