package com.vibenote.app.data.local

import androidx.compose.ui.geometry.Offset
import com.vibenote.app.domain.model.Stroke
import com.vibenote.app.domain.model.StrokeType
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
    val imageUri: String = ""
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
            imageUri = imageUri
        )
    }
    
    val pointsList = points.split(";").take(5000).mapNotNull { pair ->
        val coords = pair.split(",")
        if (coords.size == 2) {
            try {
                Offset(coords[0].toFloat(), coords[1].toFloat())
            } catch (e: Exception) {
                null
            }
        } else null
    }
    return Stroke(
        id = id.ifEmpty { UUID.randomUUID().toString() },
        points = pointsList,
        colorValue = colorValue,
        strokeWidth = strokeWidth,
        isEraser = isEraser,
        isHighlighter = isHighlighter,
        strokeType = strokeType,
        text = text,
        fillColor = fillColor,
        imageUri = imageUri
    )
}

fun Stroke.toDto(): StrokeDto {
    val pointsString = points.joinToString(";") { "${it.x},${it.y}" }
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
        imageUri = imageUri
    )
}
