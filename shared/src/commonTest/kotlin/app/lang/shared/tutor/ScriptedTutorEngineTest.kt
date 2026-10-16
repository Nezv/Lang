package app.lang.shared.tutor

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration

class ScriptedTutorEngineTest {
    private val engine = ScriptedTutorEngine(thinkingDelay = Duration.ZERO)

    @Test
    fun cafeConversationAdvancesOnlyOnRelevantChineseAnswers() = runTest {
        val cafe = engine.scenarios.first { it.id == "cafe" }
        val opening = engine.opening(cafe)
        assertTrue(opening.message.text.contains("喝"))
        assertEquals(2, opening.suggestions.size)

        val nudge = engine.reply(cafe, emptyList(), "coffee please")
        assertTrue(nudge.message.translation.contains("Chinese"))

        val reprompt = engine.reply(cafe, emptyList(), "我想喝水")
        assertFalse(reprompt.finished)
        assertTrue(reprompt.message.text.contains("咖啡和茶"))

        assertTrue(engine.reply(cafe, emptyList(), "我想喝咖啡。").message.text.contains("大杯"))
        assertTrue(engine.reply(cafe, emptyList(), "大杯，谢谢。").message.text.contains("二十五"))
        assertTrue(engine.reply(cafe, emptyList(), "给你。").finished)
    }

    @Test
    fun openingRestartsTheScript() = runTest {
        val meeting = engine.scenarios.first { it.id == "meeting" }
        engine.opening(meeting)
        engine.reply(meeting, emptyList(), "我叫安娜。")
        val restarted = engine.opening(meeting)
        assertTrue(restarted.message.text.contains("名字"))
    }
}
