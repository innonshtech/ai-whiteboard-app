package com.avitrisphare.whiteboard.presentation.canvas

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.VideoView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.viewinterop.AndroidView
import com.avitrisphare.whiteboard.domain.model.CanvasBackground
import com.avitrisphare.whiteboard.domain.model.Stroke
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.abs

interface SplitHost {
    fun focus(index: Int)
    fun beginEdit(index: Int)
    fun cancelEdit(index: Int)
    fun setStrokes(index: Int, strokes: List<Stroke>)
    fun resize(weights: List<Float>)
    fun setKind(index: Int, kind: PaneKind)
    fun pickPdf(index: Int)
    fun pickVideo(index: Int)
    fun pickWeb(index: Int)
    fun stepPage(index: Int, delta: Int)
    fun swipeBoard(delta: Int)
    fun toggleAi()
}

/**
 * One view group for every panel. A pen sample invalidates only that panel's
 * live ink, so the other panels stay still.
 */
class SplitBoardView(context: Context) : FrameLayout(context) {
    var host: SplitHost? = null

    private val cells = ArrayList<PaneCell>()
    private val pdfCache = HashMap<String, Bitmap>()
    private val renderer = Executors.newSingleThreadExecutor()
    private var panes: List<CanvasPane> = emptyList()
    private var weights: List<Float> = emptyList()
    private var activeIndex = 0
    private var background = CanvasBackground.DOTTED
    private var dark = false
    private var showGrid = false
    private var inkColor = 0xFF1B1F2A.toInt()
    private var inkWidth = 4f
    private var eraserMode = EraserMode.STANDARD
    private var eraserSize = 28f
    private var gesturePane = -1
    private var drawingPane = -1
    private var panning = false
    private var erasing = false
    private var selecting = false
    private var editOpen = false
    private val points = ArrayList<Offset>(256)
    private val liveInk = LiveInk()
    private var eraseStrokes: List<Stroke> = emptyList()
    private var lastX = 0f
    private var lastY = 0f
    private var divider: DividerHit? = null
    private var dividerStart = 0f
    private var dividerWeights: List<Float> = emptyList()
    private var laidKey = ""

    init {
        setBackgroundColor(android.graphics.Color.WHITE)
        isMotionEventSplittingEnabled = false
    }

    fun bind(state: CanvasState) {
        background = state.canvasBackground
        dark = state.isDarkCanvas
        showGrid = state.showGrid
        inkColor = state.selectedColor
        inkWidth = state.strokeWidth
        eraserMode = state.eraserMode
        eraserSize = state.eraserSize
        val count = state.splitCount.coerceIn(2, 6)
        while (cells.size < count) cells.add(PaneCell(context, cells.size))
        for (index in cells.indices) {
            cells[index].visibility = if (index < count) VISIBLE else GONE
        }
        if (gesturePane < 0) panes = state.splitPanes
        weights = if (state.splitWeights.isEmpty()) defaultWeights(count) else state.splitWeights
        activeIndex = state.splitActive.coerceIn(0, count - 1)
        val key = "$count:${width}x$height:${weights.joinToString()}"
        if (!dragging && key != laidKey && width > 0 && height > 0) {
            laidKey = key
            layoutPanes(weights)
        }
        if (gesturePane < 0) syncCells()
        invalidate()
    }

