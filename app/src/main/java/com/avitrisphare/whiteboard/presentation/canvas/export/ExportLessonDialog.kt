package com.avitrisphare.whiteboard.presentation.canvas.export

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun ExportLessonDialog(
    initialTitle: String,
    pageCount: Int,
    isExporting: Boolean,
    exportProgress: Float,
    exportStatusMessage: String,
    exportResult: ExportResult?,
    onDismiss: () -> Unit,
    onStartExport: (title: String, format: ExportFormat, destination: ExportDestination) -> Unit,
    onOpenFile: (ExportResult) -> Unit,
    onShareFile: (ExportResult) -> Unit
) {
    var lessonTitle by remember { mutableStateOf(initialTitle.ifBlank { "Classroom Lesson" }) }
    var selectedFormat by remember { mutableStateOf(ExportFormat.PDF) }
    var selectedDestination by remember { mutableStateOf(ExportDestination.LOCAL) }

    Dialog(
        onDismissRequest = { if (!isExporting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .width(620.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(20.dp)),
            color = Color(0xFF131722),
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Export Whiteboard Lesson",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF2563EB).copy(alpha = 0.2f))
                                    .border(1.dp, Color(0xFF3B82F6), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "$pageCount Pages",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF60A5FA)
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Save locally, backup to cloud, or export for GYOASH",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    if (!isExporting) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B))
                                .clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                if (exportResult != null) {
                    // Success View
                    ExportSuccessView(
                        result = exportResult,
                        lessonTitle = lessonTitle,
                        onOpenFile = { onOpenFile(exportResult) },
                        onShareFile = { onShareFile(exportResult) },
                        onDismiss = onDismiss
                    )
                } else {
                    // Configuration & In-progress view
                    Text(
                        "1. CHOOSE EXPORT FORMAT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(8.dp))

                    // Format Cards
                    FormatOptionCard(
                        format = ExportFormat.PDF,
                        icon = Icons.Default.Description,
                        accentColor = Color(0xFFEF4444),
                        isSelected = selectedFormat == ExportFormat.PDF,
                        enabled = !isExporting,
                        onClick = { selectedFormat = ExportFormat.PDF }
                    )

                    Spacer(Modifier.height(8.dp))

                    FormatOptionCard(
                        format = ExportFormat.IMAGE,
                        icon = Icons.Default.Image,
                        accentColor = Color(0xFF10B981),
                        isSelected = selectedFormat == ExportFormat.IMAGE,
                        enabled = !isExporting,
                        onClick = { selectedFormat = ExportFormat.IMAGE }
                    )

                    Spacer(Modifier.height(8.dp))

                    FormatOptionCard(
                        format = ExportFormat.IWB,
                        icon = Icons.Default.Layers,
                        accentColor = Color(0xFFA855F7),
                        isSelected = selectedFormat == ExportFormat.IWB,
                        enabled = !isExporting,
                        onClick = { selectedFormat = ExportFormat.IWB }
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "2. SELECT DESTINATION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(8.dp))

                    // Destination Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DestinationChip(
                            destination = ExportDestination.LOCAL,
                            icon = Icons.Default.Download,
                            accentColor = Color(0xFF10B981),
                            isSelected = selectedDestination == ExportDestination.LOCAL,
                            enabled = !isExporting,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedDestination = ExportDestination.LOCAL }
                        )
                        DestinationChip(
                            destination = ExportDestination.GOOGLE_DRIVE,
                            icon = Icons.Default.CloudUpload,
                            accentColor = Color(0xFFF59E0B),
                            isSelected = selectedDestination == ExportDestination.GOOGLE_DRIVE,
                            enabled = !isExporting,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedDestination = ExportDestination.GOOGLE_DRIVE }
                        )
                        DestinationChip(
                            destination = ExportDestination.ONE_DRIVE,
                            icon = Icons.Default.Cloud,
                            accentColor = Color(0xFF0284C7),
                            isSelected = selectedDestination == ExportDestination.ONE_DRIVE,
                            enabled = !isExporting,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedDestination = ExportDestination.ONE_DRIVE }
                        )
                        DestinationChip(
                            destination = ExportDestination.DROPBOX,
                            icon = Icons.Default.Inventory2,
                            accentColor = Color(0xFF6366F1),
                            isSelected = selectedDestination == ExportDestination.DROPBOX,
                            enabled = !isExporting,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedDestination = ExportDestination.DROPBOX }
                        )
                        DestinationChip(
                            destination = ExportDestination.SHARE,
                            icon = Icons.Default.Share,
                            accentColor = Color(0xFFEC4899),
                            isSelected = selectedDestination == ExportDestination.SHARE,
                            enabled = !isExporting,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedDestination = ExportDestination.SHARE }
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // Lesson Title Input
                    Text(
                        "3. FILE NAME",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(6.dp))

                    OutlinedTextField(
                        value = lessonTitle,
                        onValueChange = { lessonTitle = it },
                        singleLine = true,
                        enabled = !isExporting,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            Text(
                                ".${selectedFormat.extension}",
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8),
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = Color(0xFF0F131D),
                            unfocusedContainerColor = Color(0xFF0F131D)
                        )
                    )

                    Spacer(Modifier.height(18.dp))

                    // Progress section or Action button
                    if (isExporting) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0F131D))
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    exportStatusMessage,
                                    fontSize = 12.5.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "${(exportProgress * 100).toInt()}%",
                                    fontSize = 12.sp,
                                    color = Color(0xFF60A5FA),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = exportProgress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = Color(0xFF3B82F6),
                                trackColor = Color(0xFF1E293B)
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF1E293B))
                                    .clickable(onClick = onDismiss),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Cancel", color = Color(0xFF94A3B8), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(2f)
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF2563EB), Color(0xFF7C3AED))
                                        )
                                    )
                                    .clickable {
                                        onStartExport(lessonTitle, selectedFormat, selectedDestination)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Download,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Export as ${selectedFormat.displayName}",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormatOptionCard(
    format: ExportFormat,
    icon: ImageVector,
    accentColor: Color,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) accentColor.copy(alpha = 0.12f) else Color(0xFF0F131D))
            .border(
                width = if (isSelected) 1.8.dp else 1.dp,
                color = if (isSelected) accentColor else Color(0xFF242E42),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.2f))
                    .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        format.displayName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(accentColor.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            format.tag,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = accentColor
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    format.description,
                    fontSize = 11.5.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            Spacer(Modifier.width(8.dp))

            // Radio Indicator
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) accentColor else Color.Transparent)
                    .border(
                        2.dp,
                        if (isSelected) accentColor else Color(0xFF475569),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }
        }
    }
}

