package app.lang.shared.speech

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class SpeechStatus { Initializing, Ready, MissingChineseVoice, Unavailable }

/** Reads Chinese text aloud. Each platform provides its own engine. */
interface SpeechService {
    val status: StateFlow<SpeechStatus>

    fun speak(text: String)

    fun stop()
}

/** Used for previews, tests and screenshot rendering. */
object SilentSpeechService : SpeechService {
    override val status: StateFlow<SpeechStatus> = MutableStateFlow(SpeechStatus.Unavailable)

    override fun speak(text: String) = Unit

    override fun stop() = Unit
}
