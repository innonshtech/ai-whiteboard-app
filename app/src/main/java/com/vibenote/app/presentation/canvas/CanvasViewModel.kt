package com.vibenote.app.presentation.canvas

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vibenote.app.data.local.StrokeDto
import com.vibenote.app.data.local.toDomain
import com.vibenote.app.data.local.toDto
import com.vibenote.app.domain.model.CanvasBackground
import com.vibenote.app.domain.model.Stroke
import com.vibenote.app.domain.model.StrokeType
import com.vibenote.app.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

enum class WhiteboardTool(val label: String, val shortcut: String) {
    LOCK("Lock", ""),
    HAND("Hand", "H"),
    SELECT("Selection", "V"),
    RECTANGLE("Rectangle", "R"),
    DIAMOND("Diamond", "D"),
    CIRCLE("Circle", "O"),
    ARROW("Arrow", "A"),
    LINE("Line", "L"),
    PEN("Draw", "P"),
    TEXT("Text", "T"),
    STICKY_NOTE("Sticky Note", "N"),
    ERASER("Eraser", "E"),
    FRAME("Frame tool", "F"),
    LASER("Laser pointer", "K"),
    BUCKET_FILL("Bucket fill", "B"),
    LASSO("Lasso selection", "")
}

data class CanvasState(
    val strokes: List<Stroke> = emptyList(),
    val currentStroke: Stroke? = null,
    val selectedColor: Int = Color.White.toArgb(),
    val fillColor: Int = 0,
    val strokeWidth: Float = 4f,
    val currentTool: WhiteboardTool = WhiteboardTool.SELECT,
    val isToolLocked: Boolean = false,
    val isDrawToShapeEnabled: Boolean = false,
    val laserPoints: List<Offset> = emptyList(),
    val selectedStrokeIds: Set<String> = emptySet(),
    val isEraser: Boolean = false,
    val isHighlighter: Boolean = false,
    val isShapeMode: Boolean = false,
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val noteId: String = "",
    val noteTitle: String = "Untitled",
    val isLoading: Boolean = false,
    val canvasBackground: CanvasBackground = CanvasBackground.DARK,
    val contentJson: String = "",
    val isLiveCollabActive: Boolean = false,
    val collabRoomId: String = "",
    val userName: String = "Swift Penguin",
    val connectedCollaborators: List<String> = emptyList(),
    val errorMessage: String? = null
)

val PEN_COLORS = listOf(
    0xFFFFFFFF.toInt(), // White
    0xFF3ECF8E.toInt(), // Brand Green
    0xFFFF6B6B.toInt(), // Red
    0xFF4ECDC4.toInt(), // Teal
    0xFFFFE66D.toInt(), // Yellow
    0xFF95E1D0.toInt(), // Mint
    0xFFF38181.toInt(), // Coral
    0xFFAA96DA.toInt() // Lavender
)

val STICKY_COLORS = listOf(
    0xFFFFF9C4.toInt(), // Soft Yellow
    0xFFFFCCBC.toInt(), // Soft Coral
    0xFFC8E6C9.toInt(), // Soft Mint
    0xFFE1BEE7.toInt(), // Soft Lavender
    0xFFB3E5FC.toInt()  // Soft Sky Blue
)

