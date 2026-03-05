@file:Suppress("unused")

package com.kubosaburo.kikenotsu4.ui.screens

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import com.kubosaburo.kikenotsu4.R
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * iOS の SectionCelebrationView 相当。
 * - テキストの問題を完了したタイミングで表示
 * - キャラクター2コマアニメ
 * - メッセージ表示
 * - 紙吹雪（軽量）
 * - 拍手SE
 *
 * Navigation は呼び出し側に委譲する（onTapNext）。
 */
@Composable
fun SectionCelebrationScreen(
    message: String,
    isCurriculum: Boolean,
    modifier: Modifier = Modifier,
    // curriculum のときだけ完了確定したい場合に呼び出し側で渡す
    onCompleteSectionIfNeeded: (() -> Unit)? = null,
    // 次へ/一覧へ戻る の遷移は呼び出し側で実装
    onTapNext: () -> Unit,
    // SE
    applauseResId: Int = R.raw.applause,
) {
    val context = LocalContext.current

    var showConfetti by remember { mutableStateOf(false) }
    var hasAppeared by remember { mutableStateOf(false) }

    // onAppear 相当
    LaunchedEffect(Unit) {
        if (hasAppeared) return@LaunchedEffect
        hasAppeared = true

        // 拍手
        playOneShot(context, applauseResId)

        // 紙吹雪
        showConfetti = true

        // ✅ curriculum のときだけ完了確定（呼び出し側で Progress 更新する）
        if (isCurriculum) {
            onCompleteSectionIfNeeded?.invoke()
        }

        // 3秒程度で止める
        delay(3_000)
        showConfetti = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(18.dp))

            CelebrationCharacter(size = 220.dp)

            Text(
                text = message,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = onTapNext,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8A1E)),
                contentPadding = PaddingValues(horizontal = 36.dp, vertical = 14.dp)
            ) {
                Text(
                    text = if (isCurriculum) "次へ ▶" else "一覧へ戻る 📚",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(Modifier.height(10.dp))
        }

        if (showConfetti) {
            ConfettiOverlay(
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun CelebrationCharacter(size: Dp) {
    // 2コマアニメ（opacityで切り替え）
    val t = rememberInfiniteTransition(label = "celebrate")
    val phase by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase"
    )

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.nicos_final1),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .alpha(1f - phase),
            contentScale = ContentScale.Fit
        )
        Image(
            painter = painterResource(id = R.drawable.nicos_final2),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .alpha(phase),
            contentScale = ContentScale.Fit
        )
    }
}

/** 軽量な紙吹雪オーバーレイ（色付きの小さい矩形を上から降らせる） */
@Composable
private fun ConfettiOverlay(
    modifier: Modifier = Modifier,
    intensity: Float = 0.8f,
) {
    // NOTE: 本格的な物理はやらず、見た目優先の軽量実装
    val colors = remember {
        listOf(
            Color(0xFFFF4DA6),
            Color(0xFFFF8A1E),
            Color(0xFF22C55E),
            Color(0xFF3B78C8),
            Color(0xFFA855F7)
        )
    }

    val particleCount = (40 * intensity).coerceIn(12f, 80f).toInt()
    val particles = remember {
        List(particleCount) {
            ConfettiParticle(
                x = Random.nextFloat(),
                y = -Random.nextFloat(),
                speed = 0.25f + Random.nextFloat() * 0.75f,
                size = 6f + Random.nextFloat() * 8f,
                color = colors.random()
            )
        }
    }

    val t = rememberInfiniteTransition(label = "confetti")
    val time by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        for ((idx, p) in particles.withIndex()) {
            // time を使って下方向に流す（粒ごとのスピード/開始位置）
            val yy = ((p.y + time * p.speed + idx * 0.02f) % 1.2f) * h
            val xx = p.x * w + kotlin.math.sin((time * 6f + idx) * 0.7f) * (10f + p.size)

            drawRect(
                color = p.color,
                topLeft = androidx.compose.ui.geometry.Offset(xx, yy),
                size = androidx.compose.ui.geometry.Size(p.size, p.size * 0.6f)
            )
        }
    }
}

private data class ConfettiParticle(
    val x: Float,
    val y: Float,
    val speed: Float,
    val size: Float,
    val color: Color,
)

private fun playOneShot(context: Context, resId: Int) {
    if (resId == 0) return
    runCatching {
        val mp = MediaPlayer.create(context, resId)
        if (mp != null) {
            mp.setOnCompletionListener { it.release() }
            mp.start()
        }
    }
}
