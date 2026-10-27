package com.avitrisphare.whiteboard.presentation.canvas

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import java.util.Locale
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Language
import androidx.compose.ui.res.stringResource
import com.avitrisphare.whiteboard.R
import com.avitrisphare.whiteboard.core.localization.AppLanguage
import com.avitrisphare.whiteboard.presentation.common.LanguageSelectionDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import com.avitrisphare.whiteboard.presentation.canvas.utility.CalculatorFloatingPanel
import com.avitrisphare.whiteboard.presentation.canvas.utility.CalculatorUiState
import com.avitrisphare.whiteboard.presentation.canvas.utility.PeriodicTableFloatingPanel
import com.avitrisphare.whiteboard.presentation.canvas.utility.PeriodicTableUiState
import com.avitrisphare.whiteboard.presentation.canvas.utility.StopwatchFloatingPanel
import com.avitrisphare.whiteboard.presentation.canvas.utility.StopwatchUiState
import com.avitrisphare.whiteboard.presentation.canvas.utility.TimerFloatingPanel
import com.avitrisphare.whiteboard.presentation.canvas.utility.TimerUiState
import com.avitrisphare.whiteboard.presentation.canvas.utility.UtilityToolKind
import com.avitrisphare.whiteboard.presentation.canvas.export.ExportLessonDialog
import com.avitrisphare.whiteboard.presentation.canvas.export.ExportFormat
import com.avitrisphare.whiteboard.presentation.canvas.export.ExportDestination
import com.avitrisphare.whiteboard.presentation.canvas.export.ExportResult
import com.avitrisphare.whiteboard.presentation.canvas.export.WhiteboardExportManager
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.atan2
import kotlin.math.hypot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.runtime.CompositionLocalProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.avitrisphare.whiteboard.core.theme.LocalAvitriSphareColors
import com.avitrisphare.whiteboard.domain.model.CanvasBackground
import com.avitrisphare.whiteboard.domain.model.Stroke
import com.avitrisphare.whiteboard.domain.model.StrokeType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.InputStream
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

private class EraserSession {
    var active: Boolean = false
    var changed: Boolean = false
    var strokes: List<Stroke> = emptyList()
    var cursor: Offset? = null
    var targetSelectedIds: MutableSet<String> = mutableSetOf()
}

@OptIn(ExperimentalComposeUiApi::class)
private fun androidx.compose.ui.input.pointer.PointerInputChange.pastPositions(): List<Offset> {
    return historical.map { it.position }
}

fun List<Offset>.toSmoothedPath(): Path {
    val path = Path()
    if (size < 2) return path
    path.moveTo(this[0].x, this[0].y)
    for (i in 1 until size - 1) {
        val midX = (this[i].x + this[i + 1].x) / 2f
        val midY = (this[i].y + this[i + 1].y) / 2f
        path.quadraticBezierTo(this[i].x, this[i].y, midX, midY)
    }
    path.lineTo(last().x, last().y)
    return path
}

