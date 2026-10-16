package app.lang.shared.speech

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Windows text-to-speech through the built-in System.Speech API, driven by PowerShell.
 *
 * First version: simple and dependency-free, but ~0.5 s start-up per sentence and it needs a
 * Chinese voice installed in Windows. The text is passed through an environment variable, never
 * spliced into the command, so it cannot inject PowerShell code.
 */
class WindowsSpeechService(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : SpeechService {
    private val _status = MutableStateFlow(SpeechStatus.Initializing)
    override val status: StateFlow<SpeechStatus> = _status.asStateFlow()

    @Volatile
    private var current: Process? = null

    init {
        scope.launch {
            _status.value = if (!isWindows) SpeechStatus.Unavailable else runCatching { detect() }.getOrDefault(SpeechStatus.Unavailable)
        }
    }

    override fun speak(text: String) {
        if (_status.value != SpeechStatus.Ready || text.isBlank()) return
        scope.launch {
            stop()
            current = powershell(SPEAK_SCRIPT, mapOf(TEXT_VARIABLE to text))
        }
    }

    override fun stop() {
        current?.destroy()
        current = null
    }

    private fun detect(): SpeechStatus {
        val process = powershell(LIST_VOICES_SCRIPT, emptyMap())
        val cultures = process.inputStream.bufferedReader().readLines()
        process.waitFor()
        return if (cultures.any { it.trim().startsWith("zh", ignoreCase = true) }) SpeechStatus.Ready else SpeechStatus.MissingChineseVoice
    }

    private fun powershell(script: String, environment: Map<String, String>): Process =
        ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-Command", script)
            .redirectErrorStream(true)
            .apply { environment().putAll(environment) }
            .start()

    private companion object {
        val isWindows = System.getProperty("os.name").orEmpty().startsWith("Windows", ignoreCase = true)

        const val TEXT_VARIABLE = "LANG_TTS_TEXT"

        const val LIST_VOICES_SCRIPT =
            "Add-Type -AssemblyName System.Speech; " +
                "(New-Object System.Speech.Synthesis.SpeechSynthesizer).GetInstalledVoices() | " +
                "ForEach-Object { \$_.VoiceInfo.Culture.Name }"

        const val SPEAK_SCRIPT =
            "Add-Type -AssemblyName System.Speech; " +
                "\$s = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                "\$v = \$s.GetInstalledVoices() | Where-Object { \$_.VoiceInfo.Culture.Name -like 'zh*' } | Select-Object -First 1; " +
                "if (\$v) { \$s.SelectVoice(\$v.VoiceInfo.Name) }; " +
                "\$s.Rate = -1; " +
                "\$s.Speak(\$env:$TEXT_VARIABLE)"
    }
}
