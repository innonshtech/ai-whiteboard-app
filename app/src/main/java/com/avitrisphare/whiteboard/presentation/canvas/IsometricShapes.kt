package com.avitrisphare.whiteboard.presentation.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

fun DrawScope.drawHexagon(strokeColor: Color, p1: Offset, p2: Offset, strokeWidth: Float) {
    val left = minOf(p1.x, p2.x)
    val right = maxOf(p1.x, p2.x)
    val top = minOf(p1.y, p2.y)
    val bottom = maxOf(p1.y, p2.y)
    val cx = (left + right) / 2f
    val cy = (top + bottom) / 2f
    val r = min(right - left, bottom - top) / 2f
    val path = Path()
    for (i in 0..6) {
        val angle = i / 6f * (2f * Math.PI.toFloat()) - (Math.PI / 2).toFloat()
        val pt = Offset(cx + r * cos(angle), cy + r * sin(angle))
        if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
    }
    drawPath(path, strokeColor, style = Stroke(width = strokeWidth))
}

fun DrawScope.drawIsometricSolid(
    kind: com.avitrisphare.whiteboard.domain.model.StrokeType,
    color: Color,
    p1: Offset,
    p2: Offset,
    strokeWidth: Float
) {
    val left = minOf(p1.x, p2.x)
    val right = maxOf(p1.x, p2.x)
    val top = minOf(p1.y, p2.y)
    val bottom = maxOf(p1.y, p2.y)
    val w = right - left
    val h = bottom - top
    val path = Path()
    when (kind) {
        com.avitrisphare.whiteboard.domain.model.StrokeType.CUBE -> {
            val dx = w * 0.35f
            val dy = h * 0.35f
            path.moveTo(left, bottom)
            path.lineTo(right, bottom)
            path.lineTo(right, top + dy)
            path.lineTo(right - dx, top)
            path.lineTo(left - dx, top)
            path.lineTo(left, top + dy)
            path.close()
            drawPath(path, color.copy(alpha = 0.25f))
            drawPath(path, color, style = Stroke(width = strokeWidth))
            val topFace = Path().apply {
                moveTo(left - dx, top)
                lineTo(left, top + dy)
                lineTo(right, top + dy)
                lineTo(right - dx, top)
                close()
            }
            drawPath(topFace, color.copy(alpha = 0.15f))
            drawPath(topFace, color, style = Stroke(width = strokeWidth))
        }
        com.avitrisphare.whiteboard.domain.model.StrokeType.CYLINDER -> {
            drawOval(color, Offset(left, top), Size(w, h * 0.35f), style = Stroke(width = strokeWidth))
            drawLine(color, Offset(left, top + h * 0.2f), Offset(left, bottom - h * 0.2f), strokeWidth)
            drawLine(color, Offset(right, top + h * 0.2f), Offset(right, bottom - h * 0.2f), strokeWidth)
            drawOval(color, Offset(left, bottom - h * 0.35f), Size(w, h * 0.35f), style = Stroke(width = strokeWidth))
        }
        com.avitrisphare.whiteboard.domain.model.StrokeType.CONE -> {
            path.moveTo((left + right) / 2f, top)
            path.lineTo(left, bottom)
            path.lineTo(right, bottom)
            path.close()
            drawPath(path, color.copy(alpha = 0.2f))
            drawPath(path, color, style = Stroke(width = strokeWidth))
            drawOval(color, Offset(left, bottom - h * 0.15f), Size(w, h * 0.3f), style = Stroke(width = strokeWidth))
        }
        com.avitrisphare.whiteboard.domain.model.StrokeType.PYRAMID -> {
            path.moveTo((left + right) / 2f, top)
            path.lineTo(left, bottom)
            path.lineTo(right, bottom)
            path.close()
            drawPath(path, color.copy(alpha = 0.2f))
            drawPath(path, color, style = Stroke(width = strokeWidth))
            drawLine(color, Offset((left + right) / 2f, top), Offset(left, bottom), strokeWidth)
            drawLine(color, Offset((left + right) / 2f, top), Offset(right, bottom), strokeWidth)
        }
        else -> Unit
    }
}

fun DrawScope.drawFootballPitch() {
    drawRect(Color(0xFF2E7D32), size = size)
    val line = Color(0xE6FFFFFF)
    val sw = 3f
    drawRect(line, size = size, style = Stroke(width = sw))
    drawLine(line, Offset(size.width / 2f, 0f), Offset(size.width / 2f, size.height), sw)
    drawCircle(line, radius = size.minDimension * 0.12f, center = Offset(size.width / 2f, size.height / 2f), style = Stroke(width = sw))
}
