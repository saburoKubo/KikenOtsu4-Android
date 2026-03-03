package com.kubosaburo.kikenotsu4.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * iOSの CharacterSpeechBubbleView 風のコンポーネント。
 * - 左にキャラクター画像
 * - 右に吹き出し（角丸＋しっぽ）
 */
@Composable
fun CharacterSpeechBubbleView(
    @DrawableRes characterImage1: Int,
    text: String,
    modifier: Modifier = Modifier,
    @DrawableRes characterImage2: Int? = null,
    durationMillis: Long = 1000L,
    characterSize: Dp = 120.dp,
    bubbleCornerRadius: Dp = 18.dp,
    bubblePadding: Dp = 14.dp,
    bubbleColor: Color = MaterialTheme.colorScheme.surface,
    bubbleBorderColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
    bubbleBorderWidth: Dp = 2.dp,
    tailWidth: Dp = 14.dp,
    tailHeight: Dp = 12.dp,
    tailOffsetFromTop: Dp = 28.dp,
    textStyle: TextStyle = MaterialTheme.typography.titleMedium,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    textWeight: FontWeight = FontWeight.Bold,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TwoFrameCharacter(
            firstImage = characterImage1,
            secondImage = characterImage2,
            durationMillis = durationMillis,
            size = characterSize,
        )

        BubbleWithTail(
            modifier = Modifier.weight(1f),
            cornerRadius = bubbleCornerRadius,
            padding = bubblePadding,
            fillColor = bubbleColor,
            borderColor = bubbleBorderColor,
            borderWidth = bubbleBorderWidth,
            tailWidth = tailWidth,
            tailHeight = tailHeight,
            tailOffsetFromTop = tailOffsetFromTop,
        ) {
            Text(
                text = text,
                style = textStyle,
                fontWeight = textWeight,
                color = textColor
            )
        }
    }
}

@Composable
private fun TwoFrameCharacter(
    @DrawableRes firstImage: Int,
    @DrawableRes secondImage: Int?,
    durationMillis: Long,
    size: Dp,
) {
    // 2コマ目が無い場合は静止画
    if (secondImage == null || durationMillis <= 0L) {
        Image(
            painter = painterResource(id = firstImage),
            contentDescription = "character",
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Fit
        )
        return
    }

    var showFirst by remember(firstImage, secondImage, durationMillis) { mutableStateOf(true) }

    // durationMillis で2枚を交互に切り替え
    LaunchedEffect(firstImage, secondImage, durationMillis) {
        val half = (durationMillis / 2L).coerceAtLeast(120L)
        while (true) {
            delay(half)
            showFirst = !showFirst
        }
    }

    val resId = if (showFirst) firstImage else secondImage

    Image(
        painter = painterResource(id = resId),
        contentDescription = "character",
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(16.dp)),
        contentScale = ContentScale.Fit
    )
}

@Composable
private fun BubbleWithTail(
    modifier: Modifier = Modifier,
    cornerRadius: Dp,
    padding: Dp,
    fillColor: Color,
    borderColor: Color,
    borderWidth: Dp,
    tailWidth: Dp,
    tailHeight: Dp,
    tailOffsetFromTop: Dp,
    content: @Composable () -> Unit
) {
    // しっぽ分の左余白
    val leftInset = tailWidth

    Box(
        modifier = modifier
    ) {
        // 背景（しっぽ含む）
        Box(
            modifier = Modifier
                .padding(start = leftInset)
                .clip(RoundedCornerShape(cornerRadius))
                .background(fillColor)
                .border(borderWidth, borderColor, RoundedCornerShape(cornerRadius))
                .padding(padding)
        ) {
            content()
        }

        // しっぽ（左側）
        Tail(
            modifier = Modifier
                .width(tailWidth)
                .height(tailHeight)
                .align(Alignment.TopStart)
                .padding(top = tailOffsetFromTop),
            fillColor = fillColor,
            borderColor = borderColor,
            borderWidth = borderWidth
        )
    }
}

@Composable
private fun Tail(
    modifier: Modifier,
    fillColor: Color,
    borderColor: Color,
    borderWidth: Dp
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        drawTail(fillColor, borderColor, borderWidth.toPx())
    }
}

private fun DrawScope.drawTail(
    fillColor: Color,
    borderColor: Color,
    borderWidthPx: Float
) {
    // 左から右へ向かう三角形（やや丸い印象にしたいのでPathを少し工夫）
    val w = size.width
    val h = size.height

    val path = Path().apply {
        // 左上
        moveTo(0f, h * 0.25f)
        // 右中央
        lineTo(w, h * 0.5f)
        // 左下
        lineTo(0f, h * 0.75f)
        close()
    }

    // 塗り
    drawPath(path = path, color = fillColor, style = Fill)

    // 枠（外周）
    if (borderWidthPx > 0f) {
        drawPath(path = path, color = borderColor, style = Stroke(width = borderWidthPx))
    }
}

@Preview(showBackground = true)
@Composable
private fun CharacterSpeechBubbleViewPreview() {
    Surface {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CharacterSpeechBubbleView(
                characterImage1 = android.R.drawable.sym_def_app_icon,
                characterImage2 = android.R.drawable.sym_def_app_icon,
                durationMillis = 800L,
                text = "学ぼうとする\n姿勢が素晴らしい！\n拍手が止まらない！",
                bubbleColor = MaterialTheme.colorScheme.surface,
            )
        }
    }
}
