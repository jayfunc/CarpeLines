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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** `LyricsCardInkWashStyle.xaml`: paper, red seal and a vertical margin rule. */
@Composable
internal fun InkWashCard(
    data: LyricsCardData,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier,
) {
    CardSurface(Color(0xFFF5F4F0), modifier) {
        CardCover(
            data.cover,
            Modifier.matchParentSize(),
            ContentScale.Crop,
            alpha = 0.1f,
        )
        Box(Modifier.matchParentSize().background(Color(0xFFF5F4F0).copy(alpha = 0.6f)))
        Row(Modifier.fillMaxSize().padding(24.dp)) {
            Box(Modifier.width(50.dp).fillMaxHeight()) {
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(Color(0xFF333333).copy(alpha = 0.15f)),
                )
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 40.dp)
                        .size(width = 28.dp, height = 60.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFA01C1C)),
                    contentAlignment = Alignment.Center,
                ) {
                    CardText(
                        "水墨",
                        14.sp,
                        Color(0xFFF5F4F0),
                        family = fontFamily,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(14.dp),
                    )
                }
            }
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 20.dp, top = 10.dp, end = 10.dp, bottom = 10.dp),
            ) {
                Column(Modifier.padding(bottom = 30.dp)) {
                    CardText(
                        data.title,
                        28.sp,
                        Color(0xFF1A1A1A),
                        weight = FontWeight.SemiBold,
                        family = fontFamily,
                    )
                    CardText(
                        data.artist,
                        16.sp,
                        Color(0xFF595959),
                        family = fontFamily,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .height(1.dp)
                            .background(Color(0xFF1A1A1A).copy(alpha = 0.1f)),
                    )
                }
                Column(
                    Modifier
                        .weight(1f)
                        .clipToBounds(),
                ) {
                    CardLyrics(
                        lines = data.lines,
                        fontFamily = fontFamily,
                        textSize = 18.sp,
                        textColor = Color(0xFF262626),
                        secondarySize = 14.sp,
                        secondaryColor = Color(0xFF737373),
                        spacing = 20.dp,
                        secondarySpacing = 6.dp,
                        lineHeight = 28.sp,
                        secondaryLineHeight = 22.sp,
                    )
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 20.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CardText(
                        "曲拨心弦 词落云笺",
                        12.sp,
                        Color(0xFF333333),
                        family = fontFamily,
                        alpha = 0.7f,
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.size(4.dp).clip(CircleShape).background(Color(0xFFA01C1C)))
                }
            }
        }
    }
}

