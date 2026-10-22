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
import com.avitrisphare.whiteboard.domain.model.StrokeType
import com.avitrisphare.whiteboard.domain.model.isPlacedMedia
import com.avitrisphare.whiteboard.presentation.canvas.BoardDocument
import com.avitrisphare.whiteboard.presentation.canvas.CanvasPane
import com.avitrisphare.whiteboard.presentation.canvas.drawPageGrid
import com.avitrisphare.whiteboard.presentation.canvas.renderStroke
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
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
        onProgress(0.2f, "Serializing GYOASH Whiteboard layers...")

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

        onProgress(0.5f, "Generating universal CFF-IWB & GYOASH package...")

        val iwbFile = File(exportDir, "${safeTitle}_${timestamp}.iwb")
        val json = gson.toJson(iwbPackage)
        val contentXml = buildCffXml(lessonTitle, pages, canvasBackground, isDarkCanvas)

        ZipOutputStream(BufferedOutputStream(FileOutputStream(iwbFile))).use { zos ->
            // 1. mimetype (CFF standard)
            val mimeEntry = ZipEntry("mimetype")
            zos.putNextEntry(mimeEntry)
            zos.write("application/vnd.common-file-format".toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 2. content.xml (Universal Interactive Whiteboard Common File Format / SVG for any whiteboard)
            val xmlEntry = ZipEntry("content.xml")
            zos.putNextEntry(xmlEntry)
            zos.write(contentXml.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 3. gyoash_lesson.json (High-fidelity GYOASH Whiteboard layer data)
            val jsonEntry = ZipEntry("gyoash_lesson.json")
            zos.putNextEntry(jsonEntry)
            zos.write(json.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }

        onProgress(0.9f, "Finalizing IWB package...")
        return iwbFile
    }

    fun buildCffXml(
        lessonTitle: String,
        pages: List<List<Stroke>>,
        canvasBackground: CanvasBackground,
        isDarkCanvas: Boolean
    ): String {
        val sb = java.lang.StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<iwb xmlns=\"http://www.imsglobal.org/xsd/iwbcff_v1p0\" xmlns:svg=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\">\n")
        sb.append("  <head>\n")
        sb.append("    <meta name=\"creator\" content=\"GYOASH Whiteboard\" />\n")
        sb.append("    <meta name=\"title\" content=\"${escapeXml(lessonTitle)}\" />\n")
        sb.append("  </head>\n")
        sb.append("  <pages>\n")

        val bgColor = if (isDarkCanvas) "#171717" else when (canvasBackground) {
            CanvasBackground.DARK -> "#171717"
            CanvasBackground.WHITE, CanvasBackground.GRID, CanvasBackground.GRAPH, CanvasBackground.LINED, CanvasBackground.DOTTED -> "#FFFFFF"
            else -> "#FFFFFF"
        }

        pages.forEachIndexed { pageIndex, strokes ->
            sb.append("    <page id=\"page_$pageIndex\">\n")
            sb.append("      <svg:svg width=\"1920\" height=\"1080\" viewBox=\"0 0 1920 1080\">\n")
            // Background rect
            sb.append("        <svg:rect width=\"100%\" height=\"100%\" fill=\"$bgColor\" />\n")
            sb.append("        <svg:g id=\"layer_ink_$pageIndex\">\n")

            for (stroke in strokes) {
                val hexColor = colorToHex(stroke.colorValue)
                val width = stroke.strokeWidth.coerceAtLeast(1f)

                when (stroke.strokeType) {
                    StrokeType.TEXT, StrokeType.EQUATION, StrokeType.STICKY_NOTE -> {
                        val origin = stroke.points.firstOrNull() ?: Offset.Zero
                        val textContent = escapeXml(stroke.text.ifBlank { if (stroke.strokeType == StrokeType.STICKY_NOTE) "Note" else "" })
                        if (textContent.isNotBlank()) {
                            if (stroke.strokeType == StrokeType.STICKY_NOTE) {
                                val stickyFill = if (stroke.fillColor != 0) colorToHex(stroke.fillColor) else "#FFF9C4"
                                sb.append("          <svg:rect x=\"${origin.x}\" y=\"${origin.y}\" width=\"220\" height=\"180\" rx=\"12\" ry=\"12\" fill=\"$stickyFill\" stroke=\"#22000000\" stroke-width=\"1.5\" />\n")
                                sb.append("          <svg:text x=\"${origin.x + 16}\" y=\"${origin.y + 36}\" fill=\"#171717\" font-size=\"16\" font-family=\"sans-serif\">$textContent</svg:text>\n")
                            } else {
                                sb.append("          <svg:text x=\"${origin.x}\" y=\"${origin.y}\" fill=\"$hexColor\" font-size=\"${width.coerceAtLeast(16f)}\" font-family=\"sans-serif\">$textContent</svg:text>\n")
                            }
                        }
                    }
                    StrokeType.RECTANGLE, StrokeType.FRAME -> {
                        val b = stroke.bounds
                        val fill = if (stroke.fillColor != 0) colorToHex(stroke.fillColor) else "none"
                        sb.append("          <svg:rect x=\"${b.left}\" y=\"${b.top}\" width=\"${b.width}\" height=\"${b.height}\" stroke=\"$hexColor\" stroke-width=\"$width\" fill=\"$fill\" />\n")
                    }
                    StrokeType.CIRCLE -> {
                        val b = stroke.bounds
                        val cx = b.left + b.width / 2f
                        val cy = b.top + b.height / 2f
                        val rx = b.width / 2f
                        val ry = b.height / 2f
                        val fill = if (stroke.fillColor != 0) colorToHex(stroke.fillColor) else "none"
                        sb.append("          <svg:ellipse cx=\"$cx\" cy=\"$cy\" rx=\"$rx\" ry=\"$ry\" stroke=\"$hexColor\" stroke-width=\"$width\" fill=\"$fill\" />\n")
                    }
                    StrokeType.LINE, StrokeType.ARROW -> {
                        if (stroke.points.size >= 2) {
                            val p1 = stroke.points.first()
                            val p2 = stroke.points.last()
                            sb.append("          <svg:line x1=\"${p1.x}\" y1=\"${p1.y}\" x2=\"${p2.x}\" y2=\"${p2.y}\" stroke=\"$hexColor\" stroke-width=\"$width\" stroke-linecap=\"round\" />\n")
                        }
                    }
                    else -> {
                        // Standard ink strokes (PEN, PENCIL, MARKER, HIGHLIGHTER, BRUSH)
                        if (stroke.points.isNotEmpty()) {
                            val pathD = strokePointsToSvgPath(stroke.points)
                            val opacity = if (stroke.isHighlighter || stroke.strokeType == StrokeType.HIGHLIGHTER) "0.4" else "1.0"
                            sb.append("          <svg:path d=\"$pathD\" stroke=\"$hexColor\" stroke-width=\"$width\" fill=\"none\" stroke-linecap=\"round\" stroke-linejoin=\"round\" opacity=\"$opacity\" />\n")
                        }
                    }
                }
            }

            sb.append("        </svg:g>\n")
            sb.append("      </svg:svg>\n")
            sb.append("    </page>\n")
        }

        sb.append("  </pages>\n")
        sb.append("</iwb>\n")
        return sb.toString()
    }

    private fun colorToHex(colorValue: Int): String {
        val r = (colorValue shr 16) and 0xFF
        val g = (colorValue shr 8) and 0xFF
        val b = colorValue and 0xFF
        return "#%02X%02X%02X".format(Locale.US, r, g, b)
    }

    private fun escapeXml(str: String): String {
        return str
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun strokePointsToSvgPath(points: List<Offset>): String {
        if (points.isEmpty()) return ""
        val sb = java.lang.StringBuilder()
        sb.append("M ").append(roundFloat(points[0].x)).append(" ").append(roundFloat(points[0].y))
        for (i in 1 until points.size) {
            sb.append(" L ").append(roundFloat(points[i].x)).append(" ").append(roundFloat(points[i].y))
        }
        return sb.toString()
    }

    private fun roundFloat(f: Float): String {
        return if (f == f.toInt().toFloat()) f.toInt().toString() else "%.1f".format(Locale.US, f)
    }

    private fun parseSvgColor(colorStr: String?, defaultColor: Int = 0xFFFFFFFF.toInt()): Int {
        if (colorStr.isNullOrBlank() || colorStr == "none") return defaultColor
        val s = colorStr.trim()
        if (s.startsWith("#")) {
            val hex = s.substring(1)
            return when (hex.length) {
                3 -> {
                    val r = hex[0].toString().repeat(2).toInt(16)
                    val g = hex[1].toString().repeat(2).toInt(16)
                    val b = hex[2].toString().repeat(2).toInt(16)
                    (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                }
                6 -> (0xFFL shl 24 or hex.toLong(16)).toInt()
                8 -> hex.toLong(16).toInt()
                else -> defaultColor
            }
        }
        if (s.startsWith("rgb", ignoreCase = true)) {
            val nums = Regex("""\d+""").findAll(s).map { it.value.toInt() }.toList()
            if (nums.size >= 3) {
                return (0xFF shl 24) or ((nums[0] and 0xFF) shl 16) or ((nums[1] and 0xFF) shl 8) or (nums[2] and 0xFF)
            }
        }
        return when (s.lowercase(Locale.US)) {
            "black" -> 0xFF000000.toInt()
            "white" -> 0xFFFFFFFF.toInt()
            "red" -> 0xFFFF0000.toInt()
            "green" -> 0xFF00FF00.toInt()
            "blue" -> 0xFF0000FF.toInt()
            "yellow" -> 0xFFFFFF00.toInt()
            "cyan" -> 0xFF00FFFF.toInt()
            "magenta" -> 0xFFFF00FF.toInt()
            "gray", "grey" -> 0xFF808080.toInt()
            else -> defaultColor
        }
    }

    private fun parseSvgPathData(d: String): String {
        val numberRegex = Regex("""[-+]?[0-9]*\.?[0-9]+(?:[eE][-+]?[0-9]+)?""")
        val matches = numberRegex.findAll(d).mapNotNull { it.value.toFloatOrNull() }.toList()
        if (matches.size < 2) return ""
        val sb = java.lang.StringBuilder()
        for (i in 0 until matches.size - 1 step 2) {
            if (sb.isNotEmpty()) sb.append(";")
            sb.append(roundFloat(matches[i])).append(",").append(roundFloat(matches[i + 1]))
        }
        return sb.toString()
    }

    fun parseCffXml(xml: String): GyoashIwbPackage {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            isValidating = false
            try {
                setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            } catch (_: Exception) {}
        }
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))

        var title = "Imported Whiteboard Lesson"
        val metaNodes = doc.getElementsByTagName("meta")
        for (i in 0 until metaNodes.length) {
            val el = metaNodes.item(i) as? Element ?: continue
            if (el.getAttribute("name") == "title") {
                val t = el.getAttribute("content")
                if (t.isNotBlank()) title = t
            }
        }

        val pageNodes = doc.getElementsByTagName("page")
        val pagesList = mutableListOf<GyoashIwbPage>()

        fun extractStrokes(container: Element): List<StrokeDto> {
            val strokes = mutableListOf<StrokeDto>()

            // Extract <path> elements
            val pathNodes = container.getElementsByTagNameNS("*", "path")
            for (i in 0 until pathNodes.length) {
                val el = pathNodes.item(i) as? Element ?: continue
                val d = el.getAttribute("d")
                val points = parseSvgPathData(d)
                if (points.isNotBlank()) {
                    val color = parseSvgColor(el.getAttribute("stroke"), 0xFFFFFFFF.toInt())
                    val width = el.getAttribute("stroke-width").toFloatOrNull() ?: 4f
                    val opacity = el.getAttribute("opacity").toFloatOrNull() ?: 1.0f
                    val isHighlighter = opacity < 0.6f
                    strokes.add(
                        StrokeDto(
                            id = "stroke_${strokes.size}",
                            points = points,
                            colorValue = color,
                            strokeWidth = width,
                            isHighlighter = isHighlighter,
                            strokeType = if (isHighlighter) StrokeType.HIGHLIGHTER else StrokeType.PEN
                        )
                    )
                }
            }

            // Extract <rect> elements
            val rectNodes = container.getElementsByTagNameNS("*", "rect")
            for (i in 0 until rectNodes.length) {
                val el = rectNodes.item(i) as? Element ?: continue
                val widthAttr = el.getAttribute("width")
                if (widthAttr.contains("%")) continue // Skip 100% background rect

                val x = el.getAttribute("x").toFloatOrNull() ?: 0f
                val y = el.getAttribute("y").toFloatOrNull() ?: 0f
                val w = widthAttr.toFloatOrNull() ?: 0f
                val h = el.getAttribute("height").toFloatOrNull() ?: 0f

                if (w > 0f && h > 0f && !(x == 0f && y == 0f && w >= 1900f && h >= 1000f)) {
                    val color = parseSvgColor(el.getAttribute("stroke"), 0xFFFFFFFF.toInt())
                    val fill = parseSvgColor(el.getAttribute("fill"), 0)
                    val width = el.getAttribute("stroke-width").toFloatOrNull() ?: 3f
                    val pts = "${roundFloat(x)},${roundFloat(y)};${roundFloat(x + w)},${roundFloat(y)};${roundFloat(x + w)},${roundFloat(y + h)};${roundFloat(x)},${roundFloat(y + h)};${roundFloat(x)},${roundFloat(y)}"
                    strokes.add(
                        StrokeDto(
                            id = "rect_${strokes.size}",
                            points = pts,
                            colorValue = color,
                            strokeWidth = width,
                            fillColor = fill,
                            strokeType = StrokeType.RECTANGLE
                        )
                    )
                }
            }

            // Extract <line> elements
            val lineNodes = container.getElementsByTagNameNS("*", "line")
            for (i in 0 until lineNodes.length) {
                val el = lineNodes.item(i) as? Element ?: continue
                val x1 = el.getAttribute("x1").toFloatOrNull() ?: 0f
                val y1 = el.getAttribute("y1").toFloatOrNull() ?: 0f
                val x2 = el.getAttribute("x2").toFloatOrNull() ?: 0f
                val y2 = el.getAttribute("y2").toFloatOrNull() ?: 0f
                val color = parseSvgColor(el.getAttribute("stroke"), 0xFFFFFFFF.toInt())
                val width = el.getAttribute("stroke-width").toFloatOrNull() ?: 4f
                strokes.add(
                    StrokeDto(
                        id = "line_${strokes.size}",
                        points = "${roundFloat(x1)},${roundFloat(y1)};${roundFloat(x2)},${roundFloat(y2)}",
                        colorValue = color,
                        strokeWidth = width,
                        strokeType = StrokeType.LINE
                    )
                )
            }

            // Extract <text> elements
            val textNodes = container.getElementsByTagNameNS("*", "text")
            for (i in 0 until textNodes.length) {
                val el = textNodes.item(i) as? Element ?: continue
                val x = el.getAttribute("x").toFloatOrNull() ?: 0f
                val y = el.getAttribute("y").toFloatOrNull() ?: 0f
                val text = el.textContent?.trim() ?: ""
                if (text.isNotBlank()) {
                    val color = parseSvgColor(el.getAttribute("fill"), 0xFFFFFFFF.toInt())
                    val fontSize = el.getAttribute("font-size").toFloatOrNull() ?: 20f
                    strokes.add(
                        StrokeDto(
                            id = "text_${strokes.size}",
                            points = "${roundFloat(x)},${roundFloat(y)}",
                            text = text,
                            colorValue = color,
                            strokeWidth = fontSize,
                            strokeType = StrokeType.TEXT
                        )
                    )
                }
            }

            return strokes
        }

        if (pageNodes.length == 0) {
            val svgNodes = doc.getElementsByTagNameNS("*", "svg")
            val container = if (svgNodes.length > 0) svgNodes.item(0) as Element else doc.documentElement
            val strokes = extractStrokes(container)
            pagesList.add(GyoashIwbPage(0, listOf(GyoashIwbLayer("layer_0", "Ink & Content", true, 1f, strokes))))
        } else {
            for (p in 0 until pageNodes.length) {
                val pageEl = pageNodes.item(p) as Element
                val strokes = extractStrokes(pageEl)
                pagesList.add(GyoashIwbPage(p, listOf(GyoashIwbLayer("layer_$p", "Ink & Content", true, 1f, strokes))))
            }
        }

        if (pagesList.isEmpty()) {
            pagesList.add(GyoashIwbPage(0, listOf(GyoashIwbLayer("layer_0", "Ink & Content", true, 1f, emptyList()))))
        }

        return GyoashIwbPackage(
            format = "GYOASH_IWB_V1",
            version = 1,
            app = "CFF-IWB Importer",
            title = title,
            metadata = GyoashIwbMetadata(
                pageCount = pagesList.size,
                activePage = 0,
                canvasBackground = "dark",
                isDarkCanvas = true,
                showGrid = false
            ),
            pages = pagesList
        )
    }

    fun parseIwbBytes(bytes: ByteArray): GyoashIwbPackage {
        // Check if ZIP container signature (PK\x03\x04: 0x50, 0x4B, 0x03, 0x04)
        if (bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() && bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte()) {
            val entries = mutableMapOf<String, ByteArray>()
            ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val name = entry.name.replace('\\', '/')
                        entries[name] = zis.readBytes()
                    }
                    entry = zis.nextEntry
                }
            }

            // Priority 1: High-fidelity GYOASH proprietary package with all layers intact
            val gyoashJsonBytes = entries["gyoash_lesson.json"]
                ?: entries.entries.find { it.key.endsWith(".json") }?.value
            if (gyoashJsonBytes != null) {
                val json = String(gyoashJsonBytes, Charsets.UTF_8)
                val pkg = runCatching { gson.fromJson(json, GyoashIwbPackage::class.java) }.getOrNull()
                if (pkg != null && pkg.pages.isNotEmpty()) {
                    return pkg
                }
            }

            // Priority 2: Standard CFF-IWB content.xml (from SMART, Promethean, OpenBoard, BenQ, etc.)
            val contentXmlBytes = entries["content.xml"]
                ?: entries.entries.find { it.key.endsWith("content.xml") }?.value
            if (contentXmlBytes != null) {
                val xml = String(contentXmlBytes, Charsets.UTF_8)
                return parseCffXml(xml)
            }

            throw IllegalArgumentException("IWB archive contains neither gyoash_lesson.json nor content.xml")
        }

        // Priority 3: Plain JSON text (backward compatibility with legacy .iwb exports)
        val text = String(bytes, Charsets.UTF_8).trim()
        if (text.startsWith("{")) {
            return gson.fromJson(text, GyoashIwbPackage::class.java)
                ?: throw IllegalArgumentException("Invalid GYOASH IWB JSON format")
        }

        throw IllegalArgumentException("Unrecognized IWB file format")
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
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("Could not read IWB file")
        return parseIwbBytes(bytes)
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
