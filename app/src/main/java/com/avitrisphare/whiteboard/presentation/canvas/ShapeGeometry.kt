package com.avitrisphare.whiteboard.presentation.canvas

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.graphics.nativeCanvas
import com.avitrisphare.whiteboard.domain.model.Stroke
import com.avitrisphare.whiteboard.domain.model.StrokeType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

enum class ShapeCategory(val label: String) {
    SHAPES_2D("2D Shapes"),
    LINES_RAYS("Lines & Rays"),
    SOLIDS_3D("3D Solids")
}

enum class ShapeStyleMode {
    BORDER,
    FILL
}

data class ShapeSpec(
    val strokeType: StrokeType,
    val name: String,
    val category: ShapeCategory,
    val supportsRadius: Boolean = false,
    val supportsFill: Boolean = true
)

val ALL_SHAPES: List<ShapeSpec> = listOf(
    // 2D Shapes
    ShapeSpec(StrokeType.RECTANGLE, "Rectangle", ShapeCategory.SHAPES_2D),
    ShapeSpec(StrokeType.ROUNDED_RECTANGLE, "Rounded Rect", ShapeCategory.SHAPES_2D, supportsRadius = true),
    ShapeSpec(StrokeType.CIRCLE, "Circle", ShapeCategory.SHAPES_2D),
    ShapeSpec(StrokeType.ELLIPSE, "Ellipse", ShapeCategory.SHAPES_2D),
    ShapeSpec(StrokeType.SEMICIRCLE, "Semicircle", ShapeCategory.SHAPES_2D),
    ShapeSpec(StrokeType.RIGHT_TRIANGLE, "Right Triangle", ShapeCategory.SHAPES_2D),
    ShapeSpec(StrokeType.TRIANGLE, "Triangle", ShapeCategory.SHAPES_2D),
    ShapeSpec(StrokeType.SCALENE_TRIANGLE, "Scalene", ShapeCategory.SHAPES_2D),
    ShapeSpec(StrokeType.TRAPEZOID, "Trapezoid", ShapeCategory.SHAPES_2D),
    ShapeSpec(StrokeType.STAR, "Star", ShapeCategory.SHAPES_2D),
    ShapeSpec(StrokeType.PARALLELOGRAM, "Parallelogram", ShapeCategory.SHAPES_2D),
    ShapeSpec(StrokeType.RHOMBUS, "Rhombus", ShapeCategory.SHAPES_2D),
    ShapeSpec(StrokeType.PENTAGON, "Pentagon", ShapeCategory.SHAPES_2D),
    ShapeSpec(StrokeType.HEXAGON, "Hexagon", ShapeCategory.SHAPES_2D),
    ShapeSpec(StrokeType.OCTAGON, "Octagon", ShapeCategory.SHAPES_2D),

    // Lines & Rays
    ShapeSpec(StrokeType.LINE, "Line", ShapeCategory.LINES_RAYS, supportsFill = false),
    ShapeSpec(StrokeType.LINE_DASHED, "Dashed", ShapeCategory.LINES_RAYS, supportsFill = false),
    ShapeSpec(StrokeType.ARROW, "Arrow", ShapeCategory.LINES_RAYS, supportsFill = false),
    ShapeSpec(StrokeType.DOUBLE_ARROW, "Double Arrow", ShapeCategory.LINES_RAYS, supportsFill = false),
    ShapeSpec(StrokeType.DASHED_ARROW, "Dashed Arrow", ShapeCategory.LINES_RAYS, supportsFill = false),
    ShapeSpec(StrokeType.ANGLE_RAY, "Angle Ray", ShapeCategory.LINES_RAYS, supportsFill = false),

    // 3D Solids
    ShapeSpec(StrokeType.CUBE, "Cube", ShapeCategory.SOLIDS_3D),
    ShapeSpec(StrokeType.CUBOID, "Cuboid", ShapeCategory.SOLIDS_3D),
    ShapeSpec(StrokeType.PYRAMID, "Pyramid", ShapeCategory.SOLIDS_3D),
    ShapeSpec(StrokeType.PRISM, "Prism", ShapeCategory.SOLIDS_3D),
    ShapeSpec(StrokeType.CYLINDER, "Cylinder", ShapeCategory.SOLIDS_3D),
    ShapeSpec(StrokeType.CONE, "Cone", ShapeCategory.SOLIDS_3D),
    ShapeSpec(StrokeType.SPHERE, "Sphere", ShapeCategory.SOLIDS_3D)
)

