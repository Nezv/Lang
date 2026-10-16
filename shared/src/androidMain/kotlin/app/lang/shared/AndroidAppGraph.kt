package app.lang.shared

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import app.lang.shared.db.LangDatabase
import app.lang.shared.speech.AndroidSpeechService
import app.lang.shared.tutor.createTutorEngine
import kotlinx.coroutines.Dispatchers

/** [tutorApiKey] comes from the build (private beta only, see README); blank means the offline tutor. */
fun createAppGraph(context: Context, tutorApiKey: String? = null, tutorModel: String? = null): AppGraph = AppGraph(
    driver = AndroidSqliteDriver(LangDatabase.Schema, context.applicationContext, "lang.db"),
    speech = AndroidSpeechService(context.applicationContext),
    io = Dispatchers.IO,
    tutor = createTutorEngine(tutorApiKey, tutorModel),
)