/** `LyricsCardAncientBookStyle.xaml`: right-to-left vertical columns inside a double frame. */
@Composable
internal fun AncientBookCard(
    data: LyricsCardData,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier,
) {
    CardSurface(Color(0xFFEFE8D8), modifier) {
        Box(Modifier.matchParentSize().padding(20.dp).border(2.dp, Color(0xFF3A2E28))) {
            Box(Modifier.matchParentSize().padding(3.dp).border(1.dp, Color(0xFF3A2E28))) {
                Row(Modifier.fillMaxSize()) {
                    Column(
                        Modifier
                            .width(45.dp)
                            .fillMaxHeight()
                            .background(Color(0xFFEBE3D1)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Column(
                            Modifier.padding(top = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(9.dp),
                        ) {
                            AncientVerticalText("曲拨心弦", 12.sp, 18.sp, Color(0xFF3A2E28), fontFamily)
                            AncientVerticalText("词落云笺", 12.sp, 18.sp, Color(0xFF3A2E28), fontFamily)
                        }
                        Spacer(Modifier.weight(1f))
                        CardText(
                            "卷\n一",
                            12.sp,
                            Color(0xFF3A2E28),
                            family = fontFamily,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                        )
                    }
                    Box(Modifier.width(1.dp).fillMaxHeight().background(Color(0xFF3A2E28)))
                    Row(Modifier.weight(1f).fillMaxHeight()) {
                        Row(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clipToBounds()
                                .horizontalScroll(rememberScrollState(Int.MAX_VALUE)),
                        ) {
                            data.lines.reversed().forEach { line ->
                                Row(
                                    Modifier
                                        .fillMaxHeight()
                                        .drawBehind {
                                            drawLine(
                                                Color(0xFFBAB1A4),
                                                Offset(size.width, 0f),
                                                Offset(size.width, size.height),
                                                1.dp.toPx(),
                                            )
                                        }
                                        .padding(horizontal = 16.dp, vertical = 20.dp),
                                ) {
                                    AncientVerticalText(
                                        line.text,
                                        18.sp,
                                        26.sp,
                                        Color(0xFF241B15),
                                        fontFamily,
                                        width = 24.dp,
                                    )
                                    if (!line.secondary.isNullOrBlank()) {
                                        AncientVerticalText(
                                            line.secondary,
                                            14.sp,
                                            22.sp,
                                            Color(0xFF6B5D54),
                                            fontFamily,
                                            width = 18.dp,
                                            modifier = Modifier.padding(start = 12.dp),
                                        )
                                    }
                                }
                            }
                        }
                        Box(Modifier.width(1.dp).fillMaxHeight().background(Color(0xFFBAB1A4)))
                        Row(
                            Modifier.fillMaxHeight().padding(horizontal = 16.dp, vertical = 20.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            AncientVerticalText(
                                data.title,
                                26.sp,
                                30.sp,
                                Color(0xFF241B15),
                                fontFamily,
                                width = 32.dp,
                                weight = FontWeight.Bold,
                            )
                            Column(
                                Modifier
                                    .padding(start = 16.dp)
                                    .fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                            ) {
                                Box(
                                    Modifier
                                        .padding(bottom = 8.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color(0xFF8C2218))
                                        .padding(horizontal = 2.dp, vertical = 4.dp),
                                ) {
                                    CardText("撰", 10.sp, Color(0xFFEFE8D8), family = fontFamily)
                                }
                                AncientVerticalText(
                                    data.artist,
                                    15.sp,
                                    22.sp,
                                    Color(0xFF5A4E46),
                                    fontFamily,
                                    width = 20.dp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AncientVerticalText(
    text: String,
    size: androidx.compose.ui.unit.TextUnit,
    lineHeight: androidx.compose.ui.unit.TextUnit,
    color: Color,
    fontFamily: FontFamily,
    width: androidx.compose.ui.unit.Dp = 12.dp,
    modifier: Modifier = Modifier,
    weight: FontWeight = FontWeight.Normal,
) {
    CardText(
        text = text,
        size = size,
        color = color,
        weight = weight,
        family = fontFamily,
        lineHeight = lineHeight,
        textAlign = TextAlign.Center,
        modifier = modifier.width(width),
    )
}

/** `LyricsCardBambooSlipsStyle.xaml`: bound bamboo slats with a red seal. */
@Composable
internal fun BambooSlipsCard(
    data: LyricsCardData,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier,
) {
    CardSurface(Color(0xFFE6D3B3), modifier) {
        Column(Modifier.matchParentSize().padding(vertical = 20.dp)) {
            BambooRail()
            Row(Modifier.weight(1f).fillMaxWidth()) {
                Row(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(start = 20.dp, top = 40.dp, bottom = 40.dp)
                        .clipToBounds()
                        .horizontalScroll(rememberScrollState(Int.MAX_VALUE)),
                ) {
                    data.lines.reversed().forEach { line ->
                        Row(
                            Modifier
                                .width(60.dp)
                                .fillMaxHeight()
                                .background(Color(0xFFE6D3B3))
                                .drawBehind {
                                    drawLine(
                                        Color(0xFFC2A882),
                                        Offset(size.width - 1.dp.toPx(), 0f),
                                        Offset(size.width - 1.dp.toPx(), size.height),
                                        2.dp.toPx(),
                                    )
                                },
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            AncientVerticalText(
                                line.text,
                                20.sp,
                                30.sp,
                                Color(0xFF1A110B),
                                fontFamily,
                                width = 28.dp,
                            )
                            if (!line.secondary.isNullOrBlank()) {
                                AncientVerticalText(
                                    line.secondary,
                                    15.sp,
                                    22.sp,
                                    Color(0xFF5A402D),
                                    fontFamily,
                                    width = 16.dp,
                                )
                            }
                        }
                    }
                }
                Box(Modifier.fillMaxHeight().padding(0.dp, 40.dp, 20.dp, 40.dp)) {
                    Row(
                        Modifier.fillMaxHeight(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .drawBehind {
                                    drawLine(
                                        Color(0xFFC2A882),
                                        Offset(size.width, 0f),
                                        Offset(size.width, size.height),
                                        2.dp.toPx(),
                                    )
                                }
                                .padding(start = 10.dp),
                        ) {
                            AncientVerticalText(
                                data.title,
                                32.sp,
                                38.sp,
                                Color(0xFF1A110B),
                                fontFamily,
                                width = 40.dp,
                            )
                        }
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .padding(start = 5.dp)
                                .drawBehind {
                                    drawLine(
                                        Color(0xFFC2A882),
                                        Offset(size.width, 0f),
                                        Offset(size.width, size.height),
                                        2.dp.toPx(),
                                    )
                                },
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            AncientVerticalText(
                                data.artist,
                                18.sp,
                                24.sp,
                                Color(0xFF4A3424),
                                fontFamily,
                                width = 24.dp,
                                modifier = Modifier.padding(bottom = 20.dp),
                            )
                        }
                    }
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .offset(y = (-20).dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF80261E)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.matchParentSize().padding(2.dp).clip(CircleShape).background(Color(0xFF9E3026)))
                        CardText("简", 16.sp, Color(0xFFE3CBA8), family = fontFamily)
                    }
                }
            }
            BambooRail()
        }
    }
}

@Composable
private fun BambooRail() {
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(4.dp).background(Color(0xFF3E2723).copy(alpha = 0.8f)))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF6D4C41)))
    }
}

/** `LyricsCardDunhuangStyle.xaml`: sand-toned palette, dunes and a Maogao seal. */
@Composable
internal fun DunhuangCard(
    data: LyricsCardData,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier,
) {
    CardSurface(Color(0xFFD6C4A3), modifier) {
        androidx.compose.foundation.Canvas(Modifier.matchParentSize().background(Color(0xFFD6C4A3))) {
            fun wave(color: Color, y1: Float, y2: Float, y3: Float) {
                val path = Path().apply {
                    moveTo(0f, size.height * y1)
                    cubicTo(
                        size.width * 0.33f, size.height * y2,
                        size.width * 0.66f, size.height * y3,
                        size.width, size.height * y1,
                    )
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(path, color.copy(alpha = 0.15f))
            }
            wave(Color(0xFF4A8272), 0.25f, 0.08f, 0.50f)
            wave(Color(0xFF3B5B6D), 0.50f, 0.75f, 0.25f)
            wave(Color(0xFFA24A3B), 0.75f, 0.58f, 0.92f)
        }
        Box(Modifier.matchParentSize().padding(20.dp)) {
            Box(
                Modifier
                    .matchParentSize()
                    .drawBehind {
                        val stroke = 1.dp.toPx()
                        drawRect(Color(0xFF8F4A3E), style = Stroke(stroke))
                        drawLine(Color(0xFF8F4A3E), Offset(0f, 2.dp.toPx()), Offset(size.width, 2.dp.toPx()), 4.dp.toPx())
                        drawLine(
                            Color(0xFF8F4A3E),
                            Offset(0f, size.height - 2.dp.toPx()),
                            Offset(size.width, size.height - 2.dp.toPx()),
                            4.dp.toPx(),
                        )
                    },
            )
            Box(Modifier.matchParentSize().padding(4.dp).border(1.dp, Color(0xFF3B5B6D)))
        }
        Column(Modifier.fillMaxSize().padding(start = 36.dp, top = 40.dp, end = 36.dp, bottom = 40.dp)) {
            Column(Modifier.padding(bottom = 20.dp)) {
                CardText(
                    data.title,
                    30.sp,
                    Color(0xFF2A1610),
                    weight = FontWeight.Bold,
                    family = fontFamily,
                )
                CardText(
                    data.artist,
                    16.sp,
                    Color(0xFF6B4B3A),
                    family = fontFamily,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(bottom = 30.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(4.dp).clip(CircleShape).background(Color(0xFF3B5B6D)))
                Spacer(Modifier.width(8.dp))
                Box(Modifier.width(60.dp).height(2.dp).background(Color(0xFF8F4A3E)))
                Spacer(Modifier.width(8.dp))
                Box(Modifier.size(4.dp).clip(CircleShape).background(Color(0xFF4A8272)))
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                Column(Modifier.fillMaxSize().clipToBounds()) {
                    CardLyrics(
                        lines = data.lines,
                        fontFamily = fontFamily,
                        textSize = 19.sp,
                        textColor = Color(0xFF301F16),
                        secondarySize = 14.sp,
                        secondaryColor = Color(0xFF4B6270),
                        spacing = 24.dp,
                        secondarySpacing = 6.dp,
                        lineHeight = 28.sp,
                        secondaryLineHeight = 22.sp,
                    )
                }
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF8F4A3E).copy(alpha = 0.8f))
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                ) {
                    CardText("莫高", 12.sp, Color(0xFFD6C4A3), family = fontFamily)
                }
            }
        }
    }
}
