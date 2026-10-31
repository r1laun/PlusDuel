package com.plusduel.mobile

import com.plusduel.mobile.engine.BigFraction
import com.plusduel.mobile.engine.BotDifficulty
import com.plusduel.mobile.engine.EloCalculator
import com.plusduel.mobile.engine.ExpressionValidator
import com.plusduel.mobile.engine.RoundGenerator
import com.plusduel.mobile.engine.ValidationErrorCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PlusDuelEngineTest {

    @Test
    fun bigFraction_exactRationalArithmetic() {
        val oneThird = BigFraction.of(1).div(BigFraction.of(3))
        assertNotNull(oneThird)
        val sum = oneThird!!.add(oneThird)?.add(oneThird)
        assertNotNull(sum)
        assertTrue(sum!!.isExactInteger)
        assertEquals(1, sum.toIntOrNull())
    }

    @Test
    fun validator_acceptsValidConcatenationAndOperators() {
        val res = ExpressionValidator.validateExpression("23 + 4!", listOf(2, 3, 4), 47)
        assertTrue(res.valid)
        assertEquals(47, res.value)
    }

    @Test
    fun validator_rejectsLeadingZero() {
        val res = ExpressionValidator.validateExpression("03 + 5", listOf(0, 3, 5), 8)
        assertFalse(res.valid)
        assertEquals(ValidationErrorCode.LEADING_ZERO, res.errorCode)
    }

    @Test
    fun validator_enforcesDigitMultiset() {
        val missing = ExpressionValidator.validateExpression("2 + 3", listOf(2, 3, 5), 5)
        assertFalse(missing.valid)
        assertEquals(ValidationErrorCode.UNUSED_DIGITS, missing.errorCode)

        val extra = ExpressionValidator.validateExpression("2 + 3 + 9", listOf(2, 3), 14)
        assertFalse(extra.valid)
        assertEquals(ValidationErrorCode.EXTRA_DIGITS, extra.errorCode)
    }

    @Test
    fun validator_sqrtPrecedenceMatchesPlusDuel() {
        // √9! -> (√9)! = 3! = 6
        val res = ExpressionValidator.validateExpression("√9!", listOf(9), 6)
        assertTrue(res.valid)
        assertEquals(6, res.value)
    }

    @Test
    fun roundGenerator_producesSolvableRoundsAcrossAllSixLevels() {
        val rng = Random(42)
        for (level in 1..6) {
            repeat(5) {
                val round = RoundGenerator.generate(level, rng)
                val check = ExpressionValidator.validateExpression(
                    round.solution,
                    round.digits,
                    round.target
                )
                assertTrue(
                    "Generated solution '${round.solution}' for digits=${round.digits} target=${round.target} must be valid",
                    check.valid
                )
            }
        }
    }

    @Test
    fun eloCalculator_updatesRatingsAndTitlesCorrectly() {
        val newRating = EloCalculator.eloUpdate(1000, 1000, 1.0, EloCalculator.K_PROVISIONAL)
        assertEquals(1020, newRating)
        assertEquals("Gold", EloCalculator.titleFor(1020))
        assertEquals("Grandmaster", EloCalculator.titleFor(1820))
        assertEquals("Novice", EloCalculator.titleFor(750))
    }

    @Test
    fun botDifficulty_rollsRealisticDelays() {
        val rng = Random(123)
        for (bot in BotDifficulty.entries) {
            assertTrue(bot.pickBotName(rng).isNotBlank())
            val delay = bot.rollBotSolveDelayMs(1, 90_000L, rng)
            if (delay != null) {
                assertTrue(delay in 5_000L..89_200L)
            }
        }
    }
}
