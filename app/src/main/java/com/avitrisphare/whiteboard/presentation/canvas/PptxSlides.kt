package com.avitrisphare.whiteboard.presentation.canvas

import java.io.File
import java.util.zip.ZipFile
import kotlin.math.max

data class SlidePiece(
    val text: String = "",
    val imagePath: String? = null,
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f
)

data class PptxSlide(
    val pieces: List<SlidePiece>,
    val imageBytes: Map<String, ByteArray>
)

data class PptxDeck(
    val slides: List<PptxSlide>,
    val widthEmu: Long,
    val heightEmu: Long
)

object PptxSlides {
    fun read(file: File): PptxDeck {
        ZipFile(file).use { zip ->
            val presentation = zip.getEntry("ppt/presentation.xml")?.let { entry ->
                zip.getInputStream(entry).bufferedReader().readText()
            }.orEmpty()
            val size = slideSize(presentation)
            val names = zip.entries().toList().map { it.name }
                .filter { it.matches(SLIDE) }
                .sortedBy { slideNumber(it) }
            if (names.isEmpty()) {
                throw IllegalArgumentException("This PowerPoint has no slides. Save it as .pptx and try again.")
            }
            val slides = names.map { name ->
                val xml = zip.getInputStream(zip.getEntry(name)).bufferedReader().readText()
                val relName = name.substringBeforeLast("/") + "/_rels/" + name.substringAfterLast("/") + ".rels"
                val rels = zip.getEntry(relName)?.let { zip.getInputStream(it).bufferedReader().readText() }.orEmpty()
                val images = linkedImages(rels)
                val bytes = HashMap<String, ByteArray>()
                images.values.forEach { path ->
                    val entry = zip.getEntry(path) ?: return@forEach
                    bytes[path] = zip.getInputStream(entry).readBytes()
                }
                PptxSlide(pieces(xml, images), bytes)
            }
            return PptxDeck(slides, size.first, size.second)
        }
    }

    fun slideNumber(path: String): Int {
        return Regex("slide(\\d+)\\.xml").find(path)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }

    fun slideSize(presentationXml: String): Pair<Long, Long> {
        val cx = Regex("cx=\"(\\d+)\"").find(presentationXml)?.groupValues?.get(1)?.toLongOrNull()
        val cy = Regex("cy=\"(\\d+)\"").find(presentationXml)?.groupValues?.get(1)?.toLongOrNull()
        return (cx ?: 12192000L) to (cy ?: 6858000L)
    }

    fun pieces(slideXml: String, images: Map<String, String>): List<SlidePiece> {
        val chunks = Regex("<p:(?:sp|pic)\\b").split(slideXml).drop(1)
        if (chunks.isEmpty()) {
            return texts(slideXml).map { SlidePiece(text = it) }
        }
        return chunks.mapNotNull { chunk ->
            val text = texts(chunk).joinToString("\n").trim()
            val embed = Regex("r:embed=\"(rId\\d+)\"").find(chunk)?.groupValues?.get(1)
            val image = embed?.let { images[it] }
            val off = Regex("<a:off[^>]*x=\"(-?\\d+)\"[^>]*y=\"(-?\\d+)\"").find(chunk)
            val ext = Regex("<a:ext[^>]*cx=\"(\\d+)\"[^>]*cy=\"(\\d+)\"").find(chunk)
            if (text.isBlank() && image == null) return@mapNotNull null
            SlidePiece(
                text = text,
                imagePath = image,
                x = off?.groupValues?.get(1)?.toFloatOrNull() ?: 0f,
                y = off?.groupValues?.get(2)?.toFloatOrNull() ?: 0f,
                width = ext?.groupValues?.get(1)?.toFloatOrNull() ?: 0f,
                height = ext?.groupValues?.get(2)?.toFloatOrNull() ?: 0f
            )
        }
    }

    fun texts(xml: String): List<String> {
        return Regex("<a:t[^>]*>([^<]*)</a:t>").findAll(xml).map { unescape(it.groupValues[1]) }.filter { it.isNotBlank() }.toList()
    }

    fun linkedImages(relsXml: String): Map<String, String> {
        val pattern = Regex("Id=\"(rId\\d+)\"[^>]*Target=\"([^\"]+)\"")
        return pattern.findAll(relsXml).mapNotNull { match ->
            val target = match.groupValues[2]
            if (!target.contains("media/")) return@mapNotNull null
            val path = if (target.startsWith("../")) "ppt/" + target.removePrefix("../") else "ppt/slides/$target"
            match.groupValues[1] to path
        }.toMap()
    }

    fun pixelSize(widthEmu: Long, heightEmu: Long, targetWidth: Int = 1600): Pair<Int, Int> {
        val aspect = if (widthEmu > 0) heightEmu.toFloat() / widthEmu.toFloat() else 9f / 16f
        val height = max(1, (targetWidth * aspect).toInt())
        return targetWidth to height
    }

    private fun unescape(value: String): String {
        return value
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
    }

    private val SLIDE = Regex("ppt/slides/slide\\d+\\.xml")
}
