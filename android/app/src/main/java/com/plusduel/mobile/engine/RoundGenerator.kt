package com.plusduel.mobile.engine

import java.math.BigInteger
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.random.Random

data class LevelSpec(
    val level: Int,
    val minDigits: Int,
    val maxDigits: Int,
    val targetMin: Int,
    val targetMax: Int,
    val timeLimitSec: Int
)

data class GeneratedRound(
    val digits: List<Int>,
    val target: Int,
    val solution: String,
    val timeLimitMs: Long,
    val difficultyCoefficient: Double,
    val level: Int
)

/**
 * Guaranteed-solvable puzzle generator & scoring formulas matching PlusDuel's
 * `shared/src/difficulty.ts` and `shared/src/generator.ts`.
 */
object RoundGenerator {

    val LEVELS: List<LevelSpec> = listOf(
        LevelSpec(level = 1, minDigits = 2, maxDigits = 3, targetMin = 1, targetMax = 30, timeLimitSec = 90),
        LevelSpec(level = 2, minDigits = 3, maxDigits = 3, targetMin = 1, targetMax = 60, timeLimitSec = 90),
        LevelSpec(level = 3, minDigits = 3, maxDigits = 4, targetMin = 1, targetMax = 100, timeLimitSec = 75),
        LevelSpec(level = 4, minDigits = 4, maxDigits = 4, targetMin = 1, targetMax = 150, timeLimitSec = 75),
        LevelSpec(level = 5, minDigits = 4, maxDigits = 5, targetMin = 1, targetMax = 200, timeLimitSec = 75),
        LevelSpec(level = 6, minDigits = 5, maxDigits = 5, targetMin = 1, targetMax = 300, timeLimitSec = 60)
    )

    fun specForLevel(level: Int): LevelSpec {
        val idx = (level - 1).coerceIn(0, LEVELS.lastIndex)
        return LEVELS[idx]
    }

    fun levelForMatchRound(roundIndex: Int): Int {
        return roundIndex.coerceIn(1, LEVELS.size)
    }

    fun difficultyCoefficient(digitCount: Int, targetMax: Int): Double {
        val rangeBand = maxOf(1, ceil(targetMax / 100.0).toInt())
        return round1((digitCount * rangeBand).toDouble())
    }

    fun speedFactor(remainingMs: Long, timeLimitMs: Long): Double {
        if (timeLimitMs <= 0L) return 0.25
        val ratio = remainingMs.toDouble() / timeLimitMs.toDouble()
        return ratio.coerceIn(0.25, 1.0)
    }

    fun roundScore(
        digitCount: Int,
        targetMax: Int,
        remainingMs: Long,
        timeLimitMs: Long
    ): Double {
        return round1(difficultyCoefficient(digitCount, targetMax) * speedFactor(remainingMs, timeLimitMs))
    }

    private fun round1(n: Double): Double = (n * 10.0).roundToInt() / 10.0

    private data class Item(val value: BigFraction, val expr: String)

    fun generate(level: Int, rng: Random = Random.Default): GeneratedRound {
        val spec = specForLevel(level)
        val coeff = difficultyCoefficient(spec.maxDigits, spec.targetMax)

        repeat(40) {
            val count = rng.nextInt(spec.minDigits, spec.maxDigits + 1)
            val digits = mutableListOf(rng.nextInt(1, 10))
            for (i in 1 until count) {
                digits.add(rng.nextInt(0, 10))
            }

            val candidates = enumerateExact(digits, spec.targetMin, spec.targetMax)
            if (candidates.isNotEmpty()) {
                val pick = candidates[rng.nextInt(candidates.size)]
                val expr = stripOuterParens(pick.expr)
                val target = pick.value.toIntOrNull()
                if (target != null && ExpressionValidator.validateExpression(expr, digits, target).valid) {
                    return GeneratedRound(
                        digits = digits,
                        target = target,
                        solution = expr,
                        timeLimitMs = spec.timeLimitSec * 1000L,
                        difficultyCoefficient = difficultyCoefficient(digits.size, spec.targetMax),
                        level = spec.level
                    )
                }
            }
        }

        return GeneratedRound(
            digits = listOf(2, 3, 5),
            target = 25,
            solution = "5*(3+2)",
            timeLimitMs = spec.timeLimitSec * 1000L,
            difficultyCoefficient = coeff,
            level = spec.level
        )
    }

