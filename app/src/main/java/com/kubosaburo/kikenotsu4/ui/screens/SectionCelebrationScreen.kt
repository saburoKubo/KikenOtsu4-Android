@file:Suppress("unused")

package com.kubosaburo.kikenotsu4.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import com.kubosaburo.kikenotsu4.R
import com.kubosaburo.kikenotsu4.data.LearningEffectSound
import kotlinx.coroutines.delay
import kotlin.random.Random

/** 以前の tween 1 周分と同じ平均速度に合わせる（無限 Restart で time が 0 に戻ると見た目が切り替わるのを避けるため、経過秒は途切れさせない） */
private const val ConfettiCycleSeconds = 2.8f

private const val ConfettiFullVisibleMillis = 5_200L
private const val ConfettiFadeOutMillis = 2_800

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
    var confettiFadeOut by remember { mutableStateOf(false) }
    var hasAppeared by remember { mutableStateOf(false) }

    val confettiAlpha by animateFloatAsState(
        targetValue = if (confettiFadeOut) 0f else 1f,
        animationSpec = tween(
            durationMillis = ConfettiFadeOutMillis,
            easing = LinearEasing
        ),
        label = "confettiAlpha"
    )

    // onAppear 相当
    LaunchedEffect(Unit) {
        if (hasAppeared) return@LaunchedEffect
        hasAppeared = true

        // 拍手
        LearningEffectSound.playOneShot(context, applauseResId)

        // 紙吹雪
        showConfetti = true

        // ✅ curriculum のときだけ完了確定（呼び出し側で Progress 更新する）
        if (isCurriculum) {
            onCompleteSectionIfNeeded?.invoke()
        }

        // しばらく通常表示のあと、ゆっくり透明化（落下は続けたまま）してからレイヤーを外す
        delay(ConfettiFullVisibleMillis)
        confettiFadeOut = true
        delay(ConfettiFadeOutMillis.toLong())
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
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

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

        }

        if (showConfetti) {
            ConfettiOverlay(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(confettiAlpha)
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
            painter = painterResource(id = R.drawable.nicos_wasshoi1),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .alpha(1f - phase),
            contentScale = ContentScale.Fit
        )
        Image(
            painter = painterResource(id = R.drawable.nicos_wasshoi2),
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
    intensity: Float = 1.45f,
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

    // 粒数（多め）。端末負荷は Canvas の drawRect のみなので上限はやや高めにしてよい
    val particleCount = (118 * intensity).coerceIn(48f, 260f).toInt()
    val particles = remember {
        List(particleCount) {
            ConfettiParticle(
                x = Random.nextFloat(),
                y = -Random.nextFloat(),
                // 小さいほど縦方向の移動がゆるやか（アニメ周期と合わせて調整）
                speed = 0.12f + Random.nextFloat() * 0.42f,
                // 画面ピクセル。以前は 16〜34 程度 → ひとまわり大きく（約 28〜62）
                size = 28f + Random.nextFloat() * 34f,
                color = colors.random(),
                // 粒ごとに回転速度・向き・初期角をずらす（一枚一枚バラバラに回る）
                rotationDegPerSec = (if (Random.nextBoolean()) 1f else -1f) *
                    (90f + Random.nextFloat() * 270f),
                rotationPhaseDeg = Random.nextFloat() * 360f,
            )
        }
    }

    var elapsedSec by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var startNs = 0L
        while (true) {
            withFrameNanos { frameNs ->
                if (startNs == 0L) startNs = frameNs
                elapsedSec = (frameNs - startNs) / 1_000_000_000f
            }
        }
    }

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        for ((idx, p) in particles.withIndex()) {
            // 0→1→0 の無限アニメは毎周「パッ」と切り替わるので、単調増加の経過秒で位相を進める
            val yy = ((p.y + elapsedSec * p.speed / ConfettiCycleSeconds + idx * 0.02f) % 1.2f) * h
            val xx = p.x * w +
                kotlin.math.sin((elapsedSec * 3.2f / ConfettiCycleSeconds + idx) * 0.7f) * (10f + p.size)

            val rectW = p.size
            val rectH = p.size * 0.7f
            val pivot = Offset(xx + rectW / 2f, yy + rectH / 2f)
            val rotationDeg = p.rotationPhaseDeg + elapsedSec * p.rotationDegPerSec

            rotate(degrees = rotationDeg, pivot = pivot) {
                drawRect(
                    color = p.color,
                    topLeft = Offset(xx, yy),
                    size = androidx.compose.ui.geometry.Size(rectW, rectH)
                )
            }
        }
    }
}

private data class ConfettiParticle(
    val x: Float,
    val y: Float,
    val speed: Float,
    val size: Float,
    val color: Color,
    /** 1 秒あたりの回転角（度）。正負で回転方向。 */
    val rotationDegPerSec: Float,
    /** 開始時の位相（度）。 */
    val rotationPhaseDeg: Float,
)

