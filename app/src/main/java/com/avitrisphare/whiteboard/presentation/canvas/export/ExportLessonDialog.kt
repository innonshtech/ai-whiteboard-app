package com.avitrisphare.whiteboard.presentation.canvas.export

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ExportLessonDialog(
    initialTitle: String,
    pageCount: Int,
    isExporting: Boolean,
    exportProgress: Float,
    exportStatusMessage: String,
    exportResult: ExportResult?,
    isDark: Boolean = false,
    onDismiss: () -> Unit,
    onStartExport: (title: String, format: ExportFormat, destination: ExportDestination) -> Unit,
    onOpenFile: (ExportResult) -> Unit,
    onShareFile: (ExportResult) -> Unit
) {
    var lessonTitle by remember { mutableStateOf(initialTitle.ifBlank { "Classroom Lesson" }) }
    var selectedFormat by remember { mutableStateOf(ExportFormat.PDF) }
    var selectedDestination by remember { mutableStateOf(ExportDestination.LOCAL) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (!isExporting) onDismiss()
            },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .padding(top = 56.dp, bottom = 76.dp)
                .widthIn(min = 320.dp, max = 460.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {} // Consume click inside
                ),
            shape = RoundedCornerShape(18.dp),
            color = if (isDark) Color(0xFF1E293B) else Color.White,
            shadowElevation = 16.dp,
            border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 18.dp, vertical = 16.dp)
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Export Whiteboard Lesson",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isDark) Color(0xFF1E3A8A) else Color(0xFFEFF6FF))
                                    .border(1.dp, if (isDark) Color(0xFF3B82F6) else Color(0xFFBFDBFE), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "$pageCount Pages",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB)
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Save locally, backup to cloud, or export for GYOASH",
                            fontSize = 11.5.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }

                    if (!isExporting) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9))
                                .clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = if (isDark) Color(0xFFCBD5E1) else Color(0xFF4B5563),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

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
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.8.sp
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

                    Spacer(Modifier.height(14.dp))

                    Text(
                        "2. SELECT DESTINATION",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.8.sp
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

                    Spacer(Modifier.height(14.dp))

                    // Lesson Title Input
                    Text(
                        "3. FILE NAME",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.8.sp
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
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1E293B),
                            unfocusedTextColor = Color(0xFF1E293B),
                            focusedBorderColor = Color(0xFF2563EB),
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        )
                    )

                    Spacer(Modifier.height(16.dp))

                    // Progress section or Action button
                    if (isExporting) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF8FAFC))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    exportStatusMessage,
                                    fontSize = 12.sp,
                                    color = Color(0xFF1E293B),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "${(exportProgress * 100).toInt()}%",
                                    fontSize = 12.sp,
                                    color = Color(0xFF2563EB),
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
                                color = Color(0xFF2563EB),
                                trackColor = Color(0xFFE2E8F0)
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
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF1F5F9))
                                    .clickable(onClick = onDismiss),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Cancel", color = Color(0xFF475569), fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(2f)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF2563EB), Color(0xFF4F46E5))
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
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Export as ${selectedFormat.displayName}",
                                        color = Color.White,
                                        fontSize = 13.5.sp,
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
            .background(if (isSelected) accentColor.copy(alpha = 0.08f) else Color(0xFFF8FAFC))
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) accentColor else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.15f))
                    .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        format.displayName,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(accentColor.copy(alpha = 0.15f))
                            .padding(horizontal = 5.dp, vertical = 1.5.dp)
                    ) {
                        Text(
                            format.tag,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = accentColor
                        )
                    }
                }
                Spacer(Modifier.height(1.dp))
                Text(
                    format.description,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }

            Spacer(Modifier.width(8.dp))

            // Radio Indicator
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) accentColor else Color.Transparent)
                    .border(
                        1.5.dp,
                        if (isSelected) accentColor else Color(0xFFCBD5E1),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
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
            .background(if (isSelected) accentColor.copy(alpha = 0.12f) else Color(0xFFF8FAFC))
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) accentColor else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = destination.displayName,
                tint = if (isSelected) accentColor else Color(0xFF64748B),
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.height(3.dp))
            Text(
                destination.displayName,
                fontSize = 9.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color(0xFF1E293B) else Color(0xFF64748B),
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
                .size(52.dp)
                .clip(CircleShape)
                .background(Color(0xFF10B981).copy(alpha = 0.15f))
                .border(1.5.dp, Color(0xFF10B981), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Success",
                tint = Color(0xFF10B981),
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(Modifier.height(10.dp))

        Text(
            "Export Successful!",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B)
        )

        Spacer(Modifier.height(3.dp))

        Text(
            "Exported in ${result.durationMs}ms (Under 10s benchmark)",
            fontSize = 11.5.sp,
            color = Color(0xFF059669),
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.height(14.dp))

        // Info Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF8FAFC))
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Lesson Title:", fontSize = 11.5.sp, color = Color(0xFF64748B))
                Text(lessonTitle, fontSize = 11.5.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(5.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("File Name:", fontSize = 11.5.sp, color = Color(0xFF64748B))
                Text(result.file.name, fontSize = 11.5.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(5.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Format:", fontSize = 11.5.sp, color = Color(0xFF64748B))
                Text(result.format.displayName, fontSize = 11.5.sp, color = Color(0xFF2563EB), fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(5.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Pages:", fontSize = 11.5.sp, color = Color(0xFF64748B))
                Text("${result.pageCount} pages", fontSize = 11.5.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(5.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Size & Speed:", fontSize = 11.5.sp, color = Color(0xFF64748B))
                Text("${result.fileSizeFormatted} • ${result.durationMs} ms", fontSize = 11.5.sp, color = Color(0xFF059669), fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(5.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Destination:", fontSize = 11.5.sp, color = Color(0xFF64748B))
                Text(result.destination.displayName, fontSize = 11.5.sp, color = Color(0xFFD97706), fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF1F5F9))
                    .clickable(onClick = onOpenFile),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.OpenInNew,
                        contentDescription = null,
                        tint = Color(0xFF1E293B),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text("Open File", color = Color(0xFF1E293B), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .height(40.dp)
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
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        if (result.destination == ExportDestination.LOCAL) "Share / Send" else "Send to ${result.destination.displayName}",
                        color = Color.White,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(0.8f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFE2E8F0))
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Text("Done", color = Color(0xFF1E293B), fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
