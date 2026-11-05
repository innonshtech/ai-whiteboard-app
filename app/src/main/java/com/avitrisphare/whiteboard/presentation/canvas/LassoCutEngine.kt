package com.avitrisphare.whiteboard.presentation.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.avitrisphare.whiteboard.domain.model.Stroke
import com.avitrisphare.whiteboard.domain.model.StrokeType
import java.util.UUID
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

data class LassoCutResult(
    val strokes: List<Stroke>,
    val selectedIds: Set<String>,
    val didCut: Boolean
)

data class ShapeComponent(
    val points: List<Offset>,
    val isDashed: Boolean = false
)

object LassoCutEngine {

    private val inkTypes = setOf(
        StrokeType.PEN,
        StrokeType.PENCIL,
        StrokeType.MARKER,
        StrokeType.HIGHLIGHTER,
        StrokeType.BRUSH,
        StrokeType.LINE,
        StrokeType.ARROW,
        StrokeType.LINE_DASHED,
        StrokeType.DOUBLE_ARROW,
        StrokeType.DASHED_ARROW
    )

    private val objectTypes = setOf(
        StrokeType.TEXT,
        StrokeType.STICKY_NOTE,
        StrokeType.WEB_EMBED,
        StrokeType.IMAGE,
        StrokeType.VIDEO,
        StrokeType.EQUATION,
        StrokeType.STICKER
    )

    /**
     * Determines if a point is strictly inside the closed polygon via ray-casting.
     */
    fun pointInPolygon(pt: Offset, polygon: List<Offset>): Boolean {
        if (polygon.size < 3) return false
        var inside = false
        var j = polygon.size - 1
        for (i in polygon.indices) {
            val pi = polygon[i]
            val pj = polygon[j]
            if ((pi.y > pt.y) != (pj.y > pt.y) &&
                pt.x < (pj.x - pi.x) * (pt.y - pi.y) / (pj.y - pi.y + 1e-6f) + pi.x
            ) {
                inside = !inside
            }
            j = i
        }
        return inside
    }

    private data class SegmentIntersection(
        val t: Float,
        val point: Offset
    )

    private fun findSegmentIntersections(
        p1: Offset,
        p2: Offset,
        polygon: List<Offset>
    ): List<SegmentIntersection> {
        val vx = p2.x - p1.x
        val vy = p2.y - p1.y
        val results = ArrayList<SegmentIntersection>()
        val n = polygon.size

        for (j in 0 until n) {
            val q1 = polygon[j]
            val q2 = polygon[(j + 1) % n]
            val wx = q2.x - q1.x
            val wy = q2.y - q1.y

            val denom = vx * wy - vy * wx
            if (abs(denom) < 1e-6f) continue

            val dx = q1.x - p1.x
            val dy = q1.y - p1.y

            val t = (dx * wy - dy * wx) / denom
            val u = (dx * vy - dy * vx) / denom

            if (t > 1e-4f && t < 1f - 1e-4f && u >= -1e-5f && u <= 1f + 1e-5f) {
                val hit = Offset(p1.x + t * vx, p1.y + t * vy)
                results.add(SegmentIntersection(t, hit))
            }
        }

        if (results.isEmpty()) return emptyList()

        results.sortBy { it.t }
        val deduped = ArrayList<SegmentIntersection>(results.size)
        for (item in results) {
            if (deduped.isEmpty() || abs(item.t - deduped.last().t) > 1e-4f) {
                deduped.add(item)
            }
        }
        return deduped
    }

    data class PolylinePiece(
        val points: List<Offset>,
        val pressures: List<Float>
    )

    data class PolylineSplit(
        val insidePieces: List<PolylinePiece>,
        val outsidePieces: List<PolylinePiece>
    )

