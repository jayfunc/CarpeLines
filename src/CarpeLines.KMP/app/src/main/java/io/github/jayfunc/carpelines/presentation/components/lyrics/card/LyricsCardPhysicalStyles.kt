package io.github.jayfunc.carpelines.presentation.components.lyrics.card

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize

/** `LyricsCardVinylStyle.xaml`: sleeve and record above centred lyrics. */
@Composable
internal fun VinylCard(
    data: LyricsCardData,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier,
) {
    CardSurface(Color(0xFF121212), modifier) {
        Box(Modifier.matchParentSize().background(accentOverlayBrush(data.accent), alpha = 0.3f))
        Box(Modifier.matchParentSize().background(Color(0xCC121212)))
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 40.dp, bottom = 20.dp)
                    .size(width = 320.dp, height = 220.dp),
            ) {
                Box(Modifier.fillMaxSize()) {
                    // Record
                    Box(
                        Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 10.dp)
                            .size(200.dp),
                    ) {
                        Box(Modifier.matchParentSize().clip(CircleShape).background(Color(0xFF050505)))
                        Box(Modifier.align(Alignment.Center).size(190.dp).border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape))
                        Box(Modifier.align(Alignment.Center).size(180.dp).border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape))
                        Box(Modifier.align(Alignment.Center).size(150.dp).border(2.dp, Color.White.copy(alpha = 0.05f), CircleShape))
                        Box(
                            Modifier
                                .align(Alignment.Center)
                                .size(70.dp)
                                .clip(CircleShape),
                        ) {
                            CardCover(data.cover, Modifier.matchParentSize())
                        }
                        Box(Modifier.align(Alignment.Center).size(8.dp).clip(CircleShape).background(Color(0xFF121212)))
                    }
                    // Sleeve
                    Box(
                        Modifier
                            .align(Alignment.CenterStart)
                            .size(210.dp),
                    ) {
                        CardCover(data.cover, Modifier.matchParentSize())
                        Box(
                            Modifier
                                .align(Alignment.TopStart)
                                .fillMaxWidth()
                                .height(100.dp)
                                .background(
                                    Brush.verticalGradient(
                                        0f to Color.White.copy(alpha = 0.25f),
                                        1f to Color.Transparent,
                                    ),
                                )
                                .background(Color.Black.copy(alpha = 0.05f)),
                        )
                    }
                }
            }
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(start = 40.dp, end = 40.dp, bottom = 40.dp),
            ) {
                Column(
                    Modifier
                        .weight(1f)
                        .clipToBounds(),
                    verticalArrangement = Arrangement.Center,
                ) {
                    CardLyrics(
                        lines = data.lines,
                        fontFamily = fontFamily,
                        textSize = 18.sp,
                        textColor = Color(0xFFF0F0F0),
                        secondarySize = 14.sp,
                        secondaryColor = Color(0xFFB0B0B0),
                        spacing = 24.dp,
                        secondarySpacing = 4.dp,
                        weight = FontWeight.SemiBold,
                        lineHeight = 32.sp,
                        secondaryLineHeight = 20.sp,
                        textAlign = TextAlign.Center,
                    )
                }
                Column(Modifier.padding(top = 30.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.fillMaxWidth().height(2.dp).clip(RoundedCornerShape(1.dp)).background(Color.White.copy(alpha = 0.2f))) {
                        Box(Modifier.width(120.dp).height(2.dp).background(Color.White))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CardGlyph(CardGlyphs.Music, 14.sp, Color.White, alpha = 0.8f)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            CardText(
                                text = data.title,
                                size = 14.sp,
                                color = Color.White,
                                weight = FontWeight.Bold,
                                family = fontFamily,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            CardText(
                                text = data.artist,
                                size = 12.sp,
                                color = Color(0xFFAAAAAA),
                                family = fontFamily,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        CardWordmark(
                            color = Color.White,
                            family = fontFamily,
                            alpha = 0.3f,
                        )
                    }
                }
            }
        }
    }
}