/** Returns the bounding box (left, top, right, bottom, width, height) of two drag points */
fun shapeBoundingBox(points: List<Offset>): Rect {
    if (points.isEmpty()) return Rect.Zero
    val p1 = points.first()
    val p2 = points.last()
    return Rect(minOf(p1.x, p2.x), minOf(p1.y, p2.y), maxOf(p1.x, p2.x), maxOf(p1.y, p2.y))
}

/** Builds regular polygon paths (pentagon, hexagon, octagon) */
fun buildRegularPolygonPath(box: Rect, sides: Int, rotationOffset: Double = -PI / 2.0): Path {
    val path = Path()
    val cx = box.center.x
    val cy = box.center.y
    val rx = box.width / 2f
    val ry = box.height / 2f
    val angleStep = 2.0 * PI / sides

    for (i in 0 until sides) {
        val angle = rotationOffset + i * angleStep
        val x = (cx + rx * cos(angle)).toFloat()
        val y = (cy + ry * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

/** Builds 5-point star path */
fun buildStarPath(box: Rect): Path {
    val path = Path()
    val cx = box.center.x
    val cy = box.center.y
    val rOuterX = box.width / 2f
    val rOuterY = box.height / 2f
    val rInnerX = rOuterX * 0.42f
    val rInnerY = rOuterY * 0.42f

    val angleStep = PI / 5.0
    for (i in 0 until 10) {
        val angle = -PI / 2.0 + i * angleStep
        val rx = if (i % 2 == 0) rOuterX else rInnerX
        val ry = if (i % 2 == 0) rOuterY else rInnerY
        val x = (cx + rx * cos(angle)).toFloat()
        val y = (cy + ry * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

/** Builds trapezoid path */
fun buildTrapezoidPath(box: Rect): Path {
    val path = Path()
    val insetX = box.width * 0.22f
    path.moveTo(box.left + insetX, box.top)
    path.lineTo(box.right - insetX, box.top)
    path.lineTo(box.right, box.bottom)
    path.lineTo(box.left, box.bottom)
    path.close()
    return path
}

/** Builds parallelogram path */
fun buildParallelogramPath(box: Rect): Path {
    val path = Path()
    val slant = box.width * 0.25f
    path.moveTo(box.left + slant, box.top)
    path.lineTo(box.right, box.top)
    path.lineTo(box.right - slant, box.bottom)
    path.lineTo(box.left, box.bottom)
    path.close()
    return path
}

/** Builds rhombus path */
fun buildRhombusPath(box: Rect): Path {
    val path = Path()
    path.moveTo(box.center.x, box.top)
    path.lineTo(box.right, box.center.y)
    path.lineTo(box.center.x, box.bottom)
    path.lineTo(box.left, box.center.y)
    path.close()
    return path
}

/** Builds right-angled triangle path */
fun buildRightTrianglePath(box: Rect): Path {
    val path = Path()
    path.moveTo(box.left, box.bottom)
    path.lineTo(box.left, box.top)
    path.lineTo(box.right, box.bottom)
    path.close()
    return path
}

/** Builds scalene triangle path */
fun buildScaleneTrianglePath(box: Rect): Path {
    val path = Path()
    path.moveTo(box.left, box.bottom)
    path.lineTo(box.left + box.width * 0.35f, box.top)
    path.lineTo(box.right, box.bottom)
    path.close()
    return path
}

/** Builds semicircle path */
fun buildSemicirclePath(box: Rect): Path {
    val path = Path()
    val minHeight = (box.width * 0.5f).coerceAtLeast(8f)
    val effectiveHeight = if (box.height < 4f) minHeight else box.height
    val top = box.bottom - effectiveHeight
    val fullOval = Rect(box.left, top, box.right, box.bottom + effectiveHeight)
    path.arcTo(fullOval, 180f, 180f, false)
    path.lineTo(box.left, box.bottom)
    path.close()
    return path
}

/** Builds angle ray path */
fun buildAngleRayPath(p1: Offset, p2: Offset): Path {
    val path = Path()
    val corner = Offset(p1.x, p2.y)
    // Ray 1: corner up to p1.y
    path.moveTo(corner.x, p1.y)
    path.lineTo(corner.x, corner.y)
    // Ray 2: corner across to p2.x
    path.lineTo(p2.x, corner.y)
    return path
}

/**
 * Main renderer for all extended 2D, line, and 3D shapes.
 * Returns true if handled, false to fall back to legacy drawing.
 */
fun DrawScope.renderExtendedShape(
    stroke: Stroke,
    currentScale: Float,
    strokeColor: Color,
    strokeWidth: Float
): Boolean {
    val points = stroke.points
    if (points.size < 2) return false

    val safeScale = if (currentScale <= 0f) 1f else currentScale
    val box = shapeBoundingBox(points)
    val opacity = stroke.opacity.coerceIn(0.05f, 1f)
    val effectiveStrokeColor = strokeColor.copy(alpha = strokeColor.alpha * opacity)
    val effectiveFillColor = if (stroke.fillColor != 0) {
        val fc = Color(stroke.fillColor)
        fc.copy(alpha = fc.alpha * opacity)
    } else Color.Transparent

    val hasFill = effectiveFillColor != Color.Transparent
    val solidStroke = DrawStroke(
        width = strokeWidth,
        cap = StrokeCap.Round,
        join = StrokeJoin.Round
    )
    val dashedStroke = DrawStroke(
        width = strokeWidth,
        cap = StrokeCap.Round,
        join = StrokeJoin.Round,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f / safeScale, 8f / safeScale))
    )

    when (stroke.strokeType) {
        StrokeType.ROUNDED_RECTANGLE -> {
            val r = (if (stroke.cornerRadius > 0f) stroke.cornerRadius else 24f) / safeScale
            if (hasFill) {
                drawRoundRect(
                    color = effectiveFillColor,
                    topLeft = box.topLeft,
                    size = box.size,
                    cornerRadius = CornerRadius(r, r)
                )
            }
            drawRoundRect(
                color = effectiveStrokeColor,
                topLeft = box.topLeft,
                size = box.size,
                cornerRadius = CornerRadius(r, r),
                style = solidStroke
            )
        }
        StrokeType.SEMICIRCLE -> {
            val path = buildSemicirclePath(box)
            if (hasFill) drawPath(path, effectiveFillColor)
            drawPath(path, effectiveStrokeColor, style = solidStroke)
        }
        StrokeType.RIGHT_TRIANGLE -> {
            val path = buildRightTrianglePath(box)
            if (hasFill) drawPath(path, effectiveFillColor)
            drawPath(path, effectiveStrokeColor, style = solidStroke)
        }
        StrokeType.SCALENE_TRIANGLE -> {
            val path = buildScaleneTrianglePath(box)
            if (hasFill) drawPath(path, effectiveFillColor)
            drawPath(path, effectiveStrokeColor, style = solidStroke)
        }
        StrokeType.TRAPEZOID -> {
            val path = buildTrapezoidPath(box)
            if (hasFill) drawPath(path, effectiveFillColor)
            drawPath(path, effectiveStrokeColor, style = solidStroke)
        }
        StrokeType.PARALLELOGRAM -> {
            val path = buildParallelogramPath(box)
            if (hasFill) drawPath(path, effectiveFillColor)
            drawPath(path, effectiveStrokeColor, style = solidStroke)
        }
        StrokeType.RHOMBUS -> {
            val path = buildRhombusPath(box)
            if (hasFill) drawPath(path, effectiveFillColor)
            drawPath(path, effectiveStrokeColor, style = solidStroke)
        }
        StrokeType.PENTAGON -> {
            val path = buildRegularPolygonPath(box, 5)
            if (hasFill) drawPath(path, effectiveFillColor)
            drawPath(path, effectiveStrokeColor, style = solidStroke)
        }
        StrokeType.HEXAGON -> {
            val path = buildRegularPolygonPath(box, 6)
            if (hasFill) drawPath(path, effectiveFillColor)
            drawPath(path, effectiveStrokeColor, style = solidStroke)
        }
        StrokeType.OCTAGON -> {
            val path = buildRegularPolygonPath(box, 8, rotationOffset = -PI / 8.0)
            if (hasFill) drawPath(path, effectiveFillColor)
            drawPath(path, effectiveStrokeColor, style = solidStroke)
        }
        StrokeType.STAR -> {
            val star = regularStarPath(points.first(), points.last())
            if (hasFill) drawPath(star, effectiveFillColor)
            drawPath(star, effectiveStrokeColor, style = solidStroke)
        }

        // --- Lines & Rays ---
        StrokeType.LINE_DASHED -> {
            drawLine(
                color = effectiveStrokeColor,
                start = points.first(),
                end = points.last(),
                strokeWidth = strokeWidth,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f / safeScale, 8f / safeScale))
            )
        }
        StrokeType.DOUBLE_ARROW -> {
            val start = points.first()
            val end = points.last()
            drawLine(color = effectiveStrokeColor, start = start, end = end, strokeWidth = strokeWidth)
            drawArrowHead(end, start, effectiveStrokeColor, strokeWidth, safeScale)
            drawArrowHead(start, end, effectiveStrokeColor, strokeWidth, safeScale)
        }
        StrokeType.DASHED_ARROW -> {
            val start = points.first()
            val end = points.last()
            drawLine(
                color = effectiveStrokeColor,
                start = start,
                end = end,
                strokeWidth = strokeWidth,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f / safeScale, 8f / safeScale))
            )
            drawArrowHead(end, start, effectiveStrokeColor, strokeWidth, safeScale)
        }
        StrokeType.ANGLE_RAY -> {
            val start = points.first()
            val end = points.last()
            val corner = Offset(start.x, end.y)
            drawLine(effectiveStrokeColor, corner, Offset(start.x, start.y), strokeWidth = strokeWidth)
            drawLine(effectiveStrokeColor, corner, Offset(end.x, end.y), strokeWidth = strokeWidth)
            // Draw small angle arc or right angle marker
            val sqSize = (20f / safeScale).coerceAtLeast(10f)
            val sqPath = Path().apply {
                moveTo(corner.x, corner.y - sqSize)
                lineTo(corner.x + sqSize, corner.y - sqSize)
                lineTo(corner.x + sqSize, corner.y)
            }
            drawPath(sqPath, effectiveStrokeColor, style = DrawStroke(width = (strokeWidth * 0.75f).coerceAtLeast(1f)))
        }

        // --- 3D Solids ---
        StrokeType.CUBE, StrokeType.CUBOID -> {
            render3DCuboid(box, effectiveStrokeColor, effectiveFillColor, solidStroke, dashedStroke, isCube = stroke.strokeType == StrokeType.CUBE)
        }
        StrokeType.PYRAMID -> {
            render3DPyramid(box, effectiveStrokeColor, effectiveFillColor, solidStroke, dashedStroke)
        }
        StrokeType.PRISM -> {
            render3DPrism(box, effectiveStrokeColor, effectiveFillColor, solidStroke, dashedStroke)
        }
        StrokeType.CYLINDER -> {
            render3DCylinder(box, effectiveStrokeColor, effectiveFillColor, solidStroke, dashedStroke)
        }
        StrokeType.CONE -> {
            render3DCone(box, effectiveStrokeColor, effectiveFillColor, solidStroke, dashedStroke)
        }
        StrokeType.SPHERE -> {
            render3DSphere(box, effectiveStrokeColor, effectiveFillColor, solidStroke, dashedStroke)
        }

        else -> return false
    }

    if (stroke.showDimensions) {
        renderShapeDimensions(box, safeScale)
    }

    return true
}

