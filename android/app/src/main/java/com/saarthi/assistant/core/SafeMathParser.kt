package com.saarthi.assistant.core

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.pow

sealed class MathEvaluationResult {
    data class Success(val value: Double, val formatted: String) : MathEvaluationResult()
    data class Error(val message: String) : MathEvaluationResult()
}

/**
 * A safe, deterministic arithmetic parser and evaluator.
 * It uses a recursive-descent parser with strict token validation.
 * No scripting engines, dynamic code execution, or unrestricted eval are used.
 */
object SafeMathParser {

    fun evaluate(expression: String): MathEvaluationResult {
        val sanitized = preprocessExpression(expression)
        if (sanitized.isBlank()) {
            return MathEvaluationResult.Error("No arithmetic expression provided.")
        }

        return try {
            val tokens = tokenize(sanitized)
            if (tokens.isEmpty()) {
                return MathEvaluationResult.Error("Incomplete calculation expression.")
            }
            val parser = Parser(tokens)
            val result = parser.parse()
            if (result.isNaN() || result.isInfinite()) {
                MathEvaluationResult.Error("Calculation resulted in an invalid or overflowing number.")
            } else {
                MathEvaluationResult.Success(result, formatResult(result))
            }
        } catch (e: ArithmeticException) {
            MathEvaluationResult.Error(e.message ?: "Arithmetic error.")
        } catch (e: IllegalArgumentException) {
            MathEvaluationResult.Error(e.message ?: "Invalid calculation expression.")
        } catch (e: Exception) {
            MathEvaluationResult.Error("Invalid calculation expression.")
        }
    }

    private fun preprocessExpression(raw: String): String {
        var expr = raw.lowercase(Locale.ROOT)

        // Replace common speech and word variants with mathematical symbols
        expr = expr.replace("multiplied by", " * ")
        expr = expr.replace("times", " * ")
        expr = expr.replace("divided by", " / ")
        expr = expr.replace("to the power of", " ^ ")
        expr = expr.replace("power of", " ^ ")
        expr = expr.replace("power", " ^ ")
        expr = expr.replace("plus", " + ")
        expr = expr.replace("add", " + ")
        expr = expr.replace("minus", " - ")
        expr = expr.replace("subtract", " - ")
        expr = expr.replace("modulo", " % ")
        expr = expr.replace("mod", " % ")
        expr = expr.replace("over", " / ")
        expr = expr.replace("×", " * ")
        expr = expr.replace("÷", " / ")
        expr = expr.replace("x", " * ")

        return expr.trim()
    }

    private sealed class Token {
        data class Number(val value: Double) : Token()
        data class Op(val char: Char) : Token()
        data object OpenParen : Token()
        data object CloseParen : Token()
    }

    private fun tokenize(text: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        val len = text.length

        while (i < len) {
            val ch = text[i]

            if (ch.isWhitespace()) {
                i++
                continue
            }

            if (ch in "0123456789.") {
                val start = i
                var hasDot = false
                while (i < len && (text[i].isDigit() || text[i] == '.')) {
                    if (text[i] == '.') {
                        if (hasDot) throw IllegalArgumentException("Invalid number format in expression.")
                        hasDot = true
                    }
                    i++
                }
                val numStr = text.substring(start, i)
                val value = numStr.toDoubleOrNull()
                    ?: throw IllegalArgumentException("Invalid number: $numStr")
                tokens.add(Token.Number(value))
                continue
            }

            when (ch) {
                '+', '-', '*', '/', '%', '^' -> {
                    if (tokens.isNotEmpty()) {
                        val last = tokens.last()
                        if (last is Token.Op) {
                            throw IllegalArgumentException("Invalid syntax: consecutive operators in expression.")
                        }
                    } else if (ch != '+' && ch != '-') {
                        throw IllegalArgumentException("Invalid syntax: expression cannot start with '$ch'.")
                    }
                    tokens.add(Token.Op(ch))
                    i++
                }
                '(' -> {
                    tokens.add(Token.OpenParen)
                    i++
                }
                ')' -> {
                    tokens.add(Token.CloseParen)
                    i++
                }
                else -> {
                    throw IllegalArgumentException("Unsupported character in expression: '$ch'")
                }
            }
        }

        return tokens
    }

    private class Parser(private val tokens: List<Token>) {
        private var pos = 0

        fun parse(): Double {
            if (tokens.isEmpty()) {
                throw IllegalArgumentException("Incomplete calculation expression.")
            }
            val result = parseExpression()
            if (pos < tokens.size) {
                throw IllegalArgumentException("Unexpected token after expression.")
            }
            return result
        }

        // Expression: Term (('+' | '-') Term)*
        private fun parseExpression(): Double {
            var value = parseTerm()
            while (pos < tokens.size) {
                val token = tokens[pos]
                if (token is Token.Op && (token.char == '+' || token.char == '-')) {
                    pos++
                    val next = parseTerm()
                    value = if (token.char == '+') value + next else value - next
                } else {
                    break
                }
            }
            return value
        }

        // Term: Factor (('*' | '/' | '%') Factor)*
        private fun parseTerm(): Double {
            var value = parsePower()
            while (pos < tokens.size) {
                val token = tokens[pos]
                if (token is Token.Op && (token.char == '*' || token.char == '/' || token.char == '%')) {
                    pos++
                    val next = parsePower()
                    when (token.char) {
                        '*' -> value *= next
                        '/' -> {
                            if (next == 0.0) throw ArithmeticException("Division by zero is not allowed.")
                            value /= next
                        }
                        '%' -> {
                            if (next == 0.0) throw ArithmeticException("Division by zero is not allowed.")
                            value %= next
                        }
                    }
                } else {
                    break
                }
            }
            return value
        }

        // Power: Factor ('^' Factor)*
        private fun parsePower(): Double {
            var value = parseFactor()
            while (pos < tokens.size) {
                val token = tokens[pos]
                if (token is Token.Op && token.char == '^') {
                    pos++
                    val exponent = parseFactor()
                    value = value.pow(exponent)
                } else {
                    break
                }
            }
            return value
        }

        // Factor: Primary or unary +/-
        private fun parseFactor(): Double {
            if (pos >= tokens.size) {
                throw IllegalArgumentException("Incomplete calculation expression.")
            }

            val token = tokens[pos]
            if (token is Token.Op && (token.char == '+' || token.char == '-')) {
                pos++
                val next = parseFactor()
                return if (token.char == '+') next else -next
            }

            return parsePrimary()
        }

        private fun parsePrimary(): Double {
            if (pos >= tokens.size) {
                throw IllegalArgumentException("Incomplete calculation expression.")
            }

            return when (val token = tokens[pos]) {
                is Token.Number -> {
                    pos++
                    token.value
                }
                is Token.OpenParen -> {
                    pos++
                    val value = parseExpression()
                    if (pos >= tokens.size || tokens[pos] !is Token.CloseParen) {
                        throw IllegalArgumentException("Mismatched parentheses in expression.")
                    }
                    pos++ // consume ')'
                    value
                }
                else -> {
                    throw IllegalArgumentException("Invalid syntax in calculation expression.")
                }
            }
        }
    }

    fun formatResult(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            val symbols = DecimalFormatSymbols(Locale.US)
            val df = DecimalFormat("#,##0.######", symbols)
            df.format(value)
        }
    }
}
