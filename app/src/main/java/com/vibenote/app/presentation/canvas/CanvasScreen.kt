package com.vibenote.app.presentation.canvas

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.vibenote.app.core.theme.LocalVibeColors
import com.vibenote.app.domain.model.CanvasBackground
import com.vibenote.app.domain.model.Stroke
import com.vibenote.app.domain.model.StrokeType
import kotlinx.coroutines.launch
import java.io.InputStream
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

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
        else -> {
            if (pointsList.size == 1) {
                drawCircle(
                    color = strokeColor,
                    radius = strokeWidth / 2f,
                    center = pointsList[0]
                )
            } else {
                val path = pointsList.toSmoothedPath()
                drawPath(
                    path = path,
                    color = strokeColor,
                    style = DrawStroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
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
    val borderColor = if (isSelected) LocalVibeColors.current.brand else LocalVibeColors.current.borderStandard
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
    val coroutineScope = rememberCoroutineScope()

    var showSidebar by remember { mutableStateOf(false) }
    var showLiveCollabDialog by remember { mutableStateOf(false) }
    var showOverflowPane by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showEditTitleDialog by remember { mutableStateOf(false) }
    var showBackgroundPicker by remember { mutableStateOf(false) }
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalVibeColors.current.background)
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
                        var points = listOf(downWorld)
                        var wasMultiTouch = false
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
                            WhiteboardTool.PEN -> {
                                val newStroke = Stroke(
                                    points = points,
                                    colorValue = state.selectedColor,
                                    strokeWidth = state.strokeWidth,
                                    strokeType = StrokeType.PEN
                                )
                                viewModel.startStroke(newStroke)
                            }
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
                                        viewModel.moveSelectedStrokes(delta)
                                    } else if (state.currentTool == WhiteboardTool.LASER) {
                                        viewModel.addLaserPoint(curWorld)
                                    } else if (state.currentTool == WhiteboardTool.LASSO) {
                                        lassoPoints = lassoPoints + curWorld
                                    } else if (state.currentTool == WhiteboardTool.ERASER) {
                                        val eraseRadius = 24f / canvasScale.coerceAtLeast(0.001f)
                                        viewModel.eraseAt(curWorld, eraseRadius)
                                    } else if (state.currentTool == WhiteboardTool.PEN) {
                                        points = points + curWorld
                                        viewModel.updateStroke(points)
                                    } else if (state.currentTool in listOf(
                                            WhiteboardTool.RECTANGLE,
                                            WhiteboardTool.DIAMOND,
                                            WhiteboardTool.CIRCLE,
                                            WhiteboardTool.ARROW,
                                            WhiteboardTool.LINE,
                                            WhiteboardTool.FRAME
                                        )
                                    ) {
                                        points = listOf(downWorld, curWorld)
                                        viewModel.updateStroke(points)
                                    }
                                    prevPosition = change.position
                                    change.consume()
                                }
                            }
                        } while (event.changes.any { it.pressed })

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
                            } else if (state.currentTool == WhiteboardTool.PEN && points.size >= 1) {
                                if (state.isDrawToShapeEnabled && points.size >= 5) {
                                    val shape = viewModel.applyShapeRecognition(points)
                                    if (shape != null) {
                                        viewModel.startStroke(shape)
                                    } else {
                                        viewModel.updateStroke(points)
                                    }
                                } else {
                                    viewModel.updateStroke(points)
                                }
                                viewModel.finishStroke()
                            } else if (state.currentTool in listOf(
                                    WhiteboardTool.RECTANGLE,
                                    WhiteboardTool.DIAMOND,
                                    WhiteboardTool.CIRCLE,
                                    WhiteboardTool.ARROW,
                                    WhiteboardTool.LINE,
                                    WhiteboardTool.FRAME
                                ) && points.size >= 2
                            ) {
                                viewModel.updateStroke(points)
                                viewModel.finishStroke()
                                if (!state.isToolLocked) {
                                    viewModel.setTool(WhiteboardTool.SELECT)
                                }
                            }
                        }
                    }
                }
        ) {
            val themeBgColor = LocalVibeColors.current.background
            val bgColor = when (state.canvasBackground) {
                CanvasBackground.WHITE -> Color.White
                else -> themeBgColor
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
                    val gridColor = if (state.canvasBackground == CanvasBackground.WHITE) Color(0xFFE0E0E0) else Color(0xFF2E2E2E)
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
                            val dotRadius = 2f / canvasScale.coerceAtLeast(0.5f)
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

                    // Render visible strokes
                    val visibleStrokes = state.strokes.filter { stroke ->
                        stroke.bounds.overlaps(viewportBounds)
                    }

                    visibleStrokes.forEach { stroke ->
                        val isSelected = stroke.id in state.selectedStrokeIds
                        renderStroke(context, stroke, canvasScale, isSelected)
                    }

                    state.currentStroke?.let { activeStroke ->
                        if (activeStroke.points.isNotEmpty()) {
                            renderStroke(context, activeStroke, canvasScale, false)
                        }
                    }

                    // Render Laser Pointer Trail
                    if (state.laserPoints.size >= 2) {
                        val laserPath = state.laserPoints.toSmoothedPath()
                        drawPath(
                            path = laserPath,
                            color = Color(0xFFFF2A6D),
                            style = DrawStroke(
                                width = 5f / canvasScale.coerceAtLeast(0.5f),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                        drawCircle(
                            color = Color(0xFFFF2A6D),
                            radius = 6f / canvasScale.coerceAtLeast(0.5f),
                            center = state.laserPoints.last()
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

        // 2. Floating Top Header: Left Hamburger Menu & Right Live Share
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Floating Left: Hamburger (☰) Button + Note Title
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, Color(0xFFE5E7EB))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(
                        onClick = { showSidebar = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = Color(0xFF374151),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = state.noteTitle.ifEmpty { "Untitled" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937),
                        modifier = Modifier
                            .clickable { showEditTitleDialog = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Floating Right: Live Status Badge + Purple Share Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.isLiveCollabActive) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFECFDF5),
                        border = BorderStroke(1.dp, Color(0xFF10B981)),
                        modifier = Modifier.clickable { showLiveCollabDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Live (${state.connectedCollaborators.size})",
                                color = Color(0xFF065F46),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Purple Share Button (Excalidraw Live Collab Pill)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF6366F1),
                    shadowElevation = 4.dp,
                    modifier = Modifier.clickable { showLiveCollabDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Group,
                            contentDescription = "Share",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Share",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. Floating Whiteboard Dock (Centered at Top)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 58.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // 1. Lock Tool
                    WhiteboardDockButton(
                        icon = if (state.isToolLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        shortcut = "",
                        isActive = state.isToolLocked,
                        onClick = { viewModel.setTool(WhiteboardTool.LOCK) }
                    )

                    // Divider
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .width(1.dp)
                            .height(22.dp)
                            .background(Color(0xFFE5E7EB))
                    )

                    // 2. Hand Tool (H)
                    WhiteboardDockButton(
                        icon = Icons.Default.PanTool,
                        shortcut = "H",
                        isActive = state.currentTool == WhiteboardTool.HAND,
                        onClick = { viewModel.setTool(WhiteboardTool.HAND) }
                    )

                    // 3. Selection Tool (V)
                    WhiteboardDockButton(
                        icon = Icons.Default.NearMe,
                        shortcut = "V",
                        isActive = state.currentTool == WhiteboardTool.SELECT,
                        onClick = { viewModel.setTool(WhiteboardTool.SELECT) }
                    )

                    // 4. Rectangle Tool (R)
                    WhiteboardDockButton(
                        icon = Icons.Default.CropSquare,
                        shortcut = "R",
                        isActive = state.currentTool == WhiteboardTool.RECTANGLE,
                        onClick = { viewModel.setTool(WhiteboardTool.RECTANGLE) }
                    )

                    // 5. Diamond Tool (D)
                    WhiteboardDockButton(
                        icon = Icons.Default.Diamond,
                        shortcut = "D",
                        isActive = state.currentTool == WhiteboardTool.DIAMOND,
                        onClick = { viewModel.setTool(WhiteboardTool.DIAMOND) }
                    )

                    // 6. Circle Tool (O)
                    WhiteboardDockButton(
                        icon = Icons.Default.RadioButtonUnchecked,
                        shortcut = "O",
                        isActive = state.currentTool == WhiteboardTool.CIRCLE,
                        onClick = { viewModel.setTool(WhiteboardTool.CIRCLE) }
                    )

                    // 7. Arrow Tool (A)
                    WhiteboardDockButton(
                        icon = Icons.Default.ArrowForward,
                        shortcut = "A",
                        isActive = state.currentTool == WhiteboardTool.ARROW,
                        onClick = { viewModel.setTool(WhiteboardTool.ARROW) }
                    )

                    // 8. Line Tool (L)
                    WhiteboardDockButton(
                        icon = Icons.Default.HorizontalRule,
                        shortcut = "L",
                        isActive = state.currentTool == WhiteboardTool.LINE,
                        onClick = { viewModel.setTool(WhiteboardTool.LINE) }
                    )

                    // 9. Draw / Pen Tool (P)
                    WhiteboardDockButton(
                        icon = Icons.Default.Edit,
                        shortcut = "P",
                        isActive = state.currentTool == WhiteboardTool.PEN,
                        onClick = { viewModel.setTool(WhiteboardTool.PEN) }
                    )

                    // 10. Text Tool (T)
                    WhiteboardDockButton(
                        icon = Icons.Default.TextFields,
                        shortcut = "T",
                        isActive = state.currentTool == WhiteboardTool.TEXT,
                        onClick = { viewModel.setTool(WhiteboardTool.TEXT) }
                    )

                    // 11. Sticky Note Tool (N)
                    WhiteboardDockButton(
                        icon = Icons.Default.Description,
                        shortcut = "N",
                        isActive = state.currentTool == WhiteboardTool.STICKY_NOTE,
                        onClick = { viewModel.setTool(WhiteboardTool.STICKY_NOTE) }
                    )

                    // 12. Eraser Tool (E)
                    WhiteboardDockButton(
                        icon = Icons.Default.AutoFixNormal,
                        shortcut = "E",
                        isActive = state.currentTool == WhiteboardTool.ERASER,
                        onClick = { viewModel.setTool(WhiteboardTool.ERASER) }
                    )

                    // Divider
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .width(1.dp)
                            .height(22.dp)
                            .background(Color(0xFFE5E7EB))
                    )

                    // 13. More (Overflow ⋮) Button
                    Box {
                        IconButton(
                            onClick = { showOverflowPane = !showOverflowPane },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "More tools",
                                tint = if (showOverflowPane) Color(0xFF4338CA) else Color(0xFF4B5563),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (showOverflowPane) {
                            Popup(
                                alignment = Alignment.TopEnd,
                                offset = IntOffset(0, 120),
                                onDismissRequest = { showOverflowPane = false },
                                properties = PopupProperties(focusable = true)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.White,
                                    shadowElevation = 12.dp,
                                    border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                                    modifier = Modifier
                                        .width(220.dp)
                                        .padding(4.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        OverflowMenuItem(
                                            icon = Icons.Default.Image,
                                            label = "Insert image",
                                            shortcut = "9",
                                            onClick = {
                                                showOverflowPane = false
                                                imagePickerLauncher.launch("image/*")
                                            }
                                        )
                                        OverflowMenuItem(
                                            icon = Icons.Default.CropFree,
                                            label = "Frame tool",
                                            shortcut = "F",
                                            isSelected = state.currentTool == WhiteboardTool.FRAME,
                                            onClick = {
                                                showOverflowPane = false
                                                viewModel.setTool(WhiteboardTool.FRAME)
                                            }
                                        )
                                        OverflowMenuItem(
                                            icon = Icons.Default.Code,
                                            label = "Web Embed",
                                            shortcut = "",
                                            onClick = {
                                                showOverflowPane = false
                                                textInputValue = ""
                                                val centerWorld = CanvasTransform.screenToWorld(
                                                    screenPoint = Offset(canvasSize.width / 2f, canvasSize.height / 2f),
                                                    scale = canvasScale,
                                                    offset = Offset(canvasOffsetX, canvasOffsetY)
                                                )
                                                pendingWorldPoint = centerWorld
                                                showWebEmbedDialog = true
                                            }
                                        )
                                        OverflowMenuItem(
                                            icon = Icons.Default.AutoFixHigh,
                                            label = "Draw to shape",
                                            shortcut = "Shift+X",
                                            isSelected = state.isDrawToShapeEnabled,
                                            onClick = {
                                                showOverflowPane = false
                                                viewModel.toggleDrawToShape()
                                            }
                                        )
                                        OverflowMenuItem(
                                            icon = Icons.Default.Highlight,
                                            label = "Laser pointer",
                                            shortcut = "K",
                                            isSelected = state.currentTool == WhiteboardTool.LASER,
                                            onClick = {
                                                showOverflowPane = false
                                                viewModel.setTool(WhiteboardTool.LASER)
                                            }
                                        )
                                        OverflowMenuItem(
                                            icon = Icons.Default.FormatColorFill,
                                            label = "Bucket fill",
                                            shortcut = "B",
                                            isSelected = state.currentTool == WhiteboardTool.BUCKET_FILL,
                                            onClick = {
                                                showOverflowPane = false
                                                viewModel.setTool(WhiteboardTool.BUCKET_FILL)
                                            }
                                        )
                                        OverflowMenuItem(
                                            icon = Icons.Default.Gesture,
                                            label = "Lasso selection",
                                            shortcut = "",
                                            isSelected = state.currentTool == WhiteboardTool.LASSO,
                                            onClick = {
                                                showOverflowPane = false
                                                viewModel.setTool(WhiteboardTool.LASSO)
                                            }
                                        )
                                        Divider(
                                            color = Color(0xFFF3F4F6),
                                            thickness = 1.dp,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                        OverflowMenuItem(
                                            icon = Icons.Default.GridOn,
                                            label = "Canvas Background",
                                            onClick = {
                                                showOverflowPane = false
                                                showBackgroundPicker = true
                                            }
                                        )
                                        OverflowMenuItem(
                                            icon = Icons.Default.FormatPaint,
                                            label = "Stroke Color & Width",
                                            onClick = {
                                                showOverflowPane = false
                                                showColorPicker = true
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Selection Quick Actions Bar
            if (state.selectedStrokeIds.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1F2937),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "${state.selectedStrokeIds.size} selected",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        IconButton(
                            onClick = { viewModel.deleteSelectedStrokes() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete selection",
                                tint = Color(0xFFFF6B6B),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Property Bar (Color & Stroke Width)
            AnimatedVisibility(
                visible = showColorPicker,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = LocalVibeColors.current.surface,
                    shadowElevation = 6.dp,
                    border = BorderStroke(1.dp, LocalVibeColors.current.borderStandard),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PEN_COLORS.forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(color))
                                        .border(
                                            if (state.selectedColor == color) 2.5.dp else 1.dp,
                                            if (state.selectedColor == color) LocalVibeColors.current.brand else LocalVibeColors.current.borderStandard,
                                            CircleShape
                                        )
                                        .clickable { viewModel.setColor(color) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.width(260.dp)
                        ) {
                            Text("Width", fontSize = 12.sp, color = LocalVibeColors.current.textMuted)
                            Spacer(modifier = Modifier.width(8.dp))
                            Slider(
                                value = state.strokeWidth,
                                onValueChange = { viewModel.setStrokeWidth(it) },
                                valueRange = 2f..24f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = LocalVibeColors.current.brand,
                                    activeTrackColor = LocalVibeColors.current.brand
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "${state.strokeWidth.toInt()}px",
                                fontSize = 12.sp,
                                color = LocalVibeColors.current.textPrimary
                            )
                        }
                    }
                }
            }

            // Background Picker Popup
            AnimatedVisibility(
                visible = showBackgroundPicker,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = LocalVibeColors.current.surface,
                    shadowElevation = 6.dp,
                    border = BorderStroke(1.dp, LocalVibeColors.current.borderStandard),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            CanvasBackground.DARK,
                            CanvasBackground.WHITE,
                            CanvasBackground.LINED,
                            CanvasBackground.DOTTED,
                            CanvasBackground.GRID
                        ).forEach { type ->
                            BackgroundSwatch(
                                type = type,
                                isSelected = state.canvasBackground == type,
                                onClick = {
                                    viewModel.setCanvasBackground(type)
                                    showBackgroundPicker = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // 4. Floating Navigation HUD (Bottom Right)
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(LocalVibeColors.current.surface.copy(alpha = 0.94f))
                .border(1.dp, LocalVibeColors.current.borderStandard, RoundedCornerShape(20.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = {
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
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Remove,
                    contentDescription = "Zoom out",
                    tint = LocalVibeColors.current.textMuted,
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = CanvasTransform.formatZoom(canvasScale),
                color = LocalVibeColors.current.textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable {
                        canvasScale = 1f
                        canvasOffsetX = 0f
                        canvasOffsetY = 0f
                        viewModel.resetTransform()
                    }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )

            IconButton(
                onClick = {
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
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Zoom in",
                    tint = LocalVibeColors.current.textMuted,
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(
                onClick = {
                    viewModel.fitToContent(canvasSize.width, canvasSize.height)
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.CenterFocusStrong,
                    contentDescription = "Fit to content",
                    tint = LocalVibeColors.current.brand,
                    modifier = Modifier.size(16.dp)
                )
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
                            Text(
                                "Bloom",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937)
                            )
                            IconButton(
                                onClick = { showSidebar = false },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF6B7280))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

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
                color = LocalVibeColors.current.brand
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
                        val roomUrl = "https://bloom.app/#room=${state.collabRoomId}"
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
                                    val clip = ClipData.newPlainText("Bloom Live Session", roomUrl)
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
                    Text("• V: Selection tool\n• H: Hand (Pan tool)\n• R: Rectangle\n• D: Diamond\n• O: Circle\n• A: Arrow\n• L: Line\n• P: Freehand Pen\n• T: Text placement\n• N: Sticky Note\n• E: Eraser\n• K: Laser Pointer\n• B: Bucket Fill\n• 9: Insert Image", fontSize = 13.sp, color = Color(0xFF4B5563), lineHeight = 20.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Pinch with 2 fingers to zoom (0.001x to 100x). Double tap to fit content.", fontSize = 12.sp, color = Color(0xFF6B7280))
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
            title = { Text("Web Embed", color = LocalVibeColors.current.textPrimary) },
            text = {
                OutlinedTextField(
                    value = textInputValue,
                    onValueChange = { textInputValue = it },
                    placeholder = { Text("https://example.com") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = LocalVibeColors.current.textPrimary,
                        unfocusedTextColor = LocalVibeColors.current.textPrimary,
                        focusedBorderColor = LocalVibeColors.current.brand,
                        unfocusedBorderColor = LocalVibeColors.current.borderStandard
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addWebEmbed(textInputValue.ifBlank { "https://bloom.app" }, pendingWorldPoint)
                    showWebEmbedDialog = false
                }) {
                    Text("Embed", color = LocalVibeColors.current.brand)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWebEmbedDialog = false }) {
                    Text("Cancel", color = LocalVibeColors.current.textMuted)
                }
            },
            containerColor = LocalVibeColors.current.surface
        )
    }

    // Text Input Modal Dialog
    if (showTextInputDialog) {
        AlertDialog(
            onDismissRequest = { showTextInputDialog = false },
            title = { Text("Add Text", color = LocalVibeColors.current.textPrimary) },
            text = {
                OutlinedTextField(
                    value = textInputValue,
                    onValueChange = { textInputValue = it },
                    placeholder = { Text("Type something...") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = LocalVibeColors.current.textPrimary,
                        unfocusedTextColor = LocalVibeColors.current.textPrimary,
                        focusedBorderColor = LocalVibeColors.current.brand,
                        unfocusedBorderColor = LocalVibeColors.current.borderStandard
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
                    Text("Add", color = LocalVibeColors.current.brand)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTextInputDialog = false }) {
                    Text("Cancel", color = LocalVibeColors.current.textMuted)
                }
            },
            containerColor = LocalVibeColors.current.surface
        )
    }

    // Sticky Note Input Modal Dialog
    if (showStickyInputDialog) {
        AlertDialog(
            onDismissRequest = { showStickyInputDialog = false },
            title = { Text("Add Sticky Note", color = LocalVibeColors.current.textPrimary) },
            text = {
                Column {
                    OutlinedTextField(
                        value = textInputValue,
                        onValueChange = { textInputValue = it },
                        placeholder = { Text("Write a note...") },
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = LocalVibeColors.current.textPrimary,
                            unfocusedTextColor = LocalVibeColors.current.textPrimary,
                            focusedBorderColor = LocalVibeColors.current.brand,
                            unfocusedBorderColor = LocalVibeColors.current.borderStandard
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Color", fontSize = 12.sp, color = LocalVibeColors.current.textMuted)
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
                                        if (stickyColorSelected == color) LocalVibeColors.current.brand else Color(0x33000000),
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
                    Text("Add Note", color = LocalVibeColors.current.brand)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStickyInputDialog = false }) {
                    Text("Cancel", color = LocalVibeColors.current.textMuted)
                }
            },
            containerColor = LocalVibeColors.current.surface
        )
    }

    // Edit Title Dialog
    if (showEditTitleDialog) {
        var editedTitle by remember { mutableStateOf(state.noteTitle) }
        AlertDialog(
            onDismissRequest = { showEditTitleDialog = false },
            title = { Text("Edit Title", color = LocalVibeColors.current.textPrimary) },
            text = {
                OutlinedTextField(
                    value = editedTitle,
                    onValueChange = { editedTitle = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = LocalVibeColors.current.textPrimary,
                        unfocusedTextColor = LocalVibeColors.current.textPrimary,
                        focusedBorderColor = LocalVibeColors.current.brand,
                        unfocusedBorderColor = LocalVibeColors.current.borderStandard
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateTitle(editedTitle.ifBlank { "Untitled" })
                    showEditTitleDialog = false
                }) {
                    Text("Save", color = LocalVibeColors.current.brand)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditTitleDialog = false }) {
                    Text("Cancel", color = LocalVibeColors.current.textMuted)
                }
            },
            containerColor = LocalVibeColors.current.surface
        )
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Note", color = LocalVibeColors.current.textPrimary) },
            text = { Text("Are you sure you want to delete this note?", color = LocalVibeColors.current.textMuted) },
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
                    Text("Cancel", color = LocalVibeColors.current.textMuted)
                }
            },
            containerColor = LocalVibeColors.current.surface
        )
    }

    // Clear Canvas confirmation dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Canvas", color = LocalVibeColors.current.textPrimary) },
            text = { Text("Remove all strokes? This can be undone.", color = LocalVibeColors.current.textMuted) },
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
                    Text("Cancel", color = LocalVibeColors.current.textMuted)
                }
            },
            containerColor = LocalVibeColors.current.surface
        )
    }

    // Export success dialog
    if (showExportDialog && exportedUri != null) {
        AlertDialog(
            onDismissRequest = {
                showExportDialog = false
                exportedUri = null
            },
            title = { Text("Export Successful", color = LocalVibeColors.current.textPrimary) },
            text = { Text("Note exported to Pictures/Bloom", color = LocalVibeColors.current.textMuted) },
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
                    Text("Share", color = LocalVibeColors.current.brand)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showExportDialog = false
                    exportedUri = null
                }) {
                    Text("Close", color = LocalVibeColors.current.textMuted)
                }
            },
            containerColor = LocalVibeColors.current.surface
        )
    }
}