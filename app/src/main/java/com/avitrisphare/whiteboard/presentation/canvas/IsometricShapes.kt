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
    val totalW = size.width
    val totalH = size.height
    // 1. Alternating grass stripes across the pitch
    val stripeCount = 10
    val stripeW = totalW / stripeCount
    val grassDark = Color(0xFF276A2B)
    val grassLight = Color(0xFF2E7D32)
    for (i in 0 until stripeCount) {
        val color = if (i % 2 == 0) grassLight else grassDark
        drawRect(color, topLeft = Offset(i * stripeW, 0f), size = Size(stripeW, totalH))
    }

    // 2. Pitch boundary line with neat margin
    val marginX = (totalW * 0.03f).coerceAtLeast(16f)
    val marginY = (totalH * 0.03f).coerceAtLeast(16f)
    val pitchW = totalW - 2 * marginX
    val pitchH = totalH - 2 * marginY
    val line = Color(0xF2FFFFFF)
    val sw = 2.5f

    // Outer touchline
    drawRect(line, topLeft = Offset(marginX, marginY), size = Size(pitchW, pitchH), style = Stroke(width = sw))

    // Halfway line & center circle
    val midX = marginX + pitchW / 2f
    val midY = marginY + pitchH / 2f
    drawLine(line, Offset(midX, marginY), Offset(midX, marginY + pitchH), sw)
    val centerR = (pitchH * 0.18f).coerceAtLeast(30f)
    drawCircle(line, radius = centerR, center = Offset(midX, midY), style = Stroke(width = sw))
    drawCircle(line, radius = 4f, center = Offset(midX, midY))

    // Penalty areas (18-yard box)
    val penaltyW = pitchW * 0.16f
    val penaltyH = pitchH * 0.44f
    val penaltyTop = midY - penaltyH / 2f
    drawRect(line, topLeft = Offset(marginX, penaltyTop), size = Size(penaltyW, penaltyH), style = Stroke(width = sw))
    drawRect(line, topLeft = Offset(marginX + pitchW - penaltyW, penaltyTop), size = Size(penaltyW, penaltyH), style = Stroke(width = sw))

    // Goal areas (6-yard box)
    val goalW = pitchW * 0.06f
    val goalH = pitchH * 0.22f
    val goalTop = midY - goalH / 2f
    drawRect(line, topLeft = Offset(marginX, goalTop), size = Size(goalW, goalH), style = Stroke(width = sw))
    drawRect(line, topLeft = Offset(marginX + pitchW - goalW, goalTop), size = Size(goalW, goalH), style = Stroke(width = sw))

    // Penalty spots
    val spotDist = pitchW * 0.11f
    drawCircle(line, radius = 3.5f, center = Offset(marginX + spotDist, midY))
    drawCircle(line, radius = 3.5f, center = Offset(marginX + pitchW - spotDist, midY))

    // Corner arcs
    val cornerR = (pitchH * 0.03f).coerceAtLeast(10f)
    drawArc(line, startAngle = 0f, sweepAngle = 90f, useCenter = false, topLeft = Offset(marginX - cornerR, marginY - cornerR), size = Size(cornerR * 2, cornerR * 2), style = Stroke(width = sw))
    drawArc(line, startAngle = 270f, sweepAngle = 90f, useCenter = false, topLeft = Offset(marginX - cornerR, marginY + pitchH - cornerR), size = Size(cornerR * 2, cornerR * 2), style = Stroke(width = sw))
    drawArc(line, startAngle = 90f, sweepAngle = 90f, useCenter = false, topLeft = Offset(marginX + pitchW - cornerR, marginY - cornerR), size = Size(cornerR * 2, cornerR * 2), style = Stroke(width = sw))
    drawArc(line, startAngle = 180f, sweepAngle = 90f, useCenter = false, topLeft = Offset(marginX + pitchW - cornerR, marginY + pitchH - cornerR), size = Size(cornerR * 2, cornerR * 2), style = Stroke(width = sw))
}
