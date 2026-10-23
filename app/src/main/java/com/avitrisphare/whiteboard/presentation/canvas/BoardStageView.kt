package com.avitrisphare.whiteboard.presentation.canvas

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.SurfaceTexture
import android.os.Build
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
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
 * Hardware-composited board. The page grid, photos, video, and browser each
 * sit on their own layer. Ink is a transparent overlay, so a pen move repaints
 * only the new stroke.
 */
class BoardStageView(context: Context) : FrameLayout(context) {
    private val density = Density(
        resources.displayMetrics.density,
        resources.configuration.fontScale
    )
    private val grid = LayerView(context)
    private val media = FrameLayout(context)
    private val ink = LayerView(context)
    private val blurredInk = BlurredInkView(context)
    private val live = LiveLayer(context)
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
    private var frostedBounds: List<android.graphics.RectF> = emptyList()
    private var activeLive: LiveInk? = null
    private var documentBitmap: Bitmap? = null
    private var documentWidth = 0
    private var documentHeight = 0
    private var documentFrameX = 0f
    private var documentFrameY = 0f
    private var documentFrameScale = 1f
    private var resizing = false
    private var previewing = false
    private var paintScale = 1f
    private var paintOffsetX = 0f
    private var paintOffsetY = 0f
    private var marquee: android.graphics.RectF? = null
    private val marqueePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x738A8870
        style = android.graphics.Paint.Style.FILL
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

