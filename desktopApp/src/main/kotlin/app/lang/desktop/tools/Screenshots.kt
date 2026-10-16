package app.lang.desktop.tools

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import app.lang.desktop.ui.DesktopApp
import app.lang.desktop.ui.DesktopViewModels
import app.lang.desktop.ui.Section
import app.lang.desktop.ui.ShortcutRouter
import app.lang.designsystem.LangTheme
import app.lang.shared.createInMemoryAppGraph
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.system.exitProcess
import kotlin.time.Clock

/**
 * Renders each desktop screen off-screen into PNG files, with demo data in a throw-away database.
 * Run with: gradlew :desktopApp:screenshots
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main(args: Array<String>) {
    val outDir = File(args.firstOrNull() ?: "build/screenshots").apply { mkdirs() }
    val graph = createInMemoryAppGraph()
    runBlocking {
        graph.content.lessons().take(2).flatMap { it.sentences }.forEach { graph.cards.addSentence(it.id, Clock.System.now()) }
    }
    val viewModels = DesktopViewModels(graph)
    Thread.sleep(500)

    fun shoot(
        name: String,
        section: Section,
        dark: Boolean = false,
        width: Int = 1280,
        height: Int = 820,
        prepare: () -> Unit = {},
    ) {
        prepare()
        Thread.sleep(1200)
        val scene = ImageComposeScene(width = width, height = height, density = Density(1f)) {
            LangTheme(darkTheme = dark) { DesktopApp(viewModels, ShortcutRouter(), initialSection = section) }
        }
        var frame = 0L
        repeat(8) {
            scene.render(frame)
            frame += 100_000_000L
            Thread.sleep(100)
        }
        val png = scene.render(frame).encodeToData(EncodedImageFormat.PNG) ?: error("PNG encoding failed")
        File(outDir, "$name.png").writeBytes(png.bytes)
        scene.close()
        println("wrote ${File(outDir, "$name.png").absolutePath}")
    }

    shoot("desktop-1-today", Section.Today)
    shoot("desktop-2-lessons", Section.Learn) { viewModels.learn.openLesson("cafe") }
    shoot("desktop-3-review-front", Section.Review) { viewModels.review.refresh() }
    shoot("desktop-4-review-answer", Section.Review) { viewModels.review.reveal() }
    shoot("desktop-5-tutor", Section.Tutor) { viewModels.tutor.send("我想喝咖啡。") }
    shoot("desktop-6-today-dark", Section.Today, dark = true)
    shoot("desktop-7-lessons-smallest-window", Section.Learn, width = 1024, height = 700)
    exitProcess(0)
}
