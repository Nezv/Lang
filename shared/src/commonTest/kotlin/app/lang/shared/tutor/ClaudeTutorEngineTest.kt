package app.lang.shared.tutor

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration

class ClaudeTutorEngineTest {
    private val scripted = ScriptedTutorEngine(thinkingDelay = Duration.ZERO)
    private val cafe = scripted.scenarios.first { it.id == "cafe" }

    private fun turnJson(finished: Boolean = false, correction: String = "") = """
        {"text":"你想喝什么？","pinyin":"Nǐ xiǎng hē shénme?","translation":"What would you like to drink?",
         "correction":"$correction",
         "suggestions":[{"text":"我想喝茶。","pinyin":"Wǒ xiǎng hē chá.","translation":"I'd like tea."},
                        {"text":"咖啡，谢谢。","pinyin":"Kāfēi, xièxie.","translation":"Coffee, please."},
                        {"text":"third","pinyin":"x","translation":"x"}],
         "finished":$finished}
    """.trimIndent()

    private fun apiBody(text: String, stopReason: String = "end_turn"): String {
        val content = buildJsonArray {
            add(buildJsonObject { put("type", "thinking"); put("thinking", "") })
            add(buildJsonObject { put("type", "text"); put("text", text) })
        }
        return buildJsonObject { put("stop_reason", stopReason); put("content", content) }.toString()
    }

    private fun engine(captured: MutableList<HttpRequestData> = mutableListOf(), handler: () -> String = { apiBody(turnJson()) }) =
        ClaudeTutorEngine(
            client = HttpClient(
                MockEngine { request ->
                    captured += request
                    respond(handler(), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                },
            ),
            apiKey = "test-key",
            scenarios = scripted.scenarios,
        )

    @Test
    fun requestCarriesKeyModelSchemaAndScenarioPrompt() = runTest {
        val captured = mutableListOf<HttpRequestData>()
        val engine = engine(captured)
        val history = listOf(
            ChatMessage(Role.Tutor, "你想喝什么？"),
            ChatMessage(Role.Learner, "我想喝咖啡"),
        )
        engine.reply(cafe, history, "我想喝咖啡")

        val request = captured.single()
        assertEquals("https://api.anthropic.com/v1/messages", request.url.toString())
        assertEquals("test-key", request.headers["x-api-key"])
        assertEquals("2023-06-01", request.headers["anthropic-version"])
        assertEquals(ClaudeTutorEngine.FALLBACK_BETA, request.headers["anthropic-beta"])

        val body = Json.parseToJsonElement(request.body.toByteArray().decodeToString()).jsonObject
        assertEquals("claude-opus-5-5", body["model"]!!.jsonPrimitive.content)
        assertEquals("default", body["fallbacks"]!!.jsonPrimitive.content)
        assertTrue(body["system"]!!.jsonPrimitive.content.contains("HSK 1-2"))
        assertTrue(body["system"]!!.jsonPrimitive.content.contains(cafe.description))
        val format = body["output_config"]!!.jsonObject["format"]!!.jsonObject
        assertEquals("json_schema", format["type"]!!.jsonPrimitive.content)
        assertEquals("false", format["schema"]!!.jsonObject["additionalProperties"]!!.jsonPrimitive.content)
    }

    @Test
    fun messagesStartWithUserAndAlternate() = runTest {
        val captured = mutableListOf<HttpRequestData>()
        val engine = engine(captured)
        engine.opening(cafe)
        engine.reply(cafe, listOf(ChatMessage(Role.Tutor, "你想喝什么？")), "我想喝茶")

        suspend fun roles(request: HttpRequestData): List<String> =
            Json.parseToJsonElement(request.body.toByteArray().decodeToString()).jsonObject["messages"]!!.jsonArray
                .map { it.jsonObject["role"]!!.jsonPrimitive.content }

        assertEquals(listOf("user"), roles(captured[0]))
        // learnerText is appended when the history does not end with it yet
        assertEquals(listOf("user", "assistant", "user"), roles(captured[1]))
    }

    @Test
    fun responseMapsOntoTutorTurn() = runTest {
        val turn = engine { apiBody(turnJson(correction = "Say 我想喝咖啡.")) }.opening(cafe)
        assertEquals("你想喝什么？", turn.message.text)
        assertEquals("Nǐ xiǎng hē shénme?", turn.message.pinyin)
        assertEquals("What would you like to drink?", turn.message.translation)
        assertEquals("Say 我想喝咖啡.", turn.message.correction)
        assertEquals(Role.Tutor, turn.message.role)
        assertEquals(2, turn.suggestions.size)
        assertEquals(Phrase("我想喝茶。", "Wǒ xiǎng hē chá.", "I'd like tea."), turn.suggestions[0])
        assertFalse(turn.finished)
    }

    @Test
    fun finishedTurnHasNoHints() = runTest {
        val turn = engine { apiBody(turnJson(finished = true)) }.opening(cafe)
        assertTrue(turn.finished)
        assertTrue(turn.suggestions.isEmpty())
    }

    private suspend fun expectTutorException(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: TutorException) {
            return
        }
        fail("Expected a TutorException")
    }

    @Test
    fun unusableResponsesThrow() = runTest {
        expectTutorException { engine { apiBody(turnJson(), stopReason = "refusal") }.opening(cafe) }
        expectTutorException { engine { apiBody(turnJson(), stopReason = "max_tokens") }.opening(cafe) }
        expectTutorException { engine { apiBody("not json at all") }.opening(cafe) }
        expectTutorException { engine { "{}" }.opening(cafe) }
    }

    @Test
    fun fallsBackToScriptWhenAiFails() = runTest {
        val failing = ClaudeTutorEngine(
            HttpClient(MockEngine { respondError(HttpStatusCode.ServiceUnavailable) }),
            "k",
            scripted.scenarios,
        )
        val engine = FallbackTutorEngine(failing, ScriptedTutorEngine(Duration.ZERO))
        assertEquals(TutorMode.Ai, engine.mode.value)
        val opening = engine.opening(cafe)
        assertTrue(opening.message.text.contains("喝"))
        assertEquals(TutorMode.Unavailable, engine.mode.value)
        assertTrue(engine.reply(cafe, emptyList(), "我想喝咖啡。").message.text.contains("大杯"))
    }

    @Test
    fun aiTurnKeepsModeOnline() = runTest {
        val engine = FallbackTutorEngine(engine(), ScriptedTutorEngine(Duration.ZERO))
        assertEquals("你想喝什么？", engine.opening(cafe).message.text)
        assertEquals(TutorMode.Ai, engine.mode.value)
    }

    @Test
    fun noKeyMeansScriptedOnly() = runTest {
        val engine = createTutorEngine(apiKey = "  ")
        assertEquals(TutorMode.NoKey, engine.mode.value)
        assertTrue(engine.opening(cafe).message.text.contains("喝"))
    }
}
