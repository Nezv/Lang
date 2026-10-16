package app.lang.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lang.designsystem.HanziTextStyle
import app.lang.shared.AppGraph
import app.lang.shared.speech.SpeechStatus

/** ViewModels live as long as the window (the desktop app has no configuration changes). */
class DesktopViewModels(val graph: AppGraph) {
    val today = graph.todayViewModel()
    val learn = graph.learnViewModel()
    val review = graph.reviewViewModel()
    val tutor = graph.tutorViewModel()
}

/** Window-level keyboard shortcuts: global ones (Ctrl+1..4) plus those of the visible pane. */
class ShortcutRouter {
    internal var global: (KeyEvent) -> Boolean = { false }
    internal var pane: (KeyEvent) -> Boolean = { false }

    fun dispatch(event: KeyEvent): Boolean = global(event) || pane(event)
}

@Composable
fun PaneShortcuts(router: ShortcutRouter, handler: (KeyEvent) -> Boolean) {
    val latest by rememberUpdatedState(handler)
    DisposableEffect(router) {
        router.pane = { latest(it) }
        onDispose { router.pane = { false } }
    }
}

enum class Section(val label: String, val hanzi: String, val icon: ImageVector, val shortcut: String) {
    Today("Today", "今天", Icons.Rounded.Today, "Ctrl+1"),
    Learn("Lessons", "课程", Icons.AutoMirrored.Rounded.MenuBook, "Ctrl+2"),
    Review("Review", "复习", Icons.Rounded.Style, "Ctrl+3"),
    Tutor("AI Tutor", "对话", Icons.Rounded.Forum, "Ctrl+4"),
}

/** Desktop layout: left sidebar + one large content pane per section. */
@Composable
fun DesktopApp(viewModels: DesktopViewModels, shortcuts: ShortcutRouter, initialSection: Section = Section.Today) {
    var section by remember { mutableStateOf(initialSection) }
    val today by viewModels.today.state.collectAsState()
    val speech by viewModels.graph.speech.status.collectAsState()

    DisposableEffect(shortcuts) {
        shortcuts.global = { event ->
            val target = if (event.type == KeyEventType.KeyDown && event.isCtrlPressed) {
                when (event.key) {
                    Key.One -> Section.Today
                    Key.Two -> Section.Learn
                    Key.Three -> Section.Review
                    Key.Four -> Section.Tutor
                    else -> null
                }
            } else {
                null
            }
            if (target != null) section = target
            target != null
        }
        onDispose { shortcuts.global = { false } }
    }

    // Counts (e.g. the Review badge) change after reviewing or saving, so refresh on every navigation.
    LaunchedEffect(section) {
        viewModels.today.refresh()
        if (section == Section.Review) viewModels.review.refreshIfIdle()
    }

    // A Surface (not a plain background) so that text gets the theme's content colour in dark mode too.
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(Modifier.fillMaxSize()) {
            Sidebar(selected = section, onSelect = { section = it }, dueCount = today.dueNow, speech = speech)
            Surface(
                modifier = Modifier.weight(1f).fillMaxHeight().padding(top = 12.dp, end = 12.dp, bottom = 12.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                when (section) {
                    Section.Today -> TodayPane(
                        viewModel = viewModels.today,
                        onOpenLesson = { id ->
                            viewModels.learn.openLesson(id)
                            section = Section.Learn
                        },
                        onStartReview = { section = Section.Review },
                        onPractice = { section = Section.Tutor },
                    )
                    Section.Learn -> LearnPane(viewModels.learn, shortcuts)
                    Section.Review -> ReviewPane(viewModels.review, shortcuts, onBrowseLessons = { section = Section.Learn })
                    Section.Tutor -> TutorPane(viewModels.tutor)
                }
            }
        }
    }
}

@Composable
private fun Sidebar(selected: Section, onSelect: (Section) -> Unit, dueCount: Int, speech: SpeechStatus) {
    Column(Modifier.width(236.dp).fillMaxHeight().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 28.dp)) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text("语", color = MaterialTheme.colorScheme.onPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold, style = HanziTextStyle)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Lang", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Chinese studio", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Section.entries.forEach { item ->
            SidebarItem(
                section = item,
                selected = item == selected,
                badge = if (item == Section.Review && dueCount > 0) "$dueCount" else null,
                onClick = { onSelect(item) },
            )
        }
        Spacer(Modifier.weight(1f))
        SpeechNote(speech)
    }
}

@Composable
private fun SidebarItem(section: Section, selected: Boolean, badge: String?, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) colors.secondaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(section.icon, contentDescription = null, tint = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(section.label, style = MaterialTheme.typography.labelLarge, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
            Text(section.hanzi, style = MaterialTheme.typography.labelSmall.merge(HanziTextStyle), color = colors.onSurfaceVariant)
        }
        if (badge != null) {
            Badge { Text(badge) }
        } else {
            Text(section.shortcut, style = MaterialTheme.typography.labelSmall, color = colors.outline)
        }
    }
}

@Composable
private fun SpeechNote(status: SpeechStatus) {
    val colors = MaterialTheme.colorScheme
    val (text, tint) = when (status) {
        SpeechStatus.Ready -> "Chinese voice ready" to colors.secondary
        SpeechStatus.Initializing -> "Checking Windows voices…" to colors.onSurfaceVariant
        SpeechStatus.MissingChineseVoice ->
            "No Chinese voice installed. Windows Settings › Time & language › Speech › Add voices." to colors.tertiary
        SpeechStatus.Unavailable -> "Speech is unavailable on this system." to colors.onSurfaceVariant
    }
    Row(Modifier.padding(8.dp)) {
        Icon(Icons.Rounded.RecordVoiceOver, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}
