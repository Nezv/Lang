package app.lang.android.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.lang.shared.AppGraph

enum class Tab(val label: String, val icon: ImageVector) {
    Today("Today", Icons.Rounded.Today),
    Learn("Learn", Icons.AutoMirrored.Rounded.MenuBook),
    Review("Review", Icons.Rounded.Style),
    Tutor("Tutor", Icons.Rounded.Forum),
}

/** Phone layout: four bottom tabs; the lesson player opens full-screen on top. */
@Composable
fun LangAndroidApp(graph: AppGraph) {
    val today = viewModel { graph.todayViewModel() }
    val learn = viewModel { graph.learnViewModel() }
    val review = viewModel { graph.reviewViewModel() }
    val tutor = viewModel { graph.tutorViewModel() }

    var tab by rememberSaveable { mutableStateOf(Tab.Today) }
    val todayState by today.state.collectAsStateWithLifecycle()
    val learnState by learn.state.collectAsStateWithLifecycle()
    val speechStatus by graph.speech.status.collectAsStateWithLifecycle()

    // Counts (e.g. the Review badge) change after reviewing or saving, so refresh on every navigation.
    LaunchedEffect(tab, learnState.openLessonId) {
        today.refresh()
        if (tab == Tab.Review) review.refreshIfIdle()
    }

    if (learnState.openLesson != null) {
        BackHandler { learn.closeLesson() }
        LessonPlayerScreen(learn, speechStatus, onBack = learn::closeLesson)
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (item == Tab.Review && todayState.dueNow > 0) Badge { Text("${todayState.dueNow}") }
                                },
                            ) { Icon(item.icon, contentDescription = null) }
                        },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                Tab.Today -> TodayScreen(
                    viewModel = today,
                    onOpenLesson = learn::openLesson,
                    onStartReview = { tab = Tab.Review },
                    onPractice = { tab = Tab.Tutor },
                )
                Tab.Learn -> LearnScreen(learn)
                Tab.Review -> ReviewScreen(review, onBrowseLessons = { tab = Tab.Learn })
                Tab.Tutor -> TutorScreen(tutor)
            }
        }
    }
}
