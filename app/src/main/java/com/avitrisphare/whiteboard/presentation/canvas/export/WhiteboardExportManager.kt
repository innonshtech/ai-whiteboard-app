package com.avitrisphare.whiteboard.presentation.canvas.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas as ComposeCanvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.FileProvider
import com.avitrisphare.whiteboard.data.local.StrokeDto
import com.avitrisphare.whiteboard.data.local.toDomain
import com.avitrisphare.whiteboard.data.local.toDto
import com.avitrisphare.whiteboard.domain.model.CanvasBackground
import com.avitrisphare.whiteboard.domain.model.Stroke
import com.avitrisphare.whiteboard.domain.model.isPlacedMedia
import com.avitrisphare.whiteboard.presentation.canvas.BoardDocument
import com.avitrisphare.whiteboard.presentation.canvas.CanvasPane
import com.avitrisphare.whiteboard.presentation.canvas.drawPageGrid
import com.avitrisphare.whiteboard.presentation.canvas.renderStroke
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

object WhiteboardExportManager {

    private val gson = Gson()

    private const val PDF_PAGE_WIDTH = 1920
    private const val PDF_PAGE_HEIGHT = 1080
    private const val MARGIN_X = 80f
    private const val MARGIN_TOP = 80f
    private const val MARGIN_BOTTOM = 96f
    private const val USABLE_WIDTH = PDF_PAGE_WIDTH - 2 * MARGIN_X // 1760f
    private const val USABLE_HEIGHT = PDF_PAGE_HEIGHT - MARGIN_TOP - MARGIN_BOTTOM // 904f

    private data class ContentBounds(
        val minX: Float,
        val minY: Float,
        val maxX: Float,
        val maxY: Float,
        val isEmpty: Boolean = false
    ) {
        val width: Float get() = (maxX - minX).coerceAtLeast(10f)
        val height: Float get() = (maxY - minY).coerceAtLeast(10f)
    }

    private sealed class ExportPagePlan {
        data class DocumentPage(
            val bitmap: Bitmap,
            val strokes: List<Stroke>,
            val originalPageIndex: Int
        ) : ExportPagePlan()

        data class WhiteboardPage(
            val strokes: List<Stroke>,
            val scale: Float,
            val offsetX: Float,
            val offsetY: Float,
            val originalPageIndex: Int,
            val sliceIndex: Int,
            val totalSlicesForBoardPage: Int
        ) : ExportPagePlan()
    }

    suspend fun exportWhiteboard(
        context: Context,
        lessonTitle: String,
        pages: List<List<Stroke>>,
        canvasBackground: CanvasBackground,
        isDarkCanvas: Boolean,
        showGrid: Boolean,
        document: BoardDocument?,
        renderedPages: Map<Int, Bitmap>,
        scale: Float,
        offsetX: Float,
        offsetY: Float,
        activePage: Int,
        splitCount: Int = 1,
        splitWeights: List<Float> = emptyList(),
        splitPanes: List<CanvasPane>? = null,
        format: ExportFormat,
        destination: ExportDestination,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): ExportResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val safeTitle = lessonTitle.trim().ifEmpty { "Whiteboard_Lesson" }
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val timestamp = System.currentTimeMillis()

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val effectivePages = if (pages.isEmpty()) listOf(emptyList()) else pages

        var totalExportedPages = effectivePages.size

        val exportedFile: File = when (format) {
            ExportFormat.PDF -> {
                val (pdfFile, totalPdfPages) = exportPdf(
                    context = context,
                    lessonTitle = lessonTitle,
                    safeTitle = safeTitle,
                    timestamp = timestamp,
                    pages = effectivePages,
                    canvasBackground = canvasBackground,
                    showGrid = showGrid,
                    renderedPages = renderedPages,
                    exportDir = exportDir,
                    onProgress = onProgress
                )
                totalExportedPages = totalPdfPages
                pdfFile
            }
            ExportFormat.IMAGE -> {
                val (pngFile, totalImages) = exportDirectPng(
                    context = context,
                    lessonTitle = lessonTitle,
                    safeTitle = safeTitle,
                    timestamp = timestamp,
                    pages = effectivePages,
                    activePage = activePage,
                    canvasBackground = canvasBackground,
                    isDarkCanvas = isDarkCanvas,
                    showGrid = showGrid,
                    renderedPages = renderedPages,
                    exportDir = exportDir,
                    onProgress = onProgress
                )
                totalExportedPages = totalImages
                pngFile
            }
            ExportFormat.IWB -> {
                exportIwb(
                    safeTitle = safeTitle,
                    timestamp = timestamp,
                    lessonTitle = lessonTitle,
                    pages = effectivePages,
                    canvasBackground = canvasBackground,
                    isDarkCanvas = isDarkCanvas,
                    showGrid = showGrid,
                    scale = scale,
                    offsetX = offsetX,
                    offsetY = offsetY,
                    activePage = activePage,
                    document = document,
                    splitCount = splitCount,
                    splitWeights = splitWeights,
                    splitPanes = splitPanes,
                    exportDir = exportDir,
                    onProgress = onProgress
                )
            }
        }

