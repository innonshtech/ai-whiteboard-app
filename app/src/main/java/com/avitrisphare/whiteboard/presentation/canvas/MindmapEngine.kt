package com.avitrisphare.whiteboard.presentation.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path as ComposePath
import com.avitrisphare.whiteboard.domain.model.Stroke
import com.avitrisphare.whiteboard.domain.model.StrokeType
import java.util.UUID
import kotlin.math.abs

object MindmapEngine {

    val PALETTE_PURPLE = 0xFF8B5CF6.toInt()
    val PALETTE_CYAN = 0xFF06B6D4.toInt()
    val PALETTE_BLUE = 0xFF3B82F6.toInt()
    val PALETTE_PINK = 0xFFEC4899.toInt()
    val PALETTE_ORANGE = 0xFFF97316.toInt()
    val PALETTE_YELLOW = 0xFFEAB308.toInt()
    val PALETTE_GREEN = 0xFF10B981.toInt()
    val PALETTE_RED = 0xFFEF4444.toInt()
    val PALETTE_DARK = 0xFF1E293B.toInt()
    val PALETTE_LIGHT_FILL = 0xFFF8FAFC.toInt()

    fun generateTemplate(
        kind: MindmapTemplateKind,
        center: Offset
    ): List<Stroke> {
        return when (kind) {
            MindmapTemplateKind.BLANK -> generateBlank(center)
            MindmapTemplateKind.BILATERAL_ORTHOGONAL -> generateBilateralOrthogonal(center)
            MindmapTemplateKind.RIGHT_ORGANIC -> generateRightOrganic(center)
            MindmapTemplateKind.BILATERAL_CURVED -> generateBilateralCurved(center)
            MindmapTemplateKind.RIGHT_ORTHOGONAL -> generateRightOrthogonal(center)
            MindmapTemplateKind.DOWNWARD_ORG -> generateDownwardOrg(center)
            MindmapTemplateKind.DUAL_BRACKET -> generateDualBracket(center)
            MindmapTemplateKind.DOWNWARD_CURVED -> generateDownwardCurved(center)
        }
    }

    private fun createNode(
        topLeft: Offset,
        width: Float = 160f,
        height: Float = 64f,
        borderColor: Int = PALETTE_DARK,
        fillColor: Int = PALETTE_LIGHT_FILL,
        text: String = "",
        role: String = "node",
        cornerRadius: Float = 16f
    ): Stroke {
        val bottomRight = Offset(topLeft.x + width, topLeft.y + height)
        return Stroke(
            points = listOf(topLeft, bottomRight),
            colorValue = borderColor,
            fillColor = fillColor,
            strokeWidth = 3f,
            strokeType = StrokeType.ROUND_RECT,
            cornerRadius = cornerRadius,
            stickerKind = "mindmap:$role",
            text = text
        )
    }

    private fun createConnector(
        fromNode: Stroke,
        toNode: Stroke,
        color: Int = PALETTE_CYAN,
        wireStyle: String = "orthogonal"
    ): Stroke {
        val (p1, p2) = calculateAnchors(fromNode.bounds, toNode.bounds)
        return Stroke(
            points = listOf(p1, p2),
            colorValue = color,
            strokeWidth = 3.5f,
            strokeType = StrokeType.CONNECTOR,
            linkFromId = fromNode.id,
            linkToId = toNode.id,
            stickerKind = wireStyle
        )
    }

    fun calculateBoundaryIntersection(rect: Rect, target: Offset): Offset {
        val c = rect.center
        val dx = target.x - c.x
        val dy = target.y - c.y
        if (dx == 0f && dy == 0f) return c
        val halfW = (rect.width / 2f).coerceAtLeast(1f)
        val halfH = (rect.height / 2f).coerceAtLeast(1f)
        val scaleX = if (dx != 0f) halfW / kotlin.math.abs(dx) else Float.MAX_VALUE
        val scaleY = if (dy != 0f) halfH / kotlin.math.abs(dy) else Float.MAX_VALUE
        val scale = minOf(scaleX, scaleY)
        return Offset(c.x + dx * scale, c.y + dy * scale)
    }

    fun calculateStraightAnchors(from: Rect, to: Rect): Pair<Offset, Offset> {
        val p1 = calculateBoundaryIntersection(from, to.center)
        val p2 = calculateBoundaryIntersection(to, from.center)
        return p1 to p2
    }

    fun lineIntersectsSegment(p1: Offset, p2: Offset, q1: Offset, q2: Offset): Boolean {
        fun ccw(a: Offset, b: Offset, c: Offset): Boolean {
            return (c.y - a.y) * (b.x - a.x) > (b.y - a.y) * (c.x - a.x)
        }
        return (ccw(p1, q1, q2) != ccw(p2, q1, q2)) && (ccw(p1, p2, q1) != ccw(p1, p2, q2))
    }

