package com.avitrisphare.whiteboard.presentation.canvas

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas as ComposeCanvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.avitrisphare.whiteboard.domain.model.CanvasBackground
import com.avitrisphare.whiteboard.domain.model.Stroke
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

enum class SnipMode {
    RECTANGLE,
    SQUARE,
    FREEFORM,
    FULL_CANVAS
}

data class SnipResult(
    val filePath: String,
    val widthPx: Int,
    val heightPx: Int,
    val worldWidth: Float,
    val worldHeight: Float,
    val originalStrokeId: String = ""
)

object ImageSnipEngine {

    /**
     * Decodes source bitmap preserving resolution up to 4096px.
     */
    fun loadSourceBitmap(context: Context, uriString: String, maxDim: Int = 4096): Bitmap? {
        if (uriString.isBlank()) return null
        return try {
            val openStream: () -> java.io.InputStream? = {
                when {
                    uriString.startsWith("/") -> File(uriString).inputStream()
                    uriString.startsWith("file://") -> File(Uri.parse(uriString).path ?: uriString).inputStream()
                    uriString.startsWith("content://") -> context.contentResolver.openInputStream(Uri.parse(uriString))
                    else -> {
                        val file = File(uriString)
                        if (file.exists()) file.inputStream()
                        else context.contentResolver.openInputStream(Uri.parse(uriString))
                    }
                }
            }

            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            openStream()?.use { BitmapFactory.decodeStream(it, null, boundsOpts) }
            if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) return null

            var sample = 1
            while (boundsOpts.outWidth / sample > maxDim || boundsOpts.outHeight / sample > maxDim) {
                sample *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inMutable = false
            }
            openStream()?.use { BitmapFactory.decodeStream(it, null, decodeOpts) }
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Captures a universal canvas region containing any mix of:
     * - Background (custom color, dark, white, graph, etc.)
     * - Document page (PDF / PPT)
     * - Vector strokes (pen, pencil, highlighter, marker, brush)
     * - Shapes (rectangle, circle, diamond, arrow, 3D solids, etc.)
     * - Placed images
     * - Typed text and text boxes
     * - Sticky notes, equations, flowchart connectors
     *
     * In Freeform mode: pixels outside the freeform contour are 100% transparent.
     */
    fun captureUniversalCanvas(
        context: Context,
        cropRect: Rect,
        freeformPoints: List<Offset>?,
        mode: SnipMode,
        strokes: List<Stroke>,
        background: CanvasBackground,
        customBackgroundColor: Int?,
        customBackgroundBitmap: Bitmap? = null,
        documentBitmap: Bitmap? = null,
        documentFrame: Triple<Float, Float, Float>? = null,
        showGrid: Boolean = false,
        dark: Boolean = false,
        density: Density
    ): SnipResult? {
        val worldW = cropRect.width.coerceAtLeast(1f)
        val worldH = cropRect.height.coerceAtLeast(1f)
        if (worldW < 8f || worldH < 8f) return null

        // Render resolution: balance quality and memory (capped at 3840px max dimension)
        val maxDim = 3840f
        val renderScale = minOf(1.5f, maxDim / maxOf(worldW, worldH)).coerceAtLeast(0.5f)
        val bmpW = (worldW * renderScale).roundToInt().coerceIn(1, 4096)
        val bmpH = (worldH * renderScale).roundToInt().coerceIn(1, 4096)

        var resultBitmap: Bitmap? = null
        try {
            resultBitmap = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
            val androidCanvas = AndroidCanvas(resultBitmap)

            val composeCanvas = ComposeCanvas(androidCanvas)
            val drawScope = CanvasDrawScope()
            val scopeSize = Size(bmpW.toFloat(), bmpH.toFloat())

            drawScope.draw(density, LayoutDirection.Ltr, composeCanvas, scopeSize) {
                // 1. Draw live canvas background inside the cropped region (dots, grid, lines, colors)
                drawPageGrid(
                    background = background,
                    showGrid = showGrid,
                    dark = dark,
                    scale = renderScale,
                    offsetX = -cropRect.left * renderScale,
                    offsetY = -cropRect.top * renderScale,
                    customBackgroundColor = customBackgroundColor
                )

                // Custom background image if set
                if (background == CanvasBackground.CUSTOM && customBackgroundBitmap != null && !customBackgroundBitmap.isRecycled) {
                    val bgImg = customBackgroundBitmap.asImageBitmap()
                    drawImage(
                        image = bgImg,
                        srcOffset = IntOffset.Zero,
                        srcSize = IntSize(bgImg.width, bgImg.height),
                        dstOffset = IntOffset.Zero,
                        dstSize = IntSize(bmpW, bmpH)
                    )
                }

                // 2. Draw transformed content (world coordinate space)
                withTransform({
                    scale(renderScale, renderScale, Offset.Zero)
                    translate(-cropRect.left, -cropRect.top)
                }) {
                    // Document Page (PDF / PPT) if present
                    if (documentBitmap != null && !documentBitmap.isRecycled) {
                        val docX = documentFrame?.first ?: 0f
                        val docY = documentFrame?.second ?: 0f
                        val docScale = documentFrame?.third ?: 1f
                        withTransform({
                            translate(docX, docY)
                            scale(docScale, docScale, Offset.Zero)
                        }) {
                            drawImage(
                                image = documentBitmap.asImageBitmap(),
                                topLeft = Offset.Zero
                            )
                        }
                    }

                    // Render all visible strokes intersecting the cropRect
                    val pad = 16f
                    for (stroke in strokes) {
                        val b = stroke.bounds
                        val strokePad = stroke.strokeWidth.coerceAtLeast(pad)
                        val intersects = b.right >= cropRect.left - strokePad &&
                                b.left <= cropRect.right + strokePad &&
                                b.bottom >= cropRect.top - strokePad &&
                                b.top <= cropRect.bottom + strokePad
                        if (intersects) {
                            renderStroke(context, stroke, currentScale = 1f, isSelected = false)
                        }
                    }
                }
            }

            // In Freeform mode: smoothly mask out all pixels outside the freeform contour
            if (mode == SnipMode.FREEFORM && freeformPoints != null && freeformPoints.size >= 3) {
                val maskPath = createSmoothedAndroidPath(freeformPoints, cropRect.left, cropRect.top, renderScale)

                var maskBitmap: Bitmap? = null
                try {
                    maskBitmap = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
                    val maskCanvas = AndroidCanvas(maskBitmap)
                    val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.BLACK
                        style = Paint.Style.FILL
                    }
                    maskCanvas.drawPath(maskPath, maskPaint)

                    val blendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                    }
                    androidCanvas.drawBitmap(maskBitmap, 0f, 0f, blendPaint)
                } finally {
                    maskBitmap?.recycle()
                }
            }

            // Save the flattened bitmap to disk
            val mediaDir = File(context.filesDir, "media")
            if (!mediaDir.exists()) mediaDir.mkdirs()
            val snipFile = File(mediaDir, "snip_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(6)}.png")
            FileOutputStream(snipFile).use { out ->
                resultBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            BitmapCache.put(snipFile.absolutePath, resultBitmap)

            return SnipResult(
                filePath = snipFile.absolutePath,
                widthPx = bmpW,
                heightPx = bmpH,
                worldWidth = worldW,
                worldHeight = worldH,
                originalStrokeId = ""
            )
        } catch (_: Throwable) {
            try {
                if (resultBitmap != null && !resultBitmap.isRecycled) {
                    resultBitmap.recycle()
                }
            } catch (_: Throwable) {}
            return null
        }
    }

