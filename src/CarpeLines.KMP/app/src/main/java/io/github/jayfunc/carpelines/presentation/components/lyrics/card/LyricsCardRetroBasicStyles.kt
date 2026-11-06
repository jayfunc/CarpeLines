package io.github.jayfunc.carpelines.presentation.components.lyrics.card

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** `LyricsCardPodStyle.xaml`: click-wheel player with a tiny now-playing screen. */
@Composable
internal fun PodCard(
    data: LyricsCardData,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier,
) {
    CardSurface(Color(0xFFF5F5F5), modifier, RoundedCornerShape(8.dp)) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(20.dp)
                .border(1.dp, Color(0xFFD0D0D0), RoundedCornerShape(12.dp)),
        ) {
            Column(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .padding(start = 30.dp, top = 40.dp, end = 30.dp, bottom = 10.dp),
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White)
                            .border(2.dp, Color(0xFF333333), RoundedCornerShape(4.dp)),
                    ) {
                        Column {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(24.dp)
                                    .background(Color(0xFFF0F0F0))
                                    .drawBehind {
                                        drawLine(
                                            Color(0xFFCCCCCC),
                                            Offset(0f, size.height),
                                            Offset(size.width, size.height),
                                            1.dp.toPx(),
                                        )
                                    },
                            ) {
                                CardText(
                                    text = "Now Playing",
                                    size = 10.sp,
                                    color = Color(0xFF333333),
                                    weight = FontWeight.Bold,
                                    family = fontFamily,
                                    modifier = Modifier.align(Alignment.Center),
                                )
                                CardGlyph(
                                    CardGlyphs.Play,
                                    10.sp,
                                    Color(0xFF333333),
                                    Modifier.align(Alignment.CenterEnd).padding(end = 5.dp),
                                )
                                CardGlyph(
                                    CardGlyphs.Back,
                                    10.sp,
                                    Color(0xFF333333),
                                    Modifier.align(Alignment.CenterStart).padding(start = 5.dp),
                                )
                            }
                            Row(Modifier.weight(1f)) {
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(10.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Column(Modifier.fillMaxWidth()) {
                                        Box(
                                            Modifier
                                                .fillMaxWidth()
                                                .height(92.dp),
                                        ) {
                                            CardCover(data.cover, Modifier.matchParentSize(), ContentScale.Fit)
                                        }
                                        Box(
                                            Modifier
                                                .fillMaxWidth()
                                                .height(42.dp)
                                                .graphicsLayer { scaleY = -1f; alpha = 0.5f },
                                        ) {
                                            CardCover(data.cover, Modifier.matchParentSize(), ContentScale.Fit)
                                        }
                                        Box(
                                            Modifier
                                                .fillMaxWidth()
                                                .height(42.dp)
                                                .offset(y = (-42).dp)
                                                .background(
                                                    Brush.verticalGradient(
                                                        0f to Color.White.copy(alpha = 0.25f),
                                                        0.8f to Color.White,
                                                    ),
                                                ),
                                        )
                                    }
                                }
                                Column(
                                    Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(top = 10.dp, end = 10.dp, bottom = 10.dp)
                                        .clipToBounds(),
                                ) {
                                    CardText(
                                        text = data.title,
                                        size = 12.sp,
                                        color = Color.Black,
                                        weight = FontWeight.Bold,
                                        family = fontFamily,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    CardText(
                                        text = data.artist,
                                        size = 10.sp,
                                        color = Color(0xFF666666),
                                        family = fontFamily,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    CardLyrics(
                                        lines = data.lines,
                                        fontFamily = fontFamily,
                                        textSize = 9.sp,
                                        textColor = Color(0xFF333333),
                                        secondarySize = 8.sp,
                                        secondaryColor = Color(0xFF777777),
                                        spacing = 4.dp,
                                        secondarySpacing = 1.dp,
                                        weight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }
                }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    Box(
                        Modifier
                            .align(Alignment.Center)
                            .size(220.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF8F8F8))
                            .border(1.dp, Color(0xFFD9D9D9), CircleShape),
                    )
                    CardText(
                        text = "MENU",
                        size = 14.sp,
                        color = Color(0xFFAAAAAA),
                        weight = FontWeight.Bold,
                        family = fontFamily,
                        modifier = Modifier.align(Alignment.Center).offset(y = (-78).dp),
                    )
                    CardGlyph(
                        CardGlyphs.Play,
                        16.sp,
                        Color(0xFFAAAAAA),
                        Modifier.align(Alignment.Center).offset(y = 78.dp),
                    )
                    CardGlyph(
                        CardGlyphs.Previous,
                        16.sp,
                        Color(0xFFAAAAAA),
                        Modifier.align(Alignment.Center).offset(x = (-78).dp),
                    )
                    CardGlyph(
                        CardGlyphs.Next,
                        16.sp,
                        Color(0xFFAAAAAA),
                        Modifier.align(Alignment.Center).offset(x = 78.dp),
                    )
                    Box(
                        Modifier
                            .align(Alignment.Center)
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDDDDDD))
                            .border(1.dp, Color(0xFFCCCCCC), CircleShape),
                    )
                    CardText(
                        text = "Designed by CarpeLines on Earth",
                        size = 8.sp,
                        color = Color(0xFFBBBBBB),
                        family = fontFamily,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 15.dp),
                    )
                }
            }
        }
    }
}