    fun lineIntersectsRect(p1: Offset, p2: Offset, rect: Rect): Boolean {
        if (rect.contains(p1) || rect.contains(p2)) return true
        val tl = rect.topLeft
        val tr = rect.topRight
        val bl = rect.bottomLeft
        val br = rect.bottomRight
        return lineIntersectsSegment(p1, p2, tl, tr) ||
            lineIntersectsSegment(p1, p2, tr, br) ||
            lineIntersectsSegment(p1, p2, br, bl) ||
            lineIntersectsSegment(p1, p2, bl, tl)
    }

    /**
     * Calculates a collision-free route between [from] and [to] avoiding [obstacles]
     * and applying [laneOffset] to prevent overlapping existing parallel lines.
     */
    fun calculateCollisionFreeRoute(
        from: Rect,
        to: Rect,
        obstacles: List<Rect> = emptyList(),
        wireStyle: String = "orthogonal",
        laneOffset: Float = 0f
    ): List<Offset> {
        val fc = from.center
        val tc = to.center
        val isHorizontal = abs(tc.x - fc.x) >= abs(tc.y - fc.y)

        val corridor = Rect(
            minOf(from.left, to.left) - 10f,
            minOf(from.top, to.top) - 10f,
            maxOf(from.right, to.right) + 10f,
            maxOf(from.bottom, to.bottom) + 10f
        )
        val blockingObstacles = obstacles.filter { obs ->
            obs != from && obs != to && obs.overlaps(corridor)
        }

        val directAnchors = if (wireStyle == "straight") calculateStraightAnchors(from, to) else calculateAnchors(from, to)
        val directP1 = directAnchors.first
        val directP2 = directAnchors.second

        val directHits = blockingObstacles.any { obs -> lineIntersectsRect(directP1, directP2, obs) }

        if (!directHits) {
            if (wireStyle == "straight" || wireStyle == "curved") {
                return listOf(directP1, directP2)
            }
            if (isHorizontal) {
                val midX = (directP1.x + directP2.x) / 2f + laneOffset
                val pMid1 = Offset(midX, directP1.y)
                val pMid2 = Offset(midX, directP2.y)
                val stepHits = blockingObstacles.any { obs ->
                    lineIntersectsRect(directP1, pMid1, obs) ||
                        lineIntersectsRect(pMid1, pMid2, obs) ||
                        lineIntersectsRect(pMid2, directP2, obs)
                }
                if (!stepHits) {
                    return if (abs(pMid1.y - pMid2.y) < 2f && abs(directP1.x - directP2.x) < 2f) {
                        listOf(directP1, directP2)
                    } else {
                        listOf(directP1, pMid1, pMid2, directP2)
                    }
                }
            } else {
                val midY = (directP1.y + directP2.y) / 2f + laneOffset
                val pMid1 = Offset(directP1.x, midY)
                val pMid2 = Offset(directP2.x, midY)
                val stepHits = blockingObstacles.any { obs ->
                    lineIntersectsRect(directP1, pMid1, obs) ||
                        lineIntersectsRect(pMid1, pMid2, obs) ||
                        lineIntersectsRect(pMid2, directP2, obs)
                }
                if (!stepHits) {
                    return if (abs(pMid1.x - pMid2.x) < 2f && abs(directP1.y - directP2.y) < 2f) {
                        listOf(directP1, directP2)
                    } else {
                        listOf(directP1, pMid1, pMid2, directP2)
                    }
                }
            }
        }

        // Bypass / detour around intervening obstacles
        val margin = 32f + abs(laneOffset)
        if (isHorizontal) {
            val topBypass = (blockingObstacles.minOfOrNull { it.top } ?: minOf(from.top, to.top)) - margin
            val bottomBypass = (blockingObstacles.maxOfOrNull { it.bottom } ?: maxOf(from.bottom, to.bottom)) + margin

            val midY = (fc.y + tc.y) / 2f
            val bypassY = if (abs(topBypass - midY) <= abs(bottomBypass - midY)) topBypass else bottomBypass

            val startAnchor = calculateBoundaryIntersection(from, Offset(fc.x, bypassY))
            val endAnchor = calculateBoundaryIntersection(to, Offset(tc.x, bypassY))
            if (wireStyle == "curved") {
                val bypassMid = Offset((startAnchor.x + endAnchor.x) / 2f, bypassY)
                return listOf(startAnchor, bypassMid, endAnchor)
            }
            val c1 = Offset(startAnchor.x, bypassY)
            val c2 = Offset(endAnchor.x, bypassY)
            return listOf(startAnchor, c1, c2, endAnchor)
        } else {
            val leftBypass = (blockingObstacles.minOfOrNull { it.left } ?: minOf(from.left, to.left)) - margin
            val rightBypass = (blockingObstacles.maxOfOrNull { it.right } ?: maxOf(from.right, to.right)) + margin

            val midX = (fc.x + tc.x) / 2f
            val bypassX = if (abs(leftBypass - midX) <= abs(rightBypass - midX)) leftBypass else rightBypass

            val startAnchor = calculateBoundaryIntersection(from, Offset(bypassX, fc.y))
            val endAnchor = calculateBoundaryIntersection(to, Offset(bypassX, tc.y))
            if (wireStyle == "curved") {
                val bypassMid = Offset(bypassX, (startAnchor.y + endAnchor.y) / 2f)
                return listOf(startAnchor, bypassMid, endAnchor)
            }
            val c1 = Offset(bypassX, startAnchor.y)
            val c2 = Offset(bypassX, endAnchor.y)
            return listOf(startAnchor, c1, c2, endAnchor)
        }
    }

