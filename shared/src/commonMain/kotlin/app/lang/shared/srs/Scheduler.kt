package app.lang.shared.srs

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** The four answer buttons, exactly as in Anki. */
enum class Rating(val value: Int) { Again(1), Hard(2), Good(3), Easy(4) }

enum class CardPhase { New, Learning, Review, Relearning }

/** Scheduling state of one flashcard. [stability]/[difficulty] are null until the first answer. */
data class SrsCard(
    val phase: CardPhase = CardPhase.New,
    val step: Int = 0,
    val stability: Double? = null,
    val difficulty: Double? = null,
    val due: Instant,
    val lastReview: Instant? = null,
    val reps: Int = 0,
    val lapses: Int = 0,
)

data class ReviewOutcome(
    val card: SrsCard,
    val rating: Rating,
    val reviewedAt: Instant,
    val elapsedDays: Int,
    val interval: Duration,
)

/**
 * Anki-style scheduler: short learning steps for new/forgotten cards, then FSRS-6 intervals.
 * Mirrors py-fsrs 6.x behaviour (without interval fuzz, which is a TODO).
 */
class Scheduler(
    private val fsrs: Fsrs = Fsrs(),
    private val learningSteps: List<Duration> = listOf(1.minutes, 10.minutes),
    private val relearningSteps: List<Duration> = listOf(10.minutes),
) {
    fun review(card: SrsCard, rating: Rating, now: Instant): ReviewOutcome {
        val elapsedDays = card.lastReview?.let { (now - it).inWholeDays.toInt().coerceAtLeast(0) }
        val (stability, difficulty) = nextMemoryState(card, rating, elapsedDays)
        fun reviewInterval(): Duration = fsrs.nextIntervalDays(stability).days

        val phase: CardPhase
        var step = 0
        var lapses = card.lapses
        val interval: Duration

        if (card.phase == CardPhase.Review) {
            if (rating == Rating.Again) {
                lapses += 1
                if (relearningSteps.isEmpty()) {
                    phase = CardPhase.Review
                    interval = reviewInterval()
                } else {
                    phase = CardPhase.Relearning
                    interval = relearningSteps[0]
                }
            } else {
                phase = CardPhase.Review
                interval = reviewInterval()
            }
        } else {
            val relearning = card.phase == CardPhase.Relearning
            val steps = if (relearning) relearningSteps else learningSteps
            val stepPhase = if (relearning) CardPhase.Relearning else CardPhase.Learning
            val current = if (card.phase == CardPhase.New) 0 else card.step

            if (steps.isEmpty() || (current >= steps.size && rating != Rating.Again)) {
                phase = CardPhase.Review
                interval = reviewInterval()
            } else {
                when (rating) {
                    Rating.Again -> {
                        phase = stepPhase
                        interval = steps[0]
                    }
                    Rating.Hard -> {
                        phase = stepPhase
                        step = current
                        interval = when {
                            current == 0 && steps.size == 1 -> steps[0] * 1.5
                            current == 0 -> (steps[0] + steps[1]) / 2
                            else -> steps[current]
                        }
                    }
                    Rating.Good -> {
                        if (current + 1 >= steps.size) {
                            phase = CardPhase.Review
                            interval = reviewInterval()
                        } else {
                            phase = stepPhase
                            step = current + 1
                            interval = steps[step]
                        }
                    }
                    Rating.Easy -> {
                        phase = CardPhase.Review
                        interval = reviewInterval()
                    }
                }
            }
        }

        val updated = card.copy(
            phase = phase,
            step = step,
            stability = stability,
            difficulty = difficulty,
            due = now + interval,
            lastReview = now,
            reps = card.reps + 1,
            lapses = lapses,
        )
        return ReviewOutcome(updated, rating, now, elapsedDays ?: 0, interval)
    }

    /** What each button would schedule, for the labels under the answer buttons ("1m", "10m", "8d"). */
    fun preview(card: SrsCard, now: Instant): Map<Rating, Duration> =
        Rating.entries.associateWith { review(card, it, now).interval }

    private fun nextMemoryState(card: SrsCard, rating: Rating, elapsedDays: Int?): Pair<Double, Double> {
        val s = card.stability
        val d = card.difficulty
        if (s == null || d == null) return fsrs.initialStability(rating) to fsrs.initialDifficulty(rating)
        val nextS = if (elapsedDays != null && elapsedDays < 1) {
            fsrs.shortTermStability(s, rating)
        } else {
            fsrs.nextStability(d, s, fsrs.retrievability((elapsedDays ?: 0).toDouble(), s), rating)
        }
        return nextS to fsrs.nextDifficulty(d, rating)
    }
}
