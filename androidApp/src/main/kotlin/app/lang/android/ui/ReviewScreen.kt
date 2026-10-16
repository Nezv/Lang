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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lang.designsystem.HanziTextStyle
import app.lang.designsystem.SentenceText
import app.lang.designsystem.ratingColors
import app.lang.shared.presentation.ReviewViewModel
import app.lang.shared.srs.Rating

@Composable
fun ReviewScreen(viewModel: ReviewViewModel, onBrowseLessons: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refreshIfIdle() }
    val current = state.current

    when {
        state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        current == null -> ReviewDone(state.reviewedThisSession, onBrowseLessons)
        else -> Column(Modifier.fillMaxSize().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("复习", style = MaterialTheme.typography.titleLarge.merge(HanziTextStyle), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Review", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.weight(1f))
                Text("${state.remaining} left", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(progress = { state.sessionProgress }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            ElevatedCard(
                onClick = { if (!state.revealed) viewModel.reveal() },
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    SentenceText(
                        sentence = current.sentence,
                        showPinyin = state.revealed,
                        colorTones = state.revealed,
                        hanziSize = 40.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    IconButton(onClick = viewModel::speak) {
                        Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = "Play audio")
                    }
                    if (state.revealed) {
                        HorizontalDivider(Modifier.padding(vertical = 16.dp))
                        Text(current.sentence.translation, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                    } else {
                        Spacer(Modifier.height(16.dp))
                        Text("Tap the card to reveal", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            if (state.revealed) {
                GradeButtons(state.intervals, viewModel::grade)
            } else {
                Button(onClick = viewModel::reveal, modifier = Modifier.fillMaxWidth().height(64.dp)) {
                    Text("Show answer", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun GradeButtons(intervals: Map<Rating, String>, onGrade: (Rating) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Rating.entries.forEach { rating ->
            val (container, content) = ratingColors(rating)
            Button(
                onClick = { onGrade(rating) },
                modifier = Modifier.weight(1f).height(64.dp),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(intervals[rating].orEmpty(), style = MaterialTheme.typography.labelSmall)
                    Text(rating.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ReviewDone(reviewed: Int, onBrowseLessons: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Rounded.Celebration, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(16.dp))
        Text("All caught up!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text(
            if (reviewed > 0) "You answered $reviewed card${if (reviewed == 1) "" else "s"} this session." else "No cards are due right now.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        FilledTonalButton(onClick = onBrowseLessons) { Text("Find sentences in Lessons") }
    }
}