    fun calculateAnchors(from: Rect, to: Rect): Pair<Offset, Offset> {
        val fc = from.center
        val tc = to.center
        return if (abs(tc.x - fc.x) >= abs(tc.y - fc.y)) {
            // Horizontal connection
            if (tc.x >= fc.x) {
                Offset(from.right, fc.y) to Offset(to.left, tc.y)
            } else {
                Offset(from.left, fc.y) to Offset(to.right, tc.y)
            }
        } else {
            // Vertical connection
            if (tc.y >= fc.y) {
                Offset(fc.x, from.bottom) to Offset(tc.x, to.top)
            } else {
                Offset(fc.x, from.top) to Offset(tc.x, to.bottom)
            }
        }
    }

    // 1. Blank Template
    private fun generateBlank(center: Offset): List<Stroke> {
        val root = createNode(
            topLeft = Offset(center.x - 90f, center.y - 40f),
            width = 180f,
            height = 80f,
            borderColor = PALETTE_DARK,
            fillColor = PALETTE_LIGHT_FILL,
            text = "",
            role = "root",
            cornerRadius = 18f
        )
        return listOf(root)
    }

    // 2. Bilateral Orthogonal (Exact match to Image 2!)
    private fun generateBilateralOrthogonal(center: Offset): List<Stroke> {
        val strokes = mutableListOf<Stroke>()
        val root = createNode(
            topLeft = Offset(center.x - 90f, center.y - 40f),
            width = 180f,
            height = 80f,
            borderColor = PALETTE_DARK,
            fillColor = PALETTE_LIGHT_FILL,
            text = "Main Topic",
            role = "root",
            cornerRadius = 18f
        )
        strokes.add(root)

        // Left 4 branches: Purple, Cyan, Blue, Pink
        val leftColors = listOf(PALETTE_PURPLE, PALETTE_CYAN, PALETTE_BLUE, PALETTE_PINK)
        val leftYOffsets = listOf(-160f, -55f, 55f, 160f)
        val leftX = center.x - 380f

        leftColors.forEachIndexed { i, color ->
            val node = createNode(
                topLeft = Offset(leftX, center.y + leftYOffsets[i] - 32f),
                width = 150f,
                height = 64f,
                borderColor = color,
                fillColor = PALETTE_LIGHT_FILL,
                text = "",
                cornerRadius = 16f
            )
            val connector = createConnector(root, node, color = color, wireStyle = "orthogonal")
            strokes.add(connector)
            strokes.add(node)
        }

        // Right 4 branches: Cyan, Orange, Orange, Cyan
        val rightColors = listOf(PALETTE_CYAN, PALETTE_ORANGE, PALETTE_ORANGE, PALETTE_CYAN)
        val rightYOffsets = listOf(-160f, -55f, 55f, 160f)
        val rightX = center.x + 230f

        rightColors.forEachIndexed { i, color ->
            val node = createNode(
                topLeft = Offset(rightX, center.y + rightYOffsets[i] - 32f),
                width = 150f,
                height = 64f,
                borderColor = color,
                fillColor = PALETTE_LIGHT_FILL,
                text = "",
                cornerRadius = 16f
            )
            val connector = createConnector(root, node, color = color, wireStyle = "orthogonal")
            strokes.add(connector)
            strokes.add(node)
        }

        return strokes
    }