    fun splitPolyline(
        points: List<Offset>,
        pressures: List<Float>,
        polygon: List<Offset>
    ): PolylineSplit {
        if (points.isEmpty()) return PolylineSplit(emptyList(), emptyList())
        fun pressureAt(index: Int): Float = pressures.getOrElse(index) { 1f }

        if (points.size == 1) {
            val p = points[0]
            val pr = pressureAt(0)
            return if (pointInPolygon(p, polygon)) {
                PolylineSplit(listOf(PolylinePiece(points, listOf(pr))), emptyList())
            } else {
                PolylineSplit(emptyList(), listOf(PolylinePiece(points, listOf(pr))))
            }
        }

        data class SubSegment(
            val start: Offset,
            val end: Offset,
            val startP: Float,
            val endP: Float,
            val isInside: Boolean
        )

        val subSegments = ArrayList<SubSegment>()
        for (i in 0 until points.lastIndex) {
            val p1 = points[i]
            val p2 = points[i + 1]
            val pr1 = pressureAt(i)
            val pr2 = pressureAt(i + 1)

            val hits = findSegmentIntersections(p1, p2, polygon)
            val tList = ArrayList<Float>(hits.size + 2)
            val ptList = ArrayList<Offset>(hits.size + 2)

            tList.add(0f)
            ptList.add(p1)
            for (h in hits) {
                tList.add(h.t)
                ptList.add(h.point)
            }
            tList.add(1f)
            ptList.add(p2)

            for (k in 0 until ptList.size - 1) {
                val sPt = ptList[k]
                val ePt = ptList[k + 1]
                val tStart = tList[k]
                val tEnd = tList[k + 1]
                val sPress = pr1 + tStart * (pr2 - pr1)
                val ePress = pr1 + tEnd * (pr2 - pr1)
                val mid = Offset((sPt.x + ePt.x) / 2f, (sPt.y + ePt.y) / 2f)
                val isInside = pointInPolygon(mid, polygon)
                subSegments.add(SubSegment(sPt, ePt, sPress, ePress, isInside))
            }
        }

        val insidePieces = ArrayList<PolylinePiece>()
        val outsidePieces = ArrayList<PolylinePiece>()

        var currentBuilding: MutableList<Offset>? = null
        var currentBuildingPressures: MutableList<Float>? = null
        var currentInside: Boolean? = null

        fun closeCurrent() {
            val b = currentBuilding ?: return
            val p = currentBuildingPressures ?: return
            if (b.isNotEmpty()) {
                val piece = PolylinePiece(b, p)
                if (currentInside == true) {
                    insidePieces.add(piece)
                } else {
                    outsidePieces.add(piece)
                }
            }
            currentBuilding = null
            currentBuildingPressures = null
            currentInside = null
        }

        for (sub in subSegments) {
            if (currentInside == null) {
                currentInside = sub.isInside
                currentBuilding = mutableListOf(sub.start, sub.end)
                currentBuildingPressures = mutableListOf(sub.startP, sub.endP)
            } else if (currentInside == sub.isInside) {
                currentBuilding!!.add(sub.end)
                currentBuildingPressures!!.add(sub.endP)
            } else {
                closeCurrent()
                currentInside = sub.isInside
                currentBuilding = mutableListOf(sub.start, sub.end)
                currentBuildingPressures = mutableListOf(sub.startP, sub.endP)
            }
        }
        closeCurrent()

        return PolylineSplit(insidePieces, outsidePieces)
    }

    fun buildCylinderComponents(box: Rect): List<ShapeComponent> {
        val ry = (box.height * 0.15f).coerceAtLeast(8f)
        val cx = box.center.x
        val rx = box.width / 2f
        val topY = box.top + ry
        val bottomY = box.bottom - ry

        val topEllipse = List(33) { idx ->
            val a = idx / 32f * (2.0 * PI).toFloat()
            Offset(cx + rx * cos(a), topY + ry * sin(a))
        }

        val leftSide = listOf(Offset(box.left, topY), Offset(box.left, bottomY))
        val rightSide = listOf(Offset(box.right, topY), Offset(box.right, bottomY))

        val bottomFrontArc = List(17) { idx ->
            val a = idx / 16f * PI.toFloat()
            Offset(cx + rx * cos(a), bottomY + ry * sin(a))
        }

        val bottomBackArc = List(17) { idx ->
            val a = PI.toFloat() + idx / 16f * PI.toFloat()
            Offset(cx + rx * cos(a), bottomY + ry * sin(a))
        }

        return listOf(
            ShapeComponent(topEllipse, isDashed = false),
            ShapeComponent(leftSide, isDashed = false),
            ShapeComponent(rightSide, isDashed = false),
            ShapeComponent(bottomFrontArc, isDashed = false),
            ShapeComponent(bottomBackArc, isDashed = true)
        )
    }

