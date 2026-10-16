package app.lang.shared.srs

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * FSRS-6 memory model — the scheduler Anki uses since version 23.10 — ported from the reference
 * implementation (open-spaced-repetition/py-fsrs). Pure maths, no state.
 *
 * Stability S = days until recall probability drops to 90 %. Difficulty D is 1 (easy) .. 10 (hard).
 */
class Fsrs(
    private val w: List<Double> = DEFAULT_PARAMETERS,
    val desiredRetention: Double = 0.9,
    val maximumIntervalDays: Int = 36_500,
) {
    init {
        require(w.size == 21) { "FSRS-6 needs 21 parameters, got ${w.size}" }
        require(desiredRetention in 0.7..0.99) { "desiredRetention must be within 0.70..0.99" }
    }

    private val decay = -w[20]
    private val factor = 0.9.pow(1.0 / decay) - 1.0

    /** Probability of recalling a card [elapsedDays] after the last review. */
    fun retrievability(elapsedDays: Double, stability: Double): Double =
        (1.0 + factor * elapsedDays / stability).pow(decay)

    fun initialStability(rating: Rating): Double = max(w[rating.value - 1], MIN_STABILITY)

    fun initialDifficulty(rating: Rating): Double = rawInitialDifficulty(rating).coerceIn(1.0, 10.0)

    fun nextDifficulty(difficulty: Double, rating: Rating): Double {
        val delta = -w[6] * (rating.value - 3)
        val damped = difficulty + (10.0 - difficulty) * delta / 9.0
        val reverted = w[7] * rawInitialDifficulty(Rating.Easy) + (1.0 - w[7]) * damped
        return reverted.coerceIn(1.0, 10.0)
    }

    /** New stability after a review at least one day after the previous one. */
    fun nextStability(difficulty: Double, stability: Double, retrievability: Double, rating: Rating): Double {
        val next = if (rating == Rating.Again) {
            forgetStability(difficulty, stability, retrievability)
        } else {
            recallStability(difficulty, stability, retrievability, rating)
        }
        return max(next, MIN_STABILITY)
    }

    /** New stability after a same-day review (learning steps). */
    fun shortTermStability(stability: Double, rating: Rating): Double {
        var increase = exp(w[17] * (rating.value - 3 + w[18])) * stability.pow(-w[19])
        if (rating == Rating.Good || rating == Rating.Easy) increase = max(increase, 1.0)
        return max(stability * increase, MIN_STABILITY)
    }

    /** Days until recall probability falls to [desiredRetention]. */
    fun nextIntervalDays(stability: Double): Int {
        val days = stability / factor * (desiredRetention.pow(1.0 / decay) - 1.0)
        return days.roundToInt().coerceIn(1, maximumIntervalDays)
    }

    private fun rawInitialDifficulty(rating: Rating): Double = w[4] - exp(w[5] * (rating.value - 1)) + 1.0

    private fun recallStability(d: Double, s: Double, r: Double, rating: Rating): Double {
        val hardPenalty = if (rating == Rating.Hard) w[15] else 1.0
        val easyBonus = if (rating == Rating.Easy) w[16] else 1.0
        return s * (1.0 + exp(w[8]) * (11.0 - d) * s.pow(-w[9]) * (exp((1.0 - r) * w[10]) - 1.0) * hardPenalty * easyBonus)
    }

    private fun forgetStability(d: Double, s: Double, r: Double): Double {
        val longTerm = w[11] * d.pow(-w[12]) * ((s + 1.0).pow(w[13]) - 1.0) * exp((1.0 - r) * w[14])
        val shortTerm = s / exp(w[17] * w[18])
        return min(longTerm, shortTerm)
    }

    companion object {
        const val MIN_STABILITY = 0.001

        /** FSRS-6 defaults (py-fsrs 6.x). Personal optimisation from review history comes later. */
        val DEFAULT_PARAMETERS: List<Double> = listOf(
            0.212, 1.2931, 2.3065, 8.2956, 6.4133, 0.8334, 3.0194, 0.001, 1.8722, 0.1666, 0.796,
            1.4835, 0.0614, 0.2629, 1.6483, 0.6014, 1.8729, 0.5425, 0.0912, 0.0658, 0.1542,
        )
    }
}
