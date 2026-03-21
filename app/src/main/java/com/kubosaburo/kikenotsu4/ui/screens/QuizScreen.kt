package com.kubosaburo.kikenotsu4.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import org.json.JSONObject
import kotlin.random.Random
import com.kubosaburo.kikenotsu4.ui.components.CharacterSpeechBubbleView
import com.kubosaburo.kikenotsu4.R
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kubosaburo.kikenotsu4.data.QuizQuestion
// import com.kubosaburo.kikenotsu4.ui.parseBoldMarkdown
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import com.kubosaburo.kikenotsu4.data.LearningEffectSound

@Composable
fun QuizScreen(
    textId: String,
    allQuestions: List<QuizQuestion>,
    onBack: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    questionIds: List<String>? = null,
    onAnswerCommitted: ((questionId: String, isCorrect: Boolean) -> Unit)? = null,
    // ✅ Prefer showing SectionCelebrationScreen when a text-quiz finishes.
    // The caller (typically MainActivity/AppRoot) should route this callback to SectionCelebrationScreen.
    onShowCelebration: ((total: Int, correct: Int, wrongIds: List<String>) -> Unit)? = null,
    // (Fallback) legacy finish handler (previously used to show ResultScreen)
    onFinish: ((total: Int, correct: Int, wrongIds: List<String>) -> Unit)? = null
) {
    val questions = remember(allQuestions, textId, questionIds) {
        if (questionIds.isNullOrEmpty()) {
            allQuestions.filter { it.textId == textId }
        } else {
            val idSet = questionIds.toSet()
            val byId = allQuestions.filter { idSet.contains(it.id) }
            questionIds.mapNotNull { id -> byId.firstOrNull { it.id == id } }
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
    var answeredIsCorrect by remember(textId, questionIds, index) { mutableStateOf<Boolean?>(null) }
    var praiseText by remember(textId, questionIds, index) { mutableStateOf<String?>(null) }
    var correctCount by remember(textId, questionIds) { mutableIntStateOf(0) }
    var wrongIds by remember(textId, questionIds) { mutableStateOf<List<String>>(emptyList()) }

    val context = LocalContext.current
    val praiseProvider = remember { PraiseMessageProvider(context) }

    fun mdBold(text: String): AnnotatedString = buildAnnotatedString {
        if (text.isEmpty()) return@buildAnnotatedString
        val pattern = Regex("\\*\\*(.+?)\\*\\*")
        var last = 0
        for (m in pattern.findAll(text)) {
            val start = m.range.first
            val end = m.range.last + 1
            if (start > last) {
                append(text.substring(last, start))
            }
            val inner = m.groupValues.getOrNull(1).orEmpty()
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(inner)
            }
            last = end
        }
        if (last < text.length) {
            append(text.substring(last))
        }
    }

    fun playSe(resId: Int) {
        LearningEffectSound.playOneShot(context, resId)
    }

    val listState = rememberLazyListState()

    // 問題が切り替わったら、必ず先頭（問題文）まで戻す
    LaunchedEffect(index) {
        listState.scrollToItem(0)
        selected = null
        showExplanation = false
        answeredIsCorrect = null
        praiseText = null
    }

    val q = questions[index]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {

            Text(
                text = "問題 ${index + 1}/${questions.size}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f, fill = true)
                .fillMaxWidth(),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                // 問題文（iOS風：薄オレンジ背景＋縁＋左の縦バー）
                val border = Color(0xFFF2C8A6)
                val bg = Color(0xFFFFF2E8)
                val accent = Color(0xFFF29A3A)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(unbounded = true),
                    shape = RoundedCornerShape(24.dp),
                    colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = bg),
                    border = BorderStroke(1.5.dp, border)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(unbounded = true)
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // 左の縦バー
                            Box(
                                modifier = Modifier
                                    .width(6.dp)
                                    .height(18.dp)
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(accent)
                            )

                            Text(
                                text = "問題",
                                color = accent,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 10.dp)
                            )
                        }

                        Text(
                            text = mdBold(q.question),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .fillMaxWidth()
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

            // ✅ 正誤メッセージ（吹き出し）を問題文と選択肢の間に表示
            if (showExplanation) {
                item {
                    val msg = praiseText ?: ""
                    if (msg.isNotBlank()) {
                        val isCorrect = answeredIsCorrect == true
                        CharacterSpeechBubbleView(
                            characterImage1 = R.drawable.nicosme_normal,
                            characterImage2 = if (isCorrect) {
                                R.drawable.nicosme_happy
                            } else {
                                R.drawable.nicosme_doten
                            },
                            durationMillis = 2000L,
                            text = msg,
                            modifier = Modifier.fillMaxWidth(),
                            characterSize = 96.dp,
                            bubbleBorderColor = Color(0xFFE6B7C6) // やわらかめのピンク
                        )
                    }
                }

                item { Spacer(Modifier.height(12.dp)) }
            }

            itemsIndexed(q.choices) { i, choice ->
                val isSelected = (selected == i)
                val isCorrectChoice = (i == q.correctIndex)
                val isWrongSelected = showExplanation && isSelected && !isCorrectChoice

                val baseBg = Color(0xFFF2F2F7)
                val selectedBg = Color(0xFFEAEAF2)
                val baseStroke = Color(0xFFE2E2EA)

                val correctBg = Color(0xFFEAF7EE)
                val correctStroke = Color(0xFF2E7D32)
                val wrongBg = Color(0xFFFCE8E6)
                val wrongStroke = Color(0xFFC62828)

                val containerColor = when {
                    showExplanation && isCorrectChoice -> correctBg
                    isWrongSelected -> wrongBg
                    isSelected -> selectedBg
                    else -> baseBg
                }

                val strokeColor = when {
                    showExplanation && isCorrectChoice -> correctStroke
                    isWrongSelected -> wrongStroke
                    else -> baseStroke
                }

                val letter = ('A'.code + i).toChar().toString()

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // すでに解答済みなら何もしない（連打防止）
                            if (showExplanation) return@clickable

                            val isCorrect = (i == q.correctIndex)
                            // ✅ SE
                            if (isCorrect) {
                                playSe(R.raw.correct)
                            } else {
                                playSe(R.raw.wrong)
                            }

                            // Update local counters (kept for compatibility / future expansion)
                            if (isCorrect) {
                                correctCount++
                            } else {
                                wrongIds = (wrongIds + q.id).distinct()
                            }

                            // Per-question hook
                            onAnswerCommitted?.invoke(q.id, isCorrect)

                            // Show explanation mode (iOS風の結果表示はこの画面内で行う)
                            selected = i
                            answeredIsCorrect = isCorrect
                            praiseText = if (isCorrect) {
                                praiseProvider.randomCorrect()
                            } else {
                                praiseProvider.randomWrong()
                            }
                            showExplanation = true
                        },
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.25.dp, strokeColor),
                    colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = containerColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 左のA/B/Cバッジ
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFE9E9EF))
                                .wrapContentHeight(Alignment.CenterVertically)
                        ) {
                            Text(
                                text = letter,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        Spacer(Modifier.width(14.dp))

                        Text(
                            text = mdBold(choice),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Start,
                            softWrap = true,
                            maxLines = Int.MAX_VALUE,
                            overflow = TextOverflow.Clip
                        )
                    }
                }
            }

            if (showExplanation) {
                item { Spacer(Modifier.height(12.dp)) }

                // ✅ 解説（iOS風カード）
                item {
                    val bg = Color(0xFFFFF2E8)
                    val border = Color(0xFFF2C8A6)
                    val accent = Color(0xFF5DBB63)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(unbounded = true),
                        shape = RoundedCornerShape(24.dp),
                        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = bg),
                        border = BorderStroke(1.5.dp, border)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(unbounded = true)
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .width(6.dp)
                                        .height(18.dp)
                                        .clip(RoundedCornerShape(99.dp))
                                        .background(accent)
                                )
                                Text(
                                    text = "解説",
                                    color = accent,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 10.dp)
                                )
                            }

                            Text(
                                text = mdBold(q.explanation),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight(unbounded = true),
                                softWrap = true,
                                maxLines = Int.MAX_VALUE,
                                overflow = TextOverflow.Clip
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(14.dp)) }

                // ✅ 次の問題へ / もう一度
                item {
                    val isCorrect = answeredIsCorrect == true
                    val label = if (isCorrect) "次の問題 ▶" else "もう一度"

                    Button(
                        onClick = {
                            if (isCorrect) {
                                if (index < questions.lastIndex) {
                                    index++
                                } else {
                                    val total = questions.size
                                    val correct = correctCount
                                    val wrong = wrongIds

                                    // questionIds が渡されているときは、1テキスト完了ではなく
                                    // 復習セッション全体の完了として扱い、SectionCelebration は出さない。
                                    if (!questionIds.isNullOrEmpty()) {
                                        onFinish?.invoke(total, correct, wrong)
                                    } else if (onShowCelebration != null) {
                                        onShowCelebration.invoke(total, correct, wrong)
                                    } else {
                                        // Fallback (legacy)
                                        onFinish?.invoke(total, correct, wrong)
                                    }
                                }
                            } else {
                                // もう一度：解説モードを解除して再回答
                                selected = null
                                showExplanation = false
                                answeredIsCorrect = null
                                praiseText = null
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .height(56.dp)
                    ) {
                        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}


private class PraiseMessageProvider(private val context: android.content.Context) {

    private val correct: List<String>
    private val wrong: List<String>

    init {
        val json = runCatching {
            context.assets.open("praise_messages.json").bufferedReader().use { it.readText() }
        }.getOrDefault("{}")

        val root = runCatching { JSONObject(json) }.getOrNull() ?: JSONObject("{}")

        fun readList(key: String): List<String> {
            val arr = root.optJSONArray(key) ?: return emptyList()
            val out = ArrayList<String>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i)
                val text = obj?.optString("text")?.trim().orEmpty()
                if (text.isNotBlank()) out.add(text)
            }
            return out
        }

        // ✅ ユーザー側で wrongPraise_messages に変更済み
        correct = readList("correctPraise_messages")
        wrong = readList("wrongPraise_messages")
    }

    fun randomCorrect(): String {
        if (correct.isEmpty()) return "いい感じ！その調子！"
        return correct[Random.nextInt(correct.size)]
    }

    fun randomWrong(): String {
        if (wrong.isEmpty()) return "惜しい！解説を読んで次いこう。"
        return wrong[Random.nextInt(wrong.size)]
    }
}