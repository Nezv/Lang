package app.lang.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lang.shared.content.ContentRepository
import app.lang.shared.data.CardRepository
import app.lang.shared.model.Lesson
import app.lang.shared.model.Sentence
import app.lang.shared.speech.SpeechService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

data class LearnState(
    val lessons: List<Lesson>,
    val openLessonId: String? = null,
    val index: Int = 0,
    val showHanzi: Boolean = true,
    val showPinyin: Boolean = true,
    val showTranslation: Boolean = false,
    val savedSentenceIds: Set<String> = emptySet(),
) {
    val openLesson: Lesson? get() = lessons.firstOrNull { it.id == openLessonId }
    val currentSentence: Sentence? get() = openLesson?.sentences?.getOrNull(index)

    fun savedCount(lesson: Lesson): Int = lesson.sentences.count { it.id in savedSentenceIds }
}

/** Immersive-Chinese-style lesson player: one sentence at a time, toggles for hanzi/pinyin/English. */
class LearnViewModel(
    content: ContentRepository,
    private val cards: CardRepository,
    private val speech: SpeechService,
    private val clock: Clock,
) : ViewModel() {
    private val _state = MutableStateFlow(LearnState(lessons = content.lessons()))
    val state: StateFlow<LearnState> = _state.asStateFlow()

    init {
        refreshSaved()
    }

    fun openLesson(id: String) = _state.update { it.copy(openLessonId = id, index = 0) }

    fun closeLesson() = _state.update { it.copy(openLessonId = null, index = 0) }

    fun goTo(index: Int) = _state.update { s ->
        val size = s.openLesson?.sentences?.size ?: return@update s
        s.copy(index = index.coerceIn(0, size - 1))
    }

    fun next() = goTo(_state.value.index + 1)

    fun previous() = goTo(_state.value.index - 1)

    fun toggleHanzi() = _state.update { it.copy(showHanzi = !it.showHanzi) }

    fun togglePinyin() = _state.update { it.copy(showPinyin = !it.showPinyin) }

    fun toggleTranslation() = _state.update { it.copy(showTranslation = !it.showTranslation) }

    fun speak(sentence: Sentence) = speech.speak(sentence.hanzi)

    fun saveForReview(sentence: Sentence) {
        viewModelScope.launch {
            cards.addSentence(sentence.id, clock.now())
            refreshSaved()
        }
    }

    private fun refreshSaved() {
        viewModelScope.launch {
            val saved = cards.savedSentenceIds()
            _state.update { it.copy(savedSentenceIds = saved) }
        }
    }
}
