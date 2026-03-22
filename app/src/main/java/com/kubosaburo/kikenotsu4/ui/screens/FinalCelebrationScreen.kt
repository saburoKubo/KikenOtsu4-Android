package com.kubosaburo.kikenotsu4.ui.screens

import android.media.MediaPlayer
import com.kubosaburo.kikenotsu4.data.LearningEffectSettings
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kubosaburo.kikenotsu4.R
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * [lap] は [com.kubosaburo.kikenotsu4.data.CurriculumProgressStore.loadLap]。
 * 全完了時に先に周回が +1 されるため、初回完了直後は lap=2 → 初回文言、3 以上で「2周目も…」以降。
 */
fun finalCelebrationCopyForCurriculumLap(lap: Int): Pair<String, String> {
    val title = "全カリキュラム完了！"
    val message =
        if (lap <= 2) {
            "最後までやり切りました！本当におつかれさま！🎉"
        } else {
            "${lap - 1}周目もやり切りました。本当にお疲れ様"
        }
    return title to message
}

/**
 * Final celebration shown when the user completes ALL curriculum texts + quizzes.
 *
 * - Two-frame character animation (uses existing nicosme assets)
 * - Confetti overlay
 * - 最初のテキストから学び直す / ホームへ
 */
@Composable
fun FinalCelebrationScreen(
    contentPadding: PaddingValues,
    title: String = "全カリキュラム完了！",
    message: String = "最後までやり切りました！本当におつかれさま！🎉",
    /** カリキュラム先頭のテキストから再開（null のときはボタン非表示・プレビュー用） */
    onRestartFromFirst: (() -> Unit)? = null,
    onGoHome: () -> Unit,
) {
    val bgBrush = remember {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFFF8A3D), // orange
                Color(0xFFFF6FAE)  // pink
            )
        )
    }

    val context = LocalContext.current

    // MediaPlayer は「再生完了リスナーで release」か「onDispose で release」のどちらか一方だけにする。
    // 両方で release すると、音が終わったあとに「ホームへ戻る」で onDispose が既に解放済みインスタンスに触れてクラッシュする。
    DisposableEffect(Unit) {
        if (!LearningEffectSettings.isSoundEffectsEnabled(context)) {
            return@DisposableEffect onDispose { }
        }
        val vol = LearningEffectSettings.getVolume01(context)
        val mediaPlayer = MediaPlayer.create(context, R.raw.firework)
        mediaPlayer?.let { player ->
            player.setVolume(vol, vol)
            player.start()
        }

        onDispose {
            mediaPlayer?.run {
                runCatching {
                    if (isPlaying) stop()
                }
                runCatching {
                    release()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
            .padding(contentPadding)
    ) {

        // Confetti (always on)
        ConfettiOverlay(
            modifier = Modifier.fillMaxSize(),
            density = 120,
            pieceSize = 12.dp,
            fallDurationMs = 3200,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))

            TwoFrameCharacter(
                firstRes = R.drawable.nicos_final1,
                secondRes = R.drawable.nicos_final2,
                size = 220.dp,
                durationMillis = 900,
            )

            Spacer(Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)),
                shape = MaterialTheme.shapes.large
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF1A1A1A),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }

            Spacer(Modifier.height(18.dp))

            if (onRestartFromFirst != null) {
                Button(
                    onClick = onRestartFromFirst,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8A3D))
                ) {
                    Text("最初のテキストから学び直す ▶︎", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onGoHome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.9f))
                ) {
                    Text("ホームへ戻る", fontWeight = FontWeight.SemiBold)
                }
            } else {
                Button(
                    onClick = onGoHome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8A3D))
                ) {
                    Text("ホームへ戻る ▶︎", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = if (onRestartFromFirst != null) {
                    "最初から読み返すか、ホームで模擬試験・復習に進んでもOK！"
                } else {
                    "次は模擬試験や復習で、さらに仕上げていこう！"
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.92f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun TwoFrameCharacter(
    firstRes: Int,
    secondRes: Int,
    size: Dp,
    durationMillis: Int,
) {
    var showSecond by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(durationMillis.toLong())
            showSecond = !showSecond
        }
    }

    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Image(
            painter = painterResource(firstRes),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (showSecond) 0f else 1f)
        )
        androidx.compose.foundation.Image(
            painter = painterResource(secondRes),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (showSecond) 1f else 0f)
        )
    }
}

@Composable
private fun ConfettiOverlay(
    modifier: Modifier = Modifier,
    density: Int = 80,
    pieceSize: Dp = 10.dp,
    fallDurationMs: Int = 3000,
) {
    // Simple confetti: random rectangles falling top->bottom, looping.
    val infinite = rememberInfiniteTransition(label = "confetti")
    val t by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = fallDurationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "t"
    )

    val px = with(LocalDensity.current) { pieceSize.toPx() }

    // Stable random seeds
    val seeds = remember { List(density) { Random.nextInt() } }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        for (i in 0 until density) {
            val r = Random(seeds[i])

            // x fixed per piece
            val x = r.nextFloat() * w

            // y loops with time
            val speed = 0.6f + r.nextFloat() * 1.6f
            val y = ((t * speed + r.nextFloat()) % 1f) * (h + px) - px

            val rot = (t * 360f * (0.6f + r.nextFloat() * 1.4f) + r.nextFloat() * 360f) % 360f

            val c = when (r.nextInt(6)) {
                0 -> Color(0xFFFF7AA2) // soft pink
                1 -> Color(0xFFFFC857) // yellow
                2 -> Color(0xFF5DD39E) // green
                3 -> Color(0xFF4EA8DE) // blue
                4 -> Color(0xFFB388FF) // purple
                else -> Color(0xFFFF9F1C) // orange
            }

            rotate(rot, pivot = androidx.compose.ui.geometry.Offset(x, y)) {
                drawRect(
                    color = c,
                    topLeft = androidx.compose.ui.geometry.Offset(x, y),
                    size = androidx.compose.ui.geometry.Size(px, px * 0.6f)
                )
            }
        }
    }
}