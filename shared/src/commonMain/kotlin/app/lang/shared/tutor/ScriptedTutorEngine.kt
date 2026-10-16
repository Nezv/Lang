package app.lang.shared.tutor

import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Offline tutor that walks through fixed role-play scripts. It lets the whole conversation GUI be
 * built and tested before the AI integration, and stays as the no-network fallback.
 */
class ScriptedTutorEngine(private val thinkingDelay: Duration = 700.milliseconds) : TutorEngine {

    private class Step(
        val prompt: ChatMessage,
        val suggestions: List<Phrase>,
        val reprompt: ChatMessage,
        val accepts: (String) -> Boolean,
    )

    private class Script(val scenario: Scenario, val steps: List<Step>, val closing: ChatMessage)

    private val scripts = listOf(cafeScript(), meetingScript())
    private val progress = mutableMapOf<String, Int>()

    override val scenarios: List<Scenario> = scripts.map { it.scenario }

    override suspend fun opening(scenario: Scenario): TutorTurn {
        delay(thinkingDelay)
        progress[scenario.id] = 0
        val first = script(scenario).steps.first()
        return TutorTurn(first.prompt, first.suggestions)
    }

    override suspend fun reply(scenario: Scenario, history: List<ChatMessage>, learnerText: String): TutorTurn {
        delay(thinkingDelay)
        val script = script(scenario)
        val index = progress[scenario.id] ?: 0
        if (index >= script.steps.size) return TutorTurn(script.closing, finished = true)
        val step = script.steps[index]

        if (!containsHanzi(learnerText)) return TutorTurn(tryInChinese, step.suggestions)
        if (!step.accepts(learnerText)) return TutorTurn(step.reprompt, step.suggestions)

        val next = index + 1
        progress[scenario.id] = next
        return if (next < script.steps.size) {
            TutorTurn(script.steps[next].prompt, script.steps[next].suggestions)
        } else {
            TutorTurn(script.closing, finished = true)
        }
    }

    private fun script(scenario: Scenario) = scripts.first { it.scenario.id == scenario.id }

    private companion object {
        fun containsHanzi(text: String) = text.any { it in '一'..'鿿' }

        fun tutor(text: String, pinyin: String, translation: String) = ChatMessage(Role.Tutor, text, pinyin, translation)

        val tryInChinese = tutor("试试用中文回答吧！", "Shìshi yòng Zhōngwén huídá ba!", "Try answering in Chinese!")

        fun cafeScript() = Script(
            scenario = Scenario(
                id = "cafe",
                title = "Order a drink",
                titleHanzi = "点饮料",
                description = "You are at a café in Beijing. Order a drink and pay.",
                tutorName = "小林 · barista",
            ),
            steps = listOf(
                Step(
                    prompt = tutor("你好！欢迎光临。你想喝什么？", "Nǐ hǎo! Huānyíng guānglín. Nǐ xiǎng hē shénme?", "Hello, welcome! What would you like to drink?"),
                    suggestions = listOf(
                        Phrase("我想喝咖啡。", "Wǒ xiǎng hē kāfēi.", "I'd like a coffee."),
                        Phrase("我想喝茶。", "Wǒ xiǎng hē chá.", "I'd like some tea."),
                    ),
                    reprompt = tutor("不好意思，我们有咖啡和茶。你想喝哪个？", "Bù hǎoyìsi, wǒmen yǒu kāfēi hé chá. Nǐ xiǎng hē nǎge?", "Sorry, we have coffee and tea. Which would you like?"),
                    accepts = { it.contains("咖啡") || it.contains("茶") },
                ),
                Step(
                    prompt = tutor("好的。你要大杯还是小杯？", "Hǎo de. Nǐ yào dà bēi háishi xiǎo bēi?", "Sure. A large or a small?"),
                    suggestions = listOf(
                        Phrase("大杯，谢谢。", "Dà bēi, xièxie.", "A large one, thanks."),
                        Phrase("小杯，谢谢。", "Xiǎo bēi, xièxie.", "A small one, thanks."),
                    ),
                    reprompt = tutor("你要大杯还是小杯？", "Nǐ yào dà bēi háishi xiǎo bēi?", "Large or small?"),
                    accepts = { it.contains("大") || it.contains("小") },
                ),
                Step(
                    prompt = tutor("一共二十五块。", "Yígòng èrshíwǔ kuài.", "That's 25 yuan altogether."),
                    suggestions = listOf(
                        Phrase("给你。", "Gěi nǐ.", "Here you are."),
                        Phrase("可以用手机付吗？", "Kěyǐ yòng shǒujī fù ma?", "Can I pay with my phone?"),
                    ),
                    reprompt = tutor("一共二十五块。", "Yígòng èrshíwǔ kuài.", "That's 25 yuan altogether."),
                    accepts = { true },
                ),
            ),
            closing = tutor("好的，谢谢！再见！", "Hǎo de, xièxie! Zàijiàn!", "Great, thank you! Goodbye!"),
        )

        fun meetingScript() = Script(
            scenario = Scenario(
                id = "meeting",
                title = "Meet your teacher",
                titleHanzi = "认识老师",
                description = "Introduce yourself to Teacher Wang on your first day of class.",
                tutorName = "王老师 · teacher",
            ),
            steps = listOf(
                Step(
                    prompt = tutor("你好！我是王老师。你叫什么名字？", "Nǐ hǎo! Wǒ shì Wáng lǎoshī. Nǐ jiào shénme míngzi?", "Hi! I'm Teacher Wang. What's your name?"),
                    suggestions = listOf(
                        Phrase("我叫李明。", "Wǒ jiào Lǐ Míng.", "My name is Li Ming."),
                        Phrase("我叫安娜。", "Wǒ jiào Ānnà.", "My name is Anna."),
                    ),
                    reprompt = tutor("你叫什么名字？", "Nǐ jiào shénme míngzi?", "What's your name?"),
                    accepts = { true },
                ),
                Step(
                    prompt = tutor("很高兴认识你！你是哪国人？", "Hěn gāoxìng rènshi nǐ! Nǐ shì nǎ guó rén?", "Nice to meet you! Where are you from?"),
                    suggestions = listOf(
                        Phrase("我是英国人。", "Wǒ shì Yīngguó rén.", "I'm British."),
                        Phrase("我是巴西人。", "Wǒ shì Bāxī rén.", "I'm Brazilian."),
                    ),
                    reprompt = tutor("你是哪国人？比如：我是英国人。", "Nǐ shì nǎ guó rén? Bǐrú: wǒ shì Yīngguó rén.", "Which country are you from? For example: I'm British."),
                    accepts = { it.contains("人") || it.contains("国") },
                ),
                Step(
                    prompt = tutor("你为什么学习中文？", "Nǐ wèishénme xuéxí Zhōngwén?", "Why are you learning Chinese?"),
                    suggestions = listOf(
                        Phrase("因为我喜欢中国文化。", "Yīnwèi wǒ xǐhuan Zhōngguó wénhuà.", "Because I like Chinese culture."),
                        Phrase("因为我想去中国旅行。", "Yīnwèi wǒ xiǎng qù Zhōngguó lǚxíng.", "Because I want to travel in China."),
                    ),
                    reprompt = tutor("你为什么学习中文？", "Nǐ wèishénme xuéxí Zhōngwén?", "Why are you learning Chinese?"),
                    accepts = { true },
                ),
            ),
            closing = tutor("太好了！加油！明天见！", "Tài hǎo le! Jiāyóu! Míngtiān jiàn!", "Great! Keep it up! See you tomorrow!"),
        )
    }
}
