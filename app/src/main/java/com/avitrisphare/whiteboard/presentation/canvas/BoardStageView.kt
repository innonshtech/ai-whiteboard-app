package com.avitrisphare.whiteboard.presentation.canvas

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.drawable.GradientDrawable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.avitrisphare.whiteboard.domain.model.CanvasBackground
import com.avitrisphare.whiteboard.domain.model.Stroke
import com.avitrisphare.whiteboard.domain.model.StrokeType
import com.avitrisphare.whiteboard.domain.model.isPlacedMedia
import java.util.concurrent.Executors

/**
 * Ultra-fast hardware-accelerated board.
 * Renders directly onto the GPU hardware canvas display list (RenderNode) with zero software bitmap allocations.
 * Inking, tool changes, and modal opening/closing remain instant at 60/120fps.
 */
class BoardStageView(context: Context) : FrameLayout(context) {
    private val density = Density(
        resources.displayMetrics.density,
        resources.configuration.fontScale
    )
    private val grid = LayerView(context)
    private val media = FrameLayout(context)
    private val ink = LayerView(context)
    private val live = LiveLayer(context)
    private val loader = Executors.newSingleThreadExecutor()
    private val mediaViews = HashMap<String, View>()
    private val players = HashMap<String, MediaPlayer>()

    private var strokes: List<Stroke> = emptyList()
    private var sourceStrokes: List<Stroke>? = null
    private var sourceSelected: Set<String>? = null
    private var gestureStrokes: List<Stroke>? = null
    private var pageBackground = CanvasBackground.DOTTED
    private var customBackgroundUri: String? = null
    private var customBackgroundBitmap: Bitmap? = null
    private var showGrid = false
    private var dark = false
    private var scale = 1f
    private var offsetX = 0f
    private var offsetY = 0f
    private var selected: Set<String> = emptySet()
    private var activeLive: LiveInk? = null

