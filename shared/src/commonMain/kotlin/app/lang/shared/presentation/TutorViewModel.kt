package app.lang.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lang.shared.speech.SpeechService
import app.lang.shared.tutor.ChatMessage
import app.lang.shared.tutor.Phrase
import app.lang.shared.tutor.Role
import app.lang.shared.tutor.Scenario
import app.lang.shared.tutor.TutorEngine
import app.lang.shared.tutor.TutorMode
import app.lang.shared.tutor.TutorTurn
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TutorState(
    val scenarios: List<Scenario>,
    val scenarioId: String,
    val messages: List<ChatMessage> = emptyList(),
    val suggestions: List<Phrase> = emptyList(),
    val thinking: Boolean = false,
    val finished: Boolean = false,
    val showPinyin: Boolean = true,
    val showTranslation: Boolean = false,
    val mode: TutorMode = TutorMode.NoKey,
) {
    val scenario: Scenario get() = scenarios.first { it.id == scenarioId }
}

/** Praktika-style role-play conversation with a tutor. */
class TutorViewModel(
    private val engine: TutorEngine,
    private val speech: SpeechService,
) : ViewModel() {
    private val _state = MutableStateFlow(TutorState(engine.scenarios, engine.scenarios.first().id))
    val state: StateFlow<TutorState> = _state.asStateFlow()
    private var pending: Job? = null

    init {
        viewModelScope.launch { engine.mode.collect { mode -> _state.update { it.copy(mode = mode) } } }
        start(engine.scenarios.first().id)
    }

    fun start(scenarioId: String) {
        pending?.cancel()
        _state.update {
            it.copy(scenarioId = scenarioId, messages = emptyList(), suggestions = emptyList(), thinking = true, finished = false)
        }
        pending = viewModelScope.launch { apply(engine.opening(_state.value.scenario)) }
    }

    fun send(text: String) {
        val trimmed = text.trim()
        val current = _state.value
        if (trimmed.isEmpty() || current.thinking || current.finished) return
        val history = current.messages + ChatMessage(Role.Learner, trimmed)
        _state.update { it.copy(messages = history, suggestions = emptyList(), thinking = true) }
        pending = viewModelScope.launch { apply(engine.reply(current.scenario, history, trimmed)) }
    }

    fun speak(message: ChatMessage) = speech.speak(message.text)

    fun togglePinyin() = _state.update { it.copy(showPinyin = !it.showPinyin) }

    fun toggleTranslation() = _state.update { it.copy(showTranslation = !it.showTranslation) }

    private fun apply(turn: TutorTurn) {
        _state.update {
            it.copy(messages = it.messages + turn.message, suggestions = turn.suggestions, thinking = false, finished = turn.finished)
        }
        speech.speak(turn.message.text)
    }
}
