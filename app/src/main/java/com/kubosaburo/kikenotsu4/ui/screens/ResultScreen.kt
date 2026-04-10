package com.kubosaburo.kikenotsu4.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kubosaburo.kikenotsu4.ui.theme.KikenOtsu4Theme

@Suppress("unused")
@Composable
fun ResultScreen(
    total: Int,
    correct: Int,
    wrongIds: List<String>,
    contentPadding: PaddingValues,
    onRetry: () -> Unit,
    onBackHome: () -> Unit
) {
    val scoreText = "$correct / $total"

    Column(
        modifier = Modifier
            .padding(contentPadding)
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("スコア", style = MaterialTheme.typography.titleMedium)
        Text(scoreText, style = MaterialTheme.typography.headlineMedium)

        if (wrongIds.isNotEmpty()) {
            Text("間違えた問題", style = MaterialTheme.typography.titleMedium)

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(wrongIds) { id ->
                    Card {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("ID: $id")
                            Text("復習", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("間違いだけもう一度")
            }
        } else {
            Spacer(Modifier.weight(1f))
            Text("全問正解！", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.weight(1f))
        }

        OutlinedButton(
            onClick = onBackHome,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("ホームへ")
        }
    }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun ResultScreenPreview() {
    KikenOtsu4Theme {
        ResultScreen(
            total = 5,
            correct = 3,
            wrongIds = listOf("q0001", "q0002"),
            contentPadding = PaddingValues(0.dp),
            onRetry = {},
            onBackHome = {},
        )
    }
}
