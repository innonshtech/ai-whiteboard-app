package com.avitrisphare.whiteboard.presentation.canvas

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FormulaCard(
    solution: FormulaSolution,
    onSolve: (String) -> Unit,
    onDismiss: () -> Unit,
    onRetry: () -> Unit = {},
    onDraftChange: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var draft by remember(solution.latex) { mutableStateOf(solution.latex) }

    Surface(
        modifier = modifier.widthIn(max = 560.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF1C2130),
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier
                .padding(18.dp)
                .heightIn(max = 480.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Formula",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        "Local mathematical parsing (on-device, not cloud vision recognition)",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
                TextButton(onClick = onRetry) {
                    Text("Retry", color = Color(0xFF93C5FD))
                }
                TextButton(onClick = onDismiss) {
                    Text("Close", color = Color(0xFFC7D2FE))
                }
            }
            FormulaPreview(draft, Modifier.fillMaxWidth().height(84.dp))
            Spacer(Modifier.height(10.dp))
            Text("Edit LaTeX", color = Color(0xFF94A3B8), fontSize = 11.sp)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                ),
                singleLine = false,
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6366F1),
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedContainerColor = Color(0xFF0F172A),
                    unfocusedContainerColor = Color(0xFF0F172A)
                )
            )
            Spacer(Modifier.height(8.dp))
            Text("MathML", color = Color(0xFF94A3B8), fontSize = 11.sp)
            Text(solution.mathml, color = Color(0xFFCBD5E1), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            if (solution.solved) {
                Spacer(Modifier.height(12.dp))
                Text("Solution", color = Color.White, fontWeight = FontWeight.SemiBold)
                FormulaPreview(solution.answerLatex, Modifier.fillMaxWidth().height(72.dp))
                solution.steps.forEachIndexed { index, step ->
                    Text(
                        "${index + 1}. $step",
                        color = Color(0xFFE2E8F0),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!solution.solved) {
                    Button(
                        onClick = {
                            onDraftChange(draft)
                            onSolve(draft)
                        },
                        enabled = draft.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                    ) {
                        Text("Solve this")
                    }
                }
            }
        }
    }
}

@Composable
private fun FormulaPreview(latex: String, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier.background(Color.White, RoundedCornerShape(12.dp))
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1B1F2A.toInt()
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        }
        drawContext.canvas.nativeCanvas.let { native ->
            try {
                drawFormula(native, parseFormula(latex), 16f, 12f, 32f, paint)
            } catch (_: Exception) {
                // Safeguard against incomplete tokens during active typing
            }
        }
    }
}
