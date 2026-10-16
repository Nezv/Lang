package app.lang.desktop

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.lang.desktop.ui.DesktopApp
import app.lang.desktop.ui.DesktopViewModels
import app.lang.desktop.ui.ShortcutRouter
import app.lang.designsystem.LangTheme
import app.lang.shared.createAppGraph
import java.awt.Dimension

fun main() {
    val viewModels = DesktopViewModels(createAppGraph())
    val shortcuts = ShortcutRouter()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Lang · 中文",
            icon = rememberVectorPainter(Icons.Rounded.Translate),
            state = rememberWindowState(width = 1280.dp, height = 820.dp, position = WindowPosition(Alignment.Center)),
            onPreviewKeyEvent = shortcuts::dispatch,
        ) {
            window.minimumSize = Dimension(1024, 700)
            LangTheme {
                DesktopApp(viewModels, shortcuts)
            }
        }
    }
}
