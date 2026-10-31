package com.plusduel.mobile.engine

import java.math.BigInteger

enum class ValidationErrorCode {
    EMPTY,
    INVALID_CHAR,
    LEADING_ZERO,
    SQRT_NEEDS_ARG,
    UNBALANCED_PARENS,
    UNUSED_DIGITS,
    EXTRA_DIGITS,
    SYNTAX_ERROR,
    NON_EXACT_INTERMEDIATE,
    NON_INTEGER_RESULT,
    WRONG_VALUE
}

data class ValidationResult(
    val valid: Boolean,
    val value: Int? = null,
    val fractionValue: BigFraction? = null,
    val errorCode: ValidationErrorCode? = null,
    val errorDetail: String = ""
)

data class LiveExpressionPreview(
    val usedDigits: List<Int>,
    val missingDigits: List<Int>,
    val extraDigits: List<Int>,
    val allDigitsUsedExact: Boolean,
    val liveResult: BigFraction?,
    val isSolved: Boolean,
    val syntaxErrorCode: ValidationErrorCode? = null,
    val syntaxErrorDetail: String = ""
)

/**
 * Exact expression scanner, parser, and evaluator mirroring PlusDuel's shared validator.
 */
object ExpressionValidator {

    private val ALLOWED_CHARS = Regex("^[0-9+\\-*/^!()\\s√]+$")

    private sealed interface Token {
        data class NumberLiteral(val raw: String, val value: BigInteger) : Token
        data class Op(val symbol: Char) : Token
        data object LParen : Token
        data object RParen : Token
    }

    private data class ScanResult(
        val ok: Boolean,
        val tokens: List<Token> = emptyList(),
        val usedDigits: List<Int> = emptyList(),
        val errorCode: ValidationErrorCode? = null,
        val errorDetail: String = ""
    )

    fun normalizePretty(expr: String): String {
        return expr
            .replace('*', '×')
            .replace('/', '÷')
            .replace('-', '−')
            .replace("sqrt", "√")
    }

    fun normalizeForServer(expr: String): String {
        return expr
            .replace('×', '*')
            .replace('÷', '/')
            .replace('−', '-')
            .replace('–', '-')
            .trim()
    }

    fun validateExpression(
        expression: String,
        issuedDigits: List<Int>,
        target: Int
    ): ValidationResult {
        val trimmed = expression.trim()
        if (trimmed.isEmpty()) {
            return ValidationResult(valid = false, errorCode = ValidationErrorCode.EMPTY)
        }

        val scan = scan(trimmed)
        if (!scan.ok) {
            return ValidationResult(
                valid = false,
                errorCode = scan.errorCode,
                errorDetail = scan.errorDetail
            )
        }

        if (!sameMultiset(scan.usedDigits, issuedDigits)) {
            val missing = multisetDiff(issuedDigits, scan.usedDigits)
            val extra = multisetDiff(scan.usedDigits, issuedDigits)
            return if (extra.isNotEmpty()) {
                ValidationResult(
                    valid = false,
                    errorCode = ValidationErrorCode.EXTRA_DIGITS,
                    errorDetail = extra.joinToString(", ")
                )
            } else {
                ValidationResult(
                    valid = false,
                    errorCode = ValidationErrorCode.UNUSED_DIGITS,
                    errorDetail = missing.joinToString(", ")
                )
            }
        }

        val parser = Parser(scan.tokens)
        val parsedNode = parser.parseComplete()
            ?: return ValidationResult(
                valid = false,
                errorCode = parser.errorCode ?: ValidationErrorCode.SYNTAX_ERROR
            )

        val evaluated = evalNode(parsedNode)
            ?: return ValidationResult(
                valid = false,
                errorCode = ValidationErrorCode.NON_EXACT_INTERMEDIATE
            )

        if (!evaluated.isExactInteger) {
            return ValidationResult(
                valid = false,
                fractionValue = evaluated,
                errorCode = ValidationErrorCode.NON_INTEGER_RESULT,
                errorDetail = evaluated.toDisplayString()
            )
        }

        val intVal = evaluated.toIntOrNull()
            ?: return ValidationResult(
                valid = false,
                fractionValue = evaluated,
                errorCode = ValidationErrorCode.NON_EXACT_INTERMEDIATE
            )

        if (intVal != target) {
            return ValidationResult(
                valid = false,
                value = intVal,
                fractionValue = evaluated,
                errorCode = ValidationErrorCode.WRONG_VALUE,
                errorDetail = "$intVal ≠ $target"
            )
        }

        return ValidationResult(
            valid = true,
            value = intVal,
            fractionValue = evaluated
        )
    }