    init {
        setBackgroundColor(android.graphics.Color.WHITE)
        clipChildren = false
        media.clipChildren = false
        val fill = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        addView(grid, fill)
        addView(media, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(marqueeOverlay, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(ink, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(blurredInk, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(live, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        grid.isClickable = false
        ink.isClickable = false
        blurredInk.isClickable = false
        live.isClickable = false
        blurredInk.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        live.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        isClickable = false
        ink.setBackgroundColor(android.graphics.Color.TRANSPARENT)
    }

    fun setFrostedBounds(bounds: List<android.graphics.RectF>) {
        if (sameFrost(this.frostedBounds, bounds)) return
        this.frostedBounds = bounds
        ink.invalidate()
        blurredInk.invalidate()
        if (currentStroke != null || activeLive != null) live.invalidate()
    }

    private fun sameFrost(current: List<android.graphics.RectF>, next: List<android.graphics.RectF>): Boolean {
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
        if (previewing) return
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
        if (inkChanged || pageChanged) {
            bindMedia(if (gestureStrokes == null) strokes else this.strokes)
        }
        if (pageChanged || moved) {
            gridDirty = true
            grid.invalidate()
        }
        if (pageChanged || moved || inkChanged) {
            inkDirty = true
            ink.invalidate()
            blurredInk.invalidate()
        }
        if (moved) live.invalidate()
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
        marquee = android.graphics.RectF(
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

    /**
     * Pinch zoom draws the existing page bitmap with a matrix. The full page is
     * rebuilt once, when the fingers lift.
     */
    fun previewTransform(scale: Float, offsetX: Float, offsetY: Float) {
        if (previewing && this.scale == scale && this.offsetX == offsetX && this.offsetY == offsetY) return
        this.scale = scale
        this.offsetX = offsetX
        this.offsetY = offsetY
        previewing = true
        grid.invalidate()
        ink.invalidate()
        blurredInk.invalidate()
        live.invalidate()
    }

    fun commitPreview() {
        if (!previewing) return
        previewing = false
        gridDirty = true
        inkDirty = true
        layoutMedia()
        grid.invalidate()
        ink.invalidate()
        blurredInk.invalidate()
        live.invalidate()
    }

    /** While a divider is moving, scale the existing bitmap instead of allocating a new one. */
    fun setResizing(value: Boolean) {
        if (resizing == value) return
        resizing = value
        if (!value) {
            gridDirty = true
            inkDirty = true
            grid.invalidate()
            ink.invalidate()
            blurredInk.invalidate()
            live.invalidate()
        }
    }

    fun setTransform(scale: Float, offsetX: Float, offsetY: Float) {
        if (!previewing && this.scale == scale && this.offsetX == offsetX && this.offsetY == offsetY) return
        previewing = false
        this.scale = scale
        this.offsetX = offsetX
        this.offsetY = offsetY
        gridDirty = true
        inkDirty = true
        layoutMedia()
        grid.invalidate()
        ink.invalidate()
        blurredInk.invalidate()
        live.invalidate()
    }

    /**
     * The document is part of the background layer. Updating it does not
     * repaint the ink overlay, so annotation stays as light as a blank page.
     */
    fun setDocumentPage(bitmap: Bitmap?, pageWidth: Float, pageHeight: Float) {
        val width = pageWidth.toInt()
        val height = pageHeight.toInt()
        if (documentBitmap === bitmap && documentWidth == width && documentHeight == height) return
        documentBitmap = bitmap
        documentWidth = width
        documentHeight = height
        gridDirty = true
        grid.invalidate()
    }

    fun setDocumentFrame(x: Float, y: Float, scale: Float) {
        if (documentFrameX == x && documentFrameY == y && documentFrameScale == scale) return
        documentFrameX = x
        documentFrameY = y
        documentFrameScale = scale.coerceIn(0.2f, 3f)
        gridDirty = true
        grid.invalidate()
    }

    fun setLiveInk(ink: LiveInk?) {
        activeLive = ink
        live.invalidate()
    }

    fun showGestureStrokes(strokes: List<Stroke>) {
        gestureStrokes = strokes
        this.strokes = strokes
        inkDirty = true
        bindMedia(strokes)
        ink.invalidate()
        blurredInk.invalidate()
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

        fun currentBitmap(): Bitmap? = bitmap

        override fun onDraw(canvas: android.graphics.Canvas) {
            val width = width.coerceAtLeast(1)
            val height = height.coerceAtLeast(1)
            val existing = bitmap
            if (previewing && existing != null && !existing.isRecycled) {
                val ratio = if (paintScale == 0f) 1f else scale / paintScale
                canvas.save()
                canvas.translate(offsetX, offsetY)
                canvas.scale(ratio, ratio)
                canvas.translate(-paintOffsetX, -paintOffsetY)
                drawBitmapWithFrostedClip(canvas, existing)
                canvas.restore()
                return
            }
            if (resizing && existing != null && !existing.isRecycled) {
                canvas.drawBitmap(existing, null, android.graphics.Rect(0, 0, width, height), null)
                return
            }
            val dirty = if (this === grid) gridDirty else inkDirty
            if (dirty || existing == null || existing.width != width || existing.height != height) {
                paintScale = scale
                paintOffsetX = offsetX
                paintOffsetY = offsetY
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
                if (this === grid) gridDirty = false else {
                    inkDirty = false
                    blurredInk.invalidate()
                }
            }
            val bmp = bitmap
            if (bmp != null && !bmp.isRecycled) {
                drawBitmapWithFrostedClip(canvas, bmp)
            }
        }

        private fun drawBitmapWithFrostedClip(canvas: android.graphics.Canvas, bmp: Bitmap) {
            if (this === ink && frostedBounds.isNotEmpty()) {
                canvas.save()
                val clipPath = android.graphics.Path()
                val r = 22f * density.density
                for (rect in frostedBounds) {
                    clipPath.addRoundRect(rect, r, r, android.graphics.Path.Direction.CW)
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    canvas.clipOutPath(clipPath)
                } else {
                    @Suppress("DEPRECATION")
                    canvas.clipPath(clipPath, android.graphics.Region.Op.DIFFERENCE)
                }
                canvas.drawBitmap(bmp, 0f, 0f, null)
                canvas.restore()
            } else {
                canvas.drawBitmap(bmp, 0f, 0f, null)
            }
        }

        private fun DrawScope.paintLayer() {
            if (this@LayerView === grid) {
                drawPageGrid(pageBackground, showGrid, dark, scale, offsetX, offsetY)
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

    private inner class BlurredInkView(context: Context) : View(context) {
        init {
            setWillNotDraw(false)
            isClickable = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val blurPx = 6f * density.density
                setRenderEffect(RenderEffect.createBlurEffect(blurPx, blurPx, Shader.TileMode.CLAMP))
            }
        }

        override fun onDraw(canvas: android.graphics.Canvas) {
            if (frostedBounds.isEmpty()) return
            val b = ink.currentBitmap() ?: return
            if (b.isRecycled) return

            canvas.save()
            val path = android.graphics.Path()
            val r = 22f * density.density
            for (rect in frostedBounds) {
                path.addRoundRect(rect, r, r, android.graphics.Path.Direction.CW)
            }
            canvas.clipPath(path)

            if (previewing) {
                val ratio = if (paintScale == 0f) 1f else scale / paintScale
                canvas.translate(offsetX, offsetY)
                canvas.scale(ratio, ratio)
                canvas.translate(-paintOffsetX, -paintOffsetY)
            }
            canvas.drawBitmap(b, 0f, 0f, null)
            canvas.restore()
        }
    }

    /**
     * In-progress ink only. Committed strokes stay on the ink bitmap, so a pen
     * sample does not upload that bitmap again.
     */
    private inner class LiveLayer(context: Context) : View(context) {
        private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            style = android.graphics.Paint.Style.STROKE
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
        }
        private val liveBlurPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            style = android.graphics.Paint.Style.STROKE
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
            maskFilter = android.graphics.BlurMaskFilter(6f * density.density, android.graphics.BlurMaskFilter.Blur.NORMAL)
        }
        private val path = android.graphics.Path()
        private val liveScope = CanvasDrawScope()

        override fun onDraw(canvas: android.graphics.Canvas) {
            val stroke = activeLive?.stroke
            val active = currentStroke
            if ((stroke == null || stroke.points.isEmpty()) && (active == null || active.points.isEmpty())) return
            val s = if (active != null && active.points.isNotEmpty()) active else stroke!!
            if (s.points.size >= 2 && (isEditableShape(s.strokeType) || s.strokeType == StrokeType.FRAME)) {
                liveScope.draw(density, LayoutDirection.Ltr, Canvas(canvas), Size(width.toFloat(), height.toFloat())) {
                    inPageSpace(offsetX, offsetY, scale) {
                        renderStroke(context, s, scale, false)
                    }
                }
                return
            }
            canvas.save()
            canvas.translate(offsetX, offsetY)
            canvas.scale(scale, scale)

            if (frostedBounds.isNotEmpty()) {
                canvas.save()
                for (rect in frostedBounds) {
                    val invLeft = (rect.left - offsetX) / scale
                    val invTop = (rect.top - offsetY) / scale
                    val invRight = (rect.right - offsetX) / scale
                    val invBottom = (rect.bottom - offsetY) / scale
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        canvas.clipOutRect(invLeft, invTop, invRight, invBottom)
                    } else {
                        @Suppress("DEPRECATION")
                        canvas.clipRect(invLeft, invTop, invRight, invBottom, android.graphics.Region.Op.DIFFERENCE)
                    }
                }
                if (stroke != null && stroke.points.isNotEmpty()) drawStroke(canvas, stroke, paint)
                if (active != null && active !== stroke && active.points.isNotEmpty()) drawStroke(canvas, active, paint)
                canvas.restore()

                canvas.save()
                val clipPath = android.graphics.Path()
                val r = 22f * density.density / scale
                for (rect in frostedBounds) {
                    val invLeft = (rect.left - offsetX) / scale
                    val invTop = (rect.top - offsetY) / scale
                    val invRight = (rect.right - offsetX) / scale
                    val invBottom = (rect.bottom - offsetY) / scale
                    clipPath.addRoundRect(invLeft, invTop, invRight, invBottom, r, r, android.graphics.Path.Direction.CW)
                }
                canvas.clipPath(clipPath)
                if (stroke != null && stroke.points.isNotEmpty()) drawStroke(canvas, stroke, liveBlurPaint)
                if (active != null && active !== stroke && active.points.isNotEmpty()) drawStroke(canvas, active, liveBlurPaint)
                canvas.restore()
            } else {
                if (stroke != null && stroke.points.isNotEmpty()) drawStroke(canvas, stroke, paint)
                if (active != null && active !== stroke && active.points.isNotEmpty()) drawStroke(canvas, active, paint)
            }
            canvas.restore()
        }

        private fun drawStroke(canvas: android.graphics.Canvas, stroke: Stroke, p: android.graphics.Paint) {
            val points = stroke.points
            val blurBoost = if (p === liveBlurPaint) 1.5f else 1f
            if (stroke.strokeType == StrokeType.STICKER && points.isNotEmpty()) {
                if (p === liveBlurPaint) return
                val origin = points.first()
                val label = when (stroke.stickerKind) {
                    "star" -> "⭐"
                    "tick" -> "✓"
                    "cross" -> "✕"
                    "thumb" -> "👍"
                    "emoji" -> stroke.text.ifEmpty { "😀" }
                    else -> stroke.text.ifEmpty { stroke.stickerKind }
                }
                val textPaint = android.graphics.Paint().apply {
                    color = when (stroke.stickerKind) {
                        "tick" -> 0xFF10B981.toInt()
                        "cross" -> 0xFFEF4444.toInt()
                        "star" -> 0xFFF59E0B.toInt()
                        else -> stroke.colorValue
                    }
                    textSize = (stroke.strokeWidth * 14f).coerceIn(56f, 150f)
                    isAntiAlias = true
                }
                canvas.drawText(label, origin.x, origin.y + textPaint.textSize * 0.88f, textPaint)
                return
            }
            if (stroke.strokeType == StrokeType.CONNECTOR && points.size >= 2) {
                val p1 = points.first()
                val p2 = points.last()
                p.color = stroke.colorValue
                p.strokeWidth = stroke.strokeWidth.coerceAtLeast(3f) * blurBoost
                canvas.drawLine(p1.x, p1.y, p2.x, p2.y, p)
                val angle = kotlin.math.atan2(p2.y - p1.y, p2.x - p1.x)
                val arrowLen = (stroke.strokeWidth * 4.5f).coerceIn(16f, 32f)
                val arrowAngle = Math.PI / 6.0
                val ap1X = p2.x - (arrowLen * kotlin.math.cos(angle - arrowAngle)).toFloat()
                val ap1Y = p2.y - (arrowLen * kotlin.math.sin(angle - arrowAngle)).toFloat()
                val ap2X = p2.x - (arrowLen * kotlin.math.cos(angle + arrowAngle)).toFloat()
                val ap2Y = p2.y - (arrowLen * kotlin.math.sin(angle + arrowAngle)).toFloat()
                val arrowPath = android.graphics.Path().apply {
                    moveTo(p2.x, p2.y)
                    lineTo(ap1X, ap1Y)
                    lineTo(ap2X, ap2Y)
                    close()
                }
                val prevStyle = p.style
                p.style = android.graphics.Paint.Style.FILL
                canvas.drawPath(arrowPath, p)
                p.style = prevStyle
                return
            }
            p.color = stroke.colorValue
            p.strokeWidth = fittedStrokeWidth(stroke) * blurBoost
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
                p.style = android.graphics.Paint.Style.FILL
                canvas.drawCircle(points[0].x, points[0].y, p.strokeWidth / 2f, p)
                p.style = previous
                return
            }
            canvas.drawPath(strokePath(points), p)
        }

        private fun strokePath(points: List<Offset>): android.graphics.Path {
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
