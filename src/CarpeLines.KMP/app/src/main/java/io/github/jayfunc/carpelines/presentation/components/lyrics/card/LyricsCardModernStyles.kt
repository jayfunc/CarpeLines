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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
private fun CardAvatar(
    cover: android.graphics.Bitmap?,
    size: Dp,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = CircleShape,
) {
    Box(modifier.size(size).clip(shape).background(Color(0xFFE0E0E0))) {
        CardCover(cover, Modifier.matchParentSize())
    }
}

/** A 6 x 12 WeChat tail: flat edge against the bubble, tip pointing away from it. */
@Composable
private fun BubbleTail(
    color: Color,
    tipRight: Boolean,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(width = 6.dp, height = 12.dp)) {
        val path = Path()
        if (tipRight) {
            path.moveTo(0f, 0f)
            path.lineTo(size.width, size.height / 2f)
            path.lineTo(0f, size.height)
        } else {
            path.moveTo(size.width, 0f)
            path.lineTo(0f, size.height / 2f)
            path.lineTo(size.width, size.height)
        }
        path.close()
        drawPath(path, color)
    }
}

/**
 * WhatsApp's 8 x 8 corner tail. The right angle sits on the bubble's top edge, so the tip points
 * up and away from the bubble exactly as the template's `M 8 0 L 0 0 L 8 8 Z` path.
 */
@Composable
private fun WhatsAppTail(
    color: Color,
    tipRight: Boolean,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(8.dp)) {
        val path = Path()
        if (tipRight) {
            path.moveTo(0f, 0f)
            path.lineTo(size.width, 0f)
            path.lineTo(0f, size.height)
        } else {
            path.moveTo(size.width, 0f)
            path.lineTo(0f, 0f)
            path.lineTo(size.width, size.height)
        }
        path.close()
        drawPath(path, color)
    }
}

/**
 * Telegram's 8 x 8 bottom tail. The right angle sits on the bubble's bottom edge, matching the
 * template's `M 8 0 L 8 8 L 0 8 Z` path with its tip pointing down and away.
 */
@Composable
private fun TelegramTail(
    color: Color,
    tipRight: Boolean,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(8.dp)) {
        val path = Path()
        if (tipRight) {
            path.moveTo(0f, size.height)
            path.lineTo(size.width, size.height)
            path.lineTo(0f, 0f)
        } else {
            path.moveTo(size.width, 0f)
            path.lineTo(size.width, size.height)
            path.lineTo(0f, size.height)
        }
        path.close()
        drawPath(path, color)
    }
}

/**
 * LINE draws its tail as an 8 x 8 triangle rotated 45 degrees around its centre and shifted into
 * the bubble's rounded top corner, so the tail reads as a pointed notch on the corner. Painting it
 * behind the bubble's background leaves only the part that falls outside the rounded outline.
 */
private fun Modifier.lineBubbleTail(color: Color, tipRight: Boolean): Modifier = drawBehind {
    val unit = 1.dp.toPx()
    val path = Path()
    if (tipRight) {
        path.moveTo(0f, 0f)
        path.lineTo(8f * unit, 4f * unit)
        path.lineTo(0f, 8f * unit)
    } else {
        path.moveTo(8f * unit, 0f)
        path.lineTo(0f, 4f * unit)
        path.lineTo(8f * unit, 8f * unit)
    }
    path.close()

    if (tipRight) {
        translate(size.width) {
            translate(left = -7f * unit, top = -2f * unit) {
                rotate(degrees = -45f, pivot = Offset(4f * unit, 4f * unit)) {
                    drawPath(path, color)
                }
            }
        }
    } else {
        translate(left = 7f * unit, top = -2f * unit) {
            rotate(degrees = 45f, pivot = Offset(4f * unit, 4f * unit)) {
                drawPath(path, color)
            }
        }
    }
}

