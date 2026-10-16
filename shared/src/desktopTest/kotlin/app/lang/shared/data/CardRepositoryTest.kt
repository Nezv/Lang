package app.lang.shared.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.lang.shared.db.LangDatabase
import app.lang.shared.srs.CardPhase
import app.lang.shared.srs.Rating
import app.lang.shared.srs.Scheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class CardRepositoryTest {
    private val now = Instant.fromEpochMilliseconds(1_760_000_000_000)

    private fun repository() = CardRepository(
        LangDatabase(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY, Properties(), LangDatabase.Schema)),
        Dispatchers.Unconfined,
    )

    @Test
    fun savingTheSameSentenceTwiceKeepsOneCard() = runTest {
        val repo = repository()
        assertTrue(repo.addSentence("greet-1", now))
        assertFalse(repo.addSentence("greet-1", now))
        assertEquals(CardCounts(total = 1, due = 1, new = 1), repo.counts(now))
        assertEquals(setOf("greet-1"), repo.savedSentenceIds())
    }

    @Test
    fun reviewIsPersistedAndRescheduled() = runTest {
        val repo = repository()
        repo.addSentence("greet-1", now)
        val card = repo.dueCards(now).single()
        assertEquals(CardPhase.New, card.srs.phase)

        repo.recordReview(card, Scheduler().review(card.srs, Rating.Easy, now))

        assertTrue(repo.dueCards(now).isEmpty())
        assertEquals(1, repo.reviewsSince(now))
        val later = repo.dueCards(now + 30.days).single()
        assertEquals(CardPhase.Review, later.srs.phase)
        assertEquals(1, later.srs.reps)
    }
}