/** `LyricsCardCDStyle.xaml`: jewel-case spine, rotated artist on the left edge. */
@Composable
internal fun CDCard(
    data: LyricsCardData,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier,
) {
    CardSurface(Color(0xFFF5F5F5), modifier, RoundedCornerShape(8.dp)) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFD1D1D6), RoundedCornerShape(4.dp)),
        ) {
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.width(140.dp).fillMaxHeight().background(Color(0xFFE5E5E5))) {
                    CardCover(data.cover, Modifier.matchParentSize(), ContentScale.Crop, alpha = 0.5f)
                    Box(
                        Modifier
                            .align(Alignment.CenterStart)
                            .width(40.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
                            .background(Color(0xFFCC0000)),
                    ) {
                        CardText(
                            text = "限定盤",
                            size = 10.sp,
                            color = Color.White,
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp),
                        )
                        CardText(
                            text = "STEREO",
                            size = 10.sp,
                            color = Color.White,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 10.dp)
                                .rotate(-90f),
                        )
                    }
                    Box(
                        Modifier
                            .align(Alignment.CenterEnd)
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(Color.Black.copy(alpha = 0.1f)),
                    )
                }
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(24.dp),
                ) {
                    Column(Modifier.padding(bottom = 20.dp)) {
                        CardText(
                            text = data.title,
                            size = 22.sp,
                            color = Color.Black,
                            weight = FontWeight.Bold,
                            family = fontFamily,
                        )
                        CardText(
                            text = data.artist,
                            size = 14.sp,
                            color = Color(0xFF888888),
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Box(Modifier.fillMaxWidth().padding(top = 12.dp).height(1.dp).background(Color(0xFFEEEEEE)))
                    }
                    Column(
                        Modifier
                            .weight(1f)
                            .clipToBounds(),
                    ) {
                        CardLyrics(
                            lines = data.lines,
                            fontFamily = fontFamily,
                            textSize = 15.sp,
                            textColor = Color(0xFF333333),
                            secondarySize = 13.5.sp,
                            secondaryColor = Color(0xFF777777),
                            spacing = 16.dp,
                            secondarySpacing = 4.dp,
                            lineHeight = 24.sp,
                            secondaryLineHeight = 20.sp,
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(Color.Black))
                        Spacer(Modifier.width(4.dp))
                        CardText(
                            text = "CarpeLines Disc Digital Audio",
                            size = 8.sp,
                            color = Color.Black,
                            alpha = 0.6f,
                        )
                    }
                }
            }
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .width(140.dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                CardText(
                    text = data.artist,
                    size = 16.sp,
                    color = Color.White,
                    weight = FontWeight.Bold,
                    family = fontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(360.dp).rotate(-90f),
                )
            }
        }
    }
}

