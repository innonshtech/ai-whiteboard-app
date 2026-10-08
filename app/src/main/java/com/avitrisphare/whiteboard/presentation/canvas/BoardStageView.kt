package com.avitrisphare.whiteboard.presentation.canvas

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.avitrisphare.whiteboard.domain.model.CanvasBackground
import com.avitrisphare.whiteboard.domain.model.Stroke
import com.avitrisphare.whiteboard.domain.model.StrokeType
import com.avitrisphare.whiteboard.domain.model.isPlacedMedia
import java.util.concurrent.Executors

/**
 * Hardware-composited board. The page grid, photos, video, and browser each
 * sit on their own layer. Ink is a transparent overlay, so a pen move repaints
 * only the new stroke.
 */
class BoardStageView(context: Context) : FrameLayout(context) {
    private val grid = LayerView(context)
    private val media = FrameLayout(context)
    private val ink = LayerView(context)
    private val density = Density(
        resources.displayMetrics.density,
        resources.configuration.fontScale
    )
    private val loader = Executors.newSingleThreadExecutor()
    private val images = HashMap<String, Bitmap>()
    private val mediaViews = HashMap<String, View>()
    private val players = HashMap<String, MediaPlayer>()

    private var strokes: List<Stroke> = emptyList()
    private var gestureStrokes: List<Stroke>? = null
    private var pageBackground = CanvasBackground.DOTTED
    private var showGrid = false
    private var dark = false
    private var scale = 1f
    private var offsetX = 0f
    private var offsetY = 0f
    private var selected: Set<String> = emptySet()
    private var gridDirty = true
    private var inkDirty = true
    private var activeLive: LiveInk? = null