    private var dragging = false

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (panes.isNotEmpty() && !dragging && w > 0 && h > 0) {
            layoutPanes(weights)
            laidKey = "${panes.size}:${w}x$h:${weights.joinToString()}"
        }
    }

    private fun layoutPanes(nextWeights: List<Float>) {
        val frames = paneFrames(panes.size, width, height, nextWeights)
        frames.forEachIndexed { index, frame ->
            if (index >= cells.size) return@forEachIndexed
            val cell = cells[index]
            if (cell.parent == null) addView(cell)
            val params = LayoutParams(frame.width.coerceAtLeast(1), frame.height.coerceAtLeast(1))
            params.leftMargin = frame.left
            params.topMargin = frame.top
            cell.layoutParams = params
        }
        requestLayout()
    }

    private fun syncCells() {
        panes.forEachIndexed { index, pane ->
            if (index >= cells.size) return@forEachIndexed
            val cell = cells[index]
            val drawing = pane.kind == PaneKind.BOARD || pane.kind == PaneKind.PDF
            cell.stage.visibility = if (drawing) VISIBLE else INVISIBLE
            if (cell.shownStrokes !== pane.strokes) {
                cell.shownStrokes = pane.strokes
                cell.stage.syncPage(
                    strokes = pane.strokes,
                    background = background,
                    showGrid = showGrid,
                    dark = dark,
                    scale = cell.stage.viewScale(),
                    offsetX = cell.stage.viewOffsetX(),
                    offsetY = cell.stage.viewOffsetY(),
                    selected = emptySet()
                )
            }
            if (pane.kind == PaneKind.PDF) showDocument(index, pane) else cell.stage.setDocumentPage(null, 0f, 0f)
            showVideo(cell, pane)
            showWeb(cell, pane)
            cell.bindBar(pane, index == activeIndex)
        }
    }

    private fun showDocument(index: Int, pane: CanvasPane) {
        if (pane.source.isBlank()) return
        val key = "${pane.source}#${pane.pageIndex}"
        val cached = pdfCache[key]
        if (cached != null && !cached.isRecycled) {
            cells[index].stage.setDocumentPage(cached, cached.width.toFloat(), cached.height.toFloat())
            return
        }
        if (cells[index].loadingKey == key) return
        cells[index].loadingKey = key
        val file = File(pane.source)
        renderer.execute {
            try {
                val kind = documentKind(file.name, null) ?: DocumentKind.PDF
                val bitmap = DocumentRenderer.render(file, kind, pane.pageIndex)
                post {
                    if (panes.getOrNull(index)?.source != pane.source) {
                        bitmap.recycle()
                        return@post
                    }
                    pdfCache[key] = bitmap
                    cells[index].stage.setDocumentPage(bitmap, bitmap.width.toFloat(), bitmap.height.toFloat())
                }
            } catch (_: Exception) {
                post { cells[index].loadingKey = "" }
            }
        }
    }

    private fun showVideo(cell: PaneCell, pane: CanvasPane) {
        val video = cell.video
        if (pane.kind != PaneKind.VIDEO || pane.source.isBlank()) {
            if (video != null) {
                try {
                    video.stopPlayback()
                } catch (_: Exception) {
                }
                video.visibility = GONE
            }
            return
        }
        val view = video ?: VideoView(context).also {
            cell.video = it
            cell.addView(it, 1, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        }
        view.visibility = VISIBLE
        if (cell.videoSource != pane.source) {
            cell.videoSource = pane.source
            view.setVideoURI(Uri.parse(pane.source))
            view.setOnPreparedListener { it.start() }
        }
    }

    private fun showWeb(cell: PaneCell, pane: CanvasPane) {
        val web = cell.web
        if (pane.kind != PaneKind.WEB || pane.source.isBlank()) {
            web?.visibility = GONE
            return
        }
        val view = web ?: WebView(context).also {
            it.settings.javaScriptEnabled = true
            it.settings.domStorageEnabled = true
            cell.web = it
            cell.addView(it, 1, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        }
        view.visibility = VISIBLE
        if (cell.webSource != pane.source) {
            cell.webSource = pane.source
            view.loadUrl(pane.source)
        }
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked != MotionEvent.ACTION_DOWN || panes.isEmpty()) return false
        val hit = hitDivider(paneFrames(panes.size, width, height, weights), panes.size, event.x, event.y)
        if (hit != null) {
            divider = hit
            dragging = true
            dividerStart = if (hit.vertical) event.x else event.y
            dividerWeights = weights
            cells.forEach { it.stage.setResizing(true) }
            return true
        }
        return false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (panes.isEmpty()) return false
        if (divider != null) return dragDivider(event)
        val frames = paneFrames(panes.size, width, height, weights)
        val stylusIndex = stylusPointer(event)
        val fingers = acceptedFingerCount(event, stylusIndex >= 0)
        if (!directStroke() && stylusIndex < 0 && (fingers >= 2 || multi)) {
            return fingerGesture(event, frames, fingers)
        }
        return drawGesture(event, frames, stylusIndex)
    }

    private fun dragDivider(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                val dragged = divider ?: return false
                val delta = (if (dragged.vertical) event.x else event.y) - dividerStart
                val span = if (dragged.vertical) width.toFloat() else height.toFloat()
                weights = dragDivider(dividerWeights, panes.size, dragged.vertical, dragged.index, delta, span)
                layoutPanes(weights)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                cells.forEach { it.stage.setResizing(false) }
                host?.resize(weights)
                divider = null
                dragging = false
                laidKey = ""
            }
        }
        return true
    }

    private fun fingerGesture(event: MotionEvent, frames: List<PaneFrame>, fingers: Int): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_MOVE -> {
                val center = fingerCenter(event, stylusActive = false)
                if (!multi) {
                    abandonStroke()
                    multi = true
                    fingerPeak = fingers
                    gestureStartX = center.first
                    gestureStartY = center.second
                    pinchSpan = fingerSpan(event)
                    val index = hitPane(frames, center.first, center.second)
                    if (index >= 0) {
                        drawingPane = index
                        if (index != activeIndex) host?.focus(index)
                        activeIndex = index
                    }
                }
                if (fingers > fingerPeak) fingerPeak = fingers
                gestureEndX = center.first
                gestureEndY = center.second
                val travel = centroidTravel(gestureStartX, gestureStartY, center.first, center.second)
                if (travel > gestureTravel) gestureTravel = travel
                if (fingers == 2 && fingerPeak == 2) {
                    val span = fingerSpan(event)
                    val index = drawingPane
                    if (pinchSpan > 12f && span > 12f && index in cells.indices) {
                        zoomPane(index, center.first - frames[index].left, center.second - frames[index].top, span / pinchSpan)
                    }
                    pinchSpan = span
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> finishFingerGesture()
        }
        return true
    }

    private fun finishFingerGesture() {
        val index = drawingPane
        if (index in cells.indices) cells[index].stage.commitPreview()
        val gesture = classifyPanelGesture(
            fingerPeak,
            gestureTravel,
            gestureEndX - gestureStartX,
            gestureEndY - gestureStartY
        )
        when (gesture) {
            PanelGesture.PAGE -> {
                val delta = swipePageDelta(gestureEndX - gestureStartX, gestureEndY - gestureStartY, threshold = 90f)
                if (delta != 0) host?.swipeBoard(delta)
            }
            PanelGesture.AI_TAP -> host?.toggleAi()
            else -> Unit
        }
        multi = false
        fingerPeak = 0
        gestureTravel = 0f
        pinchSpan = 0f
        drawingPane = -1
    }

    private fun zoomPane(index: Int, focusX: Float, focusY: Float, factor: Float) {
        val stage = cells[index].stage
        val newScale = (stage.viewScale() * factor).coerceIn(CanvasTransform.MIN_SCALE, CanvasTransform.MAX_SCALE)
        val world = stage.toWorld(focusX, focusY)
        stage.previewTransform(newScale, focusX - world.x * newScale, focusY - world.y * newScale)
    }

    private fun drawGesture(event: MotionEvent, frames: List<PaneFrame>, stylusIndex: Int): Boolean {
        val tracked = if (drawPointerId >= 0) event.findPointerIndex(drawPointerId) else -1
        val pointer = when {
            tracked >= 0 -> tracked
            stylusIndex >= 0 -> stylusIndex
            else -> 0
        }
        if (pointer >= event.pointerCount) return true
        val x = event.getX(pointer)
        val y = event.getY(pointer)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                if (stylusIndex >= 0 && !stylusInk && (points.isNotEmpty() || panning || erasing || selecting)) {
                    abandonStroke()
                }
                if (drawingPane >= 0 && event.actionMasked == MotionEvent.ACTION_POINTER_DOWN) return true
                val index = hitPane(frames, x, y)
                if (index < 0) return false
                val pane = panes[index]
                if (pane.kind == PaneKind.VIDEO || pane.kind == PaneKind.WEB) return false
                if (index != activeIndex) host?.focus(index)
                activeIndex = index
                drawingPane = index
                drawPointerId = event.getPointerId(pointer)
                lastX = x
                lastY = y
                val localX = x - frames[index].left
                val localY = y - frames[index].top
                val toolType = event.getToolType(pointer)
                stylusInk = toolType == MotionEvent.TOOL_TYPE_STYLUS
                drawTool = when {
                    toolType == MotionEvent.TOOL_TYPE_ERASER -> WhiteboardTool.ERASER
                    stylusInk && pane.tool in WRITING_TOOLS -> pane.tool
                    stylusInk -> WhiteboardTool.PEN
                    else -> pane.tool
                }
                when {
                    drawTool == WhiteboardTool.HAND -> panning = true
                    drawTool == WhiteboardTool.ERASER -> startErase(index, localX, localY)
                    drawTool == WhiteboardTool.SELECT -> startSelect(index, localX, localY)
                    drawTool in WRITING_TOOLS -> startInk(index, localX, localY, event.getPressure(pointer))
                    else -> return false
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (drawPointerId >= 0 && tracked < 0) return true
                val index = drawingPane
                if (index < 0 || index >= frames.size) return false
                val localX = x - frames[index].left
                val localY = y - frames[index].top
                when {
                    panning -> {
                        cells[index].stage.panBy(x - lastX, y - lastY)
                        lastX = x
                        lastY = y
                    }
                    erasing -> eraseAt(index, localX, localY)
                    selecting -> moveSelection(index, localX, localY)
                    points.isNotEmpty() -> {
                        if (stylusInk) {
                            val history = event.historySize
                            for (step in 0 until history) {
                                appendInk(
                                    index,
                                    event.getHistoricalX(pointer, step) - frames[index].left,
                                    event.getHistoricalY(pointer, step) - frames[index].top,
                                    event.getHistoricalPressure(pointer, step)
                                )
                            }
                        }
                        appendInk(index, localX, localY, event.getPressure(pointer))
                    }
                }
                return true
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val lifted = event.actionIndex
                if (directStroke()) {
                    if (event.getPointerId(lifted) == drawPointerId) finishGesture()
                    return true
                }
                val liftedTool = event.getToolType(lifted)
                val liftedStylus = liftedTool == MotionEvent.TOOL_TYPE_STYLUS ||
                    liftedTool == MotionEvent.TOOL_TYPE_ERASER
                if (stylusInk && liftedStylus) finishGesture()
                else if (!stylusInk && event.pointerCount <= 2) finishGesture()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                finishGesture()
                return true
            }
        }
        return false
    }

    private fun stylusPointer(event: MotionEvent): Int {
        val limit = event.pointerCount.coerceAtMost(MAX_TOUCH_POINTS)
        for (index in 0 until limit) {
            val tool = event.getToolType(index)
            if (tool == MotionEvent.TOOL_TYPE_STYLUS || tool == MotionEvent.TOOL_TYPE_ERASER) return index
        }
        return -1
    }

    private fun acceptedFingerCount(event: MotionEvent, stylusActive: Boolean): Int {
        val metrics = resources.displayMetrics
        val limit = event.pointerCount.coerceAtMost(MAX_TOUCH_POINTS)
        var count = 0
        for (index in 0 until limit) {
            val tool = event.getToolType(index)
            if (tool == MotionEvent.TOOL_TYPE_STYLUS || tool == MotionEvent.TOOL_TYPE_ERASER) continue
            if (isPalmContact(
                    event.getTouchMajor(index),
                    metrics.xdpi,
                    stylusActive,
                    isStylus = false,
                    touchMinorPx = event.getTouchMinor(index),
                    ydpi = metrics.ydpi
                )
            ) {
                continue
            }
            count++
        }
        return count
    }

    private fun fingerCenter(event: MotionEvent, stylusActive: Boolean): Pair<Float, Float> {
        val metrics = resources.displayMetrics
        val limit = event.pointerCount.coerceAtMost(MAX_TOUCH_POINTS)
        var sumX = 0f
        var sumY = 0f
        var count = 0
        for (index in 0 until limit) {
            val tool = event.getToolType(index)
            if (tool == MotionEvent.TOOL_TYPE_STYLUS || tool == MotionEvent.TOOL_TYPE_ERASER) continue
            if (isPalmContact(
                    event.getTouchMajor(index),
                    metrics.xdpi,
                    stylusActive,
                    false,
                    event.getTouchMinor(index),
                    metrics.ydpi
                )
            ) {
                continue
            }
            sumX += event.getX(index)
            sumY += event.getY(index)
            count++
        }
        if (count == 0) return event.x to event.y
        return sumX / count to sumY / count
    }

    private fun fingerSpan(event: MotionEvent): Float {
        val limit = event.pointerCount.coerceAtMost(MAX_TOUCH_POINTS)
        var first = -1
        for (index in 0 until limit) {
            val tool = event.getToolType(index)
            if (tool == MotionEvent.TOOL_TYPE_STYLUS || tool == MotionEvent.TOOL_TYPE_ERASER) continue
            if (first < 0) {
                first = index
            } else {
                return centroidTravel(event.getX(first), event.getY(first), event.getX(index), event.getY(index))
            }
        }
        return 0f
    }

    private fun abandonStroke() {
        val index = drawingPane
        if (index in cells.indices) {
            cells[index].stage.setLiveInk(null)
            cells[index].stage.clearGestureStrokes()
            if (editOpen) host?.cancelEdit(index)
        }
        points.clear()
        pressures.clear()
        drawingPane = -1
        gesturePane = -1
        panning = false
        erasing = false
        selecting = false
        editOpen = false
        stylusInk = false
        drawPointerId = -1
        selectIndex = -1
    }

    private fun directStroke(): Boolean {
        if (drawingPane >= 0 &&
            (drawTool == WhiteboardTool.PENCIL || drawTool == WhiteboardTool.ERASER)
        ) {
            return true
        }
        val index = if (drawingPane >= 0) drawingPane else activeIndex
        val tool = panes.getOrNull(index)?.tool ?: return false
        return tool == WhiteboardTool.PENCIL || tool == WhiteboardTool.ERASER
    }

    private fun startInk(index: Int, x: Float, y: Float, pressure: Float) {
        gesturePane = index
        editOpen = true
        host?.beginEdit(index)
        points.clear()
        pressures.clear()
        points.add(cells[index].stage.toWorld(x, y))
        pressures.add(if (stylusInk) stylusPressure(pressure) else 1f)
        publishInk(index)
    }

    private fun appendInk(index: Int, x: Float, y: Float, pressure: Float) {
        val world = cells[index].stage.toWorld(x, y)
        val last = points.lastOrNull() ?: return
        val dx = world.x - last.x
        val dy = world.y - last.y
        val gap = INK_SAMPLE_GAP_PX
        if (dx * dx + dy * dy < gap * gap) return
        points.add(world)
        pressures.add(if (stylusInk) stylusPressure(pressure) else 1f)
        publishInk(index)
    }

    private fun publishInk(index: Int) {
        val width = if (stylusInk) {
            stylusPressureWidth(inkWidth, pressures.lastOrNull() ?: 1f)
        } else {
            inkWidth
        }
        liveInk.stroke = Stroke(
            points = ArrayList(points),
            pointPressures = ArrayList(pressures),
            colorValue = inkColor,
            strokeWidth = width,
            isHighlighter = drawTool == WhiteboardTool.HIGHLIGHTER,
            strokeType = drawTool.toStrokeType()
        )
        cells[index].stage.setLiveInk(liveInk)
    }

    private fun startErase(index: Int, x: Float, y: Float) {
        gesturePane = index
        editOpen = true
        host?.beginEdit(index)
        erasing = true
        eraseStrokes = panes[index].strokes
        eraseAt(index, x, y)
    }

    private fun eraseAt(index: Int, x: Float, y: Float) {
        val stage = cells[index].stage
        val radius = eraserSize / stage.viewScale().coerceAtLeast(0.001f)
        eraseStrokes = StrokeEraser.apply(eraseStrokes, stage.toWorld(x, y), radius, eraserMode)
        stage.showGestureStrokes(eraseStrokes)
    }

    private fun startSelect(index: Int, x: Float, y: Float) {
        val stage = cells[index].stage
        val world = stage.toWorld(x, y)
        val hit = panes[index].strokes.indexOfLast { CanvasTransform.strokeIntersectsPoint(it, world, 24f) }
        if (hit < 0) return
        gesturePane = index
        editOpen = true
        host?.beginEdit(index)
        selecting = true
        eraseStrokes = panes[index].strokes
        lastX = x
        lastY = y
        drawingPane = index
        selectIndex = hit
    }

    private var selectIndex = -1
    private var multi = false
    private var fingerPeak = 0
    private var gestureStartX = 0f
    private var gestureStartY = 0f
    private var gestureEndX = 0f
    private var gestureEndY = 0f
    private var gestureTravel = 0f
    private var pinchSpan = 0f
    private var stylusInk = false
    private var drawTool = WhiteboardTool.PEN
    private var drawPointerId = -1
    private val pressures = ArrayList<Float>(256)

    private fun moveSelection(index: Int, x: Float, y: Float) {
        val stage = cells[index].stage
        val previous = stage.toWorld(lastX, lastY)
        val current = stage.toWorld(x, y)
        lastX = x
        lastY = y
        val dx = current.x - previous.x
        val dy = current.y - previous.y
        if (abs(dx) + abs(dy) < 0.4f || selectIndex !in eraseStrokes.indices) return
        eraseStrokes = eraseStrokes.mapIndexed { strokeIndex, stroke ->
            if (strokeIndex != selectIndex) stroke
            else stroke.copy(points = stroke.points.map { Offset(it.x + dx, it.y + dy) })
        }
        stage.showGestureStrokes(eraseStrokes)
    }

    private fun finishGesture() {
        val index = drawingPane
        if (index in cells.indices) {
            val stage = cells[index].stage
            stage.setLiveInk(null)
            when {
                points.size >= 2 -> {
                    val width = if (stylusInk && pressures.isNotEmpty()) {
                        stylusPressureWidth(inkWidth, pressures.average().toFloat())
                    } else {
                        inkWidth
                    }
                    val stroke = (liveInk.stroke ?: Stroke(points = ArrayList(points))).copy(
                        points = ArrayList(points),
                        pointPressures = ArrayList(pressures),
                        strokeWidth = width,
                        strokeType = drawTool.toStrokeType(),
                        isHighlighter = drawTool == WhiteboardTool.HIGHLIGHTER
                    )
                    val next = panes[index].strokes + stroke
                    panes = replacePaneStrokes(panes, index, next)
                    cells[index].shownStrokes = next
                    stage.syncPage(
                        next,
                        background,
                        showGrid,
                        dark,
                        stage.viewScale(),
                        stage.viewOffsetX(),
                        stage.viewOffsetY(),
                        emptySet()
                    )
                    host?.setStrokes(index, next)
                }
                erasing || selecting -> {
                    stage.clearGestureStrokes()
                    panes = replacePaneStrokes(panes, index, eraseStrokes)
                    cells[index].shownStrokes = eraseStrokes
                    stage.syncPage(
                        eraseStrokes,
                        background,
                        showGrid,
                        dark,
                        stage.viewScale(),
                        stage.viewOffsetX(),
                        stage.viewOffsetY(),
                        emptySet()
                    )
                    host?.setStrokes(index, eraseStrokes)
                }
                editOpen -> host?.cancelEdit(index)
            }
        }
        points.clear()
        pressures.clear()
        drawingPane = -1
        gesturePane = -1
        panning = false
        erasing = false
        selecting = false
        editOpen = false
        stylusInk = false
        drawPointerId = -1
        selectIndex = -1
    }

    override fun dispatchDraw(canvas: android.graphics.Canvas) {
        super.dispatchDraw(canvas)
        val frames = if (panes.isEmpty() || width == 0) return else paneFrames(panes.size, width, height, weights)
        val paint = borderPaint
        frames.forEachIndexed { index, frame ->
            paint.strokeWidth = if (index == activeIndex) 6f else 2f
            paint.color = if (index == activeIndex) 0xFF2563EB.toInt() else 0xFFCBD5E1.toInt()
            canvas.drawRect(
                frame.left + 2f,
                frame.top + 2f,
                frame.right - 2f,
                frame.bottom - 2f,
                paint
            )
        }
    }

    private val borderPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.STROKE
    }

    private inner class PaneCell(context: Context, val index: Int) : FrameLayout(context) {
        val stage: BoardStageView = BoardStageView(context)
        var video: VideoView? = null
        var web: WebView? = null
        var videoSource = ""
        var webSource = ""
        var loadingKey = ""
        var shownStrokes: List<Stroke>? = null
        private val bar = LinearLayout(context)

        init {
            addView(stage, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            bar.orientation = LinearLayout.HORIZONTAL
            bar.gravity = Gravity.CENTER_VERTICAL
            bar.setBackgroundColor(0xCC1C2130.toInt())
            val params = LayoutParams(LayoutParams.MATCH_PARENT, (36 * resources.displayMetrics.density).toInt())
            params.gravity = Gravity.TOP
            addView(bar, params)
        }

        fun bindBar(pane: CanvasPane, active: Boolean) {
            bar.removeAllViews()
            if (!active) {
                bar.addView(label(pane.label.ifBlank { pane.kind.name.lowercase().replaceFirstChar { it.uppercase() } }))
                return
            }
            listOf(
                PaneKind.BOARD to "Board",
                PaneKind.PDF to "PDF",
                PaneKind.VIDEO to "Video",
                PaneKind.WEB to "Web"
            ).forEach { (kind, title) ->
                bar.addView(button(title, pane.kind == kind) {
                    if (kind == PaneKind.BOARD) host?.setKind(index, PaneKind.BOARD)
                    if (kind == PaneKind.PDF) host?.pickPdf(index)
                    if (kind == PaneKind.VIDEO) host?.pickVideo(index)
                    if (kind == PaneKind.WEB) host?.pickWeb(index)
                })
            }
            if (pane.kind == PaneKind.PDF && pane.pageCount > 1) {
                bar.addView(button("‹", false) { host?.stepPage(index, -1) })
                bar.addView(label("${pane.pageIndex + 1}/${pane.pageCount}"))
                bar.addView(button("›", false) { host?.stepPage(index, 1) })
            }
        }

        private fun label(text: String): TextView {
            return TextView(context).apply {
                this.text = text
                setTextColor(android.graphics.Color.WHITE)
                textSize = 12f
                setPadding(16, 0, 16, 0)
            }
        }

        private fun button(text: String, selected: Boolean, onClick: () -> Unit): TextView {
            return TextView(context).apply {
                this.text = text
                setTextColor(android.graphics.Color.WHITE)
                textSize = 12f
                setPadding(20, 8, 20, 8)
                setBackgroundColor(if (selected) 0xFF2563EB.toInt() else 0x00000000)
                setOnClickListener { onClick() }
            }
        }
    }
}