    private val activeLiveStrokes = ArrayList<Stroke>()
    private var documentBitmap: Bitmap? = null
    private var documentWidth = 0
    private var documentHeight = 0
    private var documentFrameX = 0f
    private var documentFrameY = 0f
    private var documentFrameScale = 1f
    private var resizing = false
    private var previewing = false
    private var marquee: RectF? = null
    private val marqueePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x738A8870
        style = Paint.Style.FILL
    }
    private val marqueeOverlay = object : View(context) {
        init {
            setWillNotDraw(false)
        }

        override fun onDraw(canvas: android.graphics.Canvas) {
            val box = marquee ?: return
            val left = offsetX + box.left * scale
            val top = offsetY + box.top * scale
            val right = offsetX + box.right * scale
            val bottom = offsetY + box.bottom * scale
            canvas.drawRoundRect(left, top, right, bottom, 16f, 16f, marqueePaint)
        }
    }
    private var eraserCursorX = 0f
    private var eraserCursorY = 0f
    private var eraserCursorRadius = 0f
    private var eraserCursorVisible = false
    private val eraserCursorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xAA374151.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * density.density
    }
    private val eraserCursorFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x14374151
        style = Paint.Style.FILL
    }
    private val eraserCursorOverlay = object : View(context) {
        init {
            setWillNotDraw(false)
            isClickable = false
        }

        override fun onDraw(canvas: android.graphics.Canvas) {
            if (!eraserCursorVisible || eraserCursorRadius <= 0f) return
            canvas.drawCircle(eraserCursorX, eraserCursorY, eraserCursorRadius, eraserCursorFill)
            canvas.drawCircle(eraserCursorX, eraserCursorY, eraserCursorRadius, eraserCursorPaint)
        }
    }

    init {
        setBackgroundColor(android.graphics.Color.WHITE)
        clipChildren = false
        media.clipChildren = false
        val fill = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        addView(grid, fill)
        addView(media, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(marqueeOverlay, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(ink, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(live, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(eraserCursorOverlay, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        grid.isClickable = false
        media.isClickable = false
        media.isFocusable = false
        ink.isClickable = false
        live.isClickable = false
        eraserCursorOverlay.isClickable = false
        live.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        isClickable = false
        ink.setBackgroundColor(android.graphics.Color.TRANSPARENT)
    }

    fun setFrostedBounds(bounds: List<RectF>) {
        // Optimized: Redundant GPU/CPU full-screen blur filter removed for smooth performance.
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean = super.dispatchTouchEvent(ev)

    private var currentPageIndex = -1

    fun syncPage(
        strokes: List<Stroke>,
        background: CanvasBackground,
        showGrid: Boolean,
        dark: Boolean,
        scale: Float,
        offsetX: Float,
        offsetY: Float,
        selected: Set<String>,
        customBackgroundUri: String = "",
        pageIndex: Int = 0
    ) {
        if (previewing) return
        val customBgChanged = customBackgroundUri != this.customBackgroundUri
        val pageIndexChanged = this.currentPageIndex != pageIndex
        this.currentPageIndex = pageIndex
        val pageChanged = pageIndexChanged || background != this.pageBackground || showGrid != this.showGrid || dark != this.dark || customBgChanged
        val moved = scale != this.scale || offsetX != this.offsetX || offsetY != this.offsetY

        // Fast change check: if stroke list reference and selection reference haven't changed, ink has NOT changed.
        val inkChanged = pageIndexChanged ||
            (sourceStrokes !== strokes) ||
            (sourceSelected != selected)

        this.pageBackground = background
        this.customBackgroundUri = customBackgroundUri
        this.showGrid = showGrid
        this.dark = dark
        this.scale = scale
        this.offsetX = offsetX
        this.offsetY = offsetY
        this.sourceSelected = selected
        this.selected = selected
        this.sourceStrokes = strokes
        this.strokes = strokes

        if (customBgChanged) {
            customBackgroundBitmap = if (customBackgroundUri.isNotBlank()) {
                BitmapCache.decodeSampledBitmap(context, customBackgroundUri)
            } else {
                null
            }
        }
        if (pageIndexChanged) {
            gestureStrokes = null
            activeLive = null
            currentStroke = null
            marquee = null
        } else if (gestureStrokes != null && gestureStrokes !== strokes) {
            gestureStrokes = null
        }

        if (inkChanged || pageChanged) {
            bindMedia(this.strokes)
        }
        if (pageChanged || moved) {
            grid.invalidate()
        }
        if (pageChanged || moved || inkChanged) {
            ink.invalidate()
        }
        if (moved || pageIndexChanged) live.invalidate()
    }

    fun viewScale(): Float = scale
    fun viewOffsetX(): Float = offsetX
    fun viewOffsetY(): Float = offsetY

    fun toWorld(x: Float, y: Float): Offset {
        return CanvasTransform.screenToWorld(Offset(x, y), scale, Offset(offsetX, offsetY))
    }

    fun setMarquee(start: Offset?, end: Offset?) {
        if (start == null || end == null) {
            if (marquee == null) return
            marquee = null
            marqueeOverlay.invalidate()
            return
        }
        marquee = RectF(
            minOf(start.x, end.x),
            minOf(start.y, end.y),
            maxOf(start.x, end.x),
            maxOf(start.y, end.y)
        )
        marqueeOverlay.invalidate()
    }

    fun panBy(dx: Float, dy: Float) {
        setTransform(scale, offsetX + dx, offsetY + dy)
    }

    fun previewTransform(scale: Float, offsetX: Float, offsetY: Float) {
        if (previewing && this.scale == scale && this.offsetX == offsetX && this.offsetY == offsetY) return
        this.scale = scale
        this.offsetX = offsetX
        this.offsetY = offsetY
        previewing = true
        grid.invalidate()
        ink.invalidate()
        live.invalidate()
    }

    fun commitPreview() {
        if (!previewing) return
        previewing = false
        layoutMedia()
        grid.invalidate()
        ink.invalidate()
        live.invalidate()
    }

    fun setResizing(value: Boolean) {
        if (resizing == value) return
        resizing = value
        if (!value) {
            grid.invalidate()
            ink.invalidate()
            live.invalidate()
        }
    }

    fun setTransform(scale: Float, offsetX: Float, offsetY: Float) {
        if (!previewing && this.scale == scale && this.offsetX == offsetX && this.offsetY == offsetY) return
        previewing = false
        this.scale = scale
        this.offsetX = offsetX
        this.offsetY = offsetY
        layoutMedia()
        grid.invalidate()
        ink.invalidate()
        live.invalidate()
    }

    fun setDocumentPage(bitmap: Bitmap?, pageWidth: Float, pageHeight: Float) {
        val width = pageWidth.toInt()
        val height = pageHeight.toInt()
        if (documentBitmap === bitmap && documentWidth == width && documentHeight == height) return
        documentBitmap = bitmap
        documentWidth = width
        documentHeight = height
        grid.invalidate()
    }

    fun setDocumentFrame(x: Float, y: Float, scale: Float) {
        if (documentFrameX == x && documentFrameY == y && documentFrameScale == scale) return
        documentFrameX = x
        documentFrameY = y
        documentFrameScale = scale.coerceIn(0.2f, 3f)
        grid.invalidate()
    }

    fun setLiveInk(ink: LiveInk?) {
        activeLive = ink
        activeLiveStrokes.clear()
        live.invalidate()
    }

    fun setLiveStrokes(strokes: List<Stroke>) {
        activeLiveStrokes.clear()
        activeLiveStrokes.addAll(strokes)
        activeLive = null
        live.invalidate()
    }

    fun clearLiveStrokes() {
        activeLive = null
        activeLiveStrokes.clear()
        live.invalidate()
    }

    fun showEraserCursor(x: Float, y: Float, radiusPx: Float) {
        eraserCursorX = x
        eraserCursorY = y
        eraserCursorRadius = radiusPx.coerceAtLeast(4f)
        eraserCursorVisible = true
        eraserCursorOverlay.invalidate()
    }

    fun hideEraserCursor() {
        if (!eraserCursorVisible) return
        eraserCursorVisible = false
        eraserCursorOverlay.invalidate()
    }

    fun showGestureStrokes(strokes: List<Stroke>) {
        gestureStrokes = strokes
        this.strokes = strokes
        bindMedia(strokes)
        ink.invalidate()
    }

    fun clearGestureStrokes() {
        gestureStrokes = null
    }

    fun shouldDeliverToMedia(event: MotionEvent, tool: WhiteboardTool): Boolean {
        if (tool == WhiteboardTool.HAND || tool == WhiteboardTool.RULER ||
            tool == WhiteboardTool.SPOTLIGHT || tool == WhiteboardTool.SHADE ||
            tool == WhiteboardTool.MAGNIFIER
        ) {
            return false
        }
        if (event.pointerCount != 1) return false
        val kind = event.getToolType(0)
        if (kind == MotionEvent.TOOL_TYPE_STYLUS || kind == MotionEvent.TOOL_TYPE_ERASER ||
            kind == MotionEvent.TOOL_TYPE_MOUSE
        ) {
            return false
        }
        val hit = mediaUnder(event.x, event.y) ?: return false
        return hit is WebView || hit is TextureView
    }

    fun deliverToMedia(event: MotionEvent) {
        val hit = mediaUnder(event.x, event.y) ?: return
        val copy = MotionEvent.obtain(event)
        val location = IntArray(2)
        hit.getLocationOnScreen(location)
        val stageLocation = IntArray(2)
        getLocationOnScreen(stageLocation)
        copy.offsetLocation(
            (stageLocation[0] - location[0]).toFloat(),
            (stageLocation[1] - location[1]).toFloat()
        )
        hit.dispatchTouchEvent(copy)
        copy.recycle()
        if (event.actionMasked == MotionEvent.ACTION_UP &&
            hit is TextureView &&
            event.eventTime - event.downTime < 280L
        ) {
            toggleVideo(hit)
        }
    }

    fun showMagnifier(x: Float, y: Float) {
        if (android.os.Build.VERSION.SDK_INT < 28) return
        if (loupe == null) {
            loupe = if (android.os.Build.VERSION.SDK_INT >= 29) {
                android.widget.Magnifier.Builder(this)
                    .setInitialZoom(2.2f)
                    .setSize(320, 320)
                    .setCornerRadius(160f)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                android.widget.Magnifier(this)
            }
        }
        loupe?.show(x, y)
    }

    fun hideMagnifier() {
        loupe?.dismiss()
    }

    private var loupe: android.widget.Magnifier? = null

    override fun onDetachedFromWindow() {
        hideMagnifier()
        players.values.forEach { player ->
            player.release()
        }
        players.clear()
        loader.shutdownNow()
        super.onDetachedFromWindow()
    }

    private fun mediaUnder(x: Float, y: Float): View? {
        for (index in media.childCount - 1 downTo 0) {
            val child = media.getChildAt(index)
            if (x >= child.left && x <= child.right && y >= child.top && y <= child.bottom) {
                return if (child is FrameLayout && child.childCount > 0) child.getChildAt(0) else child
            }
        }
        return null
    }

    private fun toggleVideo(view: TextureView) {
        val id = view.tag as? String ?: return
        val player = players[id] ?: return
        if (!player.isPlaying) player.start() else player.pause()
    }

    private fun bindMedia(source: List<Stroke>) {
        val wanted = source.filter { it.isPlacedMedia() }
        val ids = wanted.map { it.id }.toSet()
        val gone = mediaViews.keys.filter { it !in ids }
        for (id in gone) {
            media.removeView(mediaViews.remove(id))
            players.remove(id)?.release()
        }
        for (stroke in wanted) {
            val existing = mediaViews[stroke.id]
            val view = existing ?: createMedia(stroke).also { created ->
                mediaViews[stroke.id] = created
                media.addView(created)
            }
            place(view, stroke)
        }
    }

    private fun layoutMedia() {
        for (stroke in strokes) {
            if (!stroke.isPlacedMedia()) continue
            mediaViews[stroke.id]?.let { place(it, stroke) }
        }
    }

    private fun place(view: View, stroke: Stroke) {
        val bounds = stroke.bounds
        val width = (bounds.width * scale).toInt().coerceAtLeast(2)
        val height = (bounds.height * scale).toInt().coerceAtLeast(2)
        val left = (bounds.left * scale + offsetX).toInt()
        val top = (bounds.top * scale + offsetY).toInt()
        val params = LayoutParams(width, height)
        params.leftMargin = left
        params.topMargin = top
        view.layoutParams = params
        view.requestLayout()
    }

    private fun createMedia(stroke: Stroke): View {
        return when (stroke.strokeType) {
            StrokeType.IMAGE -> imageView(stroke)
            StrokeType.VIDEO -> if (stroke.imageUri.isNotEmpty()) videoView(stroke) else webEmbedView(stroke)
            StrokeType.WEB_EMBED -> webEmbedView(stroke)
            else -> View(context)
        }
    }

    private fun imageView(stroke: Stroke): ImageView {
        val view = ImageView(context)
        view.isClickable = false
        view.isLongClickable = false
        view.isFocusable = false
        view.isFocusableInTouchMode = false
        view.setOnTouchListener { _, _ -> false }
        view.scaleType = ImageView.ScaleType.FIT_XY
        view.tag = stroke.id
        val ready = BitmapCache.get(stroke.imageUri)
        if (ready != null) {
            view.setImageBitmap(ready)
            return view
        }
        val path = stroke.imageUri
        loader.execute {
            val bitmap = BitmapCache.decodeSampledBitmap(context, path) ?: return@execute
            post {
                if (view.tag == stroke.id) view.setImageBitmap(bitmap)
            }
        }
        return view
    }

    private fun videoView(stroke: Stroke): View {
        val host = FrameLayout(context)
        val texture = TextureView(context)
        texture.tag = stroke.id
        host.addView(texture, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        texture.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                val player = MediaPlayer()
                players[stroke.id] = player
                player.setSurface(Surface(surface))
                try {
                    player.setDataSource(context, Uri.parse(stroke.imageUri))
                    player.setOnPreparedListener {
                        it.isLooping = true
                        it.start()
                    }
                    player.prepareAsync()
                } catch (_: Exception) {
                    player.release()
                    players.remove(stroke.id)
                }
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) = Unit
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                players.remove(stroke.id)?.release()
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
        }
        return host
    }

    private fun webEmbedView(stroke: Stroke): View {
        val d = context.resources.displayMetrics.density
        val headerHeight = (36f * d).toInt()

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            tag = stroke.id
            val outline = GradientDrawable().apply {
                setColor(android.graphics.Color.WHITE)
                setStroke((1.5f * d).toInt(), android.graphics.Color.parseColor("#94A3B8"))
                cornerRadius = 10f * d
            }
            background = outline
            elevation = 6f * d
            clipToOutline = true
        }

        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((10f * d).toInt(), 0, (8f * d).toInt(), 0)
            setBackgroundColor(android.graphics.Color.parseColor("#E2E8F0"))
        }

        val icon = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_menu_compass)
            setColorFilter(android.graphics.Color.parseColor("#475569"))
            layoutParams = LinearLayout.LayoutParams((18f * d).toInt(), (18f * d).toInt()).apply {
                marginEnd = (6f * d).toInt()
            }
        }
        header.addView(icon)

        val titleText = try {
            Uri.parse(stroke.text).host ?: stroke.text
        } catch (_: Exception) {
            stroke.text
        }.ifBlank { "Website" }

        val titleView = TextView(context).apply {
            text = titleText
            textSize = 12f
            setTextColor(android.graphics.Color.parseColor("#1E293B"))
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        header.addView(titleView)

        val web = WebView(context).apply {
            setBackgroundColor(android.graphics.Color.WHITE)
            isVerticalScrollBarEnabled = true
            isHorizontalScrollBarEnabled = true
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.userAgentString =
                "Mozilla/5.0 (Linux; Android 13; Tablet) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            webChromeClient = WebChromeClient()
            webViewClient = WebViewClient()
            val headers = mapOf("Referer" to "https://www.youtube.com")
            loadUrl(stroke.text, headers)
        }

        val reloadBtn = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_popup_sync)
            setColorFilter(android.graphics.Color.parseColor("#475569"))
            val pad = (4f * d).toInt()
            setPadding(pad, pad, pad, pad)
            layoutParams = LinearLayout.LayoutParams((24f * d).toInt(), (24f * d).toInt())
            setOnClickListener { web.reload() }
        }
        header.addView(reloadBtn)

        container.addView(header, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, headerHeight))
        container.addView(web, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        return container
    }

    fun dispatchTouchToMedia(
        strokeId: String,
        action: Int,
        screenX: Float,
        screenY: Float,
        downTime: Long,
        eventTime: Long
    ): Boolean {
        val container = mediaViews[strokeId] ?: return false
        val web = findWebView(container) ?: return false

        val stageLoc = IntArray(2)
        getLocationOnScreen(stageLoc)
        val webLoc = IntArray(2)
        web.getLocationOnScreen(webLoc)

        val localX = screenX - (webLoc[0] - stageLoc[0])
        val localY = screenY - (webLoc[1] - stageLoc[1])

        val event = MotionEvent.obtain(downTime, eventTime, action, localX, localY, 0)
        val handled = web.dispatchTouchEvent(event)
        event.recycle()
        return handled
    }

    private fun findWebView(root: View): WebView? {
        if (root is WebView) return root
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                val child = root.getChildAt(i)
                findWebView(child)?.let { return it }
            }
        }
        return null
    }

    /**
     * Hardware-accelerated direct vector layer.
     * Evaluates directly onto Android's display list RenderNode without any software bitmap intermediary.
     */
    private inner class LayerView(context: Context) : View(context) {
        init {
            setWillNotDraw(false)
        }

        private val layerScope = CanvasDrawScope()

        override fun onDraw(canvas: android.graphics.Canvas) {
            val width = width.coerceAtLeast(1)
            val height = height.coerceAtLeast(1)

            layerScope.draw(
                density,
                LayoutDirection.Ltr,
                androidx.compose.ui.graphics.Canvas(canvas),
                Size(width.toFloat(), height.toFloat())
            ) {
                paintLayer()
            }
        }

        private fun DrawScope.paintLayer() {
            if (this@LayerView === grid) {
                if (pageBackground == CanvasBackground.CUSTOM && customBackgroundBitmap != null && !customBackgroundBitmap!!.isRecycled) {
                    drawImage(
                        image = customBackgroundBitmap!!.asImageBitmap(),
                        dstOffset = IntOffset.Zero,
                        dstSize = IntSize(size.width.toInt(), size.height.toInt())
                    )
                } else {
                    drawPageGrid(pageBackground, showGrid, dark, scale, offsetX, offsetY)
                }
                val page = documentBitmap
                if (page != null && !page.isRecycled && documentWidth > 0 && documentHeight > 0) {
                    withTransform({
                        translate(offsetX, offsetY)
                        scale(scale, scale, pivot = Offset.Zero)
                        translate(documentFrameX, documentFrameY)
                        scale(documentFrameScale, documentFrameScale, pivot = Offset.Zero)
                    }) {
                        drawImage(
                            image = page.asImageBitmap(),
                            dstOffset = IntOffset.Zero,
                            dstSize = IntSize(documentWidth, documentHeight)
                        )
                    }
                }
            } else {
                drawVectorInk(context, strokes, scale, offsetX, offsetY, selected)
            }
        }
    }

    /**
     * In-progress ink only.
     * Completely bypasses intermediate buffers and draws directly onto the GPU hardware canvas.
     */
    private inner class LiveLayer(context: Context) : View(context) {
        init {
            setWillNotDraw(false)
        }

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        private val stickerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val path = Path()
        private val liveScope = CanvasDrawScope()

        override fun onDraw(canvas: android.graphics.Canvas) {
            if (activeLiveStrokes.isNotEmpty()) {
                canvas.save()
                canvas.translate(offsetX, offsetY)
                canvas.scale(scale, scale)
                for (s in activeLiveStrokes) {
                    if (s.points.isNotEmpty()) {
                        drawStroke(canvas, s, paint)
                    }
                }
                canvas.restore()
                return
            }

            val stroke = activeLive?.stroke
            val active = currentStroke
            if ((stroke == null || stroke.points.isEmpty()) && (active == null || active.points.isEmpty())) return
            val s = if (active != null && active.points.isNotEmpty()) active else stroke!!
            if (s.points.size >= 2 && (isEditableShape(s.strokeType) || s.strokeType == StrokeType.FRAME)) {
                liveScope.draw(density, LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(canvas), Size(width.toFloat(), height.toFloat())) {
                    inPageSpace(offsetX, offsetY, scale) {
                        renderStroke(context, s, scale, false)
                    }
                }
                return
            }
            canvas.save()
            canvas.translate(offsetX, offsetY)
            canvas.scale(scale, scale)

            if (stroke != null && stroke.points.isNotEmpty()) drawStroke(canvas, stroke, paint)
            if (active != null && active !== stroke && active.points.isNotEmpty()) drawStroke(canvas, active, paint)

            canvas.restore()
        }

        private fun drawStroke(canvas: android.graphics.Canvas, stroke: Stroke, p: Paint) {
            val points = stroke.points
            if (stroke.strokeType == StrokeType.STICKER && points.isNotEmpty()) {
                val origin = points.first()
                val label = when (stroke.stickerKind) {
                    "star" -> "⭐"
                    "tick" -> "✓"
                    "cross" -> "✕"
                    "thumb" -> "👍"
                    "emoji" -> stroke.text.ifEmpty { "😀" }
                    else -> stroke.text.ifEmpty { stroke.stickerKind }
                }
                stickerTextPaint.color = when (stroke.stickerKind) {
                    "tick" -> 0xFF10B981.toInt()
                    "cross" -> 0xFFEF4444.toInt()
                    "star" -> 0xFFF59E0B.toInt()
                    else -> stroke.colorValue
                }
                stickerTextPaint.textSize = (stroke.strokeWidth * 14f).coerceIn(56f, 150f)
                canvas.drawText(label, origin.x, origin.y + stickerTextPaint.textSize * 0.88f, stickerTextPaint)
                return
            }
            if (stroke.strokeType == StrokeType.CONNECTOR && points.size >= 2) {
                p.color = stroke.colorValue
                p.strokeWidth = stroke.strokeWidth.coerceAtLeast(3f)
                val prevStyle = p.style
                p.style = Paint.Style.STROKE
                p.strokeCap = Paint.Cap.ROUND
                p.strokeJoin = Paint.Join.ROUND
                if (stroke.stickerKind == "straight") {
                    for (i in 0 until points.size - 1) {
                        canvas.drawLine(points[i].x, points[i].y, points[i + 1].x, points[i + 1].y, p)
                    }
                } else if (stroke.stickerKind == "curved") {
                    val wirePath = MindmapEngine.buildNativeCurvedPath(points)
                    canvas.drawPath(wirePath, p)
                } else {
                    val wirePath = MindmapEngine.buildNativeOrthogonalPath(points)
                    canvas.drawPath(wirePath, p)
                }
                p.style = prevStyle
                return
            }
            p.color = stroke.colorValue
            p.strokeWidth = fittedStrokeWidth(stroke)
            if (points.size >= 2 && (
                    stroke.strokeType == StrokeType.RECTANGLE ||
                        stroke.strokeType == StrokeType.FRAME ||
                        stroke.strokeType == StrokeType.CIRCLE
                    )
            ) {
                p.alpha = 255
                val a = points.first()
                val b = points.last()
                if (stroke.strokeType == StrokeType.CIRCLE) {
                    canvas.drawOval(minOf(a.x, b.x), minOf(a.y, b.y), maxOf(a.x, b.x), maxOf(a.y, b.y), p)
                } else {
                    canvas.drawRect(minOf(a.x, b.x), minOf(a.y, b.y), maxOf(a.x, b.x), maxOf(a.y, b.y), p)
                }
                return
            }
            p.alpha = if (stroke.strokeType == StrokeType.HIGHLIGHTER || stroke.isHighlighter) 102 else 255
            if (points.size == 1) {
                val previous = p.style
                p.style = Paint.Style.FILL
                canvas.drawCircle(points[0].x, points[0].y, p.strokeWidth / 2f, p)
                p.style = previous
                return
            }
            canvas.drawPath(strokePath(points), p)
        }

        private fun strokePath(points: List<Offset>): Path {
            path.reset()
            path.moveTo(points[0].x, points[0].y)
            for (index in 1 until points.size - 1) {
                val midX = (points[index].x + points[index + 1].x) / 2f
                val midY = (points[index].y + points[index + 1].y) / 2f
                path.quadTo(points[index].x, points[index].y, midX, midY)
            }
            path.lineTo(points.last().x, points.last().y)
            return path
        }

        private fun fittedStrokeWidth(stroke: Stroke): Float {
            val safe = scale.coerceAtLeast(0.001f)
            val multiplier = when (stroke.strokeType) {
                StrokeType.MARKER -> 1.7f
                StrokeType.HIGHLIGHTER -> 2.4f
                StrokeType.BRUSH -> 1.25f
                else -> if (stroke.isHighlighter) 3f else 1f
            }
            return (stroke.strokeWidth * multiplier / safe).coerceAtLeast(1.5f / safe)
        }
    }

    var currentStroke: Stroke? = null
        set(value) {
            if (field === value) return
            field = value
            live.invalidate()
        }
}