    // 3. Rightward Organic Curved (Image 1, Card 2)
    private fun generateRightOrganic(center: Offset): List<Stroke> {
        val strokes = mutableListOf<Stroke>()
        val root = createNode(
            topLeft = Offset(center.x - 220f, center.y - 38f),
            width = 170f,
            height = 76f,
            borderColor = PALETTE_CYAN,
            fillColor = PALETTE_LIGHT_FILL,
            text = "Idea",
            role = "root",
            cornerRadius = 18f
        )
        strokes.add(root)

        val colors = listOf(PALETTE_YELLOW, PALETTE_BLUE, PALETTE_GREEN, PALETTE_RED)
        val yOffsets = listOf(-140f, -46f, 46f, 140f)
        val childX = center.x + 120f

        colors.forEachIndexed { i, color ->
            val node = createNode(
                topLeft = Offset(childX, center.y + yOffsets[i] - 30f),
                width = 150f,
                height = 60f,
                borderColor = color,
                fillColor = PALETTE_LIGHT_FILL,
                text = "",
                cornerRadius = 16f
            )
            val connector = createConnector(root, node, color = color, wireStyle = "curved")
            strokes.add(connector)
            strokes.add(node)
        }

        return strokes
    }

    // 4. Bilateral Curved
    private fun generateBilateralCurved(center: Offset): List<Stroke> {
        val strokes = mutableListOf<Stroke>()
        val root = createNode(
            topLeft = Offset(center.x - 85f, center.y - 36f),
            width = 170f,
            height = 72f,
            borderColor = PALETTE_GREEN,
            fillColor = PALETTE_LIGHT_FILL,
            text = "Concept",
            role = "root",
            cornerRadius = 18f
        )
        strokes.add(root)

        // Left 2 branches
        listOf(PALETTE_YELLOW to -80f, PALETTE_RED to 80f).forEach { (color, yOff) ->
            val node = createNode(
                topLeft = Offset(center.x - 300f, center.y + yOff - 30f),
                width = 140f,
                height = 60f,
                borderColor = color,
                fillColor = PALETTE_LIGHT_FILL,
                text = "",
                cornerRadius = 16f
            )
            strokes.add(createConnector(root, node, color, "curved"))
            strokes.add(node)
        }

        // Right 2 branches
        listOf(PALETTE_CYAN to -80f, PALETTE_ORANGE to 80f).forEach { (color, yOff) ->
            val node = createNode(
                topLeft = Offset(center.x + 160f, center.y + yOff - 30f),
                width = 140f,
                height = 60f,
                borderColor = color,
                fillColor = PALETTE_LIGHT_FILL,
                text = "",
                cornerRadius = 16f
            )
            strokes.add(createConnector(root, node, color, "curved"))
            strokes.add(node)
        }

        return strokes
    }

    // 5. Rightward Orthogonal Tree
    private fun generateRightOrthogonal(center: Offset): List<Stroke> {
        val strokes = mutableListOf<Stroke>()
        val root = createNode(
            topLeft = Offset(center.x - 220f, center.y - 38f),
            width = 170f,
            height = 76f,
            borderColor = PALETTE_BLUE,
            fillColor = PALETTE_LIGHT_FILL,
            text = "Project",
            role = "root",
            cornerRadius = 18f
        )
        strokes.add(root)

        val colors = listOf(PALETTE_YELLOW, PALETTE_BLUE, PALETTE_GREEN, PALETTE_RED)
        val yOffsets = listOf(-140f, -46f, 46f, 140f)
        val childX = center.x + 120f

        colors.forEachIndexed { i, color ->
            val node = createNode(
                topLeft = Offset(childX, center.y + yOffsets[i] - 30f),
                width = 150f,
                height = 60f,
                borderColor = color,
                fillColor = PALETTE_LIGHT_FILL,
                text = "",
                cornerRadius = 16f
            )
            val connector = createConnector(root, node, color = color, wireStyle = "orthogonal")
            strokes.add(connector)
            strokes.add(node)
        }

        return strokes
    }

    // 6. Downward Org Chart Hierarchy
    private fun generateDownwardOrg(center: Offset): List<Stroke> {
        val strokes = mutableListOf<Stroke>()
        val root = createNode(
            topLeft = Offset(center.x - 90f, center.y - 180f),
            width = 180f,
            height = 70f,
            borderColor = PALETTE_CYAN,
            fillColor = PALETTE_LIGHT_FILL,
            text = "Leader",
            role = "root",
            cornerRadius = 18f
        )
        strokes.add(root)

        val colColors = listOf(PALETTE_YELLOW, PALETTE_GREEN, PALETTE_BLUE, PALETTE_RED)
        val colXs = listOf(center.x - 360f, center.x - 120f, center.x + 120f, center.x + 360f)

        colColors.forEachIndexed { i, color ->
            val colX = colXs[i]
            val tier1 = createNode(
                topLeft = Offset(colX - 70f, center.y - 20f),
                width = 140f,
                height = 56f,
                borderColor = color,
                fillColor = PALETTE_LIGHT_FILL,
                text = "",
                cornerRadius = 14f
            )
            strokes.add(createConnector(root, tier1, color = color, wireStyle = "orthogonal"))
            strokes.add(tier1)

            val tier2 = createNode(
                topLeft = Offset(colX - 70f, center.y + 100f),
                width = 140f,
                height = 56f,
                borderColor = color,
                fillColor = PALETTE_LIGHT_FILL,
                text = "",
                cornerRadius = 14f
            )
            strokes.add(createConnector(tier1, tier2, color = color, wireStyle = "orthogonal"))
            strokes.add(tier2)
        }

        return strokes
    }