    fun buildConeComponents(box: Rect): List<ShapeComponent> {
        val apex = Offset(box.center.x, box.top)
        val ry = (box.height * 0.15f).coerceAtLeast(8f)
        val cx = box.center.x
        val cy = box.bottom - ry
        val rx = box.width / 2f

        val leftSide = listOf(apex, Offset(box.left, cy))
        val rightSide = listOf(apex, Offset(box.right, cy))

        val bottomFrontArc = List(17) { idx ->
            val a = idx / 16f * PI.toFloat()
            Offset(cx + rx * cos(a), cy + ry * sin(a))
        }

        val bottomBackArc = List(17) { idx ->
            val a = PI.toFloat() + idx / 16f * PI.toFloat()
            Offset(cx + rx * cos(a), cy + ry * sin(a))
        }

        return listOf(
            ShapeComponent(leftSide, isDashed = false),
            ShapeComponent(rightSide, isDashed = false),
            ShapeComponent(bottomFrontArc, isDashed = false),
            ShapeComponent(bottomBackArc, isDashed = true)
        )
    }

    fun buildSphereComponents(box: Rect): List<ShapeComponent> {
        val cx = box.center.x
        val cy = box.center.y
        val rx = box.width / 2f
        val ryOuter = box.height / 2f
        val ryEquator = box.height * 0.22f

        val outerCircle = List(33) { idx ->
            val a = idx / 32f * (2.0 * PI).toFloat()
            Offset(cx + rx * cos(a), cy + ryOuter * sin(a))
        }

        val frontEquator = List(17) { idx ->
            val a = idx / 16f * PI.toFloat()
            Offset(cx + rx * cos(a), cy + ryEquator * sin(a))
        }

        val backEquator = List(17) { idx ->
            val a = PI.toFloat() + idx / 16f * PI.toFloat()
            Offset(cx + rx * cos(a), cy + ryEquator * sin(a))
        }

        return listOf(
            ShapeComponent(outerCircle, isDashed = false),
            ShapeComponent(frontEquator, isDashed = false),
            ShapeComponent(backEquator, isDashed = true)
        )
    }

    fun buildCuboidComponents(box: Rect): List<ShapeComponent> {
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

        val frontFace = listOf(
            Offset(frontLeft, frontTop),
            Offset(frontRight, frontTop),
            Offset(frontRight, frontBottom),
            Offset(frontLeft, frontBottom),
            Offset(frontLeft, frontTop)
        )

        return listOf(
            // Hidden back edges (dashed)
            ShapeComponent(listOf(Offset(frontLeft, frontBottom), Offset(backLeft, backBottom)), isDashed = true),
            ShapeComponent(listOf(Offset(backLeft, backBottom), Offset(backRight, backBottom)), isDashed = true),
            ShapeComponent(listOf(Offset(backLeft, backTop), Offset(backLeft, backBottom)), isDashed = true),
            // Visible front face
            ShapeComponent(frontFace, isDashed = false),
            // Visible connecting edges
            ShapeComponent(listOf(Offset(frontLeft, frontTop), Offset(backLeft, backTop)), isDashed = false),
            ShapeComponent(listOf(Offset(frontRight, frontTop), Offset(backRight, backTop)), isDashed = false),
            ShapeComponent(listOf(Offset(frontRight, frontBottom), Offset(backRight, backBottom)), isDashed = false),
            // Visible top & right back edges
            ShapeComponent(listOf(Offset(backLeft, backTop), Offset(backRight, backTop)), isDashed = false),
            ShapeComponent(listOf(Offset(backRight, backTop), Offset(backRight, backBottom)), isDashed = false)
        )
    }

    fun buildPyramidComponents(box: Rect): List<ShapeComponent> {
        val apex = Offset(box.center.x, box.top)
        val bLeft = Offset(box.left, box.bottom - box.height * 0.15f)
        val bCenter = Offset(box.center.x, box.bottom)
        val bRight = Offset(box.right, box.bottom - box.height * 0.15f)
        val bBack = Offset(box.center.x, box.bottom - box.height * 0.30f)

        return listOf(
            // Hidden base & edges (dashed)
            ShapeComponent(listOf(bLeft, bBack), isDashed = true),
            ShapeComponent(listOf(bBack, bRight), isDashed = true),
            ShapeComponent(listOf(apex, bBack), isDashed = true),
            // Visible base & front edges
            ShapeComponent(listOf(bLeft, bCenter), isDashed = false),
            ShapeComponent(listOf(bCenter, bRight), isDashed = false),
            ShapeComponent(listOf(apex, bLeft), isDashed = false),
            ShapeComponent(listOf(apex, bCenter), isDashed = false),
            ShapeComponent(listOf(apex, bRight), isDashed = false)
        )
    }

