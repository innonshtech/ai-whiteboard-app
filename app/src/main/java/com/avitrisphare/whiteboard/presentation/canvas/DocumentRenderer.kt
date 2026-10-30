package com.avitrisphare.whiteboard.presentation.canvas

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File
import kotlin.math.max

/**
 * Builds one background bitmap for a document page. Callers draw it on the
 * page layer and keep pen strokes on the ink layer, so a stroke does not
 * re-render the file.
 */
object DocumentRenderer {
    const val TARGET_WIDTH = 1600

    fun pageCount(file: File, kind: DocumentKind): Int {
        return when (kind) {
            DocumentKind.PDF -> pdfCount(file)
            DocumentKind.PPT -> PptxSlides.read(file).slides.size
        }
    }

    fun render(file: File, kind: DocumentKind, index: Int): Bitmap {
        return when (kind) {
            DocumentKind.PDF -> renderPdf(file, index)
            DocumentKind.PPT -> renderSlide(PptxSlides.read(file), index)
        }
    }

    private fun pdfCount(file: File): Int {
        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        descriptor.use {
            PdfRenderer(it).use { renderer ->
                return renderer.pageCount
            }
        }
    }

    private fun renderPdf(file: File, index: Int): Bitmap {
        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        descriptor.use {
            PdfRenderer(it).use { renderer ->
                val page = renderer.openPage(index.coerceIn(0, renderer.pageCount - 1))
                page.use {
                    val width = TARGET_WIDTH
                    val height = max(1, (page.height.toFloat() / page.width.coerceAtLeast(1) * width).toInt())
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    return bitmap
                }
            }
        }
    }

    fun renderSlide(deck: PptxDeck, index: Int): Bitmap {
        val slide = deck.slides[index.coerceIn(0, deck.slides.lastIndex)]
        val (width, height) = PptxSlides.pixelSize(deck.widthEmu, deck.heightEmu, TARGET_WIDTH)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 42f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        var stackY = 48f
        slide.pieces.forEach { piece ->
            val bytes = piece.imagePath?.let { slide.imageBytes[it] }
            val image = bytes?.let { android.graphics.BitmapFactory.decodeByteArray(it, 0, it.size) }
            val hasBox = piece.width > 0f && piece.height > 0f && deck.widthEmu > 0
            if (image != null) {
                val dest = if (hasBox) {
                    RectF(
                        piece.x / deck.widthEmu * width,
                        piece.y / deck.heightEmu * height,
                        (piece.x + piece.width) / deck.widthEmu * width,
                        (piece.y + piece.height) / deck.heightEmu * height
                    )
                } else {
                    val fitted = width * 0.8f
                    val aspect = image.height / image.width.toFloat().coerceAtLeast(1f)
                    RectF(width * 0.1f, stackY, width * 0.1f + fitted, stackY + fitted * aspect)
                }
                canvas.drawBitmap(image, null, dest, null)
                image.recycle()
                if (!hasBox) stackY = dest.bottom + 24f
            }
            if (piece.text.isNotBlank()) {
                val x = if (hasBox) piece.x / deck.widthEmu * width + 8f else 48f
                var y = if (hasBox) piece.y / deck.heightEmu * height + paint.textSize else stackY + paint.textSize
                piece.text.split("\n").forEach { line ->
                    canvas.drawText(line, x, y, paint)
                    y += paint.textSize * 1.35f
                }
                if (!hasBox) stackY = y + 12f
            }
        }
        return bitmap
    }
}