    // 7. Dual Bracket Tree
    private fun generateDualBracket(center: Offset): List<Stroke> {
        val strokes = mutableListOf<Stroke>()
        val root = createNode(
            topLeft = Offset(center.x - 85f, center.y - 36f),
            width = 170f,
            height = 72f,
            borderColor = PALETTE_BLUE,
            fillColor = PALETTE_LIGHT_FILL,
            text = "Category",
            role = "root",
            cornerRadius = 18f
        )
        strokes.add(root)

        // Left 3
        listOf(-90f, 0f, 90f).forEach { yOff ->
            val node = createNode(
                topLeft = Offset(center.x - 280f, center.y + yOff - 28f),
                width = 135f,
                height = 56f,
                borderColor = PALETTE_YELLOW,
                fillColor = PALETTE_LIGHT_FILL,
                text = "",
                cornerRadius = 14f
            )
            strokes.add(createConnector(root, node, PALETTE_YELLOW, "orthogonal"))
            strokes.add(node)
        }

        // Right 3
        listOf(-90f, 0f, 90f).forEach { yOff ->
            val node = createNode(
                topLeft = Offset(center.x + 145f, center.y + yOff - 28f),
                width = 135f,
                height = 56f,
                borderColor = PALETTE_BLUE,
                fillColor = PALETTE_LIGHT_FILL,
                text = "",
                cornerRadius = 14f
            )
            strokes.add(createConnector(root, node, PALETTE_BLUE, "orthogonal"))
            strokes.add(node)
        }

        return strokes
    }

    // 8. Downward Curved Radial
    private fun generateDownwardCurved(center: Offset): List<Stroke> {
        val strokes = mutableListOf<Stroke>()
        val root = createNode(
            topLeft = Offset(center.x - 90f, center.y - 180f),
            width = 180f,
            height = 70f,
            borderColor = PALETTE_CYAN,
            fillColor = PALETTE_LIGHT_FILL,
            text = "Overview",
            role = "root",
            cornerRadius = 18f
        )
        strokes.add(root)

        val colColors = listOf(PALETTE_YELLOW, PALETTE_GREEN, PALETTE_BLUE, PALETTE_RED)
        val colXs = listOf(center.x - 340f, center.x - 110f, center.x + 110f, center.x + 340f)

        colColors.forEachIndexed { i, color ->
            val node = createNode(
                topLeft = Offset(colXs[i] - 70f, center.y - 20f),
                width = 140f,
                height = 56f,
                borderColor = color,
                fillColor = PALETTE_LIGHT_FILL,
                text = "",
                cornerRadius = 14f
            )
            strokes.add(createConnector(root, node, color = color, wireStyle = "curved"))
            strokes.add(node)
        }

        return strokes
    }

    /**
     * Finds a position for a new box in the target direction that avoids overlapping any existing boxes.
     */
    fun findNonOverlappingPosition(
        initial: Offset,
        w: Float,
        h: Float,
        direction: String,
        existingBounds: List<Rect>,
        spacing: Float = 24f
    ): Offset {
        fun overlaps(pos: Offset): Boolean {
            val candRect = Rect(pos.x - spacing, pos.y - spacing, pos.x + w + spacing, pos.y + h + spacing)
            return existingBounds.any { eb ->
                candRect.left < eb.right && candRect.right > eb.left &&
                    candRect.top < eb.bottom && candRect.bottom > eb.top
            }
        }

        if (!overlaps(initial)) return initial

        val stepDir = when (direction.lowercase()) {
            "left" -> Offset(-(w + 40f), 0f)
            "right" -> Offset(w + 40f, 0f)
            "top" -> Offset(0f, -(h + 40f))
            "bottom" -> Offset(0f, h + 40f)
            else -> Offset(w + 40f, 0f)
        }

        val perpOffsets = when (direction.lowercase()) {
            "left", "right" -> listOf(Offset(0f, 85f), Offset(0f, -85f), Offset(0f, 170f), Offset(0f, -170f), Offset(0f, 255f), Offset(0f, -255f))
            else -> listOf(Offset(170f, 0f), Offset(-170f, 0f), Offset(340f, 0f), Offset(-340f, 0f), Offset(510f, 0f), Offset(-510f, 0f))
        }

        for (p in perpOffsets) {
            val testPos = initial + p
            if (!overlaps(testPos)) return testPos
        }

        for (step in 1..8) {
            val advanced = initial + Offset(stepDir.x * step, stepDir.y * step)
            if (!overlaps(advanced)) return advanced
            for (p in perpOffsets) {
                val testPos = advanced + p
                if (!overlaps(testPos)) return testPos
            }
        }
        return initial
    }

