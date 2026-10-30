package com.avitrisphare.whiteboard.data.local

import androidx.compose.ui.geometry.Offset
import com.avitrisphare.whiteboard.domain.model.Stroke
import com.avitrisphare.whiteboard.domain.model.StrokeType
import java.util.UUID

data class StrokeDto(
    val id: String = "",
    val points: String = "",
    val colorValue: Int = 0xFFFFFFFF.toInt(),
    val strokeWidth: Float = 4f,
    val isEraser: Boolean = false,
    val isHighlighter: Boolean = false,
    val strokeType: StrokeType = StrokeType.PEN,
    val text: String = "",
    val fillColor: Int = 0,
    val imageUri: String = "",
    val opacity: Float = 1f,
    val cornerRadius: Float = 0f,
    val showDimensions: Boolean = false,
    val linkToId: String = "",
    val linkFromId: String = "",
    val stickerKind: String = ""
)

fun StrokeDto.toDomain(): Stroke {
    if (points.length > 100_000) {
        return Stroke(
            id = id.ifEmpty { UUID.randomUUID().toString() },
            points = emptyList(),
            colorValue = colorValue,
            strokeWidth = strokeWidth,
            isEraser = isEraser,
            isHighlighter = isHighlighter,
            strokeType = strokeType,
            text = text,
            fillColor = fillColor,
            imageUri = imageUri,
            opacity = opacity,
            cornerRadius = cornerRadius,
            showDimensions = showDimensions,
            linkToId = linkToId,
            linkFromId = linkFromId,
            stickerKind = stickerKind
        )
    }
    
    val parsed = points.split(";").take(5000).mapNotNull { pair ->
        val coords = pair.split(",")
        if (coords.size >= 2) {
            try {
                val pressure = if (coords.size >= 3) coords[2].toFloat() else 1f
                Offset(coords[0].toFloat(), coords[1].toFloat()) to pressure
            } catch (e: Exception) {
                null
            }
        } else null
    }
    return Stroke(
        id = id.ifEmpty { UUID.randomUUID().toString() },
        points = parsed.map { it.first },
        colorValue = colorValue,
        strokeWidth = strokeWidth,
        isEraser = isEraser,
        isHighlighter = isHighlighter || strokeType == StrokeType.HIGHLIGHTER,
        strokeType = strokeType,
        text = text,
        fillColor = fillColor,
        imageUri = imageUri,
        pointPressures = parsed.map { it.second },
        opacity = opacity,
        cornerRadius = cornerRadius,
        showDimensions = showDimensions,
        linkToId = linkToId,
        linkFromId = linkFromId,
        stickerKind = stickerKind
    )
}

fun Stroke.toDto(): StrokeDto {
    val pointsString = points.mapIndexed { index, point ->
        val pressure = pointPressures.getOrNull(index)
        if (pressure == null || pressure == 1f) {
            "${point.x},${point.y}"
        } else {
            "${point.x},${point.y},$pressure"
        }
    }.joinToString(";")
    return StrokeDto(
        id = id,
        points = pointsString,
        colorValue = colorValue,
        strokeWidth = strokeWidth,
        isEraser = isEraser,
        isHighlighter = isHighlighter,
        strokeType = strokeType,
        text = text,
        fillColor = fillColor,
        imageUri = imageUri,
        opacity = opacity,
        cornerRadius = cornerRadius,
        showDimensions = showDimensions,
        linkToId = linkToId,
        linkFromId = linkFromId,
        stickerKind = stickerKind
    )
}
