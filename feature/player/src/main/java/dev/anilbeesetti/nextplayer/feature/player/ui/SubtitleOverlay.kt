package dev.anilbeesetti.nextplayer.feature.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.anilbeesetti.nextplayer.core.subtitle.model.SubtitleDisplayMode
import dev.anilbeesetti.nextplayer.core.subtitle.model.SubtitleSegment

@Composable
fun SubtitleOverlay(
    segments: List<SubtitleSegment>,
    provisionalText: String,
    displayMode: SubtitleDisplayMode,
    modifier: Modifier = Modifier,
) {
    val bgColor = Color.Black.copy(alpha = 0.6f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Render final segments
        val visibleSegments = segments.takeLast(3) // Show last 3 segments
        for (segment in visibleSegments) {
            when (displayMode) {
                SubtitleDisplayMode.TRANSLATION_ONLY -> {
                    segment.translationText?.let { translatedText ->
                        SubtitleText(text = translatedText, bgColor = bgColor)
                    }
                }
                SubtitleDisplayMode.ORIGINAL_ONLY -> {
                    SubtitleText(text = segment.originalText, bgColor = bgColor)
                }
                SubtitleDisplayMode.BILINGUAL -> {
                    // Original line
                    SubtitleText(
                        text = segment.originalText,
                        bgColor = bgColor,
                        fontSize = 14,
                        fontWeight = FontWeight.Normal,
                    )
                    // Translation line (or placeholder)
                    val translationDisplay = segment.translationText ?: "..."
                    SubtitleText(
                        text = translationDisplay,
                        bgColor = bgColor,
                        fontSize = 16,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        // Render provisional text
        AnimatedVisibility(
            visible = provisionalText.isNotEmpty() && displayMode != SubtitleDisplayMode.TRANSLATION_ONLY,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Text(
                text = provisionalText,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .background(
                        color = Color.Black.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(4.dp),
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun SubtitleText(
    text: String,
    bgColor: Color,
    modifier: Modifier = Modifier,
    fontSize: Int = 16,
    fontWeight: FontWeight = FontWeight.Medium,
) {
    Text(
        text = text,
        color = Color.White,
        fontSize = fontSize.sp,
        fontWeight = fontWeight,
        textAlign = TextAlign.Center,
        modifier = modifier
            .background(
                color = bgColor,
                shape = RoundedCornerShape(4.dp),
            )
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}