    fun buildPrismComponents(box: Rect): List<ShapeComponent> {
        val depthX = box.width * 0.28f
        val depthY = box.height * 0.18f

        val fApex = Offset(box.left + (box.width - depthX) / 2f, box.top + depthY)
        val fLeft = Offset(box.left, box.bottom)
        val fRight = Offset(box.right - depthX, box.bottom)

        val bApex = Offset(fApex.x + depthX, fApex.y - depthY)
        val bLeft = Offset(fLeft.x + depthX, fLeft.y - depthY)
        val bRight = Offset(fRight.x + depthX, fRight.y - depthY)

        val frontTriangle = listOf(fApex, fLeft, fRight, fApex)

        return listOf(
            // Hidden back edges (dashed)
            ShapeComponent(listOf(fLeft, bLeft), isDashed = true),
            ShapeComponent(listOf(bLeft, bRight), isDashed = true),
            ShapeComponent(listOf(bApex, bLeft), isDashed = true),
            // Front triangle
            ShapeComponent(frontTriangle, isDashed = false),
            // Connecting top & right
            ShapeComponent(listOf(fApex, bApex), isDashed = false),
            ShapeComponent(listOf(fRight, bRight), isDashed = false),
            ShapeComponent(listOf(bApex, bRight), isDashed = false)
        )
    }

