package app.lang.shared.data

import app.lang.shared.db.Card
import app.lang.shared.db.LangDatabase
import app.lang.shared.srs.CardPhase
import app.lang.shared.srs.ReviewOutcome
import app.lang.shared.srs.SrsCard
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlin.time.Instant

data class CardRecord(val id: String, val sentenceId: String, val srs: SrsCard)

data class CardCounts(val total: Int, val due: Int, val new: Int)

/** Flashcards and review history, stored in the local SQLite database. */
class CardRepository(private val db: LangDatabase, private val io: CoroutineDispatcher) {
    private val queries get() = db.cardQueries

    /** Adds a card for [sentenceId]. Returns false if that sentence is already in the deck. */
    suspend fun addSentence(sentenceId: String, now: Instant): Boolean = withContext(io) {
        db.transactionWithResult {
            if (queries.countForSentence(sentenceId).executeAsOne() > 0L) {
                false
            } else {
                queries.insertCard(
                    id = cardIdFor(sentenceId),
                    sentence_id = sentenceId,
                    phase = CardPhase.New.name,
                    step = 0L,
                    stability = null,
                    difficulty = null,
                    due_ms = now.toEpochMilliseconds(),
                    last_review_ms = null,
                    reps = 0L,
                    lapses = 0L,
                    created_ms = now.toEpochMilliseconds(),
                )
                true
            }
        }
    }

    suspend fun savedSentenceIds(): Set<String> = withContext(io) {
        queries.selectSentenceIds().executeAsList().toSet()
    }

    suspend fun dueCards(now: Instant): List<CardRecord> = withContext(io) {
        queries.selectDue(now.toEpochMilliseconds()).executeAsList().map { it.toRecord() }
    }

    suspend fun counts(now: Instant): CardCounts = withContext(io) {
        CardCounts(
            total = queries.countAll().executeAsOne().toInt(),
            due = queries.countDue(now.toEpochMilliseconds()).executeAsOne().toInt(),
            new = queries.countNew().executeAsOne().toInt(),
        )
    }

    suspend fun reviewsSince(since: Instant): Int = withContext(io) {
        queries.countReviewsSince(since.toEpochMilliseconds()).executeAsOne().toInt()
    }

    /** Saves the new schedule and the answer log atomically. */
    suspend fun recordReview(card: CardRecord, outcome: ReviewOutcome) = withContext(io) {
        val s = outcome.card
        db.transaction {
            queries.updateSchedule(
                phase = s.phase.name,
                step = s.step.toLong(),
                stability = s.stability,
                difficulty = s.difficulty,
                due_ms = s.due.toEpochMilliseconds(),
                last_review_ms = s.lastReview?.toEpochMilliseconds(),
                reps = s.reps.toLong(),
                lapses = s.lapses.toLong(),
                id = card.id,
            )
            queries.insertReview(
                card_id = card.id,
                rating = outcome.rating.value.toLong(),
                reviewed_ms = outcome.reviewedAt.toEpochMilliseconds(),
                elapsed_days = outcome.elapsedDays.toLong(),
                interval_ms = outcome.interval.inWholeMilliseconds,
            )
        }
    }

    private fun cardIdFor(sentenceId: String) = "sentence:$sentenceId"

    private fun Card.toRecord() = CardRecord(
        id = id,
        sentenceId = sentence_id,
        srs = SrsCard(
            phase = CardPhase.valueOf(phase),
            step = step.toInt(),
            stability = stability,
            difficulty = difficulty,
            due = Instant.fromEpochMilliseconds(due_ms),
            lastReview = last_review_ms?.let { Instant.fromEpochMilliseconds(it) },
            reps = reps.toInt(),
            lapses = lapses.toInt(),
        ),
    )
}
