package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsDialog(
    fontSizeSp: Int,
    onFontSizeChange: (Int) -> Unit,
    forceOffline: Boolean,
    onToggleOffline: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Editor & Compiler Settings",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Font size option
                Column {
                    Text(
                        text = "Editor Font Size",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(12, 14, 16, 18).forEach { size ->
                            FilterChip(
                                selected = fontSizeSp == size,
                                onClick = { onFontSizeChange(size) },
                                label = { Text("${size}sp", fontSize = 12.sp) }
                            )
                        }
                    }
                }

                // Offline Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "Offline Simulation Mode",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Use internal C/C++ engine without sending requests over the network.",
                            fontSize = 11.sp,
                            color = Color(0xFFA6ADC8)
                        )
                    }
                    Switch(
                        checked = forceOffline,
                        onCheckedChange = { onToggleOffline() },
                        modifier = Modifier.testTag("switch_offline_mode")
                    )
                }

                // Compiler Details
                Column {
                    Text(
                        text = "Active Toolchain",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "• GCC 10.2 / Clang 12 (C)\n• G++ 17 (C++)\n• Real-time AST & Token Linter\n• Built-in Offline Interpreter",
                        fontSize = 11.sp,
                        color = Color(0xFFA6ADC8),
                        lineHeight = 16.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
        containerColor = Color(0xFF1E1E2E),
        shape = RoundedCornerShape(12.dp)
    )
}