    init {
        setBackgroundColor(android.graphics.Color.WHITE)
        clipChildren = false
        media.clipChildren = false
        val fill = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        addView(grid, fill)
        addView(media, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(ink, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        grid.isClickable = false
        ink.isClickable = false
        isClickable = false
        ink.setBackgroundColor(android.graphics.Color.TRANSPARENT)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean = false

    fun syncPage(
        strokes: List<Stroke>,
        background: CanvasBackground,
        showGrid: Boolean,
        dark: Boolean,
        scale: Float,
        offsetX: Float,
        offsetY: Float,
        selected: Set<String>
    ) {
        val pageChanged = background != this.pageBackground || showGrid != this.showGrid || dark != this.dark
        val moved = scale != this.scale || offsetX != this.offsetX || offsetY != this.offsetY
        val inkChanged = this.strokes !== strokes || this.selected !== selected
        this.pageBackground = background
        this.showGrid = showGrid
        this.dark = dark
        this.scale = scale
        this.offsetX = offsetX
        this.offsetY = offsetY
        this.selected = selected
        if (gestureStrokes == null) this.strokes = strokes
        if (pageChanged || moved) gridDirty = true
        if (pageChanged || moved || inkChanged) inkDirty = true
        bindMedia(if (gestureStrokes == null) strokes else this.strokes)
        grid.invalidate()
        ink.invalidate()
    }

    fun setTransform(scale: Float, offsetX: Float, offsetY: Float) {
        if (this.scale == scale && this.offsetX == offsetX && this.offsetY == offsetY) return
        this.scale = scale
        this.offsetX = offsetX
        this.offsetY = offsetY
        gridDirty = true
        inkDirty = true
        layoutMedia()
        grid.invalidate()
        ink.invalidate()
    }

    fun setLiveInk(ink: LiveInk?) {
        activeLive = ink
        this.ink.invalidate()
    }

    fun showGestureStrokes(strokes: List<Stroke>) {
        gestureStrokes = strokes
        this.strokes = strokes
        inkDirty = true
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
            images.remove(id)?.recycle()
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
    }

    private fun createMedia(stroke: Stroke): View {
        return when (stroke.strokeType) {
            StrokeType.IMAGE -> imageView(stroke)
            StrokeType.VIDEO -> if (stroke.imageUri.isNotEmpty()) videoView(stroke) else webView(stroke.text)
            StrokeType.WEB_EMBED -> webView(stroke.text)
            else -> View(context)
        }
    }

    private fun imageView(stroke: Stroke): ImageView {
        val view = ImageView(context)
        view.scaleType = ImageView.ScaleType.FIT_XY
        view.tag = stroke.id
        val ready = images[stroke.id]
        if (ready != null) {
            view.setImageBitmap(ready)
            return view
        }
        val path = stroke.imageUri
        loader.execute {
            val bitmap = decodeFile(path) ?: return@execute
            post {
                images[stroke.id] = bitmap
                if (view.tag == stroke.id) view.setImageBitmap(bitmap)
            }
        }
        return view
    }

    private fun decodeFile(path: String): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            if (path.startsWith("/")) BitmapFactory.decodeFile(path, bounds)
            else context.contentResolver.openInputStream(Uri.parse(path))?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            var sample = 1
            while (bounds.outWidth / sample > 2048 || bounds.outHeight / sample > 2048) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            if (path.startsWith("/")) BitmapFactory.decodeFile(path, opts)
            else context.contentResolver.openInputStream(Uri.parse(path))?.use {
                BitmapFactory.decodeStream(it, null, opts)
            }
        } catch (_: Exception) {
            null
        }
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

    private fun webView(url: String): WebView {
        val web = WebView(context)
        web.setBackgroundColor(android.graphics.Color.WHITE)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.mediaPlaybackRequiresUserGesture = false
        web.settings.userAgentString =
            "Mozilla/5.0 (Linux; Android 13; Tablet) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        web.webChromeClient = WebChromeClient()
        web.webViewClient = WebViewClient()
        val headers = mapOf("Referer" to "https://www.youtube.com")
        web.loadUrl(url, headers)
        return web
    }

    private inner class LayerView(context: Context) : View(context) {
        private var bitmap: Bitmap? = null

        override fun onDraw(canvas: android.graphics.Canvas) {
            val width = width.coerceAtLeast(1)
            val height = height.coerceAtLeast(1)
            val dirty = if (this === grid) gridDirty else inkDirty
            if (dirty || bitmap == null || bitmap?.width != width || bitmap?.height != height) {
                val next = if (bitmap != null && bitmap?.width == width && bitmap?.height == height) {
                    bitmap!!
                } else {
                    bitmap?.recycle()
                    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                }
                bitmap = next
                next.eraseColor(android.graphics.Color.TRANSPARENT)
                val scope = CanvasDrawScope()
                scope.draw(density, LayoutDirection.Ltr, Canvas(next.asImageBitmap()), Size(width.toFloat(), height.toFloat())) {
                    paintLayer()
                }
                if (this === grid) gridDirty = false else inkDirty = false
            }
            canvas.drawBitmap(bitmap!!, 0f, 0f, null)
            if (this === ink) {
                drawLive(canvas)
            }
        }

        private fun DrawScope.paintLayer() {
            if (this@LayerView === grid) {
                drawPageGrid(pageBackground, showGrid, dark, scale, offsetX, offsetY)
            } else {
                drawVectorInk(context, strokes, scale, offsetX, offsetY, selected)
            }
        }

        private val livePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            style = android.graphics.Paint.Style.STROKE
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
        }

        private fun drawLive(canvas: android.graphics.Canvas) {
            val live = activeLive?.stroke
            val active = currentStroke
            if (live == null && (active == null || active.points.isEmpty())) return
            canvas.save()
            canvas.translate(offsetX, offsetY)
            canvas.scale(scale, scale)
            if (live != null && live.points.isNotEmpty()) drawInkPath(canvas, live)
            if (active != null && active.points.isNotEmpty()) drawInkPath(canvas, active)
            canvas.restore()
        }

        private fun drawInkPath(canvas: android.graphics.Canvas, stroke: Stroke) {
            val points = stroke.points
            if (points.size >= 2 && stroke.strokeType == StrokeType.RECTANGLE ||
                points.size >= 2 && stroke.strokeType == StrokeType.FRAME ||
                points.size >= 2 && stroke.strokeType == StrokeType.CIRCLE
            ) {
                val a = points.first()
                val b = points.last()
                val left = minOf(a.x, b.x)
                val top = minOf(a.y, b.y)
                val right = maxOf(a.x, b.x)
                val bottom = maxOf(a.y, b.y)
                livePaint.color = stroke.colorValue
                livePaint.alpha = 255
                livePaint.strokeWidth = fittedStrokeWidth(stroke)
                if (stroke.strokeType == StrokeType.CIRCLE) {
                    canvas.drawOval(left, top, right, bottom, livePaint)
                } else {
                    canvas.drawRect(left, top, right, bottom, livePaint)
                }
                return
            }
            livePaint.color = stroke.colorValue
            livePaint.alpha = if (stroke.strokeType == StrokeType.HIGHLIGHTER || stroke.isHighlighter) 102 else 255
            livePaint.strokeWidth = fittedStrokeWidth(stroke)
            if (points.size == 1) {
                val previous = livePaint.style
                livePaint.style = android.graphics.Paint.Style.FILL
                canvas.drawCircle(points[0].x, points[0].y, livePaint.strokeWidth / 2f, livePaint)
                livePaint.style = previous
                return
            }
            val path = android.graphics.Path()
            path.moveTo(points[0].x, points[0].y)
            for (index in 1 until points.size - 1) {
                val midX = (points[index].x + points[index + 1].x) / 2f
                val midY = (points[index].y + points[index + 1].y) / 2f
                path.quadTo(points[index].x, points[index].y, midX, midY)
            }
            val last = points.last()
            path.lineTo(last.x, last.y)
            canvas.drawPath(path, livePaint)
        }

        /** Matches the saved stroke. The canvas scale is already applied, so the width is divided back out. */
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
            field = value
            ink.invalidate()
        }
}
