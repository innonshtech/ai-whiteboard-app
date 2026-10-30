package com.avitrisphare.whiteboard.presentation.canvas

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder
import com.google.zxing.qrcode.encoder.QRCode

/**
 * Encodes a real QR symbol. Phone cameras have to be able to read the panel login code.
 */
object QRCodeHelper {

    fun generateQRCodeBitmap(content: String, size: Int = 256): ImageBitmap {
        val modules = encodeModules(content)
        val count = modules.size
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (y in 0 until size) {
            val row = (y * count / size).coerceIn(0, count - 1)
            for (x in 0 until size) {
                val col = (x * count / size).coerceIn(0, count - 1)
                bitmap.setPixel(x, y, if (modules[row][col]) AndroidColor.BLACK else AndroidColor.WHITE)
            }
        }
        return bitmap.asImageBitmap()
    }

    fun encodeModules(content: String): Array<BooleanArray> {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        val qr: QRCode = Encoder.encode(content, ErrorCorrectionLevel.M, hints)
        val matrix = qr.matrix
        val quiet = 2
        val count = matrix.width + quiet * 2
        return Array(count) { row ->
            BooleanArray(count) { col ->
                val r = row - quiet
                val c = col - quiet
                r in 0 until matrix.width && c in 0 until matrix.width && matrix.get(c, r) == 1.toByte()
            }
        }
    }
}
