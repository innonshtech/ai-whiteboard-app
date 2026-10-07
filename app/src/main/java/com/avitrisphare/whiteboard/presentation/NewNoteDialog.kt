package com.avitrisphare.whiteboard.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.avitrisphare.whiteboard.core.theme.LocalAvitriSphareColors
import com.avitrisphare.whiteboard.core.theme.AvitriSphareColors

@Composable
fun NewNoteDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "New Note",
                color = LocalAvitriSphareColors.current.textPrimary
            )
        },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = LocalAvitriSphareColors.current.textPrimary,
                    unfocusedTextColor = LocalAvitriSphareColors.current.textPrimary,
                    focusedBorderColor = LocalAvitriSphareColors.current.brand,
                    unfocusedBorderColor = LocalAvitriSphareColors.current.borderStandard
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(title.ifBlank { "Untitled" }) }
            ) {
                Text(
                    text = "Create",
                    color = LocalAvitriSphareColors.current.brand
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    color = LocalAvitriSphareColors.current.textMuted
                )
            }
        },
        containerColor = LocalAvitriSphareColors.current.background
    )
}