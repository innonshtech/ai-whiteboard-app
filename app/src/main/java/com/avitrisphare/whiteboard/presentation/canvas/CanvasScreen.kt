package com.avitrisphare.whiteboard.presentation.canvas

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
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
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
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
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.avitrisphare.whiteboard.core.theme.LocalAvitriSphareColors
import com.avitrisphare.whiteboard.domain.model.CanvasBackground
import com.avitrisphare.whiteboard.domain.model.Stroke
import com.avitrisphare.whiteboard.domain.model.StrokeType
import kotlinx.coroutines.launch
import java.io.InputStream
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

private class LiveInk {
    var stroke: Stroke? = null
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
    val strokeColor = if (stroke.isHighlighter) Color(stroke.colorValue).copy(alpha = 0.4f) else Color(stroke.colorValue)

    when (stroke.strokeType) {
        StrokeType.CIRCLE -> {
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
            }
        }
        StrokeType.RECTANGLE -> {
            if (pointsList.size >= 2) {
                val p1 = pointsList.first()
                val p2 = pointsList.last()
                val left = minOf(p1.x, p2.x)
                val top = minOf(p1.y, p2.y)
                val width = kotlin.math.abs(p2.x - p1.x)
                val height = kotlin.math.abs(p2.y - p1.y)
                if (stroke.fillColor != 0) {
                    drawRoundRect(
                        color = Color(stroke.fillColor),
                        topLeft = Offset(left, top),
                        size = Size(width, height),
                        cornerRadius = CornerRadius(4f / safeScale, 4f / safeScale)
                    )
                }
                drawRoundRect(
                    color = strokeColor,
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    cornerRadius = CornerRadius(4f / safeScale, 4f / safeScale),
                    style = DrawStroke(width = strokeWidth)
                )
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
        StrokeType.IMAGE -> {
            if (pointsList.size >= 2 && stroke.imageUri.isNotEmpty()) {
                val p1 = pointsList.first()
                val p2 = pointsList.last()
                val left = minOf(p1.x, p2.x)
                val top = minOf(p1.y, p2.y)
                val width = kotlin.math.abs(p2.x - p1.x).toInt().coerceAtLeast(10)
                val height = kotlin.math.abs(p2.y - p1.y).toInt().coerceAtLeast(10)

                try {
                    val uri = Uri.parse(stroke.imageUri)
                    val input: InputStream? = context.contentResolver.openInputStream(uri)
                    val bmp = BitmapFactory.decodeStream(input)
                    input?.close()
                    if (bmp != null) {
                        val composeBmp = bmp.asImageBitmap()
                        drawImage(
                            image = composeBmp,
                            dstOffset = IntOffset(left.toInt(), top.toInt()),
                            dstSize = IntSize(width, height)
                        )
                    }
                } catch (e: Exception) {
                    drawRoundRect(
                        color = Color(0xFF1E293B),
                        topLeft = Offset(left, top),
                        size = Size(width.toFloat(), height.toFloat()),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                }
            }
        }
        StrokeType.WEB_EMBED -> {
            val origin = pointsList.first()
            val w = 280f
            val h = 130f
            drawRoundRect(
                color = Color(0xFF1E1E2E),
                topLeft = origin,
                size = Size(w, h),
                cornerRadius = CornerRadius(12f, 12f)
            )
            drawRoundRect(
                color = Color(0xFF6366F1),
                topLeft = origin,
                size = Size(w, h),
                cornerRadius = CornerRadius(12f, 12f),
                style = DrawStroke(width = 1.5f)
            )
            drawContext.canvas.nativeCanvas.apply {
                val titlePaint = Paint().apply {
                    color = 0xFFFFFFFF.toInt()
                    textSize = 15f
                    isAntiAlias = true
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val urlPaint = Paint().apply {
                    color = 0xFF818CF8.toInt()
                    textSize = 12f
                    isAntiAlias = true
                }
                drawText("🌐 Web Embed", origin.x + 16f, origin.y + 36f, titlePaint)
                val urlText = stroke.text.ifEmpty { "https://..." }
                drawText(urlText.take(35), origin.x + 16f, origin.y + 70f, urlPaint)
            }
        }
        StrokeType.STICKY_NOTE -> {
            val origin = pointsList.first()
            val w = 220f
            val h = 180f
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
        StrokeType.TEXT -> {
            val origin = pointsList.first()
            drawContext.canvas.nativeCanvas.apply {
                val paint = Paint().apply {
                    color = stroke.colorValue
                    textSize = 20f
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
    }

    if (isSelected) {
        val b = stroke.bounds
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
    onClick: () -> Unit
) {
    val activeBg = if (isSelected) Color(0xFFEEF2FF) else Color.Transparent
    val activeText = if (isSelected) Color(0xFF4338CA) else Color(0xFF1F2937)
    val activeIcon = if (isSelected) Color(0xFF4F46E5) else Color(0xFF4B5563)

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
                color = Color(0xFF9CA3AF),
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
            .clickable(onClick = onClick)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            when (type) {
                CanvasBackground.DARK -> drawRect(Color(0xFF171717))
                CanvasBackground.WHITE -> drawRect(Color.White)
                CanvasBackground.LINED -> {
                    drawRect(Color(0xFF171717))
                    for (y in 0..size.height.toInt() step 8) {
                        drawLine(Color(0xFF2E2E2E), Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), 1f)
                    }
                }
                CanvasBackground.DOTTED -> {
                    drawRect(Color(0xFF171717))
                    for (y in 0..size.height.toInt() step 8)
                        for (x in 0..size.width.toInt() step 8)
                            drawCircle(Color(0xFF2E2E2E), 1f, Offset(x.toFloat(), y.toFloat()))
                }
                CanvasBackground.GRID -> {
                    drawRect(Color(0xFF171717))
                    for (v in 0..size.height.toInt() step 8)
                        drawLine(Color(0xFF2E2E2E), Offset(0f, v.toFloat()), Offset(size.width, v.toFloat()), 1f)
                    for (h in 0..size.width.toInt() step 8)
                        drawLine(Color(0xFF2E2E2E), Offset(h.toFloat(), 0f), Offset(h.toFloat(), size.height), 1f)
                }
            }
        }
    }
}

@Composable
fun CanvasScreen(
    noteId: String,
    noteTitle: String = "",
    viewModel: CanvasViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    LaunchedEffect(noteId) {
        viewModel.loadNote(noteId)
    }

    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    var showSidebar by remember { mutableStateOf(false) }
    var showLiveCollabDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showEditTitleDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showTextInputDialog by remember { mutableStateOf(false) }
    var showStickyInputDialog by remember { mutableStateOf(false) }
    var showWebEmbedDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showCommandPalette by remember { mutableStateOf(false) }
    var commandSearchQuery by remember { mutableStateOf("") }
    var pendingWorldPoint by remember { mutableStateOf(Offset.Zero) }
    var textInputValue by remember { mutableStateOf("") }
    var stickyColorSelected by remember { mutableStateOf(STICKY_COLORS.first()) }
    var exportedUri by remember { mutableStateOf<Uri?>(null) }
    var lassoPoints by remember { mutableStateOf(listOf<Offset>()) }

    var canvasScale by remember { mutableFloatStateOf(1f) }
    val liveInkTick = remember { mutableIntStateOf(0) }
    val liveInk = remember { LiveInk() }
    var canvasOffsetX by remember { mutableFloatStateOf(0f) }
    var canvasOffsetY by remember { mutableFloatStateOf(0f) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }

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

    LaunchedEffect(state.scale, state.offsetX, state.offsetY) {
        canvasScale = state.scale
        canvasOffsetX = state.offsetX
        canvasOffsetY = state.offsetY
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (state.isDarkCanvas) Color(0xFF171717) else Color.White)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
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
        // 1. Full-bleed Infinite Whiteboard Canvas Viewport
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { canvasSize = Size(it.width.toFloat(), it.height.toFloat()) }
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
                    state.strokeWidth,
                    state.isToolLocked,
                    state.selectedStrokeIds,
                    state.isDrawToShapeEnabled
                ) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val downWorld = CanvasTransform.screenToWorld(
                            screenPoint = down.position,
                            scale = canvasScale,
                            offset = Offset(canvasOffsetX, canvasOffsetY)
                        )
                        var prevPosition = down.position
                        var shapePoints = listOf(downWorld)
                        val inkPoints = ArrayList<Offset>(256)
                        val inkPressures = ArrayList<Float>(256)
                        inkPoints.add(downWorld)
                        inkPressures.add(down.inkPressure())
                        var wasMultiTouch = false
                        val writingNow = state.currentTool in WRITING_TOOLS
                        val inkTemplate = if (writingNow) {
                            Stroke(
                                points = inkPoints,
                                pointPressures = inkPressures,
                                colorValue = state.selectedColor,
                                strokeWidth = state.strokeWidth,
                                isHighlighter = state.currentTool == WhiteboardTool.HIGHLIGHTER,
                                strokeType = state.currentTool.toStrokeType()
                            )
                        } else {
                            null
                        }
                        fun publishInk() {
                            val template = inkTemplate ?: return
                            liveInk.stroke = template.copy(
                                points = ArrayList(inkPoints),
                                pointPressures = ArrayList(inkPressures)
                            )
                            liveInkTick.intValue = liveInkTick.intValue + 1
                        }
                        fun appendInk(screen: Offset, pressure: Float) {
                            val world = CanvasTransform.screenToWorld(
                                screenPoint = screen,
                                scale = canvasScale,
                                offset = Offset(canvasOffsetX, canvasOffsetY)
                            )
                            val last = inkPoints.lastOrNull()
                            if (last != null) {
                                val dx = (world.x - last.x) * canvasScale
                                val dy = (world.y - last.y) * canvasScale
                                if (dx * dx + dy * dy < 0.49f) return
                            }
                            inkPoints.add(world)
                            inkPressures.add(pressure)
                        }
                        var isDraggingSelection = false

                        when (state.currentTool) {
                            WhiteboardTool.HAND -> {
                                // Hand pan mode
                            }
                            WhiteboardTool.SELECT -> {
                                val hit = viewModel.selectStrokeAt(downWorld)
                                if (hit) {
                                    isDraggingSelection = true
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
                            WhiteboardTool.STICKY_NOTE -> {
                                pendingWorldPoint = downWorld
                                textInputValue = ""
                                showStickyInputDialog = true
                            }
                            WhiteboardTool.ERASER -> {
                                val eraseRadius = 24f / canvasScale.coerceAtLeast(0.001f)
                                viewModel.eraseAt(downWorld, eraseRadius)
                            }
                            WhiteboardTool.PEN,
                            WhiteboardTool.PENCIL,
                            WhiteboardTool.MARKER,
                            WhiteboardTool.HIGHLIGHTER,
                            WhiteboardTool.BRUSH -> publishInk()
                            WhiteboardTool.RECTANGLE,
                            WhiteboardTool.DIAMOND,
                            WhiteboardTool.CIRCLE,
                            WhiteboardTool.ARROW,
                            WhiteboardTool.LINE,
                            WhiteboardTool.FRAME -> {
                                val sType = when (state.currentTool) {
                                    WhiteboardTool.RECTANGLE -> StrokeType.RECTANGLE
                                    WhiteboardTool.DIAMOND -> StrokeType.DIAMOND
                                    WhiteboardTool.CIRCLE -> StrokeType.CIRCLE
                                    WhiteboardTool.ARROW -> StrokeType.ARROW
                                    WhiteboardTool.LINE -> StrokeType.LINE
                                    WhiteboardTool.FRAME -> StrokeType.FRAME
                                    else -> StrokeType.PEN
                                }
                                val newStroke = Stroke(
                                    points = listOf(downWorld, downWorld),
                                    colorValue = state.selectedColor,
                                    strokeWidth = state.strokeWidth,
                                    strokeType = sType
                                )
                                shapePoints = newStroke.points
                                viewModel.startStroke(newStroke)
                            }
                            else -> {}
                        }

                        do {
                            val event = awaitPointerEvent()
                            if (event.changes.size > 1) {
                                wasMultiTouch = true
                                val zoomChange = event.calculateZoom()
                                val panChange = event.calculatePan()
                                val centroid = event.calculateCentroid(useCurrent = false)

                                if (zoomChange != 1f || panChange != Offset.Zero) {
                                    val newScale = (canvasScale * zoomChange).coerceIn(
                                        CanvasTransform.MIN_SCALE,
                                        CanvasTransform.MAX_SCALE
                                    )
                                    val newOffset = CanvasTransform.calculatePivotZoomOffset(
                                        oldScale = canvasScale,
                                        newScale = newScale,
                                        oldOffset = Offset(canvasOffsetX, canvasOffsetY),
                                        centroid = centroid,
                                        panDelta = panChange
                                    )
                                    canvasScale = newScale
                                    canvasOffsetX = newOffset.x
                                    canvasOffsetY = newOffset.y
                                    viewModel.updateTransform(canvasScale, canvasOffsetX, canvasOffsetY)
                                }
                                event.changes.forEach { it.consume() }
                            } else {
                                val change = event.changes.first()
                                if (change.pressed) {
                                    val curWorld = CanvasTransform.screenToWorld(
                                        screenPoint = change.position,
                                        scale = canvasScale,
                                        offset = Offset(canvasOffsetX, canvasOffsetY)
                                    )

                                    if (state.currentTool == WhiteboardTool.HAND) {
                                        val panX = change.position.x - prevPosition.x
                                        val panY = change.position.y - prevPosition.y
                                        canvasOffsetX += panX
                                        canvasOffsetY += panY
                                        viewModel.updateTransform(canvasScale, canvasOffsetX, canvasOffsetY)
                                    } else if (state.currentTool == WhiteboardTool.SELECT && isDraggingSelection) {
                                        val prevWorld = CanvasTransform.screenToWorld(
                                            screenPoint = prevPosition,
                                            scale = canvasScale,
                                            offset = Offset(canvasOffsetX, canvasOffsetY)
                                        )
                                        val delta = Offset(curWorld.x - prevWorld.x, curWorld.y - prevWorld.y)
                                        viewModel.beginSelectionMove()
                                        viewModel.moveSelectedStrokes(delta)
                                    } else if (state.currentTool == WhiteboardTool.LASER) {
                                        viewModel.addLaserPoint(curWorld)
                                    } else if (state.currentTool == WhiteboardTool.LASSO) {
                                        lassoPoints = lassoPoints + curWorld
                                    } else if (state.currentTool == WhiteboardTool.ERASER) {
                                        val eraseRadius = 24f / canvasScale.coerceAtLeast(0.001f)
                                        viewModel.eraseAt(curWorld, eraseRadius)
                                    } else if (writingNow) {
                                        change.pastPositions().forEach { appendInk(it, change.inkPressure()) }
                                        appendInk(change.position, change.inkPressure())
                                        publishInk()
                                    } else if (state.currentTool in listOf(
                                            WhiteboardTool.RECTANGLE,
                                            WhiteboardTool.DIAMOND,
                                            WhiteboardTool.CIRCLE,
                                            WhiteboardTool.ARROW,
                                            WhiteboardTool.LINE,
                                            WhiteboardTool.FRAME
                                        )
                                    ) {
                                        shapePoints = listOf(downWorld, curWorld)
                                        viewModel.updateStroke(shapePoints)
                                    }
                                    prevPosition = change.position
                                    change.consume()
                                }
                            }
                        } while (event.changes.any { it.pressed })

                        viewModel.endSelectionMove()

                        // Release Touch
                        if (!wasMultiTouch) {
                            if (state.currentTool == WhiteboardTool.LASSO) {
                                viewModel.lassoSelect(lassoPoints)
                                lassoPoints = emptyList()
                                if (!state.isToolLocked) {
                                    viewModel.setTool(WhiteboardTool.SELECT)
                                }
                            } else if (state.currentTool == WhiteboardTool.ERASER) {
                                viewModel.finishErasing()
                            } else if (writingNow && inkPoints.isNotEmpty()) {
                                val finished = if (state.currentTool == WhiteboardTool.PEN && state.isDrawToShapeEnabled && inkPoints.size >= 5) {
                                    viewModel.applyShapeRecognition(inkPoints) ?: inkTemplate!!.copy(
                                        points = ArrayList(inkPoints),
                                        pointPressures = ArrayList(inkPressures)
                                    )
                                } else {
                                    inkTemplate!!.copy(
                                        points = ArrayList(inkPoints),
                                        pointPressures = ArrayList(inkPressures)
                                    )
                                }
                                liveInk.stroke = null
                                liveInkTick.intValue = liveInkTick.intValue + 1
                                viewModel.commitStroke(finished)
                            } else if (state.currentTool in listOf(
                                    WhiteboardTool.RECTANGLE,
                                    WhiteboardTool.DIAMOND,
                                    WhiteboardTool.CIRCLE,
                                    WhiteboardTool.ARROW,
                                    WhiteboardTool.LINE,
                                    WhiteboardTool.FRAME
                                ) && shapePoints.size >= 2
                            ) {
                                viewModel.updateStroke(shapePoints)
                                viewModel.finishStroke()
                                if (!state.isToolLocked) {
                                    viewModel.setTool(WhiteboardTool.SELECT)
                                }
                            }
                        }
                    }
                }
        ) {
            val bgColor = when (state.canvasBackground) {
                CanvasBackground.DARK -> Color(0xFF171717)
                else -> Color.White
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(bgColor, size = size)

                val viewportBounds = CanvasTransform.calculateViewportWorldBounds(
                    viewportSize = size,
                    scale = canvasScale,
                    offset = Offset(canvasOffsetX, canvasOffsetY)
                )

                withTransform({
                    translate(canvasOffsetX, canvasOffsetY)
                    scale(canvasScale, canvasScale, pivot = Offset.Zero)
                }) {
                    val gridColor = if (state.canvasBackground == CanvasBackground.DARK) Color(0xFF2E2E2E) else Color(0xFFD5D8DE)
                    val spacing = CanvasTransform.getAdaptiveGridSpacing(80f, canvasScale)

                    when (state.canvasBackground) {
                        CanvasBackground.LINED -> {
                            val startY = floor(viewportBounds.top / spacing) * spacing
                            val endY = ceil(viewportBounds.bottom / spacing) * spacing
                            var y = startY
                            val lineThickness = 1f / canvasScale.coerceAtLeast(0.5f)
                            while (y <= endY) {
                                drawLine(
                                    color = gridColor,
                                    start = Offset(viewportBounds.left, y),
                                    end = Offset(viewportBounds.right, y),
                                    strokeWidth = lineThickness
                                )
                                y += spacing
                            }
                        }
                        CanvasBackground.DOTTED -> {
                            val startX = floor(viewportBounds.left / spacing) * spacing
                            val endX = ceil(viewportBounds.right / spacing) * spacing
                            val startY = floor(viewportBounds.top / spacing) * spacing
                            val endY = ceil(viewportBounds.bottom / spacing) * spacing
                            val dotRadius = 1.25f / canvasScale.coerceAtLeast(0.5f)
                            var y = startY
                            while (y <= endY) {
                                var x = startX
                                while (x <= endX) {
                                    drawCircle(
                                        color = gridColor,
                                        radius = dotRadius,
                                        center = Offset(x, y)
                                    )
                                    x += spacing
                                }
                                y += spacing
                            }
                        }
                        CanvasBackground.GRID -> {
                            val startX = floor(viewportBounds.left / spacing) * spacing
                            val endX = ceil(viewportBounds.right / spacing) * spacing
                            val startY = floor(viewportBounds.top / spacing) * spacing
                            val endY = ceil(viewportBounds.bottom / spacing) * spacing
                            val lineThickness = 1f / canvasScale.coerceAtLeast(0.5f)
                            var x = startX
                            while (x <= endX) {
                                drawLine(
                                    color = gridColor,
                                    start = Offset(x, viewportBounds.top),
                                    end = Offset(x, viewportBounds.bottom),
                                    strokeWidth = lineThickness
                                )
                                x += spacing
                            }
                            var y = startY
                            while (y <= endY) {
                                drawLine(
                                    color = gridColor,
                                    start = Offset(viewportBounds.left, y),
                                    end = Offset(viewportBounds.right, y),
                                    strokeWidth = lineThickness
                                )
                                y += spacing
                            }
                        }
                        else -> {}
                    }

                    if (state.showGrid && state.canvasBackground != CanvasBackground.GRID) {
                        val startX = floor(viewportBounds.left / spacing) * spacing
                        val endX = ceil(viewportBounds.right / spacing) * spacing
                        val startY = floor(viewportBounds.top / spacing) * spacing
                        val endY = ceil(viewportBounds.bottom / spacing) * spacing
                        val lineThickness = 1f / canvasScale.coerceAtLeast(0.5f)
                        val overlay = if (state.isDarkCanvas) Color(0xFF3A3A3A) else Color(0xFFC5C8D0)
                        var x = startX
                        while (x <= endX) {
                            drawLine(
                                color = overlay,
                                start = Offset(x, viewportBounds.top),
                                end = Offset(x, viewportBounds.bottom),
                                strokeWidth = lineThickness
                            )
                            x += spacing
                        }
                        var y = startY
                        while (y <= endY) {
                            drawLine(
                                color = overlay,
                                start = Offset(viewportBounds.left, y),
                                end = Offset(viewportBounds.right, y),
                                strokeWidth = lineThickness
                            )
                            y += spacing
                        }
                    }

                    // Render visible strokes
                    val visibleStrokes = state.strokes.filter { stroke ->
                        stroke.bounds.overlaps(viewportBounds)
                    }

                    visibleStrokes.forEach { stroke ->
                        val isSelected = stroke.id in state.selectedStrokeIds
                        renderStroke(context, stroke, canvasScale, isSelected)
                    }

                    val liveTick = liveInkTick.intValue
                    if (liveTick >= 0) {
                        liveInk.stroke?.let { activeStroke ->
                            if (activeStroke.points.isNotEmpty()) {
                                renderStroke(context, activeStroke, canvasScale, false)
                            }
                        }
                    }

                    state.currentStroke?.let { activeStroke ->
                        if (activeStroke.points.isNotEmpty()) {
                            renderStroke(context, activeStroke, canvasScale, false)
                        }
                    }

                    if (state.laserPoints.isNotEmpty()) {
                        val center = state.laserPoints.last()
                        val radius = 10f / canvasScale.coerceAtLeast(0.5f)
                        drawCircle(
                            color = Color(0x55FF2A2A),
                            radius = radius * 2.2f,
                            center = center
                        )
                        drawCircle(
                            color = Color(0xFFFF2A2A),
                            radius = radius,
                            center = center
                        )
                    }

                    // Render Lasso Loop
                    if (lassoPoints.size >= 2) {
                        val lassoPath = lassoPoints.toSmoothedPath()
                        drawPath(
                            path = lassoPath,
                            color = Color(0xFF6366F1),
                            style = DrawStroke(
                                width = 1.5f / canvasScale.coerceAtLeast(0.5f),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                            )
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
            onExport = {
                viewModel.exportAsPng(context) { uri ->
                    exportedUri = uri
                    showExportDialog = true
                }
            },
            onLive = { showLiveCollabDialog = true },
            onHelp = { showHelpDialog = true },
            onClear = { showClearDialog = true },
            onRename = { showEditTitleDialog = true },
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
            onFit = { viewModel.fitToContent(canvasSize.width, canvasSize.height) }
        )

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
                Surface(
                    shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                    color = Color.White,
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
                                    color = Color(0xFF1F2937)
                                )
                                Text(
                                    "Whiteboard",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF6B7280)
                                )
                            }
                            IconButton(
                                onClick = { showSidebar = false },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF6B7280))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        SidebarMenuItem(
                            icon = Icons.Default.Edit,
                            label = "Rename board",
                            onClick = {
                                showSidebar = false
                                showEditTitleDialog = true
                            }
                        )

                        // 1. Open (Ctrl+O)
                        SidebarMenuItem(
                            icon = Icons.Default.FolderOpen,
                            label = "Open",
                            shortcut = "Ctrl+O",
                            onClick = {
                                showSidebar = false
                                onNavigateBack()
                            }
                        )

                        // 2. Save to... (Highlighted in User's Screenshot)
                        SidebarMenuItem(
                            icon = Icons.Default.Save,
                            label = "Save to...",
                            isSelected = true,
                            onClick = {
                                showSidebar = false
                                viewModel.exportNoteJson { path ->
                                    Toast.makeText(context, "Saved to $path", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )

                        // 3. Export image... (Ctrl+Shift+E)
                        SidebarMenuItem(
                            icon = Icons.Default.Image,
                            label = "Export image...",
                            shortcut = "Ctrl+Shift+E",
                            onClick = {
                                showSidebar = false
                                viewModel.exportAsPng(context) { uri ->
                                    exportedUri = uri
                                    showExportDialog = true
                                }
                            }
                        )

                        // 4. Live collaboration...
                        SidebarMenuItem(
                            icon = Icons.Default.Group,
                            label = "Live collaboration...",
                            onClick = {
                                showSidebar = false
                                showLiveCollabDialog = true
                            }
                        )

                        Divider(
                            color = Color(0xFFF3F4F6),
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        // 5. Command palette (Ctrl+/)
                        SidebarMenuItem(
                            icon = Icons.Default.Bolt,
                            label = "Command palette",
                            shortcut = "Ctrl+/",
                            onClick = {
                                showSidebar = false
                                showCommandPalette = true
                            }
                        )

                        // 6. Find on canvas (Ctrl+F)
                        SidebarMenuItem(
                            icon = Icons.Default.Search,
                            label = "Find on canvas",
                            shortcut = "Ctrl+F",
                            onClick = {
                                showSidebar = false
                                showCommandPalette = true
                            }
                        )

                        // 7. Help (?)
                        SidebarMenuItem(
                            icon = Icons.Default.HelpOutline,
                            label = "Help",
                            shortcut = "?",
                            onClick = {
                                showSidebar = false
                                showHelpDialog = true
                            }
                        )

                        // 8. Reset the canvas
                        SidebarMenuItem(
                            icon = Icons.Default.LayersClear,
                            label = "Reset the canvas",
                            onClick = {
                                showSidebar = false
                                showClearDialog = true
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

    // 6. Live Collaboration Modal Dialog (Exact Match to User's Images 2 & 3)
    if (showLiveCollabDialog) {
        Dialog(onDismissRequest = { showLiveCollabDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = if (!state.isLiveCollabActive) Alignment.CenterHorizontally else Alignment.Start
                ) {
                    // Title: Live collaboration
                    Text(
                        text = "Live collaboration",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5B5BD6)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!state.isLiveCollabActive) {
                        // Image 2 State: Before Session Started
                        Text(
                            text = "Invite people to collaborate on your drawing.",
                            fontSize = 14.sp,
                            color = Color(0xFF1F2937),
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Don't worry, the session is end-to-end encrypted, and fully private. Not even our server can see what you draw.",
                            fontSize = 13.sp,
                            color = Color(0xFF4B5563),
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Start Session Button
                        Button(
                            onClick = { viewModel.startLiveSession() },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF5B5BD6),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Start session",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        // Image 3 State: Active Live Session
                        // 1. Your name
                        Text(
                            text = "Your name",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1F2937)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = state.userName,
                            onValueChange = { viewModel.setUserName(it) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF3F4F6),
                                unfocusedContainerColor = Color(0xFFF3F4F6),
                                focusedBorderColor = Color(0xFF5B5BD6),
                                unfocusedBorderColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 2. Link with Copy button
                        val roomUrl = "https://avitrisphere.app/#room=${state.collabRoomId}"
                        Text(
                            text = "Link",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1F2937)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = roomUrl,
                                onValueChange = {},
                                readOnly = true,
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFFF9FAFB),
                                    unfocusedContainerColor = Color(0xFFF9FAFB),
                                    focusedBorderColor = Color(0xFFD1D5DB),
                                    unfocusedBorderColor = Color(0xFFE5E7EB)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("AvitriSphare Live Session", roomUrl)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Link copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF5B5BD6),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.height(52.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy link", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // 3. QR Code Preview
                        val qrBitmap = remember(roomUrl) { QRCodeHelper.generateQRCodeBitmap(roomUrl, 240) }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF9FAFB))
                                .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = qrBitmap,
                                contentDescription = "Room QR Code",
                                modifier = Modifier.size(160.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 4. Privacy & Disconnect note
                        Text(
                            text = "🔒 Don't worry, the session is end-to-end encrypted, and fully private. Not even our server can see what you draw.",
                            fontSize = 12.sp,
                            color = Color(0xFF4B5563),
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Stopping the session will disconnect you from the room, but you'll be able to continue working with the scene, locally. Note that this won't affect other people, and they'll still be able to collaborate on their version.",
                            fontSize = 11.sp,
                            color = Color(0xFF6B7280),
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 5. Stop session button
                        Button(
                            onClick = { viewModel.stopLiveSession() },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFEE2E2),
                                contentColor = Color(0xFFDC2626)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Stop session", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Command Palette Dialog
    if (showCommandPalette) {
        Dialog(onDismissRequest = { showCommandPalette = false }) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = commandSearchQuery,
                        onValueChange = { commandSearchQuery = it },
                        placeholder = { Text("Search tools & commands...") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF9CA3AF)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF9FAFB),
                            unfocusedContainerColor = Color(0xFFF9FAFB),
                            focusedBorderColor = Color(0xFF5B5BD6),
                            unfocusedBorderColor = Color(0xFFE5E7EB)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    listOf(
                        "Start Live Collaboration" to { showCommandPalette = false; showLiveCollabDialog = true },
                        "Insert Image" to { showCommandPalette = false; imagePickerLauncher.launch("image/*") },
                        "Draw Rectangle" to { showCommandPalette = false; viewModel.setTool(WhiteboardTool.RECTANGLE) },
                        "Draw Diamond" to { showCommandPalette = false; viewModel.setTool(WhiteboardTool.DIAMOND) },
                        "Draw Arrow" to { showCommandPalette = false; viewModel.setTool(WhiteboardTool.ARROW) },
                        "Add Sticky Note" to { showCommandPalette = false; showStickyInputDialog = true },
                        "Toggle Laser Pointer" to { showCommandPalette = false; viewModel.setTool(WhiteboardTool.LASER) },
                        "Export as PNG" to { showCommandPalette = false; viewModel.exportAsPng(context) { exportedUri = it; showExportDialog = true } },
                        "Reset Zoom (100%)" to { showCommandPalette = false; viewModel.resetTransform() },
                        "Fit to Content" to { showCommandPalette = false; viewModel.fitToContent(canvasSize.width, canvasSize.height) }
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
                                Text(name, fontSize = 14.sp, color = Color(0xFF1F2937), fontWeight = FontWeight.Medium)
                            }
                        }
                }
            }
        }
    }

    // Help & Shortcuts Dialog
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("Whiteboard Guide & Shortcuts", color = Color(0xFF1F2937), fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("• V: Selection tool\n• H: Hand (Pan tool)\n• R: Rectangle\n• D: Diamond\n• O: Circle\n• A: Arrow\n• L: Line\n• P: Pen, plus pencil, marker, highlighter, and brush\n• T: Text placement\n• N: Sticky Note\n• E: Eraser\n• K: Laser Pointer\n• B: Bucket Fill\n• 9: Insert Image", fontSize = 13.sp, color = Color(0xFF4B5563), lineHeight = 20.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Pinch with 2 fingers to zoom from 10% to 500%. Two fingers also pan. Ctrl+Z undoes, Ctrl+Y redoes. Double tap fits the page.", fontSize = 12.sp, color = Color(0xFF6B7280))
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("Got it", color = Color(0xFF5B5BD6), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White
        )
    }

    // Web Embed Dialog
    if (showWebEmbedDialog) {
        AlertDialog(
            onDismissRequest = { showWebEmbedDialog = false },
            title = { Text("Web Embed", color = LocalAvitriSphareColors.current.textPrimary) },
            text = {
                OutlinedTextField(
                    value = textInputValue,
                    onValueChange = { textInputValue = it },
                    placeholder = { Text("https://example.com") },
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
                TextButton(onClick = {
                    viewModel.addWebEmbed(textInputValue.ifBlank { "https://avitrisphere.app" }, pendingWorldPoint)
                    showWebEmbedDialog = false
                }) {
                    Text("Embed", color = LocalAvitriSphareColors.current.brand)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWebEmbedDialog = false }) {
                    Text("Cancel", color = LocalAvitriSphareColors.current.textMuted)
                }
            },
            containerColor = LocalAvitriSphareColors.current.surface
        )
    }

    // Text Input Modal Dialog
    if (showTextInputDialog) {
        AlertDialog(
            onDismissRequest = { showTextInputDialog = false },
            title = { Text("Add Text", color = LocalAvitriSphareColors.current.textPrimary) },
            text = {
                OutlinedTextField(
                    value = textInputValue,
                    onValueChange = { textInputValue = it },
                    placeholder = { Text("Type something...") },
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
                TextButton(onClick = {
                    viewModel.addTextOrSticky(
                        text = textInputValue,
                        worldPoint = pendingWorldPoint,
                        isSticky = false,
                        color = state.selectedColor
                    )
                    showTextInputDialog = false
                }) {
                    Text("Add", color = LocalAvitriSphareColors.current.brand)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTextInputDialog = false }) {
                    Text("Cancel", color = LocalAvitriSphareColors.current.textMuted)
                }
            },
            containerColor = LocalAvitriSphareColors.current.surface
        )
    }

    // Sticky Note Input Modal Dialog
    if (showStickyInputDialog) {
        AlertDialog(
            onDismissRequest = { showStickyInputDialog = false },
            title = { Text("Add Sticky Note", color = LocalAvitriSphareColors.current.textPrimary) },
            text = {
                Column {
                    OutlinedTextField(
                        value = textInputValue,
                        onValueChange = { textInputValue = it },
                        placeholder = { Text("Write a note...") },
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = LocalAvitriSphareColors.current.textPrimary,
                            unfocusedTextColor = LocalAvitriSphareColors.current.textPrimary,
                            focusedBorderColor = LocalAvitriSphareColors.current.brand,
                            unfocusedBorderColor = LocalAvitriSphareColors.current.borderStandard
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Color", fontSize = 12.sp, color = LocalAvitriSphareColors.current.textMuted)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        STICKY_COLORS.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(color))
                                    .border(
                                        if (stickyColorSelected == color) 2.5.dp else 1.dp,
                                        if (stickyColorSelected == color) LocalAvitriSphareColors.current.brand else Color(0x33000000),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { stickyColorSelected = color }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addTextOrSticky(
                        text = textInputValue,
                        worldPoint = pendingWorldPoint,
                        isSticky = true,
                        fill = stickyColorSelected
                    )
                    showStickyInputDialog = false
                }) {
                    Text("Add Note", color = LocalAvitriSphareColors.current.brand)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStickyInputDialog = false }) {
                    Text("Cancel", color = LocalAvitriSphareColors.current.textMuted)
                }
            },
            containerColor = LocalAvitriSphareColors.current.surface
        )
    }

    // Edit Title Dialog
    if (showEditTitleDialog) {
        var editedTitle by remember { mutableStateOf(state.noteTitle) }
        AlertDialog(
            onDismissRequest = { showEditTitleDialog = false },
            title = { Text("Edit Title", color = LocalAvitriSphareColors.current.textPrimary) },
            text = {
                OutlinedTextField(
                    value = editedTitle,
                    onValueChange = { editedTitle = it },
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
                TextButton(onClick = {
                    viewModel.updateTitle(editedTitle.ifBlank { "Untitled" })
                    showEditTitleDialog = false
                }) {
                    Text("Save", color = LocalAvitriSphareColors.current.brand)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditTitleDialog = false }) {
                    Text("Cancel", color = LocalAvitriSphareColors.current.textMuted)
                }
            },
            containerColor = LocalAvitriSphareColors.current.surface
        )
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Note", color = LocalAvitriSphareColors.current.textPrimary) },
            text = { Text("Are you sure you want to delete this note?", color = LocalAvitriSphareColors.current.textMuted) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteNote { onNavigateBack() }
                    showDeleteDialog = false
                }) {
                    Text("Delete", color = Color(0xFFFF6B6B))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = LocalAvitriSphareColors.current.textMuted)
                }
            },
            containerColor = LocalAvitriSphareColors.current.surface
        )
    }

    // Clear Canvas confirmation dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Canvas", color = LocalAvitriSphareColors.current.textPrimary) },
            text = { Text("Remove all strokes? This can be undone.", color = LocalAvitriSphareColors.current.textMuted) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearCanvas()
                    showClearDialog = false
                }) {
                    Text("Clear", color = Color(0xFFFF6B6B))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = LocalAvitriSphareColors.current.textMuted)
                }
            },
            containerColor = LocalAvitriSphareColors.current.surface
        )
    }

    // Export success dialog
    if (showExportDialog && exportedUri != null) {
        AlertDialog(
            onDismissRequest = {
                showExportDialog = false
                exportedUri = null
            },
            title = { Text("Export Successful", color = LocalAvitriSphareColors.current.textPrimary) },
            text = { Text("Note exported to Pictures/AvitriSphare", color = LocalAvitriSphareColors.current.textMuted) },
            confirmButton = {
                TextButton(onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, exportedUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Note"))
                    showExportDialog = false
                    exportedUri = null
                }) {
                    Text("Share", color = LocalAvitriSphareColors.current.brand)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showExportDialog = false
                    exportedUri = null
                }) {
                    Text("Close", color = LocalAvitriSphareColors.current.textMuted)
                }
            },
            containerColor = LocalAvitriSphareColors.current.surface
        )
    }
}