    /**
     * Creates a new connected branch in the given direction (Right, Left, Bottom, Top) without overlapping existing boxes.
     */
    fun createBranchInDirection(
        parent: Stroke,
        direction: String,
        branchIndex: Int,
        existingBounds: List<Rect> = emptyList(),
        wireStyle: String = "orthogonal"
    ): Pair<Stroke, Stroke> {
        val colors = listOf(PALETTE_PURPLE, PALETTE_CYAN, PALETTE_BLUE, PALETTE_PINK, PALETTE_ORANGE, PALETTE_GREEN)
        val color = colors[branchIndex % colors.size]

        val w = 150f
        val h = 60f
        val pb = parent.bounds

        // Alternate offset so consecutive branches in the same direction don't overlap
        val vOffset = if (branchIndex == 0) 0f else if (branchIndex % 2 == 1) ((branchIndex + 1) / 2) * 85f else -((branchIndex) / 2) * 85f
        val hOffset = if (branchIndex == 0) 0f else if (branchIndex % 2 == 1) ((branchIndex + 1) / 2) * 170f else -((branchIndex) / 2) * 170f

        val baseTopLeft = when (direction.lowercase()) {
            "left" -> Offset(pb.left - 130f - w, pb.top + vOffset)
            "bottom" -> Offset(pb.left + (pb.width - w) / 2f + hOffset, pb.bottom + 90f)
            "top" -> Offset(pb.left + (pb.width - w) / 2f + hOffset, pb.top - 90f - h)
            else -> Offset(pb.right + 130f, pb.top + vOffset) // "right" default
        }

        val allBounds = if (existingBounds.isEmpty()) listOf(pb) else (existingBounds + pb)
        val childTopLeft = findNonOverlappingPosition(baseTopLeft, w, h, direction, allBounds, spacing = 24f)

        val child = createNode(
            topLeft = childTopLeft,
            width = w,
            height = h,
            borderColor = color,
            fillColor = PALETTE_LIGHT_FILL,
            text = "",
            cornerRadius = 16f
        )

        val connector = createConnector(parent, child, color = color, wireStyle = wireStyle)
        return child to connector
    }

    /**
     * Calculates the stepped orthogonal Path for Compose Canvas
     */
    fun buildComposeOrthogonalPath(p1: Offset, p2: Offset): ComposePath {
        val path = ComposePath()
        path.moveTo(p1.x, p1.y)
        if (abs(p2.x - p1.x) >= abs(p2.y - p1.y)) {
            val midX = (p1.x + p2.x) / 2f
            path.lineTo(midX, p1.y)
            path.lineTo(midX, p2.y)
            path.lineTo(p2.x, p2.y)
        } else {
            val midY = (p1.y + p2.y) / 2f
            path.lineTo(p1.x, midY)
            path.lineTo(p2.x, midY)
            path.lineTo(p2.x, p2.y)
        }
        return path
    }

    data class CurvedControlPoints(
        val cx1: Float,
        val cy1: Float,
        val cx2: Float,
        val cy2: Float
    )

    fun calculateCurvedControlPoints(p1: Offset, p2: Offset): CurvedControlPoints {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        val isHorizontal = abs(dx) >= abs(dy)

        return if (isHorizontal) {
            val bow = if (abs(dy) < 8f) minOf(26f, abs(dx) * 0.16f) else 0f
            CurvedControlPoints(
                cx1 = p1.x + dx * 0.5f,
                cy1 = p1.y + bow,
                cx2 = p1.x + dx * 0.5f,
                cy2 = p2.y + bow
            )
        } else {
            val bow = if (abs(dx) < 8f) minOf(26f, abs(dy) * 0.16f) else 0f
            CurvedControlPoints(
                cx1 = p1.x + bow,
                cy1 = p1.y + dy * 0.5f,
                cx2 = p2.x + bow,
                cy2 = p1.y + dy * 0.5f
            )
        }
    }

    /**
     * Calculates the smooth cubic Bezier curved Path for Compose Canvas
     */
    fun buildComposeCurvedPath(p1: Offset, p2: Offset): ComposePath {
        val path = ComposePath()
        path.moveTo(p1.x, p1.y)
        val cp = calculateCurvedControlPoints(p1, p2)
        path.cubicTo(cp.cx1, cp.cy1, cp.cx2, cp.cy2, p2.x, p2.y)
        return path
    }

