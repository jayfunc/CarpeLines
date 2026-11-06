package io.github.jayfunc.carpelines.presentation.components.lyrics.card

import android.graphics.Bitmap
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.jayfunc.carpelines.R

/**
 * The share-card designs ported from BetterLyrics' `LyricsCard*Style.xaml` control templates.
 *
 * Geometry is kept in the same units the original XAML uses (device-independent pixels), so a
 * 450 x 600 template stays a 450 x 600 card; the share page scales it to fit the preview and the
 * exported bitmap is captured at the card's own size instead of the preview's.
 */
enum class LyricsCardGroup(@StringRes val labelRes: Int) {
    CLASSIC(R.string.lyrics_card_group_classic),
    PHYSICAL(R.string.lyrics_card_group_physical),
    TRACES(R.string.lyrics_card_group_traces),
    RETRO(R.string.lyrics_card_group_retro),
    MODERN(R.string.lyrics_card_group_modern),
    ATMOSPHERE(R.string.lyrics_card_group_atmosphere),
    CHINESE(R.string.lyrics_card_group_chinese),
}

/** Font families offered by the original font picker, mapped to the platform generics. */
enum class LyricsCardFont(@StringRes val labelRes: Int, val family: FontFamily) {
    SANS(R.string.lyrics_card_font_sans, FontFamily.SansSerif),
    SERIF(R.string.lyrics_card_font_serif, FontFamily.Serif),
    MONO(R.string.lyrics_card_font_mono, FontFamily.Monospace),
    HAND(R.string.lyrics_card_font_hand, FontFamily.Cursive),
}

