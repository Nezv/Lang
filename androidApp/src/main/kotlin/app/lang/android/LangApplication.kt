package app.lang.android

import android.app.Application
import app.lang.shared.AppGraph
import app.lang.shared.createAppGraph

class LangApplication : Application() {
    val graph: AppGraph by lazy { createAppGraph(this, BuildConfig.ANTHROPIC_API_KEY, BuildConfig.TUTOR_MODEL) }
}