    /**
     * Calculates the stepped orthogonal Path for Android Native Canvas (BoardStageView)
     */
    fun buildNativeOrthogonalPath(p1: Offset, p2: Offset): android.graphics.Path {
        val path = android.graphics.Path()
        path.moveTo(p1.x, p1.y)
        if (abs(p2.x - p1.x) >= abs(p2.y - p1.y)) {
            val midX = (p1.x + p2.x) / 2f
            path.lineTo(midX, p1.y)
            path.lineTo(midX, p2.y)
            path.lineTo(p2.x, p2.y)
        } else {
            val midY = (p1.y + p2.y) / 2f
            path.lineTo(p1.x, midY)
            path.lineTo(p2.x, midY)
            path.lineTo(p2.x, p2.y)
        }
        return path
    }

    /**
     * Calculates the smooth cubic Bezier curved Path for Android Native Canvas (BoardStageView)
     */
    fun buildNativeCurvedPath(p1: Offset, p2: Offset): android.graphics.Path {
        val path = android.graphics.Path()
        path.moveTo(p1.x, p1.y)
        val cp = calculateCurvedControlPoints(p1, p2)
        path.cubicTo(cp.cx1, cp.cy1, cp.cx2, cp.cy2, p2.x, p2.y)
        return path
    }

    fun buildComposeOrthogonalPath(points: List<Offset>): ComposePath {
        if (points.size <= 2) return buildComposeOrthogonalPath(points.first(), points.last())
        val path = ComposePath()
        path.moveTo(points.first().x, points.first().y)
        for (i in 1 until points.size) {
            path.lineTo(points[i].x, points[i].y)
        }
        return path
    }

    fun buildComposeCurvedPath(points: List<Offset>): ComposePath {
        if (points.isEmpty()) return ComposePath()
        if (points.size <= 2) return buildComposeCurvedPath(points.first(), points.last())
        val path = ComposePath()
        path.moveTo(points.first().x, points.first().y)
        if (points.size == 3) {
            path.quadraticBezierTo(points[1].x, points[1].y, points[2].x, points[2].y)
            return path
        }
        for (i in 0 until points.size - 1) {
            val pt1 = points[i]
            val pt2 = points[i + 1]
            val cp = calculateCurvedControlPoints(pt1, pt2)
            path.cubicTo(cp.cx1, cp.cy1, cp.cx2, cp.cy2, pt2.x, pt2.y)
        }
        return path
    }

    fun buildNativeOrthogonalPath(points: List<Offset>): android.graphics.Path {
        if (points.size <= 2) return buildNativeOrthogonalPath(points.first(), points.last())
        val path = android.graphics.Path()
        path.moveTo(points.first().x, points.first().y)
        for (i in 1 until points.size) {
            path.lineTo(points[i].x, points[i].y)
        }
        return path
    }

    fun buildNativeCurvedPath(points: List<Offset>): android.graphics.Path {
        val path = android.graphics.Path()
        if (points.isEmpty()) return path
        if (points.size <= 2) return buildNativeCurvedPath(points.first(), points.last())
        path.moveTo(points.first().x, points.first().y)
        if (points.size == 3) {
            path.quadTo(points[1].x, points[1].y, points[2].x, points[2].y)
            return path
        }
        for (i in 0 until points.size - 1) {
            val pt1 = points[i]
            val pt2 = points[i + 1]
            val cp = calculateCurvedControlPoints(pt1, pt2)
            path.cubicTo(cp.cx1, cp.cy1, cp.cx2, cp.cy2, pt2.x, pt2.y)
        }
        return path
    }

