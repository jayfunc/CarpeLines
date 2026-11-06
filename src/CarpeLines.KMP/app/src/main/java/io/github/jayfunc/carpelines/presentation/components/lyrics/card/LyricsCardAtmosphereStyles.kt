package io.github.jayfunc.carpelines.presentation.components.lyrics.card

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** `LyricsCardCinematicStyle.xaml`: film still with yellow subtitle lines and a credit block. */
@Composable
internal fun CinematicCard(
    data: LyricsCardData,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier,
) {
    CardSurface(Color.Black, modifier) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                CardText(
                    "NOW PLAYING",
                    size = 10.sp,
                    color = Color(0xFF444444),
                    family = fontFamily,
                    letterSpacing = 2.sp,
                )
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                CardCover(
                    data.cover,
                    Modifier.matchParentSize(),
                    ContentScale.Crop,
                    alpha = 0.9f,
                )
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.4f to Color(0x40000000),
                                1f to Color.Black,
                            ),
                        ),
                )
                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                ) {
                    data.lines.forEach { line ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp)
                                .clipToBounds(),
                        ) {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                listOf(
                                    Offset(2f, 2f),
                                    Offset(-1f, -1f),
                                    Offset(1f, -1f),
                                    Offset(-1f, 1f),
                                ).forEach { offset ->
                                    CardText(
                                        text = line.text,
                                        size = 22.sp,
                                        color = Color.Black,
                                        weight = FontWeight.Bold,
                                        family = fontFamily,
                                        textAlign = TextAlign.Center,
                                        alpha = 0.8f,
                                        modifier = Modifier.offset(
                                            x = offset.x.dp,
                                            y = offset.y.dp,
                                        ),
                                    )
                                }
                                CardText(
                                    text = line.text,
                                    size = 22.sp,
                                    color = Color(0xFFFFCB4C),
                                    weight = FontWeight.Bold,
                                    family = fontFamily,
                                    textAlign = TextAlign.Center,
                                )
                            }
                            if (!line.secondary.isNullOrBlank()) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CardText(
                                        text = line.secondary,
                                        size = 15.sp,
                                        color = Color.Black,
                                        weight = FontWeight.SemiBold,
                                        family = fontFamily,
                                        textAlign = TextAlign.Center,
                                        alpha = 0.8f,
                                        modifier = Modifier.offset(x = 1.dp, y = 1.dp),
                                    )
                                    CardText(
                                        text = line.secondary,
                                        size = 15.sp,
                                        color = Color.Black,
                                        weight = FontWeight.SemiBold,
                                        family = fontFamily,
                                        textAlign = TextAlign.Center,
                                        alpha = 0.8f,
                                        modifier = Modifier.offset(x = (-1).dp, y = (-1).dp),
                                    )
                                    CardText(
                                        text = line.secondary,
                                        size = 15.sp,
                                        color = Color(0xFFE0E0E0),
                                        weight = FontWeight.SemiBold,
                                        family = fontFamily,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(top = 10.dp, bottom = 40.dp),
            ) {
                CardText(
                    text = data.title,
                    size = 26.sp,
                    color = Color.White,
                    weight = FontWeight.Black,
                    family = fontFamily,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                )
                CardAnnotatedText(
                    text = buildAnnotatedString {
                        fun muted(value: String) = withStyle(SpanStyle(color = Color(0xFF555555))) { append(value) }
                        fun bright(value: String) = withStyle(
                            SpanStyle(color = Color(0xFFCCCCCC), fontWeight = FontWeight.Bold),
                        ) { append(value) }
                        muted("STARRING")
                        bright(data.artist)
                        muted("  MUSIC BY")
                        withStyle(SpanStyle(color = Color(0xFFCCCCCC))) { append(data.artist) }
                        muted("  PRODUCED BY")
                        withStyle(SpanStyle(color = Color(0xFFCCCCCC))) { append("CARPELINES") }
                        append("\n")
                        muted("DOLBY DIGITAL")
                        withStyle(SpanStyle(color = Color(0xFF333333))) { append("  |  ") }
                        muted("RESTRICTED")
                        withStyle(SpanStyle(color = Color(0xFF333333))) { append("  |  ") }
                        muted("RATED R")
                    },
                    size = 9.sp,
                    family = fontFamily,
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp,
                    modifier = Modifier.fillMaxWidth().padding(start = 40.dp, top = 12.dp, end = 40.dp),
                )
            }
        }
    }
}

/** `LyricsCardCyberpunkStyle.xaml`: neon frame, chromatic title and terminal styling. */
@Composable
internal fun CyberpunkCard(
    data: LyricsCardData,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier,
) {
    CardSurface(Color(0xFF050505), modifier) {
        CardCover(
            data.cover,
            Modifier.matchParentSize(),
            ContentScale.Crop,
            alpha = 0.2f,
        )
        Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.7f)))
        val frame = Brush.linearGradient(
            listOf(Color(0xFF00FF9D), Color(0xFF00E5FF), Color(0xFFFF0055)),
        )
        Box(
            Modifier
                .fillMaxSize()
                .padding(20.dp)
                .drawBehind {
                    drawRect(
                        brush = frame,
                        style = Stroke(width = 2.dp.toPx()),
                    )
                },
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(Color(0xFF111111)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CardText(
                        "CARPELINES",
                        12.sp,
                        Color(0xFF00FF9D),
                        family = fontFamily,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.size(10.dp).background(Color(0xFFFF0055)))
                    Spacer(Modifier.width(4.dp))
                    Box(Modifier.size(10.dp).background(Color(0xFF00E5FF)))
                    Spacer(Modifier.width(10.dp))
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 30.dp, vertical = 20.dp),
                ) {
                    CardText(
                        data.title,
                        36.sp,
                        Color(0xFFFF0055),
                        weight = FontWeight.Black,
                        family = fontFamily,
                        alpha = 0.7f,
                        modifier = Modifier.offset(x = 2.dp, y = 2.dp),
                    )
                    CardText(
                        data.title,
                        36.sp,
                        Color(0xFF00E5FF),
                        weight = FontWeight.Black,
                        family = fontFamily,
                        alpha = 0.7f,
                        modifier = Modifier.offset(x = (-2).dp, y = (-2).dp),
                    )
                    CardText(
                        data.title,
                        36.sp,
                        Color.White,
                        weight = FontWeight.Black,
                        family = fontFamily,
                    )
                }
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 30.dp, vertical = 10.dp)
                        .clipToBounds(),
                ) {
                    CardLyrics(
                        lines = data.lines,
                        fontFamily = fontFamily,
                        textSize = 22.sp,
                        textColor = Color.White,
                        secondarySize = 14.sp,
                        secondaryColor = Color(0xFF00E5FF).copy(alpha = 0.7f),
                        spacing = 24.dp,
                        secondarySpacing = 4.dp,
                        weight = FontWeight.Bold,
                        lineHeight = 28.sp,
                        secondaryLineHeight = 20.sp,
                    )
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(40.dp).border(1.dp, Color.White)) {
                        CardCover(data.cover, Modifier.matchParentSize(), ContentScale.Fit)
                    }
                    Column(Modifier.padding(start = 10.dp).weight(1f)) {
                        CardText(
                            data.artist,
                            14.sp,
                            Color.White,
                            weight = FontWeight.Bold,
                            family = fontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        CardText("// NOW_PLAYING", 10.sp, Color(0xFF888888), family = fontFamily)
                    }
                    CardText("WARNING: LOUD", 10.sp, Color(0xFFFFFF00), family = fontFamily)
                }
            }
        }
    }
}