/** `LyricsCardWindowsPhoneStyle.xaml`: full-bleed lock screen with a huge clock. */
@Composable
internal fun WindowsPhoneCard(
    data: LyricsCardData,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier,
) {
    CardSurface(Color.Black, modifier) {
        CardCover(
            cover = data.cover,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.6f,
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(400.dp)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        1f to Color(0xCC000000),
                    ),
                ),
        )
        // Status bar
        Row(
            Modifier
                .fillMaxWidth()
                .height(32.dp)
                .background(Color(0x33000000))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CardGlyph(CardGlyphs.Person, 14.sp, Color.White)
            Spacer(Modifier.width(8.dp))
            CardText("5G", 12.sp, Color.White, weight = FontWeight.Bold, family = fontFamily)
            Spacer(Modifier.weight(1f))
            CardGlyph(CardGlyphs.Settings, 14.sp, Color.White)
            Spacer(Modifier.width(12.dp))
            CardGlyph(CardGlyphs.Message, 14.sp, Color.White)
        }
        // App bar
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 32.dp)
                .height(100.dp)
                .background(Color(0xFF1F1F1F)),
        ) {
            Box(Modifier.size(100.dp)) {
                CardCover(data.cover, Modifier.matchParentSize(), ContentScale.Crop)
            }
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 15.dp, vertical = 10.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    CardText(
                        text = data.artist,
                        size = 15.sp,
                        color = Color.White,
                        family = fontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    CardText(
                        text = data.title,
                        size = 13.sp,
                        color = Color(0xFF999999),
                        family = fontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Row(
                    Modifier.padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(25.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CardGlyph(CardGlyphs.Previous, 18.sp, Color(0xFFCCCCCC))
                    CardGlyph(CardGlyphs.Play, 22.sp, Color.White)
                    CardGlyph(CardGlyphs.Next, 18.sp, Color(0xFFCCCCCC))
                }
            }
        }
        // Lyrics
        Box(
            Modifier
                .fillMaxSize()
                .padding(start = 30.dp, top = 160.dp, end = 30.dp, bottom = 240.dp)
                .clipToBounds(),
            contentAlignment = Alignment.Center,
        ) {
            CardLyrics(
                lines = data.lines,
                fontFamily = fontFamily,
                textSize = 26.sp,
                textColor = Color.White,
                secondarySize = 18.sp,
                secondaryColor = Color(0xFFA6A6A6),
                spacing = 24.dp,
                secondarySpacing = 4.dp,
                lineHeight = 34.sp,
                secondaryLineHeight = 24.sp,
                textAlign = TextAlign.Center,
            )
        }
        // Clock block
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, bottom = 40.dp),
        ) {
            CardText(
                text = formatCardTimeShort(),
                size = 90.sp,
                color = Color.White,
                family = fontFamily,
                modifier = Modifier.offset(x = (-4).dp, y = (-12).dp),
            )
            CardText(
                text = formatCardDateLong(),
                size = 22.sp,
                color = Color.White,
                family = fontFamily,
                modifier = Modifier.offset(x = 2.dp),
            )
            CardText(
                text = "CarpeLines",
                size = 16.sp,
                color = Color(0xFFCCCCCC),
                family = fontFamily,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp, start = 2.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CardGlyph(CardGlyphs.Message, 20.sp, Color.White)
                    CardText("1", 10.sp, Color.White, weight = FontWeight.Bold, family = fontFamily)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CardGlyph(CardGlyphs.Person, 20.sp, Color.White)
                    CardText("5", 10.sp, Color.White, weight = FontWeight.Bold, family = fontFamily)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CardGlyph(CardGlyphs.Link, 20.sp, Color.White)
                    CardText("2", 10.sp, Color.White, weight = FontWeight.Bold, family = fontFamily)
                }
            }
        }
        CardGlyph(
            CardGlyphs.ChevronDown,
            12.sp,
            Color.White,
            Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
            alpha = 0.5f,
        )
    }
}