        onProgress(0.95f, "Preparing destination...")

        val resultUri = dispatchFile(
            context = context,
            file = exportedFile,
            format = format,
            destination = destination
        )

        val duration = System.currentTimeMillis() - startTime
        onProgress(1f, "Export Complete!")

        ExportResult(
            file = exportedFile,
            uri = resultUri,
            format = format,
            destination = destination,
            pageCount = totalExportedPages,
            durationMs = duration,
            fileSizeFormatted = formatFileSize(exportedFile.length())
        )
    }

    private fun isLightColor(argb: Int): Boolean {
        val alpha = (argb shr 24) and 0xFF
        if (alpha < 30) return false
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        val luminance = 0.2126 * (r / 255.0) + 0.7152 * (g / 255.0) + 0.0722 * (b / 255.0)
        return luminance > 0.68 || (r > 235 && g > 235 && b > 235)
    }

    private fun remapStrokeForWhiteBackground(stroke: Stroke): Stroke {
        if (stroke.isPlacedMedia()) return stroke

        val originalColor = stroke.colorValue
        val newColor = if (isLightColor(originalColor)) {
            0xFF111827.toInt() // Obsidian dark slate / black for crisp contrast
        } else {
            originalColor
        }

        return if (newColor != originalColor) {
            stroke.copy(colorValue = newColor)
        } else {
            stroke
        }
    }

    private fun computeContentBounds(strokes: List<Stroke>): ContentBounds {
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        var hasValidStroke = false

        for (stroke in strokes) {
            if (stroke.isEraser) continue
            if (stroke.points.isEmpty()) continue
            val b = stroke.bounds
            if (!b.left.isFinite() || !b.top.isFinite() || !b.right.isFinite() || !b.bottom.isFinite()) continue
            if (b.left.isNaN() || b.top.isNaN() || b.right.isNaN() || b.bottom.isNaN()) continue
            if (b.width <= 0f && b.height <= 0f) continue

            minX = min(minX, b.left)
            minY = min(minY, b.top)
            maxX = max(maxX, b.right)
            maxY = max(maxY, b.bottom)
            hasValidStroke = true
        }

        if (!hasValidStroke || minX >= maxX || minY >= maxY) {
            return ContentBounds(0f, 0f, PDF_PAGE_WIDTH.toFloat(), PDF_PAGE_HEIGHT.toFloat(), isEmpty = true)
        }

        val pad = 36f
        return ContentBounds(
            minX = minX - pad,
            minY = minY - pad,
            maxX = maxX + pad,
            maxY = maxY + pad,
            isEmpty = false
        )
    }

    private fun findBestSplitY(
        strokes: List<Stroke>,
        minY: Float,
        maxY: Float,
        preferredSplitY: Float,
        searchRadius: Float = 90f
    ): Float {
        val windowStart = (preferredSplitY - searchRadius).coerceAtLeast(minY)
        val windowEnd = (preferredSplitY + searchRadius).coerceAtMost(maxY)
        if (windowStart >= windowEnd) return preferredSplitY

        val intervals = mutableListOf<Pair<Float, Float>>()
        for (stroke in strokes) {
            if (stroke.isEraser) continue
            val b = stroke.bounds
            if (b.bottom < windowStart || b.top > windowEnd) continue
            intervals.add(Pair(b.top, b.bottom))
        }

        if (intervals.isEmpty()) return preferredSplitY

        var bestY = preferredSplitY
        var minCrossings = Int.MAX_VALUE

        var testY = windowStart
        while (testY <= windowEnd) {
            var crossings = 0
            for ((top, bottom) in intervals) {
                if (testY in top..bottom) {
                    crossings++
                }
            }
            if (crossings == 0) {
                val distToPref = abs(testY - preferredSplitY)
                val currentBestDist = abs(bestY - preferredSplitY)
                if (minCrossings > 0 || distToPref < currentBestDist) {
                    minCrossings = 0
                    bestY = testY
                }
            } else if (crossings < minCrossings) {
                minCrossings = crossings
                bestY = testY
            }
            testY += 4f
        }

        return bestY
    }

    private fun buildPagePlans(
        pages: List<List<Stroke>>,
        renderedPages: Map<Int, Bitmap>
    ): List<ExportPagePlan> {
        val plans = mutableListOf<ExportPagePlan>()

        pages.forEachIndexed { pageIndex, strokes ->
            val docBitmap = renderedPages[pageIndex]
            val remappedStrokes = strokes.map { remapStrokeForWhiteBackground(it) }

            if (docBitmap != null && !docBitmap.isRecycled) {
                plans.add(
                    ExportPagePlan.DocumentPage(
                        bitmap = docBitmap,
                        strokes = remappedStrokes,
                        originalPageIndex = pageIndex
                    )
                )
            } else {
                val bounds = computeContentBounds(remappedStrokes)

                if (bounds.isEmpty || remappedStrokes.isEmpty()) {
                    // Blank page
                    plans.add(
                        ExportPagePlan.WhiteboardPage(
                            strokes = emptyList(),
                            scale = 1f,
                            offsetX = 0f,
                            offsetY = 0f,
                            originalPageIndex = pageIndex,
                            sliceIndex = 0,
                            totalSlicesForBoardPage = 1
                        )
                    )
                } else {
                    // Determine horizontal scale to fit usable width
                    val fitWidthScale = (USABLE_WIDTH / bounds.width).coerceAtMost(1.0f).coerceAtLeast(0.15f)
                    val scaledHeightAtFitWidth = bounds.height * fitWidthScale

                    if (scaledHeightAtFitWidth <= USABLE_HEIGHT) {
                        // Entire whiteboard content fits on a SINGLE page cleanly
                        val fitScale = min(USABLE_WIDTH / bounds.width, USABLE_HEIGHT / bounds.height)
                            .coerceAtMost(1.0f).coerceAtLeast(0.15f)
                        val renderedW = bounds.width * fitScale
                        val renderedH = bounds.height * fitScale
                        val offsetX = MARGIN_X + (USABLE_WIDTH - renderedW) / 2f - bounds.minX * fitScale
                        val offsetY = MARGIN_TOP + (USABLE_HEIGHT - renderedH) / 2f - bounds.minY * fitScale

                        plans.add(
                            ExportPagePlan.WhiteboardPage(
                                strokes = remappedStrokes,
                                scale = fitScale,
                                offsetX = offsetX,
                                offsetY = offsetY,
                                originalPageIndex = pageIndex,
                                sliceIndex = 0,
                                totalSlicesForBoardPage = 1
                            )
                        )
                    } else {
                        // Content is taller than 1 page: automatically divide into sequential pages
                        val pageSliceHeightWorld = USABLE_HEIGHT / fitWidthScale
                        val overlapWorld = (60f / fitWidthScale).coerceAtMost(pageSliceHeightWorld * 0.25f)

                        val slices = mutableListOf<Pair<Float, Float>>()
                        var curTop = bounds.minY
                        while (curTop < bounds.maxY) {
                            val rawBottom = curTop + pageSliceHeightWorld
                            if (rawBottom >= bounds.maxY) {
                                slices.add(Pair(curTop, bounds.maxY))
                                break
                            }
                            val splitY = findBestSplitY(
                                strokes = remappedStrokes,
                                minY = curTop + 40f,
                                maxY = bounds.maxY,
                                preferredSplitY = rawBottom - overlapWorld
                            )
                            val sliceBottom = (splitY + overlapWorld).coerceAtMost(bounds.maxY)
                            slices.add(Pair(curTop, sliceBottom))

                            if (splitY <= curTop) {
                                curTop = curTop + (pageSliceHeightWorld - overlapWorld).coerceAtLeast(100f)
                            } else {
                                curTop = splitY
                            }
                        }

                        val totalSlices = slices.size
                        val renderedW = bounds.width * fitWidthScale
                        val offsetX = MARGIN_X + (USABLE_WIDTH - renderedW) / 2f - bounds.minX * fitWidthScale

                        slices.forEachIndexed { sIdx, (sliceTop, sliceBottom) ->
                            val offsetY = MARGIN_TOP - sliceTop * fitWidthScale
                            val sliceStrokes = remappedStrokes.filter { stroke ->
                                stroke.bounds.bottom >= sliceTop - 12f && stroke.bounds.top <= sliceBottom + 12f
                            }
                            plans.add(
                                ExportPagePlan.WhiteboardPage(
                                    strokes = sliceStrokes,
                                    scale = fitWidthScale,
                                    offsetX = offsetX,
                                    offsetY = offsetY,
                                    originalPageIndex = pageIndex,
                                    sliceIndex = sIdx,
                                    totalSlicesForBoardPage = totalSlices
                                )
                            )
                        }
                    }
                }
            }
        }

        return plans
    }

    private fun exportPdf(
        context: Context,
        lessonTitle: String,
        safeTitle: String,
        timestamp: Long,
        pages: List<List<Stroke>>,
        canvasBackground: CanvasBackground,
        showGrid: Boolean,
        renderedPages: Map<Int, Bitmap>,
        exportDir: File,
        onProgress: (Float, String) -> Unit
    ): Pair<File, Int> {
        val pdf = PdfDocument()
        val plans = buildPagePlans(pages, renderedPages)
        val totalPlans = plans.size.coerceAtLeast(1)

        try {
            plans.forEachIndexed { pdfIndex, plan ->
                onProgress(
                    (pdfIndex.toFloat() / totalPlans) * 0.9f,
                    "Rendering PDF page ${pdfIndex + 1} of $totalPlans..."
                )

                when (plan) {
                    is ExportPagePlan.DocumentPage -> {
                        val width = plan.bitmap.width
                        val height = plan.bitmap.height
                        val pageInfo = PdfDocument.PageInfo.Builder(width, height, pdfIndex + 1).create()
                        val page = pdf.startPage(pageInfo)
                        val canvas = page.canvas

                        // Always white background
                        canvas.drawColor(0xFFFFFFFF.toInt())
                        canvas.drawBitmap(plan.bitmap, 0f, 0f, null)

                        val scope = CanvasDrawScope()
                        val composeCanvas = ComposeCanvas(canvas)
                        scope.draw(
                            density = Density(1f),
                            layoutDirection = LayoutDirection.Ltr,
                            canvas = composeCanvas,
                            size = Size(width.toFloat(), height.toFloat())
                        ) {
                            plan.strokes.forEach { stroke ->
                                renderStroke(context, stroke, currentScale = 1f, isSelected = false)
                            }
                        }

                        // Footer
                        drawPdfFooter(canvas, width, height, lessonTitle, pdfIndex + 1, totalPlans)
                        pdf.finishPage(page)
                    }
                    is ExportPagePlan.WhiteboardPage -> {
                        val pageInfo = PdfDocument.PageInfo.Builder(PDF_PAGE_WIDTH, PDF_PAGE_HEIGHT, pdfIndex + 1).create()
                        val page = pdf.startPage(pageInfo)
                        val canvas = page.canvas

                        // Background fill: pure white or graph tint
                        val bgArgb = when (canvasBackground) {
                            CanvasBackground.GRAPH -> 0xFFF7FBFF.toInt()
                            else -> 0xFFFFFFFF.toInt()
                        }
                        canvas.drawColor(bgArgb)

                        // Clip to printable content boundaries so ink never bleeds into margins or footer
                        canvas.save()
                        canvas.clipRect(
                            MARGIN_X - 6f,
                            MARGIN_TOP - 6f,
                            PDF_PAGE_WIDTH - MARGIN_X + 6f,
                            PDF_PAGE_HEIGHT - MARGIN_BOTTOM + 6f
                        )

                        val scope = CanvasDrawScope()
                        val composeCanvas = ComposeCanvas(canvas)
                        scope.draw(
                            density = Density(1f),
                            layoutDirection = LayoutDirection.Ltr,
                            canvas = composeCanvas,
                            size = Size(PDF_PAGE_WIDTH.toFloat(), PDF_PAGE_HEIGHT.toFloat())
                        ) {
                            // Render graph / grid / notebook lines if enabled or graph background
                            if (canvasBackground == CanvasBackground.GRAPH ||
                                canvasBackground == CanvasBackground.GRID ||
                                canvasBackground == CanvasBackground.LINED ||
                                canvasBackground == CanvasBackground.DOTTED ||
                                canvasBackground == CanvasBackground.FOOTBALL ||
                                showGrid
                            ) {
                                drawPageGrid(
                                    background = if (canvasBackground == CanvasBackground.DARK) CanvasBackground.WHITE else canvasBackground,
                                    showGrid = showGrid,
                                    dark = false,
                                    scale = plan.scale,
                                    offsetX = plan.offsetX,
                                    offsetY = plan.offsetY
                                )
                            }

                            withTransform({
                                translate(plan.offsetX, plan.offsetY)
                                scale(plan.scale, plan.scale, pivot = Offset.Zero)
                            }) {
                                plan.strokes.forEach { stroke ->
                                    renderStroke(context, stroke, currentScale = plan.scale, isSelected = false)
                                }
                            }
                        }
                        canvas.restore()

                        // Footer with lesson title, divider, and page count
                        drawPdfFooter(canvas, PDF_PAGE_WIDTH, PDF_PAGE_HEIGHT, lessonTitle, pdfIndex + 1, totalPlans)
                        pdf.finishPage(page)
                    }
                }
            }

            val outputFile = File(exportDir, "${safeTitle}_${timestamp}.pdf")
            outputFile.outputStream().use { pdf.writeTo(it) }
            return Pair(outputFile, totalPlans)
        } finally {
            pdf.close()
        }
    }

    private fun drawPdfFooter(
        canvas: Canvas,
        pageWidth: Int,
        pageHeight: Int,
        lessonTitle: String,
        currentPage: Int,
        totalPages: Int
    ) {
        val footerDividerY = pageHeight - 64f
        val linePaint = Paint().apply {
            color = 0xFFE2E8F0.toInt()
            strokeWidth = 1.5f
            isAntiAlias = true
        }
        canvas.drawLine(MARGIN_X, footerDividerY, pageWidth - MARGIN_X, footerDividerY, linePaint)

        val footerTextPaint = Paint().apply {
            color = 0xFF64748B.toInt()
            textSize = 20f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val titleDisplay = lessonTitle.trim().ifEmpty { "Whiteboard Lesson" }
        canvas.drawText(titleDisplay, MARGIN_X, footerDividerY + 36f, footerTextPaint)

        val pageText = "Page $currentPage of $totalPages"
        val textWidth = footerTextPaint.measureText(pageText)
        canvas.drawText(pageText, pageWidth - MARGIN_X - textWidth, footerDividerY + 36f, footerTextPaint)
    }

    private fun exportDirectPng(
        context: Context,
        lessonTitle: String,
        safeTitle: String,
        timestamp: Long,
        pages: List<List<Stroke>>,
        activePage: Int,
        canvasBackground: CanvasBackground,
        isDarkCanvas: Boolean,
        showGrid: Boolean,
        renderedPages: Map<Int, Bitmap>,
        exportDir: File,
        onProgress: (Float, String) -> Unit
    ): Pair<File, Int> {
        onProgress(0.2f, "Preparing high-resolution PNG image...")

        val pageIndex = activePage.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
        val rawStrokes = if (pages.isNotEmpty()) pages[pageIndex] else emptyList()
        val docBitmap = renderedPages[pageIndex]

        val remappedStrokes = rawStrokes.map { remapStrokeForWhiteBackground(it) }
        val bounds = computeContentBounds(remappedStrokes)

        val width = if (docBitmap != null && !docBitmap.isRecycled) docBitmap.width else PDF_PAGE_WIDTH
        val height = if (docBitmap != null && !docBitmap.isRecycled) docBitmap.height else PDF_PAGE_HEIGHT

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background fill
        val bgArgb = when (canvasBackground) {
            CanvasBackground.GRAPH -> 0xFFF7FBFF.toInt()
            else -> 0xFFFFFFFF.toInt()
        }
        canvas.drawColor(bgArgb)

        onProgress(0.5f, "Rendering content, graphs, and diagrams...")

        if (docBitmap != null && !docBitmap.isRecycled) {
            canvas.drawBitmap(docBitmap, 0f, 0f, null)
            val scope = CanvasDrawScope()
            val composeCanvas = ComposeCanvas(canvas)
            scope.draw(
                density = Density(1f),
                layoutDirection = LayoutDirection.Ltr,
                canvas = composeCanvas,
                size = Size(width.toFloat(), height.toFloat())
            ) {
                remappedStrokes.forEach { stroke ->
                    renderStroke(context, stroke, currentScale = 1f, isSelected = false)
                }
            }
        } else {
            val fitScale: Float
            val offsetX: Float
            val offsetY: Float

            if (bounds.isEmpty || remappedStrokes.isEmpty()) {
                fitScale = 1f
                offsetX = 0f
                offsetY = 0f
            } else {
                fitScale = min(USABLE_WIDTH / bounds.width, USABLE_HEIGHT / bounds.height)
                    .coerceAtMost(1.0f).coerceAtLeast(0.15f)
                val renderedW = bounds.width * fitScale
                val renderedH = bounds.height * fitScale
                offsetX = MARGIN_X + (USABLE_WIDTH - renderedW) / 2f - bounds.minX * fitScale
                offsetY = MARGIN_TOP + (USABLE_HEIGHT - renderedH) / 2f - bounds.minY * fitScale
            }

            val scope = CanvasDrawScope()
            val composeCanvas = ComposeCanvas(canvas)
            scope.draw(
                density = Density(1f),
                layoutDirection = LayoutDirection.Ltr,
                canvas = composeCanvas,
                size = Size(width.toFloat(), height.toFloat())
            ) {
                // Render graph / grid / notebook lines if enabled or graph background
                if (canvasBackground == CanvasBackground.GRAPH ||
                    canvasBackground == CanvasBackground.GRID ||
                    canvasBackground == CanvasBackground.LINED ||
                    canvasBackground == CanvasBackground.DOTTED ||
                    canvasBackground == CanvasBackground.FOOTBALL ||
                    showGrid
                ) {
                    drawPageGrid(
                        background = if (canvasBackground == CanvasBackground.DARK) CanvasBackground.WHITE else canvasBackground,
                        showGrid = showGrid,
                        dark = false,
                        scale = fitScale,
                        offsetX = offsetX,
                        offsetY = offsetY
                    )
                }

                // Render vector ink, shapes, text, equations, sticky notes, and placed images/diagrams!
                withTransform({
                    translate(offsetX, offsetY)
                    scale(fitScale, fitScale, pivot = Offset.Zero)
                }) {
                    remappedStrokes.forEach { stroke ->
                        renderStroke(context, stroke, currentScale = fitScale, isSelected = false)
                    }
                }
            }
        }

        onProgress(0.85f, "Saving .png file...")

        // Direct .png file export
        val pngFile = File(exportDir, "${safeTitle}_${timestamp}.png")
        BufferedOutputStream(FileOutputStream(pngFile)).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bitmap.recycle()

        return Pair(pngFile, 1)
    }

    @Deprecated("Use exportDirectPng instead", ReplaceWith("exportDirectPng"))
    private fun exportImageZip(
        context: Context,
        lessonTitle: String,
        safeTitle: String,
        timestamp: Long,
        pages: List<List<Stroke>>,
        renderedPages: Map<Int, Bitmap>,
        exportDir: File,
        onProgress: (Float, String) -> Unit
    ): Pair<File, Int> = exportDirectPng(
        context = context,
        lessonTitle = lessonTitle,
        safeTitle = safeTitle,
        timestamp = timestamp,
        pages = pages,
        activePage = 0,
        canvasBackground = CanvasBackground.WHITE,
        isDarkCanvas = false,
        showGrid = false,
        renderedPages = renderedPages,
        exportDir = exportDir,
        onProgress = onProgress
    )

    private fun exportIwb(
        safeTitle: String,
        timestamp: Long,
        lessonTitle: String,
        pages: List<List<Stroke>>,
        canvasBackground: CanvasBackground,
        isDarkCanvas: Boolean,
        showGrid: Boolean,
        scale: Float,
        offsetX: Float,
        offsetY: Float,
        activePage: Int,
        document: BoardDocument?,
        splitCount: Int,
        splitWeights: List<Float>,
        splitPanes: List<CanvasPane>?,
        exportDir: File,
        onProgress: (Float, String) -> Unit
    ): File {
        onProgress(0.5f, "Serializing GYOASH IWB layers...")

        val iwbPages = pages.mapIndexed { index, strokes ->
            GyoashIwbPage(
                pageIndex = index,
                layers = listOf(
                    GyoashIwbLayer(
                        id = "layer_ink_$index",
                        name = "Ink & Content",
                        visible = true,
                        opacity = 1.0f,
                        strokes = strokes.map { it.toDto() }
                    )
                )
            )
        }

        val iwbPackage = GyoashIwbPackage(
            format = "GYOASH_IWB_V1",
            version = 1,
            app = "GYOASH Whiteboard",
            exportedAt = timestamp,
            title = lessonTitle,
            metadata = GyoashIwbMetadata(
                pageCount = pages.size,
                activePage = activePage,
                canvasBackground = canvasBackground.key,
                isDarkCanvas = isDarkCanvas,
                showGrid = showGrid,
                scale = scale,
                offsetX = offsetX,
                offsetY = offsetY
            ),
            pages = iwbPages,
            documentPath = document?.path,
            documentName = document?.name,
            documentKind = document?.kind?.name,
            splitCount = splitCount.takeIf { it > 1 },
            splitWeights = splitWeights.takeIf { splitCount > 1 },
            splitPanes = if (splitCount > 1) splitPanes?.map { pane ->
                GyoashIwbPane(
                    kind = pane.kind.name,
                    strokes = pane.strokes.map { it.toDto() },
                    source = pane.source,
                    label = pane.label,
                    pageIndex = pane.pageIndex,
                    pageCount = pane.pageCount,
                    tool = pane.tool.name
                )
            } else null
        )

        val iwbFile = File(exportDir, "${safeTitle}_${timestamp}.iwb")
        val json = gson.toJson(iwbPackage)
        iwbFile.writeText(json)
        return iwbFile
    }

    private fun dispatchFile(
        context: Context,
        file: File,
        format: ExportFormat,
        destination: ExportDestination
    ): Uri {
        val authority = "${context.packageName}.fileprovider"
        var contentUri = FileProvider.getUriForFile(context, authority, file)

        // Save to public storage if LOCAL destination
        if (destination == ExportDestination.LOCAL) {
            val publicUri = saveToPublicStorage(context, file, format)
            if (publicUri != null) {
                contentUri = publicUri
            }
        }

        return contentUri
    }

    private fun saveToPublicStorage(
        context: Context,
        file: File,
        format: ExportFormat
    ): Uri? {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, file.name)
                    put(MediaStore.Downloads.MIME_TYPE, format.mimeType)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/GYOASH")
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        file.inputStream().use { input -> input.copyTo(output) }
                    }
                    return uri
                }
            } else {
                val publicDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "GYOASH"
                ).apply { mkdirs() }
                val destFile = File(publicDir, file.name)
                file.copyTo(destFile, overwrite = true)
                return Uri.fromFile(destFile)
            }
        } catch (_: Exception) {
            // Fallback to internal/cache directory which already succeeded
        }
        return null
    }

    fun importIwb(context: Context, uri: Uri): GyoashIwbPackage {
        val json = context.contentResolver.openInputStream(uri)?.use { it.reader().readText() }
            ?: throw IllegalArgumentException("Could not read IWB file")
        return gson.fromJson(json, GyoashIwbPackage::class.java)
            ?: throw IllegalArgumentException("Invalid GYOASH IWB file format")
    }

    fun launchShareIntent(
        context: Context,
        result: ExportResult,
        lessonTitle: String
    ) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = result.format.mimeType
            putExtra(Intent.EXTRA_STREAM, result.uri)
            putExtra(Intent.EXTRA_SUBJECT, "GYOASH Lesson: $lessonTitle")
            putExtra(
                Intent.EXTRA_TEXT,
                "Exported ${result.format.displayName} (${result.pageCount} pages) from GYOASH Whiteboard."
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val targetPkg = result.destination.targetPackage
        if (targetPkg != null && isAppInstalled(context, targetPkg)) {
            shareIntent.setPackage(targetPkg)
            try {
                shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(shareIntent)
                return
            } catch (_: Exception) {
                // Fallback to chooser below
                shareIntent.setPackage(null)
            }
        }

        val chooser = Intent.createChooser(
            shareIntent,
            "Save / Share to ${result.destination.displayName}"
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun openExportedFile(context: Context, result: ExportResult) {
        try {
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(result.uri, result.format.mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(viewIntent)
        } catch (_: Exception) {
            // Fallback to general chooser
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(result.uri, "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(viewIntent, "Open with").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }

    private fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes.toFloat() / (1024 * 1024))
            bytes >= 1024 -> String.format("%.1f KB", bytes.toFloat() / 1024)
            else -> "$bytes B"
        }
    }
}
