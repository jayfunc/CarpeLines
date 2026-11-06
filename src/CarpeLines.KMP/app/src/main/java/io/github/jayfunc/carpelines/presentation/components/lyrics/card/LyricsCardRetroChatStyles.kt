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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** `LyricsCardRetroQQStyle.xaml`: the 2005 QQ conversation window. */
@Composable
internal fun RetroQQCard(
    data: LyricsCardData,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier,
) {
    CardSurface(Color(0xFF0055E4), modifier, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(start = 2.dp, end = 2.dp, bottom = 2.dp)
                .background(Color(0xFFDFDFDF)),
        ) {
            Box(Modifier.fillMaxWidth().height(2.dp).background(Color(0xFF338BFA)))
            // Title bar
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(Color(0xFF0269FC))
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Box(Modifier.size(16.dp).clip(RoundedCornerShape(1.dp))) {
                    CardCover(data.cover, Modifier.matchParentSize(), ContentScale.Crop)
                }
                Spacer(Modifier.width(4.dp))
                CardText(
                    text = data.artist,
                    size = 12.sp,
                    color = Color.White,
                    weight = FontWeight.Bold,
                    family = fontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 3.dp).weight(1f, fill = false),
                )
                CardText(
                    text = "-发送消息",
                    size = 12.sp,
                    color = Color.White,
                    weight = FontWeight.Bold,
                    family = fontFamily,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    MiniWindowButton("0", Color(0xFF20FFFFFF), Color.White, Color.White)
                    MiniWindowButton("1", Color(0xFF225FEB), Color(0xFF8A9DE7), Color(0xFF5799FC))
                    MiniWindowButton("r", Color(0xFFD54739), Color.White, Color.White)
                }
            }
            // Identity row
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .padding(start = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(32.dp).background(Color.White).border(1.5.dp, Color(0xFF6C6D6D))) {
                    CardCover(data.cover, Modifier.matchParentSize())
                }
                Column(Modifier.padding(start = 8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row {
                        CardText("QQ#: ", 12.sp, Color.Black, family = fontFamily)
                        CardText("3344520", 12.sp, Color.Black, family = fontFamily)
                    }
                    Row {
                        CardText("昵称: ", 12.sp, Color.Black, family = fontFamily)
                        CardText(
                            data.title,
                            12.sp,
                            Color.Black,
                            family = fontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            // Conversation
            Row(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(3.dp),
            ) {
                Column(Modifier.weight(1f).fillMaxHeight().padding(end = 3.dp)) {
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(Color.White)
                            .border(
                                1.dp,
                                Color(0xFFABABAB),
                                RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .clipToBounds(),
                    ) {
                        data.lines.forEach { line ->
                            Column(Modifier.padding(bottom = 12.dp)) {
                                Row {
                                    CardText(
                                        data.artist,
                                        12.sp,
                                        Color(0xFF0000FF),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.width(200.dp),
                                    )
                                    CardText(" 13:14:20", 12.sp, Color(0xFF0000FF))
                                }
                                CardText(
                                    line.text,
                                    13.sp,
                                    Color.Black,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                                if (!line.secondary.isNullOrBlank()) {
                                    Row(Modifier.padding(top = 8.dp)) {
                                        CardText("我", 12.sp, Color(0xFF008000))
                                        CardText(" 20:13:14", 12.sp, Color(0xFF008000))
                                    }
                                    CardText(
                                        line.secondary,
                                        13.sp,
                                        Color.Black,
                                        lineHeight = 18.sp,
                                        modifier = Modifier.padding(start = 8.dp),
                                    )
                                }
                            }
                        }
                    }
                    // Formatting toolbar
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                            .background(Color(0xFF6281D0))
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CardText("字体", 12.sp, Color.White, family = fontFamily)
                        CardText("表情", 12.sp, Color.White, family = fontFamily)
                        CardText("手机短信", 12.sp, Color.White, family = fontFamily)
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(Color.White)
                            .border(1.dp, Color(0xFFABABAB)),
                    ) {
                        CardText(
                            text = "CarpeLines...",
                            size = 12.sp,
                            color = Color(0xFFCCCCCC),
                            family = fontFamily,
                            modifier = Modifier.padding(4.dp),
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(top = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            QQButton("聊天记录")
                            QQButton("消息模式")
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            QQButton("取消发送")
                            QQButton("送讯息", Color(0xFFC0D2F1), horizontalPadding = 14.dp)
                        }
                    }
                }
                // Side panel
                Column(Modifier.width(140.dp)) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(Color(0xFF6281D0))
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CardText("QQ秀", 11.sp, Color.White, family = fontFamily)
                        Spacer(Modifier.weight(1f))
                        Box(
                            Modifier.size(12.dp).clip(CircleShape).background(Color(0xFF767DD3))
                                .border(1.dp, Color(0xFF5C7CC3), CircleShape),
                        )
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .background(Color.White)
                            .border(1.dp, Color(0xFF6281D0)),
                    ) {
                        CardCover(data.cover, Modifier.matchParentSize(), ContentScale.Fit)
                    }
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color(0xFF6281D0))
                            .padding(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        CardText("您可以...", 11.sp, Color.White, family = fontFamily)
                        CardText("VQQ 视频聊天", 11.sp, Color.White, family = fontFamily)
                        CardText("语音聊天", 11.sp, Color.White, family = fontFamily)
                        CardText("申请QQ号码", 11.sp, Color(0xFFFFFF00), weight = FontWeight.Bold, family = fontFamily)
                        CardText("发送文件", 11.sp, Color.White, family = fontFamily)
                        CardText("其他...", 11.sp, Color.White, family = fontFamily)
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniWindowButton(text: String, background: Color, contentColor: Color, borderColor: Color) {
    Box(
        Modifier
            .size(16.dp)
            .background(background, RoundedCornerShape(2.dp))
            .border(1.dp, borderColor, RoundedCornerShape(2.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (text == "0") {
            Box(Modifier.width(6.dp).height(1.dp).background(contentColor))
        } else if (text == "1") {
            Box(Modifier.size(6.dp).border(1.dp, contentColor))
        } else {
            CardText(text, 10.sp, contentColor, weight = FontWeight.Bold)
        }
    }
}

@Composable
private fun QQButton(
    label: String,
    background: Color = Color(0xFFEBEEF6),
    horizontalPadding: androidx.compose.ui.unit.Dp = 6.dp,
) {
    Box(
        Modifier
            .background(background, RoundedCornerShape(2.dp))
            .border(1.dp, Color(0xFFCED3D9), RoundedCornerShape(2.dp))
            .padding(horizontal = horizontalPadding, vertical = 2.dp),
    ) {
        CardText(label, 12.sp, Color.Black, maxLines = 1)
    }
}

/** `LyricsCardRetroMSNStyle.xaml`: Windows Live Messenger conversation chrome. */
@Composable
internal fun RetroMSNCard(
    data: LyricsCardData,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier,
) {
    CardSurface(Color(0xFFEBF3FA), modifier, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)) {
        Column(Modifier.fillMaxSize()) {
            // Title bar
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .background(
                        Brush.verticalGradient(
                            0f to Color(0xFF0058EE),
                            0.1f to Color(0xFF3B8BF6),
                            1f to Color(0xFF0050DC),
                        ),
                    )
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CardGlyph(CardGlyphs.Person, 14.sp, Color.White)
                Spacer(Modifier.width(6.dp))
                CardText(
                    data.artist,
                    12.sp,
                    Color.White,
                    weight = FontWeight.Bold,
                    family = fontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(200.dp),
                )
                CardText(" - Conversation", 12.sp, Color(0xFFAACCFF), family = fontFamily)
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    MsnWindowButton("-", Color(0xFF3875D7))
                    Box(
                        Modifier
                            .size(20.dp)
                            .background(Color(0xFF3875D7), RoundedCornerShape(3.dp))
                            .border(1.dp, Color.White, RoundedCornerShape(3.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.size(12.dp).border(1.dp, Color.White))
                    }
                    MsnWindowButton("×", Color(0xFFE64028))
                }
            }
            // Menu bar
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(Color(0xFFEBEADB))
                    .drawBehind {
                        drawLine(Color(0xFFACA899), Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
                    }
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                listOf("File", "Edit", "Actions", "Tools", "Help").forEach {
                    CardText(it, 11.sp, Color.Black, family = fontFamily)
                }
            }
            // To row
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(Color(0xFFEBF3FA))
                    .drawBehind {
                        drawLine(Color(0xFFACA899), Offset(0f, 0f), Offset(0f, size.height), 1.dp.toPx())
                        drawLine(Color(0xFFACA899), Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.padding(start = 10.dp).size(32.dp).border(1.dp, Color(0xFF8CA0B4))) {
                    CardCover(data.cover, Modifier.matchParentSize(), ContentScale.Crop)
                }
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    CardText("To: CarpeLines User", 11.sp, Color(0xFF444444), family = fontFamily)
                    CardText("<Type a personal message>", 11.sp, Color(0xFF888888), family = fontFamily, italic = true)
                }
                Row(
                    Modifier.padding(end = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CardGlyph(CardGlyphs.Video, 14.sp, Color(0xFF444444))
                    CardGlyph(CardGlyphs.Contact, 14.sp, Color(0xFF444444))
                }
            }
            // Chat
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFFEBF3FA))
                    .padding(start = 4.dp, end = 4.dp, bottom = 4.dp),
            ) {
                Box(Modifier.fillMaxSize().background(Color.White).border(1.dp, Color(0xFFACA899))) {
                    CardCover(
                        data.cover,
                        Modifier.matchParentSize(),
                        ContentScale.Crop,
                        alpha = 0.15f,
                    )
                    Row(Modifier.fillMaxSize()) {
                        Column(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(10.dp)
                                .clipToBounds(),
                        ) {
                            data.lines.forEach { line ->
                                Column(Modifier.padding(bottom = 12.dp)) {
                                    Row {
                                        CardText(
                                            data.artist,
                                            11.sp,
                                            Color(0xFF808080),
                                            weight = FontWeight.Bold,
                                            family = fontFamily,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.width(220.dp),
                                        )
                                        CardText(" says:", 11.sp, Color(0xFF808080), family = fontFamily)
                                    }
                                    Column(Modifier.padding(start = 10.dp)) {
                                        CardText(line.text, 14.sp, Color(0xFF000080), family = fontFamily)
                                        if (!line.secondary.isNullOrBlank()) {
                                            CardText(
                                                line.secondary,
                                                12.sp,
                                                Color(0xFF666666),
                                                family = fontFamily,
                                                modifier = Modifier.padding(top = 2.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Box(Modifier.width(100.dp).padding(5.dp)) {
                            Box(
                                Modifier
                                    .size(80.dp)
                                    .background(Color.White)
                                    .border(1.dp, Color(0xFF8CA0B4)),
                            ) {
                                CardCover(data.cover, Modifier.matchParentSize(), ContentScale.Crop)
                            }
                        }
                    }
                }
            }
            // Composer
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEBF3FA))
                    .padding(start = 4.dp, end = 4.dp, bottom = 4.dp),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFEBF3FA))
                        .border(1.dp, Color(0xFFACA899))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CardText("A", 14.sp, Color.Black, weight = FontWeight.Bold, family = fontFamily)
                    CardGlyph(CardGlyphs.Emoji, 12.sp, Color.Black)
                    CardText("Backgrounds", 10.sp, Color.Black, family = fontFamily)
                    CardText("Winks", 10.sp, Color.Black, family = fontFamily)
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(Color.White)
                        .border(1.dp, Color(0xFFACA899)),
                ) {
                    CardText(
                        "CarpeLines...",
                        12.sp,
                        Color(0xFFCCCCCC),
                        family = fontFamily,
                        modifier = Modifier.padding(5.dp),
                    )
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .background(Color(0xFFF0F0F0))
                            .border(1.dp, Color(0xFF888888))
                            .padding(horizontal = 15.dp, vertical = 4.dp),
                    ) {
                        CardText("Send", 11.sp, Color.Black, family = fontFamily)
                    }
                }
            }
            // Status bar
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(Color(0xFFEBF3FA))
                    .border(1.dp, Color(0xFFACA899))
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                CardText(
                    "Last message received at 12:00 PM.",
                    10.sp,
                    Color(0xFF666666),
                    family = fontFamily,
                )
            }
        }
    }
}

@Composable
private fun MsnWindowButton(text: String, background: Color) {
    Box(
        Modifier
            .size(20.dp)
            .background(background, RoundedCornerShape(3.dp))
            .border(1.dp, Color.White, RoundedCornerShape(3.dp)),
        contentAlignment = Alignment.Center,
    ) {
        CardText(text, if (text == "×") 12.sp else 14.sp, Color.White, weight = FontWeight.Bold)
    }
}
