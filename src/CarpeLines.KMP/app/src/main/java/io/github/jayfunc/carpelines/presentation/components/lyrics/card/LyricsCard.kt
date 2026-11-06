package io.github.jayfunc.carpelines.presentation.components.lyrics.card

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Renders one BetterLyrics card template at its native 450-ish width. The caller owns scaling and
 * capture; the card itself always lays out at its template size so the exported PNG keeps the
 * original proportions.
 */
@Composable
fun LyricsCard(
    style: LyricsCardStyle,
    data: LyricsCardData,
    config: LyricsCardConfig,
    modifier: Modifier = Modifier,
) {
    val cardModifier = modifier.requiredSize(style.width, style.heightFor(data.lines.size))
    val font = config.font.family
    when (style) {
        LyricsCardStyle.MINIMAL -> MinimalCard(data, font, cardModifier)
        LyricsCardStyle.SWISS -> SwissCard(data, font, cardModifier)
        LyricsCardStyle.MAGAZINE -> MagazineCard(data, font, cardModifier)
        LyricsCardStyle.VINYL -> VinylCard(data, font, cardModifier)
        LyricsCardStyle.CD -> CDCard(data, font, cardModifier)
        LyricsCardStyle.POLAROID -> PolaroidCard(data, font, cardModifier)
        LyricsCardStyle.TICKET -> TicketCard(data, font, cardModifier)
        LyricsCardStyle.RECEIPT -> ReceiptCard(data, font, cardModifier)
        LyricsCardStyle.JOURNAL -> JournalCard(data, font, cardModifier)
        LyricsCardStyle.STICKY_NOTE -> StickyNoteCard(data, font, cardModifier)
        LyricsCardStyle.POD -> PodCard(data, font, cardModifier)
        LyricsCardStyle.WINDOWS_PHONE -> WindowsPhoneCard(data, font, cardModifier)
        LyricsCardStyle.RETRO_QQ -> RetroQQCard(data, font, cardModifier)
        LyricsCardStyle.RETRO_MSN -> RetroMSNCard(data, font, cardModifier)
        LyricsCardStyle.QQ -> QQCard(data, font, cardModifier)
        LyricsCardStyle.WECHAT -> WeChatCard(data, font, cardModifier)
        LyricsCardStyle.WHATSAPP -> WhatsAppCard(data, font, cardModifier)
        LyricsCardStyle.TELEGRAM -> TelegramCard(data, font, cardModifier)
        LyricsCardStyle.LINE -> LINECard(data, font, cardModifier)
        LyricsCardStyle.CINEMATIC -> CinematicCard(data, font, cardModifier)
        LyricsCardStyle.CYBERPUNK -> CyberpunkCard(data, font, cardModifier)
        LyricsCardStyle.INK_WASH -> InkWashCard(data, font, cardModifier)
        LyricsCardStyle.ANCIENT_BOOK -> AncientBookCard(data, font, cardModifier)
        LyricsCardStyle.BAMBOO_SLIPS -> BambooSlipsCard(data, font, cardModifier)
        LyricsCardStyle.DUNHUANG -> DunhuangCard(data, font, cardModifier)
    }
}

/**
 * Measures the card at its native size, then reports the scaled size to the caller. [fit] shrinks
 * the card to fit both axes (style thumbnails); otherwise it fills the available width and reports
 * its full scaled height so the caller can scroll a tall selection.
 */
@Composable
internal fun ScaledLyricsCard(
    style: LyricsCardStyle,
    data: LyricsCardData,
    config: LyricsCardConfig,
    fit: Boolean,
    modifier: Modifier = Modifier,
    cardModifier: Modifier = Modifier,
) {
    Layout(
        content = { LyricsCard(style, data, config, cardModifier) },
        modifier = modifier,
    ) { measurables, constraints ->
        val card = measurables.first().measure(Constraints())
        val maxWidth = constraints.maxWidth.takeIf { it != Constraints.Infinity } ?: card.width
        val maxHeight = constraints.maxHeight.takeIf { it != Constraints.Infinity } ?: card.height
        val scale = if (fit) {
            min(maxWidth / card.width.toFloat(), maxHeight / card.height.toFloat())
        } else {
            maxWidth / card.width.toFloat()
        }
        val width = (card.width * scale).roundToInt()
        val height = (card.height * scale).roundToInt()
        layout(width, height) {
            card.placeWithLayer(0, 0) {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0f)
            }
        }
    }
}
