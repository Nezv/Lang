package app.lang.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.BookmarkAdded
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lang.designsystem.HanziTextStyle
import app.lang.designsystem.SentenceText
import app.lang.designsystem.WordMeaning
import app.lang.shared.model.Lesson
import app.lang.shared.presentation.LearnState
import app.lang.shared.presentation.LearnViewModel

/** Two-pane lesson browser: lessons and sentences on the left, the sentence player on the right. */
@Composable
fun LearnPane(viewModel: LearnViewModel, shortcuts: ShortcutRouter) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) {
        if (state.openLesson == null) state.lessons.firstOrNull()?.let { viewModel.openLesson(it.id) }
    }
    var selectedToken by remember(state.openLessonId, state.index) { mutableStateOf<Int?>(null) }

    PaneShortcuts(shortcuts) { event ->
        if (event.type != KeyEventType.KeyDown) return@PaneShortcuts false
        val sentence = state.currentSentence
        when (event.key) {
            Key.DirectionRight -> viewModel.next()
            Key.DirectionLeft -> viewModel.previous()
            Key.Spacebar -> sentence?.let(viewModel::speak)
            Key.H -> viewModel.toggleHanzi()
            Key.P -> viewModel.togglePinyin()
            Key.T -> viewModel.toggleTranslation()
            Key.S -> sentence?.let(viewModel::saveForReview)
            else -> return@PaneShortcuts false
        }
        true
    }

    Row(Modifier.fillMaxSize()) {
        LessonList(
            state = state,
            onSelectLesson = viewModel::openLesson,
            onSelectSentence = viewModel::goTo,
            modifier = Modifier.width(300.dp).fillMaxHeight(),
        )
        VerticalDivider()
        Player(
            state = state,
            viewModel = viewModel,
            selectedToken = selectedToken,
            onSelectToken = { selectedToken = if (selectedToken == it) null else it },
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
    }
}

@Composable
private fun LessonList(
    state: LearnState,
    onSelectLesson: (String) -> Unit,
    onSelectSentence: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item {
            Text("Lessons", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(8.dp))
        }
        state.lessons.forEach { lesson ->
            val open = lesson.id == state.openLessonId
            item(key = lesson.id) { LessonHeader(lesson, state.savedCount(lesson), open) { onSelectLesson(lesson.id) } }
            if (open) {
                lesson.sentences.forEachIndexed { index, sentence ->
                    item(key = sentence.id) {
                        val selected = index == state.index
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                                .clickable { onSelectSentence(index) }
                                .pointerHoverIcon(PointerIcon.Hand)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("${index + 1}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline, modifier = Modifier.width(20.dp))
                            Text(
                                sentence.hanzi,
                                style = MaterialTheme.typography.bodyLarge.merge(HanziTextStyle),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            if (sentence.id in state.savedSentenceIds) {
                                Icon(Icons.Rounded.BookmarkAdded, contentDescription = "Saved", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonHeader(lesson: Lesson, saved: Int, open: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (open) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent)
            .clickable(onClick = onClick)
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                lesson.titleHanzi,
                style = MaterialTheme.typography.titleLarge.merge(HanziTextStyle),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(lesson.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("${lesson.level} · $saved/${lesson.sentences.size} saved", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Player(
    state: LearnState,
    viewModel: LearnViewModel,
    selectedToken: Int?,
    onSelectToken: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lesson = state.openLesson
    val sentence = state.currentSentence
    if (lesson == null || sentence == null) {
        Box(modifier, contentAlignment = Alignment.Center) { Text("Pick a lesson on the left") }
        return
    }
    val saved = sentence.id in state.savedSentenceIds

    Column(modifier.padding(36.dp)) {
        Text(lesson.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text("Sentence ${state.index + 1} of ${lesson.sentences.size}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(progress = { (state.index + 1f) / lesson.sentences.size }, modifier = Modifier.fillMaxWidth())

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(max = 900.dp)) {
                SentenceText(
                    sentence = sentence,
                    showHanzi = state.showHanzi,
                    showPinyin = state.showPinyin,
                    hanziSize = 52.sp,
                    selectedToken = selectedToken,
                    onTokenClick = { index, _ -> onSelectToken(index) },
                )
                if (state.showTranslation) {
                    Spacer(Modifier.height(20.dp))
                    Text(sentence.translation, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(28.dp))
                val token = selectedToken?.let { sentence.tokens.getOrNull(it) }
                if (token != null) {
                    WordMeaning(token)
                } else {
                    Text("Click a word to see its meaning", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Wrapping rows keep every control readable when the window is narrow.
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(selected = state.showHanzi, onClick = viewModel::toggleHanzi, label = { Text("汉字  H", style = HanziTextStyle) })
            FilterChip(selected = state.showPinyin, onClick = viewModel::togglePinyin, label = { Text("Pinyin  P") })
            FilterChip(selected = state.showTranslation, onClick = viewModel::toggleTranslation, label = { Text("English  T") })
        }
        Spacer(Modifier.height(12.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = viewModel::previous, enabled = state.index > 0) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = null)
                Text("Previous", maxLines = 1)
            }
            Button(onClick = { viewModel.speak(sentence) }) {
                Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Listen  Space", maxLines = 1)
            }
            FilledTonalButton(onClick = { viewModel.saveForReview(sentence) }, enabled = !saved) {
                Icon(if (saved) Icons.Rounded.BookmarkAdded else Icons.Rounded.BookmarkAdd, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (saved) "Saved" else "Save  S", maxLines = 1)
            }
            OutlinedButton(onClick = viewModel::next, enabled = state.index < lesson.sentences.lastIndex) {
                Text("Next", maxLines = 1)
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
            }
        }
    }
}