@Composable
fun SplitCanvas(
    state: CanvasState,
    viewModel: CanvasViewModel,
    onToggleAi: () -> Unit = {}
) {
    AndroidView(
        factory = { context ->
            SplitBoardView(context).also { board ->
                board.host = object : SplitHost {
                    override fun focus(index: Int) = viewModel.focusPane(index)
                    override fun beginEdit(index: Int) = viewModel.beginPaneEdit(index)
                    override fun cancelEdit(index: Int) = viewModel.cancelPaneEdit(index)
                    override fun setStrokes(index: Int, strokes: List<Stroke>) = viewModel.setPaneStrokes(index, strokes)
                    override fun resize(weights: List<Float>) = viewModel.resizeSplit(weights)
                    override fun setKind(index: Int, kind: PaneKind) = viewModel.setPaneKind(index, kind)
                    override fun pickPdf(index: Int) = viewModel.requestPanePick(index, PaneKind.PDF)
                    override fun pickVideo(index: Int) = viewModel.requestPanePick(index, PaneKind.VIDEO)
                    override fun pickWeb(index: Int) = viewModel.requestPanePick(index, PaneKind.WEB)
                    override fun stepPage(index: Int, delta: Int) = viewModel.stepPanePage(index, delta)
                    override fun swipeBoard(delta: Int) {
                        val latest = viewModel.state.value
                        val pane = latest.splitPanes.getOrNull(latest.splitActive)
                        if (pane != null && pane.kind == PaneKind.PDF && pane.pageCount > 1) {
                            viewModel.stepPanePage(latest.splitActive, delta)
                        } else {
                            val next = pageStep(latest.pageIndex, latest.pageCount, delta)
                            if (next != latest.pageIndex) viewModel.selectPage(next)
                        }
                    }
                    override fun toggleAi() = onToggleAi()
                }
            }
        },
        update = { it.bind(state) },
        modifier = Modifier.fillMaxSize()
    )
}
