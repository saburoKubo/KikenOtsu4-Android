package com.kubosaburo.kikenotsu4.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kubosaburo.kikenotsu4.R
import com.kubosaburo.kikenotsu4.data.QuizLogStore
import com.kubosaburo.kikenotsu4.data.LearnStreakStore
import com.kubosaburo.kikenotsu4.ui.components.CharacterSpeechBubbleView
import kotlin.math.roundToInt

@Suppress("unused")
@Composable
fun ProgressScreen(
    quizLogStore: QuizLogStore,
    completedSectionCount: Int = 0,
    totalSectionCount: Int = 0,
    /** [com.kubosaburo.kikenotsu4.data.CurriculumProgressStore.loadLap]。2 以上で「○周目」を全体進捗に表示 */
    curriculumLap: Int = 1,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val context = LocalContext.current
    var stats by remember { mutableStateOf(quizLogStore.getStats()) }

    // 画面を開いたタイミングで最新化（超シンプル）
    LaunchedEffect(Unit) {
        stats = quizLogStore.getStats()
    }
    val overallProgressValue = if (totalSectionCount > 0) {
        (completedSectionCount.toFloat() / totalSectionCount.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val overallProgressPercent = (overallProgressValue * 100f).roundToInt()

    val streakDays = LearnStreakStore.getStreakDays(context)

    val progressValue = if (stats.totalAnswered == 0) 0f
    else (stats.totalCorrect.toFloat() / stats.totalAnswered.toFloat()).coerceIn(0f, 1f)
    val accuracy = (progressValue * 100f).roundToInt()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            // Scaffold の innerPadding に TopAppBar 下辺が含まれるため、追加分は控えめに
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 120.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            CharacterSpeechBubbleView(
                characterImage1 = R.drawable.pico_normal,
                text = when {
                    stats.totalAnswered == 0 -> "まずは1問解いてみよう。小さな一歩が進捗になる。"
                    accuracy >= 80 -> "いいペースです。この調子でどんどん定着させよう。"
                    accuracy >= 60 -> "着実に進んでいます。間違えた問題を見直すとさらに伸びます。"
                    else -> "ここから伸びます。復習を重ねて少しずつ正答率を上げていこう。"
                },
                characterImage2 = R.drawable.pico_happy,
                durationMillis = 2000L,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "全体の進捗",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (curriculumLap >= 2) {
                            Text(
                                text = "${curriculumLap}周目",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { overallProgressValue },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$completedSectionCount / $totalSectionCount セクション完了",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "$overallProgressPercent%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = buildString {
                            append("章全体の完了セクション数から進捗を表示しています。")
                            if (curriculumLap >= 2) {
                                append(" カリキュラムを最後まで終えてからの再開は「${curriculumLap}周目」として表示します。")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "連続学習",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${streakDays}日継続中",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = when {
                                streakDays >= 7 -> "いいペース！"
                                streakDays >= 3 -> "継続できてる！"
                                streakDays >= 1 -> "この調子！"
                                else -> "まずは1日！"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = if (streakDays == 0) {
                            "今日1回学習すると、連続学習が1日になります。"
                        } else {
                            "1日1回でもOK。継続がいちばん強い。"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