/** `LyricsCardQQStyle.xaml`: modern QQ chat bubbles around a shared track card. */
@Composable
internal fun QQCard(
    data: LyricsCardData,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier,
) {
    CardSurface(Color(0xFFF2F2F2), modifier, RoundedCornerShape(8.dp)) {
        Column(
            Modifier
                .fillMaxSize()
                .border(1.dp, Color(0xFFE5E5E5), RoundedCornerShape(8.dp)),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CardText(
                    data.artist,
                    size = 14.sp,
                    color = Color(0xFF333333),
                    weight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(250.dp),
                )
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    CardGlyph(CardGlyphs.Contact, 13.sp, Color(0xFF333333))
                    CardGlyph(CardGlyphs.Video, 13.sp, Color(0xFF333333))
                    CardGlyph(CardGlyphs.Phone, 13.sp, Color(0xFF333333))
                    CardGlyph(CardGlyphs.More, 13.sp, Color(0xFF333333))
                    CardGlyph(CardGlyphs.Search, 13.sp, Color(0xFF333333))
                    CardGlyph(CardGlyphs.Menu, 13.sp, Color(0xFF333333))
                }
            }
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds()
                    .padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 20.dp),
            ) {
                CardText(
                    "刚刚",
                    size = 12.sp,
                    color = Color(0xFFB3B3B3),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                )
                Row(Modifier.padding(bottom = 16.dp)) {
                    CardAvatar(data.cover, 36.dp, Modifier.align(Alignment.Top))
                    Spacer(Modifier.width(6.dp))
                    Column(
                        Modifier
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFEBEBEB), RoundedCornerShape(12.dp))
                            .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 8.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.size(48.dp).clip(RoundedCornerShape(4.dp))) {
                                CardCover(data.cover, Modifier.matchParentSize(), ContentScale.Crop)
                            }
                            Column(Modifier.align(Alignment.CenterVertically)) {
                                CardText(
                                    data.title,
                                    14.sp,
                                    Color.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.width(220.dp),
                                )
                                CardText(
                                    data.artist,
                                    12.sp,
                                    Color(0xFF808080),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                        }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, bottom = 8.dp)
                                .height(1.dp)
                                .background(Color(0xFFF0F0F0)),
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CardGlyph(CardGlyphs.Link, 10.sp, Color(0xFFA0A0A0))
                            Spacer(Modifier.width(4.dp))
                            CardText("CarpeLines", 10.sp, Color(0xFFA0A0A0))
                        }
                    }
                }
                data.lines.forEach { line ->
                    Row(Modifier.padding(bottom = 16.dp)) {
                        CardAvatar(data.cover, 36.dp, Modifier.align(Alignment.Top))
                        Spacer(Modifier.width(6.dp))
                        Box(
                            Modifier
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFEBEBEB), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        ) {
                            CardText(line.text, 15.sp, Color.Black, family = fontFamily, lineHeight = 22.sp)
                        }
                    }
                    if (!line.secondary.isNullOrBlank()) {
                        Row(
                            Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            Box(
                                Modifier
                                    .background(Color(0xFF0099FF), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                            ) {
                                CardText(line.secondary, 15.sp, Color.White, family = fontFamily, lineHeight = 22.sp)
                            }
                        }
                    }
                }
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(Color(0xFFF5F5F5))
                    .drawTopLine(Color(0xFFE5E5E5)),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    listOf(CardGlyphs.Mic, CardGlyphs.Image, CardGlyphs.Folder, CardGlyphs.Camera, CardGlyphs.Emoji, CardGlyphs.Music, CardGlyphs.Add)
                        .forEach { CardGlyph(it, 13.sp, Color(0xFF333333)) }
                }
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier.fillMaxWidth().padding(end = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Box(
                        Modifier
                            .border(1.dp, Color(0xFFD3D3D3), RoundedCornerShape(8.dp))
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                    ) {
                        CardText("关闭", 13.sp, Color(0xFF333333))
                    }
                    Spacer(Modifier.width(6.dp))
                    Row(
                        Modifier
                            .background(Color(0xFF0099FF), RoundedCornerShape(8.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CardText("发送", 13.sp, Color(0xFF3EB2FF))
                        Box(Modifier.padding(horizontal = 10.dp).width(1.dp).height(14.dp).background(Color(0xFF12A0FF)))
                        CardGlyph(CardGlyphs.ChevronDown, 10.sp, Color(0xFFC5E8FF))
                    }
                }
            }
        }
    }
}

