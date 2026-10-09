package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Language
import com.example.ui.theme.IdeSurface

@Composable
fun QuickKeyToolbar(
    language: Language,
    onInsert: (snippet: String, cursorAdvance: Int) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onFormat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(IdeSurface)
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Undo / Redo
        IconButton(
            onClick = onUndo,
            modifier = Modifier.height(34.dp).testTag("btn_undo")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Undo,
                contentDescription = "Undo",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        IconButton(
            onClick = onRedo,
            modifier = Modifier.height(34.dp).testTag("btn_redo")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Redo,
                contentDescription = "Redo",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        IconButton(
            onClick = onFormat,
            modifier = Modifier.height(34.dp).testTag("btn_format")
        ) {
            Icon(
                imageVector = Icons.Default.AutoFixHigh,
                contentDescription = "Auto Format Code",
                tint = MaterialTheme.colorScheme.secondary
            )
        }

        // Tab (4 spaces)
        KeyChip(label = "Tab", onClick = { onInsert("    ", 4) })

        // Semicolon
        KeyChip(label = ";", onClick = { onInsert(";", 1) })

        // Brackets with smart cursor positioning inside
        KeyChip(label = "{ }", onClick = { onInsert("{\n    \n}", 6) })
        KeyChip(label = "( )", onClick = { onInsert("()", 1) })
        KeyChip(label = "[ ]", onClick = { onInsert("[]", 1) })

        // Quotes
        KeyChip(label = "\" \"", onClick = { onInsert("\"\"", 1) })
        KeyChip(label = "' '", onClick = { onInsert("''", 1) })

        // Preprocessor
        KeyChip(label = "#", onClick = { onInsert("#", 1) })

        // Pointers & Addresses
        KeyChip(label = "&", onClick = { onInsert("&", 1) })
        KeyChip(label = "*", onClick = { onInsert("*", 1) })
        KeyChip(label = "->", onClick = { onInsert("->", 2) })

        // Operators
        KeyChip(label = "=", onClick = { onInsert(" = ", 3) })
        KeyChip(label = "==", onClick = { onInsert(" == ", 4) })
        KeyChip(label = "!=", onClick = { onInsert(" != ", 4) })
        KeyChip(label = "&&", onClick = { onInsert(" && ", 4) })
        KeyChip(label = "||", onClick = { onInsert(" || ", 4) })

        if (language == Language.CPP) {
            KeyChip(label = "::", onClick = { onInsert("::", 2) })
            KeyChip(label = "<<", onClick = { onInsert(" << ", 4) })
            KeyChip(label = ">>", onClick = { onInsert(" >> ", 4) })
        }

        // Common format tokens
        KeyChip(label = "\\n", onClick = { onInsert("\\n", 2) })
        KeyChip(label = "%d", onClick = { onInsert("%d", 2) })
        KeyChip(label = "%s", onClick = { onInsert("%s", 2) })
    }
}

@Composable
private fun KeyChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
        modifier = Modifier.height(34.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
