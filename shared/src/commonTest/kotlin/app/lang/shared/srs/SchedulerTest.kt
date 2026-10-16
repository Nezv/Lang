package app.lang.shared.srs

import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class SchedulerTest {
    private val now = Instant.fromEpochMilliseconds(1_760_000_000_000)
    private val fsrs = Fsrs()
    private val scheduler = Scheduler(fsrs)
    private val newCard = SrsCard(due = now)
    private val matureCard = SrsCard(
        phase = CardPhase.Review,
        stability = 10.0,
        difficulty = 5.0,
        due = now,
        lastReview = now - 10.days,
        reps = 5,
    )

    @Test
    fun retrievabilityIsNinetyPercentWhenElapsedEqualsStability() {
        assertEquals(0.9, fsrs.retrievability(elapsedDays = 7.0, stability = 7.0), 1e-9)
    }

    @Test
    fun intervalEqualsStabilityAtNinetyPercentRetention() {
        assertEquals(12, fsrs.nextIntervalDays(12.0))
        assertEquals(1, fsrs.nextIntervalDays(0.2))
    }

    @Test
    fun initialDifficultyMatchesFsrsFormula() {
        val w = Fsrs.DEFAULT_PARAMETERS
        assertEquals(w[4] - exp(w[5] * 2) + 1, fsrs.initialDifficulty(Rating.Good), 1e-9)
    }

    @Test
    fun newCardFollowsLearningSteps() {
        assertEquals(1.minutes, scheduler.review(newCard, Rating.Again, now).interval)
        assertEquals(5.minutes + 30.seconds, scheduler.review(newCard, Rating.Hard, now).interval)
        val good = scheduler.review(newCard, Rating.Good, now)
        assertEquals(10.minutes, good.interval)
        assertEquals(CardPhase.Learning, good.card.phase)
        assertEquals(1, good.card.step)
    }

    @Test
    fun easyOnNewCardGraduatesWithInitialStability() {
        val easy = scheduler.review(newCard, Rating.Easy, now)
        assertEquals(CardPhase.Review, easy.card.phase)
        assertEquals(Fsrs.DEFAULT_PARAMETERS[3].roundToInt().days, easy.interval)
    }

    @Test
    fun goodOnLastLearningStepGraduates() {
        val first = scheduler.review(newCard, Rating.Good, now).card
        val second = scheduler.review(first, Rating.Good, now + 10.minutes)
        assertEquals(CardPhase.Review, second.card.phase)
        assertTrue(second.interval >= 1.days)
    }

    @Test
    fun reviewIntervalsGrowWithRating() {
        val preview = scheduler.preview(matureCard, now)
        assertEquals(10.minutes, preview.getValue(Rating.Again))
        assertTrue(preview.getValue(Rating.Hard) < preview.getValue(Rating.Good))
        assertTrue(preview.getValue(Rating.Good) < preview.getValue(Rating.Easy))
    }

    @Test
    fun lapseMovesCardToRelearning() {
        val lapsed = scheduler.review(matureCard, Rating.Again, now).card
        assertEquals(CardPhase.Relearning, lapsed.phase)
        assertEquals(1, lapsed.lapses)
        assertTrue(lapsed.stability!! < matureCard.stability!!)
    }

    @Test
    fun difficultyStaysWithinBounds() {
        var d = 5.0
        repeat(50) { d = fsrs.nextDifficulty(d, Rating.Again) }
        assertTrue(d <= 10.0)
        repeat(100) { d = fsrs.nextDifficulty(d, Rating.Easy) }
        assertTrue(d >= 1.0)
    }
}
