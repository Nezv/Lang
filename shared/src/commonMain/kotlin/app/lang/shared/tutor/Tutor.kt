package app.lang.shared.tutor

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class Role { Tutor, Learner }

data class ChatMessage(
    val role: Role,
    val text: String,
    val pinyin: String = "",
    val translation: String = "",
    /** Short note on the learner's previous message (AI tutor only); empty when there is nothing to fix. */
    val correction: String = "",
)

/** A ready-made reply the learner can tap (Praktika-style hints). */
data class Phrase(val text: String, val pinyin: String, val translation: String)

data class Scenario(
    val id: String,
    val title: String,
    val titleHanzi: String,
    val description: String,
    val tutorName: String,
)

data class TutorTurn(
    val message: ChatMessage,
    val suggestions: List<Phrase> = emptyList(),
    val finished: Boolean = false,
)

/** Which tutor is currently answering; the GUIs show [hint] as a small status line. */
enum class TutorMode(val hint: String) {
    Ai("AI tutor online"),
    NoKey("Offline practice: no API key set"),
    Unavailable("Offline practice: AI tutor unreachable"),
}

/**
 * Conversation partner for role-play practice. [ClaudeTutorEngine] talks to the Claude API; the
 * offline [ScriptedTutorEngine] is the fallback (see [FallbackTutorEngine]).
 */
interface TutorEngine {
    val scenarios: List<Scenario>

    /** Current mode; the scripted engine on its own is always [TutorMode.NoKey]. */
    val mode: StateFlow<TutorMode> get() = MutableStateFlow(TutorMode.NoKey)

    suspend fun opening(scenario: Scenario): TutorTurn

    suspend fun reply(scenario: Scenario, history: List<ChatMessage>, learnerText: String): TutorTurn
}
