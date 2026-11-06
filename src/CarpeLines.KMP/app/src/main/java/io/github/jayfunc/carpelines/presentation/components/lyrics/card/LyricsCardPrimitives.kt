package io.github.jayfunc.carpelines.presentation.components.lyrics.card

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jayfunc.carpelines.utils.BrandName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Material stand-ins for the Segoe Fluent glyphs the WinUI templates use. */
internal object CardGlyphs {
    val Back = Icons.Rounded.ArrowBack
    val Search = Icons.Rounded.Search
    val Globe = Icons.Rounded.Public
    val Add = Icons.Rounded.Add
    val Attach = Icons.Rounded.AttachFile
    val Send = Icons.Rounded.Send
    val More = Icons.Rounded.MoreVert
    val Settings = Icons.Rounded.Settings
    val Video = Icons.Rounded.Videocam
    val Contact = Icons.Rounded.AccountCircle
    val Person = Icons.Rounded.Person
    val Phone = Icons.Rounded.Phone
    val DoubleCheck = Icons.Rounded.DoneAll
    val Link = Icons.Rounded.Link
    val ChevronDown = Icons.Rounded.KeyboardArrowDown
    val Edit = Icons.Rounded.Edit
    val Menu = Icons.Rounded.Menu
    val Mic = Icons.Rounded.Mic
    val Emoji = Icons.Rounded.EmojiEmotions
    val Image = Icons.Rounded.Image
    val Camera = Icons.Rounded.PhotoCamera
    val Folder = Icons.Rounded.Folder
    val Play = Icons.Rounded.PlayArrow
    val Previous = Icons.Rounded.SkipPrevious
    val Next = Icons.Rounded.SkipNext
    val Message = Icons.Rounded.ChatBubbleOutline
    val Music = Icons.Rounded.MusicNote
}

@Composable
internal fun CardGlyph(
    glyph: ImageVector,
    size: TextUnit,
    color: Color,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
) {
    Icon(
        imageVector = glyph,
        contentDescription = null,
        tint = color.copy(alpha = color.alpha * alpha),
        modifier = modifier.size(size.value.dp),
    )
}

@Composable
internal fun CardText(
    text: String,
    size: TextUnit,
    color: Color,
    modifier: Modifier = Modifier,
    weight: FontWeight = FontWeight.Normal,
    family: FontFamily = FontFamily.SansSerif,
    lineHeight: TextUnit? = null,
    textAlign: TextAlign = TextAlign.Start,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    italic: Boolean = false,
    alpha: Float = 1f,
) {
    if (text.isEmpty()) return
    Text(
        text = text,
        modifier = modifier,
        style = TextStyle(
            color = color.copy(alpha = color.alpha * alpha),
            fontSize = size,
            fontWeight = weight,
            fontFamily = family,
            fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
            lineHeight = lineHeight ?: TextUnit.Unspecified,
            letterSpacing = letterSpacing,
            textAlign = textAlign,
        ),
        maxLines = maxLines,
        overflow = overflow,
    )
}

@Composable
internal fun CardAnnotatedText(
    text: androidx.compose.ui.text.AnnotatedString,
    size: TextUnit,
    family: FontFamily = FontFamily.SansSerif,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
    lineHeight: TextUnit? = null,
) {
    Text(
        text = text,
        modifier = modifier,
        style = TextStyle(
            fontSize = size,
            fontFamily = family,
            textAlign = textAlign,
            lineHeight = lineHeight ?: TextUnit.Unspecified,
        ),
    )
}

@Composable
internal fun CardCover(
    cover: android.graphics.Bitmap?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    alpha: Float = 1f,
) {
    Box(modifier = modifier) {
        if (cover != null) {
            Image(
                bitmap = cover.asImageBitmap(),
                contentDescription = null,
                contentScale = contentScale,
                alpha = alpha,
                modifier = Modifier.matchParentSize(),
            )
        } else {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color(0xFFE2E2E2)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = CardGlyphs.Music,
                    contentDescription = null,
                    tint = Color(0xFF9E9E9E),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/**
 * A run of lyric rows with the shared typography. Rows keep their order; secondary rows are drawn
 * only when the song actually carries a translation or romanization.
 */
@Composable
internal fun CardLyrics(
    lines: List<LyricsCardLine>,
    fontFamily: FontFamily,
    textSize: TextUnit,
    textColor: Color,
    secondarySize: TextUnit,
    secondaryColor: Color,
    modifier: Modifier = Modifier,
    spacing: Dp = 24.dp,
    secondarySpacing: Dp = 4.dp,
    weight: FontWeight = FontWeight.Normal,
    lineHeight: TextUnit? = null,
    secondaryLineHeight: TextUnit? = null,
    textAlign: TextAlign = TextAlign.Start,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    italic: Boolean = false,
    secondaryItalic: Boolean = false,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing)) {
        lines.forEach { line ->
            Column {
                CardText(
                    text = line.text,
                    size = textSize,
                    color = textColor,
                    weight = weight,
                    family = fontFamily,
                    lineHeight = lineHeight,
                    textAlign = textAlign,
                    letterSpacing = letterSpacing,
                    italic = italic,
                )
                val secondary = line.secondary
                if (!secondary.isNullOrBlank()) {
                    Spacer(Modifier.height(secondarySpacing))
                    CardText(
                        text = secondary,
                        size = secondarySize,
                        color = secondaryColor,
                        family = fontFamily,
                        lineHeight = secondaryLineHeight,
                        textAlign = textAlign,
                        italic = secondaryItalic,
                    )
                }
            }
        }
    }
}

/** The gradient BetterLyrics builds from the album accent for the vinyl and Swiss templates. */
internal fun accentOverlayBrush(accent: Color): Brush = Brush.verticalGradient(
    0f to accent.copy(alpha = 180f / 255f),
    0.6f to Color(
        red = accent.red * 0.5f,
        green = accent.green * 0.5f,
        blue = accent.blue * 0.5f,
        alpha = 220f / 255f,
    ),
    1f to Color.Black,
)

/** Root surface shared by the templates: clips to the template corner radius and paints the base. */
@Composable
internal fun CardSurface(
    color: Color,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = androidx.compose.ui.graphics.RectangleShape,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(color),
        content = content,
    )
}

@Composable
internal fun CardWordmark(
    color: Color,
    modifier: Modifier = Modifier,
    size: TextUnit = 12.sp,
    weight: FontWeight = FontWeight.Bold,
    family: FontFamily = FontFamily.SansSerif,
    alpha: Float = 1f,
    prefix: String = "",
    italic: Boolean = false,
) {
    CardText(
        text = prefix + BrandName.FULL,
        size = size,
        color = color,
        modifier = modifier,
        weight = weight,
        family = family,
        alpha = alpha,
        italic = italic,
    )
}

internal fun formatCardDateShort(): String =
    SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date())

internal fun formatCardDateLong(): String =
    SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())

internal fun formatCardTimeShort(): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

internal fun formatCardTimeWithSeconds(): String =
    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

/** XAML FontWeight names used by the templates, mapped to Compose weights. */
internal val XamlBlack = FontWeight.Black
internal val XamlHeavy = FontWeight.Black
internal val XamlBold = FontWeight.Bold
internal val XamlSemiBold = FontWeight.SemiBold
internal val XamlNormal = FontWeight.Normal
internal val XamlLight = FontWeight.Light

@Immutable
internal data class CardPalette(val accent: Color, val onAccent: Color = Color.White)
