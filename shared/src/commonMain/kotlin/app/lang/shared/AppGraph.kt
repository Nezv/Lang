package app.lang.shared

import app.cash.sqldelight.db.SqlDriver
import app.lang.shared.content.ContentRepository
import app.lang.shared.data.CardRepository
import app.lang.shared.db.LangDatabase
import app.lang.shared.presentation.LearnViewModel
import app.lang.shared.presentation.ReviewViewModel
import app.lang.shared.presentation.TodayViewModel
import app.lang.shared.presentation.TutorViewModel
import app.lang.shared.speech.SpeechService
import app.lang.shared.srs.Scheduler
import app.lang.shared.tutor.TutorEngine
import app.lang.shared.tutor.createTutorEngine
import kotlinx.coroutines.CoroutineDispatcher
import kotlin.time.Clock

/**
 * Hand-written dependency graph shared by both apps. Platform code only supplies the pieces that
 * differ (database driver, speech engine, IO dispatcher) via `createAppGraph(...)`.
 */
class AppGraph(
    driver: SqlDriver,
    val speech: SpeechService,
    io: CoroutineDispatcher,
    private val clock: Clock = Clock.System,
    private val tutor: TutorEngine = createTutorEngine(apiKey = null),
) {
    private val database = LangDatabase(driver)

    val content = ContentRepository()
    val cards = CardRepository(database, io)
    private val scheduler = Scheduler()

    fun todayViewModel() = TodayViewModel(content, cards, clock)

    fun learnViewModel() = LearnViewModel(content, cards, speech, clock)

    fun reviewViewModel() = ReviewViewModel(content, cards, scheduler, speech, clock)

    fun tutorViewModel() = TutorViewModel(tutor, speech)
}
