package com.avitrisphare.whiteboard.presentation.canvas.utility

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.avitrisphare.whiteboard.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CalculatorFloatingPanel(
    state: CalculatorUiState,
    onStateChange: (CalculatorUiState) -> Unit,
    onBringToFront: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val engine = remember(state.isDegreeMode) { CalculatorEngine(state.isDegreeMode) }

    fun appendInput(token: String) {
        val newExpr = state.expression + token
        onStateChange(state.copy(expression = newExpr))
    }

    fun calculate() {
        if (state.expression.isBlank()) return
        when (val eval = engine.evaluate(state.expression)) {
            is CalculatorEngine.EvalResult.Success -> {
                onStateChange(
                    state.copy(
                        result = eval.formatted,
                        history = listOf(state.expression to eval.formatted) + state.history.take(9)
                    )
                )
            }
            is CalculatorEngine.EvalResult.Error -> {
                onStateChange(state.copy(result = eval.message))
            }
        }
    }

    fun clear() {
        onStateChange(state.copy(expression = "", result = "0"))
    }

    fun backspace() {
        if (state.expression.isNotEmpty()) {
            val newExpr = state.expression.dropLast(1)
            onStateChange(state.copy(expression = newExpr))
        }
    }

    val panelWidth = if (state.isScientific) 275.dp else 215.dp

    FloatingToolPanel(
        title = if (state.isScientific) "Scientific Calc" else "Calculator",
        icon = Icons.Default.Calculate,
        position = state.position,
        isMinimized = state.isMinimized,
        onPositionChange = { onStateChange(state.copy(position = it)) },
        onToggleMinimize = { onStateChange(state.copy(isMinimized = !state.isMinimized)) },
        onClose = { onStateChange(state.copy(isVisible = false)) },
        minimizedSummary = "Calc: ${state.result}",
        onBringToFront = onBringToFront,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.width(panelWidth),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Display Screen
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF111622))
                    .border(1.dp, Color(0xFF283248), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = if (state.expression.isEmpty()) " " else state.expression,
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = state.result,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        textAlign = TextAlign.End
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // Mode Selector Bar: Basic/Scientific and Deg/Rad
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF20293A))
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (!state.isScientific) Color(0xFF2563EB) else Color.Transparent)
                            .clickable { onStateChange(state.copy(isScientific = false)) }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(stringResource(R.string.calc_basic), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (state.isScientific) Color(0xFF2563EB) else Color.Transparent)
                            .clickable { onStateChange(state.copy(isScientific = true)) }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(stringResource(R.string.calc_scientific), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }

                if (state.isScientific) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF283248))
                            .clickable { onStateChange(state.copy(isDegreeMode = !state.isDegreeMode)) }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            if (state.isDegreeMode) "DEG" else "RAD",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF60A5FA)
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // Keypad Grid
            if (state.isScientific) {
                // 5-Column Scientific Keypad
                val rows = listOf(
                    listOf("sin", "cos", "tan", "(", ")"),
                    listOf("asin", "acos", "atan", "π", "e"),
                    listOf("sqrt", "^", "log", "ln", "C"),
                    listOf("7", "8", "9", "÷", "⌫"),
                    listOf("4", "5", "6", "×", "!"),
                    listOf("1", "2", "3", "-", "%"),
                    listOf("0", ".", "+/-", "+", "=")
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    rows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            row.forEach { key ->
                                CalcButton(
                                    label = key,
                                    height = 28.dp,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        handleKey(key, ::appendInput, ::calculate, ::clear, ::backspace, state, onStateChange)
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                // 4-Column Basic Keypad
                val rows = listOf(
                    listOf("C", "⌫", "%", "÷"),
                    listOf("7", "8", "9", "×"),
                    listOf("4", "5", "6", "-"),
                    listOf("1", "2", "3", "+"),
                    listOf("+/-", "0", ".", "=")
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    rows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            row.forEach { key ->
                                CalcButton(
                                    label = key,
                                    height = 32.dp,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        handleKey(key, ::appendInput, ::calculate, ::clear, ::backspace, state, onStateChange)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun handleKey(
    key: String,
    appendInput: (String) -> Unit,
    calculate: () -> Unit,
    clear: () -> Unit,
    backspace: () -> Unit,
    state: CalculatorUiState,
    onStateChange: (CalculatorUiState) -> Unit
) {
    when (key) {
        "=" -> calculate()
        "C" -> clear()
        "⌫" -> backspace()
        "+/-" -> {
            if (state.expression.startsWith("-")) {
                onStateChange(state.copy(expression = state.expression.removePrefix("-")))
            } else {
                onStateChange(state.copy(expression = "-(" + state.expression + ")"))
            }
        }
        "sin", "cos", "tan", "asin", "acos", "atan", "log", "ln" -> appendInput("$key(")
        "sqrt" -> appendInput("sqrt(")
        "^" -> appendInput("^")
        "!" -> appendInput("!")
        "π" -> appendInput("π")
        "e" -> appendInput("e")
        else -> appendInput(key)
    }
}

@Composable
private fun CalcButton(
    label: String,
    height: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAction = label in listOf("=", "+", "-", "×", "÷")
    val isDanger = label in listOf("C", "⌫")
    val isSpecial = label in listOf("sin", "cos", "tan", "asin", "acos", "atan", "sqrt", "^", "log", "ln", "π", "e", "!", "%", "(", ")")

    val bg = when {
        label == "=" -> Color(0xFF2563EB)
        isAction -> Color(0xFF1E3A8A)
        isDanger -> Color(0xFF3F2022)
        isSpecial -> Color(0xFF1E293B)
        else -> Color(0xFF2A3449)
    }

    val textColor = when {
        label == "=" -> Color.White
        isDanger -> Color(0xFFFCA5A5)
        isAction -> Color(0xFF93C5FD)
        isSpecial -> Color(0xFF93C5FD)
        else -> Color.White
    }

    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = if (label.length > 3) 10.sp else 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}
