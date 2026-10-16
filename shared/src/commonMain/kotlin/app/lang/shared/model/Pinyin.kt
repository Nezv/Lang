package app.lang.shared.model

/** Helpers for pinyin written with tone marks (ā á ǎ à). */
object Pinyin {
    private val toneMarks = mapOf(
        1 to "āēīōūǖĀĒĪŌŪǕ",
        2 to "áéíóúǘÁÉÍÓÚǗ",
        3 to "ǎěǐǒǔǚǍĚǏǑǓǙ",
        4 to "àèìòùǜÀÈÌÒÙǛ",
    )

    /** Tone 1–4 from the first tone mark found, or 5 for the neutral tone (no mark). */
    fun tone(syllable: String): Int {
        for (ch in syllable) {
            for ((tone, marks) in toneMarks) {
                if (ch in marks) return tone
            }
        }
        return 5
    }

    fun syllables(pinyin: String): List<String> = pinyin.split(' ').filter { it.isNotBlank() }
}
