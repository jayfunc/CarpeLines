package io.github.jayfunc.carpelines.presentation.components.lyrics.card

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** `LyricsCardJournalStyle.xaml`: ring-bound notebook page with a red margin rule. */
@Composable
internal fun JournalCard(
    data: LyricsCardData,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier,
) {
    CardSurface(Color(0xFFF5F5F5), modifier, RoundedCornerShape(8.dp)) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(40.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFF4EBD9))
                .border(1.dp, Color(0xFFE0D5C1), RoundedCornerShape(4.dp)),
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(30.dp),
            ) {
                repeat(9) {
                    Box(
                        Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF5F5F5))
                            .border(1.dp, Color(0xFFE0D5C1), CircleShape),
                    )
                }
            }
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = 45.dp)
                    .width(1.5.dp)
                    .fillMaxHeight()
                    .background(Color(0xFFD9A8A8).copy(alpha = 0.6f)),
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(start = 65.dp, top = 36.dp, end = 36.dp, bottom = 36.dp),
            ) {
                Column(Modifier.padding(bottom = 24.dp)) {
                    CardText(
                        text = data.title,
                        size = 24.sp,
                        color = Color(0xFF3C4858),
                        weight = FontWeight.SemiBold,
                        family = fontFamily,
                    )
                    CardText(
                        text = data.artist,
                        size = 15.sp,
                        color = Color(0xFF6C7A89),
                        family = fontFamily,
                        modifier = Modifier.padding(top = 8.dp),
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
                        textSize = 16.sp,
                        textColor = Color(0xFF3C4858),
                        secondarySize = 14.sp,
                        secondaryColor = Color(0xFF8B96A5),
                        spacing = 16.dp,
                        secondarySpacing = 4.dp,
                        lineHeight = 26.sp,
                        secondaryLineHeight = 22.sp,
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End) {
                    CardWordmark(
                        color = Color(0xFF3C4858),
                        size = 10.sp,
                        family = fontFamily,
                        alpha = 0.5f,
                        italic = true,
                    )
                }
            }
        }
    }
}

/** `LyricsCardStickyNoteStyle.xaml`: yellow note, bent corner and tape strip. */
@Composable
internal fun StickyNoteCard(
    data: LyricsCardData,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(40.dp)
                .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp, bottomEnd = 16.dp, bottomStart = 2.dp))
                .background(Color(0xFFFFF4A3))
                .border(
                    1.dp,
                    Color(0xFFEAE096),
                    RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp, bottomEnd = 16.dp, bottomStart = 2.dp),
                ),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(start = 32.dp, top = 40.dp, end = 32.dp, bottom = 32.dp),
            ) {
                Column(Modifier.padding(bottom = 24.dp)) {
                    CardText(
                        text = data.title,
                        size = 26.sp,
                        color = Color(0xFF2C3440),
                        weight = FontWeight.SemiBold,
                        family = fontFamily,
                    )
                    CardText(
                        text = data.artist,
                        size = 15.sp,
                        color = Color(0xFF5C6673),
                        family = fontFamily,
                        modifier = Modifier.padding(top = 8.dp),
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
                        textColor = Color(0xFF2C3440),
                        secondarySize = 14.sp,
                        secondaryColor = Color(0xFF7A8491),
                        spacing = 16.dp,
                        secondarySpacing = 4.dp,
                        lineHeight = 28.sp,
                        secondaryLineHeight = 22.sp,
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End) {
                    CardWordmark(
                        color = Color(0xFF2C3440),
                        size = 10.sp,
                        family = fontFamily,
                        alpha = 0.5f,
                        italic = true,
                    )
                }
            }
        }
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 25.dp)
                .size(width = 100.dp, height = 25.dp)
                .rotate(-2f)
                .background(Color(0xFFD9D9D9).copy(alpha = 0.6f)),
        )
    }
}
