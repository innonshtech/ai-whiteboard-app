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
            StrokeType.RECTANGLE, StrokeType.SQUARE, StrokeType.FRAME -> listOf(
                Offset(left, top),
                Offset(right, top),
                Offset(right, bottom),
                Offset(left, bottom),
                Offset(left, top)
            )
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

            // Polyline source: ink strokes use raw points, shapes use their outline
            val isInk = stroke.strokeType in inkTypes
            val sourcePoints = if (isInk) stroke.points else (outlineOf(stroke) ?: stroke.points)
            if (sourcePoints.isEmpty()) {
                outStrokes.add(stroke)
                continue
            }

            val pressures = if (isInk && sourcePoints === stroke.points) stroke.pointPressures else emptyList()
            val split = splitPolyline(sourcePoints, pressures, lassoPoints)

            when {
                // Completely inside the lasso: keep stroke intact and select it
                split.outsidePieces.isEmpty() && split.insidePieces.isNotEmpty() -> {
                    selectedIds.add(stroke.id)
                    outStrokes.add(stroke)
                }

                // Completely outside the lasso: keep stroke intact, unselected
                split.insidePieces.isEmpty() -> {
                    outStrokes.add(stroke)
                }

                // Partially inside: stroke was cut!
                else -> {
                    didCut = true

                    // Add outside pieces (kept as unselected remainder)
                    for (piece in split.outsidePieces) {
                        outStrokes.add(
                            stroke.copy(
                                id = UUID.randomUUID().toString(),
                                points = piece.points,
                                pointPressures = piece.pressures,
                                strokeType = if (isInk) stroke.strokeType else StrokeType.PEN,
                                text = if (isInk) stroke.text else "",
                                fillColor = if (isInk) stroke.fillColor else 0
                            )
                        )
                    }

                    // Add inside pieces (the cut-out portion, selected)
                    for (piece in split.insidePieces) {
                        val pieceId = UUID.randomUUID().toString()
                        selectedIds.add(pieceId)
                        outStrokes.add(
                            stroke.copy(
                                id = pieceId,
                                points = piece.points,
                                pointPressures = piece.pressures,
                                strokeType = if (isInk) stroke.strokeType else StrokeType.PEN,
                                text = if (isInk) stroke.text else "",
                                fillColor = if (isInk) stroke.fillColor else 0
                            )
                        )
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

