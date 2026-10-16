package app.lang.shared.tutor

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/** The Claude API answered, but not with a usable tutor turn (refusal, cut-off, malformed JSON). */
class TutorException(message: String) : Exception(message)

/**
 * AI tutor: free-form role-play through the Claude Messages API (raw HTTP over Ktor, since the
 * official SDKs are JVM-only). Every reply is constrained to a JSON schema so the GUI always gets
 * hanzi, pinyin, translation, two hints, an optional correction and a `finished` flag.
 */
class ClaudeTutorEngine(
    private val client: HttpClient,
    private val apiKey: String,
    override val scenarios: List<Scenario>,
    private val model: String = DEFAULT_MODEL,
    private val baseUrl: String = "https://api.anthropic.com",
) : TutorEngine {

    override suspend fun opening(scenario: Scenario): TutorTurn = ask(scenario, emptyList())

    override suspend fun reply(scenario: Scenario, history: List<ChatMessage>, learnerText: String): TutorTurn {
        val last = history.lastOrNull()
        val full = if (last?.role == Role.Learner && last.text == learnerText) history else history + ChatMessage(Role.Learner, learnerText)
        return ask(scenario, full)
    }

    private suspend fun ask(scenario: Scenario, history: List<ChatMessage>): TutorTurn {
        val response = client.post("$baseUrl/v1/messages") {
            header("x-api-key", apiKey)
            header("anthropic-version", API_VERSION)
            header("anthropic-beta", FALLBACK_BETA)
            contentType(ContentType.Application.Json)
            setBody(buildRequest(scenario, history).toString())
        }
        return parseResponse(response.bodyAsText())
    }

    internal fun buildRequest(scenario: Scenario, history: List<ChatMessage>): JsonObject = buildJsonObject {
        put("model", model)
        put("max_tokens", MAX_TOKENS)
        // If a safety classifier declines the request, the API retries it on its recommended model.
        put("fallbacks", "default")
        put("system", systemPrompt(scenario))
        putJsonObject("output_config") {
            put("effort", "low")
            putJsonObject("format") {
                put("type", "json_schema")
                put("schema", TURN_SCHEMA)
            }
        }
        putJsonArray("messages") {
            // The API wants the first message from the user, so the tutor's opening line is always
            // preceded by a stage cue; later messages alternate learner (user) / tutor (assistant).
            add(message("user", START_CUE))
            history.forEach { add(message(if (it.role == Role.Tutor) "assistant" else "user", it.text)) }
        }
    }

    internal fun parseResponse(body: String): TutorTurn {
        val root = try {
            Json.parseToJsonElement(body).jsonObject
        } catch (e: Exception) {
            throw TutorException("Response is not JSON")
        }
        val stop = root["stop_reason"]?.jsonPrimitive?.contentOrNull
        if (stop == "refusal") throw TutorException("The request was declined")
        if (stop == "max_tokens") throw TutorException("The reply was cut off")
        val text = root["content"]?.jsonArray
            ?.firstOrNull { it.jsonObject["type"]?.jsonPrimitive?.contentOrNull == "text" }
            ?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull
            ?: throw TutorException("Response has no text")
        val turn = try {
            lenientJson.decodeFromString<TurnDto>(text)
        } catch (e: Exception) {
            throw TutorException("Reply does not match the tutor format")
        }
        if (turn.text.isBlank()) throw TutorException("Reply is empty")
        val message = ChatMessage(Role.Tutor, turn.text.trim(), turn.pinyin.trim(), turn.translation.trim(), turn.correction.trim())
        val hints = turn.suggestions.filter { it.text.isNotBlank() }.take(2).map { Phrase(it.text.trim(), it.pinyin.trim(), it.translation.trim()) }
        return TutorTurn(message, if (turn.finished) emptyList() else hints, turn.finished)
    }

    @Serializable
    private class PhraseDto(val text: String = "", val pinyin: String = "", val translation: String = "")

    @Serializable
    private class TurnDto(
        val text: String = "",
        val pinyin: String = "",
        val translation: String = "",
        val correction: String = "",
        val suggestions: List<PhraseDto> = emptyList(),
        val finished: Boolean = false,
    )

    internal companion object {
        const val DEFAULT_MODEL = "claude-opus-5-5"
        const val API_VERSION = "2023-06-01"
        const val FALLBACK_BETA = "server-side-fallback-2026-07-01"
        const val MAX_TOKENS = 4000
        const val START_CUE = "(The scene begins. Please say your first line.)"

        private val lenientJson = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

        private fun message(role: String, text: String) = buildJsonObject {
            put("role", role)
            put("content", text)
        }

        fun systemPrompt(scenario: Scenario): String = """
            You are ${scenario.tutorName}, a friendly Mandarin Chinese conversation partner in a language-learning app. The learner is an English speaker at HSK 1-2 level.

            Scenario: ${scenario.title} (${scenario.titleHanzi}). ${scenario.description}

            Rules:
            - Stay inside this scenario and in character. Keep the conversation natural and free-form, but steer it back gently if the learner drifts off topic.
            - Speak simplified Chinese using only HSK 1-2 vocabulary and grammar. One to two short sentences per turn. Ask one simple question at a time.
            - Every Chinese text field needs pinyin with tone marks (for example "Nǐ hǎo"), never tone numbers. Translations are natural English.
            - If the learner writes English or something unclear, answer in simple Chinese and encourage them to try in Chinese.
            - "correction": if the learner's last message had a mistake, give a gentle one-sentence English tip with the corrected Chinese (and pinyin). Praise is fine. Use an empty string when the message was correct or there is no learner message yet. Never scold.
            - "suggestions": exactly two different replies the learner could say next, short and at HSK 1-2 level, each with pinyin and translation. Use an empty list when finished.
            - Set "finished" to true only once the scenario has reached a natural ending (for example after paying, or after saying goodbye). Your final turn should be a short farewell.
        """.trimIndent()

        private fun stringProps(vararg fields: String) = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") { fields.forEach { putJsonObject(it) { put("type", "string") } } }
            put("required", JsonArray(fields.map { JsonPrimitive(it) }))
            put("additionalProperties", false)
        }

        val TURN_SCHEMA: JsonObject = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("text") {
                    put("type", "string")
                    put("description", "Tutor line in simplified Chinese")
                }
                putJsonObject("pinyin") {
                    put("type", "string")
                    put("description", "Pinyin with tone marks for text")
                }
                putJsonObject("translation") {
                    put("type", "string")
                    put("description", "English translation of text")
                }
                putJsonObject("correction") {
                    put("type", "string")
                    put("description", "Gentle note on the learner's last message, or empty")
                }
                putJsonObject("suggestions") {
                    put("type", "array")
                    put("items", stringProps("text", "pinyin", "translation"))
                }
                putJsonObject("finished") { put("type", "boolean") }
            }
            put("required", JsonArray(listOf("text", "pinyin", "translation", "correction", "suggestions", "finished").map { JsonPrimitive(it) }))
            put("additionalProperties", false)
        }
    }
}