    fun outlineOf(stroke: Stroke): List<Offset>? {
        if (stroke.points.size < 2) return null
        val a = stroke.points.first()
        val b = stroke.points.last()
        val left = minOf(a.x, b.x)
        val top = minOf(a.y, b.y)
        val right = maxOf(a.x, b.x)
        val bottom = maxOf(a.y, b.y)
        val width = right - left
        val height = bottom - top
        if (width <= 0f && height <= 0f) return null

        return when (stroke.strokeType) {
            StrokeType.RECTANGLE, StrokeType.FRAME -> {
                if (stroke.cornerRadius > 0f) {
                    val r = stroke.cornerRadius.coerceAtMost(minOf(width, height) / 2f)
                    buildRoundRectOutline(left, top, right, bottom, r)
                } else {
                    listOf(
                        Offset(left, top),
                        Offset(right, top),
                        Offset(right, bottom),
                        Offset(left, bottom),
                        Offset(left, top)
                    )
                }
            }
            StrokeType.SQUARE -> {
                val side = minOf(width, height)
                if (stroke.cornerRadius > 0f) {
                    val r = stroke.cornerRadius.coerceAtMost(side / 2f)
                    buildRoundRectOutline(left, top, left + side, top + side, r)
                } else {
                    listOf(
                        Offset(left, top),
                        Offset(left + side, top),
                        Offset(left + side, top + side),
                        Offset(left, top + side),
                        Offset(left, top)
                    )
                }
            }
            StrokeType.TRIANGLE -> listOf(
                Offset((left + right) / 2f, top),
                Offset(right, bottom),
                Offset(left, bottom),
                Offset((left + right) / 2f, top)
            )
            StrokeType.RIGHT_TRIANGLE -> listOf(
                Offset(left, top),
                Offset(left, bottom),
                Offset(right, bottom),
                Offset(left, top)
            )
            StrokeType.SCALENE_TRIANGLE -> listOf(
                Offset(left, bottom),
                Offset(left + width * 0.35f, top),
                Offset(right, bottom),
                Offset(left, bottom)
            )
            StrokeType.SEMICIRCLE -> {
                val minHeight = (width * 0.5f).coerceAtLeast(8f)
                val effectiveHeight = if (height < 4f) minHeight else height
                val cx = (left + right) / 2f
                val cy = bottom
                val rx = width / 2f
                val ry = effectiveHeight
                val pts = ArrayList<Offset>(34)
                for (i in 0..32) {
                    val angle = PI - (i / 32.0) * PI
                    pts.add(Offset((cx + rx * cos(angle)).toFloat(), (cy - ry * sin(angle)).toFloat()))
                }
                pts.add(Offset(left, bottom))
                pts
            }
            StrokeType.DIAMOND, StrokeType.RHOMBUS -> {
                val cx = (left + right) / 2f
                val cy = (top + bottom) / 2f
                listOf(
                    Offset(cx, top),
                    Offset(right, cy),
                    Offset(cx, bottom),
                    Offset(left, cy),
                    Offset(cx, top)
                )
            }
            StrokeType.ROUND_RECT, StrokeType.ROUNDED_RECTANGLE -> {
                val r = (if (stroke.cornerRadius > 0f) stroke.cornerRadius else minOf(width, height) * 0.15f)
                    .coerceAtMost(minOf(width, height) / 2f)
                buildRoundRectOutline(left, top, right, bottom, r)
            }
            StrokeType.CIRCLE, StrokeType.ELLIPSE -> {
                val cx = (left + right) / 2f
                val cy = (top + bottom) / 2f
                val rx = width / 2f
                val ry = height / 2f
                List(65) { idx ->
                    val angle = idx / 64f * (2.0 * PI).toFloat()
                    Offset(cx + rx * cos(angle), cy + ry * sin(angle))
                }
            }
            StrokeType.STAR -> {
                val cx = (left + right) / 2f
                val cy = (top + bottom) / 2f
                val outer = minOf(width, height) / 2f
                val inner = outer * 0.45f
                List(11) { i ->
                    val angle = -PI / 2.0 + i * PI / 5.0
                    val rad = if (i % 2 == 0) outer else inner
                    Offset(cx + (rad * cos(angle)).toFloat(), cy + (rad * sin(angle)).toFloat())
                }
            }
            StrokeType.HEXAGON -> {
                val cx = (left + right) / 2f
                val cy = (top + bottom) / 2f
                val rx = width / 2f
                val ry = height / 2f
                List(7) { i ->
                    val angle = i / 6.0 * (2.0 * PI) - PI / 2.0
                    Offset(cx + (rx * cos(angle)).toFloat(), cy + (ry * sin(angle)).toFloat())
                }
            }
            StrokeType.OCTAGON -> {
                val cx = (left + right) / 2f
                val cy = (top + bottom) / 2f
                val rx = width / 2f
                val ry = height / 2f
                List(9) { i ->
                    val angle = i / 8.0 * (2.0 * PI) - PI / 8.0
                    Offset(cx + (rx * cos(angle)).toFloat(), cy + (ry * sin(angle)).toFloat())
                }
            }
            StrokeType.PENTAGON -> {
                val cx = (left + right) / 2f
                val cy = (top + bottom) / 2f
                val rx = width / 2f
                val ry = height / 2f
                List(6) { i ->
                    val angle = i / 5.0 * (2.0 * PI) - PI / 2.0
                    Offset(cx + (rx * cos(angle)).toFloat(), cy + (ry * sin(angle)).toFloat())
                }
            }
            StrokeType.TRAPEZOID -> {
                val inset = width * 0.2f
                listOf(
                    Offset(left + inset, top),
                    Offset(right - inset, top),
                    Offset(right, bottom),
                    Offset(left, bottom),
                    Offset(left + inset, top)
                )
            }
            StrokeType.PARALLELOGRAM -> {
                val inset = width * 0.2f
                listOf(
                    Offset(left + inset, top),
                    Offset(right, top),
                    Offset(right - inset, bottom),
                    Offset(left, bottom),
                    Offset(left + inset, top)
                )
            }
            else -> null
        }
    }

    private fun buildRoundRectOutline(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        r: Float
    ): List<Offset> {
        val pts = ArrayList<Offset>()
        pts.add(Offset(left + r, top))
        pts.add(Offset(right - r, top))
        for (step in 1..4) {
            val a = -PI / 2.0 + (step / 4.0) * (PI / 2.0)
            pts.add(Offset((right - r + r * cos(a)).toFloat(), (top + r + r * sin(a)).toFloat()))
        }
        pts.add(Offset(right, bottom - r))
        for (step in 1..4) {
            val a = 0.0 + (step / 4.0) * (PI / 2.0)
            pts.add(Offset((right - r + r * cos(a)).toFloat(), (bottom - r + r * sin(a)).toFloat()))
        }
        pts.add(Offset(left + r, bottom))
        for (step in 1..4) {
            val a = PI / 2.0 + (step / 4.0) * (PI / 2.0)
            pts.add(Offset((left + r + r * cos(a)).toFloat(), (bottom - r + r * sin(a)).toFloat()))
        }
        pts.add(Offset(left, top + r))
        for (step in 1..4) {
            val a = PI + (step / 4.0) * (PI / 2.0)
            pts.add(Offset((left + r + r * cos(a)).toFloat(), (top + r + r * sin(a)).toFloat()))
        }
        pts.add(pts.first())
        return pts
    }

