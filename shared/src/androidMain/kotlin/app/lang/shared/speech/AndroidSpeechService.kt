package app.lang.shared.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/** Uses the phone's built-in text-to-speech engine (Google TTS on most devices). */
class AndroidSpeechService(context: Context) : SpeechService, TextToSpeech.OnInitListener {
    private val _status = MutableStateFlow(SpeechStatus.Initializing)
    override val status: StateFlow<SpeechStatus> = _status.asStateFlow()

    private val tts = TextToSpeech(context, this)

    override fun onInit(result: Int) {
        if (result != TextToSpeech.SUCCESS) {
            _status.value = SpeechStatus.Unavailable
            return
        }
        val language = tts.setLanguage(Locale.SIMPLIFIED_CHINESE)
        _status.value = if (language == TextToSpeech.LANG_MISSING_DATA || language == TextToSpeech.LANG_NOT_SUPPORTED) {
            SpeechStatus.MissingChineseVoice
        } else {
            tts.setSpeechRate(0.9f)
            SpeechStatus.Ready
        }
    }

    override fun speak(text: String) {
        if (_status.value != SpeechStatus.Ready || text.isBlank()) return
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "lang-${text.hashCode()}")
    }

    override fun stop() {
        tts.stop()
    }
}
