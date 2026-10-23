package com.avitrisphare.whiteboard.presentation.canvas.export

import android.net.Uri
import com.avitrisphare.whiteboard.data.local.StrokeDto
import java.io.File

enum class ExportFormat(
    val displayName: String,
    val extension: String,
    val mimeType: String,
    val description: String,
    val tag: String
) {
    PDF(
        displayName = "PDF Document",
        extension = "pdf",
        mimeType = "application/pdf",
        description = "All pages merged into a single multi-page PDF with all ink, annotations, and backgrounds.",
        tag = "All Pages Merged"
    ),
    IMAGE(
        displayName = "PNG Image (.png)",
        extension = "png",
        mimeType = "image/png",
        description = "Direct high-resolution PNG image with all writing, graphs, diagrams, and content intact.",
        tag = "Direct .png"
    ),
    IWB(
        displayName = "GYOASH IWB Format (.iwb)",
        extension = "iwb",
        mimeType = "application/octet-stream",
        description = "Universal & GYOASH format with all layers, strokes, and pages intact for re-opening on any interactive whiteboard.",
        tag = "Re-openable & Layers"
    );

    companion object {
        @Deprecated("Use IMAGE instead for direct PNG export", ReplaceWith("IMAGE"))
        val IMAGE_ZIP: ExportFormat get() = IMAGE
    }
}

enum class ExportDestination(
    val displayName: String,
    val targetPackage: String?,
    val description: String
) {
    LOCAL(
        displayName = "Local Download",
        targetPackage = null,
        description = "Save directly to Downloads/GYOASH on this device"
    ),
    GOOGLE_DRIVE(
        displayName = "Google Drive",
        targetPackage = "com.google.android.apps.docs",
        description = "Upload directly to Google Drive"
    ),
    ONE_DRIVE(
        displayName = "OneDrive",
        targetPackage = "com.microsoft.skydrive",
        description = "Upload directly to Microsoft OneDrive"
    ),
    DROPBOX(
        displayName = "Dropbox",
        targetPackage = "com.dropbox.android",
        description = "Upload directly to Dropbox"
    ),
    SHARE(
        displayName = "Direct Share",
        targetPackage = null,
        description = "Share via email, Classroom, or other apps"
    )
}

data class GyoashIwbPackage(
    val format: String = "GYOASH_IWB_V1",
    val version: Int = 1,
    val app: String = "GYOASH Whiteboard",
    val exportedAt: Long = System.currentTimeMillis(),
    val title: String,
    val metadata: GyoashIwbMetadata,
    val pages: List<GyoashIwbPage>,
    val documentPath: String? = null,
    val documentName: String? = null,
    val documentKind: String? = null,
    val splitCount: Int? = null,
    val splitWeights: List<Float>? = null,
    val splitPanes: List<GyoashIwbPane>? = null
)

data class GyoashIwbMetadata(
    val pageCount: Int,
    val activePage: Int = 0,
    val canvasBackground: String,
    val isDarkCanvas: Boolean,
    val showGrid: Boolean,
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
)

data class GyoashIwbPage(
    val pageIndex: Int,
    val layers: List<GyoashIwbLayer>
)

data class GyoashIwbLayer(
    val id: String,
    val name: String,
    val visible: Boolean = true,
    val opacity: Float = 1.0f,
    val strokes: List<StrokeDto>
)

data class GyoashIwbPane(
    val kind: String? = null,
    val strokes: List<StrokeDto>? = null,
    val source: String? = null,
    val label: String? = null,
    val pageIndex: Int? = null,
    val pageCount: Int? = null,
    val tool: String? = null
)

data class ExportResult(
    val file: File,
    val uri: Uri,
    val format: ExportFormat,
    val destination: ExportDestination,
    val pageCount: Int,
    val durationMs: Long,
    val fileSizeFormatted: String
)
