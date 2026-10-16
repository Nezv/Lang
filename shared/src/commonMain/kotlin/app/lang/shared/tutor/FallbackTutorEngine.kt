package app.lang.shared.tutor

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Uses the AI engine when there is one and falls back to the scripted tutor when there is no key,
 * no network or the API fails. Each scenario restart tries the AI again; a failure in the middle of
 * a conversation switches that conversation to the script (restarting it) until the next restart.
 */
class FallbackTutorEngine(
    private val ai: TutorEngine?,
    private val scripted: TutorEngine,
) : TutorEngine {
    override val scenarios: List<Scenario> = scripted.scenarios

    private val _mode = MutableStateFlow(if (ai == null) TutorMode.NoKey else TutorMode.Ai)
    override val mode: StateFlow<TutorMode> = _mode.asStateFlow()

    private var usingScript = ai == null

    override suspend fun opening(scenario: Scenario): TutorTurn {
        if (ai != null) {
            try {
                val turn = ai.opening(scenario)
                usingScript = false
                _mode.value = TutorMode.Ai
                return turn
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                usingScript = true
                _mode.value = TutorMode.Unavailable
            }
        }
        return scripted.opening(scenario)
    }

    override suspend fun reply(scenario: Scenario, history: List<ChatMessage>, learnerText: String): TutorTurn {
        if (!usingScript && ai != null) {
            try {
                return ai.reply(scenario, history, learnerText)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                usingScript = true
                _mode.value = TutorMode.Unavailable
                return scripted.opening(scenario)
            }
        }
        return scripted.reply(scenario, history, learnerText)
    }
}
