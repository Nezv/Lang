package app.lang.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lang.shared.content.ContentRepository
import app.lang.shared.data.CardRecord
import app.lang.shared.data.CardRepository
import app.lang.shared.model.Sentence
import app.lang.shared.speech.SpeechService
import app.lang.shared.srs.Rating
import app.lang.shared.srs.Scheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

data class ReviewItem(val card: CardRecord, val sentence: Sentence)

data class ReviewState(
    val loading: Boolean = true,
    val queue: List<ReviewItem> = emptyList(),
    val revealed: Boolean = false,
    val reviewedThisSession: Int = 0,
    /** Label shown under each answer button, e.g. Again → "1m", Easy → "8d". */
    val intervals: Map<Rating, String> = emptyMap(),
) {
    val current: ReviewItem? get() = queue.firstOrNull()
    val remaining: Int get() = queue.size
    val sessionProgress: Float
        get() = if (reviewedThisSession + remaining == 0) 0f else reviewedThisSession.toFloat() / (reviewedThisSession + remaining)
}

/** Anki-style review session: show the sentence, reveal, answer Again/Hard/Good/Easy. */
class ReviewViewModel(
    private val content: ContentRepository,
    private val cards: CardRepository,
    private val scheduler: Scheduler,
    private val speech: SpeechService,
    private val clock: Clock,
) : ViewModel() {
    private val _state = MutableStateFlow(ReviewState())
    val state: StateFlow<ReviewState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val now = clock.now()
            val items = cards.dueCards(now).mapNotNull { record ->
                content.sentence(record.sentenceId)?.let { ReviewItem(record, it) }
            }
            _state.update {
                it.copy(loading = false, queue = items, revealed = false, intervals = previewFor(items.firstOrNull(), now))
            }
        }
    }

    /** Reloads only when no session is in progress, so switching tabs does not lose the queue. */
    fun refreshIfIdle() {
        if (_state.value.current == null) refresh()
    }

    fun reveal() {
        if (_state.value.current == null) return
        _state.update { it.copy(revealed = true) }
    }

    fun grade(rating: Rating) {
        val state = _state.value
        val item = state.current ?: return
        if (!state.revealed) return
        val now = clock.now()
        val outcome = scheduler.review(item.card.srs, rating, now)
        val updated = item.copy(card = item.card.copy(srs = outcome.card))
        _state.update { s ->
            val rest = s.queue.drop(1)
            // Cards in learning steps come back later in the same session, like Anki.
            val queue = if (outcome.card.due <= now + LEARN_AHEAD) rest + updated else rest
            s.copy(
                queue = queue,
                revealed = false,
                reviewedThisSession = s.reviewedThisSession + 1,
                intervals = previewFor(queue.firstOrNull(), now),
            )
        }
        viewModelScope.launch { cards.recordReview(item.card, outcome) }
    }

    fun speak() {
        _state.value.current?.let { speech.speak(it.sentence.hanzi) }
    }

    private fun previewFor(item: ReviewItem?, now: Instant): Map<Rating, String> =
        item?.let { scheduler.preview(it.card.srs, now).mapValues { (_, interval) -> formatInterval(interval) } }
            ?: emptyMap()

    private companion object {
        val LEARN_AHEAD = 20.minutes
    }
}
