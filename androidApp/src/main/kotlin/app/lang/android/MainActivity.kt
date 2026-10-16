package app.lang.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.lang.android.ui.LangAndroidApp
import app.lang.designsystem.LangTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val graph = (application as LangApplication).graph
        setContent {
            LangTheme {
                LangAndroidApp(graph)
            }
        }
    }
}