    /**
     * Constructs a continuous, smooth closed AndroidPath using quadratic Bezier curves (quadTo).
     */
    fun createSmoothedAndroidPath(
        points: List<Offset>,
        cropLeft: Float,
        cropTop: Float,
        scale: Float
    ): AndroidPath {
        val path = AndroidPath()
        if (points.isEmpty()) return path
        val first = points.first()
        val startX = (first.x - cropLeft) * scale
        val startY = (first.y - cropTop) * scale
        path.moveTo(startX, startY)
        if (points.size == 1) return path
        if (points.size == 2) {
            val p1 = points[1]
            path.lineTo((p1.x - cropLeft) * scale, (p1.y - cropTop) * scale)
            path.close()
            return path
        }

        for (i in 1 until points.size - 1) {
            val p0 = points[i]
            val p1 = points[i + 1]
            val midX = ((p0.x + p1.x) / 2f - cropLeft) * scale
            val midY = ((p0.y + p1.y) / 2f - cropTop) * scale
            val ctrlX = (p0.x - cropLeft) * scale
            val ctrlY = (p0.y - cropTop) * scale
            path.quadTo(ctrlX, ctrlY, midX, midY)
        }

        val last = points.last()
        val closeMidX = ((last.x + first.x) / 2f - cropLeft) * scale
        val closeMidY = ((last.y + first.y) / 2f - cropTop) * scale
        path.quadTo(
            (last.x - cropLeft) * scale,
            (last.y - cropTop) * scale,
            closeMidX,
            closeMidY
        )
        path.lineTo(startX, startY)
        path.close()
        return path
    }
}
