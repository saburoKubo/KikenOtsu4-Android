package com.kubosaburo.kikenotsu4.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kubosaburo.kikenotsu4.R
import com.kubosaburo.kikenotsu4.ui.components.CharacterSpeechBubbleView
import org.json.JSONObject
import kotlin.random.Random

/**
 * iOS の ReviewIntroView 相当：
 * 「今日は復習が◯問あります」→「学習を続ける」 で復習(Quiz)へ。
 *
 * NOTE:
 * - 画面上部の TopBar は AppRoot 側の Scaffold が担当する想定なので、この画面は中身のみ描画します。
 * - 吹き出し文言は assets の `praise_messages.json` から `startingPraise_messages` を読み、ランダム表示します。
 */
@Composable
fun ReviewIntroScreen(
    contentPadding: PaddingValues,
    dueCount: Int,
    onStartReview: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onLater: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // startingPraise_messages を読み、毎回同じにならないように表示文言を決める
    var phrase by remember { mutableStateOf("復習していこう") }
    LaunchedEffect(dueCount) {
        val phrases = loadStartingPraiseMessages(context)
        phrase = if (phrases.isNotEmpty()) {
            phrases[Random.nextInt(phrases.size)]
        } else {
            // フォールバック
            "忘れかけたところを、いま確認すると一番伸びるよ！"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.Center
    ) {

        // タイトル
        Text(
            text = "本日の復習",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "今日の復習：${dueCount}問",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(18.dp))

        // キャラ吹き出し（中央寄せ）
        CharacterSpeechBubbleView(
            characterImage1 = R.drawable.nicosme_normal,
            characterImage2 = R.drawable.nicosme_openmouth,
            durationMillis = 2200,
            text = phrase,
            modifier = Modifier.fillMaxWidth(),
            characterSize = 120.dp,
            bubbleBorderColor = MaterialTheme.colorScheme.outline
        )

        Spacer(Modifier.height(18.dp))

        // 説明カード（iOS の「Proにすると〜」等は後で拡張できる）
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(14.dp)) {
                Text(
                    text = "忘れかけのタイミングに合わせて復習問題が出ます。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "今のうちに確認して、定着させよう！",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        // CTA
        Button(
            onClick = onStartReview,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("復習問題を解く ▶︎", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(10.dp))

//        OutlinedButton(
//            onClick = onLater,
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(48.dp),
//            shape = MaterialTheme.shapes.large
//        ) {
//            Text("あとで")
//        }
//
//        Spacer(Modifier.height(10.dp))
    }
}

private fun loadStartingPraiseMessages(context: Context): List<String> {
    return runCatching {
        val json = context.assets.open("praise_messages.json").bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        val arr = root.optJSONArray("startingPraise_messages") ?: return@runCatching emptyList<String>()

        buildList {
            for (i in 0 until arr.length()) {
                val any = arr.opt(i) ?: continue

                val s = when (any) {
                    is String -> any
                    is JSONObject -> {
                        // Support multiple possible field names
                        any.optString("text", "").ifBlank {
                            any.optString("text2", "").ifBlank {
                                any.optString("message", "")
                            }
                        }
                    }
                    else -> any.toString()
                }.trim()

                if (s.isNotEmpty()) add(s)
            }
        }
    }.getOrElse { emptyList() }
}
