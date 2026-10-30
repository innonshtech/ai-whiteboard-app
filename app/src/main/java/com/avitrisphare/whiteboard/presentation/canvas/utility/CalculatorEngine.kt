package com.avitrisphare.whiteboard.presentation.canvas.utility

import kotlin.math.*

/**
 * Robust mathematical expression parser and evaluator for standard and scientific calculator.
 * Supports:
 * - Basic arithmetic: +, -, *, /, %
 * - Powers: ^
 * - Factorials: n!
 * - Trigonometry: sin, cos, tan, asin, acos, atan (supports Degree & Radian modes)
 * - Roots & Logs: sqrt, cbrt, log (base 10), ln (natural log)
 * - Constants: pi (π), e
 * - Parentheses: ( )
 */
class CalculatorEngine(var isDegreeMode: Boolean = true) {

    sealed class EvalResult {
        data class Success(val value: Double, val formatted: String) : EvalResult()
        data class Error(val message: String) : EvalResult()
    }

    fun evaluate(expression: String): EvalResult {
        val sanitized = expression
            .replace("×", "*")
            .replace("÷", "/")
            .replace("π", "pi")
            .replace("√", "sqrt ")
            .trim()

        if (sanitized.isEmpty()) {
            return EvalResult.Error("Empty expression")
        }

        return try {
            val parser = Parser(sanitized, isDegreeMode)
            val result = parser.parse()
            if (result.isNaN()) {
                EvalResult.Error("Not a number")
            } else if (result.isInfinite()) {
                EvalResult.Error("Cannot divide by 0")
            } else {
                EvalResult.Success(result, formatResult(result))
            }
        } catch (e: ArithmeticException) {
            EvalResult.Error(e.message ?: "Math error")
        } catch (e: Exception) {
            EvalResult.Error("Syntax error")
        }
    }

    fun formatResult(value: Double): String {
        if (value.isNaN()) return "NaN"
        if (value.isInfinite()) return if (value > 0) "Infinity" else "-Infinity"
        // Avoid scientific notation for reasonably sized numbers
        val absVal = abs(value)
        if (absVal >= 1e12 || (absVal < 1e-6 && absVal > 0.0)) {
            return String.format(java.util.Locale.US, "%.6e", value)
        }
        val rounded = (value * 1e8).roundToLong() / 1e8
        return if (rounded == rounded.toLong().toDouble()) {
            rounded.toLong().toString()
        } else {
            // Trim trailing zeroes
            val str = String.format(java.util.Locale.US, "%.8f", value)
            str.trimEnd('0').trimEnd('.')
        }
    }

    private class Parser(private val input: String, private val isDegreeMode: Boolean) {
        private var pos = 0

        fun parse(): Double {
            val value = parseExpression()
            skipWhitespace()
            if (pos < input.length) {
                throw IllegalArgumentException("Unexpected character at pos $pos: '${input[pos]}'")
            }
            return value
        }

        private fun parseExpression(): Double {
            var value = parseTerm()
            while (true) {
                skipWhitespace()
                if (match('+')) {
                    value += parseTerm()
                } else if (match('-')) {
                    value -= parseTerm()
                } else {
                    break
                }
            }
            return value
        }

        private fun parseTerm(): Double {
            var value = parsePower()
            while (true) {
                skipWhitespace()
                if (match('*')) {
                    value *= parsePower()
                } else if (match('/')) {
                    val divisor = parsePower()
                    if (divisor == 0.0) throw ArithmeticException("Cannot divide by 0")
                    value /= divisor
                } else if (match('%')) {
                    val divisor = parsePower()
                    if (divisor == 0.0) throw ArithmeticException("Cannot divide by 0")
                    value %= divisor
                } else {
                    break
                }
            }
            return value
        }

        private fun parsePower(): Double {
            var value = parseUnary()
            skipWhitespace()
            if (match('^')) {
                val exponent = parsePower() // right-associative
                value = value.pow(exponent)
            }
            return value
        }

        private fun parseUnary(): Double {
            skipWhitespace()
            if (match('+')) {
                return parseUnary()
            }
            if (match('-')) {
                return -parseUnary()
            }
            return parsePostfix()
        }

        private fun parsePostfix(): Double {
            var value = parsePrimary()
            skipWhitespace()
            while (match('!')) {
                if (value < 0 || value != floor(value) || value > 170) {
                    throw ArithmeticException("Factorial undefined for $value")
                }
                value = factorial(value.toInt())
            }
            return value
        }