/** `LyricsCardWeChatStyle.xaml`: chat with square avatars and green reply bubbles. */
@Composable
internal fun WeChatCard(
    data: LyricsCardData,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier,
) {
    CardSurface(Color(0xFFF5F5F5), modifier, RoundedCornerShape(4.dp)) {
        Column(
            Modifier
                .fillMaxSize()
                .border(1.dp, Color(0xFFE7E7E7), RoundedCornerShape(4.dp)),
        ) {
            Row(
                Modifier.fillMaxWidth().height(50.dp).padding(start = 20.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CardText(
                    data.artist,
                    16.sp,
                    Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(300.dp),
                )
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CardGlyph(CardGlyphs.Search, 16.sp, Color.Black)
                    CardGlyph(CardGlyphs.Contact, 16.sp, Color.Black)
                    CardGlyph(CardGlyphs.Menu, 16.sp, Color.Black)
                }
            }
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds()
                    .padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 30.dp),
            ) {
                CardText(
                    formatCardTimeShort(),
                    12.sp,
                    Color(0xFFDADADA),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                )
                Row(Modifier.padding(bottom = 16.dp)) {
                    CardAvatar(
                        data.cover,
                        36.dp,
                        Modifier.align(Alignment.Top),
                        RoundedCornerShape(4.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    BubbleTail(Color.White, tipRight = false, modifier = Modifier.padding(top = 14.dp))
                    Column(
                        Modifier
                            .width(250.dp)
                            .background(Color.White, RoundedCornerShape(4.dp)),
                    ) {
                        Row(Modifier.padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 8.dp)) {
                            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                CardText(
                                    data.title,
                                    15.sp,
                                    Color.Black,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                CardText(
                                    data.artist,
                                    12.sp,
                                    Color(0xFF888888),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                            Box(Modifier.size(44.dp).clip(RoundedCornerShape(2.dp))) {
                                CardCover(data.cover, Modifier.matchParentSize(), ContentScale.Crop)
                            }
                        }
                        Box(Modifier.padding(horizontal = 12.dp).height(1.dp).fillMaxWidth().background(Color(0xFFF0F0F0)))
                        Row(
                            Modifier.padding(start = 12.dp, top = 4.dp, end = 12.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CardGlyph(CardGlyphs.Link, 10.sp, Color(0xFFB0B0B0))
                            Spacer(Modifier.width(4.dp))
                            CardText("CarpeLines", 10.sp, Color(0xFFB0B0B0))
                        }
                    }
                }
                data.lines.forEach { line ->
                    Row(Modifier.padding(bottom = 16.dp)) {
                        CardAvatar(
                            data.cover,
                            36.dp,
                            Modifier.align(Alignment.CenterVertically),
                            RoundedCornerShape(4.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        BubbleTail(Color.White, tipRight = false, modifier = Modifier.padding(top = 14.dp))
                        Box(
                            Modifier
                                .background(Color.White, RoundedCornerShape(4.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        ) {
                            CardText(line.text, 15.sp, Color.Black, family = fontFamily, lineHeight = 24.sp)
                        }
                    }
                    if (!line.secondary.isNullOrBlank()) {
                        Row(
                            Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            Box(
                                Modifier
                                    .background(Color(0xFF95EC69), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                            ) {
                                CardText(line.secondary, 15.sp, Color.Black, family = fontFamily, lineHeight = 24.sp)
                            }
                            BubbleTail(Color(0xFF95EC69), tipRight = true, modifier = Modifier.padding(top = 14.dp))
                        }
                    }
                }
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(Color(0xFFF5F5F5))
                    .drawTopLine(Color(0xFFECECEC)),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    listOf(CardGlyphs.Mic, CardGlyphs.Image, CardGlyphs.Folder, CardGlyphs.Camera, CardGlyphs.Add)
                        .forEach { CardGlyph(it, 16.sp, Color.Black) }
                }
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier.fillMaxWidth().padding(end = 16.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Box(
                        Modifier
                            .background(Color(0xFFE0E0E0), RoundedCornerShape(4.dp))
                            .padding(horizontal = 24.dp, vertical = 6.dp),
                    ) {
                        CardText("发送(S)", 13.sp, Color(0xFFA2A2A2))
                    }
                }
            }
        }
    }
}

/** `LyricsCardWhatsAppStyle.xaml`: media message plus white and green chat bubbles. */
@Composable
internal fun WhatsAppCard(
    data: LyricsCardData,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier,
) {
    CardSurface(Color(0xFFEFEAE2), modifier) {
        Column(
            Modifier
                .fillMaxSize()
                .border(1.dp, Color(0xFFD1D7DB)),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(Color.White)
                    .drawBehindBottomLine(Color(0xFFD1D7DB))
                    .padding(start = 16.dp, end = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CardAvatar(data.cover, 40.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    CardText(data.artist, 16.sp, Color(0xFF111B21), weight = FontWeight.SemiBold)
                    CardText("online", 13.sp, Color(0xFF667781), modifier = Modifier.padding(top = 2.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    CardGlyph(CardGlyphs.Video, 16.sp, Color.Black)
                    CardGlyph(CardGlyphs.Phone, 16.sp, Color.Black)
                }
            }
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds()
                    .padding(top = 12.dp, bottom = 24.dp),
            ) {
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .background(Color.White, RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    CardText("TODAY", 12.sp, Color(0xFF54656F))
                }
                Row(Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    WhatsAppTail(Color.White, tipRight = false, modifier = Modifier.offset(x = 1.dp))
                    Column(
                        Modifier
                            .background(Color.White, RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp, bottomStart = 8.dp))
                            .padding(start = 4.dp, top = 4.dp, end = 4.dp, bottom = 8.dp),
                    ) {
                        Box(Modifier.size(260.dp).clip(RoundedCornerShape(6.dp))) {
                            CardCover(data.cover, Modifier.matchParentSize(), ContentScale.Crop)
                        }
                        Row(Modifier.padding(start = 8.dp, top = 8.dp, end = 4.dp)) {
                            CardText(
                                data.title,
                                15.sp,
                                Color(0xFF111B21),
                                modifier = Modifier.weight(1f).padding(end = 60.dp),
                            )
                            CardText("10:05 AM", 11.sp, Color(0xFF667781), modifier = Modifier.align(Alignment.Bottom))
                        }
                    }
                }
                data.lines.forEach { line ->
                    Row(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp)) {
                        WhatsAppTail(Color.White, tipRight = false, modifier = Modifier.offset(x = 1.dp))
                        Column(
                            Modifier
                                .background(Color.White, RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp, bottomStart = 8.dp))
                                .padding(start = 12.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
                        ) {
                            CardText(line.text, 15.sp, Color(0xFF111B21), family = fontFamily, lineHeight = 22.sp)
                            CardText(
                                "10:06 AM",
                                11.sp,
                                Color(0xFF667781),
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                            )
                        }
                    }
                    if (!line.secondary.isNullOrBlank()) {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            Column(
                                Modifier
                                    .background(Color(0xFFD9FDD3), RoundedCornerShape(topStart = 8.dp, bottomEnd = 8.dp, bottomStart = 8.dp))
                                    .padding(start = 12.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
                            ) {
                                CardText(line.secondary, 15.sp, Color(0xFF111B21), family = fontFamily, lineHeight = 22.sp)
                                Row(
                                    Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    CardText("10:07 AM", 11.sp, Color(0xFF667781), modifier = Modifier.padding(end = 4.dp))
                                    CardGlyph(CardGlyphs.DoubleCheck, 13.sp, Color(0xFF53BDEB))
                                }
                            }
                            WhatsAppTail(Color(0xFFD9FDD3), tipRight = true, modifier = Modifier.offset(x = (-1).dp))
                        }
                    }
                }
            }
            Row(
                Modifier
                    .padding(12.dp)
                    .fillMaxWidth()
                    .height(62.dp)
                    .background(Color.White, RoundedCornerShape(32.dp))
                    .padding(start = 20.dp, end = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CardGlyph(CardGlyphs.Add, 20.sp, Color.Black)
                Spacer(Modifier.width(20.dp))
                CardGlyph(CardGlyphs.Camera, 20.sp, Color.Black)
                Spacer(Modifier.weight(1f))
                CardText("CarpeLines", 14.sp, Color(0xFF666666), modifier = Modifier.weight(4f))
                CardGlyph(CardGlyphs.Send, 20.sp, Color.Black)
            }
        }
    }
}

/** `LyricsCardTelegramStyle.xaml`: dark Telegram desktop bubbles. */
@Composable
internal fun TelegramCard(
    data: LyricsCardData,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier,
) {
    CardSurface(Color(0xFF0E1621), modifier, RoundedCornerShape(8.dp)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .background(Color(0xFF17212B))
                    .padding(start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CardAvatar(data.cover, 40.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    CardText(data.artist, 15.sp, Color(0xFFF5F5F5), weight = FontWeight.SemiBold)
                    CardText("online", 13.sp, Color(0xFF5288C1), modifier = Modifier.padding(top = 2.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    listOf(CardGlyphs.Video, CardGlyphs.Contact, CardGlyphs.Search, CardGlyphs.Menu)
                        .forEach { CardGlyph(it, 16.sp, Color(0xFF7B8DA1)) }
                }
            }
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds()
                    .padding(top = 16.dp, bottom = 24.dp),
            ) {
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .background(Color(0xFF18222D), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    CardText("Today", 13.sp, Color(0xFF7A8B9C), weight = FontWeight.SemiBold)
                }
                Row(Modifier.padding(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 16.dp)) {
                    TelegramTail(
                        Color(0xFF182533),
                        tipRight = false,
                        modifier = Modifier.align(Alignment.Bottom).offset(x = 1.dp),
                    )
                    Column(
                        Modifier
                            .background(Color(0xFF182533), RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomEnd = 12.dp))
                            .padding(12.dp),
                    ) {
                        Row {
                            Box(
                                Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2B5278)),
                                contentAlignment = Alignment.Center,
                            ) {
                                CardCover(data.cover, Modifier.matchParentSize())
                                Box(Modifier.matchParentSize().background(Color(0x66000000)))
                                CardGlyph(CardGlyphs.Play, 18.sp, Color.White)
                            }
                            Column(Modifier.padding(start = 12.dp).width(200.dp)) {
                                CardText(data.title, 15.sp, Color.White, weight = FontWeight.SemiBold, maxLines = 2)
                                CardText(data.artist, 13.sp, Color(0xFF7B8DA1), modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                        CardText(
                            "10:05",
                            11.sp,
                            Color(0xFF7B8DA1),
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        )
                    }
                }
                data.lines.forEach { line ->
                    Row(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp)) {
                        TelegramTail(
                            Color(0xFF182533),
                            tipRight = false,
                            modifier = Modifier.align(Alignment.Bottom).offset(x = 1.dp),
                        )
                        Column(
                            Modifier
                                .background(Color(0xFF182533), RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomEnd = 12.dp))
                                .padding(start = 12.dp, top = 10.dp, end = 12.dp, bottom = 6.dp),
                        ) {
                            CardText(line.text, 15.sp, Color.White, family = fontFamily, lineHeight = 22.sp)
                            CardText(
                                "10:06",
                                11.sp,
                                Color(0xFF7B8DA1),
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            )
                        }
                    }
                    if (!line.secondary.isNullOrBlank()) {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            Column(
                                Modifier
                                    .background(Color(0xFF2B5278), RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 12.dp))
                                    .padding(start = 12.dp, top = 10.dp, end = 12.dp, bottom = 6.dp),
                            ) {
                                CardText(line.secondary, 15.sp, Color.White, family = fontFamily, lineHeight = 22.sp)
                                Row(
                                    Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    CardText("10:07", 11.sp, Color(0xFF71A0CD), modifier = Modifier.padding(end = 4.dp))
                                    CardGlyph(CardGlyphs.DoubleCheck, 13.sp, Color(0xFF53AEE5))
                                }
                            }
                            TelegramTail(
                                Color(0xFF2B5278),
                                tipRight = true,
                                modifier = Modifier.align(Alignment.Bottom).offset(x = (-1).dp),
                            )
                        }
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                    .background(Color(0xFF17212B))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CardGlyph(CardGlyphs.Attach, 20.sp, Color(0xFF7B8DA1))
                CardText(
                    "CarpeLines",
                    14.sp,
                    Color(0xFF7B8DA1),
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                )
                CardGlyph(CardGlyphs.Mic, 20.sp, Color(0xFF7B8DA1))
                Spacer(Modifier.width(16.dp))
                CardGlyph(CardGlyphs.Send, 20.sp, Color(0xFF7B8DA1))
            }
        }
    }
}

/** `LyricsCardLINEStyle.xaml`: pale blue LINE chat with read receipts. */
@Composable
internal fun LINECard(
    data: LyricsCardData,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier,
) {
    CardSurface(Color(0xFF8CABD9), modifier) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(Color.White)
                    .padding(start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CardGlyph(CardGlyphs.Back, 20.sp, Color(0xFF111111))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    CardText(
                        data.artist,
                        16.sp,
                        Color(0xFF111111),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(250.dp),
                    )
                    CardText("自動で返信しています", 12.sp, Color(0xFF939393))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    CardGlyph(CardGlyphs.Search, 16.sp, Color(0xFF111111))
                    CardGlyph(CardGlyphs.Globe, 16.sp, Color(0xFF111111))
                }
            }
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds()
                    .padding(top = 16.dp, bottom = 24.dp),
            ) {
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .background(Color(0x33000000), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    CardText("今日", 12.sp, Color.White)
                }
                Row(Modifier.padding(start = 12.dp, top = 20.dp, end = 16.dp, bottom = 16.dp)) {
                    CardAvatar(data.cover, 40.dp, Modifier.align(Alignment.Top))
                    Row(Modifier.padding(start = 4.dp)) {
                        Column(
                            Modifier
                                .lineBubbleTail(Color.White, tipRight = false)
                                .background(Color.White, RoundedCornerShape(16.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .size(30.dp)
                                        .border(1.dp, Color(0xFFF0F0F0), RoundedCornerShape(4.dp)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CardGlyph(CardGlyphs.Link, 14.sp, Color(0xFF888888))
                                }
                                CardText(
                                    data.title,
                                    15.sp,
                                    Color(0xFF111111),
                                    maxLines = 2,
                                    modifier = Modifier.padding(start = 10.dp).width(200.dp),
                                )
                            }
                        }
                        CardText(
                            "10:05",
                            10.sp,
                            Color(0xFF445975),
                            modifier = Modifier.align(Alignment.Bottom).padding(start = 6.dp, bottom = 6.dp),
                        )
                    }
                }
                data.lines.forEach { line ->
                    Row(Modifier.padding(start = 12.dp, end = 16.dp, bottom = 4.dp)) {
                        CardAvatar(data.cover, 40.dp, Modifier.align(Alignment.Top))
                        Row(Modifier.padding(start = 4.dp)) {
                            Column(
                                Modifier
                                    .lineBubbleTail(Color.White, tipRight = false)
                                    .background(Color.White, RoundedCornerShape(16.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                            ) {
                                CardText(line.text, 15.sp, Color(0xFF111111), family = fontFamily, lineHeight = 22.sp)
                            }
                            CardText(
                                "10:06",
                                10.sp,
                                Color(0xFF445975),
                                modifier = Modifier.align(Alignment.Bottom).padding(start = 6.dp, bottom = 6.dp),
                            )
                        }
                    }
                    if (!line.secondary.isNullOrBlank()) {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp, end = 4.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            Column(Modifier.align(Alignment.Bottom).padding(end = 6.dp)) {
                                CardText("既読", 10.sp, Color(0xFF445975), textAlign = TextAlign.End)
                                CardText("10:07", 10.sp, Color(0xFF445975), textAlign = TextAlign.End)
                            }
                            Box(
                                Modifier
                                    .lineBubbleTail(Color(0xFF9DE694), tipRight = true)
                                    .background(Color(0xFF9DE694), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                            ) {
                                CardText(line.secondary, 15.sp, Color(0xFF111111), family = fontFamily, lineHeight = 22.sp)
                            }
                        }
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(Color.White)
                    .padding(start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CardGlyph(CardGlyphs.Add, 20.sp, Color(0xFF111111))
                Spacer(Modifier.width(16.dp))
                CardGlyph(CardGlyphs.Camera, 20.sp, Color(0xFF111111))
                Spacer(Modifier.width(16.dp))
                CardGlyph(CardGlyphs.Image, 20.sp, Color(0xFF111111))
                Spacer(Modifier.width(12.dp))
                Box(
                    Modifier
                        .weight(1f)
                        .height(36.dp)
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(18.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    CardText("CarpeLines", 14.sp, Color(0xFF999999))
                }
                CardGlyph(CardGlyphs.Send, 20.sp, Color(0xFF111111), Modifier.padding(start = 12.dp))
            }
        }
    }
}

@Composable
private fun Modifier.drawTopLine(color: Color): Modifier = this.drawBehind {
    drawLine(color, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(size.width, 0f), 1.dp.toPx())
}

@Composable
private fun Modifier.drawBehindBottomLine(color: Color): Modifier = this.drawBehind {
    drawLine(
        color,
        androidx.compose.ui.geometry.Offset(0f, size.height),
        androidx.compose.ui.geometry.Offset(size.width, size.height),
        1.dp.toPx(),
    )
}