/** `LyricsCardPolaroidStyle.xaml`: instant photo with handwritten lyric block. */
@Composable
internal fun PolaroidCard(
    data: LyricsCardData,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        Box(Modifier.fillMaxSize().padding(vertical = 25.dp).background(Color.White)) {
            Column(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(360.dp)
                        .padding(start = 20.dp, top = 20.dp, end = 20.dp),
                ) {
                    Box(Modifier.matchParentSize().background(Color(0xFFEEEEEE))) {
                        CardCover(data.cover, Modifier.matchParentSize(), ContentScale.Crop)
                    }
                }
                Column(
                    Modifier
                        .weight(1f)
                        .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
                ) {
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(10.dp)
                            .padding(top = 20.dp)
                            .clipToBounds(),
                    ) {
                        CardLyrics(
                            lines = data.lines,
                            fontFamily = fontFamily,
                            textSize = 20.sp,
                            textColor = Color(0xFF2B2B2B),
                            secondarySize = 16.sp,
                            secondaryColor = Color(0xFF666666),
                            spacing = 16.dp,
                            secondarySpacing = 2.dp,
                            weight = FontWeight.Bold,
                            lineHeight = 28.sp,
                            secondaryLineHeight = 22.sp,
                            textAlign = TextAlign.Center,
                            letterSpacing = 0.2.sp,
                            secondaryItalic = true,
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        CardText(
                            text = "#${io.github.jayfunc.carpelines.utils.BrandName.FULL}",
                            size = 16.sp,
                            color = Color(0xFF333333),
                            weight = FontWeight.Bold,
                            family = fontFamily,
                            alpha = 0.4f,
                            modifier = Modifier.rotate(-2f),
                        )
                        Spacer(Modifier.weight(1f))
                        CardText(
                            text = "${data.title} - ${data.artist}",
                            size = 14.sp,
                            color = Color(0xFF555555),
                            family = fontFamily,
                            alpha = 0.6f,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/** `LyricsCardTicketStyle.xaml`: admit-one stub with a perforation line. */
@Composable
internal fun TicketCard(
    data: LyricsCardData,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier,
) {
    CardSurface(Color(0xFFF5F5F5), modifier, RoundedCornerShape(8.dp)) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(20.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFFF8F0)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .ticketBorder(
                        color = Color(0xFFE6DFD8),
                        radius = 12.dp,
                        top = true,
                        start = true,
                        end = true,
                    )
                    .padding(start = 24.dp, top = 24.dp, end = 24.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(80.dp).clip(RoundedCornerShape(4.dp))) {
                        CardCover(data.cover, Modifier.matchParentSize(), ContentScale.Fit)
                    }
                    Column(Modifier.weight(1f).padding(start = 16.dp)) {
                        CardText(
                            text = "ADMIT ONE",
                            size = 10.sp,
                            color = Color(0xFFFF5555),
                            weight = FontWeight.Bold,
                            family = fontFamily,
                            letterSpacing = 0.2.sp,
                        )
                        CardText(
                            text = data.title,
                            size = 20.sp,
                            color = Color(0xFF333333),
                            weight = FontWeight.Black,
                            family = fontFamily,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        CardText(
                            text = data.artist,
                            size = 14.sp,
                            color = Color(0xFF777777),
                            family = fontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .ticketBorder(
                        color = Color(0xFFE6DFD8),
                        start = true,
                        end = true,
                    )
                    .padding(horizontal = 24.dp),
            ) {
                Column(
                    Modifier
                        .weight(1f)
                        .padding(top = 30.dp, bottom = 30.dp)
                        .clipToBounds(),
                ) {
                    CardLyrics(
                        lines = data.lines,
                        fontFamily = fontFamily,
                        textSize = 18.sp,
                        textColor = Color(0xFF444444),
                        secondarySize = 14.sp,
                        secondaryColor = Color(0xFF888888),
                        spacing = 16.dp,
                        secondarySpacing = 2.dp,
                        lineHeight = 28.sp,
                        secondaryLineHeight = 20.sp,
                    )
                }
            }
            Box(Modifier.fillMaxWidth().height(20.dp)) {
                Canvas(Modifier.fillMaxSize().padding(horizontal = 10.dp)) {
                    drawLine(
                        color = Color(0xFFCCCCCC),
                        start = Offset(0f, size.height / 2f),
                        end = Offset(size.width, size.height / 2f),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 8.dp.toPx())),
                    )
                }
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = (-10).dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF5F5F5))
                        .border(1.dp, Color(0xFFE6DFD8), CircleShape),
                )
                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = 10.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF5F5F5))
                        .border(1.dp, Color(0xFFE6DFD8), CircleShape),
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .ticketBorder(
                        color = Color(0xFFE6DFD8),
                        radius = 12.dp,
                        start = true,
                        end = true,
                        bottom = true,
                    )
                    .padding(start = 24.dp, top = 10.dp, end = 24.dp, bottom = 24.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(Modifier.weight(1f)) {
                    CardText("DATE", 10.sp, Color(0xFF999999), family = fontFamily)
                    CardText(formatCardDateShort(), 14.sp, Color(0xFF333333), family = fontFamily)
                    CardText(
                        "LOCATION",
                        10.sp,
                        Color(0xFF999999),
                        family = fontFamily,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    CardText("CarpeLines", 14.sp, Color(0xFF333333), family = fontFamily)
                }
                CardText(
                    text = "||| |||| | ||||",
                    size = 16.sp,
                    color = Color(0xFF333333),
                    family = fontFamily,
                    alpha = 0.8f,
                )
            }
        }
    }
}

