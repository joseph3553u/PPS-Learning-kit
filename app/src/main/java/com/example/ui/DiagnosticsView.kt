package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Diagnostic
import com.example.model.DiagnosticSeverity

@Composable
fun DiagnosticsView(
    diagnostics: List<Diagnostic>,
    onDiagnosticClick: (Diagnostic) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF181825))
            .padding(10.dp)
    ) {
        // Summary Header
        val errorCount = diagnostics.count { it.severity == DiagnosticSeverity.ERROR }
        val warningCount = diagnostics.count { it.severity == DiagnosticSeverity.WARNING }
        val infoCount = diagnostics.count { it.severity == DiagnosticSeverity.INFO }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Diagnostics & Linter",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.weight(1f))

            if (errorCount > 0) {
                BadgeChip(
                    text = "$errorCount Errors",
                    color = Color(0xFFFF5252),
                    bgColor = Color(0x33FF5252)
                )
            }
            if (warningCount > 0) {
                BadgeChip(
                    text = "$warningCount Warnings",
                    color = Color(0xFFFFB74D),
                    bgColor = Color(0x33FFB74D)
                )
            }
            if (infoCount > 0) {
                BadgeChip(
                    text = "$infoCount Notes",
                    color = Color(0xFF64B5F6),
                    bgColor = Color(0x3364B5F6)
                )
            }
        }

        if (diagnostics.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "No errors",
                        tint = Color(0xFFA6E3A1),
                        modifier = Modifier.size(42.dp)
                    )
                    Text(
                        text = "Clean Code! No Syntax Errors Found",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA6E3A1),
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Real-time linter is actively verifying bracket balance, semicolons, and syntax.",
                        color = Color(0xFFA6ADC8),
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().testTag("diagnostics_list"),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(diagnostics) { diag ->
                    DiagnosticItem(diag = diag, onClick = { onDiagnosticClick(diag) })
                }
            }
        }
    }
}

@Composable
private fun DiagnosticItem(
    diag: Diagnostic,
    onClick: () -> Unit
) {
    val (icon, tint, bg) = when (diag.severity) {
        DiagnosticSeverity.ERROR -> Triple(Icons.Default.Error, Color(0xFFFF5252), Color(0x22FF5252))
        DiagnosticSeverity.WARNING -> Triple(Icons.Default.Warning, Color(0xFFFFB74D), Color(0x22FFB74D))
        DiagnosticSeverity.INFO -> Triple(Icons.Default.Info, Color(0xFF64B5F6), Color(0x2264B5F6))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(bg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = diag.severity.name,
                    tint = tint,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = Color(0xFF313244),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "Line ${diag.line}:${diag.column}",
                            color = tint,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = diag.rule.replace("_", " ").uppercase(),
                        color = Color(0xFF6C7086),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = diag.message,
                    color = Color(0xFFCDD6F4),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Jump to line",
                tint = Color(0xFF6C7086),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun BadgeChip(
    text: String,
    color: Color,
    bgColor: Color
) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}
