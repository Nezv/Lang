package app.lang.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lang.shared.content.ContentRepository
import app.lang.shared.data.CardRepository
import app.lang.shared.model.Lesson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

data class LessonProgress(val lesson: Lesson, val saved: Int) {
    val total: Int get() = lesson.sentences.size
    val complete: Boolean get() = saved >= total
    val fraction: Float get() = if (total == 0) 0f else saved.toFloat() / total
}

data class TodayState(
    val loading: Boolean = true,
    val dueNow: Int = 0,
    val totalCards: Int = 0,
    val newCards: Int = 0,
    val reviewedToday: Int = 0,
    val lessons: List<LessonProgress> = emptyList(),
) {
    /** First lesson that still has unsaved sentences, otherwise the first lesson. */
    val nextLesson: LessonProgress? get() = lessons.firstOrNull { !it.complete } ?: lessons.firstOrNull()
}

class TodayViewModel(
    private val content: ContentRepository,
    private val cards: CardRepository,
    private val clock: Clock,
) : ViewModel() {
    private val _state = MutableStateFlow(TodayState())
    val state: StateFlow<TodayState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val now = clock.now()
            val zone = TimeZone.currentSystemDefault()
            val startOfDay = now.toLocalDateTime(zone).date.atStartOfDayIn(zone)
            val counts = cards.counts(now)
            val saved = cards.savedSentenceIds()
            _state.value = TodayState(
                loading = false,
                dueNow = counts.due,
                totalCards = counts.total,
                newCards = counts.new,
                reviewedToday = cards.reviewsSince(startOfDay),
                lessons = content.lessons().map { lesson ->
                    LessonProgress(lesson, lesson.sentences.count { it.id in saved })
                },
            )
        }
    }
}