/** `LyricsCardReceiptStyle.xaml`: till roll with prices and a barcode. */
@Composable
internal fun ReceiptCard(
    data: LyricsCardData,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier,
) {
    CardSurface(Color(0xFFFFFDF8), modifier) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxWidth().padding(top = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Box(Modifier.padding(bottom = 10.dp).size(60.dp)) {
                    CardCover(data.cover, Modifier.matchParentSize(), ContentScale.Fit)
                }
                CardText("OFFICIAL RECEIPT", 16.sp, Color(0xFF333333), weight = FontWeight.Bold, family = fontFamily)
                CardText("CARPELINES STORE", 12.sp, Color(0xFF555555), family = fontFamily)
                CardText("--------------------------------------", size = 12.sp, color = Color(0xFF999999), family = fontFamily, maxLines = 1)
                Row(horizontalArrangement = Arrangement.Center) {
                    CardText("DATE: ", 10.sp, Color(0xFF555555), family = fontFamily)
                    CardText(formatCardDateShort(), 10.sp, Color(0xFF555555), family = fontFamily)
                    CardText("TIME: ", 10.sp, Color(0xFF555555), family = fontFamily, modifier = Modifier.padding(start = 20.dp))
                    CardText(formatCardTimeShort(), 10.sp, Color(0xFF555555), family = fontFamily)
                }
                CardText("--------------------------------------", size = 12.sp, color = Color(0xFF999999), family = fontFamily, maxLines = 1)
            }
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp, vertical = 20.dp)
                    .clipToBounds(),
            ) {
                CardText(
                    text = "ITEM: MUSIC MEMORY",
                    size = 10.sp,
                    color = Color(0xFF777777),
                    family = fontFamily,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                Row(Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                    Column(Modifier.weight(1f).padding(end = 10.dp)) {
                        CardText(
                            text = data.title,
                            size = 18.sp,
                            color = Color.Black,
                            weight = FontWeight.Bold,
                            family = fontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        CardText(
                            text = data.artist,
                            size = 12.sp,
                            color = Color(0xFF555555),
                            family = fontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Row {
                        CardText(" x 1", 18.sp, Color.Black, family = fontFamily)
                        CardText("$9.99", 18.sp, Color.Black, family = fontFamily, modifier = Modifier.padding(start = 20.dp))
                    }
                }
                CardLyrics(
                    lines = data.lines,
                    fontFamily = fontFamily,
                    textSize = 14.sp,
                    textColor = Color(0xFF333333),
                    secondarySize = 11.sp,
                    secondaryColor = Color(0xFF888888),
                    spacing = 16.dp,
                    secondarySpacing = 2.dp,
                )
            }
            Column(
                Modifier.fillMaxWidth().padding(start = 30.dp, end = 30.dp, bottom = 30.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CardText(
                    "**************************************",
                    size = 12.sp,
                    color = Color(0xFF999999),
                    family = fontFamily,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    maxLines = 1,
                )
                CardText(
                    "TOTAL: PRICELESS",
                    size = 16.sp,
                    color = Color.Black,
                    weight = FontWeight.Bold,
                    family = fontFamily,
                    modifier = Modifier.align(Alignment.End),
                )
                CardText(
                    "THANK YOU FOR LISTENING!",
                    size = 10.sp,
                    color = Color(0xFF555555),
                    family = fontFamily,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                CardText(
                    "||| |||| | ||| || |||| ||",
                    size = 36.sp,
                    color = Color.Black,
                    weight = FontWeight.Light,
                    family = FontFamily.SansSerif,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    maxLines = 1,
                    alpha = 0.8f,
                )
                CardText(
                    "0 12345 67890 5",
                    size = 10.sp,
                    color = Color.Black,
                    family = fontFamily,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }
}

/**
 * Strokes only the requested sides, mirroring the ticket template's XAML `BorderThickness` values
 * (`1,1,1,0`, `1,0`, `1,0,1,1`). Compose's [border] always outlines all four sides, which added
 * stray horizontal rules above and below the ticket's lyrics area.
 */
private fun Modifier.ticketBorder(
    color: Color,
    width: Dp = 1.dp,
    radius: Dp = 0.dp,
    top: Boolean = false,
    start: Boolean = false,
    end: Boolean = false,
    bottom: Boolean = false,
): Modifier = drawBehind {
    val stroke = width.toPx()
    val half = stroke / 2f
    val r = radius.toPx().coerceAtLeast(half)
    val left = half
    val topEdge = half
    val right = size.width - half
    val bottomEdge = size.height - half

    if (top) drawLine(color, Offset(left + r, topEdge), Offset(right - r, topEdge), stroke)
    if (bottom) drawLine(color, Offset(left + r, bottomEdge), Offset(right - r, bottomEdge), stroke)
    if (start) drawLine(color, Offset(left, topEdge + r), Offset(left, bottomEdge - r), stroke)
    if (end) drawLine(color, Offset(right, topEdge + r), Offset(right, bottomEdge - r), stroke)

    if (radius > 0.dp) {
        if (top && start) {
            drawArc(
                color = color,
                startAngle = 180f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(left, topEdge),
                size = Size(2 * r, 2 * r),
                style = Stroke(stroke),
            )
        }
        if (top && end) {
            drawArc(
                color = color,
                startAngle = 270f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(right - 2 * r, topEdge),
                size = Size(2 * r, 2 * r),
                style = Stroke(stroke),
            )
        }
        if (bottom && start) {
            drawArc(
                color = color,
                startAngle = 90f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(left, bottomEdge - 2 * r),
                size = Size(2 * r, 2 * r),
                style = Stroke(stroke),
            )
        }
        if (bottom && end) {
            drawArc(
                color = color,
                startAngle = 0f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(right - 2 * r, bottomEdge - 2 * r),
                size = Size(2 * r, 2 * r),
                style = Stroke(stroke),
            )
        }
    }
}