enum class LyricsCardStyle(
    val key: String,
    @StringRes val labelRes: Int,
    val group: LyricsCardGroup,
    val width: Dp,
    val height: Dp,
    val defaultFont: LyricsCardFont,
    val swatch: Color,
) {
    MINIMAL(
        key = "LyricsCardMinimalStyle",
        labelRes = R.string.lyrics_card_style_minimal,
        group = LyricsCardGroup.CLASSIC,
        width = 450.dp,
        height = 600.dp,
        defaultFont = LyricsCardFont.SANS,
        swatch = Color(0xFFFFFFFF),
    ),
    SWISS(
        key = "LyricsCardSwissStyle",
        labelRes = R.string.lyrics_card_style_swiss,
        group = LyricsCardGroup.CLASSIC,
        width = 450.dp,
        height = 600.dp,
        defaultFont = LyricsCardFont.SANS,
        swatch = Color(0xFFF0F0F0),
    ),
    MAGAZINE(
        key = "LyricsCardMagazineStyle",
        labelRes = R.string.lyrics_card_style_magazine,
        group = LyricsCardGroup.CLASSIC,
        width = 450.dp,
        height = 600.dp,
        defaultFont = LyricsCardFont.SERIF,
        swatch = Color(0xFF9E9E9E),
    ),
    VINYL(
        key = "LyricsCardVinylStyle",
        labelRes = R.string.lyrics_card_style_vinyl,
        group = LyricsCardGroup.PHYSICAL,
        width = 450.dp,
        height = 600.dp,
        defaultFont = LyricsCardFont.SANS,
        swatch = Color(0xFF121212),
    ),
    CD(
        key = "LyricsCardCDStyle",
        labelRes = R.string.lyrics_card_style_cd,
        group = LyricsCardGroup.PHYSICAL,
        width = 450.dp,
        height = 600.dp,
        defaultFont = LyricsCardFont.SANS,
        swatch = Color(0xFFE5E5E5),
    ),
    POLAROID(
        key = "LyricsCardPolaroidStyle",
        labelRes = R.string.lyrics_card_style_polaroid,
        group = LyricsCardGroup.PHYSICAL,
        width = 400.dp,
        height = 650.dp,
        defaultFont = LyricsCardFont.HAND,
        swatch = Color(0xFFFAFAFA),
    ),
    TICKET(
        key = "LyricsCardTicketStyle",
        labelRes = R.string.lyrics_card_style_ticket,
        group = LyricsCardGroup.PHYSICAL,
        width = 450.dp,
        height = 600.dp,
        defaultFont = LyricsCardFont.MONO,
        swatch = Color(0xFFFFF8F0),
    ),
    RECEIPT(
        key = "LyricsCardReceiptStyle",
        labelRes = R.string.lyrics_card_style_receipt,
        group = LyricsCardGroup.PHYSICAL,
        width = 380.dp,
        height = 600.dp,
        defaultFont = LyricsCardFont.MONO,
        swatch = Color(0xFFFFFDF8),
    ),
    JOURNAL(
        key = "LyricsCardJournalStyle",
        labelRes = R.string.lyrics_card_style_journal,
        group = LyricsCardGroup.TRACES,
        width = 450.dp,
        height = 600.dp,
        defaultFont = LyricsCardFont.HAND,
        swatch = Color(0xFFF4EBD9),
    ),
    STICKY_NOTE(
        key = "LyricsCardStickyNoteStyle",
        labelRes = R.string.lyrics_card_style_sticky_note,
        group = LyricsCardGroup.TRACES,
        width = 450.dp,
        height = 500.dp,
        defaultFont = LyricsCardFont.HAND,
        swatch = Color(0xFFFFF4A3),
    ),
    POD(
        key = "LyricsCardPodStyle",
        labelRes = R.string.lyrics_card_style_pod,
        group = LyricsCardGroup.RETRO,
        width = 450.dp,
        height = 600.dp,
        defaultFont = LyricsCardFont.SANS,
        swatch = Color(0xFFF8F8F8),
    ),
    WINDOWS_PHONE(
        key = "LyricsCardWindowsPhoneStyle",
        labelRes = R.string.lyrics_card_style_windows_phone,
        group = LyricsCardGroup.RETRO,
        width = 450.dp,
        height = 800.dp,
        defaultFont = LyricsCardFont.SANS,
        swatch = Color(0xFF111111),
    ),
    RETRO_QQ(
        key = "LyricsCardRetroQQStyle",
        labelRes = R.string.lyrics_card_style_retro_qq,
        group = LyricsCardGroup.RETRO,
        width = 450.dp,
        height = 600.dp,
        defaultFont = LyricsCardFont.SANS,
        swatch = Color(0xFF0055E4),
    ),
    RETRO_MSN(
        key = "LyricsCardRetroMSNStyle",
        labelRes = R.string.lyrics_card_style_retro_msn,
        group = LyricsCardGroup.RETRO,
        width = 450.dp,
        height = 600.dp,
        defaultFont = LyricsCardFont.SANS,
        swatch = Color(0xFFEBF3FA),
    ),
    QQ(
        key = "LyricsCardQQStyle",
        labelRes = R.string.lyrics_card_style_qq,
        group = LyricsCardGroup.MODERN,
        width = 480.dp,
        height = 600.dp,
        defaultFont = LyricsCardFont.SANS,
        swatch = Color(0xFFF2F2F2),
    ),
    WECHAT(
        key = "LyricsCardWeChatStyle",
        labelRes = R.string.lyrics_card_style_wechat,
        group = LyricsCardGroup.MODERN,
        width = 460.dp,
        height = 600.dp,
        defaultFont = LyricsCardFont.SANS,
        swatch = Color(0xFFF5F5F5),
    ),
    WHATSAPP(
        key = "LyricsCardWhatsAppStyle",
        labelRes = R.string.lyrics_card_style_whatsapp,
        group = LyricsCardGroup.MODERN,
        width = 460.dp,
        height = 700.dp,
        defaultFont = LyricsCardFont.SANS,
        swatch = Color(0xFFEFEAE2),
    ),
    TELEGRAM(
        key = "LyricsCardTelegramStyle",
        labelRes = R.string.lyrics_card_style_telegram,
        group = LyricsCardGroup.MODERN,
        width = 460.dp,
        height = 600.dp,
        defaultFont = LyricsCardFont.SANS,
        swatch = Color(0xFF0E1621),
    ),
    LINE(
        key = "LyricsCardLINEStyle",
        labelRes = R.string.lyrics_card_style_line,
        group = LyricsCardGroup.MODERN,
        width = 460.dp,
        height = 800.dp,
        defaultFont = LyricsCardFont.SANS,
        swatch = Color(0xFF8CABD9),
    ),
    CINEMATIC(
        key = "LyricsCardCinematicStyle",
        labelRes = R.string.lyrics_card_style_cinematic,
        group = LyricsCardGroup.ATMOSPHERE,
        width = 450.dp,
        height = 600.dp,
        defaultFont = LyricsCardFont.SERIF,
        swatch = Color(0xFF000000),
    ),
    CYBERPUNK(
        key = "LyricsCardCyberpunkStyle",
        labelRes = R.string.lyrics_card_style_cyberpunk,
        group = LyricsCardGroup.ATMOSPHERE,
        width = 450.dp,
        height = 600.dp,
        defaultFont = LyricsCardFont.MONO,
        swatch = Color(0xFF050505),
    ),
    INK_WASH(
        key = "LyricsCardInkWashStyle",
        labelRes = R.string.lyrics_card_style_ink_wash,
        group = LyricsCardGroup.CHINESE,
        width = 450.dp,
        height = 600.dp,
        defaultFont = LyricsCardFont.SERIF,
        swatch = Color(0xFFF5F4F0),
    ),
    ANCIENT_BOOK(
        key = "LyricsCardAncientBookStyle",
        labelRes = R.string.lyrics_card_style_ancient_book,
        group = LyricsCardGroup.CHINESE,
        width = 450.dp,
        height = 600.dp,
        defaultFont = LyricsCardFont.SERIF,
        swatch = Color(0xFFEFE8D8),
    ),
    BAMBOO_SLIPS(
        key = "LyricsCardBambooSlipsStyle",
        labelRes = R.string.lyrics_card_style_bamboo_slips,
        group = LyricsCardGroup.CHINESE,
        width = 450.dp,
        height = 600.dp,
        defaultFont = LyricsCardFont.SERIF,
        swatch = Color(0xFFE6D3B3),
    ),
    DUNHUANG(
        key = "LyricsCardDunhuangStyle",
        labelRes = R.string.lyrics_card_style_dunhuang,
        group = LyricsCardGroup.CHINESE,
        width = 450.dp,
        height = 600.dp,
        defaultFont = LyricsCardFont.SERIF,
        swatch = Color(0xFFD6C4A3),
    );

    companion object {
        val default: LyricsCardStyle = MINIMAL

        fun fromKey(key: String?): LyricsCardStyle =
            entries.firstOrNull { it.key == key } ?: default
    }

    /**
     * BetterLyrics lets the card grow past its minimum height as rows are selected. The templates
     * keep their intrinsic size, then uncover one more row's worth of space for each row beyond
     * what the minimum height already shows.
     */
    val baseLineCapacity: Int
        get() = when (this) {
            MINIMAL -> 6
            SWISS -> 6
            MAGAZINE -> 4
            VINYL -> 4
            CD -> 6
            POLAROID -> 3
            TICKET -> 5
            RECEIPT -> 7
            JOURNAL -> 6
            STICKY_NOTE -> 5
            POD -> 9
            WINDOWS_PHONE -> 5
            RETRO_QQ -> 6
            RETRO_MSN -> 8
            QQ -> 4
            WECHAT -> 5
            WHATSAPP -> 4
            TELEGRAM -> 6
            LINE -> 8
            CINEMATIC -> 5
            CYBERPUNK -> 5
            INK_WASH -> 6
            ANCIENT_BOOK -> 0
            BAMBOO_SLIPS -> 0
            DUNHUANG -> 5
        }

    val lineGrowth: Dp
        get() = when (this) {
            MINIMAL -> 60.dp
            SWISS -> 52.dp
            MAGAZINE -> 48.dp
            VINYL -> 56.dp
            CD -> 40.dp
            POLAROID -> 44.dp
            TICKET -> 44.dp
            RECEIPT -> 34.dp
            JOURNAL -> 42.dp
            STICKY_NOTE -> 44.dp
            POD -> 14.dp
            WINDOWS_PHONE -> 58.dp
            RETRO_QQ -> 30.dp
            RETRO_MSN -> 26.dp
            QQ -> 40.dp
            WECHAT -> 40.dp
            WHATSAPP -> 40.dp
            TELEGRAM -> 40.dp
            LINE -> 40.dp
            CINEMATIC -> 46.dp
            CYBERPUNK -> 52.dp
            INK_WASH -> 48.dp
            ANCIENT_BOOK -> 0.dp
            BAMBOO_SLIPS -> 0.dp
            DUNHUANG -> 50.dp
        }

    fun heightFor(lineCount: Int): Dp =
        height + lineGrowth * (lineCount - baseLineCapacity).coerceAtLeast(0)
}

@Immutable
data class LyricsCardLine(val text: String, val secondary: String? = null)

@Immutable
data class LyricsCardData(
    val title: String,
    val artist: String,
    val cover: Bitmap?,
    val accent: Color,
    val lines: List<LyricsCardLine>,
)

@Immutable
data class LyricsCardConfig(val font: LyricsCardFont)
