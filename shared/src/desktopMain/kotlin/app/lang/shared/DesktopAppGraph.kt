package app.lang.shared

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.lang.shared.db.LangDatabase
import app.lang.shared.speech.SilentSpeechService
import app.lang.shared.speech.WindowsSpeechService
import app.lang.shared.tutor.createTutorEngine
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.util.Properties

/** User data lives in %APPDATA%\Lang (roams with the Windows profile). */
fun defaultDataDir(): File = File(System.getenv("APPDATA") ?: System.getProperty("user.home"), "Lang")

fun createAppGraph(dataDir: File = defaultDataDir()): AppGraph {
    dataDir.mkdirs()
    val url = "jdbc:sqlite:" + File(dataDir, "lang.db").absolutePath
    return AppGraph(
        driver = JdbcSqliteDriver(url, Properties(), LangDatabase.Schema),
        speech = WindowsSpeechService(),
        io = Dispatchers.IO,
        tutor = createTutorEngine(apiKey = readSetting("ANTHROPIC_API_KEY", "anthropic.api.key", dataDir), model = readSetting("LANG_TUTOR_MODEL", "tutor.model", dataDir)),
    )
}

/**
 * Private-beta key lookup, first hit wins: environment variable, then a `secrets.properties` file in
 * the working directory (gitignored), then `%APPDATA%\Lang\secrets.properties`.
 */
internal fun readSetting(envName: String, propertyName: String, dataDir: File): String? {
    System.getenv(envName)?.takeIf { it.isNotBlank() }?.let { return it }
    return listOf(File("secrets.properties"), File(dataDir, "secrets.properties"))
        .filter { it.isFile }
        .firstNotNullOfOrNull { file ->
            runCatching { Properties().apply { file.inputStream().use { load(it) } }.getProperty(propertyName) }.getOrNull()?.takeIf { it.isNotBlank() }
        }
}

/** Throw-away graph with an in-memory database (screenshots, demos, tests). */
fun createInMemoryAppGraph(): AppGraph = AppGraph(
    driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY, Properties(), LangDatabase.Schema),
    speech = SilentSpeechService,
    io = Dispatchers.IO,
)
