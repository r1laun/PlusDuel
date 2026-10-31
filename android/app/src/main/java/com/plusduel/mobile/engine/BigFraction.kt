package com.plusduel.mobile.engine

import java.math.BigInteger
import kotlin.math.abs

/**
 * Exact rational number backed by [BigInteger], mirroring PlusDuel's fraction.js + evaluate.ts.
 * Invariant: [den] > 0 and gcd(|num|, den) == 1.
 */
class BigFraction private constructor(
    val num: BigInteger,
    val den: BigInteger
) {
    val isZero: Boolean get() = num == BigInteger.ZERO
    val isExactInteger: Boolean get() = den == BigInteger.ONE

    fun toLongOrNull(): Long? {
        if (!isExactInteger) return null
        return try {
            num.longValueExact()
        } catch (_: ArithmeticException) {
            null
        }
    }

    fun toIntOrNull(): Int? {
        if (!isExactInteger) return null
        return try {
            num.intValueExact()
        } catch (_: ArithmeticException) {
            null
        }
    }

    fun toDisplayString(): String {
        return if (isExactInteger) {
            num.toString()
        } else {
            "$num/$den"
        }
    }

    fun negate(): BigFraction = BigFraction(num.negate(), den)

    fun add(other: BigFraction): BigFraction? {
        val n = num.multiply(other.den).add(other.num.multiply(den))
        val d = den.multiply(other.den)
        return createGuarded(n, d)
    }

    fun sub(other: BigFraction): BigFraction? {
        val n = num.multiply(other.den).subtract(other.num.multiply(den))
        val d = den.multiply(other.den)
        return createGuarded(n, d)
    }

    fun mul(other: BigFraction): BigFraction? {
        val n = num.multiply(other.num)
        val d = den.multiply(other.den)
        return createGuarded(n, d)
    }

    fun div(other: BigFraction): BigFraction? {
        if (other.isZero) return null
        val n = num.multiply(other.den)
        val d = den.multiply(other.num)
        return createGuarded(n, d)
    }

    fun pow(exponent: BigFraction): BigFraction? {
        val exp = exponent.toIntOrNull() ?: return null
        if (abs(exp) > MAX_EXPONENT) return null
        if (exp == 0) return ONE
        if (isZero && exp < 0) return null
        return try {
            if (exp > 0) {
                createGuarded(num.pow(exp), den.pow(exp))
            } else {
                val pos = -exp
                createGuarded(den.pow(pos), num.pow(pos))
            }
        } catch (_: ArithmeticException) {
            null
        }
    }

    fun sqrtExact(): BigFraction? {
        if (!isExactInteger) return null
        if (num < BigInteger.ZERO) return null
        if (num == BigInteger.ZERO) return ZERO
        if (num == BigInteger.ONE) return ONE
        val root = isqrt(num)
        if (root.multiply(root) != num) return null
        return createGuarded(root, BigInteger.ONE)
    }

    fun factorialExact(): BigFraction? {
        val n = toIntOrNull() ?: return null
        if (n < 0 || n > MAX_FACTORIAL) return null
        var acc = BigInteger.ONE
        for (i in 2..n) {
            acc = acc.multiply(BigInteger.valueOf(i.toLong()))
            if (acc.toString().length > MAX_MAGNITUDE_DIGITS) return null
        }
        return createGuarded(acc, BigInteger.ONE)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BigFraction) return false
        return num == other.num && den == other.den
    }

    override fun hashCode(): Int = 31 * num.hashCode() + den.hashCode()

    override fun toString(): String = toDisplayString()

    companion object {
        private const val MAX_MAGNITUDE_DIGITS = 30
        private const val MAX_FACTORIAL = 200
        private const val MAX_EXPONENT = 32

        val ZERO = BigFraction(BigInteger.ZERO, BigInteger.ONE)
        val ONE = BigFraction(BigInteger.ONE, BigInteger.ONE)

        fun of(value: Long): BigFraction = BigFraction(BigInteger.valueOf(value), BigInteger.ONE)
        fun of(value: Int): BigFraction = BigFraction(BigInteger.valueOf(value.toLong()), BigInteger.ONE)

        fun ofBigInteger(value: BigInteger): BigFraction? = createGuarded(value, BigInteger.ONE)

        fun createGuarded(rawNum: BigInteger, rawDen: BigInteger): BigFraction? {
            if (rawDen == BigInteger.ZERO) return null
            if (rawNum == BigInteger.ZERO) return ZERO

            val sign = if (rawDen.signum() < 0) -1 else 1
            val nSigned = if (sign < 0) rawNum.negate() else rawNum
            val dPos = rawDen.abs()

            val g = nSigned.abs().gcd(dPos)
            val n = if (g > BigInteger.ONE) nSigned.divide(g) else nSigned
            val d = if (g > BigInteger.ONE) dPos.divide(g) else dPos

            if (n.abs().toString().length > MAX_MAGNITUDE_DIGITS ||
                d.toString().length > MAX_MAGNITUDE_DIGITS
            ) {
                return null
            }
            return BigFraction(n, d)
        }

        private fun isqrt(n: BigInteger): BigInteger {
            if (n < BigInteger.TWO) return n
            var x = BigInteger.ONE.shiftLeft((n.bitLength() + 1) / 2)
            var y = x.add(n.divide(x)).shiftRight(1)
            while (y < x) {
                x = y
                y = x.add(n.divide(x)).shiftRight(1)
            }
            return x
        }
    }
}
