package app.lang.shared.model

/**
 * One word (or punctuation mark) of a sentence.
 *
 * [pinyin] holds one syllable per character, separated by spaces ("nǐ hǎo"), so a GUI can place
 * each syllable above its character. Erhua words such as 哪儿 have one syllable fewer than
 * characters; GUIs then show the whole word's pinyin centred above the word.
 */
data class Token(
    val hanzi: String,
    val pinyin: String = "",
    val gloss: String = "",
) {
    val isPunctuation: Boolean get() = pinyin.isBlank()
    val syllables: List<String> get() = Pinyin.syllables(pinyin)

    /** Pinyin as written in running text: the syllables of one word are joined ("Yīngguó"). */
    val pinyinWord: String get() = syllables.joinToString("")

    /** All characters used by the app are in the Basic Multilingual Plane, so one Char = one hanzi. */
    val characters: List<String> get() = hanzi.map { it.toString() }
}

data class Sentence(
    val id: String,
    val tokens: List<Token>,
    val translation: String,
) {
    val hanzi: String get() = tokens.joinToString("") { it.hanzi }

    /** Whole-sentence pinyin in standard running-text form, e.g. "Wǒ shì Yīngguó rén." */
    val pinyin: String
        get() = buildString {
            for (token in tokens) {
                if (token.isPunctuation) {
                    append(westernPunctuation(token.hanzi))
                } else {
                    if (isNotEmpty()) append(' ')
                    append(token.pinyinWord)
                }
            }
        }.replaceFirstChar { it.uppercaseChar() }
}

data class Lesson(
    val id: String,
    val title: String,
    val titleHanzi: String,
    val level: String,
    val description: String,
    val sentences: List<Sentence>,
)

private fun westernPunctuation(mark: String): String = when (mark) {
    "。" -> "."
    "，", "、" -> ","
    "！" -> "!"
    "？" -> "?"
    "：" -> ":"
    "；" -> ";"
    else -> mark
}
