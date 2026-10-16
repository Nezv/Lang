package app.lang.desktop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lang.designsystem.HanziTextStyle
import app.lang.designsystem.SentenceText
import app.lang.designsystem.ratingColors
import app.lang.shared.presentation.ReviewViewModel
import app.lang.shared.srs.Rating

/** Anki-desktop-style review: Space reveals, 1–4 answer, R replays audio. */
@Composable
fun ReviewPane(viewModel: ReviewViewModel, shortcuts: ShortcutRouter, onBrowseLessons: () -> Unit) {
    val state by viewModel.state.collectAsState()

    PaneShortcuts(shortcuts) { event ->
        if (event.type != KeyEventType.KeyDown || state.current == null) return@PaneShortcuts false
        val rating = when (event.key) {
            Key.One, Key.NumPad1 -> Rating.Again
            Key.Two, Key.NumPad2 -> Rating.Hard
            Key.Three, Key.NumPad3 -> Rating.Good
            Key.Four, Key.NumPad4 -> Rating.Easy
            else -> null
        }
        when {
            event.key == Key.Spacebar || event.key == Key.Enter -> if (state.revealed) viewModel.grade(Rating.Good) else viewModel.reveal()
            rating != null && state.revealed -> viewModel.grade(rating)
            event.key == Key.R -> viewModel.speak()
            else -> return@PaneShortcuts false
        }
        true
    }

    val current = state.current
    Box(Modifier.fillMaxSize().padding(36.dp), contentAlignment = Alignment.TopCenter) {
        when {
            state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            current == null -> ReviewDone(state.reviewedThisSession, onBrowseLessons, Modifier.align(Alignment.Center))
            else -> Column(Modifier.widthIn(max = 820.dp).fillMaxHeight()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("复习", style = MaterialTheme.typography.headlineSmall.merge(HanziTextStyle), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Text("Review", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "${state.reviewedThisSession} done · ${state.remaining} left",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(progress = { state.sessionProgress }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(24.dp))
                OutlinedCard(Modifier.fillMaxWidth().weight(1f)) {
                    Column(
                        Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        SentenceText(current.sentence, showPinyin = state.revealed, colorTones = state.revealed, hanziSize = 56.sp)
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = viewModel::speak) {
                            Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Replay  R")
                        }
                        if (state.revealed) {
                            HorizontalDivider(Modifier.padding(vertical = 20.dp).widthIn(max = 480.dp))
                            Text(current.sentence.translation, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                if (state.revealed) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Rating.entries.forEach { rating ->
                            val (container, content) = ratingColors(rating)
                            Button(
                                onClick = { viewModel.grade(rating) },
                                modifier = Modifier.weight(1f).height(64.dp),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(state.intervals[rating].orEmpty(), style = MaterialTheme.typography.labelMedium)
                                    Text("${rating.name}   ${rating.value}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                } else {
                    Button(onClick = viewModel::reveal, modifier = Modifier.fillMaxWidth().height(64.dp), shape = RoundedCornerShape(14.dp)) {
                        Text("Show answer   Space", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewDone(reviewed: Int, onBrowseLessons: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Rounded.Celebration, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(80.dp))
        Spacer(Modifier.height(16.dp))
        Text("All caught up!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Text(
            if (reviewed > 0) "You answered $reviewed card${if (reviewed == 1) "" else "s"} this session." else "No cards are due right now.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        FilledTonalButton(onClick = onBrowseLessons) { Text("Find sentences in Lessons   Ctrl+2") }
    }
}
