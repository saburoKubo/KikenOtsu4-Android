package com.kubosaburo.kikenotsu4.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kubosaburo.kikenotsu4.data.QuizQuestion
import com.kubosaburo.kikenotsu4.ui.parseBoldMarkdown

@Composable
fun QuizScreen(
    textId: String,
    allQuestions: List<QuizQuestion>,
    onBack: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    questionIds: List<String>? = null,
    onAnswerCommitted: ((questionId: String, isCorrect: Boolean) -> Unit)? = null,
    onFinish: ((total: Int, correct: Int, wrongIds: List<String>) -> Unit)? = null
) {
    val questions = remember(allQuestions, textId, questionIds) {
        val base = allQuestions.filter { it.textId == textId }
        if (questionIds.isNullOrEmpty()) {
            base
        } else {
            val set = questionIds.toSet()
            base.filter { set.contains(it.id) }
        }
    }

    if (questions.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("このテキストの問題が見つかりません", style = MaterialTheme.typography.titleMedium)
            Text("text_id = $textId の問題が questions.json にあるか確認してください。")
            Button(onClick = onBack) { Text("戻る") }
        }
        return
    }

    var index by remember(textId, questionIds) { mutableIntStateOf(0) }
    var selected by remember(textId, questionIds, index) { mutableStateOf<Int?>(null) }
    var showExplanation by remember(textId, questionIds, index) { mutableStateOf(false) }
    var correctCount by remember(textId, questionIds) { mutableIntStateOf(0) }
    var wrongIds by remember(textId, questionIds) { mutableStateOf<List<String>>(emptyList()) }

    val listState = rememberLazyListState()

    // 問題が切り替わったら、必ず先頭（問題文）まで戻す
    LaunchedEffect(index) {
        listState.scrollToItem(0)
    }

    val q = questions[index]
    val isLast = index == questions.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${index + 1}/${questions.size}")
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f, fill = true)
                .fillMaxWidth(),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                // 問題文（全文を必ず表示。親の高さ制約で末尾だけ見えるのを防ぐ）
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(unbounded = true)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(unbounded = true)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = q.question,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopStart)
                                .wrapContentHeight(unbounded = true),
                            textAlign = TextAlign.Start,
                            softWrap = true,
                            maxLines = Int.MAX_VALUE,
                            overflow = TextOverflow.Clip
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(6.dp)) }

            itemsIndexed(q.choices) { i, choice ->
                val cs = MaterialTheme.colorScheme

                val isSelected = (selected == i)
                val isCorrect = (i == q.correctIndex)
                val isWrongSelected = showExplanation && isSelected && !isCorrect

                val borderColor = when {
                    showExplanation && isCorrect -> cs.primary
                    isWrongSelected -> cs.error
                    !showExplanation && isSelected -> cs.primary
                    else -> cs.outline
                }

                val containerColor = when {
                    showExplanation && isCorrect -> cs.primaryContainer
                    isWrongSelected -> cs.errorContainer
                    !showExplanation && isSelected -> cs.surfaceVariant
                    else -> Color.Transparent
                }

                val contentColor = when {
                    showExplanation && isCorrect -> cs.onPrimaryContainer
                    isWrongSelected -> cs.onErrorContainer
                    !showExplanation && isSelected -> cs.onSurfaceVariant
                    else -> cs.onSurface
                }

                OutlinedButton(
                    onClick = { selected = i },
                    enabled = !showExplanation,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(2.dp, borderColor),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = containerColor,
                        contentColor = contentColor
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = choice,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Left,
                            softWrap = true,
                            maxLines = Int.MAX_VALUE,
                            overflow = TextOverflow.Clip
                        )

                        if (showExplanation) {
                            val mark = if (isCorrect) "✅" else if (isSelected) "❌" else ""
                            if (mark.isNotEmpty()) {
                                Spacer(Modifier.height(0.dp))
                                Text(mark, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (showExplanation) {
                item { Spacer(Modifier.height(8.dp)) }
                item {
                    Text("解説", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                item {
                    Text(parseBoldMarkdown(q.explanation))
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text("戻る")
            }

            val canCheck = (selected != null) && (!showExplanation)
            Button(
                onClick = {
                    if (!showExplanation) {
                        val isCorrect = (selected == q.correctIndex)
                        if (isCorrect) {
                            correctCount++
                        } else {
                            // 間違えた問題IDを保持（重複は排除）
                            wrongIds = (wrongIds + q.id).distinct()
                        }
                        // 1問確定フック（ログ保存など）
                        onAnswerCommitted?.invoke(q.id, isCorrect)

                        showExplanation = true
                    } else {
                        if (!isLast) {
                            index++
                            selected = null
                            showExplanation = false
                        } else {
                            // 終了：結果を返せる場合は返す / 無ければ従来通り戻る
                            val finish = onFinish
                            if (finish != null) {
                                finish(questions.size, correctCount, wrongIds)
                            } else {
                                onBack()
                            }
                        }
                    }
                },
                enabled = canCheck || showExplanation,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    when {
                        !showExplanation -> "答え合わせ"
                        !isLast -> "次へ"
                        else -> if (onFinish != null) "結果へ" else "終了"
                    }
                )
            }
        }

        Text("正解数: $correctCount", style = MaterialTheme.typography.bodySmall)
    }
}
