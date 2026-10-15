package com.avitrisphare.whiteboard.presentation.canvas

import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import org.junit.Assert.assertEquals
import org.junit.Test

class QRCodeHelperTest {
    @Test
    fun encodedLoginCodeCanBeReadBack() {
        val payload = "gyoash://qr-login?nonce=1c18d236-bf99-4242-8b33-482ef4becafd"
        val modules = QRCodeHelper.encodeModules(payload)
        val count = modules.size
        val scale = 8
        val size = count * scale
        val pixels = IntArray(size * size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                val dark = modules[y / scale][x / scale]
                pixels[y * size + x] = if (dark) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
            }
        }
        val bitmap = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(size, size, pixels)))
        val text = MultiFormatReader().decode(bitmap).text
        assertEquals(payload, text)
    }
}
