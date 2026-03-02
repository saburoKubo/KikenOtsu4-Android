package com.kubosaburo.kikenotsu4.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kubosaburo.kikenotsu4.data.QuizLogStore
import kotlin.math.roundToInt

@Composable
fun ProgressScreen(
    quizLogStore: QuizLogStore,
    onBack: () -> Unit
) {
    val statsState = remember { mutableStateOf(quizLogStore.getStats()) }

    // 画面を開いたタイミングで最新化（超シンプル）
    LaunchedEffect(Unit) {
        statsState.value = quizLogStore.getStats()
    }

    val stats = statsState.value
    val accuracy = if (stats.totalAnswered == 0) 0
    else ((stats.totalCorrect.toFloat() / stats.totalAnswered.toFloat()) * 100f).roundToInt()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // タイトル行（iOS寄せのシンプル）
        Column(Modifier.fillMaxWidth()) {
            Text("進捗", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("学習の記録（端末内）", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatRow(label = "解いた問題数", value = "${stats.totalAnswered}問")
                StatRow(label = "正解数", value = "${stats.totalCorrect}問")
                StatRow(label = "正答率", value = "${accuracy}%")

                LinearProgressIndicator(
                    progress = if (stats.totalAnswered == 0) 0f else stats.totalCorrect.toFloat() / stats.totalAnswered.toFloat(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                )

                StatRow(label = "間違い（復習対象）", value = "${stats.wrongCount}問")
            }
        }

        Spacer(Modifier.height(4.dp))

        TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
            Text("← 戻る")
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    }
}