    fun componentsOf(stroke: Stroke): List<ShapeComponent> {
        if (stroke.points.size < 2) {
            return if (stroke.points.isNotEmpty()) {
                listOf(ShapeComponent(stroke.points, isDashed = false))
            } else {
                emptyList()
            }
        }

        val p1 = stroke.points.first()
        val p2 = stroke.points.last()
        val box = Rect(minOf(p1.x, p2.x), minOf(p1.y, p2.y), maxOf(p1.x, p2.x), maxOf(p1.y, p2.y))

        return when (stroke.strokeType) {
            StrokeType.CYLINDER -> buildCylinderComponents(box)
            StrokeType.CONE -> buildConeComponents(box)
            StrokeType.SPHERE -> buildSphereComponents(box)
            StrokeType.CUBE, StrokeType.CUBOID -> buildCuboidComponents(box)
            StrokeType.PYRAMID -> buildPyramidComponents(box)
            StrokeType.PRISM -> buildPrismComponents(box)
            StrokeType.ANGLE_RAY -> {
                val corner = Offset(p1.x, p2.y)
                listOf(
                    ShapeComponent(listOf(p1, corner), isDashed = false),
                    ShapeComponent(listOf(corner, p2), isDashed = false)
                )
            }
            else -> {
                val outline = outlineOf(stroke)
                if (outline != null) {
                    listOf(ShapeComponent(outline, isDashed = false))
                } else {
                    val isDashed = stroke.strokeType == StrokeType.LINE_DASHED || stroke.strokeType == StrokeType.DASHED_ARROW
                    listOf(ShapeComponent(stroke.points, isDashed = isDashed))
                }
            }
        }
    }

    data class ResolvedPiece(
        val points: List<Offset>,
        val pressures: List<Float>,
        val strokeType: StrokeType
    )

    fun resolvePiece(
        original: Stroke,
        piece: PolylinePiece,
        isDashed: Boolean
    ): ResolvedPiece {
        val pts = piece.points
        val prs = piece.pressures

        if (isDashed) {
            if (original.strokeType == StrokeType.DASHED_ARROW) {
                val origEnd = original.points.lastOrNull()
                if (origEnd != null && pts.isNotEmpty() &&
                    hypot(pts.last().x - origEnd.x, pts.last().y - origEnd.y) < 4f
                ) {
                    return ResolvedPiece(pts, prs, StrokeType.DASHED_ARROW)
                }
            }
            return ResolvedPiece(pts, prs, StrokeType.LINE_DASHED)
        }

        if (original.strokeType == StrokeType.ARROW) {
            val origEnd = original.points.lastOrNull()
            if (origEnd != null && pts.isNotEmpty() &&
                hypot(pts.last().x - origEnd.x, pts.last().y - origEnd.y) < 4f
            ) {
                return ResolvedPiece(pts, prs, StrokeType.ARROW)
            }
            return ResolvedPiece(pts, prs, StrokeType.LINE)
        }

        if (original.strokeType == StrokeType.DOUBLE_ARROW) {
            val origStart = original.points.firstOrNull()
            val origEnd = original.points.lastOrNull()
            if (origEnd != null && pts.isNotEmpty() &&
                hypot(pts.last().x - origEnd.x, pts.last().y - origEnd.y) < 4f
            ) {
                return ResolvedPiece(pts, prs, StrokeType.ARROW)
            }
            if (origStart != null && pts.isNotEmpty() &&
                hypot(pts.first().x - origStart.x, pts.first().y - origStart.y) < 4f
            ) {
                return ResolvedPiece(pts.reversed(), prs.reversed(), StrokeType.ARROW)
            }
            return ResolvedPiece(pts, prs, StrokeType.LINE)
        }

        if (original.strokeType == StrokeType.LINE) {
            return ResolvedPiece(pts, prs, StrokeType.LINE)
        }

        if (original.strokeType in setOf(
                StrokeType.PEN,
                StrokeType.PENCIL,
                StrokeType.MARKER,
                StrokeType.HIGHLIGHTER,
                StrokeType.BRUSH
            )
        ) {
            return ResolvedPiece(pts, prs, original.strokeType)
        }

        return ResolvedPiece(pts, prs, StrokeType.LINE)
    }

