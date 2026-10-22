package com.avitrisphare.whiteboard.presentation.canvas.utility

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

enum class UtilityToolKind(val label: String) {
    TIMER("Timer"),
    STOPWATCH("Stopwatch"),
    CALCULATOR("Calculator"),
    PERIODIC_TABLE("Periodic Table")
}

data class TimerUiState(
    val totalSeconds: Int = 300,
    val remainingSeconds: Int = 300,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false,
    val isMinimized: Boolean = false,
    val isVisible: Boolean = false,
    val position: Offset = Offset(60f, 120f)
) {
    val progress: Float
        get() = if (totalSeconds > 0) (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f) else 0f

    val formattedTime: String
        get() {
            val mins = remainingSeconds / 60
            val secs = remainingSeconds % 60
            return "%02d:%02d".format(mins, secs)
        }
}

data class StopwatchUiState(
    val elapsedMillis: Long = 0L,
    val isRunning: Boolean = false,
    val laps: List<Long> = emptyList(),
    val isMinimized: Boolean = false,
    val isVisible: Boolean = false,
    val position: Offset = Offset(80f, 160f)
) {
    val formattedTime: String
        get() {
            val totalSeconds = elapsedMillis / 1000
            val mins = totalSeconds / 60
            val secs = totalSeconds % 60
            val hundredths = (elapsedMillis % 1000) / 10
            return "%02d:%02d.%02d".format(mins, secs, hundredths)
        }
}

data class CalculatorUiState(
    val expression: String = "",
    val result: String = "0",
    val isScientific: Boolean = false,
    val isDegreeMode: Boolean = true,
    val history: List<Pair<String, String>> = emptyList(),
    val isMinimized: Boolean = false,
    val isVisible: Boolean = false,
    val position: Offset = Offset(100f, 100f)
)

enum class ElementCategory(val displayName: String, val color: Color) {
    REACTIVE_NONMETAL("Reactive Nonmetal", Color(0xFF22C55E)),
    NOBLE_GAS("Noble Gas", Color(0xFFA855F7)),
    ALKALI_METAL("Alkali Metal", Color(0xFFEF4444)),
    ALKALINE_EARTH("Alkaline Earth Metal", Color(0xFFF97316)),
    METALLOID("Metalloid", Color(0xFF14B8A6)),
    POST_TRANSITION("Post-Transition Metal", Color(0xFF3B82F6)),
    TRANSITION_METAL("Transition Metal", Color(0xFF6366F1)),
    LANTHANIDE("Lanthanide", Color(0xFFEC4899)),
    ACTINIDE("Actinide", Color(0xFFF43F5E)),
    UNKNOWN("Unknown Properties", Color(0xFF94A3B8))
}

data class ChemicalElement(
    val atomicNumber: Int,
    val symbol: String,
    val name: String,
    val atomicMass: Double,
    val period: Int,
    val group: Int?, // null for lanthanides/actinides
    val category: ElementCategory,
    val phase: String, // Solid, Liquid, Gas
    val electronConfig: String,
    val summary: String = ""
) {
    val massNumber: Int
        get() = kotlin.math.round(atomicMass).toInt()
}

data class PeriodicTableUiState(
    val selectedElement: ChemicalElement? = null,
    val searchQuery: String = "",
    val selectedCategory: ElementCategory? = null,
    val isMinimized: Boolean = false,
    val isVisible: Boolean = false,
    val position: Offset = Offset(40f, 80f),
    val zoomScale: Float = 1.0f,
    val isDarkMode: Boolean = true
)
