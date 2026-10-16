package app.lang.shared.content

import app.lang.shared.model.Lesson
import app.lang.shared.model.Sentence
import app.lang.shared.model.Token

/**
 * Built-in starter lessons (HSK 1 level) so the app is usable offline from the first launch.
 * Pinyin uses dictionary tones; the only sandhi written out are 不 (bú) and 一 (yí), as most
 * textbooks do. Later this moves to downloadable JSON content packs (see TODO.md).
 */
internal object SeedContent {
    private fun w(hanzi: String, pinyin: String, gloss: String) = Token(hanzi, pinyin, gloss)
    private fun p(mark: String) = Token(mark)
    private fun s(id: String, translation: String, vararg tokens: Token) = Sentence(id, tokens.toList(), translation)

    val lessons: List<Lesson> = listOf(
        Lesson(
            id = "greetings",
            title = "Greetings",
            titleHanzi = "问候",
            level = "HSK 1",
            description = "Say hello, introduce yourself, thank people and say goodbye.",
            sentences = listOf(
                s("greet-1", "Hello!", w("你好", "nǐ hǎo", "hello"), p("！")),
                s(
                    "greet-2", "My name is Li Ming.",
                    w("我", "wǒ", "I; me"), w("叫", "jiào", "to be called"),
                    w("李明", "Lǐ Míng", "Li Ming (a name)"), p("。"),
                ),
                s("greet-3", "Thank you!", w("谢谢", "xiè xie", "to thank"), w("你", "nǐ", "you"), p("！")),
                s("greet-4", "You're welcome.", w("不客气", "bú kè qi", "you're welcome"), p("。")),
                s("greet-5", "See you tomorrow!", w("明天", "míng tiān", "tomorrow"), w("见", "jiàn", "to see; to meet"), p("！")),
            ),
        ),
        Lesson(
            id = "getting-to-know",
            title = "Getting to know you",
            titleHanzi = "认识你",
            level = "HSK 1",
            description = "Talk about where you are from and why you study Chinese.",
            sentences = listOf(
                s(
                    "know-1", "Which country are you from?",
                    w("你", "nǐ", "you"), w("是", "shì", "to be"), w("哪", "nǎ", "which"),
                    w("国", "guó", "country"), w("人", "rén", "person"), p("？"),
                ),
                s(
                    "know-2", "I'm British.",
                    w("我", "wǒ", "I; me"), w("是", "shì", "to be"), w("英国", "Yīng guó", "the UK"),
                    w("人", "rén", "person"), p("。"),
                ),
                s(
                    "know-3", "I'm studying Chinese.",
                    w("我", "wǒ", "I; me"), w("在", "zài", "(doing) right now"), w("学习", "xué xí", "to study"),
                    w("中文", "Zhōng wén", "Chinese (language)"), p("。"),
                ),
                s("know-4", "I don't understand (what I hear).", w("我", "wǒ", "I; me"), w("听不懂", "tīng bu dǒng", "can't understand by listening"), p("。")),
                s(
                    "know-5", "Please say it again.",
                    w("请", "qǐng", "please"), w("再", "zài", "again"), w("说", "shuō", "to say; to speak"),
                    w("一遍", "yí biàn", "one time (through)"), p("。"),
                ),
            ),
        ),
        Lesson(
            id = "cafe",
            title = "At the café",
            titleHanzi = "在咖啡馆",
            level = "HSK 1",
            description = "Order drinks, ask prices and make small talk.",
            sentences = listOf(
                s("cafe-1", "Do you drink tea?", w("你", "nǐ", "you"), w("喝", "hē", "to drink"), w("茶", "chá", "tea"), w("吗", "ma", "(question particle)"), p("？")),
                s("cafe-2", "I'd like a coffee.", w("我", "wǒ", "I; me"), w("想", "xiǎng", "would like to"), w("喝", "hē", "to drink"), w("咖啡", "kā fēi", "coffee"), p("。")),
                s("cafe-3", "How much is this?", w("这个", "zhè ge", "this one"), w("多少", "duō shao", "how much; how many"), w("钱", "qián", "money"), p("？")),
                s("cafe-4", "The weather is nice today.", w("今天", "jīn tiān", "today"), w("天气", "tiān qì", "weather"), w("很", "hěn", "very"), w("好", "hǎo", "good"), p("。")),
                s("cafe-5", "Where is the restroom?", w("洗手间", "xǐ shǒu jiān", "restroom"), w("在", "zài", "to be (located) at"), w("哪儿", "nǎr", "where"), p("？")),
            ),
        ),
    )
}
