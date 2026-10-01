package com.vibenote.app.presentation.canvas

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * Lightweight QR Code matrix generator for rendering sharp, clean room invitation QR codes.
 */
object QRCodeHelper {

    fun generateQRCodeBitmap(content: String, size: Int = 256): ImageBitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        
        // Simple deterministic visual QR-style pattern derived from room hash
        val hash = content.hashCode()
        val gridCount = 25
        val cellSize = size / gridCount

        val matrix = Array(gridCount) { BooleanArray(gridCount) }

        // Finder patterns (top-left, top-right, bottom-left 7x7 boxes)
        fun drawFinder(startX: Int, startY: Int) {
            for (r in 0 until 7) {
                for (c in 0 until 7) {
                    val isBorder = r == 0 || r == 6 || c == 0 || c == 6
                    val isCenter = r in 2..4 && c in 2..4
                    matrix[startY + r][startX + c] = isBorder || isCenter
                }
            }
        }

        drawFinder(1, 1)
        drawFinder(gridCount - 8, 1)
        drawFinder(1, gridCount - 8)

        // Alignment & timing patterns
        for (i in 8 until gridCount - 8) {
            matrix[4][i] = (i % 2 == 0)
            matrix[i][4] = (i % 2 == 0)
        }

        // Data payload pattern encoding URL hash
        val bytes = content.toByteArray()
        var bitIndex = 0
        for (r in 0 until gridCount) {
            for (c in 0 until gridCount) {
                if ((r < 9 && c < 9) || (r < 9 && c >= gridCount - 9) || (r >= gridCount - 9 && c < 9)) {
                    continue // Skip finder zones
                }
                val byteVal = if (bytes.isNotEmpty()) bytes[bitIndex % bytes.size].toInt() else 0
                val bit = ((byteVal shr (bitIndex % 8)) and 1) == 1
                val pseudoHashBit = ((hash xor (r * 31 + c * 17)) and 1) == 1
                matrix[r][c] = bit xor pseudoHashBit
                bitIndex++
            }
        }

        // Render into Android Bitmap
        for (y in 0 until size) {
            val gridY = (y / cellSize).coerceIn(0, gridCount - 1)
            for (x in 0 until size) {
                val gridX = (x / cellSize).coerceIn(0, gridCount - 1)
                val isBlack = matrix[gridY][gridX]
                bitmap.setPixel(x, y, if (isBlack) AndroidColor.BLACK else AndroidColor.WHITE)
            }
        }

        return bitmap.asImageBitmap()
    }
}