        private fun parsePrimary(): Double {
            skipWhitespace()
            if (pos >= input.length) {
                throw IllegalArgumentException("Unexpected end of expression")
            }

            if (match('(')) {
                val value = parseExpression()
                skipWhitespace()
                if (!match(')')) {
                    throw IllegalArgumentException("Missing closing parenthesis")
                }
                return value
            }

            val ch = input[pos]
            if (ch.isDigit() || ch == '.') {
                return parseNumber()
            }

            if (ch.isLetter()) {
                val word = parseWord()
                skipWhitespace()
                return when (word.lowercase()) {
                    "pi" -> Math.PI
                    "e" -> Math.E
                    "sin" -> {
                        val arg = parseFunctionArg()
                        val radians = if (isDegreeMode) Math.toRadians(arg) else arg
                        sin(radians)
                    }
                    "cos" -> {
                        val arg = parseFunctionArg()
                        val radians = if (isDegreeMode) Math.toRadians(arg) else arg
                        cos(radians)
                    }
                    "tan" -> {
                        val arg = parseFunctionArg()
                        val radians = if (isDegreeMode) Math.toRadians(arg) else arg
                        tan(radians)
                    }
                    "asin" -> {
                        val arg = parseFunctionArg()
                        val res = asin(arg)
                        if (isDegreeMode) Math.toDegrees(res) else res
                    }
                    "acos" -> {
                        val arg = parseFunctionArg()
                        val res = acos(arg)
                        if (isDegreeMode) Math.toDegrees(res) else res
                    }
                    "atan" -> {
                        val arg = parseFunctionArg()
                        val res = atan(arg)
                        if (isDegreeMode) Math.toDegrees(res) else res
                    }
                    "sqrt" -> {
                        val arg = parseFunctionArg()
                        if (arg < 0) throw ArithmeticException("Square root of negative number")
                        sqrt(arg)
                    }
                    "cbrt" -> cbrt(parseFunctionArg())
                    "log" -> {
                        val arg = parseFunctionArg()
                        if (arg <= 0) throw ArithmeticException("Log of non-positive number")
                        log10(arg)
                    }
                    "ln" -> {
                        val arg = parseFunctionArg()
                        if (arg <= 0) throw ArithmeticException("Ln of non-positive number")
                        ln(arg)
                    }
                    "abs" -> abs(parseFunctionArg())
                    "exp" -> exp(parseFunctionArg())
                    "fact" -> {
                        val arg = parseFunctionArg()
                        if (arg < 0 || arg != floor(arg) || arg > 170) {
                            throw ArithmeticException("Factorial undefined for $arg")
                        }
                        factorial(arg.toInt())
                    }
                    else -> throw IllegalArgumentException("Unknown function or identifier: '$word'")
                }
            }

            throw IllegalArgumentException("Unexpected character: '$ch'")
        }

        private fun parseFunctionArg(): Double {
            skipWhitespace()
            if (!match('(')) {
                // Support syntax like "sqrt 16"
                return parseUnary()
            }
            val arg = parseExpression()
            skipWhitespace()
            if (!match(')')) {
                throw IllegalArgumentException("Missing closing parenthesis for function")
            }
            return arg
        }

        private fun parseNumber(): Double {
            val start = pos
            var hasDot = false
            while (pos < input.length) {
                val c = input[pos]
                if (c == '.') {
                    if (hasDot) break
                    hasDot = true
                    pos++
                } else if (c.isDigit()) {
                    pos++
                } else {
                    break
                }
            }
            val numStr = input.substring(start, pos)
            return numStr.toDouble()
        }

        private fun parseWord(): String {
            val start = pos
            while (pos < input.length && input[pos].isLetter()) {
                pos++
            }
            return input.substring(start, pos)
        }

        private fun skipWhitespace() {
            while (pos < input.length && input[pos].isWhitespace()) {
                pos++
            }
        }

        private fun match(expected: Char): Boolean {
            skipWhitespace()
            if (pos < input.length && input[pos] == expected) {
                pos++
                return true
            }
            return false
        }

        private fun factorial(n: Int): Double {
            var res = 1.0
            for (i in 2..n) {
                res *= i
            }
            return res
        }
    }
}