/** Draws arrow head at tip pointing away from origin */
fun DrawScope.drawArrowHead(
    tip: Offset,
    origin: Offset,
    color: Color,
    strokeWidth: Float,
    safeScale: Float
) {
    val angle = atan2((tip.y - origin.y).toDouble(), (tip.x - origin.x).toDouble())
    val headLength = (22f / safeScale).coerceAtLeast(12f)
    val headAngle = PI / 6.0

    val p1 = Offset(
        (tip.x - headLength * cos(angle - headAngle)).toFloat(),
        (tip.y - headLength * sin(angle - headAngle)).toFloat()
    )
    val p2 = Offset(
        (tip.x - headLength * cos(angle + headAngle)).toFloat(),
        (tip.y - headLength * sin(angle + headAngle)).toFloat()
    )
    val path = Path().apply {
        moveTo(p1.x, p1.y)
        lineTo(tip.x, tip.y)
        lineTo(p2.x, p2.y)
    }
    drawPath(path, color, style = DrawStroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** 3D Cuboid / Cube isometric rendering */
fun DrawScope.render3DCuboid(
    box: Rect,
    strokeColor: Color,
    fillColor: Color,
    solidStroke: DrawStroke,
    dashedStroke: DrawStroke,
    isCube: Boolean
) {
    val depthX = box.width * 0.25f
    val depthY = box.height * 0.22f

    val frontLeft = box.left
    val frontRight = box.right - depthX
    val frontTop = box.top + depthY
    val frontBottom = box.bottom

    val backLeft = box.left + depthX
    val backRight = box.right
    val backTop = box.top
    val backBottom = box.bottom - depthY

    // Shaded faces if fill active
    if (fillColor != Color.Transparent) {
        val topFace = Path().apply {
            moveTo(frontLeft, frontTop)
            lineTo(backLeft, backTop)
            lineTo(backRight, backTop)
            lineTo(frontRight, frontTop)
            close()
        }
        drawPath(topFace, fillColor.copy(alpha = (fillColor.alpha * 0.8f).coerceIn(0f, 1f)))

        val sideFace = Path().apply {
            moveTo(frontRight, frontTop)
            lineTo(backRight, backTop)
            lineTo(backRight, backBottom)
            lineTo(frontRight, frontBottom)
            close()
        }
        drawPath(sideFace, fillColor.copy(alpha = (fillColor.alpha * 0.65f).coerceIn(0f, 1f)))

        drawRect(fillColor, Offset(frontLeft, frontTop), Size(frontRight - frontLeft, frontBottom - frontTop))
    }

    // Hidden back edges (dashed)
    drawLine(strokeColor.copy(alpha = 0.5f), Offset(frontLeft, frontBottom), Offset(backLeft, backBottom), strokeWidth = solidStroke.width, pathEffect = dashedStroke.pathEffect)
    drawLine(strokeColor.copy(alpha = 0.5f), Offset(backLeft, backBottom), Offset(backRight, backBottom), strokeWidth = solidStroke.width, pathEffect = dashedStroke.pathEffect)
    drawLine(strokeColor.copy(alpha = 0.5f), Offset(backLeft, backTop), Offset(backLeft, backBottom), strokeWidth = solidStroke.width, pathEffect = dashedStroke.pathEffect)

    // Visible front face
    drawRect(strokeColor, Offset(frontLeft, frontTop), Size(frontRight - frontLeft, frontBottom - frontTop), style = solidStroke)

    // Visible connecting edges
    drawLine(strokeColor, Offset(frontLeft, frontTop), Offset(backLeft, backTop), strokeWidth = solidStroke.width)
    drawLine(strokeColor, Offset(frontRight, frontTop), Offset(backRight, backTop), strokeWidth = solidStroke.width)
    drawLine(strokeColor, Offset(frontRight, frontBottom), Offset(backRight, backBottom), strokeWidth = solidStroke.width)

    // Visible top & right back edges
    drawLine(strokeColor, Offset(backLeft, backTop), Offset(backRight, backTop), strokeWidth = solidStroke.width)
    drawLine(strokeColor, Offset(backRight, backTop), Offset(backRight, backBottom), strokeWidth = solidStroke.width)
}

/** 3D Pyramid rendering */
fun DrawScope.render3DPyramid(
    box: Rect,
    strokeColor: Color,
    fillColor: Color,
    solidStroke: DrawStroke,
    dashedStroke: DrawStroke
) {
    val apex = Offset(box.center.x, box.top)
    val bLeft = Offset(box.left, box.bottom - box.height * 0.15f)
    val bCenter = Offset(box.center.x, box.bottom)
    val bRight = Offset(box.right, box.bottom - box.height * 0.15f)
    val bBack = Offset(box.center.x, box.bottom - box.height * 0.30f)

    if (fillColor != Color.Transparent) {
        val leftFace = Path().apply {
            moveTo(apex.x, apex.y)
            lineTo(bLeft.x, bLeft.y)
            lineTo(bCenter.x, bCenter.y)
            close()
        }
        drawPath(leftFace, fillColor)

        val rightFace = Path().apply {
            moveTo(apex.x, apex.y)
            lineTo(bCenter.x, bCenter.y)
            lineTo(bRight.x, bRight.y)
            close()
        }
        drawPath(rightFace, fillColor.copy(alpha = (fillColor.alpha * 0.75f).coerceIn(0f, 1f)))
    }

    // Hidden base & edges
    drawLine(strokeColor.copy(alpha = 0.5f), bLeft, bBack, strokeWidth = solidStroke.width, pathEffect = dashedStroke.pathEffect)
    drawLine(strokeColor.copy(alpha = 0.5f), bBack, bRight, strokeWidth = solidStroke.width, pathEffect = dashedStroke.pathEffect)
    drawLine(strokeColor.copy(alpha = 0.5f), apex, bBack, strokeWidth = solidStroke.width, pathEffect = dashedStroke.pathEffect)

    // Visible base & front edges
    drawLine(strokeColor, bLeft, bCenter, strokeWidth = solidStroke.width)
    drawLine(strokeColor, bCenter, bRight, strokeWidth = solidStroke.width)
    drawLine(strokeColor, apex, bLeft, strokeWidth = solidStroke.width)
    drawLine(strokeColor, apex, bCenter, strokeWidth = solidStroke.width)
    drawLine(strokeColor, apex, bRight, strokeWidth = solidStroke.width)
}

/** 3D Prism rendering */
fun DrawScope.render3DPrism(
    box: Rect,
    strokeColor: Color,
    fillColor: Color,
    solidStroke: DrawStroke,
    dashedStroke: DrawStroke
) {
    val depthX = box.width * 0.28f
    val depthY = box.height * 0.18f

    val fApex = Offset(box.left + (box.width - depthX) / 2f, box.top + depthY)
    val fLeft = Offset(box.left, box.bottom)
    val fRight = Offset(box.right - depthX, box.bottom)

    val bApex = Offset(fApex.x + depthX, fApex.y - depthY)
    val bLeft = Offset(fLeft.x + depthX, fLeft.y - depthY)
    val bRight = Offset(fRight.x + depthX, fRight.y - depthY)

    if (fillColor != Color.Transparent) {
        val topFace = Path().apply {
            moveTo(fApex.x, fApex.y)
            lineTo(bApex.x, bApex.y)
            lineTo(bRight.x, bRight.y)
            lineTo(fRight.x, fRight.y)
            close()
        }
        drawPath(topFace, fillColor.copy(alpha = (fillColor.alpha * 0.75f).coerceIn(0f, 1f)))

        val frontFace = Path().apply {
            moveTo(fApex.x, fApex.y)
            lineTo(fLeft.x, fLeft.y)
            lineTo(fRight.x, fRight.y)
            close()
        }
        drawPath(frontFace, fillColor)
    }

    // Hidden back edges
    drawLine(strokeColor.copy(alpha = 0.5f), fLeft, bLeft, strokeWidth = solidStroke.width, pathEffect = dashedStroke.pathEffect)
    drawLine(strokeColor.copy(alpha = 0.5f), bLeft, bRight, strokeWidth = solidStroke.width, pathEffect = dashedStroke.pathEffect)
    drawLine(strokeColor.copy(alpha = 0.5f), bApex, bLeft, strokeWidth = solidStroke.width, pathEffect = dashedStroke.pathEffect)

    // Front triangle
    val frontTri = Path().apply {
        moveTo(fApex.x, fApex.y)
        lineTo(fLeft.x, fLeft.y)
        lineTo(fRight.x, fRight.y)
        close()
    }
    drawPath(frontTri, strokeColor, style = solidStroke)

    // Connecting top & right
    drawLine(strokeColor, fApex, bApex, strokeWidth = solidStroke.width)
    drawLine(strokeColor, fRight, bRight, strokeWidth = solidStroke.width)
    drawLine(strokeColor, bApex, bRight, strokeWidth = solidStroke.width)
}

/** 3D Cylinder rendering */
fun DrawScope.render3DCylinder(
    box: Rect,
    strokeColor: Color,
    fillColor: Color,
    solidStroke: DrawStroke,
    dashedStroke: DrawStroke
) {
    val ry = (box.height * 0.15f).coerceAtLeast(8f)
    val topBounds = Rect(box.left, box.top, box.right, box.top + ry * 2f)
    val bottomBounds = Rect(box.left, box.bottom - ry * 2f, box.right, box.bottom)

    if (fillColor != Color.Transparent) {
        val body = Path().apply {
            moveTo(box.left, box.top + ry)
            lineTo(box.left, box.bottom - ry)
            arcTo(bottomBounds, 180f, -180f, false)
            lineTo(box.right, box.top + ry)
            close()
        }
        drawPath(body, fillColor)
        drawOval(fillColor.copy(alpha = (fillColor.alpha * 1.15f).coerceIn(0f, 1f)), topLeft = topBounds.topLeft, size = topBounds.size)
    }

    // Top full ellipse
    drawOval(strokeColor, topLeft = topBounds.topLeft, size = topBounds.size, style = solidStroke)

    // Side tangent lines
    drawLine(strokeColor, Offset(box.left, box.top + ry), Offset(box.left, box.bottom - ry), strokeWidth = solidStroke.width)
    drawLine(strokeColor, Offset(box.right, box.top + ry), Offset(box.right, box.bottom - ry), strokeWidth = solidStroke.width)

    // Bottom front arc
    val frontArc = Path().apply {
        arcTo(bottomBounds, 0f, 180f, false)
    }
    drawPath(frontArc, strokeColor, style = solidStroke)

    // Bottom back arc (dashed)
    val backArc = Path().apply {
        arcTo(bottomBounds, 180f, 180f, false)
    }
    drawPath(backArc, strokeColor.copy(alpha = 0.5f), style = dashedStroke)
}

/** 3D Cone rendering */
fun DrawScope.render3DCone(
    box: Rect,
    strokeColor: Color,
    fillColor: Color,
    solidStroke: DrawStroke,
    dashedStroke: DrawStroke
) {
    val apex = Offset(box.center.x, box.top)
    val ry = (box.height * 0.15f).coerceAtLeast(8f)
    val bottomBounds = Rect(box.left, box.bottom - ry * 2f, box.right, box.bottom)

    if (fillColor != Color.Transparent) {
        val coneBody = Path().apply {
            moveTo(apex.x, apex.y)
            lineTo(box.left, box.bottom - ry)
            arcTo(bottomBounds, 180f, -180f, false)
            close()
        }
        drawPath(coneBody, fillColor)
    }

    // Side generator lines
    drawLine(strokeColor, apex, Offset(box.left, box.bottom - ry), strokeWidth = solidStroke.width)
    drawLine(strokeColor, apex, Offset(box.right, box.bottom - ry), strokeWidth = solidStroke.width)

    // Bottom front arc
    val frontArc = Path().apply {
        arcTo(bottomBounds, 0f, 180f, false)
    }
    drawPath(frontArc, strokeColor, style = solidStroke)

    // Bottom back arc (dashed)
    val backArc = Path().apply {
        arcTo(bottomBounds, 180f, 180f, false)
    }
    drawPath(backArc, strokeColor.copy(alpha = 0.5f), style = dashedStroke)
}

/** 3D Sphere rendering */
fun DrawScope.render3DSphere(
    box: Rect,
    strokeColor: Color,
    fillColor: Color,
    solidStroke: DrawStroke,
    dashedStroke: DrawStroke
) {
    if (fillColor != Color.Transparent) {
        drawOval(fillColor, topLeft = box.topLeft, size = box.size)
    }

    // Outer circle
    drawOval(strokeColor, topLeft = box.topLeft, size = box.size, style = solidStroke)

    // Equator contour ellipse
    val ry = box.height * 0.22f
    val equatorBounds = Rect(box.left, box.center.y - ry, box.right, box.center.y + ry)

    // Front equator arc
    val frontEquator = Path().apply {
        arcTo(equatorBounds, 0f, 180f, false)
    }
    drawPath(frontEquator, strokeColor, style = solidStroke)

    // Back equator arc (dashed)
    val backEquator = Path().apply {
        arcTo(equatorBounds, 180f, 180f, false)
    }
    drawPath(backEquator, strokeColor.copy(alpha = 0.5f), style = dashedStroke)
}

/** Renders dimension labels along the shape bounds */
private fun DrawScope.renderShapeDimensions(box: Rect, safeScale: Float) {
    val paint = Paint().apply {
        color = 0xFF3B82F6.toInt()
        textSize = (13f / safeScale).coerceIn(11f, 22f)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }
    val wPx = box.width.roundToInt()
    val hPx = box.height.roundToInt()

    drawContext.canvas.nativeCanvas.apply {
        // Top width label
        drawText("${wPx}px", box.center.x - 20f, box.top - 8f, paint)
        // Right height label
        drawText("${hPx}px", box.right + 8f, box.center.y + 5f, paint)
    }
}

fun isFillableShape(type: StrokeType): Boolean {
    return when (type) {
        StrokeType.LINE,
        StrokeType.ARROW,
        StrokeType.DOUBLE_ARROW,
        StrokeType.LINE_DASHED,
        StrokeType.DASHED_ARROW,
        StrokeType.ANGLE_RAY,
        StrokeType.PEN,
        StrokeType.PENCIL,
        StrokeType.MARKER,
        StrokeType.HIGHLIGHTER,
        StrokeType.BRUSH,
        StrokeType.IMAGE,
        StrokeType.VIDEO,
        StrokeType.WEB_EMBED,
        StrokeType.EQUATION,
        StrokeType.CONNECTOR,
        StrokeType.STICKER,
        StrokeType.FRAME -> false
        else -> true
    }
}
