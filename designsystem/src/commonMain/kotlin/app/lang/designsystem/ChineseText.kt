package app.lang.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lang.shared.model.Pinyin
import app.lang.shared.model.Sentence
import app.lang.shared.model.Token

/** Use for any Chinese text: the locale makes fonts pick Simplified-Chinese glyph shapes. */
val HanziTextStyle = TextStyle(localeList = LocaleList("zh-CN"))

/**
 * A sentence with pinyin above each character (ruby text), optional tone colours and tappable words.
 * Hiding both hanzi and pinyin turns it into a listening exercise (blanks).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SentenceText(
    sentence: Sentence,
    modifier: Modifier = Modifier,
    showHanzi: Boolean = true,
    showPinyin: Boolean = true,
    colorTones: Boolean = true,
    hanziSize: TextUnit = 32.sp,
    selectedToken: Int? = null,
    onTokenClick: ((index: Int, token: Token) -> Unit)? = null,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        sentence.tokens.forEachIndexed { index, token ->
            TokenText(
                token = token,
                showHanzi = showHanzi,
                showPinyin = showPinyin,
                colorTones = colorTones,
                hanziSize = hanziSize,
                selected = index == selectedToken,
                onClick = if (onTokenClick != null && !token.isPunctuation) ({ onTokenClick(index, token) }) else null,
            )
        }
    }
}

@Composable
private fun TokenText(
    token: Token,
    showHanzi: Boolean,
    showPinyin: Boolean,
    colorTones: Boolean,
    hanziSize: TextUnit,
    selected: Boolean,
    onClick: (() -> Unit)?,
) {
    var modifier: Modifier = Modifier.clip(RoundedCornerShape(6.dp))
    if (selected) modifier = modifier.background(MaterialTheme.colorScheme.primaryContainer)
    if (onClick != null) modifier = modifier.clickable(onClick = onClick)

    Row(modifier.padding(horizontal = 1.dp, vertical = 2.dp)) {
        val syllables = token.syllables
        val characters = token.characters
        if (!token.isPunctuation && syllables.size == characters.size) {
            characters.forEachIndexed { i, character ->
                Glyph(character, syllables[i], showHanzi, showPinyin, colorTones, hanziSize)
            }
        } else {
            Glyph(token.hanzi, token.pinyinWord, showHanzi, showPinyin, colorTones, hanziSize)
        }
    }
}

@Composable
private fun Glyph(
    hanzi: String,
    pinyin: String,
    showHanzi: Boolean,
    showPinyin: Boolean,
    colorTones: Boolean,
    hanziSize: TextUnit,
) {
    val color = if (colorTones && pinyin.isNotEmpty()) {
        LocalToneColors.current.forTone(Pinyin.tone(pinyin))
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val pinyinSize = if (showHanzi) hanziSize * 0.42f else hanziSize * 0.7f
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 1.dp)) {
        if (showPinyin) {
            Text(text = pinyin.ifEmpty { " " }, fontSize = pinyinSize, color = color, maxLines = 1)
        }
        if (showHanzi) {
            Text(text = hanzi, fontSize = hanziSize, color = color, style = HanziTextStyle, maxLines = 1)
        }
        if (!showHanzi && !showPinyin) {
            val blank = if (pinyin.isEmpty()) hanzi else "＿".repeat(hanzi.length)
            Text(text = blank, fontSize = hanziSize, color = MaterialTheme.colorScheme.outline, style = HanziTextStyle)
        }
    }
}

/** Dictionary-style card for a tapped word. */
@Composable
fun WordMeaning(token: Token, modifier: Modifier = Modifier) {
    OutlinedCard(modifier) {
        Row(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(token.hanzi, style = MaterialTheme.typography.headlineSmall.merge(HanziTextStyle))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(token.pinyinWord, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Text(token.gloss, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
