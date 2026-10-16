package app.lang.desktop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.lang.designsystem.HanziTextStyle
import app.lang.shared.presentation.LessonProgress
import app.lang.shared.presentation.TodayViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TodayPane(
    viewModel: TodayViewModel,
    onOpenLesson: (String) -> Unit,
    onStartReview: () -> Unit,
    onPractice: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(36.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "你好！",
                style = MaterialTheme.typography.displayMedium.merge(HanziTextStyle),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(16.dp))
            Text(
                "Welcome back. Here is your Chinese for today.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Metric("Due now", state.dueNow, "cards waiting", Modifier.weight(1f))
            Metric("Reviewed today", state.reviewedToday, "answers given", Modifier.weight(1f))
            Metric("Deck size", state.totalCards, "${state.newCards} not studied yet", Modifier.weight(1f))
        }

        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ActionPanel(
                title = "复习  Review",
                body = when {
                    state.dueNow > 0 -> "${state.dueNow} card${if (state.dueNow == 1) "" else "s"} ready. Use keys 1–4 to answer, like Anki."
                    state.totalCards > 0 -> "Nothing due right now. Your cards come back when it's time to review them."
                    else -> "Your deck is empty. Save sentences from a lesson to start it."
                },
                button = "Start review   Ctrl+3",
                container = MaterialTheme.colorScheme.primaryContainer,
                onClick = onStartReview,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            ActionPanel(
                title = "对话  AI tutor",
                body = "Role-play real situations: order a drink at a café or meet your new teacher.",
                button = "Start talking   Ctrl+4",
                container = MaterialTheme.colorScheme.tertiaryContainer,
                onClick = onPractice,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }

        Text("Your lessons", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            state.lessons.forEach { progress ->
                LessonTile(progress, onClick = { onOpenLesson(progress.lesson.id) })
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: Int, caption: String, modifier: Modifier = Modifier) {
    OutlinedCard(modifier) {
        Column(Modifier.padding(20.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$value", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold)
            Text(caption, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ActionPanel(
    title: String,
    body: String,
    button: String,
    container: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = container)) {
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall.merge(HanziTextStyle), fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(body, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f, fill = false))
            Spacer(Modifier.height(16.dp))
            Button(onClick = onClick) { Text(button) }
        }
    }
}

@Composable
private fun LessonTile(progress: LessonProgress, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = Modifier.width(300.dp)) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    progress.lesson.titleHanzi,
                    style = MaterialTheme.typography.headlineSmall.merge(HanziTextStyle),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.weight(1f))
                Text(progress.lesson.level, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(progress.lesson.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                progress.lesson.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                minLines = 2,
                maxLines = 2,
            )
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(progress = { progress.fraction }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(4.dp))
            Text("${progress.saved}/${progress.total} saved to review", style = MaterialTheme.typography.labelSmall)
        }
    }
}