    /**
     * Auto-arranges a mind map tree connected to [targetNodeId] in either Horizontal or Vertical hierarchy.
     */
    fun layoutTree(
        targetNodeId: String,
        strokes: List<Stroke>,
        isHorizontal: Boolean
    ): List<Stroke> {
        val targetNode = strokes.firstOrNull { it.id == targetNodeId } ?: return strokes
        val nodeMap = strokes.filter { it.strokeType != StrokeType.CONNECTOR }.associateBy { it.id }.toMutableMap()
        val connectors = strokes.filter { it.strokeType == StrokeType.CONNECTOR }

        // Find root: trace back linkToId to linkFromId
        var rootId = targetNodeId
        val visited = mutableSetOf<String>()
        while (rootId !in visited) {
            visited.add(rootId)
            val parentConn = connectors.firstOrNull { it.linkToId == rootId }
            if (parentConn != null && parentConn.linkFromId in nodeMap) {
                rootId = parentConn.linkFromId
            } else {
                break
            }
        }
        val rootNode = nodeMap[rootId] ?: targetNode

        // Build adjacency from connectors
        val childrenMap = mutableMapOf<String, MutableList<String>>()
        connectors.forEach { c ->
            if (c.linkFromId in nodeMap && c.linkToId in nodeMap) {
                childrenMap.getOrPut(c.linkFromId) { mutableListOf() }.add(c.linkToId)
            }
        }

        // Calculate subtree heights and widths
        fun getSubtreeSpan(nodeId: String): Float {
            val children = childrenMap[nodeId] ?: emptyList()
            if (children.isEmpty()) {
                val n = nodeMap[nodeId] ?: return 80f
                return if (isHorizontal) n.bounds.height.coerceAtLeast(60f) else n.bounds.width.coerceAtLeast(140f)
            }
            val gap = if (isHorizontal) 24f else 30f
            return children.sumOf { getSubtreeSpan(it).toDouble() }.toFloat() + (children.size - 1) * gap
        }

        // Layout recursive
        fun layoutNode(nodeId: String, parentOffset: Offset) {
            val children = childrenMap[nodeId] ?: emptyList()
            if (children.isEmpty()) return

            val gap = if (isHorizontal) 24f else 30f
            val totalSpan = children.sumOf { getSubtreeSpan(it).toDouble() }.toFloat() + (children.size - 1) * gap
            var currentSpanOffset = -totalSpan / 2f

            children.forEach { childId ->
                val child = nodeMap[childId] ?: return@forEach
                val childWidth = child.bounds.width.coerceAtLeast(130f)
                val childHeight = child.bounds.height.coerceAtLeast(56f)
                val childSubtreeSpan = getSubtreeSpan(childId)
                val childCenterSpan = currentSpanOffset + childSubtreeSpan / 2f

                val newTopLeft = if (isHorizontal) {
                    val childX = parentOffset.x + (nodeMap[nodeId]?.bounds?.width ?: 160f) + 90f
                    val childY = parentOffset.y + childCenterSpan - childHeight / 2f
                    Offset(childX, childY)
                } else {
                    val childX = parentOffset.x + childCenterSpan - childWidth / 2f
                    val childY = parentOffset.y + (nodeMap[nodeId]?.bounds?.height ?: 64f) + 80f
                    Offset(childX, childY)
                }

                val movedChild = child.copy(
                    points = listOf(newTopLeft, Offset(newTopLeft.x + childWidth, newTopLeft.y + childHeight))
                )
                nodeMap[childId] = movedChild

                val childAnchor = if (isHorizontal) {
                    Offset(newTopLeft.x, newTopLeft.y + childHeight / 2f)
                } else {
                    Offset(newTopLeft.x + childWidth / 2f, newTopLeft.y)
                }
                layoutNode(childId, childAnchor)

                currentSpanOffset += childSubtreeSpan + gap
            }
        }

        val rootAnchor = if (isHorizontal) {
            Offset(rootNode.bounds.left, rootNode.bounds.center.y)
        } else {
            Offset(rootNode.bounds.center.x, rootNode.bounds.top)
        }
        layoutNode(rootId, rootAnchor)

        // Update all connector anchors
        val updatedStrokes = strokes.map { stroke ->
            if (stroke.strokeType == StrokeType.CONNECTOR) {
                val from = nodeMap[stroke.linkFromId]
                val to = nodeMap[stroke.linkToId]
                if (from != null && to != null) {
                    val (p1, p2) = calculateAnchors(from.bounds, to.bounds)
                    stroke.copy(points = listOf(p1, p2))
                } else stroke
            } else {
                nodeMap[stroke.id] ?: stroke
            }
        }
        return updatedStrokes
    }

    /**
     * Calculates a non-overlapping center position for a newly created mind map template.
     * Prevents successive mind map templates from colliding with existing ones.
     */
    fun calculateNonOverlappingCenter(
        existingStrokes: List<Stroke>,
        requestedCenter: Offset,
        halfWidth: Float = 360f,
        halfHeight: Float = 220f,
        margin: Float = 100f
    ): Offset {
        val existingBounds = existingStrokes.filter {
            it.stickerKind.startsWith("mindmap:") || it.stickerKind.startsWith("flow:") || it.strokeType == StrokeType.CONNECTOR
        }.map { it.bounds }

        if (existingBounds.isEmpty()) return requestedCenter

        fun collides(c: Offset): Boolean {
            val cand = Rect(
                c.x - halfWidth - margin,
                c.y - halfHeight - margin,
                c.x + halfWidth + margin,
                c.y + halfHeight + margin
            )
            return existingBounds.any { it.overlaps(cand) }
        }

        if (!collides(requestedCenter)) return requestedCenter

        // Try placing to the right of existing cluster
        val maxRight = existingBounds.maxOf { it.right }
        val rightCandidate = Offset(maxRight + margin + halfWidth, requestedCenter.y)
        if (!collides(rightCandidate)) return rightCandidate

        // Try placing below existing cluster
        val maxBottom = existingBounds.maxOf { it.bottom }
        val bottomCandidate = Offset(requestedCenter.x, maxBottom + margin + halfHeight)
        if (!collides(bottomCandidate)) return bottomCandidate

        // Fallback: place further to the right
        return rightCandidate
    }
}
