
package com.kubosaburo.kikenotsu4.ui.screens

import com.kubosaburo.kikenotsu4.R

import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.kubosaburo.kikenotsu4.data.MockTestDefinition
import com.kubosaburo.kikenotsu4.data.MockTestLoader

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.kubosaburo.kikenotsu4.ui.components.CharacterSpeechBubbleView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MockTestHomeScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onStartTrial: () -> Unit,
    onStartNormalMock: (() -> Unit)? = null,
    isPro: Boolean = false
) {
    val context = LocalContext.current
    val payload = remember {
        runCatching { MockTestLoader.loadMockTests(context) }.getOrNull()
    }
    val trialTest = remember(payload) {
        payload?.mockTests?.firstOrNull { it.id.trim().lowercase() == "mock_trial_fixed" }
    }
    val randomTest = remember(payload) {
        payload?.mockTests?.firstOrNull { it.id.trim().lowercase() == "mock_random" }
    }

    fun sectionSummary(test: MockTestDefinition?): String {
        if (test == null) return ""
        val law = test.countFor("法令")
        val physics = test.countFor("物理化学")
        val nature = test.countFor("性質・消火")
        val total = law + physics + nature
        return "法令${law}問・物理化学${physics}問・性質・消火${nature}問（計${total}問）"
    }

    fun descriptionOrFallback(test: MockTestDefinition?, fallback: String): String {
        return test?.description?.takeIf { it.isNotBlank() } ?: fallback
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CharacterSpeechBubbleView(
            characterImage1 = R.drawable.nico_normal,
            text = if (isPro) {
                "今日は実戦モードでいこう！\n毎回ちがう問題で、本番感覚をしっかり鍛えよう✨"
            } else {
                "まずはお試し模試で力だめし！\n本番形式で、今の実力を気軽にチェックしよう✨"
            },
            characterImage2 = R.drawable.nico_normal_smile,
            durationMillis = 2000L,
            modifier = Modifier.fillMaxWidth()
        )

        if (!isPro) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = trialTest?.title ?: "お試し模試（無料）",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = buildString {
                            append(
                                descriptionOrFallback(
                                    trialTest,
                                    "固定問題で模擬テストを体験できる枠です。まずはここから始められるようにします。"
                                )
                            )
                            val summary = sectionSummary(trialTest)
                            if (summary.isNotBlank()) {
                                append("\n")
                                append(summary)
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = onStartTrial,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("お試し模試を開始")
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isPro) {
                        randomTest?.title ?: "ランダム模試"
                    } else {
                        randomTest?.title ?: "通常模試（今後追加）"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = buildString {
                        append(
                            descriptionOrFallback(
                                randomTest,
                                "ランダム出題・結果保存・合格判定はこのあと順番に入れていきます。"
                            )
                        )
                        val summary = sectionSummary(randomTest)
                        if (summary.isNotBlank()) {
                            append("\n")
                            append(summary)
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = { onStartNormalMock?.invoke() },
                    enabled = isPro && onStartNormalMock != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        if (isPro) {
                            "ランダム模試を開始"
                        } else {
                            "通常模試は準備中"
                        }
                    )
                }
            }
        }
        
    }
}