    fun previewExpression(
        expression: String,
        issuedDigits: List<Int>,
        target: Int
    ): LiveExpressionPreview {
        val trimmed = expression.trim()
        if (trimmed.isEmpty()) {
            return LiveExpressionPreview(
                usedDigits = emptyList(),
                missingDigits = issuedDigits.sorted(),
                extraDigits = emptyList(),
                allDigitsUsedExact = false,
                liveResult = null,
                isSolved = false
            )
        }

        val scan = scan(trimmed)
        val used = scan.usedDigits
        val missing = multisetDiff(issuedDigits, used)
        val extra = multisetDiff(used, issuedDigits)
        val allMatch = sameMultiset(used, issuedDigits)

        if (!scan.ok) {
            return LiveExpressionPreview(
                usedDigits = used,
                missingDigits = missing,
                extraDigits = extra,
                allDigitsUsedExact = allMatch,
                liveResult = null,
                isSolved = false,
                syntaxErrorCode = scan.errorCode,
                syntaxErrorDetail = scan.errorDetail
            )
        }

        val parser = Parser(scan.tokens)
        val ast = parser.parseComplete()
        val liveValue = if (ast != null) evalNode(ast) else null
        val isSolved = allMatch &&
            liveValue != null &&
            liveValue.isExactInteger &&
            liveValue.toIntOrNull() == target

        return LiveExpressionPreview(
            usedDigits = used,
            missingDigits = missing,
            extraDigits = extra,
            allDigitsUsedExact = allMatch,
            liveResult = liveValue,
            isSolved = isSolved
        )
    }

    fun sameMultiset(a: List<Int>, b: List<Int>): Boolean {
        if (a.size != b.size) return false
        return a.sorted() == b.sorted()
    }

    fun multisetDiff(source: List<Int>, subtract: List<Int>): List<Int> {
        val pool = subtract.toMutableList()
        val out = mutableListOf<Int>()
        for (v in source) {
            val idx = pool.indexOf(v)
            if (idx == -1) out.add(v) else pool.removeAt(idx)
        }
        return out.sorted()
    }

    private fun scan(rawInput: String): ScanResult {
        val expr = rawInput
            .replace('−', '-')
            .replace('–', '-')
            .replace('×', '*')
            .replace('÷', '/')
            .replace("sqrt", "√")
            .trim()

        if (expr.isEmpty()) {
            return ScanResult(ok = false, errorCode = ValidationErrorCode.EMPTY)
        }

        val allUsedDigits = expr.filter { it in '0'..'9' }.map { it - '0' }

        if (!ALLOWED_CHARS.matches(expr)) {
            return ScanResult(
                ok = false,
                usedDigits = allUsedDigits,
                errorCode = ValidationErrorCode.INVALID_CHAR
            )
        }

        val tokens = mutableListOf<Token>()
        var i = 0
        var parenDepth = 0

        while (i < expr.length) {
            val ch = expr[i]
            when {
                ch.isWhitespace() -> i++
                ch in '0'..'9' -> {
                    var j = i
                    while (j < expr.length && expr[j] in '0'..'9') j++
                    val literal = expr.substring(i, j)
                    if (literal.length > 1 && literal[0] == '0') {
                        return ScanResult(
                            ok = false,
                            usedDigits = allUsedDigits,
                            errorCode = ValidationErrorCode.LEADING_ZERO,
                            errorDetail = literal
                        )
                    }
                    val bigVal = try {
                        BigInteger(literal)
                    } catch (_: NumberFormatException) {
                        return ScanResult(
                            ok = false,
                            usedDigits = allUsedDigits,
                            errorCode = ValidationErrorCode.SYNTAX_ERROR
                        )
                    }
                    tokens.add(Token.NumberLiteral(literal, bigVal))
                    i = j
                }
                ch == '(' -> {
                    parenDepth++
                    tokens.add(Token.LParen)
                    i++
                }
                ch == ')' -> {
                    parenDepth--
                    if (parenDepth < 0) {
                        return ScanResult(
                            ok = false,
                            usedDigits = allUsedDigits,
                            errorCode = ValidationErrorCode.UNBALANCED_PARENS
                        )
                    }
                    tokens.add(Token.RParen)
                    i++
                }
                ch in charArrayOf('+', '-', '*', '/', '^', '!', '√') -> {
                    if (ch == '√') {
                        var k = i + 1
                        while (k < expr.length && expr[k].isWhitespace()) k++
                        if (k >= expr.length) {
                            return ScanResult(
                                ok = false,
                                usedDigits = allUsedDigits,
                                errorCode = ValidationErrorCode.SQRT_NEEDS_ARG
                            )
                        }
                        val next = expr[k]
                        if (next != '(' && next !in '0'..'9' && next != '√') {
                            return ScanResult(
                                ok = false,
                                usedDigits = allUsedDigits,
                                errorCode = ValidationErrorCode.SQRT_NEEDS_ARG
                            )
                        }
                    }
                    tokens.add(Token.Op(ch))
                    i++
                }
                else -> {
                    return ScanResult(
                        ok = false,
                        usedDigits = allUsedDigits,
                        errorCode = ValidationErrorCode.INVALID_CHAR
                    )
                }
            }
        }

        if (parenDepth != 0) {
            return ScanResult(
                ok = false,
                usedDigits = allUsedDigits,
                errorCode = ValidationErrorCode.UNBALANCED_PARENS
            )
        }

        return ScanResult(
            ok = true,
            tokens = tokens,
            usedDigits = allUsedDigits
        )
    }

