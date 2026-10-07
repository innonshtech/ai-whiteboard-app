package com.avitrisphare.whiteboard.data.local

import androidx.room.TypeConverter
import com.avitrisphare.whiteboard.domain.model.CanvasBackground

class Converters {
    @TypeConverter
    fun fromCanvasBackground(value: CanvasBackground): String = value.key

    @TypeConverter
    fun toCanvasBackground(value: String): CanvasBackground = CanvasBackground.fromKey(value)
}