@HiltViewModel
class CanvasViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CanvasState())
    val state: StateFlow<CanvasState> = _state.asStateFlow()

    private val undoStack = mutableListOf<List<Stroke>>()
    private val redoStack = mutableListOf<List<Stroke>>()
    private val gson = Gson()
    private val MAX_UNDO = 50
    private var saveJob: Job? = null
    private var laserJob: Job? = null

    fun setTool(tool: WhiteboardTool) {
        if (tool == WhiteboardTool.LOCK) {
            _state.update { it.copy(isToolLocked = !it.isToolLocked) }
            return
        }
        _state.update {
            it.copy(
                currentTool = tool,
                isEraser = tool == WhiteboardTool.ERASER,
                isHighlighter = false,
                isShapeMode = tool in listOf(
                    WhiteboardTool.RECTANGLE,
                    WhiteboardTool.DIAMOND,
                    WhiteboardTool.CIRCLE,
                    WhiteboardTool.ARROW,
                    WhiteboardTool.LINE,
                    WhiteboardTool.FRAME
                )
            )
        }
    }

    fun toggleDrawToShape() {
        _state.update { it.copy(isDrawToShapeEnabled = !it.isDrawToShapeEnabled) }
    }

    fun addLaserPoint(point: Offset) {
        laserJob?.cancel()
        _state.update { it.copy(laserPoints = (it.laserPoints + point).takeLast(40)) }
        laserJob = viewModelScope.launch {
            delay(1200)
            _state.update { it.copy(laserPoints = emptyList()) }
        }
    }

    fun selectStrokeAt(worldPoint: Offset, isMulti: Boolean = false): Boolean {
        val strokes = _state.value.strokes
        val hit = strokes.lastOrNull { stroke ->
            if (stroke.strokeType in listOf(StrokeType.STICKY_NOTE, StrokeType.TEXT, StrokeType.WEB_EMBED, StrokeType.IMAGE, StrokeType.FRAME)) {
                stroke.bounds.contains(worldPoint)
            } else {
                CanvasTransform.strokeIntersectsPoint(stroke, worldPoint, thresholdRadius = 16f)
            }
        }
        if (hit != null) {
            _state.update { s ->
                val newSelection = if (isMulti) s.selectedStrokeIds + hit.id else setOf(hit.id)
                s.copy(selectedStrokeIds = newSelection)
            }
            return true
        } else {
            if (!isMulti) {
                _state.update { it.copy(selectedStrokeIds = emptySet()) }
            }
            return false
        }
    }

    fun lassoSelect(lassoPoints: List<Offset>) {
        if (lassoPoints.size < 3) return
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
        val selected = _state.value.strokes.filter { stroke ->
            lassoRect.overlaps(stroke.bounds)
        }.map { it.id }.toSet()

        _state.update { it.copy(selectedStrokeIds = selected) }
    }

    fun bucketFillAt(worldPoint: Offset, fillColor: Int) {
        val strokes = _state.value.strokes
        val hitIndex = strokes.indexOfLast { stroke ->
            stroke.bounds.contains(worldPoint) || CanvasTransform.strokeIntersectsPoint(stroke, worldPoint, thresholdRadius = 24f)
        }
        if (hitIndex != -1) {
            saveToUndoStack()
            _state.update { s ->
                val updated = s.strokes.toMutableList()
                val target = updated[hitIndex]
                updated[hitIndex] = target.copy(fillColor = fillColor)
                s.copy(strokes = updated, canUndo = true, canRedo = false)
            }
            redoStack.clear()
            scheduleSave()
        }
    }

    fun clearSelection() {
        _state.update { it.copy(selectedStrokeIds = emptySet()) }
    }

    fun moveSelectedStrokes(delta: Offset) {
        if (delta == Offset.Zero || _state.value.selectedStrokeIds.isEmpty()) return
        val selectedIds = _state.value.selectedStrokeIds
        _state.update { s ->
            val updated = s.strokes.map { stroke ->
                if (stroke.id in selectedIds) {
                    val newPoints = stroke.points.map { Offset(it.x + delta.x, it.y + delta.y) }
                    stroke.copy(points = newPoints)
                } else {
                    stroke
                }
            }
            s.copy(strokes = updated)
        }
        scheduleSave()
    }

    fun deleteSelectedStrokes() {
        val selectedIds = _state.value.selectedStrokeIds
        if (selectedIds.isEmpty()) return
        saveToUndoStack()
        _state.update { s ->
            s.copy(
                strokes = s.strokes.filterNot { it.id in selectedIds },
                selectedStrokeIds = emptySet(),
                canUndo = true,
                canRedo = false
            )
        }
        redoStack.clear()
        scheduleSave()
    }

    fun addTextOrSticky(text: String, worldPoint: Offset, isSticky: Boolean, color: Int = 0xFFFFFFFF.toInt(), fill: Int = 0) {
        if (text.isBlank()) return
        saveToUndoStack()
        val stickyFill = if (isSticky && fill == 0) 0xFFFFF9C4.toInt() else fill
        val textColor = if (isSticky) 0xFF171717.toInt() else color
        val newStroke = Stroke(
            points = listOf(worldPoint),
            colorValue = textColor,
            strokeWidth = 2f,
            strokeType = if (isSticky) StrokeType.STICKY_NOTE else StrokeType.TEXT,
            text = text,
            fillColor = stickyFill
        )
        _state.update { s ->
            s.copy(
                strokes = s.strokes + newStroke,
                selectedStrokeIds = setOf(newStroke.id),
                currentTool = if (s.isToolLocked) s.currentTool else WhiteboardTool.SELECT,
                canUndo = true,
                canRedo = false
            )
        }
        redoStack.clear()
        scheduleSave()
    }

    fun addImage(uriString: String, worldPoint: Offset) {
        if (uriString.isBlank()) return
        saveToUndoStack()
        val endPoint = Offset(worldPoint.x + 300f, worldPoint.y + 220f)
        val newStroke = Stroke(
            points = listOf(worldPoint, endPoint),
            strokeType = StrokeType.IMAGE,
            imageUri = uriString
        )
        _state.update { s ->
            s.copy(
                strokes = s.strokes + newStroke,
                selectedStrokeIds = setOf(newStroke.id),
                currentTool = WhiteboardTool.SELECT,
                canUndo = true,
                canRedo = false
            )
        }
        redoStack.clear()
        scheduleSave()
    }

    fun addWebEmbed(url: String, worldPoint: Offset) {
        if (url.isBlank()) return
        saveToUndoStack()
        val newStroke = Stroke(
            points = listOf(worldPoint),
            strokeType = StrokeType.WEB_EMBED,
            text = url,
            colorValue = 0xFF6366F1.toInt()
        )
        _state.update { s ->
            s.copy(
                strokes = s.strokes + newStroke,
                selectedStrokeIds = setOf(newStroke.id),
                currentTool = WhiteboardTool.SELECT,
                canUndo = true,
                canRedo = false
            )
        }
        redoStack.clear()
        scheduleSave()
    }

    fun updateStrokeText(id: String, newText: String) {
        saveToUndoStack()
        _state.update { s ->
            val updated = s.strokes.map {
                if (it.id == id) it.copy(text = newText) else it
            }
            s.copy(strokes = updated, canUndo = true, canRedo = false)
        }
        scheduleSave()
    }

    fun applyShapeRecognition(points: List<Offset>): Stroke? {
        val result = ShapeRecognitionHelper.recognize(points) ?: return null
        if (result.confidence < 0.7f) return null
        return Stroke(
            points = points,
            colorValue = _state.value.selectedColor,
            strokeWidth = _state.value.strokeWidth,
            strokeType = result.strokeType
        )
    }

    fun loadNote(noteId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoading = true, noteId = noteId) }
            
            val note = noteRepository.getNoteById(noteId)
            if (note != null) {
                _state.update { it.copy(
                    noteTitle = note.title,
                    canvasBackground = note.canvasBackground,
                    contentJson = note.contentJson ?: ""
                ) }
                
                val strokesFile = File(context.filesDir, "strokes_$noteId.json")
                if (strokesFile.exists()) {
                    try {
                        if (strokesFile.length() > 10_000_000) {
                            _state.update { it.copy(strokes = emptyList(), isLoading = false, errorMessage = "Note file too large") }
                            return@launch
                        }
                        val json = strokesFile.readText()
                        val type = object : TypeToken<List<StrokeDto>>() {}.type
                        val dtos: List<StrokeDto> = gson.fromJson(json, type) ?: emptyList()
                        val strokes = dtos.take(10000).map { it.toDomain() }
                        _state.update { it.copy(strokes = strokes, isLoading = false) }
                    } catch (e: Exception) {
                        _state.update { it.copy(strokes = emptyList(), isLoading = false, errorMessage = "Failed to load note") }
                    }
                } else {
                    _state.update { it.copy(isLoading = false) }
                }
            } else {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun startStroke(stroke: Stroke) {
        _state.update { it.copy(currentStroke = stroke) }
    }

    fun updateStroke(points: List<Offset>) {
        _state.update { current ->
            current.copy(currentStroke = current.currentStroke?.copy(points = points))
        }
    }

    fun finishStroke() {
        val currentStroke = _state.value.currentStroke ?: return
        saveToUndoStack()
        _state.update { s ->
            s.copy(
                strokes = s.strokes + currentStroke,
                currentStroke = null,
                canUndo = true,
                canRedo = false
            )
        }
        redoStack.clear()
        scheduleSave()
    }

    fun toggleEraser() {
        _state.update { it.copy(isEraser = !it.isEraser, isHighlighter = false, isShapeMode = false) }
    }

    fun toggleHighlighter() {
        _state.update { it.copy(isHighlighter = !it.isHighlighter, isEraser = false, isShapeMode = false) }
    }

    fun toggleShapeMode() {
        _state.update { it.copy(isShapeMode = !it.isShapeMode, isEraser = false, isHighlighter = false) }
    }

    fun setColor(color: Int) {
        _state.update { it.copy(selectedColor = color, isEraser = false, isHighlighter = false) }
    }

    fun setStrokeWidth(width: Float) {
        _state.update { it.copy(strokeWidth = width) }
    }

    fun updateTransform(scale: Float, offsetX: Float, offsetY: Float) {
        _state.update { it.copy(scale = scale, offsetX = offsetX, offsetY = offsetY) }
    }

    fun resetTransform() {
        _state.update { it.copy(scale = 1f, offsetX = 0f, offsetY = 0f) }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val currentStrokes = _state.value.strokes
        redoStack.add(currentStrokes)
        val previousStrokes = undoStack.removeLastOrNull() ?: emptyList()
        _state.update { s ->
            s.copy(
                strokes = previousStrokes,
                canUndo = undoStack.isNotEmpty(),
                canRedo = true
            )
        }
        scheduleSave()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val currentStrokes = _state.value.strokes
        undoStack.add(currentStrokes)
        val nextStrokes = redoStack.removeLastOrNull() ?: emptyList()
        _state.update { s ->
            s.copy(
                strokes = nextStrokes,
                canUndo = true,
                canRedo = redoStack.isNotEmpty()
            )
        }
        scheduleSave()
    }

    fun clearCanvas() {
        saveToUndoStack()
        _state.update { s ->
            s.copy(
                strokes = emptyList(),
                canUndo = true,
                canRedo = false
            )
        }
        redoStack.clear()
        scheduleSave()
    }

    fun updateTitle(newTitle: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val noteId = _state.value.noteId
            if (noteId.isNotEmpty()) {
                val note = noteRepository.getNoteById(noteId)
                if (note != null) {
                    val updatedNote = note.copy(title = newTitle, updatedAt = System.currentTimeMillis())
                    noteRepository.updateNote(updatedNote)
                    withContext(Dispatchers.Main) {
                        _state.update { it.copy(noteTitle = newTitle) }
                    }
                }
            }
        }
    }

    fun deleteNote(onDeleted: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val noteId = _state.value.noteId
            if (noteId.isNotEmpty()) {
                val note = noteRepository.getNoteById(noteId)
                if (note != null) {
                    noteRepository.deleteNote(note)
                    val strokesFile = File(context.filesDir, "strokes_$noteId.json")
                    if (strokesFile.exists()) {
                        strokesFile.delete()
                    }
                    withContext(Dispatchers.Main) { onDeleted() }
                }
            }
        }
    }

    private fun saveToUndoStack() {
        undoStack.add(_state.value.strokes)
        if (undoStack.size > MAX_UNDO) undoStack.removeAt(0)
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(1500)
            val noteId = _state.value.noteId
            if (noteId.isEmpty()) return@launch
            val strokes = _state.value.strokes
            val timestamp = System.currentTimeMillis()
            
            withContext(Dispatchers.IO) {
                try {
                    val strokesFile = File(context.filesDir, "strokes_$noteId.json")
                    val dtos = strokes.map { it.toDto() }
                    val json = gson.toJson(dtos)
                    strokesFile.writeText(json)
                    noteRepository.updateNoteTimestamp(noteId, timestamp)
                } catch (e: Exception) {
                    _state.update { it.copy(errorMessage = "Failed to save note: ${e.message}") }
                }
            }
        }
    }

    suspend fun saveNow() {
        saveJob?.cancel()
        val noteId = _state.value.noteId
        if (noteId.isEmpty()) return
        val strokes = _state.value.strokes
        val timestamp = System.currentTimeMillis()
        withContext(Dispatchers.IO) {
            try {
                val strokesFile = File(context.filesDir, "strokes_$noteId.json")
                val dtos = strokes.map { it.toDto() }
                val json = gson.toJson(dtos)
                strokesFile.writeText(json)
                noteRepository.updateNoteTimestamp(noteId, timestamp)
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = "Failed to save note: ${e.message}") }
            }
        }
    }

    private var isErasingActive = false

    fun eraseAt(worldPoint: Offset, thresholdRadius: Float = 24f): Boolean {
        val currentStrokes = _state.value.strokes
        if (currentStrokes.isEmpty()) return false

        val remaining = currentStrokes.filterNot { stroke ->
            CanvasTransform.strokeIntersectsPoint(stroke, worldPoint, thresholdRadius)
        }

        if (remaining.size != currentStrokes.size) {
            if (!isErasingActive) {
                saveToUndoStack()
                isErasingActive = true
            }
            _state.update { s ->
                s.copy(
                    strokes = remaining,
                    canUndo = true,
                    canRedo = false
                )
            }
            redoStack.clear()
            scheduleSave()
            return true
        }
        return false
    }

    fun finishErasing() {
        isErasingActive = false
    }

    fun fitToContent(viewportWidth: Float, viewportHeight: Float) {
        if (viewportWidth <= 0f || viewportHeight <= 0f) return
        val contentBounds = CanvasTransform.calculateContentBounds(_state.value.strokes)
        val (targetScale, targetX, targetY) = CanvasTransform.calculateFitTransform(
            contentBounds = contentBounds,
            viewportSize = androidx.compose.ui.geometry.Size(viewportWidth, viewportHeight),
            paddingPx = 80f
        )
        _state.update { it.copy(scale = targetScale, offsetX = targetX, offsetY = targetY) }
    }

    fun exportAsPng(context: Context, onExported: (Uri) -> Unit) {
        viewModelScope.launch(Dispatchers.Default) {
            val strokes = _state.value.strokes
            val contentBounds = CanvasTransform.calculateContentBounds(strokes)

            var bitmapWidth = 2048
            var bitmapHeight = 1536
            var scaleFactor = 1f
            var transX = 0f
            var transY = 0f

            if (contentBounds != null && contentBounds.width > 0 && contentBounds.height > 0) {
                val padding = 100f
                val paddedWidth = contentBounds.width + padding * 2
                val paddedHeight = contentBounds.height + padding * 2

                val maxDim = 3840f
                val fitScale = kotlin.math.min(1f, maxDim / kotlin.math.max(paddedWidth, paddedHeight))
                bitmapWidth = (paddedWidth * fitScale).toInt().coerceIn(800, 4096)
                bitmapHeight = (paddedHeight * fitScale).toInt().coerceIn(600, 4096)

                scaleFactor = bitmapWidth.toFloat() / paddedWidth
                transX = -(contentBounds.left - padding) * scaleFactor
                transY = -(contentBounds.top - padding) * scaleFactor
            }

            val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)

            val bgArgb = when (_state.value.canvasBackground) {
                CanvasBackground.WHITE -> 0xFFFFFFFF.toInt()
                else -> 0xFF171717.toInt()
            }
            canvas.drawColor(bgArgb)

            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.STROKE
                strokeCap = android.graphics.Paint.Cap.ROUND
                strokeJoin = android.graphics.Paint.Join.ROUND
            }

            strokes.forEach { stroke ->
                val pointsList = stroke.points
                if (pointsList.size >= 2) {
                    paint.color = if (stroke.isHighlighter) {
                        val c = Color(stroke.colorValue).copy(alpha = 0.4f)
                        c.toArgb()
                    } else {
                        stroke.colorValue
                    }
                    paint.strokeWidth = stroke.strokeWidth * scaleFactor

                    val path = android.graphics.Path()
                    val p0x = pointsList[0].x * scaleFactor + transX
                    val p0y = pointsList[0].y * scaleFactor + transY
                    path.moveTo(p0x, p0y)
                    for (i in 1 until pointsList.size) {
                        val px = pointsList[i].x * scaleFactor + transX
                        val py = pointsList[i].y * scaleFactor + transY
                        path.lineTo(px, py)
                    }
                    canvas.drawPath(path, paint)
                }
            }

            val filename = "bloom_${_state.value.noteTitle.replace(" ", "_")}_${System.currentTimeMillis()}.png"
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Bloom")
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
                bitmap.recycle()
                withContext(Dispatchers.Main) { onExported(it) }
            } ?: bitmap.recycle()
        }
    }

    fun setCanvasBackground(background: CanvasBackground) {
        val noteId = _state.value.noteId
        if (noteId.isEmpty()) return
        
        _state.update { it.copy(canvasBackground = background) }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val note = noteRepository.getNoteById(noteId)
                if (note != null) {
                    val updatedNote = note.copy(
                        canvasBackground = background,
                        updatedAt = System.currentTimeMillis()
                    )
                    noteRepository.updateNote(updatedNote)
                }
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = "Failed to update background: ${e.message}") }
            }
        }
    }

    fun startLiveSession() {
        val roomId = java.util.UUID.randomUUID().toString().replace("-", "").take(16)
        _state.update {
            it.copy(
                isLiveCollabActive = true,
                collabRoomId = roomId,
                connectedCollaborators = listOf(it.userName + " (You)")
            )
        }
    }

    fun stopLiveSession() {
        _state.update {
            it.copy(
                isLiveCollabActive = false,
                collabRoomId = "",
                connectedCollaborators = emptyList()
            )
        }
    }

    fun setUserName(name: String) {
        _state.update { it.copy(userName = name.ifBlank { "Swift Penguin" }) }
    }

    fun exportNoteJson(onSaved: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val dtos = _state.value.strokes.map { it.toDto() }
            val json = gson.toJson(dtos)
            val file = File(context.filesDir, "export_${_state.value.noteTitle.replace(" ", "_")}.bloom")
            file.writeText(json)
            withContext(Dispatchers.Main) { onSaved(file.absolutePath) }
        }
    }
}