package app.lang.shared.tutor

import app.lang.shared.network.createHttpClient

/** Builds the tutor for a platform: AI with scripted fallback when [apiKey] is set, scripted only otherwise. */
fun createTutorEngine(apiKey: String?, model: String? = null): TutorEngine {
    val scripted = ScriptedTutorEngine()
    val key = apiKey?.trim().orEmpty()
    val ai = if (key.isEmpty()) {
        null
    } else {
        ClaudeTutorEngine(createHttpClient(), key, scripted.scenarios, model?.takeIf { it.isNotBlank() } ?: ClaudeTutorEngine.DEFAULT_MODEL)
    }
    return FallbackTutorEngine(ai, scripted)
}
