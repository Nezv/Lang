package app.lang.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lang.designsystem.ChatBubble
import app.lang.designsystem.HanziTextStyle
import app.lang.designsystem.TypingBubble
import app.lang.shared.presentation.TutorViewModel
import app.lang.shared.tutor.TutorMode

@Composable
fun TutorScreen(viewModel: TutorViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val itemCount = state.messages.size + (if (state.thinking || state.finished) 1 else 0)
    LaunchedEffect(itemCount) { if (itemCount > 0) listState.animateScrollToItem(itemCount - 1) }

    Column(Modifier.fillMaxSize().imePadding()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.scenarios, key = { it.id }) { scenario ->
                FilterChip(
                    selected = scenario.id == state.scenarioId,
                    onClick = { viewModel.start(scenario.id) },
                    label = { Text("${scenario.titleHanzi} · ${scenario.title}", style = HanziTextStyle) },
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                state.scenario.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            FilterChip(selected = state.showPinyin, onClick = viewModel::togglePinyin, label = { Text("Pīnyīn") })
            Spacer(Modifier.width(6.dp))
            FilterChip(selected = state.showTranslation, onClick = viewModel::toggleTranslation, label = { Text("EN") })
        }
        Text(
            (if (state.mode == TutorMode.Ai) "● " else "○ ") + state.mode.hint,
            style = MaterialTheme.typography.labelSmall,
            color = if (state.mode == TutorMode.Ai) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(state.messages) { _, message ->
                ChatBubble(
                    message = message,
                    tutorName = state.scenario.tutorName,
                    showPinyin = state.showPinyin,
                    showTranslation = state.showTranslation,
                    onSpeak = { viewModel.speak(message) },
                )
            }
            if (state.thinking) item { TypingBubble(state.scenario.tutorName) }
            if (state.finished) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Conversation complete!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            FilledTonalButton(onClick = { viewModel.start(state.scenarioId) }) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Practise again")
                            }
                        }
                    }
                }
            }
        }
        if (state.suggestions.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.suggestions) { phrase ->
                    SuggestionChip(
                        onClick = { viewModel.send(phrase.text) },
                        label = { Text(phrase.text, style = MaterialTheme.typography.bodyLarge.merge(HanziTextStyle)) },
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("用中文回答… Reply in Chinese", style = HanziTextStyle) },
                shape = RoundedCornerShape(28.dp),
                maxLines = 3,
            )
            Spacer(Modifier.width(8.dp))
            if (input.isBlank()) {
                FilledTonalIconButton(onClick = {}, enabled = false) {
                    Icon(Icons.Rounded.Mic, contentDescription = "Voice input (coming with the AI tutor)")
                }
            } else {
                FilledIconButton(onClick = { viewModel.send(input); input = "" }) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send")
                }
            }
        }
    }
}