    private fun stripOuterParens(expr: String): String {
        if (expr.length >= 2 && expr.first() == '(' && expr.last() == ')') {
            var depth = 0
            for (i in 0 until expr.length - 1) {
                when (expr[i]) {
                    '(' -> depth++
                    ')' -> {
                        depth--
                        if (depth == 0) return expr
                    }
                }
            }
            return expr.substring(1, expr.length - 1)
        }
        return expr
    }

    private fun compositions(digits: List<Int>): List<List<List<Int>>> {
        val n = digits.size
        val out = mutableListOf<List<List<Int>>>()
        val totalMasks = 1 shl (n - 1)
        for (mask in 0 until totalMasks) {
            val groups = mutableListOf<List<Int>>()
            var current = mutableListOf(digits[0])
            for (i in 1 until n) {
                if ((mask and (1 shl (i - 1))) != 0) {
                    groups.add(current)
                    current = mutableListOf(digits[i])
                } else {
                    current.add(digits[i])
                }
            }
            groups.add(current)
            out.add(groups)
        }
        return out
    }

    private fun groupValue(group: List<Int>): Item? {
        if (group.size > 1 && group[0] == 0) return null
        val s = group.joinToString("")
        val v = s.toLongOrNull() ?: return null
        return Item(BigFraction.of(v), s)
    }

    private fun combine(a: Item, b: Item): List<Item> {
        val out = mutableListOf<Item>()
        val x = a.value
        val y = b.value

        x.add(y)?.let { out.add(Item(it, "(${a.expr}+${b.expr})")) }
        x.mul(y)?.let { out.add(Item(it, "(${a.expr}*${b.expr})")) }
        x.div(y)?.let { out.add(Item(it, "(${a.expr}/${b.expr})")) }
        y.div(x)?.let { out.add(Item(it, "(${b.expr}/${a.expr})")) }
        x.sub(y)?.let { out.add(Item(it, "(${a.expr}-${b.expr})")) }
        y.sub(x)?.let { out.add(Item(it, "(${b.expr}-${a.expr})")) }

        for ((base, exp, expr) in listOf(
            Triple(x, y, "${a.expr}^${b.expr}"),
            Triple(y, x, "${b.expr}^${a.expr}")
        )) {
            val eInt = exp.toIntOrNull()
            val bInt = base.toIntOrNull()
            if (eInt != null && eInt in 0..6 && bInt != null && bInt in -50..50) {
                base.pow(exp)?.let { out.add(Item(it, "($expr)")) }
            }
        }
        return out
    }

    private fun enumerateExact(digits: List<Int>, targetMin: Int, targetMax: Int): List<Item> {
        val found = mutableListOf<Item>()
        val seen = HashSet<BigInteger>()
        var budget = 35_000

        fun go(items: List<Item>) {
            if (budget <= 0 || found.size >= 120) return
            if (items.size == 1) {
                budget--
                val v = items[0]
                if (v.value.isExactInteger) {
                    val intV = v.value.toIntOrNull()
                    if (intV != null && intV in targetMin..targetMax && seen.add(v.value.num)) {
                        found.add(v)
                    }
                }
                return
            }
            for (i in items.indices) {
                for (j in i + 1 until items.size) {
                    val rest = mutableListOf<Item>()
                    for (k in items.indices) {
                        if (k != i && k != j) rest.add(items[k])
                    }
                    for (combo in combine(items[i], items[j])) {
                        val next = ArrayList<Item>(rest.size + 1)
                        next.addAll(rest)
                        next.add(combo)
                        go(next)
                        if (budget <= 0 || found.size >= 120) return
                    }
                }
            }
        }

        for (groups in compositions(digits)) {
            val leaves = mutableListOf<Item>()
            var valid = true
            for (g in groups) {
                val gv = groupValue(g)
                if (gv == null) {
                    valid = false
                    break
                }
                leaves.add(gv)
            }
            if (valid) go(leaves)
            if (budget <= 0 || found.size >= 120) break
        }

        return found
    }
}
