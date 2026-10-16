package app.lang.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.lang.shared.srs.Rating
import app.lang.shared.tutor.ChatMessage
import app.lang.shared.tutor.Role

/** Container/content colours for the four answer buttons (same meaning on every platform). */
@Composable
fun ratingColors(rating: Rating): Pair<Color, Color> {
    val c = MaterialTheme.colorScheme
    return when (rating) {
        Rating.Again -> c.errorContainer to c.onErrorContainer
        Rating.Hard -> c.surfaceContainerHighest to c.onSurface
        Rating.Good -> c.primary to c.onPrimary
        Rating.Easy -> c.secondary to c.onSecondary
    }
}

/** One message of a tutor conversation; tutor on the left, learner on the right. */
@Composable
fun ChatBubble(
    message: ChatMessage,
    tutorName: String,
    showPinyin: Boolean,
    showTranslation: Boolean,
    onSpeak: (() -> Unit)?,
    modifier: Modifier = Modifier,
    maxWidth: Dp = 320.dp,
) {
    val fromTutor = message.role == Role.Tutor
    Row(modifier.fillMaxWidth(), horizontalArrangement = if (fromTutor) Arrangement.Start else Arrangement.End) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (fromTutor) 4.dp else 18.dp,
                bottomEnd = if (fromTutor) 18.dp else 4.dp,
            ),
            color = if (fromTutor) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.widthIn(max = maxWidth),
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (fromTutor) {
                    Text(tutorName, style = MaterialTheme.typography.labelSmall.merge(HanziTextStyle), color = MaterialTheme.colorScheme.primary)
                }
                Text(message.text, style = MaterialTheme.typography.titleLarge.merge(HanziTextStyle))
                if (showPinyin && message.pinyin.isNotEmpty()) {
                    Text(message.pinyin, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (showTranslation && message.translation.isNotEmpty()) {
                    Text(
                        message.translation,
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (fromTutor && message.correction.isNotEmpty()) {
                    Text(
                        "💡 ${message.correction}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                if (fromTutor && onSpeak != null) {
                    IconButton(onClick = onSpeak, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = "Play", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun TypingBubble(tutorName: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier,
    ) {
        Text(
            "$tutorName is typing…",
            style = MaterialTheme.typography.bodyMedium.merge(HanziTextStyle),
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}
