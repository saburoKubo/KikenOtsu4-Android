package com.kubosaburo.kikenotsu4.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import com.kubosaburo.kikenotsu4.ui.components.CharacterSpeechBubbleView
import com.kubosaburo.kikenotsu4.R
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kubosaburo.kikenotsu4.ui.theme.KikenOtsu4Theme
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
    val questionIdsKey = questionIds.orEmpty().joinToString(",")

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

    // 画面回転で Activity が再作成されても維持する（復習・テキスト後クイズの途中で戻らないように）
    var index by rememberSaveable(textId, questionIdsKey) { mutableIntStateOf(0) }
    /** 未選択は -1。選択肢インデックスは 0 以上 */
    var selectedChoice by rememberSaveable(textId, questionIdsKey, index) { mutableIntStateOf(-1) }
    var showExplanation by rememberSaveable(textId, questionIdsKey, index) { mutableStateOf(false) }
    /** -1: 未回答, 0: 不正解, 1: 正解 */
    var answeredState by rememberSaveable(textId, questionIdsKey, index) { mutableIntStateOf(-1) }
    var correctCount by rememberSaveable(textId, questionIdsKey) { mutableIntStateOf(0) }
    var wrongIdsStr by rememberSaveable(textId, questionIdsKey) { mutableStateOf("") }

    fun wrongIdsList(): List<String> =
        if (wrongIdsStr.isEmpty()) emptyList() else wrongIdsStr.split(",").filter { it.isNotEmpty() }

    fun appendWrongId(id: String) {
        wrongIdsStr = (wrongIdsList() + id).distinct().joinToString(",")
    }

    val context = LocalContext.current

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

    // 問題が変わったときは先頭へスクロール（選択・正誤は index キーの Saveable で別スロットになる）
    LaunchedEffect(index) {
        listState.scrollToItem(0)
    }

    val q = questions[index]
    val dark = isSystemInDarkTheme()
    val scheme = MaterialTheme.colorScheme

    val questionCardBg = if (dark) scheme.surfaceContainerHigh else Color(0xFFFFF2E8)
    val questionCardBorder =
        if (dark) scheme.outline.copy(alpha = 0.45f) else Color(0xFFF2C8A6)
    val questionAccent = if (dark) scheme.primary else Color(0xFFF29A3A)
    val onQuestionCard = scheme.onSurface

    val choiceBaseBg = if (dark) scheme.surfaceContainerHighest else Color(0xFFF2F2F7)
    val choiceSelectedBg = if (dark) scheme.surfaceContainerHigh else Color(0xFFEAEAF2)
    val choiceBaseStroke =
        if (dark) scheme.outlineVariant.copy(alpha = 0.6f) else Color(0xFFE2E2EA)

    val correctBg = if (dark) Color(0xFF1A3D2E) else Color(0xFFEAF7EE)
    val correctStroke = if (dark) Color(0xFF66BB6A) else Color(0xFF2E7D32)
    val wrongBg = if (dark) Color(0xFF3D2424) else Color(0xFFFCE8E6)
    val wrongStroke = if (dark) Color(0xFFFF8A80) else Color(0xFFC62828)

    val choiceLetterBg = if (dark) scheme.surfaceContainer else Color(0xFFE9E9EF)

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
                // 問題文（iOS風：薄オレンジ背景＋縁＋左の縦バー）／ダークはサーフェス系
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(unbounded = true),
                    shape = RoundedCornerShape(24.dp),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = questionCardBg,
                        contentColor = onQuestionCard,
                    ),
                    border = BorderStroke(1.5.dp, questionCardBorder)
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
                                    .background(questionAccent)
                            )

                            Text(
                                text = "問題",
                                color = questionAccent,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 10.dp)
                            )
                        }

                        Text(
                            text = mdBold(q.question),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = onQuestionCard,
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
            if (showExplanation && answeredState >= 0) {
                item {
                    val isCorrect = answeredState == 1
                    val msg = if (isCorrect) "正解" else "残念"
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
                        bubbleBorderColor = if (dark) {
                            scheme.outline.copy(alpha = 0.4f)
                        } else {
                            Color(0xFFE6B7C6)
                        }
                    )
                }

                item { Spacer(Modifier.height(12.dp)) }
            }

            itemsIndexed(q.choices) { i, choice ->
                val isSelected = (selectedChoice == i)
                val isCorrectChoice = (i == q.correctIndex)
                val isWrongSelected = showExplanation && isSelected && !isCorrectChoice

                val containerColor = when {
                    showExplanation && isCorrectChoice -> correctBg
                    isWrongSelected -> wrongBg
                    isSelected -> choiceSelectedBg
                    else -> choiceBaseBg
                }

                val strokeColor = when {
                    showExplanation && isCorrectChoice -> correctStroke
                    isWrongSelected -> wrongStroke
                    else -> choiceBaseStroke
                }
                val choiceContentColor = contentColorFor(containerColor)

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
                                correctCount += 1
                            } else {
                                appendWrongId(q.id)
                            }

                            // Per-question hook
                            onAnswerCommitted?.invoke(q.id, isCorrect)

                            // Show explanation mode (iOS風の結果表示はこの画面内で行う)
                            selectedChoice = i
                            answeredState = if (isCorrect) 1 else 0
                            showExplanation = true
                        },
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.25.dp, strokeColor),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = containerColor,
                        contentColor = choiceContentColor,
                    )
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
                                .background(choiceLetterBg)
                                .wrapContentHeight(Alignment.CenterVertically)
                        ) {
                            Text(
                                text = letter,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = choiceContentColor,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        Spacer(Modifier.width(14.dp))

                        Text(
                            text = mdBold(choice),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = choiceContentColor,
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
                    val explanationCardBg = if (dark) scheme.surfaceContainerHigh else Color(0xFFFFF2E8)
                    val explanationBorder =
                        if (dark) scheme.outline.copy(alpha = 0.45f) else Color(0xFFF2C8A6)
                    val explanationAccent = if (dark) Color(0xFF81C784) else Color(0xFF5DBB63)
                    val explanationContentColor = scheme.onSurface

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(unbounded = true),
                        shape = RoundedCornerShape(24.dp),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = explanationCardBg,
                            contentColor = explanationContentColor,
                        ),
                        border = BorderStroke(1.5.dp, explanationBorder)
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
                                        .background(explanationAccent)
                                )
                                Text(
                                    text = "解説",
                                    color = explanationAccent,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 10.dp)
                                )
                            }

                            Text(
                                text = mdBold(q.explanation),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = explanationContentColor,
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
                    val isCorrect = answeredState == 1
                    val label = if (isCorrect) "次の問題 ▶" else "もう一度"

                    Button(
                        onClick = {
                            if (isCorrect) {
                                if (index < questions.lastIndex) {
                                    index += 1
                                } else {
                                    val total = questions.size
                                    val correct = correctCount
                                    val wrong = wrongIdsList()

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
                                selectedChoice = -1
                                showExplanation = false
                                answeredState = -1
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

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun QuizScreenPreview() {
    KikenOtsu4Theme {
        QuizScreen(
            textId = "text_001",
            allQuestions = listOf(
                QuizQuestion(
                    id = "q_preview_1",
                    question = "サンプル問題：次のうち正しいものはどれか。",
                    choices = listOf("選択肢A", "選択肢B", "選択肢C", "選択肢D", "選択肢E"),
                    correctIndex = 0,
                    textId = "text_001",
                    category = "法令",
                    explanation = "解説のプレビューです。",
                ),
            ),
            onBack = {},
            contentPadding = PaddingValues(0.dp),
        )
    }
}