@Composable
private fun DestinationChip(
    destination: ExportDestination,
    icon: ImageVector,
    accentColor: Color,
    isSelected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) accentColor.copy(alpha = 0.18f) else Color(0xFF0F131D))
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) accentColor else Color(0xFF242E42),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = destination.displayName,
                tint = if (isSelected) accentColor else Color(0xFF94A3B8),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                destination.displayName,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.White else Color(0xFF94A3B8),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ExportSuccessView(
    result: ExportResult,
    lessonTitle: String,
    onOpenFile: () -> Unit,
    onShareFile: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Color(0xFF10B981).copy(alpha = 0.2f))
                .border(2.dp, Color(0xFF10B981), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Success",
                tint = Color(0xFF10B981),
                modifier = Modifier.size(34.dp)
            )
        }

        Spacer(Modifier.height(14.dp))

        Text(
            "Export Successful!",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(Modifier.height(4.dp))

        Text(
            "Your lesson was exported in ${result.durationMs}ms (Under 10s benchmark)",
            fontSize = 12.sp,
            color = Color(0xFF10B981),
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.height(16.dp))

        // Info Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0F131D))
                .border(1.dp, Color(0xFF242E42), RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Lesson Title:", fontSize = 12.sp, color = Color(0xFF64748B))
                Text(lessonTitle, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("File Name:", fontSize = 12.sp, color = Color(0xFF64748B))
                Text(result.file.name, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Format:", fontSize = 12.sp, color = Color(0xFF64748B))
                Text(result.format.displayName, fontSize = 12.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Pages:", fontSize = 12.sp, color = Color(0xFF64748B))
                Text("${result.pageCount} pages", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Size & Speed:", fontSize = 12.sp, color = Color(0xFF64748B))
                Text("${result.fileSizeFormatted} • ${result.durationMs} ms", fontSize = 12.sp, color = Color(0xFF34D399), fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Destination:", fontSize = 12.sp, color = Color(0xFF64748B))
                Text(result.destination.displayName, fontSize = 12.sp, color = Color(0xFFFBBF24), fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(20.dp))

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1E293B))
                    .clickable(onClick = onOpenFile),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.OpenInNew,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Open File", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF2563EB))
                    .clickable(onClick = onShareFile),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (result.destination == ExportDestination.LOCAL) "Share / Send" else "Send to ${result.destination.displayName}",
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(0.8f)
                    .height(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF334155))
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Text("Done", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
