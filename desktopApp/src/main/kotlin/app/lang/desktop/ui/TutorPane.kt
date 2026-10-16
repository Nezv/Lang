package app.lang.desktop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.lang.designsystem.ChatBubble
import app.lang.designsystem.HanziTextStyle
import app.lang.designsystem.TypingBubble
import app.lang.shared.presentation.TutorViewModel
import app.lang.shared.tutor.Phrase
import app.lang.shared.tutor.Scenario
import app.lang.shared.tutor.TutorMode

/** Chat in the middle, scenario picker / display options / reply hints in a side panel. */
@Composable
fun TutorPane(viewModel: TutorViewModel) {
    val state by viewModel.state.collectAsState()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val itemCount = state.messages.size + (if (state.thinking || state.finished) 1 else 0)
    LaunchedEffect(itemCount) { if (itemCount > 0) listState.animateScrollToItem(itemCount - 1) }

    fun send() {
        viewModel.send(input)
        input = ""
    }

    Row(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${state.scenario.titleHanzi}  ${state.scenario.title}",
                        style = MaterialTheme.typography.headlineSmall.merge(HanziTextStyle),
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(state.scenario.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(onClick = { viewModel.start(state.scenarioId) }) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Restart")
                }
            }
            HorizontalDivider()
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.messages) { message ->
                    ChatBubble(
                        message = message,
                        tutorName = state.scenario.tutorName,
                        showPinyin = state.showPinyin,
                        showTranslation = state.showTranslation,
                        onSpeak = { viewModel.speak(message) },
                        maxWidth = 560.dp,
                    )
                }
                if (state.thinking) item { TypingBubble(state.scenario.tutorName) }
                if (state.finished) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                            Text(
                                "Conversation complete! Pick another scenario or press Restart to practise again.",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f).onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.Enter && !event.isShiftPressed) {
                            send()
                            true
                        } else {
                            false
                        }
                    },
                    placeholder = { Text("Type your reply in Chinese and press Enter (Shift+Enter for a new line)") },
                    textStyle = MaterialTheme.typography.titleMedium.merge(HanziTextStyle),
                    shape = RoundedCornerShape(16.dp),
                    maxLines = 4,
                )
                Spacer(Modifier.width(12.dp))
                FilledIconButton(onClick = ::send, enabled = input.isNotBlank()) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send")
                }
            }
        }
        VerticalDivider()
        Column(
            Modifier.width(310.dp).fillMaxHeight().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Scenarios", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            state.scenarios.forEach { scenario ->
                ScenarioCard(scenario, selected = scenario.id == state.scenarioId) { viewModel.start(scenario.id) }
            }
            HorizontalDivider()
            Text("Show in chat", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            SwitchRow("Pinyin", state.showPinyin, viewModel::togglePinyin)
            SwitchRow("English translation", state.showTranslation, viewModel::toggleTranslation)
            HorizontalDivider()
            Text("Useful replies", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (state.suggestions.isEmpty()) {
                Text("Hints appear here during the conversation.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            state.suggestions.forEach { phrase -> PhraseCard(phrase) { viewModel.send(phrase.text) } }
            Spacer(Modifier.weight(1f))
            Text(
                (if (state.mode == TutorMode.Ai) "● " else "○ ") + state.mode.hint,
                style = MaterialTheme.typography.labelSmall,
                color = if (state.mode == TutorMode.Ai) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun ScenarioCard(scenario: Scenario, selected: Boolean, onClick: () -> Unit) {
    val colors = if (selected) {
        CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    } else {
        CardDefaults.outlinedCardColors()
    }
    OutlinedCard(onClick = onClick, colors = colors, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text("${scenario.titleHanzi}  ${scenario.title}", style = MaterialTheme.typography.titleSmall.merge(HanziTextStyle), fontWeight = FontWeight.SemiBold)
            Text(scenario.tutorName, style = MaterialTheme.typography.labelSmall.merge(HanziTextStyle), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PhraseCard(phrase: Phrase, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(phrase.text, style = MaterialTheme.typography.titleMedium.merge(HanziTextStyle))
            Text(phrase.pinyin, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            Text(phrase.translation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}
