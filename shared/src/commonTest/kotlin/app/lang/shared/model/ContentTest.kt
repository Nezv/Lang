package app.lang.shared.model

import app.lang.shared.content.ContentRepository
import app.lang.shared.presentation.formatInterval
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ContentTest {
    @Test
    fun detectsTonesFromMarks() {
        assertEquals(1, Pinyin.tone("mā"))
        assertEquals(2, Pinyin.tone("má"))
        assertEquals(3, Pinyin.tone("mǎ"))
        assertEquals(4, Pinyin.tone("mà"))
        assertEquals(5, Pinyin.tone("ma"))
        assertEquals(4, Pinyin.tone("lǜ"))
    }

    @Test
    fun sentencePinyinJoinsWordsAndPunctuation() {
        val sentence = Sentence(
            id = "t",
            tokens = listOf(Token("我", "wǒ"), Token("是", "shì"), Token("英国", "Yīng guó"), Token("人", "rén"), Token("。")),
            translation = "I'm British.",
        )
        assertEquals("Wǒ shì Yīngguó rén.", sentence.pinyin)
        assertEquals("我是英国人。", sentence.hanzi)
    }

    @Test
    fun seedContentIsWellFormed() {
        val sentences = ContentRepository().lessons().flatMap { it.sentences }
        assertTrue(sentences.isNotEmpty())
        assertEquals(sentences.size, sentences.map { it.id }.toSet().size, "sentence ids must be unique")
        sentences.flatMap { it.tokens }.filterNot { it.isPunctuation }.forEach { token ->
            val chars = token.characters.size
            val syllables = token.syllables.size
            val erhua = token.hanzi.endsWith("儿") && syllables == chars - 1
            assertTrue(syllables == chars || erhua, "pinyin/hanzi mismatch in ${token.hanzi}")
            assertTrue(token.gloss.isNotBlank(), "missing gloss for ${token.hanzi}")
        }
    }

    @Test
    fun intervalLabelsLookLikeAnki() {
        assertEquals("1m", formatInterval(1.minutes))
        assertEquals("6m", formatInterval(5.minutes + 30.seconds))
        assertEquals("8d", formatInterval(8.days))
        assertEquals("1.5mo", formatInterval(45.days))
        assertEquals("2y", formatInterval(730.days))
    }
}
