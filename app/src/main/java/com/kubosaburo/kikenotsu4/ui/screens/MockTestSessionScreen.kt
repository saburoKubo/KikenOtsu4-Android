package com.kubosaburo.kikenotsu4.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale
import com.kubosaburo.kikenotsu4.data.MockTestResultStore
import com.kubosaburo.kikenotsu4.data.MockTestDefinition
import com.kubosaburo.kikenotsu4.data.MockTestLoader
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.random.Random

private const val MOCK_LAW_COUNT = 15
private const val MOCK_PHYSICS_COUNT = 10
private const val MOCK_NATURE_COUNT = 10
private const val MOCK_TOTAL_COUNT = MOCK_LAW_COUNT + MOCK_PHYSICS_COUNT + MOCK_NATURE_COUNT


@Serializable
private data class MockSessionQuestion(
    val id: String,
    val question: String,
    val choices: List<String>,
    @SerialName("correctIndex")
    val correctIndex: Int,
    val category: String,
    val explanation: String = ""
)

@Serializable
private data class QuestionsPayload(
    val questions: List<MockSessionQuestion> = emptyList()
)

@Composable
fun MockTestSessionScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    isPro: Boolean = false
) {
    val context = LocalContext.current

    val mockTest = remember(isPro) {
        runCatching {
            if (isPro) {
                MockTestLoader.loadRandomMockTest(context)
            } else {
                MockTestLoader.loadTrialMockTest(context)
            }
        }.getOrNull()
    }

    val allQuestionsById = remember {
        runCatching { loadQuestionsById(context) }.getOrDefault(emptyMap())
    }

    val sessionQuestionIds = remember(mockTest, allQuestionsById, isPro) {
        buildSessionQuestionIds(
            mockTest = mockTest,
            allQuestionsById = allQuestionsById,
            isPro = isPro
        )
    }

    val sessionQuestions = remember(sessionQuestionIds, allQuestionsById) {
        sessionQuestionIds.mapNotNull { allQuestionsById[it] }
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var isAnswered by remember { mutableStateOf(false) }
    var isFinished by remember { mutableStateOf(false) }
    var correctCount by remember { mutableIntStateOf(0) }
    var wrongCount by remember { mutableIntStateOf(0) }
    var remainingSeconds by remember(mockTest) {
        mutableIntStateOf((mockTest?.timeLimit ?: 60) * 60)
    }

    val answeredCorrectByQuestionId = remember { mutableStateMapOf<String, Boolean>() }
    val startedAtMillis = remember { System.currentTimeMillis() }
    var hasSavedResult by remember { mutableStateOf(false) }

    val modeTitle = mockTest?.title ?: if (isPro) {
        "通常模試（ランダム出題）"
    } else {
        "お試し模試（固定出題）"
    }
    val modeDescription = mockTest?.description ?: if (isPro) {
        "有料版では毎回ランダムで $MOCK_TOTAL_COUNT 問を出題します。"
    } else {
        "無料版では固定の $MOCK_TOTAL_COUNT 問を出題します。"
    }


    fun formatTime(seconds: Int): String {
        val safe = seconds.coerceAtLeast(0)
        val min = safe / 60
        val sec = safe % 60
        return String.format(Locale.US, "%02d:%02d", min, sec)
    }

    val currentQuestion = sessionQuestions.getOrNull(currentIndex)

    val progressValue = if (sessionQuestions.isNotEmpty()) {
        (currentIndex + 1).toFloat() / sessionQuestions.size.toFloat()
    } else {
        0f
    }

    val categoryStats = remember(answeredCorrectByQuestionId.toMap(), sessionQuestions) {
        buildCategoryStats(sessionQuestions, answeredCorrectByQuestionId)
    }

    fun finishSession() {
        if (!isFinished) {
            isFinished = true
        }

        if (hasSavedResult) return
        hasSavedResult = true

        val durationSeconds = ((System.currentTimeMillis() - startedAtMillis) / 1000L).toInt().coerceAtLeast(0)
        val mockTestId = mockTest?.id ?: if (isPro) "mock_random" else "mock_trial_fixed"

        val result = MockTestResultStore.Result(
            mockTestId = mockTestId,
            seed = null,
            finishedAtMillis = System.currentTimeMillis(),
            durationSeconds = durationSeconds,
            total = sessionQuestions.size,
            correct = correctCount,
            wrong = wrongCount,
            categoryStats = categoryStats.mapValues { (_, pair) ->
                MockTestResultStore.CategoryStat(
                    correct = pair.first,
                    total = pair.second,
                )
            }
        )

        MockTestResultStore.recordResult(
            context = context,
            result = result,
        )
    }

    LaunchedEffect(isFinished, sessionQuestions.size) {
        if (isFinished || sessionQuestions.isEmpty()) return@LaunchedEffect
        while (!isFinished && remainingSeconds > 0) {
            delay(1000)
            remainingSeconds -= 1
        }
        if (!isFinished) {
            finishSession()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = modeTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = modeDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (sessionQuestions.isNotEmpty()) {
                            "進捗 ${currentIndex + 1} / ${sessionQuestions.size}"
                        } else {
                            "進捗 0 / 0"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "残り ${formatTime(remainingSeconds)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                LinearProgressIndicator(
                    progress = { progressValue.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.18f)
                )
            }
        }

        if (sessionQuestions.isEmpty()) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "問題を読み込めませんでした",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "mock_tests.json または questions.json の内容を確認してください。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (isFinished) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "模試結果",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "正解 ${correctCount}問 / 不正解 ${wrongCount}問",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "正答率 ${if (sessionQuestions.isNotEmpty()) (correctCount * 100 / sessionQuestions.size) else 0}%",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    categoryStats.forEach { (category, pair) ->
                        val total = pair.second
                        val correct = pair.first
                        val rate = if (total > 0) correct * 100 / total else 0
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${correct}/${total}問（${rate}%）",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Card(
                shape = RoundedCornerShape(20.dp),
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
                        text = "問題 ${currentIndex + 1}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = currentQuestion?.question.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = currentQuestion?.category.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            currentQuestion?.choices?.forEachIndexed { index, choice ->
                val isSelected = selectedIndex == index
                val isCorrectChoice = currentQuestion.correctIndex == index
                val background = when {
                    !isAnswered && isSelected -> MaterialTheme.colorScheme.secondaryContainer
                    isAnswered && isCorrectChoice -> MaterialTheme.colorScheme.primaryContainer
                    isAnswered && isSelected -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surface
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = background),
                    border = BorderStroke(
                        width = if (isSelected || (isAnswered && isCorrectChoice)) 2.dp else 1.dp,
                        color = when {
                            isAnswered && isCorrectChoice -> MaterialTheme.colorScheme.primary
                            isAnswered && isSelected -> MaterialTheme.colorScheme.error
                            isSelected -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.outlineVariant
                        }
                    ),
                    onClick = {
                        if (isAnswered) return@Card
                        selectedIndex = index
                        isAnswered = true
                        val correct = index == currentQuestion.correctIndex
                        answeredCorrectByQuestionId[currentQuestion.id] = correct
                        if (correct) {
                            correctCount += 1
                        } else {
                            wrongCount += 1
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = ('A' + index).toString(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isAnswered && isCorrectChoice -> MaterialTheme.colorScheme.primary
                                isAnswered && isSelected -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Text(
                            text = choice,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (isAnswered && currentQuestion != null) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (selectedIndex == currentQuestion.correctIndex) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (selectedIndex == currentQuestion.correctIndex) "正解です" else "不正解です",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedIndex == currentQuestion.correctIndex) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                        Text(
                            text = currentQuestion.explanation.ifBlank { "解説はありません。" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = {
                        if (currentIndex + 1 < sessionQuestions.size) {
                            currentIndex += 1
                            selectedIndex = null
                            isAnswered = false
                        } else {
                            finishSession()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(if (currentIndex + 1 < sessionQuestions.size) "次の問題へ" else "結果を見る")
                }
            }
        }


        Button(onClick = onBack) {
            Text(if (isFinished) "模擬テスト一覧へ戻る" else "中断して一覧へ戻る")
        }
    }
}


private fun loadQuestionsById(context: android.content.Context): Map<String, MockSessionQuestion> {
    val raw = context.assets
        .open("questions.json")
        .bufferedReader()
        .use { it.readText() }

    val json = Json { ignoreUnknownKeys = true }

    val list = runCatching {
        json.decodeFromString(ListSerializer(MockSessionQuestion.serializer()), raw)
    }.getOrElse {
        json.decodeFromString(QuestionsPayload.serializer(), raw).questions
    }

    return list.associateBy { it.id }
}

private fun buildSessionQuestionIds(
    mockTest: MockTestDefinition?,
    allQuestionsById: Map<String, MockSessionQuestion>,
    isPro: Boolean
): List<String> {
    if (mockTest == null) return emptyList()

    if (!isPro && mockTest.questionIds.isNotEmpty()) {
        return mockTest.questionIds.filter { allQuestionsById.containsKey(it) }
    }

    if (mockTest.sections.isEmpty()) return emptyList()

    val random = Random(System.currentTimeMillis())
    val allQuestions = allQuestionsById.values.toList()

    return mockTest.sections.flatMap { section ->
        val candidates = allQuestions.filter { question ->
            normalizeCategory(question.category) == normalizeCategory(section.title) &&
                isQuestionIdInRange(question.id, section.range.start, section.range.end)
        }
        candidates.shuffled(random).take(section.count).map { it.id }
    }
}

private fun buildCategoryStats(
    sessionQuestions: List<MockSessionQuestion>,
    answeredCorrectByQuestionId: Map<String, Boolean>
): Map<String, Pair<Int, Int>> {
    val grouped = sessionQuestions.groupBy { normalizeCategory(it.category) }
    return grouped.mapValues { (_, questions) ->
        val total = questions.size
        val correct = questions.count { answeredCorrectByQuestionId[it.id] == true }
        correct to total
    }
}

private fun normalizeCategory(raw: String): String {
    return when (raw.trim()) {
        "性質/消火", "消火・性質", "性質消火" -> "性質・消火"
        else -> raw.trim()
    }
}

private fun isQuestionIdInRange(id: String, start: String, end: String): Boolean {
    val value = extractQuestionNumber(id)
    val startValue = extractQuestionNumber(start)
    val endValue = extractQuestionNumber(end)
    return value in startValue..endValue
}

private fun extractQuestionNumber(id: String): Int {
    return id.filter { it.isDigit() }.toIntOrNull() ?: -1
}