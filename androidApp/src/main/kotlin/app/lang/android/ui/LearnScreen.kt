package app.lang.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.BookmarkAdded
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lang.designsystem.HanziTextStyle
import app.lang.designsystem.SentenceText
import app.lang.designsystem.WordMeaning
import app.lang.shared.model.Lesson
import app.lang.shared.model.Sentence
import app.lang.shared.presentation.LearnState
import app.lang.shared.presentation.LearnViewModel
import app.lang.shared.speech.SpeechStatus
import kotlinx.coroutines.launch

@Composable
fun LearnScreen(viewModel: LearnViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(Modifier.padding(bottom = 4.dp)) {
                Text("Lessons", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Listen, read, tap words, and save sentences to your review deck.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(state.lessons, key = { it.id }) { lesson ->
            LessonRow(lesson, saved = state.savedCount(lesson)) { viewModel.openLesson(lesson.id) }
        }
    }
}

@Composable
private fun LessonRow(lesson: Lesson, saved: Int, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(72.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        lesson.titleHanzi,
                        style = MaterialTheme.typography.titleMedium.merge(HanziTextStyle),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(lesson.level, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                Text(lesson.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(lesson.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { saved.toFloat() / lesson.sentences.size },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("$saved/${lesson.sentences.size}", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonPlayerScreen(viewModel: LearnViewModel, speechStatus: SpeechStatus, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lesson = state.openLesson ?: return
    val pagerState = rememberPagerState(initialPage = state.index) { lesson.sentences.size }
    val scope = rememberCoroutineScope()
    LaunchedEffect(pagerState.currentPage) { viewModel.goTo(pagerState.currentPage) }

    val page = pagerState.currentPage
    val sentence = lesson.sentences[page]
    val saved = sentence.id in state.savedSentenceIds

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(lesson.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${lesson.titleHanzi} · ${page + 1} / ${lesson.sentences.size}",
                            style = MaterialTheme.typography.labelMedium.merge(HanziTextStyle),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            LinearProgressIndicator(
                progress = { (page + 1f) / lesson.sentences.size },
                modifier = Modifier.fillMaxWidth(),
            )
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp),
                pageSpacing = 12.dp,
            ) { index ->
                SentencePage(lesson.sentences[index], state)
            }
            DisplayToggles(state, viewModel)
            if (speechStatus == SpeechStatus.MissingChineseVoice) {
                Text(
                    "No Chinese voice on this phone: Settings › System › Languages › Text-to-speech.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalIconButton(onClick = { scope.launch { pagerState.animateScrollToPage(page - 1) } }, enabled = page > 0) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous sentence")
                }
                Button(onClick = { viewModel.speak(sentence) }, modifier = Modifier.weight(1f).height(52.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Listen")
                }
                FilledTonalButton(onClick = { viewModel.saveForReview(sentence) }, enabled = !saved, modifier = Modifier.weight(1f).height(52.dp)) {
                    Icon(if (saved) Icons.Rounded.BookmarkAdded else Icons.Rounded.BookmarkAdd, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (saved) "Saved" else "Save")
                }
                FilledTonalIconButton(
                    onClick = { scope.launch { pagerState.animateScrollToPage(page + 1) } },
                    enabled = page < lesson.sentences.lastIndex,
                ) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next sentence")
                }
            }
        }
    }
}

@Composable
private fun SentencePage(sentence: Sentence, state: LearnState) {
    var selected by remember(sentence.id) { mutableStateOf<Int?>(null) }
    Column(
        Modifier.fillMaxSize().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                SentenceText(
                    sentence = sentence,
                    showHanzi = state.showHanzi,
                    showPinyin = state.showPinyin,
                    hanziSize = 36.sp,
                    selectedToken = selected,
                    onTokenClick = { index, _ -> selected = if (selected == index) null else index },
                )
                if (state.showTranslation) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        sentence.translation,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        val token = selected?.let { sentence.tokens.getOrNull(it) }
        if (token != null) {
            WordMeaning(token)
        } else {
            Text("Tap a word to see its meaning", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DisplayToggles(state: LearnState, viewModel: LearnViewModel) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        FilterChip(selected = state.showHanzi, onClick = viewModel::toggleHanzi, label = { Text("汉字", style = HanziTextStyle) })
        FilterChip(selected = state.showPinyin, onClick = viewModel::togglePinyin, label = { Text("Pinyin") })
        FilterChip(selected = state.showTranslation, onClick = viewModel::toggleTranslation, label = { Text("English") })
    }
}
