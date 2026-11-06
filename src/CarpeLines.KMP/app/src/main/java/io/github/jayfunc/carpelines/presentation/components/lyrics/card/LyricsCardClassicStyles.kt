package io.github.jayfunc.carpelines.presentation.components.lyrics.card

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** `LyricsCardMinimalStyle.xaml`: white sheet, double frame, bold lyrics, cover footer. */
@Composable
internal fun MinimalCard(
    data: LyricsCardData,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier,
) {
    CardSurface(Color.White, modifier) {
        Box(
            Modifier
                .matchParentSize()
                .padding(20.dp)
                .border(4.dp, Color.Black),
        )
        Column(
            Modifier
                .fillMaxSize()
                .padding(40.dp),
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .clipToBounds(),
            ) {
                CardLyrics(
                    lines = data.lines,
                    fontFamily = fontFamily,
                    textSize = 26.sp,
                    textColor = Color.Black,
                    secondarySize = 18.sp,
                    secondaryColor = Color(0xFF666666),
                    spacing = 24.dp,
                    secondarySpacing = 4.dp,
                    weight = FontWeight.Bold,
                    lineHeight = 36.sp,
                    secondaryLineHeight = 26.sp,
                )
            }
            Column(Modifier.fillMaxWidth().padding(top = 20.dp)) {
                Box(Modifier.fillMaxWidth().height(2.dp).background(Color.Black))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(60.dp),
                    ) {
                        CardCover(data.cover, Modifier.matchParentSize(), ContentScale.Fit)
                    }
                    Box(Modifier.weight(1f).padding(start = 12.dp)) {
                        Column(Modifier.align(Alignment.CenterStart)) {
                            CardText(
                                text = data.title,
                                size = 18.sp,
                                color = Color.Black,
                                weight = FontWeight.Black,
                                family = fontFamily,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            CardText(
                                text = data.artist,
                                size = 14.sp,
                                color = Color(0xFF555555),
                                family = fontFamily,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        CardWordmark(
                            color = Color.Black,
                            modifier = Modifier.align(Alignment.BottomEnd),
                            family = fontFamily,
                            alpha = 0.6f,
                        )
                    }
                }
            }
        }
    }
}

/** `LyricsCardSwissStyle.xaml`: exhibition poster, oversized number, black footer bar. */
@Composable
internal fun SwissCard(
    data: LyricsCardData,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier,
) {
    CardSurface(Color(0xFFF0F0F0), modifier) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(20.dp)
                .border(4.dp, Color.Black),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(20.dp),
            ) {
                Column {
                    CardText(
                        text = data.title,
                        size = 48.sp,
                        color = Color.Black,
                        weight = FontWeight.Black,
                        family = fontFamily,
                        lineHeight = 44.sp,
                    )
                    CardText(
                        text = data.artist,
                        size = 20.sp,
                        color = Color.Black,
                        weight = FontWeight.Bold,
                        family = fontFamily,
                        modifier = Modifier.padding(top = 10.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                CardText(
                    text = "01",
                    size = 80.sp,
                    color = Color(0xFFEEEEEE),
                    weight = FontWeight.Bold,
                    family = fontFamily,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 10.dp, y = (-20).dp),
                )
            }
            Box(Modifier.fillMaxWidth().height(4.dp).background(Color.Black))
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(accentOverlayBrush(data.accent)),
            ) {
                Box(Modifier.matchParentSize().background(Color.White.copy(alpha = 0.8f)))
                CardLyrics(
                    lines = data.lines,
                    fontFamily = fontFamily,
                    textSize = 24.sp,
                    textColor = Color.Black,
                    secondarySize = 16.sp,
                    secondaryColor = Color(0xFF444444),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 30.dp)
                        .clipToBounds(),
                    spacing = 24.dp,
                    secondarySpacing = 4.dp,
                    weight = FontWeight.Black,
                    lineHeight = 28.sp,
                    secondaryLineHeight = 22.sp,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(horizontal = 20.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                ) {
                    CardCover(data.cover, Modifier.matchParentSize())
                }
                Spacer(Modifier.width(10.dp))
                CardText(
                    text = "NOW PLAYING",
                    size = 14.sp,
                    color = Color.White,
                    weight = FontWeight.Bold,
                    family = fontFamily,
                )
                Spacer(Modifier.weight(1f))
                CardWordmark(
                    color = Color.White,
                    family = fontFamily,
                    alpha = 0.5f,
                )
            }
        }
    }
}

/** `LyricsCardMagazineStyle.xaml`: full-bleed cover, editorial type, issue line. */
@Composable
internal fun MagazineCard(
    data: LyricsCardData,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier,
) {
    CardSurface(Color.White, modifier) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().height(300.dp)) {
                CardCover(
                    cover = data.cover,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                )
                CardText(
                    text = "LYRICS",
                    size = 80.sp,
                    color = Color.White,
                    weight = FontWeight.Bold,
                    family = fontFamily,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(y = 15.dp),
                    alpha = 0.2f,
                )
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 30.dp, top = 10.dp, end = 30.dp, bottom = 30.dp),
            ) {
                Column(
                    Modifier
                        .weight(1f)
                        .clipToBounds(),
                ) {
                    Column(Modifier.padding(top = 30.dp)) {
                        CardText(
                            text = data.title,
                            size = 32.sp,
                            color = Color.Black,
                            weight = FontWeight.Black,
                            family = fontFamily,
                            lineHeight = 36.sp,
                        )
                        CardText(
                            text = data.artist,
                            size = 16.sp,
                            color = Color(0xFF666666),
                            family = fontFamily,
                            italic = true,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        Box(
                            Modifier
                                .padding(top = 15.dp, bottom = 25.dp)
                                .size(width = 40.dp, height = 4.dp)
                                .background(Color.Black),
                        )
                    }
                    CardLyrics(
                        lines = data.lines,
                        fontFamily = fontFamily,
                        textSize = 18.sp,
                        textColor = Color(0xFF333333),
                        secondarySize = 14.sp,
                        secondaryColor = Color(0xFF888888),
                        spacing = 20.dp,
                        secondarySpacing = 4.dp,
                        lineHeight = 28.sp,
                        secondaryLineHeight = 20.sp,
                    )
                }
                Row(Modifier.padding(top = 20.dp)) {
                    CardWordmark(
                        color = Color.Black,
                        size = 12.sp,
                        family = fontFamily,
                        alpha = 0.5f,
                    )
                    CardText(
                        text = " • ISSUE 01",
                        size = 12.sp,
                        color = Color.Black,
                        family = fontFamily,
                        alpha = 0.5f,
                    )
                }
            }
        }
    }
}