    private sealed interface AstNode {
        data class Constant(val value: BigInteger) : AstNode
        data class UnaryMinus(val operand: AstNode) : AstNode
        data class Sqrt(val operand: AstNode) : AstNode
        data class Factorial(val operand: AstNode) : AstNode
        data class Binary(val op: Char, val left: AstNode, val right: AstNode) : AstNode
    }

    private class Parser(private val tokens: List<Token>) {
        private var pos = 0
        var errorCode: ValidationErrorCode? = null

        fun parseComplete(): AstNode? {
            if (tokens.isEmpty()) return null
            val node = parseAddSub() ?: return null
            if (pos != tokens.size) {
                errorCode = ValidationErrorCode.SYNTAX_ERROR
                return null
            }
            return node
        }

        private fun peek(): Token? = tokens.getOrNull(pos)
        private fun consume(): Token = tokens[pos++]

        private fun parseAddSub(): AstNode? {
            var left = parseMulDiv() ?: return null
            while (true) {
                val t = peek()
                if (t is Token.Op && (t.symbol == '+' || t.symbol == '-')) {
                    consume()
                    val right = parseMulDiv() ?: return null
                    left = AstNode.Binary(t.symbol, left, right)
                } else {
                    break
                }
            }
            return left
        }

        private fun parseMulDiv(): AstNode? {
            var left = parseUnary() ?: return null
            while (true) {
                val t = peek()
                if (t is Token.Op && (t.symbol == '*' || t.symbol == '/')) {
                    consume()
                    val right = parseUnary() ?: return null
                    left = AstNode.Binary(t.symbol, left, right)
                } else {
                    break
                }
            }
            return left
        }

        private fun parseUnary(): AstNode? {
            val t = peek()
            if (t is Token.Op && t.symbol == '-') {
                consume()
                val operand = parseUnary() ?: return null
                return AstNode.UnaryMinus(operand)
            }
            return parsePow()
        }

        private fun parsePow(): AstNode? {
            val base = parsePostfix() ?: return null
            val t = peek()
            if (t is Token.Op && t.symbol == '^') {
                consume()
                val exponent = parseUnary() ?: return null
                return AstNode.Binary('^', base, exponent)
            }
            return base
        }

        private fun parsePostfix(): AstNode? {
            var node = parseSqrtOrPrimary() ?: return null
            while (true) {
                val t = peek()
                if (t is Token.Op && t.symbol == '!') {
                    consume()
                    node = AstNode.Factorial(node)
                } else {
                    break
                }
            }
            return node
        }

        private fun parseSqrtOrPrimary(): AstNode? {
            val t = peek() ?: run {
                errorCode = ValidationErrorCode.SYNTAX_ERROR
                return null
            }
            if (t is Token.Op && t.symbol == '√') {
                consume()
                val inner = parseSqrtOrPrimary() ?: run {
                    if (errorCode == null) errorCode = ValidationErrorCode.SQRT_NEEDS_ARG
                    return null
                }
                return AstNode.Sqrt(inner)
            }
            return parsePrimary()
        }

        private fun parsePrimary(): AstNode? {
            val t = peek() ?: run {
                errorCode = ValidationErrorCode.SYNTAX_ERROR
                return null
            }
            return when (t) {
                is Token.NumberLiteral -> {
                    consume()
                    AstNode.Constant(t.value)
                }
                is Token.LParen -> {
                    consume()
                    val inside = parseAddSub() ?: return null
                    val close = peek()
                    if (close !is Token.RParen) {
                        errorCode = ValidationErrorCode.UNBALANCED_PARENS
                        return null
                    }
                    consume()
                    inside
                }
                else -> {
                    errorCode = ValidationErrorCode.SYNTAX_ERROR
                    null
                }
            }
        }
    }

    private fun evalNode(node: AstNode): BigFraction? {
        return when (node) {
            is AstNode.Constant -> BigFraction.ofBigInteger(node.value)
            is AstNode.UnaryMinus -> evalNode(node.operand)?.negate()
            is AstNode.Sqrt -> evalNode(node.operand)?.sqrtExact()
            is AstNode.Factorial -> evalNode(node.operand)?.factorialExact()
            is AstNode.Binary -> {
                val left = evalNode(node.left) ?: return null
                val right = evalNode(node.right) ?: return null
                when (node.op) {
                    '+' -> left.add(right)
                    '-' -> left.sub(right)
                    '*' -> left.mul(right)
                    '/' -> left.div(right)
                    '^' -> left.pow(right)
                    else -> null
                }
            }
        }
    }
}