private fun decodeMediaBitmap(context: Context, uriString: String, maxDim: Int = 2048): android.graphics.Bitmap? {
    return try {
        val openStream: () -> java.io.InputStream? = {
            when {
                uriString.startsWith("/") -> java.io.File(uriString).inputStream()
                uriString.startsWith("http://") || uriString.startsWith("https://") -> {
                    val conn = java.net.URL(uriString).openConnection()
                    conn.connectTimeout = 8000
                    conn.getInputStream()
                }
                else -> context.contentResolver.openInputStream(Uri.parse(uriString))
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openStream()?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > maxDim || bounds.outHeight / sample > maxDim) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        openStream()?.use { BitmapFactory.decodeStream(it, null, opts) }
    } catch (_: Exception) {
        null
    }
}

private fun DrawScope.drawCenteredNodeText(text: String, cx: Float, cy: Float, colorInt: Int, strokeW: Float) {
    if (text.isEmpty()) return
    drawContext.canvas.nativeCanvas.apply {
        val tPaint = Paint().apply {
            color = colorInt
            textSize = (strokeW * 4.2f).coerceIn(16f, 32f)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val lines = text.split("\n")
        val lineH = tPaint.textSize * 1.25f
        val startY = cy - ((lines.size - 1) * lineH) / 2f + tPaint.textSize * 0.35f
        lines.forEachIndexed { i, line ->
            drawText(line, cx, startY + i * lineH, tPaint)
        }
    }
}

fun DrawScope.renderStroke(
    context: Context,
    stroke: Stroke,
    currentScale: Float = 1f,
    isSelected: Boolean = false
) {
    val pointsList = stroke.points
    if (pointsList.isEmpty()) return
    val safeScale = if (currentScale <= 0f) 1f else currentScale
    val baseWidth = if (stroke.isHighlighter) stroke.strokeWidth * 3f else stroke.strokeWidth
    val strokeWidth = (baseWidth / safeScale).coerceAtLeast(1f / safeScale)
    val opacity = stroke.opacity.coerceIn(0.05f, 1f)
    val strokeColor = if (stroke.isHighlighter) {
        Color(stroke.colorValue).copy(alpha = 0.4f * opacity)
    } else {
        val baseColor = Color(stroke.colorValue)
        baseColor.copy(alpha = (baseColor.alpha * opacity).coerceIn(0f, 1f))
    }

    if (renderExtendedShape(stroke, safeScale, strokeColor, strokeWidth)) {
        return
    }

    when (stroke.strokeType) {
        StrokeType.CIRCLE, StrokeType.ELLIPSE -> {
            if (pointsList.size >= 2) {
                val p1 = pointsList.first()
                val p2 = pointsList.last()
                val left = minOf(p1.x, p2.x)
                val top = minOf(p1.y, p2.y)
                val width = kotlin.math.abs(p2.x - p1.x)
                val height = kotlin.math.abs(p2.y - p1.y)
                if (stroke.fillColor != 0) {
                    drawOval(
                        color = Color(stroke.fillColor),
                        topLeft = Offset(left, top),
                        size = Size(width, height)
                    )
                }
                drawOval(
                    color = strokeColor,
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    style = DrawStroke(width = strokeWidth)
                )
                if (stroke.text.isNotEmpty()) {
                    drawCenteredNodeText(stroke.text, left + width / 2f, top + height / 2f, stroke.colorValue, stroke.strokeWidth)
                }
            }
        }
        StrokeType.RECTANGLE, StrokeType.SQUARE -> {
            if (pointsList.size >= 2) {
                val p1 = pointsList.first()
                val p2 = pointsList.last()
                val left = minOf(p1.x, p2.x)
                val top = minOf(p1.y, p2.y)
                val width = kotlin.math.abs(p2.x - p1.x)
                val height = kotlin.math.abs(p2.y - p1.y)
                val radius = (if (stroke.cornerRadius > 0f) stroke.cornerRadius else 4f) / safeScale
                if (stroke.fillColor != 0) {
                    drawRoundRect(
                        color = Color(stroke.fillColor),
                        topLeft = Offset(left, top),
                        size = Size(width, height),
                        cornerRadius = CornerRadius(radius, radius)
                    )
                }
                drawRoundRect(
                    color = strokeColor,
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    cornerRadius = CornerRadius(radius, radius),
                    style = DrawStroke(width = strokeWidth)
                )
                if (stroke.text.isNotEmpty()) {
                    drawCenteredNodeText(stroke.text, left + width / 2f, top + height / 2f, stroke.colorValue, stroke.strokeWidth)
                }
            }
        }
        StrokeType.DIAMOND -> {
            if (pointsList.size >= 2) {
                val p1 = pointsList.first()
                val p2 = pointsList.last()
                val left = minOf(p1.x, p2.x)
                val right = maxOf(p1.x, p2.x)
                val top = minOf(p1.y, p2.y)
                val bottom = maxOf(p1.y, p2.y)
                val midX = (left + right) / 2f
                val midY = (top + bottom) / 2f

                val diamondPath = Path().apply {
                    moveTo(midX, top)
                    lineTo(right, midY)
                    lineTo(midX, bottom)
                    lineTo(left, midY)
                    close()
                }
                if (stroke.fillColor != 0) {
                    drawPath(path = diamondPath, color = Color(stroke.fillColor))
                }
                drawPath(
                    path = diamondPath,
                    color = strokeColor,
                    style = DrawStroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                if (stroke.text.isNotEmpty()) {
                    drawCenteredNodeText(stroke.text, midX, midY, stroke.colorValue, stroke.strokeWidth)
                }
            }
        }
        StrokeType.FRAME -> {
            if (pointsList.size >= 2) {
                val p1 = pointsList.first()
                val p2 = pointsList.last()
                val left = minOf(p1.x, p2.x)
                val top = minOf(p1.y, p2.y)
                val width = kotlin.math.abs(p2.x - p1.x)
                val height = kotlin.math.abs(p2.y - p1.y)

                drawRect(
                    color = Color(0x0C000000),
                    topLeft = Offset(left, top),
                    size = Size(width, height)
                )
                drawRect(
                    color = Color(0xFF6B7280),
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    style = DrawStroke(
                        width = 1.5f / safeScale,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f / safeScale, 6f / safeScale))
                    )
                )
                drawContext.canvas.nativeCanvas.apply {
                    val paint = Paint().apply {
                        color = 0xFF9CA3AF.toInt()
                        textSize = 14f
                        isAntiAlias = true
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }
                    drawText("Frame", left, top - 8f, paint)
                }
            }
        }
        StrokeType.TRIANGLE -> {
            if (pointsList.size >= 3) {
                val triangle = Path().apply {
                    moveTo(pointsList[0].x, pointsList[0].y)
                    lineTo(pointsList[1].x, pointsList[1].y)
                    lineTo(pointsList[2].x, pointsList[2].y)
                    close()
                }
                if (stroke.fillColor != 0) {
                    val fc = Color(stroke.fillColor)
                    drawPath(triangle, fc.copy(alpha = (fc.alpha * opacity).coerceIn(0f, 1f)))
                }
                drawPath(triangle, strokeColor, style = DrawStroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        StrokeType.STAR -> {
            if (pointsList.size >= 2) {
                val star = regularStarPath(pointsList.first(), pointsList.last())
                if (stroke.fillColor != 0) {
                    val fc = Color(stroke.fillColor)
                    drawPath(star, fc.copy(alpha = (fc.alpha * opacity).coerceIn(0f, 1f)))
                }
                drawPath(star, strokeColor, style = DrawStroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        StrokeType.LINE -> {
            if (pointsList.size >= 2) {
                drawLine(
                    color = strokeColor,
                    start = pointsList.first(),
                    end = pointsList.last(),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
        }
        StrokeType.ARROW -> {
            if (pointsList.size >= 2) {
                val start = pointsList.first()
                val end = pointsList.last()
                drawLine(
                    color = strokeColor,
                    start = start,
                    end = end,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                val angle = atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())
                val headLength = (20f / safeScale).coerceAtLeast(10f / safeScale)
                val arrowAngle = Math.PI / 6.0

                val arrowP1 = Offset(
                    (end.x - headLength * cos(angle - arrowAngle)).toFloat(),
                    (end.y - headLength * sin(angle - arrowAngle)).toFloat()
                )
                val arrowP2 = Offset(
                    (end.x - headLength * cos(angle + arrowAngle)).toFloat(),
                    (end.y - headLength * sin(angle + arrowAngle)).toFloat()
                )

                val headPath = Path().apply {
                    moveTo(arrowP1.x, arrowP1.y)
                    lineTo(end.x, end.y)
                    lineTo(arrowP2.x, arrowP2.y)
                }
                drawPath(
                    path = headPath,
                    color = strokeColor,
                    style = DrawStroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
        StrokeType.HEXAGON -> {
            if (pointsList.size >= 2) {
                drawHexagon(strokeColor, pointsList.first(), pointsList.last(), strokeWidth)
            }
        }
        StrokeType.ROUND_RECT -> {
            if (pointsList.size >= 2) {
                val p1 = pointsList.first()
                val p2 = pointsList.last()
                val left = minOf(p1.x, p2.x)
                val top = minOf(p1.y, p2.y)
                val width = kotlin.math.abs(p2.x - p1.x)
                val height = kotlin.math.abs(p2.y - p1.y)
                val radius = 24f / safeScale
                if (stroke.fillColor != 0) {
                    drawRoundRect(
                        Color(stroke.fillColor),
                        Offset(left, top),
                        Size(width, height),
                        CornerRadius(radius, radius)
                    )
                }
                drawRoundRect(
                    strokeColor,
                    Offset(left, top),
                    Size(width, height),
                    cornerRadius = CornerRadius(radius, radius),
                    style = DrawStroke(width = strokeWidth)
                )
                if (stroke.text.isNotEmpty()) {
                    drawCenteredNodeText(stroke.text, left + width / 2f, top + height / 2f, stroke.colorValue, stroke.strokeWidth)
                }
            }
        }
        StrokeType.CUBE, StrokeType.CYLINDER, StrokeType.CONE, StrokeType.PYRAMID -> {
            if (pointsList.size >= 2) {
                drawIsometricSolid(stroke.strokeType, strokeColor, pointsList.first(), pointsList.last(), strokeWidth)
            }
        }
        StrokeType.CONNECTOR -> {
            if (pointsList.size >= 2) {
                val p1 = pointsList.first()
                val p2 = pointsList.last()
                val actualWidth = strokeWidth.coerceAtLeast(3f)
                drawLine(strokeColor, p1, p2, actualWidth, cap = StrokeCap.Round)
                val angle = kotlin.math.atan2(p2.y - p1.y, p2.x - p1.x)
                val arrowLen = (actualWidth * 4.5f).coerceIn(16f, 32f)
                val arrowAngle = Math.PI / 6.0
                val ap1 = Offset(
                    p2.x - (arrowLen * kotlin.math.cos(angle - arrowAngle)).toFloat(),
                    p2.y - (arrowLen * kotlin.math.sin(angle - arrowAngle)).toFloat()
                )
                val ap2 = Offset(
                    p2.x - (arrowLen * kotlin.math.cos(angle + arrowAngle)).toFloat(),
                    p2.y - (arrowLen * kotlin.math.sin(angle + arrowAngle)).toFloat()
                )
                val arrowPath = Path().apply {
                    moveTo(p2.x, p2.y)
                    lineTo(ap1.x, ap1.y)
                    lineTo(ap2.x, ap2.y)
                    close()
                }
                drawPath(arrowPath, color = strokeColor)
            }
        }
        StrokeType.STICKER -> {
            val origin = pointsList.first()
            val label = when (stroke.stickerKind) {
                "star" -> "⭐"
                "tick" -> "✓"
                "cross" -> "✕"
                "thumb" -> "👍"
                "emoji" -> stroke.text.ifEmpty { "😀" }
                else -> stroke.text.ifEmpty { stroke.stickerKind }
            }
            drawContext.canvas.nativeCanvas.apply {
                val paint = Paint().apply {
                    color = when (stroke.stickerKind) {
                        "tick" -> 0xFF10B981.toInt()
                        "cross" -> 0xFFEF4444.toInt()
                        "star" -> 0xFFF59E0B.toInt()
                        else -> stroke.colorValue
                    }
                    textSize = (stroke.strokeWidth * 14f).coerceIn(56f, 150f)
                    isAntiAlias = true
                }
                drawText(label, origin.x, origin.y + paint.textSize * 0.88f, paint)
            }
        }
        StrokeType.IMAGE -> {
            if (stroke.imageUri.isNotBlank()) {
                val bmp = decodeMediaBitmap(context, stroke.imageUri)
                if (bmp != null) {
                    val b = stroke.bounds
                    val destRect = android.graphics.Rect(b.left.toInt(), b.top.toInt(), b.right.toInt(), b.bottom.toInt())
                    drawContext.canvas.nativeCanvas.drawBitmap(bmp, null, destRect, null)
                }
            }
        }
        StrokeType.VIDEO, StrokeType.WEB_EMBED -> Unit
        StrokeType.STICKY_NOTE -> {
            val origin = pointsList.first()
            val scale = (stroke.strokeWidth / 20f).coerceIn(0.75f, 3f)
            val w = 220f * scale
            val h = 180f * scale
            val fillColor = if (stroke.fillColor != 0) Color(stroke.fillColor) else Color(0xFFFFF9C4)
            drawRoundRect(
                color = fillColor,
                topLeft = origin,
                size = Size(w, h),
                cornerRadius = CornerRadius(12f, 12f)
            )
            drawRoundRect(
                color = Color(0x22000000),
                topLeft = origin,
                size = Size(w, h),
                cornerRadius = CornerRadius(12f, 12f),
                style = DrawStroke(width = 1.5f)
            )
            drawContext.canvas.nativeCanvas.apply {
                val paint = Paint().apply {
                    color = 0xFF171717.toInt()
                    textSize = 16f
                    isAntiAlias = true
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                }
                val text = stroke.text.ifEmpty { "Sticky note" }
                val lines = text.split("\n")
                var curY = origin.y + 30f
                for (line in lines) {
                    drawText(line, origin.x + 16f, curY, paint)
                    curY += 22f
                    if (curY > origin.y + h - 10f) break
                }
            }
        }
        StrokeType.EQUATION -> {
            val origin = pointsList.first()
            val size = stroke.strokeWidth.coerceAtLeast(28f)
            drawContext.canvas.nativeCanvas.let { native ->
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = stroke.colorValue
                    typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
                }
                val pad = formulaPadding(size)
                drawFormula(native, parseFormula(stroke.text), origin.x + pad, origin.y + pad, size, paint)
            }
        }
        StrokeType.TEXT -> {
            val origin = pointsList.first()
            drawContext.canvas.nativeCanvas.apply {
                val paint = Paint().apply {
                    color = stroke.colorValue
                    textSize = if (stroke.strokeWidth >= 12f) stroke.strokeWidth else 20f
                    isAntiAlias = true
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val lines = stroke.text.split("\n")
                var curY = origin.y + 20f
                for (line in lines) {
                    drawText(line, origin.x, curY, paint)
                    curY += 26f
                }
            }
        }
        StrokeType.PEN,
        StrokeType.PENCIL,
        StrokeType.MARKER,
        StrokeType.HIGHLIGHTER,
        StrokeType.BRUSH -> drawWritingStroke(stroke, safeScale)
        else -> {}
    }

    if (isSelected) {
        val b = strokeSelectionBounds(stroke)
        val pad = 8f / safeScale
        val handleRadius = 5f / safeScale
        val selColor = Color(0xFF6366F1)
        drawRect(
            color = selColor,
            topLeft = Offset(b.left - pad, b.top - pad),
            size = Size(b.width + pad * 2f, b.height + pad * 2f),
            style = DrawStroke(
                width = 1.5f / safeScale,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f / safeScale, 8f / safeScale))
            )
        )
        val corners = listOf(
            Offset(b.left - pad, b.top - pad),
            Offset(b.right + pad, b.top - pad),
            Offset(b.right + pad, b.bottom + pad),
            Offset(b.left - pad, b.bottom + pad)
        )
        corners.forEach { pt ->
            drawCircle(color = Color.White, radius = handleRadius, center = pt)
            drawCircle(color = selColor, radius = handleRadius, center = pt, style = DrawStroke(1.5f / safeScale))
        }
    }
}

@Composable
fun WhiteboardDockButton(
    icon: ImageVector,
    shortcut: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val activeBg = Color(0xFFE0E7FF)
    val activeTint = Color(0xFF3730A3)
    val inactiveTint = Color(0xFF4B5563)
    val badgeColor = if (isActive) Color(0xFF4F46E5) else Color(0xFF9CA3AF)

    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isActive) activeBg else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) activeTint else inactiveTint,
            modifier = Modifier.size(20.dp)
        )
        if (shortcut.isNotEmpty()) {
            Text(
                text = shortcut,
                color = badgeColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 2.dp, bottom = 1.dp)
            )
        }
    }
}

@Composable
fun SidebarMenuItem(
    icon: ImageVector,
    label: String,
    shortcut: String = "",
    isSelected: Boolean = false,
    isDark: Boolean = false,
    onClick: () -> Unit
) {
    val activeBg = if (isSelected) {
        if (isDark) Color(0xFF312E81) else Color(0xFFEEF2FF)
    } else Color.Transparent
    val activeText = if (isSelected) {
        if (isDark) Color(0xFFA5B4FC) else Color(0xFF4338CA)
    } else {
        if (isDark) Color(0xFFF1F5F9) else Color(0xFF1F2937)
    }
    val activeIcon = if (isSelected) {
        if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)
    } else {
        if (isDark) Color(0xFF94A3B8) else Color(0xFF4B5563)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(activeBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = activeIcon,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            color = activeText,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        if (shortcut.isNotEmpty()) {
            Text(
                text = shortcut,
                color = if (isDark) Color(0xFF64748B) else Color(0xFF9CA3AF),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun OverflowMenuItem(
    icon: ImageVector,
    label: String,
    shortcut: String = "",
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    val activeBg = if (isSelected) Color(0xFFEEF2FF) else Color.Transparent
    val activeText = if (isSelected) Color(0xFF4338CA) else Color(0xFF1F2937)
    val activeIcon = if (isSelected) Color(0xFF4F46E5) else Color(0xFF374151)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(activeBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = activeIcon,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            color = activeText,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        if (shortcut.isNotEmpty()) {
            Text(
                text = shortcut,
                color = Color(0xFF9CA3AF),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun BackgroundSwatch(type: CanvasBackground, isSelected: Boolean, onClick: () -> Unit) {
    val borderColor = if (isSelected) LocalAvitriSphareColors.current.brand else LocalAvitriSphareColors.current.borderStandard
    val borderWidth = if (isSelected) 3.dp else 1.dp

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .border(borderWidth, borderColor, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            when (type) {
                CanvasBackground.DARK -> drawRect(Color(0xFF171717))
                CanvasBackground.WHITE -> drawRect(Color.White)
                CanvasBackground.LINED -> {
                    drawRect(Color.White)
                    for (y in 0..size.height.toInt() step 8) {
                        drawLine(Color(0xFFD5D8DE), Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), 1f)
                    }
                }
                CanvasBackground.DOTTED -> {
                    drawRect(Color.White)
                    for (y in 0..size.height.toInt() step 8)
                        for (x in 0..size.width.toInt() step 8)
                            drawCircle(Color(0xFF9E9E9E), 1f, Offset(x.toFloat(), y.toFloat()))
                }
                CanvasBackground.GRID -> {
                    drawRect(Color.White)
                    for (v in 0..size.height.toInt() step 8)
                        drawLine(Color(0xFFD5D8DE), Offset(0f, v.toFloat()), Offset(size.width, v.toFloat()), 1f)
                    for (h in 0..size.width.toInt() step 8)
                        drawLine(Color(0xFFD5D8DE), Offset(h.toFloat(), 0f), Offset(h.toFloat(), size.height), 1f)
                }
                CanvasBackground.GRAPH -> {
                    drawRect(Color(0xFFF7FBFF))
                    for (v in 0..size.height.toInt() step 6)
                        drawLine(Color(0xFFD3E3F4), Offset(0f, v.toFloat()), Offset(size.width, v.toFloat()), 1f)
                    for (h in 0..size.width.toInt() step 6)
                        drawLine(Color(0xFFD3E3F4), Offset(h.toFloat(), 0f), Offset(h.toFloat(), size.height), 1f)
                }
                CanvasBackground.FOOTBALL -> {
                    drawRect(Color(0xFF2E7D32))
                    drawRect(Color(0x99FFFFFF), style = DrawStroke(2f))
                    drawLine(Color.White, Offset(size.width / 2f, 0f), Offset(size.width / 2f, size.height), 2f)
                }
                CanvasBackground.CUSTOM -> drawRect(Color(0xFFE2E8F0))
            }
        }
        if (type == CanvasBackground.CUSTOM) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = "Custom image background",
                tint = Color(0xFF475569),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CanvasScreen(
    noteId: String,
    noteTitle: String = "",
    viewModel: CanvasViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    demoMode: Boolean = false,
    panelSession: com.avitrisphare.whiteboard.presentation.entry.PanelSessionStore? = null,
    initialImportUri: Uri? = null,
    currentLanguage: AppLanguage = AppLanguage.ENGLISH,
    onLanguageChange: (AppLanguage) -> Unit = {}
) {
    val context = LocalContext.current

    LaunchedEffect(noteId) {
        viewModel.loadNote(noteId)
    }

    LaunchedEffect(initialImportUri) {
        if (initialImportUri != null) {
            viewModel.importIwbFile(context, initialImportUri) { _, msg ->
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(panelSession) {
        panelSession?.let { session ->
            viewModel.setUiLocaleTag(session.uiLocaleTag())
            viewModel.configureHelpPanel(
                panelId = session.panelId(),
                licenseState = if (session.snapshot().isLicensed) "Licensed" else "Not licensed"
            )
        }
    }

    val state by viewModel.state.collectAsStateWithLifecycle()
    val pageBitmap by viewModel.pageBitmap.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }

    var showSidebar by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showAiPanel by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showEditTitleDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showExportLessonDialog by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableFloatStateOf(0f) }
    var exportStatusMessage by remember { mutableStateOf("") }
    var exportResult by remember { mutableStateOf<ExportResult?>(null) }
    var showTextInputDialog by remember { mutableStateOf(false) }
    var showWebEmbedDialog by remember { mutableStateOf(false) }
    var showStickerEmojiDialog by remember { mutableStateOf(false) }
    var editingNodeStrokeId by remember { mutableStateOf<String?>(null) }
    var editingNodeText by remember { mutableStateOf("") }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showCommandPalette by remember { mutableStateOf(false) }
    var commandSearchQuery by remember { mutableStateOf("") }
    var pendingWorldPoint by remember { mutableStateOf(Offset.Zero) }
    var textInputValue by remember { mutableStateOf("") }
    var exportedUri by remember { mutableStateOf<Uri?>(null) }
    var lassoPoints by remember { mutableStateOf(listOf<Offset>()) }
    var timerState by remember { mutableStateOf(TimerUiState()) }
    var stopwatchState by remember { mutableStateOf(StopwatchUiState()) }
    var calculatorState by remember { mutableStateOf(CalculatorUiState()) }
    var periodicTableState by remember { mutableStateOf(PeriodicTableUiState()) }
    var toolZOrder by remember {
        mutableStateOf(
            listOf(
                UtilityToolKind.TIMER,
                UtilityToolKind.STOPWATCH,
                UtilityToolKind.CALCULATOR,
                UtilityToolKind.PERIODIC_TABLE
            )
        )
    }
    val bringToolToFront: (UtilityToolKind) -> Unit = { tool ->
        if (toolZOrder.lastOrNull() != tool) {
            toolZOrder = toolZOrder.filter { it != tool } + tool
        }
    }

    var canvasScale by remember { mutableFloatStateOf(1f) }
    val liveInkTick = remember { mutableIntStateOf(0) }
    val liveInk = remember { LiveInk() }
    val eraserTick = remember { mutableIntStateOf(0) }
    val eraserSession = remember { EraserSession() }
    var boardStage by remember { mutableStateOf<BoardStageView?>(null) }
    var shadeFromTop by remember { mutableStateOf(true) }
    var shadeEdge by remember { mutableFloatStateOf(0.4f) }
    var spotlightCenter by remember { mutableStateOf<Offset?>(null) }
    var spotlightRadius by remember { mutableFloatStateOf(150f) }
    var showImageSearch by remember { mutableStateOf(false) }
    var showYouTubeDialog by remember { mutableStateOf(false) }
    var imageQuery by remember { mutableStateOf("") }
    var imageSearchTerm by remember { mutableStateOf("") }
    var youtubeQuery by remember { mutableStateOf("") }
    val composeView = LocalView.current
    val touchContacts = remember(composeView) { TouchContacts(composeView) }
    val xdpi = remember {
        val metrics = context.resources.displayMetrics
        if (metrics.xdpi > 50f) metrics.xdpi else metrics.densityDpi.toFloat()
    }
    val ydpi = remember {
        val metrics = context.resources.displayMetrics
        if (metrics.ydpi > 50f) metrics.ydpi else xdpi
    }
    var canvasOffsetX by remember { mutableFloatStateOf(0f) }
    var canvasOffsetY by remember { mutableFloatStateOf(0f) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var canvasOrigin by remember { mutableStateOf(Offset.Zero) }
    val chromeBounds = remember { ChromeBounds() }
    var dockDismissNonce by remember { mutableIntStateOf(0) }
    var selectionToolbarDismissed by remember { mutableStateOf(false) }

    LaunchedEffect(state.selectedStrokeIds) {
        if (state.selectedStrokeIds.isNotEmpty()) {
            selectionToolbarDismissed = false
        }
    }

    // Math instruments live in screen space so centimetre marks stay physical size.
    var mathTools by remember { mutableStateOf(state.mathTools) }
    var mathGhost by remember { mutableStateOf<PlacedMathTool?>(null) }
    var mathReadout by remember { mutableStateOf<Pair<String, Offset>?>(null) }
    var rulerLivePreview by remember { mutableStateOf<Pair<Offset, Offset>?>(null) }
    var shapePrompt by remember { mutableStateOf<ShapePrompt?>(null) }
    var shapePromptVisible by remember { mutableStateOf(false) }

    LaunchedEffect(shapePrompt?.strokeId) {
        val prompt = shapePrompt
        if (prompt == null) {
            shapePromptVisible = false
            return@LaunchedEffect
        }
        shapePromptVisible = false
        delay(SHAPE_PROMPT_DELAY_MS)
        if (shapePrompt?.strokeId != prompt.strokeId) return@LaunchedEffect
        shapePromptVisible = true
        delay(SHAPE_AUTO_CONVERT_MS)
        if (shapePrompt?.strokeId == prompt.strokeId) {
            viewModel.replaceStroke(prompt.replacement)
            shapePrompt = null
            shapePromptVisible = false
        }
    }
    val pxPerCm = remember(xdpi) { xdpi / 2.54f }

    fun canvasCenter(): Offset {
        val cx = if (canvasSize.width > 0) canvasSize.width / 2f else 400f
        val cy = if (canvasSize.height > 0) canvasSize.height / 2f else 500f
        return Offset(cx, cy)
    }

    fun replaceMathTool(updated: PlacedMathTool) {
        mathTools = mathTools.map { if (it.id == updated.id) updated else it }
    }

    fun removeMathTool(id: String) {
        val removed = mathTools.find { it.id == id } ?: return
        mathTools = mathTools.filter { it.id != id }
        viewModel.setMathTools(mathTools)
        val active = when (removed.kind) {
            MathToolKind.RULER -> WhiteboardTool.RULER
            MathToolKind.COMPASS -> WhiteboardTool.COMPASS
            MathToolKind.PROTRACTOR -> WhiteboardTool.PROTRACTOR
            MathToolKind.SET_SQUARE -> WhiteboardTool.SET_SQUARE
            MathToolKind.TRIANGLE -> WhiteboardTool.TRIANGLE
        }
        if (viewModel.state.value.currentTool == active) {
            val remainingTool = mathTools.lastOrNull()
            if (remainingTool != null) {
                val nextTool = when (remainingTool.kind) {
                    MathToolKind.RULER -> WhiteboardTool.RULER
                    MathToolKind.COMPASS -> WhiteboardTool.COMPASS
                    MathToolKind.PROTRACTOR -> WhiteboardTool.PROTRACTOR
                    MathToolKind.SET_SQUARE -> WhiteboardTool.SET_SQUARE
                    MathToolKind.TRIANGLE -> WhiteboardTool.TRIANGLE
                }
                viewModel.setTool(nextTool)
            } else {
                viewModel.setTool(WhiteboardTool.SELECT)
            }
        }
    }

    fun removeAllMathTools() {
        mathTools = emptyList()
        viewModel.setMathTools(emptyList())
        val current = viewModel.state.value.currentTool
        if (current == WhiteboardTool.RULER || current == WhiteboardTool.COMPASS || current == WhiteboardTool.PROTRACTOR ||
            current == WhiteboardTool.SET_SQUARE || current == WhiteboardTool.TRIANGLE
        ) {
            viewModel.setTool(WhiteboardTool.SELECT)
        }
    }

    LaunchedEffect(state.pageIndex, state.pageEpoch) {
        rulerLivePreview = null
        mathReadout = null
        mathGhost = null
        shapePrompt = null
        shapePromptVisible = false
        boardStage?.setLiveInk(null)
        boardStage?.clearGestureStrokes()
        boardStage?.setMarquee(null, null)
        mathTools = viewModel.state.value.mathTools
        val curTool = viewModel.state.value.currentTool
        if (curTool in setOf(
                WhiteboardTool.RULER,
                WhiteboardTool.COMPASS,
                WhiteboardTool.PROTRACTOR,
                WhiteboardTool.SET_SQUARE,
                WhiteboardTool.TRIANGLE
            )
        ) {
            val matchingKind = when (curTool) {
                WhiteboardTool.RULER -> MathToolKind.RULER
                WhiteboardTool.COMPASS -> MathToolKind.COMPASS
                WhiteboardTool.PROTRACTOR -> MathToolKind.PROTRACTOR
                WhiteboardTool.SET_SQUARE -> MathToolKind.SET_SQUARE
                WhiteboardTool.TRIANGLE -> MathToolKind.TRIANGLE
                else -> null
            }
            if (mathTools.none { it.kind == matchingKind }) {
                viewModel.setTool(viewModel.state.value.inkTool)
            }
        }
        viewModel.cancelCurrentStroke()
    }

    fun placeMathTool(kind: MathToolKind, center: Offset, isDrag: Boolean = false) {
        val existingIndex = mathTools.indexOfFirst { it.kind == kind }

        val tool = when (kind) {
            MathToolKind.RULER -> WhiteboardTool.RULER
            MathToolKind.COMPASS -> WhiteboardTool.COMPASS
            MathToolKind.PROTRACTOR -> WhiteboardTool.PROTRACTOR
            MathToolKind.SET_SQUARE -> WhiteboardTool.SET_SQUARE
            MathToolKind.TRIANGLE -> WhiteboardTool.TRIANGLE
        }

        if (existingIndex >= 0) {
            // Tool is already on the whiteboard:
            if (isDrag) {
                mathTools = mathTools.toMutableList().also {
                    it[existingIndex] = it[existingIndex].copy(center = center)
                }
                viewModel.setMathTools(mathTools)
                viewModel.setTool(tool)
            } else {
                // Tapped in the dock when already active: toggle close!
                removeMathTool(mathTools[existingIndex].id)
                Toast.makeText(context, "${kind.name.lowercase().replaceFirstChar { it.uppercase() }} closed", Toast.LENGTH_SHORT).show()
            }
            return
        }

        // Tool is new: add it to the whiteboard without overlapping when spawned at center, or exactly at dragged center
        val targetCenter = if (isDrag) center else {
            findNonOverlappingPosition(
                desiredCenter = canvasCenter(),
                kind = kind,
                span = defaultMathSpan(kind),
                existingTools = mathTools,
                canvasSize = canvasSize
            )
        }
        val newTool = newMathTool(kind, targetCenter)
        mathTools = mathTools + newTool
        viewModel.setMathTools(mathTools)
        viewModel.setTool(tool)
    }

    fun adjustMathTool(kind: MathToolKind, adjust: MathToolAdjust) {
        val current = mathTools.find { it.kind == kind } ?: return
        val rotation = adjust.rotationDeg?.let { deg ->
            val wrapped = deg % 360f
            if (wrapped < 0f) wrapped + 360f else wrapped
        } ?: current.rotationDeg
        replaceMathTool(
            current.copy(
                span = adjust.span?.let { clampMathSpan(kind, it) } ?: current.span,
                rotationDeg = rotation,
                pointerAngleDeg = adjust.pointerAngleDeg?.coerceIn(0f, 180f) ?: current.pointerAngleDeg
            )
        )
        viewModel.setMathTools(mathTools)
    }

    fun rootToCanvas(root: Offset): Offset = root - canvasOrigin

    var panePickIndex by remember { mutableIntStateOf(-1) }
    var paneWebIndex by remember { mutableIntStateOf(-1) }
    var paneWebAddress by remember { mutableStateOf("https://") }

    val paneDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        val index = panePickIndex
        if (uri == null || index < 0) return@rememberLauncherForActivityResult
        viewModel.openPaneDocument(index, uri)
    }

    val paneVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        val index = panePickIndex
        if (uri == null || index < 0) return@rememberLauncherForActivityResult
        viewModel.openPaneVideo(index, uri)
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        viewModel.openDocument(uri, canvasSize.width, canvasSize.height)
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val centerWorld = CanvasTransform.screenToWorld(
                screenPoint = Offset(canvasSize.width / 2f, canvasSize.height / 2f),
                scale = canvasScale,
                offset = Offset(canvasOffsetX, canvasOffsetY)
            )
            viewModel.addImage(it.toString(), centerWorld)
        }
    }

    val customBgPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.setPageBackground(CanvasBackground.CUSTOM, it.toString())
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
        }
        val centerWorld = CanvasTransform.screenToWorld(
            screenPoint = Offset(canvasSize.width / 2f, canvasSize.height / 2f),
            scale = canvasScale,
            offset = Offset(canvasOffsetX, canvasOffsetY)
        )
        viewModel.addLocalVideo(uri.toString(), centerWorld)
    }

    val iwbPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importIwbFile(context, uri) { _, msg ->
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(state.splitCount, state.scale, state.offsetX, state.offsetY) {
        canvasScale = state.scale
        canvasOffsetX = state.offsetX
        canvasOffsetY = state.offsetY
        boardStage?.setTransform(state.scale, state.offsetX, state.offsetY)
        if (state.splitCount == 1) {
            eraserSession.active = false
            eraserSession.cursor = null
            eraserSession.changed = false
            eraserSession.strokes = emptyList()
            boardStage?.clearGestureStrokes()
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(state.paneRequest) {
        val request = state.paneRequest ?: return@LaunchedEffect
        val kind = request.substringBefore(':')
        val index = request.substringAfter(':').toIntOrNull() ?: return@LaunchedEffect
        viewModel.clearPaneRequest()
        panePickIndex = index
        when (kind) {
            PaneKind.PDF.name -> paneDocumentLauncher.launch(
                arrayOf(
                    "application/pdf",
                    "application/vnd.ms-powerpoint",
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                )
            )
            PaneKind.VIDEO.name -> paneVideoLauncher.launch(arrayOf("video/*"))
            PaneKind.WEB.name -> {
                paneWebAddress = "https://"
                paneWebIndex = index
            }
        }
    }

    CompositionLocalProvider(LocalDarkTheme provides state.isDarkTheme) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (state.isDarkCanvas) Color(0xFF171717) else Color.White)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                if (event.key == Key.Delete || event.key == Key.Backspace) {
                    if (state.selectedStrokeIds.isNotEmpty() && state.editingTextId == null) {
                        viewModel.deleteSelectedStrokes()
                        return@onPreviewKeyEvent true
                    }
                }
                val shortcut = event.isCtrlPressed || event.isMetaPressed
                if (!shortcut) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.Z -> {
                        if (event.isShiftPressed) viewModel.redo() else viewModel.undo()
                        true
                    }
                    Key.Y -> {
                        viewModel.redo()
                        true
                    }
                    else -> false
                }
            }
    ) {
        if (state.splitCount > 1) {
            SplitCanvas(
                state = state,
                viewModel = viewModel,
                onToggleAi = { showAiPanel = !showAiPanel }
            )
        } else {
        // 1. Full-bleed Infinite Whiteboard Canvas Viewport
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged {
                    canvasSize = Size(it.width.toFloat(), it.height.toFloat())
                    viewModel.updateViewport(canvasSize.width, canvasSize.height)
                }
                .onGloballyPositioned { canvasOrigin = it.positionInRoot() }
                .pointerInteropFilter { event ->
                    touchContacts.record(event)
                    val stage = boardStage
                    if (stage != null && stage.shouldDeliverToMedia(event, viewModel.state.value.currentTool)) {
                        stage.deliverToMedia(event)
                        true
                    } else {
                        false
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (canvasSize.width > 0 && canvasSize.height > 0) {
                                viewModel.fitToContent(canvasSize.width, canvasSize.height)
                            } else {
                                canvasScale = 1f
                                canvasOffsetX = 0f
                                canvasOffsetY = 0f
                                viewModel.resetTransform()
                            }
                        }
                    )
                }
                .pointerInput(
                    state.currentTool,
                    state.selectedColor,
                    state.secondaryColor,
                    state.strokeWidth,
                    state.eraserMode,
                    state.eraserSize,
                    state.isToolLocked,
                    state.selectedStrokeIds,
                    state.isDrawToShapeEnabled,
                    state.palmEraserEnabled,
                    state.restrictEraserToSelection
                ) {
                    awaitEachGesture {
                        fun PointerInputChange.isStylusPointer(): Boolean {
                            val tool = touchContacts.toolType(id)
                            return tool == MotionEvent.TOOL_TYPE_STYLUS ||
                                tool == MotionEvent.TOOL_TYPE_ERASER ||
                                type == PointerType.Stylus ||
                                type == PointerType.Eraser
                        }
                        fun PointerInputChange.isPalmTouch(): Boolean {
                            if (isStylusPointer()) return false
                            return isPalmContactArea(
                                touchMajorPx = touchContacts.majorPx(id),
                                xdpi = xdpi,
                                touchMinorPx = touchContacts.minorPx(id),
                                ydpi = ydpi
                            )
                        }
                        fun PointerInputChange.rejectedPalm(stylusDrawing: Boolean): Boolean {
                            val stylus = isStylusPointer()
                            if (stylus) return false
                            val palm = isPalmTouch()
                            if (!palm) return false
                            // Resting palm while drawing with a stylus is ALWAYS ignored/rejected
                            if (stylusDrawing || touchContacts.stylusActive) return true
                            // If no stylus is active, allow palm contact through for palm erasing
                            return false
                        }
                        var down = awaitFirstDown(requireUnconsumed = false)
                        dockDismissNonce++
                        while (down.rejectedPalm(stylusDrawing = false)) {
                            down.consume()
                            val skipped = awaitPointerEvent()
                            val next = skipped.changes.firstOrNull { it.pressed && !it.rejectedPalm(stylusDrawing = false) }
                            if (next != null) {
                                down = next
                                break
                            }
                            if (skipped.changes.none { it.pressed }) return@awaitEachGesture
                        }
                        val downWorld = CanvasTransform.screenToWorld(
                            screenPoint = down.position,
                            scale = canvasScale,
                            offset = Offset(canvasOffsetX, canvasOffsetY)
                        )
                        val latestState = viewModel.state.value
                        val isPalmEraser = (latestState.currentTool in WRITING_TOOLS || latestState.currentTool == WhiteboardTool.ERASER) &&
                            !down.isStylusPointer() &&
                            down.isPalmTouch()
                        var prevPosition = down.position
                        var shapePoints = listOf(downWorld)
                        val stylusDraw = down.isStylusPointer()
                        val isShapeActive = state.currentTool == WhiteboardTool.SHAPE || state.currentTool in setOf(
                            WhiteboardTool.RECTANGLE,
                            WhiteboardTool.CIRCLE,
                            WhiteboardTool.DIAMOND,
                            WhiteboardTool.LINE,
                            WhiteboardTool.ARROW,
                            WhiteboardTool.FRAME,
                            WhiteboardTool.TRIANGLE,
                            WhiteboardTool.STAR,
                            WhiteboardTool.ELLIPSE,
                            WhiteboardTool.SQUARE,
                            WhiteboardTool.HEXAGON,
                            WhiteboardTool.ROUND_RECT,
                            WhiteboardTool.CUBE,
                            WhiteboardTool.CYLINDER,
                            WhiteboardTool.CONE,
                            WhiteboardTool.PYRAMID
                        )
                        val rawToolType = touchContacts.toolType(down.position)
                        val isEraserType = rawToolType == MotionEvent.TOOL_TYPE_ERASER || down.type == PointerType.Eraser
                        val isStylusType = rawToolType == MotionEvent.TOOL_TYPE_STYLUS || down.type == PointerType.Stylus
                        val majorPx = touchContacts.majorPx(down.position)
                        val minorPx = touchContacts.minorPx(down.position)
                        val isPenNib = isPenNibContact(
                            isEraser = isEraserType,
                            isStylus = isStylusType || stylusDraw,
                            majorPx = majorPx,
                            minorPx = minorPx,
                            xdpi = xdpi
                        )

                        val contactSizePx = if (majorPx > 0f) {
                            if (minorPx > 0f) (majorPx + minorPx) / 2f else majorPx
                        } else {
                            0f
                        }
                        val secondaryStrokeWidth = if (contactSizePx > 0f) {
                            if (contactSizePx <= 5.0f) {
                                (contactSizePx * 5f).coerceIn(14f, 32f)
                            } else {
                                contactSizePx.coerceIn(12f, 44f)
                            }
                        } else {
                            if (isEraserType) 16f else 20f
                        }

                        val activeInkColor = if (isPenNib) state.selectedColor else state.secondaryColor
                        val activeInkWidth = if (isPenNib) state.strokeWidth else secondaryStrokeWidth
                        android.util.Log.i("AvitriTouch", "DECISION down.id=${down.id.value} pos=${down.position} tool=$rawToolType stylusDraw=$stylusDraw majorPx=$majorPx minorPx=$minorPx xdpi=$xdpi -> isPenNib=$isPenNib (color=${if (isPenNib) "PRIMARY" else "SECONDARY"}, width=$activeInkWidth)")

                        val activeTool = if (isPalmEraser) {
                            WhiteboardTool.ERASER
                        } else if (stylusDraw) {
                            if (isEraserType && latestState.currentTool == WhiteboardTool.ERASER) {
                                WhiteboardTool.ERASER
                            } else if (isShapeActive) {
                                state.currentTool
                            } else {
                                latestState.inkTool
                            }
                        } else {
                            latestState.currentTool
                        }
                        val palmMajor = touchContacts.majorPx(down.id)
                        val customEraserRadius = if (isPalmEraser && palmMajor > 0f) {
                            (palmMajor / 2f) / canvasScale.coerceAtLeast(0.001f)
                        } else null
                        val inkPoints = ArrayList<Offset>(256)
                        val inkPressures = ArrayList<Float>(256)
                        inkPoints.add(downWorld)
                        inkPressures.add(if (stylusDraw) stylusPressure(down.pressure) else down.inkPressure())
                        if (activeTool in WRITING_TOOLS) {
                            liveInk.begin(downWorld)
                            shapePrompt = null
                            shapePromptVisible = false
                        }
                        var wasMultiTouch = false
                        var transformMoved = false
                        var fingerPeak = 1
                        var gestureStart: Offset? = null
                        var gestureEnd = Offset.Zero
                        var gestureTravel = 0f
                        var gestureScale = canvasScale
                        var gestureOffsetX = canvasOffsetX
                        var gestureOffsetY = canvasOffsetY
                        val fingerScratch = ArrayList<androidx.compose.ui.input.pointer.PointerInputChange>(8)
                        val writingNow = activeTool in WRITING_TOOLS
                        val inkTemplate = if (writingNow) {
                            Stroke(
                                points = inkPoints,
                                pointPressures = inkPressures,
                                colorValue = activeInkColor,
                                strokeWidth = activeInkWidth,
                                isHighlighter = activeTool == WhiteboardTool.HIGHLIGHTER,
                                strokeType = activeTool.toStrokeType()
                            )
                        } else {
                            null
                        }
                        fun publishInk() {
                            val template = inkTemplate ?: return
                            val last = inkPoints.lastOrNull()
                            val dx = touchContacts.predictDx
                            val dy = touchContacts.predictDy
                            liveInk.predicted = if (last == null || (dx == 0f && dy == 0f)) {
                                null
                            } else {
                                val safeScale = canvasScale.coerceAtLeast(0.001f)
                                Offset(last.x + dx / safeScale, last.y + dy / safeScale)
                            }
                            liveInk.stroke = if (isPenNib) {
                                if (stylusDraw) {
                                    template.copy(
                                        strokeWidth = stylusPressureWidth(
                                            activeInkWidth,
                                            inkPressures.lastOrNull() ?: 1f
                                        )
                                    )
                                } else {
                                    template
                                }
                            } else {
                                val curMajor = touchContacts.majorPx(down.position)
                                val curMinor = touchContacts.minorPx(down.position)
                                val curContact = if (curMajor > 0f) (if (curMinor > 0f) (curMajor + curMinor) / 2f else curMajor) else 0f
                                val liveWidth = if (curContact > 0f) {
                                    if (curContact <= 5.0f) (curContact * 5f).coerceIn(14f, 32f) else curContact.coerceIn(12f, 44f)
                                } else activeInkWidth
                                template.copy(strokeWidth = liveWidth)
                            }
                            liveInkTick.intValue = liveInkTick.intValue + 1
                            boardStage?.setLiveInk(liveInk)
                        }
                        fun appendInk(screen: Offset, pressure: Float) {
                            val world = CanvasTransform.screenToWorld(
                                screenPoint = screen,
                                scale = canvasScale,
                                offset = Offset(canvasOffsetX, canvasOffsetY)
                            )
                            val last = inkPoints.lastOrNull()
                            if (last != null) {
                                val dx = world.x - last.x
                                val dy = world.y - last.y
                                val gap = inkSampleGapPx(state.inkSampleGapMm, xdpi, canvasScale)
                                if (dx * dx + dy * dy < gap * gap) return
                            }
                            inkPoints.add(world)
                            inkPressures.add(pressure)
                            liveInk.extend(world)
                        }
                        fun eraseSample(world: Offset, screen: Offset, customRadius: Float? = null) {
                            val latest = viewModel.state.value
                            if (!eraserSession.active) {
                                eraserSession.active = true
                                eraserSession.changed = false
                                eraserSession.strokes = latest.strokes
                                eraserSession.targetSelectedIds = latest.selectedStrokeIds.toMutableSet()
                            }
                            val baseRadius = latest.eraserSize / canvasScale.coerceAtLeast(0.001f)
                            val radius = customRadius ?: baseRadius

                            val eraseModeToUse = if (isPalmEraser) EraserMode.SMART else latest.eraserMode
                            val restrictSelection = latest.selectedStrokeIds.isNotEmpty()

                            val (updated, newSelectedIds) = StrokeEraser.applySelection(
                                strokes = eraserSession.strokes,
                                selectedIds = eraserSession.targetSelectedIds,
                                center = world,
                                radius = radius,
                                mode = eraseModeToUse,
                                restrictToSelection = restrictSelection
                            )
                            if (updated !== eraserSession.strokes) {
                                eraserSession.strokes = updated
                                eraserSession.targetSelectedIds = newSelectedIds.toMutableSet()
                                eraserSession.changed = true
                            }
                            eraserSession.cursor = screen
                            eraserTick.intValue = eraserTick.intValue + 1
                            boardStage?.showGestureStrokes(eraserSession.strokes)
                        }
                        if (activeTool == WhiteboardTool.ERASER) {
                            eraseSample(downWorld, down.position, customEraserRadius)
                        }
                        var isDraggingSelection = false
                        var selectingBox = false
                        var strokeHit = false
                        var resizeStrokeId: String? = null
                        var resizeHandle: ShapeHandleHit? = null
                        val mathGesture = MathGesture()
                        val mathHit = hitTestPlacedTools(mathTools, down.position)
                        val straightedge = if (mathHit != null) {
                            null
                        } else if (activeTool != WhiteboardTool.HAND && activeTool != WhiteboardTool.ERASER) {
                            rulerForStraightedge(mathTools, down.position)
                        } else {
                            null
                        }
                        if (mathHit != null) {
                            liveInk.stroke = null
                            liveInkTick.intValue = liveInkTick.intValue + 1
                            boardStage?.setLiveInk(null)
                            rulerLivePreview = null
                        }
                        if (mathHit != null) {
                            val tool = mathHit.tool
                            if (mathHit.zone == MathToolZone.CLOSE) {
                                removeMathTool(tool.id)
                                val toolName = tool.kind.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
                                Toast.makeText(context, "$toolName closed", Toast.LENGTH_SHORT).show()
                                mathGesture.reset()
                            } else {
                            mathGesture.toolId = tool.id
                            mathGesture.zone = mathHit.zone
                            mathGesture.draggingPositiveEnd = mathHit.zone == MathToolZone.ROTATE_POSITIVE
                            if (mathHit.zone == MathToolZone.ROTATE_POSITIVE || mathHit.zone == MathToolZone.ROTATE_NEGATIVE) {
                                mathGesture.pivot = endPoint(tool, positive = !mathGesture.draggingPositiveEnd)
                            }
                            if (mathHit.zone == MathToolZone.PENCIL) {
                                mathGesture.startSpan = tool.span
                                mathGesture.startAngleDeg = tool.rotationDeg
                                mathGesture.lastAngleDeg = tool.rotationDeg
                            }
                            if (mathHit.zone == MathToolZone.SCALE) {
                                mathGesture.startSpan = tool.span
                                mathGesture.startToolScale = tool.toolScale
                                mathGesture.startAngleDeg = tool.rotationDeg
                                mathGesture.startDist = hypot(down.position.x - tool.center.x, down.position.y - tool.center.y).coerceAtLeast(10f)
                                val angle = protractorAngle(tool, down.position)
                                if (angle != null) replaceMathTool(tool.copy(markedAngleDeg = angle))
                            }
                            if (mathHit.zone == MathToolZone.ANGLE_ARM) {
                                val local = toLocal(down.position, tool.center, tool.rotationDeg)
                                val a = (atan2(-local.y, local.x) * (180f / Math.PI.toFloat())).coerceIn(0f, 180f)
                                replaceMathTool(tool.copy(pointerAngleDeg = kotlin.math.round(a * 10f) / 10f))
                            }
                            if (mathHit.zone == MathToolZone.ROTATE) {
                                mathGesture.startAngleDeg = tool.rotationDeg
                                val radToDeg = 180f / Math.PI.toFloat()
                                mathGesture.lastAngleDeg = atan2(down.position.y - tool.center.y, down.position.x - tool.center.x) * radToDeg
                                mathGesture.sweepDeg = 0f
                            }
                            if (mathHit.zone == MathToolZone.SPREAD) {
                                mathGesture.startSpan = tool.span
                                mathGesture.startToolScale = tool.toolScale
                                mathGesture.startAngleDeg = tool.rotationDeg
                                mathGesture.startDist = hypot(down.position.x - tool.center.x, down.position.y - tool.center.y).coerceAtLeast(10f)
                            }
                            }
                        } else if (straightedge != null) {
                            mathGesture.rulerDrawing = true
                            mathGesture.toolId = straightedge.id
                            mathGesture.drawDir = direction(straightedge.rotationDeg)
                            mathGesture.rulerEdgeSign = rulerEdgeForPoint(straightedge, down.position)

                            val snappedStartScreen = snapToRulerEdge(straightedge, down.position, mathGesture.rulerEdgeSign)
                            val snappedStartWorld = CanvasTransform.screenToWorld(
                                snappedStartScreen,
                                canvasScale,
                                Offset(canvasOffsetX, canvasOffsetY)
                            )
                            val strokeColor = if (state.selectedColor != 0) state.selectedColor else (if (state.isDarkCanvas) 0xFFFFFFFF.toInt() else 0xFF1E293B.toInt())
                            val strokeWidth = if (state.strokeWidth > 0f) state.strokeWidth else 6f
                            val newStroke = Stroke(
                                points = listOf(snappedStartWorld, snappedStartWorld),
                                colorValue = strokeColor,
                                strokeWidth = strokeWidth,
                                isHighlighter = activeTool == WhiteboardTool.HIGHLIGHTER,
                                strokeType = if (activeTool == WhiteboardTool.HIGHLIGHTER) StrokeType.HIGHLIGHTER else StrokeType.LINE
                            )
                            shapePoints = newStroke.points
                            viewModel.startStroke(newStroke)
                            liveInk.stroke = newStroke
                            liveInkTick.intValue = liveInkTick.intValue + 1
                            boardStage?.setLiveInk(liveInk)
                            rulerLivePreview = snappedStartScreen to snappedStartScreen
                            val perp = perpendicular(straightedge.rotationDeg)
                            val readoutPos = snappedStartScreen + perp * (mathGesture.rulerEdgeSign * 38f)
                            mathReadout = "Line: 0.0 cm" to readoutPos
                        }

                        if (state.selectedStrokeIds.isNotEmpty() && activeTool != WhiteboardTool.SELECT) {
                            viewModel.clearSelection()
                        }

                        if (mathHit == null && straightedge == null) when (activeTool) {
                            WhiteboardTool.HAND -> {
                                // Hand pan mode
                            }
                            WhiteboardTool.SELECT -> {
                                val handleSlop = 28f / canvasScale.coerceAtLeast(0.001f)
                                val moveSlop = 36f / canvasScale.coerceAtLeast(0.001f)
                                val selectedHandle = viewModel.state.value.strokes.firstNotNullOfOrNull { stroke ->
                                    if (stroke.id !in state.selectedStrokeIds) {
                                        null
                                    } else {
                                        hitShapeHandle(stroke, downWorld, handleSlop)?.let { stroke.id to it }
                                    }
                                }
                                if (selectedHandle != null) {
                                    resizeStrokeId = selectedHandle.first
                                    resizeHandle = selectedHandle.second
                                } else {
                                    val selectedStrokes = viewModel.state.value.strokes.filter { it.id in state.selectedStrokeIds }
                                    val hitSelected = selectedStrokes.isNotEmpty() && selectedStrokes.any { stroke ->
                                        val b = strokeSelectionBounds(stroke)
                                        downWorld.x >= b.left - moveSlop && downWorld.x <= b.right + moveSlop &&
                                        downWorld.y >= b.top - moveSlop && downWorld.y <= b.bottom + moveSlop
                                    }
                                    if (hitSelected) {
                                        isDraggingSelection = true
                                    } else {
                                        val hit = viewModel.strokeAt(downWorld)
                                        strokeHit = hit != null
                                        if (hit != null) {
                                            viewModel.selectStrokeAt(downWorld)
                                            isDraggingSelection = true
                                        } else {
                                            selectingBox = true
                                        }
                                    }
                                }
                            }
                            WhiteboardTool.BUCKET_FILL -> {
                                viewModel.bucketFillAt(downWorld, state.selectedColor)
                            }
                            WhiteboardTool.LASSO -> {
                                lassoPoints = listOf(downWorld)
                            }
                            WhiteboardTool.LASER -> {
                                viewModel.addLaserPoint(downWorld)
                            }
                            WhiteboardTool.TEXT -> {
                                pendingWorldPoint = downWorld
                                textInputValue = ""
                                showTextInputDialog = true
                            }
                            WhiteboardTool.ERASER -> eraseSample(downWorld, down.position)
                            WhiteboardTool.SPOTLIGHT -> spotlightCenter = down.position
                            WhiteboardTool.SHADE -> {
                                val height = canvasSize.height.coerceAtLeast(1f)
                                shadeEdge = (down.position.y / height).coerceIn(0.05f, 0.95f)
                            }
                            WhiteboardTool.MAGNIFIER -> boardStage?.showMagnifier(down.position.x, down.position.y)
                            WhiteboardTool.PEN,
                            WhiteboardTool.MATH_PEN,
                            WhiteboardTool.PENCIL,
                            WhiteboardTool.MARKER,
                            WhiteboardTool.HIGHLIGHTER,
                            WhiteboardTool.BRUSH -> publishInk()
                            WhiteboardTool.RECTANGLE,
                            WhiteboardTool.DIAMOND,
                            WhiteboardTool.CIRCLE,
                            WhiteboardTool.ARROW,
                            WhiteboardTool.LINE,
                            WhiteboardTool.FRAME,
                            WhiteboardTool.TRIANGLE,
                            WhiteboardTool.STAR,
                            WhiteboardTool.ELLIPSE,
                            WhiteboardTool.SQUARE,
                            WhiteboardTool.HEXAGON,
                            WhiteboardTool.ROUND_RECT,
                            WhiteboardTool.CUBE,
                            WhiteboardTool.CYLINDER,
                            WhiteboardTool.CONE,
                            WhiteboardTool.PYRAMID,
                            WhiteboardTool.SHAPE -> {
                                val sType = when (activeTool) {
                                    WhiteboardTool.RECTANGLE -> StrokeType.RECTANGLE
                                    WhiteboardTool.DIAMOND -> StrokeType.DIAMOND
                                    WhiteboardTool.CIRCLE -> StrokeType.CIRCLE
                                    WhiteboardTool.ARROW -> StrokeType.ARROW
                                    WhiteboardTool.LINE -> StrokeType.LINE
                                    WhiteboardTool.FRAME -> StrokeType.FRAME
                                    WhiteboardTool.TRIANGLE -> StrokeType.TRIANGLE
                                    WhiteboardTool.STAR -> StrokeType.STAR
                                    WhiteboardTool.ELLIPSE -> StrokeType.ELLIPSE
                                    WhiteboardTool.SQUARE -> StrokeType.SQUARE
                                    WhiteboardTool.HEXAGON -> StrokeType.HEXAGON
                                    WhiteboardTool.ROUND_RECT -> StrokeType.ROUND_RECT
                                    WhiteboardTool.CUBE -> StrokeType.CUBE
                                    WhiteboardTool.CYLINDER -> StrokeType.CYLINDER
                                    WhiteboardTool.CONE -> StrokeType.CONE
                                    WhiteboardTool.PYRAMID -> StrokeType.PYRAMID
                                    WhiteboardTool.SHAPE -> state.activeShapeType
                                    else -> StrokeType.PEN
                                }
                                val newStroke = Stroke(
                                    points = listOf(downWorld, downWorld),
                                    colorValue = if (activeTool == WhiteboardTool.SHAPE) state.shapeBorderColor else activeInkColor,
                                    fillColor = if (activeTool == WhiteboardTool.SHAPE) state.shapeFillColor else state.fillColor,
                                    strokeWidth = if (activeTool == WhiteboardTool.SHAPE) state.strokeWidth else activeInkWidth,
                                    strokeType = sType,
                                    opacity = if (activeTool == WhiteboardTool.SHAPE) state.shapeOpacity else 1f,
                                    cornerRadius = if (activeTool == WhiteboardTool.SHAPE) state.shapeCornerRadius else 0f,
                                    showDimensions = if (activeTool == WhiteboardTool.SHAPE) state.showShapeDimensions else false
                                )
                                shapePoints = newStroke.points
                                viewModel.startStroke(newStroke)
                            }
                            else -> {}
                        }

                        do {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.filter { it.pressed }
                            val anyStylusPressed = pressed.any { it.isStylusPointer() }
                            val realPressed = pressed.filter { change ->
                                if (change.rejectedPalm(stylusDrawing = stylusDraw || anyStylusPressed)) {
                                    change.consume()
                                    false
                                } else {
                                    true
                                }
                            }
                            fingerScratch.clear()
                            for (change in realPressed) {
                                if (!change.isStylusPointer()) fingerScratch.add(change)
                            }
                            if (fingerScratch.size > fingerPeak) fingerPeak = fingerScratch.size
                            val drawsImmediately = activeTool == WhiteboardTool.PENCIL ||
                                activeTool == WhiteboardTool.MATH_PEN ||
                                activeTool == WhiteboardTool.ERASER
                            val multiFingers = !drawsImmediately &&
                                !touchContacts.stylusActive &&
                                fingerScratch.size >= 2
                            if (multiFingers) {
                                if (!wasMultiTouch) {
                                    wasMultiTouch = true
                                    liveInk.stroke = null
                                    liveInk.predicted = null
                                    boardStage?.setLiveInk(null)
                                    viewModel.cancelCurrentStroke()
                                    rulerLivePreview = null
                                }
                                var sumX = 0f
                                var sumY = 0f
                                for (finger in fingerScratch) {
                                    sumX += finger.position.x
                                    sumY += finger.position.y
                                }
                                val fingerCenter = Offset(sumX / fingerScratch.size, sumY / fingerScratch.size)
                                val origin = gestureStart ?: fingerCenter
                                gestureStart = origin
                                gestureEnd = fingerCenter
                                val travel = centroidTravel(origin.x, origin.y, fingerCenter.x, fingerCenter.y)
                                if (travel > gestureTravel) gestureTravel = travel
                                val centroid = event.calculateCentroid(useCurrent = true)
                                val pinchedTool = if (fingerScratch.size != 2) {
                                    null
                                } else {
                                    mathGesture.toolId?.let { id -> mathTools.find { it.id == id } }
                                        ?: hitTestPlacedTools(mathTools, centroid)?.tool
                                }
                                if (pinchedTool != null) {
                                    mathGesture.twoFinger = true
                                    mathGesture.rulerDrawing = false
                                    mathGesture.arcMode = false
                                    viewModel.cancelCurrentStroke()
                                    rulerLivePreview = null
                                    val pan = event.calculatePan()
                                    val rotation = event.calculateRotation()
                                    val zoom = event.calculateZoom()
                                    val newSpan = if (zoom != 1f && (pinchedTool.kind == MathToolKind.RULER || pinchedTool.kind == MathToolKind.COMPASS)) {
                                        (pinchedTool.span * zoom).coerceIn(180f, 3600f)
                                    } else pinchedTool.span
                                    val updated = translateAndRotate(
                                        pinchedTool.copy(span = newSpan),
                                        pan,
                                        rotation
                                    )
                                    replaceMathTool(updated)
                                    if (updated.kind == MathToolKind.RULER) {
                                        val deg = ((updated.rotationDeg % 360f + 360f) % 360f).roundToInt()
                                        mathReadout = "${formatCentimetres(updated.span, pxPerCm)} • $deg°" to centroid + Offset(0f, -42f)
                                    } else {
                                        mathReadout = null
                                    }
                                    event.changes.forEach { it.consume() }
                                } else if (fingerScratch.size == 2) {
                                val zoomChange = event.calculateZoom()
                                val panChange = event.calculatePan()

                                if (zoomChange != 1f || panChange != Offset.Zero) {
                                    val newScale = (gestureScale * zoomChange).coerceIn(
                                        CanvasTransform.MIN_SCALE,
                                        CanvasTransform.MAX_SCALE
                                    )
                                    val newOffset = CanvasTransform.calculatePivotZoomOffset(
                                        oldScale = gestureScale,
                                        newScale = newScale,
                                        oldOffset = Offset(gestureOffsetX, gestureOffsetY),
                                        centroid = centroid,
                                        panDelta = panChange
                                    )
                                    gestureScale = newScale
                                    gestureOffsetX = newOffset.x
                                    gestureOffsetY = newOffset.y
                                    transformMoved = true
                                    boardStage?.previewTransform(gestureScale, gestureOffsetX, gestureOffsetY)
                                }
                                event.changes.forEach { it.consume() }
                                } else {
                                    event.changes.forEach { it.consume() }
                                }
                            } else {
                                val change = if (drawsImmediately) {
                                    realPressed.firstOrNull { it.id == down.id }
                                } else {
                                    realPressed.firstOrNull { pointer ->
                                        val tool = touchContacts.toolType(pointer.id)
                                        tool == MotionEvent.TOOL_TYPE_STYLUS ||
                                            tool == MotionEvent.TOOL_TYPE_ERASER ||
                                            pointer.type == PointerType.Stylus ||
                                            pointer.type == PointerType.Eraser
                                    } ?: realPressed.firstOrNull()
                                }
                                if (change != null) {
                                    val curWorld = CanvasTransform.screenToWorld(
                                        screenPoint = change.position,
                                        scale = canvasScale,
                                        offset = Offset(canvasOffsetX, canvasOffsetY)
                                    )

                                    if (mathGesture.toolId != null && !mathGesture.rulerDrawing) {
                                        val tool = mathTools.find { it.id == mathGesture.toolId }
                                        if (tool != null) {
                                            val dx = change.position.x - prevPosition.x
                                            val dy = change.position.y - prevPosition.y
                                            mathGesture.movedPx += Offset(dx, dy).getDistance()
                                            when (mathGesture.zone) {
                                                MathToolZone.MOVE -> replaceMathTool(tool.copy(center = Offset(tool.center.x + dx, tool.center.y + dy)))
                                                MathToolZone.ROTATE -> {
                                                    val radToDeg = 180f / Math.PI.toFloat()
                                                    val curAngle = atan2(change.position.y - tool.center.y, change.position.x - tool.center.x) * radToDeg
                                                    val delta = shortestAngleDelta(mathGesture.lastAngleDeg, curAngle)
                                                    mathGesture.lastAngleDeg = curAngle
                                                    val next = tool.copy(rotationDeg = tool.rotationDeg + delta)
                                                    replaceMathTool(next)
                                                    if (tool.kind == MathToolKind.COMPASS) {
                                                        mathGesture.sweepDeg += delta
                                                        if (kotlin.math.abs(mathGesture.sweepDeg) >= ARC_ANGLE_SLOP) {
                                                            mathGesture.arcMode = true
                                                            val worldPts = screenPointsToWorld(
                                                                compassArcScreenPoints(next, mathGesture.startAngleDeg, mathGesture.sweepDeg),
                                                                canvasScale,
                                                                Offset(canvasOffsetX, canvasOffsetY)
                                                            )
                                                            val arc = arcStroke(worldPts, state.selectedColor, state.strokeWidth)
                                                            if (viewModel.state.value.currentStroke == null) {
                                                                viewModel.startStroke(arc)
                                                            } else {
                                                                viewModel.updateStroke(worldPts)
                                                            }
                                                        }
                                                    }
                                                }
                                                MathToolZone.ANGLE_ARM -> {
                                                    val local = toLocal(change.position, tool.center, tool.rotationDeg)
                                                    var angle = atan2(-local.y, local.x) * (180f / Math.PI.toFloat())
                                                    if (angle < 0f) angle += 360f
                                                    val clamped = (kotlin.math.round(angle.coerceIn(0f, 180f) * 10f) / 10f)
                                                    replaceMathTool(tool.copy(pointerAngleDeg = clamped))
                                                }
                                                MathToolZone.SPREAD -> {
                                                    val newRadius = hypot(change.position.x - tool.center.x, change.position.y - tool.center.y)
                                                        .coerceIn(COMPASS_MIN_RADIUS, COMPASS_MAX_RADIUS)
                                                    replaceMathTool(tool.copy(span = newRadius))
                                                }
                                                MathToolZone.ROTATE_POSITIVE, MathToolZone.ROTATE_NEGATIVE -> {
                                                    val updated = if (tool.kind == MathToolKind.RULER) {
                                                        dragRulerEnd(tool, change.position, mathGesture.draggingPositiveEnd)
                                                    } else {
                                                        rotateToolByEnd(tool, change.position, mathGesture.draggingPositiveEnd)
                                                    }
                                                    replaceMathTool(updated)
                                                    val deg = ((updated.rotationDeg % 360f + 360f) % 360f).roundToInt()
                                                    val text = if (tool.kind == MathToolKind.RULER) {
                                                        "${formatCentimetres(updated.span, pxPerCm)} • $deg°"
                                                    } else {
                                                        "$deg°"
                                                    }
                                                    mathReadout = text to change.position + Offset(0f, -38f)
                                                }
                                                MathToolZone.PENCIL -> {
                                                    if (tool.kind == MathToolKind.COMPASS) {
                                                        val next = dragCompassPencil(tool, change.position, mathGesture)
                                                        replaceMathTool(next)
                                                        if (mathGesture.arcMode) {
                                                            val worldPts = screenPointsToWorld(
                                                                compassArcScreenPoints(next, mathGesture.startAngleDeg, mathGesture.sweepDeg),
                                                                canvasScale,
                                                                Offset(canvasOffsetX, canvasOffsetY)
                                                            )
                                                            val arc = arcStroke(worldPts, state.selectedColor, state.strokeWidth)
                                                            if (viewModel.state.value.currentStroke == null) {
                                                                viewModel.startStroke(arc)
                                                            } else {
                                                                viewModel.updateStroke(worldPts)
                                                            }
                                                        }
                                                    }
                                                }
                                                MathToolZone.SCALE -> {
                                                    val curDist = hypot(change.position.x - tool.center.x, change.position.y - tool.center.y)
                                                    val ratio = if (mathGesture.startDist > 10f) curDist / mathGesture.startDist else 1f
                                                    if (tool.kind == MathToolKind.COMPASS) {
                                                        val newScale = (mathGesture.startToolScale * ratio).coerceIn(0.6f, 3.5f)
                                                        replaceMathTool(tool.copy(toolScale = newScale))
                                                    } else {
                                                        val maxSpan = when (tool.kind) {
                                                            MathToolKind.SET_SQUARE -> 1000f
                                                            MathToolKind.TRIANGLE -> 900f
                                                            MathToolKind.PROTRACTOR -> 800f
                                                            MathToolKind.RULER -> 1800f
                                                            else -> 800f
                                                        }
                                                        val minSpan = when (tool.kind) {
                                                            MathToolKind.RULER -> 200f
                                                            else -> 120f
                                                        }
                                                        val newSpan = (mathGesture.startSpan * ratio).coerceIn(minSpan, maxSpan)
                                                        replaceMathTool(tool.copy(span = newSpan))
                                                    }
                                                }
                                                MathToolZone.CLOSE, MathToolZone.ANGLE_MODE_1, MathToolZone.ANGLE_MODE_ARC_LEFT, MathToolZone.ANGLE_MODE_ARC, MathToolZone.ANGLE_MODE_2, MathToolZone.SIDE_1_PENCIL, MathToolZone.SIDE_2_PENCIL, MathToolZone.SIDE_3_PENCIL, MathToolZone.NONE -> Unit
                                            }
                                        }
                                    } else if (mathGesture.rulerDrawing) {
                                        val ruler = mathTools.find { it.id == mathGesture.toolId }
                                        if (ruler != null && shapePoints.isNotEmpty()) {
                                            val snappedStartWorld = shapePoints.first()
                                            val snappedCurrentScreen = snapToRulerEdge(ruler, change.position, mathGesture.rulerEdgeSign)
                                            val snappedCurrentWorld = CanvasTransform.screenToWorld(
                                                snappedCurrentScreen,
                                                canvasScale,
                                                Offset(canvasOffsetX, canvasOffsetY)
                                            )
                                            shapePoints = listOf(snappedStartWorld, snappedCurrentWorld)
                                            viewModel.updateStroke(shapePoints)

                                            val strokeColor = if (state.selectedColor != 0) state.selectedColor else (if (state.isDarkCanvas) 0xFFFFFFFF.toInt() else 0xFF1E293B.toInt())
                                            val strokeWidth = if (state.strokeWidth > 0f) state.strokeWidth else 6f
                                            val liveStroke = Stroke(
                                                id = viewModel.state.value.currentStroke?.id ?: "ruler_live",
                                                points = shapePoints,
                                                colorValue = strokeColor,
                                                strokeWidth = strokeWidth,
                                                isHighlighter = activeTool == WhiteboardTool.HIGHLIGHTER,
                                                strokeType = if (activeTool == WhiteboardTool.HIGHLIGHTER) StrokeType.HIGHLIGHTER else StrokeType.LINE
                                            )
                                            liveInk.stroke = liveStroke
                                            liveInkTick.intValue = liveInkTick.intValue + 1
                                            boardStage?.setLiveInk(liveInk)

                                            val snappedStartScreen = CanvasTransform.worldToScreen(
                                                snappedStartWorld,
                                                canvasScale,
                                                Offset(canvasOffsetX, canvasOffsetY)
                                            )
                                            rulerLivePreview = snappedStartScreen to snappedCurrentScreen

                                            val segLengthPx = (snappedCurrentScreen - snappedStartScreen).getDistance()
                                            val lengthText = formatCentimetres(segLengthPx, pxPerCm)

                                            val midScreen = (snappedStartScreen + snappedCurrentScreen) / 2f
                                            val perp = perpendicular(ruler.rotationDeg)
                                            val readoutPos = midScreen + perp * (mathGesture.rulerEdgeSign * 38f)
                                            mathReadout = "Line: $lengthText" to readoutPos
                                        }
                                    } else if (activeTool == WhiteboardTool.HAND) {
                                        val panX = change.position.x - prevPosition.x
                                        val panY = change.position.y - prevPosition.y
                                        canvasOffsetX += panX
                                        canvasOffsetY += panY
                                        transformMoved = true
                                        boardStage?.setTransform(canvasScale, canvasOffsetX, canvasOffsetY)
                                    } else if (resizeStrokeId != null && resizeHandle != null) {
                                        viewModel.beginSelectionMove()
                                        viewModel.resizeStroke(resizeStrokeId, resizeHandle, curWorld)
                                    } else if (activeTool == WhiteboardTool.SELECT && selectingBox) {
                                        boardStage?.setMarquee(downWorld, curWorld)
                                    } else if (activeTool == WhiteboardTool.SELECT && isDraggingSelection) {
                                        val prevWorld = CanvasTransform.screenToWorld(
                                            screenPoint = prevPosition,
                                            scale = canvasScale,
                                            offset = Offset(canvasOffsetX, canvasOffsetY)
                                        )
                                        val delta = Offset(curWorld.x - prevWorld.x, curWorld.y - prevWorld.y)
                                        viewModel.beginSelectionMove()
                                        viewModel.moveSelectedStrokes(delta)
                                    } else if (activeTool == WhiteboardTool.LASER) {
                                        viewModel.addLaserPoint(curWorld)
                                    } else if (activeTool == WhiteboardTool.LASSO) {
                                        lassoPoints = lassoPoints + curWorld
                                    } else if (activeTool == WhiteboardTool.ERASER) {
                                        val curPalmRadius = if (isPalmEraser) {
                                            val curMajor = touchContacts.majorPx(change.id)
                                            if (curMajor > 0f) (curMajor / 2f) / canvasScale.coerceAtLeast(0.001f) else customEraserRadius
                                        } else {
                                            customEraserRadius
                                        }
                                        eraseSample(curWorld, change.position, curPalmRadius)
                                    } else if (activeTool == WhiteboardTool.SPOTLIGHT) {
                                        spotlightCenter = change.position
                                    } else if (activeTool == WhiteboardTool.SHADE) {
                                        val height = canvasSize.height.coerceAtLeast(1f)
                                        shadeEdge = (change.position.y / height).coerceIn(0.05f, 0.95f)
                                    } else if (activeTool == WhiteboardTool.MAGNIFIER) {
                                        boardStage?.showMagnifier(change.position.x, change.position.y)
                                    } else if (writingNow) {
                                        if (stylusDraw) {
                                            val pressure = stylusPressure(change.pressure)
                                            change.historical.forEach { sample ->
                                                appendInk(sample.position, pressure)
                                            }
                                            appendInk(change.position, pressure)
                                        } else {
                                            change.pastPositions().forEach { appendInk(it, change.inkPressure()) }
                                            appendInk(change.position, change.inkPressure())
                                        }
                                        publishInk()
                                    } else if (isShapeActive || activeTool in listOf(
                                            WhiteboardTool.RECTANGLE,
                                            WhiteboardTool.DIAMOND,
                                            WhiteboardTool.CIRCLE,
                                            WhiteboardTool.ARROW,
                                            WhiteboardTool.LINE,
                                            WhiteboardTool.FRAME,
                                            WhiteboardTool.TRIANGLE,
                                            WhiteboardTool.STAR,
                                            WhiteboardTool.ELLIPSE,
                                            WhiteboardTool.SQUARE,
                                            WhiteboardTool.HEXAGON,
                                            WhiteboardTool.ROUND_RECT,
                                            WhiteboardTool.CUBE,
                                            WhiteboardTool.CYLINDER,
                                            WhiteboardTool.CONE,
                                            WhiteboardTool.PYRAMID,
                                            WhiteboardTool.SHAPE
                                        )
                                    ) {
                                        val sType = when (activeTool) {
                                            WhiteboardTool.RECTANGLE -> StrokeType.RECTANGLE
                                            WhiteboardTool.DIAMOND -> StrokeType.DIAMOND
                                            WhiteboardTool.CIRCLE -> StrokeType.CIRCLE
                                            WhiteboardTool.ARROW -> StrokeType.ARROW
                                            WhiteboardTool.LINE -> StrokeType.LINE
                                            WhiteboardTool.FRAME -> StrokeType.FRAME
                                            WhiteboardTool.TRIANGLE -> StrokeType.TRIANGLE
                                            WhiteboardTool.STAR -> StrokeType.STAR
                                            WhiteboardTool.ELLIPSE -> StrokeType.ELLIPSE
                                            WhiteboardTool.SQUARE -> StrokeType.SQUARE
                                            WhiteboardTool.HEXAGON -> StrokeType.HEXAGON
                                            WhiteboardTool.ROUND_RECT -> StrokeType.ROUND_RECT
                                            WhiteboardTool.CUBE -> StrokeType.CUBE
                                            WhiteboardTool.CYLINDER -> StrokeType.CYLINDER
                                            WhiteboardTool.CONE -> StrokeType.CONE
                                            WhiteboardTool.PYRAMID -> StrokeType.PYRAMID
                                            WhiteboardTool.SHAPE -> state.activeShapeType
                                            else -> StrokeType.PEN
                                        }
                                        val endPoint = if (sType in lockedAspect) lockToSquare(downWorld, curWorld) else curWorld
                                        shapePoints = listOf(downWorld, endPoint)
                                        viewModel.updateStroke(shapePoints)
                                    }
                                    prevPosition = change.position
                                    change.consume()
                                }
                            }
                        } while (event.changes.any { it.pressed })

                        val moved = kotlin.math.hypot(
                            (prevPosition.x - down.position.x).toDouble(),
                            (prevPosition.y - down.position.y).toDouble()
                        ).toFloat()

                        if (selectingBox) {
                            boardStage?.setMarquee(null, null)
                            if (moved < 14f) {
                                val clickedStroke = viewModel.strokeAt(downWorld)
                                if (clickedStroke != null) {
                                    viewModel.selectStrokeAt(downWorld)
                                } else {
                                    viewModel.clearSelection()
                                }
                            } else {
                                val endWorld = CanvasTransform.screenToWorld(
                                    prevPosition,
                                    canvasScale,
                                    Offset(canvasOffsetX, canvasOffsetY)
                                )
                                viewModel.selectInRect(
                                    androidx.compose.ui.geometry.Rect(
                                        minOf(downWorld.x, endWorld.x),
                                        minOf(downWorld.y, endWorld.y),
                                        maxOf(downWorld.x, endWorld.x),
                                        maxOf(downWorld.y, endWorld.y)
                                    )
                                )
                            }
                        } else if (isDraggingSelection && moved < 14f) {
                            // Tapping an already-selected shape re-opens the toolbar if it was closed
                            selectionToolbarDismissed = false
                        }
                        viewModel.endSelectionMove()

                        if (activeTool == WhiteboardTool.MAGNIFIER) {
                            boardStage?.hideMagnifier()
                        }
                        if (transformMoved) {
                            canvasScale = gestureScale
                            canvasOffsetX = gestureOffsetX
                            canvasOffsetY = gestureOffsetY
                            viewModel.updateTransform(canvasScale, canvasOffsetX, canvasOffsetY)
                            boardStage?.commitPreview()
                        }
                        if (wasMultiTouch) {
                            val start = gestureStart
                            if (start != null) {
                                val gesture = classifyPanelGesture(
                                    fingerPeak,
                                    gestureTravel,
                                    gestureEnd.x - start.x,
                                    gestureEnd.y - start.y
                                )
                                if (gesture == PanelGesture.AI_TAP) {
                                    showAiPanel = !showAiPanel
                                } else if (gesture == PanelGesture.PAGE) {
                                    val delta = swipePageDelta(
                                        gestureEnd.x - start.x,
                                        gestureEnd.y - start.y,
                                        threshold = 90f
                                    )
                                    val latest = viewModel.state.value
                                    val next = pageStep(latest.pageIndex, latest.pageCount, delta)
                                    if (delta != 0 && next != latest.pageIndex) viewModel.selectPage(next)
                                }
                            }
                        }
                        if (eraserSession.active) {
                            boardStage?.clearGestureStrokes()
                            if (eraserSession.changed) {
                                viewModel.commitErasedStrokes(eraserSession.strokes)
                                val latest = viewModel.state.value
                                if (latest.selectedStrokeIds.isNotEmpty()) {
                                    viewModel.selectStrokes(eraserSession.targetSelectedIds)
                                }
                            }
                            eraserSession.active = false
                            eraserSession.changed = false
                            eraserSession.cursor = null
                            eraserSession.targetSelectedIds.clear()
                            eraserTick.intValue = eraserTick.intValue + 1
                        }

                        // Release Touch
                        if (!wasMultiTouch && mathGesture.rulerDrawing && shapePoints.size >= 2) {
                            val start = shapePoints.first()
                            val end = shapePoints.last()
                            val lengthPx = screenLengthPx(start, end, canvasScale)
                            if (lengthPx > 8f) {
                                viewModel.updateStroke(shapePoints)
                                viewModel.finishStroke()

                                val ruler = mathTools.find { it.id == mathGesture.toolId }
                                val perp = if (ruler != null) {
                                    perpendicular(ruler.rotationDeg) * mathGesture.rulerEdgeSign
                                } else {
                                    Offset(-mathGesture.drawDir.y, mathGesture.drawDir.x)
                                }

                                val mid = Offset((start.x + end.x) / 2f, (start.y + end.y) / 2f)
                                val labelOffsetWorld = perp * (28f / canvasScale.coerceAtLeast(0.001f))
                                val labelAt = mid + labelOffsetWorld

                                val lengthText = formatCentimetres(lengthPx, pxPerCm)
                                val labelColor = if (state.selectedColor != 0) state.selectedColor else (if (state.isDarkCanvas) 0xFFFFFFFF.toInt() else 0xFF1E293B.toInt())
                                viewModel.commitStroke(
                                    measurementLabel(
                                        lengthText,
                                        labelAt,
                                        labelColor
                                    )
                                )
                                Toast.makeText(context, "Drawn: $lengthText", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.cancelCurrentStroke()
                            }
                            liveInk.stroke = null
                            liveInkTick.intValue = liveInkTick.intValue + 1
                            boardStage?.setLiveInk(null)
                            rulerLivePreview = null
                            mathReadout = null
                            mathGesture.rulerDrawing = false
                        } else if (!wasMultiTouch && mathGesture.toolId != null) {
                            mathReadout = null
                            rulerLivePreview = null
                            val tool = mathTools.find { it.id == mathGesture.toolId }
                            val finger = canvasOrigin + prevPosition
                            if (tool != null && mathGesture.movedPx > MATH_TAP_SLOP && droppedOnToolbar(finger, chromeBounds.dock)) {
                                viewModel.cancelCurrentStroke()
                                removeMathTool(tool.id)
                                Toast.makeText(context, "Tool removed", Toast.LENGTH_SHORT).show()
                            } else {
                                if (tool != null && mathGesture.movedPx > MATH_TAP_SLOP) {
                                    viewModel.setMathTools(mathTools)
                                }
                                if (tool != null && mathGesture.zone == MathToolZone.CLOSE && mathGesture.movedPx <= MATH_TAP_SLOP) {
                                viewModel.cancelCurrentStroke()
                                removeMathTool(tool.id)
                                val toolName = tool.kind.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
                                Toast.makeText(context, "$toolName closed", Toast.LENGTH_SHORT).show()
                            } else if (tool != null && tool.kind == MathToolKind.SET_SQUARE && (mathGesture.zone == MathToolZone.PENCIL || mathGesture.zone == MathToolZone.SIDE_1_PENCIL || mathGesture.zone == MathToolZone.SIDE_2_PENCIL || mathGesture.zone == MathToolZone.SIDE_3_PENCIL) && mathGesture.movedPx <= MATH_TAP_SLOP) {
                                val half = tool.span / 2f
                                val dir = direction(tool.rotationDeg)
                                val perp = perpendicular(tool.rotationDeg)
                                val pCorner = tool.center - dir * half + perp * half
                                val pBase = tool.center + dir * half + perp * half
                                val pTop = tool.center - dir * half - perp * half
                                val offset = Offset(canvasOffsetX, canvasOffsetY)
                                val wCorner = CanvasTransform.screenToWorld(pCorner, canvasScale, offset)
                                val wBase = CanvasTransform.screenToWorld(pBase, canvasScale, offset)
                                val wTop = CanvasTransform.screenToWorld(pTop, canvasScale, offset)

                                val legCm = tool.span / pxPerCm.coerceAtLeast(1f)
                                val baseLabel = String.format(Locale.US, "%.1f cm", legCm)
                                val heightLabel = String.format(Locale.US, "%.1f cm", legCm)
                                val hypLabel = String.format(Locale.US, "%.1f cm", legCm * 1.4142f)

                                val midBase = Offset((wCorner.x + wBase.x) / 2f, (wCorner.y + wBase.y) / 2f)
                                val midHeight = Offset((wCorner.x + wTop.x) / 2f, (wCorner.y + wTop.y) / 2f)
                                val midHyp = Offset((wTop.x + wBase.x) / 2f, (wTop.y + wBase.y) / 2f)

                                when (mathGesture.zone) {
                                    MathToolZone.SIDE_1_PENCIL -> {
                                        viewModel.commitStroke(straightStroke(wCorner, wBase, state.selectedColor, state.strokeWidth))
                                        viewModel.commitStroke(measurementLabel(baseLabel, midBase, state.selectedColor))
                                        Toast.makeText(context, "Base line ($baseLabel) drawn", Toast.LENGTH_SHORT).show()
                                    }
                                    MathToolZone.SIDE_2_PENCIL -> {
                                        viewModel.commitStroke(straightStroke(wCorner, wTop, state.selectedColor, state.strokeWidth))
                                        viewModel.commitStroke(measurementLabel(heightLabel, midHeight, state.selectedColor))
                                        Toast.makeText(context, "Height line ($heightLabel) drawn", Toast.LENGTH_SHORT).show()
                                    }
                                    MathToolZone.SIDE_3_PENCIL -> {
                                        viewModel.commitStroke(straightStroke(wTop, wBase, state.selectedColor, state.strokeWidth))
                                        viewModel.commitStroke(measurementLabel(hypLabel, midHyp, state.selectedColor))
                                        Toast.makeText(context, "Hypotenuse line ($hypLabel) drawn", Toast.LENGTH_SHORT).show()
                                    }
                                    MathToolZone.PENCIL -> {
                                        val pts = listOf(wCorner, wBase, wTop, wCorner)
                                        viewModel.commitStroke(
                                            Stroke(
                                                points = pts,
                                                colorValue = state.selectedColor,
                                                strokeWidth = state.strokeWidth,
                                                strokeType = StrokeType.TRIANGLE
                                            )
                                        )
                                        viewModel.commitStroke(measurementLabel(baseLabel, midBase, state.selectedColor))
                                        viewModel.commitStroke(measurementLabel(heightLabel, midHeight, state.selectedColor))
                                        viewModel.commitStroke(measurementLabel(hypLabel, midHyp, state.selectedColor))
                                        Toast.makeText(context, "Set square (45°-90°-45°) with measurements drawn", Toast.LENGTH_SHORT).show()
                                    }
                                    else -> Unit
                                }
                            } else if (tool != null && tool.kind == MathToolKind.TRIANGLE && (mathGesture.zone == MathToolZone.PENCIL || mathGesture.zone == MathToolZone.SIDE_1_PENCIL || mathGesture.zone == MathToolZone.SIDE_2_PENCIL || mathGesture.zone == MathToolZone.SIDE_3_PENCIL) && mathGesture.movedPx <= MATH_TAP_SLOP) {
                                val side = tool.span * 1.8f
                                val h = side * 0.8660254f
                                val dir = direction(tool.rotationDeg)
                                val perp = perpendicular(tool.rotationDeg)
                                val topScreen = tool.center - perp * (h * 0.6667f)
                                val leftScreen = tool.center - dir * (side / 2f) + perp * (h * 0.3333f)
                                val rightScreen = tool.center + dir * (side / 2f) + perp * (h * 0.3333f)
                                val offset = Offset(canvasOffsetX, canvasOffsetY)
                                val wTop = CanvasTransform.screenToWorld(topScreen, canvasScale, offset)
                                val wLeft = CanvasTransform.screenToWorld(leftScreen, canvasScale, offset)
                                val wRight = CanvasTransform.screenToWorld(rightScreen, canvasScale, offset)

                                val sideCm = side / pxPerCm.coerceAtLeast(1f)
                                val sideLabel = String.format(Locale.US, "%.1f cm", sideCm)

                                val midBottom = Offset((wLeft.x + wRight.x) / 2f, (wLeft.y + wRight.y) / 2f)
                                val midLeft = Offset((wTop.x + wLeft.x) / 2f, (wTop.y + wLeft.y) / 2f)
                                val midRight = Offset((wTop.x + wRight.x) / 2f, (wTop.y + wRight.y) / 2f)

                                when (mathGesture.zone) {
                                    MathToolZone.SIDE_1_PENCIL -> {
                                        viewModel.commitStroke(straightStroke(wLeft, wRight, state.selectedColor, state.strokeWidth))
                                        viewModel.commitStroke(measurementLabel(sideLabel, midBottom, state.selectedColor))
                                        Toast.makeText(context, "Side A ($sideLabel) drawn", Toast.LENGTH_SHORT).show()
                                    }
                                    MathToolZone.SIDE_2_PENCIL -> {
                                        viewModel.commitStroke(straightStroke(wLeft, wTop, state.selectedColor, state.strokeWidth))
                                        viewModel.commitStroke(measurementLabel(sideLabel, midLeft, state.selectedColor))
                                        Toast.makeText(context, "Side B ($sideLabel) drawn", Toast.LENGTH_SHORT).show()
                                    }
                                    MathToolZone.SIDE_3_PENCIL -> {
                                        viewModel.commitStroke(straightStroke(wTop, wRight, state.selectedColor, state.strokeWidth))
                                        viewModel.commitStroke(measurementLabel(sideLabel, midRight, state.selectedColor))
                                        Toast.makeText(context, "Side C ($sideLabel) drawn", Toast.LENGTH_SHORT).show()
                                    }
                                    MathToolZone.PENCIL -> {
                                        val pts = listOf(wTop, wLeft, wRight, wTop)
                                        viewModel.commitStroke(
                                            Stroke(
                                                points = pts,
                                                colorValue = state.selectedColor,
                                                strokeWidth = state.strokeWidth,
                                                strokeType = StrokeType.TRIANGLE
                                            )
                                        )
                                        viewModel.commitStroke(measurementLabel(sideLabel, midBottom, state.selectedColor))
                                        viewModel.commitStroke(measurementLabel(sideLabel, midLeft, state.selectedColor))
                                        viewModel.commitStroke(measurementLabel(sideLabel, midRight, state.selectedColor))
                                        Toast.makeText(context, "Triangle (60°-60°-60°) with measurements drawn", Toast.LENGTH_SHORT).show()
                                    }
                                    else -> Unit
                                }
                            } else if (tool != null && tool.kind == MathToolKind.PROTRACTOR &&
                                (mathGesture.zone == MathToolZone.ANGLE_MODE_1 ||
                                 mathGesture.zone == MathToolZone.ANGLE_MODE_ARC_LEFT ||
                                 mathGesture.zone == MathToolZone.ANGLE_MODE_ARC ||
                                 mathGesture.zone == MathToolZone.ANGLE_MODE_2) &&
                                mathGesture.movedPx <= MATH_TAP_SLOP
                            ) {
                                val pDeg = tool.pointerAngleDeg
                                val rad = Math.toRadians(pDeg.toDouble()).toFloat()
                                val dir = direction(tool.rotationDeg)
                                val perp = perpendicular(tool.rotationDeg)
                                val armDir = dir * cos(rad) - perp * sin(rad)
                                val armScreen = tool.center + armDir * tool.span
                                val rightBaseScreen = tool.center + dir * tool.span
                                val leftBaseScreen = tool.center - dir * tool.span
                                val offset = Offset(canvasOffsetX, canvasOffsetY)
                                val centerWorld = CanvasTransform.screenToWorld(tool.center, canvasScale, offset)
                                val armWorld = CanvasTransform.screenToWorld(armScreen, canvasScale, offset)
                                val rightBaseWorld = CanvasTransform.screenToWorld(rightBaseScreen, canvasScale, offset)
                                val leftBaseWorld = CanvasTransform.screenToWorld(leftBaseScreen, canvasScale, offset)
                                when (mathGesture.zone) {
                                    MathToolZone.ANGLE_MODE_1 -> {
                                        // Left Angle: from Left baseline (180°) to arm
                                        val leftAngleDeg = (180f - pDeg).coerceIn(0f, 180f)
                                        viewModel.commitStroke(straightStroke(centerWorld, leftBaseWorld, state.selectedColor, state.strokeWidth))
                                        viewModel.commitStroke(straightStroke(centerWorld, armWorld, state.selectedColor, state.strokeWidth))
                                        val arcScreenPoints = mutableListOf<Offset>()
                                        val steps = (leftAngleDeg / 3f).roundToInt().coerceAtLeast(8)
                                        val arcR = tool.span * 0.45f
                                        for (i in 0..steps) {
                                            val deg = 180f - (leftAngleDeg * i) / steps
                                            val r = Math.toRadians(deg.toDouble()).toFloat()
                                            val d = dir * cos(r) - perp * sin(r)
                                            arcScreenPoints.add(tool.center + d * arcR)
                                        }
                                        viewModel.commitStroke(
                                            arcStroke(
                                                screenPointsToWorld(arcScreenPoints, canvasScale, offset),
                                                state.selectedColor,
                                                (state.strokeWidth * 0.85f).coerceAtLeast(2f)
                                            )
                                        )
                                        val midRad = Math.toRadians((180.0 - leftAngleDeg / 2.0)).toFloat()
                                        val midD = dir * cos(midRad) - perp * sin(midRad)
                                        val labelScreen = tool.center + midD * (arcR + 32f) - perp * 26f
                                        viewModel.commitStroke(
                                            measurementLabel("${leftAngleDeg.roundToInt()}°", CanvasTransform.screenToWorld(labelScreen, canvasScale, offset), state.selectedColor)
                                        )
                                        Toast.makeText(context, "Left Angle ${leftAngleDeg.roundToInt()}° drawn", Toast.LENGTH_SHORT).show()
                                    }
                                    MathToolZone.ANGLE_MODE_ARC_LEFT -> {
                                        // Left Arc: circular arc only from Left baseline (180°) down to arm
                                        val leftAngleDeg = (180f - pDeg).coerceIn(0f, 180f)
                                        val arcScreenPoints = mutableListOf<Offset>()
                                        val steps = (leftAngleDeg / 2.5f).roundToInt().coerceAtLeast(8)
                                        for (i in 0..steps) {
                                            val deg = 180f - (leftAngleDeg * i) / steps
                                            val r = Math.toRadians(deg.toDouble()).toFloat()
                                            val d = dir * cos(r) - perp * sin(r)
                                            arcScreenPoints.add(tool.center + d * tool.span)
                                        }
                                        viewModel.commitStroke(
                                            arcStroke(
                                                screenPointsToWorld(arcScreenPoints, canvasScale, offset),
                                                state.selectedColor,
                                                state.strokeWidth
                                            )
                                        )
                                        val midRad = Math.toRadians((180.0 - leftAngleDeg / 2.0)).toFloat()
                                        val midD = dir * cos(midRad) - perp * sin(midRad)
                                        val labelScreen = tool.center + midD * (tool.span + 36f) - perp * 24f
                                        viewModel.commitStroke(
                                            measurementLabel("${leftAngleDeg.roundToInt()}°", CanvasTransform.screenToWorld(labelScreen, canvasScale, offset), state.selectedColor)
                                        )
                                        Toast.makeText(context, "Left Arc (${leftAngleDeg.roundToInt()}°) drawn", Toast.LENGTH_SHORT).show()
                                    }
                                    MathToolZone.ANGLE_MODE_ARC -> {
                                        // Right Arc: circular arc only from Right baseline (0°) up to arm
                                        val arcScreenPoints = mutableListOf<Offset>()
                                        val steps = (pDeg / 2.5f).roundToInt().coerceAtLeast(8)
                                        for (i in 0..steps) {
                                            val deg = (pDeg * i) / steps
                                            val r = Math.toRadians(deg.toDouble()).toFloat()
                                            val d = dir * cos(r) - perp * sin(r)
                                            arcScreenPoints.add(tool.center + d * tool.span)
                                        }
                                        viewModel.commitStroke(
                                            arcStroke(
                                                screenPointsToWorld(arcScreenPoints, canvasScale, offset),
                                                state.selectedColor,
                                                state.strokeWidth
                                            )
                                        )
                                        val midRad = Math.toRadians((pDeg / 2f).toDouble()).toFloat()
                                        val midD = dir * cos(midRad) - perp * sin(midRad)
                                        val labelScreen = tool.center + midD * (tool.span + 36f) - perp * 24f
                                        viewModel.commitStroke(
                                            measurementLabel("${pDeg.roundToInt()}°", CanvasTransform.screenToWorld(labelScreen, canvasScale, offset), state.selectedColor)
                                        )
                                        Toast.makeText(context, "Right Arc (${pDeg.roundToInt()}°) drawn", Toast.LENGTH_SHORT).show()
                                    }
                                    MathToolZone.ANGLE_MODE_2 -> {
                                        // Right Angle: from Right baseline (0°) to arm
                                        viewModel.commitStroke(straightStroke(centerWorld, rightBaseWorld, state.selectedColor, state.strokeWidth))
                                        viewModel.commitStroke(straightStroke(centerWorld, armWorld, state.selectedColor, state.strokeWidth))
                                        val arcScreenPoints = mutableListOf<Offset>()
                                        val steps = (pDeg / 3f).roundToInt().coerceAtLeast(8)
                                        val arcR = tool.span * 0.45f
                                        for (i in 0..steps) {
                                            val deg = (pDeg * i) / steps
                                            val r = Math.toRadians(deg.toDouble()).toFloat()
                                            val d = dir * cos(r) - perp * sin(r)
                                            arcScreenPoints.add(tool.center + d * arcR)
                                        }
                                        viewModel.commitStroke(
                                            arcStroke(
                                                screenPointsToWorld(arcScreenPoints, canvasScale, offset),
                                                state.selectedColor,
                                                (state.strokeWidth * 0.85f).coerceAtLeast(2f)
                                            )
                                        )
                                        val midRad = Math.toRadians((pDeg / 2f).toDouble()).toFloat()
                                        val midD = dir * cos(midRad) - perp * sin(midRad)
                                        val labelScreen = tool.center + midD * (arcR + 32f) - perp * 26f
                                        viewModel.commitStroke(
                                            measurementLabel("${pDeg.roundToInt()}°", CanvasTransform.screenToWorld(labelScreen, canvasScale, offset), state.selectedColor)
                                        )
                                        Toast.makeText(context, "Right Angle ${pDeg.roundToInt()}° drawn", Toast.LENGTH_SHORT).show()
                                    }
                                    else -> Unit
                                }
                            } else if (tool != null && tool.kind == MathToolKind.COMPASS && (mathGesture.zone == MathToolZone.PENCIL || mathGesture.zone == MathToolZone.ROTATE)) {
                                val worldCenter = CanvasTransform.screenToWorld(
                                    tool.center,
                                    canvasScale,
                                    Offset(canvasOffsetX, canvasOffsetY)
                                )
                                val worldRadius = tool.span / canvasScale.coerceAtLeast(0.001f)
                                if (mathGesture.movedPx <= MATH_TAP_SLOP || isFullCircleSweep(mathGesture.sweepDeg)) {
                                    viewModel.cancelCurrentStroke()
                                    viewModel.commitStroke(
                                        circleStroke(worldCenter, worldRadius, state.selectedColor, state.strokeWidth)
                                    )
                                } else if (mathGesture.arcMode) {
                                    viewModel.finishStroke()
                                }
                            } else if (tool != null && tool.kind == MathToolKind.SET_SQUARE &&
                                mathGesture.zone == MathToolZone.SCALE && tool.markedAngleDeg != null
                            ) {
                                val angle = tool.markedAngleDeg
                                val rim = rimPoint(tool.center, tool.rotationDeg, angle, tool.span)
                                val labelScreen = rimPoint(tool.center, tool.rotationDeg, angle, tool.span + 36f)
                                val offset = Offset(canvasOffsetX, canvasOffsetY)
                                viewModel.commitStroke(
                                    straightStroke(
                                        CanvasTransform.screenToWorld(tool.center, canvasScale, offset),
                                        CanvasTransform.screenToWorld(rim, canvasScale, offset),
                                        state.selectedColor,
                                        state.strokeWidth
                                    )
                                )
                                viewModel.commitStroke(
                                    measurementLabel(
                                        formatDegrees(angle),
                                        CanvasTransform.screenToWorld(labelScreen, canvasScale, offset),
                                        state.selectedColor
                                    )
                                )
                            }
                            }
                        mathReadout = null
                        } else if (!wasMultiTouch) {
                            val latest = viewModel.state.value
                            val moved = kotlin.math.hypot(
                                (prevPosition.x - down.position.x).toDouble(),
                                (prevPosition.y - down.position.y).toDouble()
                            ).toFloat()
                            if (activeTool == WhiteboardTool.HAND && latest.document != null) {
                                val delta = swipePageDelta(
                                    prevPosition.x - down.position.x,
                                    prevPosition.y - down.position.y
                                )
                                val next = pageStep(latest.pageIndex, latest.pageCount, delta)
                                if (delta != 0 && next != latest.pageIndex) viewModel.selectPage(next)
                            } else if (
                                pptAdvancesOnTap(latest.document?.kind, moved, strokeHit) &&
                                resizeStrokeId == null && !isDraggingSelection
                            ) {
                                val next = pageStep(latest.pageIndex, latest.pageCount, 1)
                                if (next != latest.pageIndex) viewModel.selectPage(next)
                            }
                            if (activeTool == WhiteboardTool.LASSO) {
                                viewModel.lassoSelect(lassoPoints)
                                lassoPoints = emptyList()
                                if (!state.isToolLocked) {
                                    viewModel.setTool(WhiteboardTool.SELECT)
                                }
                            } else if (writingNow && inkPoints.isNotEmpty()) {
                                val finishedWidth = if (isPenNib) {
                                    if (stylusDraw) {
                                        stylusPressureWidth(
                                            activeInkWidth,
                                            inkPressures.average().toFloat()
                                        )
                                    } else {
                                        activeInkWidth
                                    }
                                } else {
                                    val curMajor = touchContacts.majorPx(down.position)
                                    val curMinor = touchContacts.minorPx(down.position)
                                    val curContact = if (curMajor > 0f) (if (curMinor > 0f) (curMajor + curMinor) / 2f else curMajor) else 0f
                                    if (curContact > 0f) {
                                        if (curContact <= 5.0f) (curContact * 5f).coerceIn(14f, 32f) else curContact.coerceIn(12f, 44f)
                                    } else activeInkWidth
                                }
                                val finished = inkTemplate!!.copy(
                                    points = ArrayList(inkPoints),
                                    pointPressures = ArrayList(inkPressures),
                                    strokeWidth = finishedWidth,
                                    colorValue = activeInkColor
                                )
                                liveInk.stroke = null
                                liveInkTick.intValue = liveInkTick.intValue + 1
                                boardStage?.setLiveInk(null)
                                viewModel.commitStroke(finished)
                                val canRecognize = !state.smartWriteEnabled &&
                                    state.isDrawToShapeEnabled && activeTool in setOf(
                                    WhiteboardTool.PEN,
                                    WhiteboardTool.PENCIL,
                                    WhiteboardTool.MARKER
                                )
                                if (canRecognize) {
                                    val recognized = ShapeRecognitionHelper.recognize(finished.points)
                                    if (recognized != null && recognized.confidence >= 0.7f) {
                                        shapePrompt = ShapePrompt(
                                            strokeId = finished.id,
                                            replacement = finished.copy(
                                                points = recognized.points,
                                                pointPressures = emptyList(),
                                                strokeType = recognized.strokeType
                                            )
                                        )
                                    }
                                }
                            } else if ((isShapeActive || activeTool in listOf(
                                    WhiteboardTool.RECTANGLE,
                                    WhiteboardTool.DIAMOND,
                                    WhiteboardTool.CIRCLE,
                                    WhiteboardTool.ARROW,
                                    WhiteboardTool.LINE,
                                    WhiteboardTool.FRAME,
                                    WhiteboardTool.TRIANGLE,
                                    WhiteboardTool.STAR,
                                    WhiteboardTool.ELLIPSE,
                                    WhiteboardTool.SQUARE,
                                    WhiteboardTool.HEXAGON,
                                    WhiteboardTool.ROUND_RECT,
                                    WhiteboardTool.CUBE,
                                    WhiteboardTool.CYLINDER,
                                    WhiteboardTool.CONE,
                                    WhiteboardTool.PYRAMID,
                                    WhiteboardTool.SHAPE
                                )) && shapePoints.size >= 2
                            ) {
                                viewModel.updateStroke(shapePoints)
                                viewModel.finishStroke()
                            }
                            rulerLivePreview = null
                            mathReadout = null
                        }
                        if (wasMultiTouch) {
                            viewModel.setMathTools(mathTools)
                        }
                    }
                }
        ) {
            AndroidView(
                factory = { context ->
                    BoardStageView(context).also { boardStage = it }
                },
                update = { stage ->
                    val page = viewModel.state.value
                    stage.syncPage(
                        strokes = if (eraserSession.active) eraserSession.strokes else page.strokes,
                        background = page.canvasBackground,
                        showGrid = page.showGrid,
                        dark = page.isDarkCanvas,
                        scale = page.scale,
                        offsetX = page.offsetX,
                        offsetY = page.offsetY,
                        selected = page.selectedStrokeIds,
                        customBackgroundUri = page.pageCustomBackgroundUris.getOrElse(page.pageIndex) { "" },
                        pageIndex = page.pageIndex
                    )
                    stage.currentStroke = page.currentStroke
                    val document = page.document
                    stage.setDocumentPage(
                        pageBitmap,
                        document?.pageWidth ?: 0f,
                        document?.pageHeight ?: 0f
                    )
                    stage.setDocumentFrame(page.documentFrameX, page.documentFrameY, page.documentFrameScale)
                    stage.setFrostedBounds(chromeBounds.frost.map { android.graphics.RectF(it.left, it.top, it.right, it.bottom) })
                },
                modifier = Modifier.fillMaxSize()
            )
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        compositingStrategy = if (state.currentTool == WhiteboardTool.SPOTLIGHT) {
                            CompositingStrategy.Offscreen
                        } else {
                            CompositingStrategy.Auto
                        }
                    }
            ) {
                if (state.currentTool == WhiteboardTool.SPOTLIGHT) {
                    val center = spotlightCenter ?: Offset(size.width / 2f, size.height / 2f)
                    drawRect(Color(0xB3000000))
                    drawCircle(
                        color = Color.Transparent,
                        radius = spotlightRadius,
                        center = center,
                        blendMode = BlendMode.Clear
                    )
                    drawCircle(
                        color = Color.White,
                        radius = spotlightRadius,
                        center = center,
                        style = DrawStroke(width = 2f)
                    )
                }
                if (state.currentTool == WhiteboardTool.SHADE) {
                    val edge = (shadeEdge * size.height).coerceIn(0f, size.height)
                    val shade = Color(0xF21B1F2A)
                    if (shadeFromTop) {
                        drawRect(shade, size = Size(size.width, edge))
                    } else {
                        drawRect(shade, topLeft = Offset(0f, edge), size = Size(size.width, size.height - edge))
                    }
                    drawLine(
                        color = Color(0xFF8B5CF6),
                        start = Offset(0f, edge),
                        end = Offset(size.width, edge),
                        strokeWidth = 6f
                    )
                }
                withTransform({
                    translate(canvasOffsetX, canvasOffsetY)
                    scale(canvasScale, canvasScale, pivot = Offset.Zero)
                }) {
                    if (state.laserPoints.isNotEmpty()) {
                        val center = state.laserPoints.last()
                        val radius = 10f / canvasScale.coerceAtLeast(0.5f)
                        drawCircle(Color(0x55FF2A2A), radius * 2.2f, center)
                        drawCircle(Color(0xFFFF2A2A), radius, center)
                    }
                    if (lassoPoints.size >= 2) {
                        drawPath(
                            path = lassoPoints.toSmoothedPath(),
                            color = Color(0xFF6366F1),
                            style = DrawStroke(
                                width = 1.5f / canvasScale.coerceAtLeast(0.5f),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                            )
                        )
                    }
                }
                val eraserCursor = eraserSession.cursor
                if (state.currentTool == WhiteboardTool.ERASER && eraserCursor != null) {
                    drawCircle(
                        color = Color(0xAA374151),
                        radius = state.eraserSize,
                        center = eraserCursor,
                        style = DrawStroke(width = 1.5f)
                    )
                }
                val liveLine = rulerLivePreview
                if (liveLine != null) {
                    val previewColor = if (state.selectedColor != 0) Color(state.selectedColor) else (if (state.isDarkCanvas) Color.White else Color.Black)
                    val previewWidth = if (state.strokeWidth > 0f) state.strokeWidth else 6f
                    drawLine(
                        color = if (state.currentTool == WhiteboardTool.HIGHLIGHTER) previewColor.copy(alpha = 0.5f) else previewColor,
                        start = liveLine.first,
                        end = liveLine.second,
                        strokeWidth = previewWidth,
                        cap = StrokeCap.Round
                    )
                }
                val readout = mathReadout
                mathTools.forEach { tool ->
                    drawMathTool(tool, state.isDarkCanvas, pxPerCm)
                }
                mathGhost?.let { ghost ->
                    drawMathTool(ghost, state.isDarkCanvas, pxPerCm)
                }
                if (readout != null) {
                    drawMathReadout(readout.first, readout.second)
                }
            }

            val currentDoc = state.document
            if (currentDoc != null && state.splitCount <= 1) {
                val docWidth = if (currentDoc.pageWidth > 0f) currentDoc.pageWidth else 1600f
                val docHeight = if (currentDoc.pageHeight > 0f) currentDoc.pageHeight else 2262f
                val frameScale = state.documentFrameScale
                val frameX = state.documentFrameX
                val frameY = state.documentFrameY

                val pdfScreenX = canvasOffsetX + (frameX * canvasScale)
                val pdfScreenY = canvasOffsetY + (frameY * canvasScale)
                val pdfScreenW = docWidth * frameScale * canvasScale
                val pdfScreenH = docHeight * frameScale * canvasScale

                // 1. Subtle dashed boundary showing PDF frame
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        color = Color(0x733B82F6),
                        topLeft = Offset(pdfScreenX, pdfScreenY),
                        size = Size(pdfScreenW, pdfScreenH),
                        style = DrawStroke(
                            width = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f))
                        )
                    )
                }

                // 2. Draggable Top Header Pill
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (pdfScreenX + 12f).roundToInt().coerceAtLeast(10),
                                (pdfScreenY - 42f).roundToInt().coerceAtLeast(10)
                            )
                        }
                        .wrapContentSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xE61E2638))
                        .border(1.dp, Color(0xFF3B82F6), RoundedCornerShape(8.dp))
                        .pointerInput(canvasScale, state.documentFrameX, state.documentFrameY, state.documentFrameScale) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val worldDx = dragAmount.x / canvasScale.coerceAtLeast(0.001f)
                                val worldDy = dragAmount.y / canvasScale.coerceAtLeast(0.001f)
                                viewModel.setDocumentFrame(
                                    state.documentFrameX + worldDx,
                                    state.documentFrameY + worldDy,
                                    state.documentFrameScale
                                )
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.OpenWith,
                            contentDescription = "Drag to reposition PDF",
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "PDF: Drag to move",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF283248))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "${(state.documentFrameScale * 100).roundToInt()}%",
                                color = Color(0xFF93C5FD),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 3. Corner Resize Handle (Bottom-Right)
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (pdfScreenX + pdfScreenW - 22f).roundToInt(),
                                (pdfScreenY + pdfScreenH - 22f).roundToInt()
                            )
                        }
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2563EB))
                        .border(2.dp, Color.White, CircleShape)
                        .pointerInput(canvasScale, state.documentFrameScale, docWidth) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val worldDx = dragAmount.x / canvasScale.coerceAtLeast(0.001f)
                                val currentW = docWidth * state.documentFrameScale
                                val newW = (currentW + worldDx).coerceAtLeast(docWidth * 0.2f)
                                val newScale = (newW / docWidth).coerceIn(0.2f, 3.0f)
                                viewModel.setDocumentFrame(
                                    state.documentFrameX,
                                    state.documentFrameY,
                                    newScale
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ZoomOutMap,
                        contentDescription = "Drag to resize PDF",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        }

        AvitriSphareWhiteboardChrome(
            state = state,
            viewModel = viewModel,
            onExit = onNavigateBack,
            onOpenMenu = { showSidebar = true },
            onInsertImage = { imagePickerLauncher.launch("image/*") },
            onPickCustomBackground = { customBgPickerLauncher.launch("image/*") },
            onExport = {
                exportResult = null
                viewModel.clearSelection()
                selectionToolbarDismissed = true
                showExportLessonDialog = true
            },
            onOpenIwb = {
                iwbPickerLauncher.launch(arrayOf("*/*"))
            },
            onHelp = { showHelpDialog = true },
            onClear = { showClearDialog = true },
            onRename = { showEditTitleDialog = true },
            onSearchImages = { showImageSearch = true },
            onInsertVideo = { videoPickerLauncher.launch(arrayOf("video/*")) },
            onYouTube = {
                youtubeQuery = ""
                showYouTubeDialog = true
            },
            shadeFromTop = shadeFromTop,
            onToggleShadeSide = { shadeFromTop = !shadeFromTop },
            spotlightRadius = spotlightRadius,
            onSpotlightRadius = { spotlightRadius = it },
            onWebEmbed = {
                textInputValue = ""
                val centerWorld = CanvasTransform.screenToWorld(
                    screenPoint = Offset(canvasSize.width / 2f, canvasSize.height / 2f),
                    scale = canvasScale,
                    offset = Offset(canvasOffsetX, canvasOffsetY)
                )
                pendingWorldPoint = centerWorld
                showWebEmbedDialog = true
            },
            onZoomIn = {
                val newScale = (canvasScale * 1.5f).coerceIn(CanvasTransform.MIN_SCALE, CanvasTransform.MAX_SCALE)
                val center = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
                val newOffset = CanvasTransform.calculatePivotZoomOffset(
                    canvasScale,
                    newScale,
                    Offset(canvasOffsetX, canvasOffsetY),
                    center,
                    Offset.Zero
                )
                canvasScale = newScale
                canvasOffsetX = newOffset.x
                canvasOffsetY = newOffset.y
                viewModel.updateTransform(canvasScale, canvasOffsetX, canvasOffsetY)
            },
            onZoomOut = {
                val newScale = (canvasScale / 1.5f).coerceIn(CanvasTransform.MIN_SCALE, CanvasTransform.MAX_SCALE)
                val center = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
                val newOffset = CanvasTransform.calculatePivotZoomOffset(
                    canvasScale,
                    newScale,
                    Offset(canvasOffsetX, canvasOffsetY),
                    center,
                    Offset.Zero
                )
                canvasScale = newScale
                canvasOffsetX = newOffset.x
                canvasOffsetY = newOffset.y
                viewModel.updateTransform(canvasScale, canvasOffsetX, canvasOffsetY)
            },
            onFit = { viewModel.fitToContent(canvasSize.width, canvasSize.height) },
            placedMathTools = mathTools,
            pxPerCm = pxPerCm,
            onMathDrag = { kind, root ->
                mathGhost = newMathTool(kind, rootToCanvas(root))
            },
            onMathAdjust = { kind, adjust -> adjustMathTool(kind, adjust) },
            onMathDragEnd = { kind, root, moved ->
                mathGhost = null
                val overToolbar = moved && droppedOnToolbar(root, chromeBounds.dock)
                if (overToolbar) {
                    val existing = mathTools.find { it.kind == kind }
                    if (existing != null) {
                        removeMathTool(existing.id)
                        Toast.makeText(context, "Tool removed", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val local = if (moved) rootToCanvas(root) else canvasCenter()
                    placeMathTool(kind, local, isDrag = moved)
                }
            },
            onDockBounds = { next ->
                val prev = chromeBounds.dock
                val unchanged = prev != null &&
                    kotlin.math.abs(prev.left - next.left) < 1f &&
                    kotlin.math.abs(prev.top - next.top) < 1f &&
                    kotlin.math.abs(prev.right - next.right) < 1f &&
                    kotlin.math.abs(prev.bottom - next.bottom) < 1f
                if (!unchanged) chromeBounds.dock = next
            },
            onFrostedBounds = { rects ->
                if (!frostUnchanged(chromeBounds.frost, rects)) {
                    chromeBounds.frost = rects
                    boardStage?.setFrostedBounds(rects.map { r -> android.graphics.RectF(r.left, r.top, r.right, r.bottom) })
                }
            },
            onOpenDocument = {
                documentPickerLauncher.launch(
                    arrayOf(
                        "application/pdf",
                        "application/vnd.ms-powerpoint",
                        "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                    )
                )
            },
            demoMode = demoMode,
            dockDismissNonce = dockDismissNonce,
            selectionToolbarDismissed = selectionToolbarDismissed,
            onDismissSelectionToolbar = { selectionToolbarDismissed = it },
            onSave = { viewModel.confirmSave() },
            onSaveSlides = {
                viewModel.exportAllSlidesPdf { uri ->
                    if (uri != null) {
                        exportedUri = uri
                        showExportDialog = true
                    }
                }
            },
            onCopySelection = { viewModel.copySelectedStrokes() },
            onPasteClipboard = { viewModel.pasteStrokeClipboard() },
            onToggleLocale = {
                val nextLang = when (currentLanguage) {
                    AppLanguage.ENGLISH -> AppLanguage.HINDI
                    AppLanguage.HINDI -> AppLanguage.MARATHI
                    AppLanguage.MARATHI -> AppLanguage.ENGLISH
                }
                onLanguageChange(nextLang)
                panelSession?.setUiLocaleTag(nextLang.code)
                viewModel.setUiLocaleTag(nextLang.code)
            },
            onOpenLanguageDialog = {
                showLanguageDialog = true
            },
            isTimerVisible = timerState.isVisible,
            onToggleTimer = {
                val next = !timerState.isVisible
                timerState = timerState.copy(isVisible = next)
                if (next) bringToolToFront(UtilityToolKind.TIMER)
            },
            isStopwatchVisible = stopwatchState.isVisible,
            onToggleStopwatch = {
                val next = !stopwatchState.isVisible
                stopwatchState = stopwatchState.copy(isVisible = next)
                if (next) bringToolToFront(UtilityToolKind.STOPWATCH)
            },
            isCalculatorVisible = calculatorState.isVisible,
            onToggleCalculator = {
                val next = !calculatorState.isVisible
                calculatorState = calculatorState.copy(isVisible = next)
                if (next) bringToolToFront(UtilityToolKind.CALCULATOR)
            },
            isPeriodicTableVisible = periodicTableState.isVisible,
            onTogglePeriodicTable = {
                val next = !periodicTableState.isVisible
                periodicTableState = periodicTableState.copy(isVisible = next)
                if (next) bringToolToFront(UtilityToolKind.PERIODIC_TABLE)
            },
            onOpenStickerEmoji = {
                viewModel.clearSelection()
                selectionToolbarDismissed = true
                showStickerEmojiDialog = true
            },
            onSelectSticker = { kind, text ->
                viewModel.clearSelection()
                val center = canvasCenter()
                viewModel.insertSticker(kind, center, text)
            },
            onEditNodeText = { strokeId, currentText ->
                editingNodeStrokeId = strokeId
                editingNodeText = currentText
            }
        )

        // Floating Utility Panels (Timer, Stopwatch, Calculator, Periodic Table)
        // Dynamically rendered in toolZOrder so whichever tool is clicked appears at the top
        toolZOrder.forEach { toolKind ->
            when (toolKind) {
                UtilityToolKind.TIMER -> {
                    if (timerState.isVisible) {
                        TimerFloatingPanel(
                            state = timerState,
                            onStateChange = { timerState = it },
                            onBringToFront = { bringToolToFront(UtilityToolKind.TIMER) }
                        )
                    }
                }
                UtilityToolKind.STOPWATCH -> {
                    if (stopwatchState.isVisible) {
                        StopwatchFloatingPanel(
                            state = stopwatchState,
                            onStateChange = { stopwatchState = it },
                            onBringToFront = { bringToolToFront(UtilityToolKind.STOPWATCH) }
                        )
                    }
                }
                UtilityToolKind.CALCULATOR -> {
                    if (calculatorState.isVisible) {
                        CalculatorFloatingPanel(
                            state = calculatorState,
                            onStateChange = { calculatorState = it },
                            onBringToFront = { bringToolToFront(UtilityToolKind.CALCULATOR) }
                        )
                    }
                }
                UtilityToolKind.PERIODIC_TABLE -> {
                    if (periodicTableState.isVisible) {
                        PeriodicTableFloatingPanel(
                            state = periodicTableState,
                            onStateChange = { periodicTableState = it },
                            onBringToFront = { bringToolToFront(UtilityToolKind.PERIODIC_TABLE) }
                        )
                    }
                }
            }
        }

        val openDocument = state.document
        if (openDocument != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xF21C2130),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        viewModel.selectPage(pageStep(state.pageIndex, state.pageCount, -1))
                    }) { Text("Prev", color = Color.White) }
                    Text(
                        "${openDocument.name}  ${state.pageIndex + 1}/${state.pageCount}",
                        color = Color.White,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                    TextButton(onClick = {
                        viewModel.selectPage(pageStep(state.pageIndex, state.pageCount, 1))
                    }) { Text("Next", color = Color.White) }

                    if (openDocument.kind == DocumentKind.PDF) {
                        if (state.isPdfOcrActive) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = Color(0xFF60A5FA),
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "OCR: ${state.pdfOcrCurrentPage}/${state.pdfOcrTotalPages}",
                                    color = Color(0xFF93C5FD),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.width(8.dp))
                                TextButton(onClick = { viewModel.cancelPdfOcr() }) {
                                    Text("Stop", color = Color(0xFFFF6B6B))
                                }
                            }
                        } else {
                            val currentOcrDoc = state.pdfOcrDocument
                            val hasOcrForCurrentPage = currentOcrDoc?.pages?.any { it.pageIndex == state.pageIndex } == true
                            if (currentOcrDoc != null) {
                                TextButton(
                                    onClick = { viewModel.togglePdfSearchPanel() },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = if (state.showPdfSearchPanel) Color(0xFF34D399) else Color(0xFF60A5FA)
                                    )
                                ) {
                                    Text(
                                        if (state.showPdfSearchPanel) "Hide Text" else "Search & Text",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            if (!hasOcrForCurrentPage) {
                                TextButton(
                                    onClick = { viewModel.makePdfEditableCurrentPage() },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF60A5FA))
                                ) {
                                    Text("OCR this page", fontWeight = FontWeight.SemiBold)
                                }
                            }
                            if (currentOcrDoc == null || currentOcrDoc.pages.size < state.pageCount) {
                                TextButton(
                                    onClick = { viewModel.makePdfEditable() },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF93C5FD))
                                ) {
                                    Text(
                                        if (currentOcrDoc == null) "OCR all pages" else "OCR all",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // 4-Directional Move Controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF283248))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "Move:",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        IconButton(
                            onClick = {
                                viewModel.setDocumentFrame(
                                    state.documentFrameX - 60f,
                                    state.documentFrameY,
                                    state.documentFrameScale
                                )
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Text("←", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(
                            onClick = {
                                viewModel.setDocumentFrame(
                                    state.documentFrameX + 60f,
                                    state.documentFrameY,
                                    state.documentFrameScale
                                )
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Text("→", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(
                            onClick = {
                                viewModel.setDocumentFrame(
                                    state.documentFrameX,
                                    state.documentFrameY - 60f,
                                    state.documentFrameScale
                                )
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Text("↑", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(
                            onClick = {
                                viewModel.setDocumentFrame(
                                    state.documentFrameX,
                                    state.documentFrameY + 60f,
                                    state.documentFrameScale
                                )
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Text("↓", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.width(4.dp))

                    // PDF Zoom Controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF283248))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        IconButton(
                            onClick = {
                                viewModel.setDocumentFrame(
                                    state.documentFrameX,
                                    state.documentFrameY,
                                    (state.documentFrameScale * 0.9f).coerceIn(0.2f, 3.0f)
                                )
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Shrink PDF", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                        Text(
                            "${(state.documentFrameScale * 100).roundToInt()}%",
                            color = Color(0xFF60A5FA),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        IconButton(
                            onClick = {
                                viewModel.setDocumentFrame(
                                    state.documentFrameX,
                                    state.documentFrameY,
                                    (state.documentFrameScale * 1.1f).coerceIn(0.2f, 3.0f)
                                )
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Enlarge PDF", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }

                    Spacer(Modifier.width(4.dp))

                    TextButton(onClick = {
                        if (canvasSize.width > 0 && canvasSize.height > 0) {
                            viewModel.fitToContent(canvasSize.width, canvasSize.height)
                        } else {
                            viewModel.resetDocumentFrame()
                        }
                    }) {
                        Text("Fit PDF", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                    }
                    TextButton(onClick = {
                        viewModel.exportAnnotatedPdf { uri ->
                            exportedUri = uri
                            showExportDialog = true
                        }
                    }) { Text("Export PDF", color = Color.White) }
                    TextButton(onClick = { viewModel.closeDocument() }) {
                        Text("Close", color = Color.White)
                    }
                }
            }

            val ocrDoc = state.pdfOcrDocument
            if (state.showPdfSearchPanel && ocrDoc != null) {
                val currentPageText = ocrDoc.textForPage(state.pageIndex)
                val searchResults = remember(state.pdfSearchQuery, ocrDoc) {
                    ocrDoc.search(state.pdfSearchQuery)
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 64.dp, end = 16.dp)
                        .width(360.dp)
                        .heightIn(max = 520.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xF21C2130),
                    shadowElevation = 12.dp
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    "Document Live Text",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    "Extracted searchable text. Layout editability and unsupported languages are not promised.",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                            IconButton(
                                onClick = { viewModel.togglePdfSearchPanel(false) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = state.pdfSearchQuery,
                            onValueChange = { viewModel.setPdfSearchQuery(it) },
                            placeholder = { Text("Search within PDF...", fontSize = 13.sp, color = Color.Gray) },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF3B82F6),
                                unfocusedBorderColor = Color(0xFF4B5563)
                            )
                        )

                        if (state.pdfSearchQuery.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            if (searchResults.isEmpty()) {
                                Text("No matches found", color = Color(0xFFF87171), fontSize = 12.sp)
                            } else {
                                Text(
                                    "Found in ${searchResults.size} page(s):",
                                    color = Color(0xFF34D399),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(4.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(searchResults) { (matchedPage, count) ->
                                        val isCurrent = matchedPage == state.pageIndex
                                        Text(
                                            text = "Page ${matchedPage + 1} ($count)",
                                            fontSize = 12.sp,
                                            color = Color.White,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isCurrent) Color(0xFF2563EB) else Color(0xFF374151))
                                                .clickable { viewModel.selectPage(matchedPage) }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Page ${state.pageIndex + 1} Selectable Text:",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF111827))
                                .padding(8.dp)
                        ) {
                            SelectionContainer {
                                BasicTextField(
                                    value = if (currentPageText.isBlank()) "No text detected on this page." else currentPageText,
                                    onValueChange = {},
                                    readOnly = true,
                                    textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.copyPdfTextToCanvas(currentPageText) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                            ) {
                                Text("Copy to Canvas", fontSize = 12.sp, maxLines = 1)
                            }
                            Button(
                                onClick = { viewModel.copyPdfTextToClipboard(currentPageText) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                            ) {
                                Text("Copy for AI", fontSize = 12.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        val smartOffer = state.smartWriteOffer
        if (smartOffer != null) {
            val anchor = CanvasTransform.worldToScreen(
                smartOffer.anchor,
                canvasScale,
                Offset(canvasOffsetX, canvasOffsetY)
            )
            val dockTop = chromeBounds.dock?.top ?: Float.MAX_VALUE
            val targetY = if (anchor.y + 70f >= dockTop) {
                (anchor.y - 70f).coerceAtLeast(80f)
            } else {
                (anchor.y + 12f).coerceAtLeast(80f)
            }
            Surface(
                modifier = Modifier
                    .offset { IntOffset(anchor.x.roundToInt().coerceAtLeast(8), targetY.roundToInt()) }
                    .widthIn(max = 520.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                    if (smartOffer.lowConfidence) {
                        Text(
                            "Low confidence — please check",
                            color = Color(0xFFD97706),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PEN_COLORS.take(6).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(Color(color))
                                    .border(
                                        width = if (smartOffer.color == color) 2.dp else 1.dp,
                                        color = if (smartOffer.color == color) Color(0xFF3B82F6) else Color(0xFFE2E8F0),
                                        shape = CircleShape
                                    )
                                    .clickable { viewModel.recolorSmartWrite(color) }
                            )
                        }
                        smartOffer.candidates.forEach { candidate ->
                            Text(
                                text = candidate,
                                color = Color(0xFF1E293B),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF1F5F9))
                                    .clickable { viewModel.acceptSmartWrite(candidate) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                        Text(
                            text = "Close",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp,
                            modifier = Modifier
                                .clickable { viewModel.dismissSmartWrite() }
                                .padding(horizontal = 6.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        val editing = state.strokes.firstOrNull { it.id == state.editingTextId }
        if (editing != null && editing.points.isNotEmpty()) {
            val origin = CanvasTransform.worldToScreen(
                editing.points.first(),
                canvasScale,
                Offset(canvasOffsetX, canvasOffsetY)
            )
            Column(
                modifier = Modifier
                    .offset { IntOffset(origin.x.roundToInt().coerceAtLeast(8), origin.y.roundToInt().coerceAtLeast(8)) }
                    .width(240.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(8.dp)
            ) {
                val warning = state.smartWriteWarning
                if (warning != null) {
                    Text(warning, color = Color(0xFFB45309), fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                }
                var draft by remember(editing.id) { mutableStateOf(editing.text) }
                OutlinedTextField(
                    value = draft,
                    onValueChange = {
                        draft = it
                        viewModel.editStrokeText(editing.id, it)
                    },
                    singleLine = false,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Color(editing.colorValue),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(onClick = { viewModel.endTextEdit() }) {
                    Text("Done")
                }
            }
        }

        state.formulaSolution?.let { solution ->
            FormulaCard(
                solution = solution,
                onSolve = { latex -> viewModel.solveEditedEquation(latex) },
                onDismiss = { viewModel.dismissFormula() },
                onRetry = { viewModel.retryEquationConversion() },
                onDraftChange = { latex -> viewModel.updateEquationDraft(latex) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 108.dp)
            )
        }

        if (state.equationBusy) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 72.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xF21C2130),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF818CF8)
                    )
                    Text(
                        "Recognizing equation…",
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }
            }
        }

        if (state.smartWriteBusy && smartOffer == null && editing == null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 72.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xF21C2130)
            ) {
                Text(
                    "Reading handwriting…",
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }

        if (shapePromptVisible) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 64.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 12.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        "Convert to shape?",
                        color = Color(0xFF1F2937),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Text(
                        "Converts automatically",
                        color = Color(0xFF6B7280),
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            shapePrompt = null
                            shapePromptVisible = false
                        }) {
                            Text("No", color = Color(0xFF4B5563), fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = {
                                val prompt = shapePrompt
                                if (prompt != null) viewModel.replaceStroke(prompt.replacement)
                                shapePrompt = null
                                shapePromptVisible = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                        ) {
                            Text("Yes")
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showAiPanel,
            enter = fadeIn() + slideInHorizontally { it },
            exit = fadeOut() + slideOutHorizontally { it }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x33000000))
                    .clickable { showAiPanel = false }
            ) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(320.dp)
                        .clickable { },
                    color = Color(0xFF1C2130),
                    shadowElevation = 12.dp
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "AI",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { showAiPanel = false }) {
                                Text("Close", color = Color(0xFFC7D2FE))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Four-finger tap opens and closes this panel.",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(20.dp))
                        listOf(
                            "Lesson step" to "Walk the class through the next step on the board.",
                            "Explain" to "Restate what is on the canvas in plain language.",
                            "Quiz" to "Turn the current page into a short check.",
                            "Voice" to "Listen, then place the answer on the board."
                        ).forEach { (title, detail) ->
                            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(detail, color = Color(0xFFCBD5E1), fontSize = 13.sp)
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
            }
        }

        // 5. Left Floating Sidebar (Exact Match to User's Image 1)
        AnimatedVisibility(
            visible = showSidebar,
            enter = fadeIn() + slideInHorizontally { -it },
            exit = fadeOut() + slideOutHorizontally { -it }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x40000000))
                    .clickable { showSidebar = false }
            ) {
                val isDark = state.isDarkTheme
                Surface(
                    shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                    color = if (isDark) Color(0xFF1E293B) else Color.White,
                    border = if (isDark) BorderStroke(1.dp, Color(0xFF334155)) else null,
                    shadowElevation = 16.dp,
                    modifier = Modifier
                        .width(260.dp)
                        .fillMaxHeight()
                        .statusBarsPadding()
                        .clickable(enabled = false) {}
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Sidebar Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "AvitriSphare",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1F2937)
                                )
                                Text(
                                    stringResource(R.string.app_subtitle),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF6B7280)
                                )
                            }
                            IconButton(
                                onClick = { showSidebar = false },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), tint = if (isDark) Color(0xFFCBD5E1) else Color(0xFF6B7280))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 0. Profile
                        SidebarMenuItem(
                            icon = Icons.Default.Person,
                            label = stringResource(R.string.tool_profile),
                            isDark = isDark,
                            onClick = {
                                showSidebar = false
                                showProfileDialog = true
                            }
                        )

                        SidebarMenuItem(
                            icon = Icons.Default.Edit,
                            label = stringResource(R.string.menu_rename_board),
                            isDark = isDark,
                            onClick = {
                                showSidebar = false
                                showEditTitleDialog = true
                            }
                        )

                        // 1. Open (Ctrl+O)
                        SidebarMenuItem(
                            icon = Icons.Default.FolderOpen,
                            label = stringResource(R.string.menu_open),
                            shortcut = "Ctrl+O",
                            isDark = isDark,
                            onClick = {
                                showSidebar = false
                                onNavigateBack()
                            }
                        )

                        // 2. Export lesson (PDF, PNG ZIP, IWB)
                        SidebarMenuItem(
                            icon = Icons.Default.IosShare,
                            label = stringResource(R.string.menu_export_lesson),
                            shortcut = "Ctrl+Shift+E",
                            isSelected = true,
                            isDark = isDark,
                            onClick = {
                                showSidebar = false
                                exportResult = null
                                showExportLessonDialog = true
                            }
                        )

                        // 3. Open GYOASH Lesson (.iwb)...
                        SidebarMenuItem(
                            icon = Icons.Default.FolderOpen,
                            label = stringResource(R.string.menu_open_iwb),
                            isDark = isDark,
                            onClick = {
                                showSidebar = false
                                iwbPickerLauncher.launch(arrayOf("*/*"))
                            }
                        )

                        Divider(
                            color = if (isDark) Color(0xFF334155) else Color(0xFFF3F4F6),
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        // 5. Command palette (Ctrl+/)
                        SidebarMenuItem(
                            icon = Icons.Default.Bolt,
                            label = stringResource(R.string.menu_command_palette),
                            shortcut = "Ctrl+/",
                            isDark = isDark,
                            onClick = {
                                showSidebar = false
                                showCommandPalette = true
                            }
                        )

                        // 6. Find on canvas (Ctrl+F)
                        SidebarMenuItem(
                            icon = Icons.Default.Search,
                            label = stringResource(R.string.menu_find_on_canvas),
                            shortcut = "Ctrl+F",
                            isDark = isDark,
                            onClick = {
                                showSidebar = false
                                showCommandPalette = true
                            }
                        )

                        // 7. Help (?)
                        SidebarMenuItem(
                            icon = Icons.Default.HelpOutline,
                            label = stringResource(R.string.menu_help),
                            shortcut = "?",
                            isDark = isDark,
                            onClick = {
                                showSidebar = false
                                showHelpDialog = true
                            }
                        )

                        // 8. Reset the canvas
                        SidebarMenuItem(
                            icon = Icons.Default.LayersClear,
                            label = stringResource(R.string.menu_reset_canvas),
                            isDark = isDark,
                            onClick = {
                                showSidebar = false
                                showClearDialog = true
                            }
                        )

                        Divider(
                            color = if (isDark) Color(0xFF334155) else Color(0xFFF3F4F6),
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        // 9. Change Language (English, Hindi, Marathi)
                        SidebarMenuItem(
                            icon = Icons.Default.Language,
                            label = stringResource(R.string.change_language),
                            shortcut = currentLanguage.nativeName,
                            isDark = isDark,
                            onClick = {
                                showSidebar = false
                                showLanguageDialog = true
                            }
                        )

                        // 10. Theme (Light / Dark)
                        SidebarMenuItem(
                            icon = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                            label = if (isDark) stringResource(R.string.theme_light) else stringResource(R.string.theme_dark),
                            isDark = isDark,
                            onClick = {
                                viewModel.toggleAppTheme()
                            }
                        )
                    }
                }
            }
        }

        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = LocalAvitriSphareColors.current.brand
            )
        }
    }

    // Command Palette Dialog
    if (showCommandPalette) {
        val isDark = state.isDarkTheme || isSystemInDarkTheme()
        val dialogBg = if (isDark) Color(0xFF1E293B) else Color.White

        Dialog(onDismissRequest = { showCommandPalette = false }) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = dialogBg,
                border = if (isDark) BorderStroke(1.dp, Color(0xFF334155)) else null,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = commandSearchQuery,
                        onValueChange = { commandSearchQuery = it },
                        placeholder = { Text("Search tools & commands...", color = if (isDark) Color(0xFF94A3B8) else Color(0xFF6B7280)) },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF9CA3AF)) },
                        textStyle = TextStyle(
                            color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF111827),
                            fontSize = 14.sp
                        ),
                        colors = boardFieldColors(isDark),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    listOf(
                        "Insert Image" to { showCommandPalette = false; imagePickerLauncher.launch("image/*") },
                        "Draw Rectangle" to { showCommandPalette = false; viewModel.setTool(WhiteboardTool.RECTANGLE) },
                        "Draw Diamond" to { showCommandPalette = false; viewModel.setTool(WhiteboardTool.DIAMOND) },
                        "Draw Arrow" to { showCommandPalette = false; viewModel.setTool(WhiteboardTool.ARROW) },
                        "Toggle Laser Pointer" to { showCommandPalette = false; viewModel.setTool(WhiteboardTool.LASER) },
                        "Place ruler" to { showCommandPalette = false; placeMathTool(MathToolKind.RULER, canvasCenter()) },
                        "Place compass" to { showCommandPalette = false; placeMathTool(MathToolKind.COMPASS, canvasCenter()) },
                        "Place protractor" to { showCommandPalette = false; placeMathTool(MathToolKind.PROTRACTOR, canvasCenter()) },
                        "Place set square" to { showCommandPalette = false; placeMathTool(MathToolKind.SET_SQUARE, canvasCenter()) },
                        "Place triangle" to { showCommandPalette = false; placeMathTool(MathToolKind.TRIANGLE, canvasCenter()) },
                        "Flowchart: 3 Connected Nodes" to { showCommandPalette = false; viewModel.createThreeNodeFlowchart(canvasCenter()) },
                        "Flowchart: Process Node" to { showCommandPalette = false; viewModel.addFlowchartNode("process", canvasCenter()) },
                        "Flowchart: Decision Node" to { showCommandPalette = false; viewModel.addFlowchartNode("decision", canvasCenter()) },
                        "Flowchart: Terminator Node" to { showCommandPalette = false; viewModel.addFlowchartNode("terminator", canvasCenter()) },
                        "Stickers & Emoji" to { showCommandPalette = false; showStickerEmojiDialog = true },
                        "Countdown Timer" to { showCommandPalette = false; timerState = timerState.copy(isVisible = true); bringToolToFront(UtilityToolKind.TIMER) },
                        "Stopwatch" to { showCommandPalette = false; stopwatchState = stopwatchState.copy(isVisible = true); bringToolToFront(UtilityToolKind.STOPWATCH) },
                        "Scientific Calculator" to { showCommandPalette = false; calculatorState = calculatorState.copy(isVisible = true); bringToolToFront(UtilityToolKind.CALCULATOR) },
                        "Periodic Table" to { showCommandPalette = false; periodicTableState = periodicTableState.copy(isVisible = true); bringToolToFront(UtilityToolKind.PERIODIC_TABLE) },
                        "Graph paper" to { showCommandPalette = false; viewModel.toggleGraphPaper() },
                        "Standard eraser" to { showCommandPalette = false; viewModel.setEraserMode(EraserMode.STANDARD) },
                        "Smart eraser" to { showCommandPalette = false; viewModel.setEraserMode(EraserMode.SMART) },
                        "Clear page" to { showCommandPalette = false; showClearDialog = true },
                        "Export as PNG" to { showCommandPalette = false; viewModel.exportAsPng(context) { exportedUri = it; showExportDialog = true } },
                        "Export lesson (PDF/ZIP/IWB)" to {
                            showCommandPalette = false
                            exportResult = null
                            showExportLessonDialog = true
                        },
                        "Open GYOASH Lesson (.iwb)" to {
                            showCommandPalette = false
                            iwbPickerLauncher.launch(arrayOf("*/*"))
                        },
                        "Reset Zoom (100%)" to { showCommandPalette = false; viewModel.resetTransform() },
                        "Fit to Content" to { showCommandPalette = false; viewModel.fitToContent(canvasSize.width, canvasSize.height) },
                        "Paste from clipboard" to { showCommandPalette = false; viewModel.pasteStrokeClipboard() },
                        "Add new page" to { showCommandPalette = false; viewModel.addPage() },
                        "Duplicate current page" to { showCommandPalette = false; viewModel.duplicatePage(state.pageIndex) }
                    ).filter { it.first.contains(commandSearchQuery, ignoreCase = true) }
                        .forEach { (name, action) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { action() }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Bolt, null, tint = Color(0xFF5B5BD6), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    name,
                                    fontSize = 14.sp,
                                    color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1F2937),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                }
            }
        }
    }

    // Help & Shortcuts Dialog
    if (showHelpDialog) {
        val isDark = state.isDarkTheme || isSystemInDarkTheme()
        val textPrimary = if (isDark) Color(0xFFE2E8F0) else Color(0xFF4B5563)
        val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF6B7280)
        BoardModal(
            title = "Whiteboard guide",
            onDismiss = { showHelpDialog = false },
            confirmLabel = "Got it",
            onConfirm = { showHelpDialog = false },
            cancelLabel = null,
            isDark = isDark
        ) {
            Column(modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                Text(
                    "V selects. H pans. R rectangle, D diamond, O circle, A arrow, L line. P pen, plus pencil, marker, highlighter, and brush. T text. E eraser: Standard erases inside the circle, Smart removes the whole stroke. Palm touches wider than 3 cm are ignored while a stylus is down. K laser, B bucket fill, 9 inserts an image.",
                    fontSize = 13.sp,
                    color = textPrimary,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Drag the ruler, compass, or protractor from the toolbar onto the board. Drag either end to rotate. On the compass, drag the pencil to set the radius in centimetres, then tap it to draw a circle or sweep it to draw an arc. On the protractor, tap the scale to mark an angle in degrees. Draw along the ruler edge to place a straight line with its length. Two fingers on a tool move and rotate it. Drag a tool back to the toolbar to remove it. Graph paper adds a squared background. A rough circle, ellipse, rectangle, square, triangle, line, arrow, star, or diamond offers Convert to shape half a second after you lift the pen. Yes converts it, No keeps the drawing, and no answer converts it after two seconds. Turn Shape recognition off in the menu for free drawing. A converted shape can be moved, resized from its corners, and recoloured.",
                    fontSize = 12.sp,
                    color = textSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text("Pinch with 2 fingers on empty board to zoom from 10% to 500%. Two fingers also pan. Ctrl+Z undoes, Ctrl+Y redoes. Double tap fits the page.", fontSize = 12.sp, color = textSecondary)
                if (state.panelIdForHelp.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Panel: ${state.panelIdForHelp} · ${state.licenseStateForHelp}",
                        fontSize = 11.sp,
                        color = if (isDark) Color(0xFF64748B) else Color(0xFF9CA3AF)
                    )
                }
            }
        }
    }

    if (showImageSearch) {
        val isDark = state.isDarkTheme || isSystemInDarkTheme()
        val dialogBg = if (isDark) Color(0xFF1E293B) else Color.White
        val subtitleColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF6B7280)

        Dialog(onDismissRequest = { showImageSearch = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = dialogBg,
                border = if (isDark) BorderStroke(1.dp, Color(0xFF334155)) else null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    BoardModalHeader("Google Images", isDark = isDark) { showImageSearch = false }
                    Text(
                        "Search stays in this window. Tap a picture to paste it on the board.",
                        fontSize = 12.sp,
                        color = subtitleColor
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = imageQuery,
                        onValueChange = { imageQuery = it },
                        singleLine = true,
                        placeholder = { Text("Search images", color = if (isDark) Color(0xFF94A3B8) else Color(0xFF6B7280)) },
                        textStyle = TextStyle(
                            color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF111827),
                            fontSize = 14.sp
                        ),
                        colors = boardFieldColors(isDark),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { imageSearchTerm = imageQuery.trim() }),
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(onClick = { imageSearchTerm = imageQuery.trim() }) {
                        Text("Search in this window", color = if (isDark) Color(0xFF818CF8) else Color(0xFF5B5BD6))
                    }
                    AndroidView(
                        factory = { viewContext ->
                            android.webkit.WebView(viewContext).apply {
                                browseInsideApp(onImage = { raw ->
                                    val image = googleImageResultUrl(raw) ?: raw
                                    if (image.startsWith("http://") || image.startsWith("https://")) {
                                        val centerWorld = CanvasTransform.screenToWorld(
                                            screenPoint = Offset(canvasSize.width / 2f, canvasSize.height / 2f),
                                            scale = canvasScale,
                                            offset = Offset(canvasOffsetX, canvasOffsetY)
                                        )
                                        viewModel.addImage(image, centerWorld)
                                        showImageSearch = false
                                    }
                                })
                            }
                        },
                        update = { web ->
                            val target = if (imageSearchTerm.isBlank()) {
                                "https://www.google.com/search?tbm=isch&udm=2"
                            } else {
                                googleImageSearchUrl(imageSearchTerm)
                            }
                            if (web.tag != target) {
                                web.tag = target
                                web.loadUrl(target)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(420.dp)
                    )
                }
            }
        }
    }

    if (showYouTubeDialog) {
        val isDark = state.isDarkTheme || isSystemInDarkTheme()
        val dialogBg = if (isDark) Color(0xFF1E293B) else Color.White
        val subtitleColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF6B7280)

        Dialog(onDismissRequest = { showYouTubeDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = dialogBg,
                border = if (isDark) BorderStroke(1.dp, Color(0xFF334155)) else null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    BoardModalHeader("YouTube", isDark = isDark) { showYouTubeDialog = false }
                    Text("Paste a link or tap a video in the results.", fontSize = 12.sp, color = subtitleColor)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = youtubeQuery,
                        onValueChange = { youtubeQuery = it },
                        singleLine = true,
                        placeholder = { Text("Link or search", color = if (isDark) Color(0xFF94A3B8) else Color(0xFF6B7280)) },
                        textStyle = TextStyle(
                            color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF111827),
                            fontSize = 14.sp
                        ),
                        colors = boardFieldColors(isDark),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = {
                        val centerWorld = CanvasTransform.screenToWorld(
                            screenPoint = Offset(canvasSize.width / 2f, canvasSize.height / 2f),
                            scale = canvasScale,
                            offset = Offset(canvasOffsetX, canvasOffsetY)
                        )
                        if (youtubeVideoId(youtubeQuery) != null) {
                            viewModel.addYouTube(youtubeQuery, centerWorld)
                            showYouTubeDialog = false
                        }
                    }) { Text("Place link on board", color = if (isDark) Color(0xFF818CF8) else Color(0xFF5B5BD6)) }
                    AndroidView(
                        factory = { viewContext ->
                            android.webkit.WebView(viewContext).apply {
                                browseInsideApp(onNavigate = { url ->
                                    if (youtubeVideoId(url) == null) return@browseInsideApp false
                                    val centerWorld = CanvasTransform.screenToWorld(
                                        screenPoint = Offset(canvasSize.width / 2f, canvasSize.height / 2f),
                                        scale = canvasScale,
                                        offset = Offset(canvasOffsetX, canvasOffsetY)
                                    )
                                    viewModel.addYouTube(url, centerWorld)
                                    showYouTubeDialog = false
                                    true
                                })
                            }
                        },
                        update = { web ->
                            val target = if (youtubeQuery.isBlank()) "https://m.youtube.com" else youtubeSearchUrl(youtubeQuery)
                            if (web.tag != target) {
                                web.tag = target
                                web.loadUrl(target)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(420.dp)
                    )
                }
            }
        }
    }

    if (paneWebIndex >= 0) {
        BoardModal(
            title = "Open a page",
            onDismiss = { paneWebIndex = -1 },
            confirmLabel = "Open",
            onConfirm = {
                viewModel.openPaneWeb(paneWebIndex, paneWebAddress)
                paneWebIndex = -1
            }
        ) {
            OutlinedTextField(
                value = paneWebAddress,
                onValueChange = { paneWebAddress = it },
                singleLine = true,
                placeholder = { Text("https://") },
                colors = boardFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showWebEmbedDialog) {
        BoardModal(
            title = "Website",
            onDismiss = { showWebEmbedDialog = false },
            confirmLabel = "Embed",
            onConfirm = {
                viewModel.addWebEmbed(textInputValue.ifBlank { "https://avitrisphere.app" }, pendingWorldPoint)
                showWebEmbedDialog = false
            }
        ) {
            OutlinedTextField(
                value = textInputValue,
                onValueChange = { textInputValue = it },
                placeholder = { Text("https://example.com") },
                singleLine = true,
                colors = boardFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showTextInputDialog) {
        BoardModal(
            title = "Add text",
            onDismiss = { showTextInputDialog = false },
            confirmLabel = "Add",
            onConfirm = {
                viewModel.addTextOrSticky(
                    text = textInputValue,
                    worldPoint = pendingWorldPoint,
                    isSticky = false,
                    color = state.selectedColor
                )
                showTextInputDialog = false
            }
        ) {
            OutlinedTextField(
                value = textInputValue,
                onValueChange = { textInputValue = it },
                placeholder = { Text("Type something…") },
                colors = boardFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (editingNodeStrokeId != null) {
        BoardModal(
            title = "Node text",
            onDismiss = { editingNodeStrokeId = null },
            confirmLabel = "Save",
            onConfirm = {
                editingNodeStrokeId?.let { id ->
                    viewModel.updateStrokeText(id, editingNodeText)
                }
                editingNodeStrokeId = null
            }
        ) {
            OutlinedTextField(
                value = editingNodeText,
                onValueChange = { editingNodeText = it },
                placeholder = { Text("Type text for this node…") },
                colors = boardFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showEditTitleDialog) {
        var editedTitle by remember { mutableStateOf(state.noteTitle) }
        BoardModal(
            title = stringResource(R.string.dialog_edit_title),
            onDismiss = { showEditTitleDialog = false },
            confirmLabel = stringResource(R.string.save),
            isDark = state.isDarkTheme,
            onConfirm = {
                viewModel.updateTitle(editedTitle.ifBlank { "Untitled" })
                showEditTitleDialog = false
            }
        ) {
            OutlinedTextField(
                value = editedTitle,
                onValueChange = { editedTitle = it },
                singleLine = true,
                colors = boardFieldColors(state.isDarkTheme),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showProfileDialog) {
        var profileDraft by remember(state.userName) { mutableStateOf(state.userName) }
        BoardModal(
            title = stringResource(R.string.tool_profile),
            onDismiss = { showProfileDialog = false },
            confirmLabel = stringResource(R.string.save),
            isDark = state.isDarkTheme,
            onConfirm = {
                viewModel.setUserName(profileDraft)
                showProfileDialog = false
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Your name",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (state.isDarkTheme) Color(0xFFF1F5F9) else Color(0xFF1F2937)
                )
                OutlinedTextField(
                    value = profileDraft,
                    onValueChange = { profileDraft = it },
                    singleLine = true,
                    colors = boardFieldColors(state.isDarkTheme),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showDeleteDialog) {
        BoardModal(
            title = stringResource(R.string.dialog_delete_title),
            onDismiss = { showDeleteDialog = false },
            confirmLabel = stringResource(R.string.delete),
            isDark = state.isDarkTheme,
            onConfirm = {
                viewModel.deleteNote { onNavigateBack() }
                showDeleteDialog = false
            },
            confirmDestructive = true
        ) {
            Text(stringResource(R.string.dialog_delete_msg), color = if (state.isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF6B7280), fontSize = 14.sp)
        }
    }

    if (state.showCloseSplitDialog) {
        val activePaneIndex = state.splitActive.coerceIn(0, (state.splitPanes.size - 1).coerceAtLeast(0))
        val activePane = state.splitPanes.getOrNull(activePaneIndex)
        val activeStrokeCount = activePane?.strokes?.size ?: 0
        val totalStrokes = state.splitPanes.sumOf { it.strokes.size }

        BoardModal(
            title = stringResource(R.string.dialog_split_leave_title),
            onDismiss = { viewModel.dismissCloseSplitDialog() },
            confirmLabel = stringResource(R.string.dialog_keep_active),
            isDark = state.isDarkTheme,
            onConfirm = { viewModel.closeSplit(activePaneIndex) }
        ) {
            Text(stringResource(R.string.dialog_split_leave_msg), color = if (state.isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF6B7280), fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "Active: Pane ${activePaneIndex + 1} ($activeStrokeCount stroke${if (activeStrokeCount == 1) "" else "s"})",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (state.isDarkTheme) Color(0xFF60A5FA) else Color(0xFF2563EB)
            )
            if (state.splitPanes.size == 2 || (totalStrokes > activeStrokeCount && state.splitPanes.size > 1)) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (state.splitPanes.size == 2) {
                        val otherIndex = if (activePaneIndex == 0) 1 else 0
                        val otherPane = state.splitPanes.getOrNull(otherIndex)
                        val otherCount = otherPane?.strokes?.size ?: 0
                        TextButton(
                            onClick = { viewModel.closeSplit(otherIndex) },
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (state.isDarkTheme) Color(0xFF334155) else Color(0xFFF1F5F9))
                        ) {
                            Text("Keep Pane ${otherIndex + 1} ($otherCount)", fontSize = 12.sp, color = if (state.isDarkTheme) Color(0xFFF1F5F9) else Color(0xFF334155))
                        }
                    }
                    if (totalStrokes > activeStrokeCount && state.splitPanes.size > 1) {
                        TextButton(
                            onClick = { viewModel.closeSplit(-1) },
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (state.isDarkTheme) Color(0xFF334155) else Color(0xFFF1F5F9))
                        ) {
                            Text("Combine All ($totalStrokes)", fontSize = 12.sp, color = if (state.isDarkTheme) Color(0xFFF1F5F9) else Color(0xFF334155))
                        }
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        BoardModal(
            title = stringResource(R.string.dialog_clear_title),
            onDismiss = { showClearDialog = false },
            confirmLabel = stringResource(R.string.dialog_clear_title),
            isDark = state.isDarkTheme,
            onConfirm = {
                viewModel.clearCanvas()
                showClearDialog = false
            },
            confirmDestructive = true
        ) {
            Text(stringResource(R.string.dialog_clear_msg), color = if (state.isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF6B7280), fontSize = 14.sp)
        }
    }

    if (showExportDialog && exportedUri != null) {
        BoardModal(
            title = stringResource(R.string.dialog_exported_title),
            isDark = state.isDarkTheme,
            onDismiss = {
                showExportDialog = false
                exportedUri = null
            },
            confirmLabel = stringResource(R.string.share),
            onConfirm = {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, exportedUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Note"))
                showExportDialog = false
                exportedUri = null
            },
            cancelLabel = null
        ) {
            Text(stringResource(R.string.dialog_exported_msg), color = if (state.isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF6B7280), fontSize = 14.sp)
        }
    }

    // Export lesson dialog (PDF, PNG ZIP, IWB)
    if (showExportLessonDialog) {
        val allPages = viewModel.getAllPages()
        ExportLessonDialog(
            initialTitle = state.noteTitle,
            pageCount = allPages.size.coerceAtLeast(1),
            isExporting = isExporting,
            exportProgress = exportProgress,
            exportStatusMessage = exportStatusMessage,
            exportResult = exportResult,
            isDark = state.isDarkTheme,
            onDismiss = {
                showExportLessonDialog = false
                exportResult = null
                isExporting = false
            },
            onStartExport = { title, format, destination ->
                isExporting = true
                exportProgress = 0.05f
                exportStatusMessage = "Starting export for ${format.displayName}..."
                viewModel.exportLesson(
                    context = context,
                    lessonTitle = title,
                    format = format,
                    destination = destination,
                    onProgress = { progress, message ->
                        exportProgress = progress
                        exportStatusMessage = message
                    },
                    onResult = { result ->
                        isExporting = false
                        exportResult = result
                    }
                )
            },
            onOpenFile = { result ->
                WhiteboardExportManager.openExportedFile(context, result)
            },
            onShareFile = { result ->
                WhiteboardExportManager.launchShareIntent(context, result, state.noteTitle)
            }
        )
    }

    if (showStickerEmojiDialog) {
        StickerEmojiPickerDialog(
            onDismiss = { showStickerEmojiDialog = false },
            onSelectSticker = { kind, text ->
                val center = canvasCenter()
                viewModel.insertSticker(kind, center, text)
            },
            isDark = state.isDarkTheme
        )
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = currentLanguage,
            isDark = state.isDarkTheme,
            onLanguageSelected = { newLang ->
                onLanguageChange(newLang)
                panelSession?.setUiLocaleTag(newLang.code)
                viewModel.setUiLocaleTag(newLang.code)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }
    }
}

/** Dock position kept off the compose state so opening a panel does not redraw the page. */
private class ChromeBounds {
    var dock: Rect? = null
    var frost: List<Rect> = emptyList()
}

@Composable
private fun boardFieldColors(isDark: Boolean = LocalDarkTheme.current) = OutlinedTextFieldDefaults.colors(
    focusedTextColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF111827),
    unfocusedTextColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF111827),
    focusedBorderColor = Color(0xFF6366F1),
    unfocusedBorderColor = if (isDark) Color(0xFF475569) else Color(0xFFE5E7EB),
    focusedContainerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC),
    unfocusedContainerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC),
    cursorColor = Color(0xFF6366F1),
    focusedPlaceholderColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF6B7280),
    unfocusedPlaceholderColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF6B7280)
)

@Composable
private fun BoardModalHeader(title: String, isDark: Boolean = LocalDarkTheme.current, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF111827),
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0xFF334155) else Color(0xFFF3F4F6))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Close",
                tint = if (isDark) Color(0xFFCBD5E1) else Color(0xFF6B7280),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun BoardModal(
    title: String,
    onDismiss: () -> Unit,
    confirmLabel: String? = null,
    onConfirm: (() -> Unit)? = null,
    confirmDestructive: Boolean = false,
    cancelLabel: String? = "Cancel",
    isDark: Boolean = LocalDarkTheme.current,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isDark) Color(0xFF1E293B) else Color.White,
            border = if (isDark) BorderStroke(1.dp, Color(0xFF334155)) else null,
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                BoardModalHeader(title, isDark = isDark, onDismiss = onDismiss)
                Spacer(Modifier.height(14.dp))
                content()
                if (confirmLabel != null || cancelLabel != null) {
                    Spacer(Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (cancelLabel != null) {
                            TextButton(onClick = onDismiss) {
                                Text(
                                    cancelLabel,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF6B7280),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                        }
                        if (confirmLabel != null && onConfirm != null) {
                            val fill = if (confirmDestructive) {
                                Brush.linearGradient(listOf(Color(0xFFEF4444), Color(0xFFF97316)))
                            } else {
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF22D3EE), Color(0xFF3B82F6), Color(0xFFA855F7))
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(fill)
                                    .clickable(onClick = onConfirm)
                                    .padding(horizontal = 18.dp, vertical = 10.dp)
                            ) {
                                Text(confirmLabel, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun frostUnchanged(current: List<Rect>, next: List<Rect>): Boolean {
    if (current.size != next.size) return false
    for (index in current.indices) {
        val before = current[index]
        val after = next[index]
        if (kotlin.math.abs(before.left - after.left) > 1f) return false
        if (kotlin.math.abs(before.top - after.top) > 1f) return false
        if (kotlin.math.abs(before.right - after.right) > 1f) return false
        if (kotlin.math.abs(before.bottom - after.bottom) > 1f) return false
    }
    return true
}