    fun cutAndSelect(
        strokes: List<Stroke>,
        lassoPoints: List<Offset>
    ): LassoCutResult {
        if (lassoPoints.size < 3 || strokes.isEmpty()) {
            return LassoCutResult(strokes, emptySet(), false)
        }

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        for (p in lassoPoints) {
            if (p.x < minX) minX = p.x
            if (p.x > maxX) maxX = p.x
            if (p.y < minY) minY = p.y
            if (p.y > maxY) maxY = p.y
        }
        val lassoRect = Rect(minX, minY, maxX, maxY)

        val outStrokes = ArrayList<Stroke>(strokes.size)
        val selectedIds = mutableSetOf<String>()
        var didCut = false

        for (stroke in strokes) {
            val bounds = stroke.bounds
            // Quick bounding box rejection
            if (!lassoRect.overlaps(bounds) && !lassoRect.contains(bounds.topLeft) && !bounds.contains(lassoRect.topLeft)) {
                outStrokes.add(stroke)
                continue
            }

            // Non-splittable objects (Text, Image, Sticky Note, Equation, Web Embed, etc.)
            if (stroke.strokeType in objectTypes) {
                val hit = pointInPolygon(bounds.center, lassoPoints) ||
                    pointInPolygon(bounds.topLeft, lassoPoints) ||
                    pointInPolygon(bounds.topRight, lassoPoints) ||
                    pointInPolygon(bounds.bottomLeft, lassoPoints) ||
                    pointInPolygon(bounds.bottomRight, lassoPoints)
                if (hit) {
                    selectedIds.add(stroke.id)
                }
                outStrokes.add(stroke)
                continue
            }

            val components = componentsOf(stroke)
            if (components.isEmpty()) {
                outStrokes.add(stroke)
                continue
            }

            val isInk = stroke.strokeType in inkTypes

            data class CompSplit(
                val comp: ShapeComponent,
                val insidePieces: List<PolylinePiece>,
                val outsidePieces: List<PolylinePiece>
            )

            var hasInside = false
            var hasOutside = false
            val splitResults = ArrayList<CompSplit>(components.size)

            for (comp in components) {
                val pressures = if (isInk && comp.points === stroke.points) stroke.pointPressures else emptyList()
                val split = splitPolyline(comp.points, pressures, lassoPoints)
                if (split.insidePieces.isNotEmpty()) hasInside = true
                if (split.outsidePieces.isNotEmpty()) hasOutside = true
                splitResults.add(CompSplit(comp, split.insidePieces, split.outsidePieces))
            }

            when {
                // Completely inside the lasso: keep stroke intact and select it
                hasInside && !hasOutside -> {
                    selectedIds.add(stroke.id)
                    outStrokes.add(stroke)
                }

                // Completely outside the lasso: keep stroke intact, unselected
                !hasInside -> {
                    outStrokes.add(stroke)
                }

                // Partially inside: stroke was cut!
                else -> {
                    didCut = true

                    // Add outside pieces (kept as unselected remainder)
                    for (result in splitResults) {
                        for (piece in result.outsidePieces) {
                            if (piece.points.size < 2 && stroke.points.size > 1) continue
                            val resolved = resolvePiece(stroke, piece, result.comp.isDashed)
                            outStrokes.add(
                                stroke.copy(
                                    id = UUID.randomUUID().toString(),
                                    points = resolved.points,
                                    pointPressures = resolved.pressures,
                                    strokeType = resolved.strokeType,
                                    text = if (isInk) stroke.text else "",
                                    fillColor = if (isInk) stroke.fillColor else 0
                                )
                            )
                        }
                    }

                    // Add inside pieces (the cut-out portion, selected)
                    for (result in splitResults) {
                        for (piece in result.insidePieces) {
                            if (piece.points.size < 2 && stroke.points.size > 1) continue
                            val pieceId = UUID.randomUUID().toString()
                            selectedIds.add(pieceId)
                            val resolved = resolvePiece(stroke, piece, result.comp.isDashed)
                            outStrokes.add(
                                stroke.copy(
                                    id = pieceId,
                                    points = resolved.points,
                                    pointPressures = resolved.pressures,
                                    strokeType = resolved.strokeType,
                                    text = if (isInk) stroke.text else "",
                                    fillColor = if (isInk) stroke.fillColor else 0
                                )
                            )
                        }
                    }
                }
            }
        }

        return LassoCutResult(
            strokes = if (didCut) outStrokes else strokes,
            selectedIds = selectedIds,
            didCut = didCut
        )
    }
}
