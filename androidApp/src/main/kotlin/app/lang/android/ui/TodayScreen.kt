package app.lang.android.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.LocalLibrary
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lang.designsystem.HanziTextStyle
import app.lang.shared.presentation.LessonProgress
import app.lang.shared.presentation.TodayViewModel

@Composable
fun TodayScreen(
    viewModel: TodayViewModel,
    onOpenLesson: (String) -> Unit,
    onStartReview: () -> Unit,
    onPractice: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column {
                Text(
                    "你好！",
                    style = MaterialTheme.typography.displaySmall.merge(HanziTextStyle),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "Nǐ hǎo! Ready for today's Chinese?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Due now", state.dueNow, Icons.Rounded.Style, Modifier.weight(1f))
                StatTile("Done today", state.reviewedToday, Icons.Rounded.Done, Modifier.weight(1f))
                StatTile("In deck", state.totalCards, Icons.Rounded.LocalLibrary, Modifier.weight(1f))
            }
        }
        item { ReviewCallout(state.dueNow, state.totalCards, onStartReview) }
        state.nextLesson?.let { next ->
            item { ContinueLessonCard(next) { onOpenLesson(next.lesson.id) } }
        }
        item { TutorCallout(onPractice) }
    }
}

@Composable
private fun StatTile(label: String, value: Int, icon: ImageVector, modifier: Modifier = Modifier) {
    ElevatedCard(modifier) {
        Column(Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text("$value", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ReviewCallout(due: Int, total: Int, onStartReview: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text("复习 · Review", style = MaterialTheme.typography.titleLarge.merge(HanziTextStyle), fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                when {
                    due > 0 -> "$due card${if (due == 1) "" else "s"} ready. A few minutes now keeps them fresh."
                    total > 0 -> "Nothing due right now. Your cards come back when it's time to review them."
                    else -> "Your deck is empty. Save sentences from a lesson to start it."
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onStartReview) { Text(if (due > 0) "Start review" else "Open review") }
        }
    }
}

@Composable
private fun ContinueLessonCard(progress: LessonProgress, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                progress.lesson.titleHanzi,
                style = MaterialTheme.typography.headlineSmall.merge(HanziTextStyle),
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("Continue learning", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(progress.lesson.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { progress.fraction }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                Text(
                    "${progress.saved} of ${progress.total} sentences saved",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
private fun TutorCallout(onPractice: () -> Unit) {
    ElevatedCard(onClick = onPractice) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.RecordVoiceOver,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(40.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("对话 · Speak with your tutor", style = MaterialTheme.typography.titleMedium.merge(HanziTextStyle), fontWeight = FontWeight.SemiBold)
                Text(
                    "Role-play real situations: order a drink, meet your teacher.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
