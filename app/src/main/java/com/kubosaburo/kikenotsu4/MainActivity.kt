@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.kubosaburo.kikenotsu4

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kubosaburo.kikenotsu4.data.AssetRepository
import com.kubosaburo.kikenotsu4.data.QuizLogStore
import com.kubosaburo.kikenotsu4.data.QuizQuestion
import com.kubosaburo.kikenotsu4.data.TextItem
import com.kubosaburo.kikenotsu4.ui.components.CharacterSpeechBubbleView
import com.kubosaburo.kikenotsu4.ui.screens.FreeStudyHomeScreen
import com.kubosaburo.kikenotsu4.ui.screens.QuizScreen
import com.kubosaburo.kikenotsu4.ui.screens.TextDetailScreen
import com.kubosaburo.kikenotsu4.ui.screens.TextListScreen
import com.kubosaburo.kikenotsu4.ui.theme.KikenOtsu4Theme
import com.kubosaburo.kikenotsu4.ui.screens.SectionCelebrationScreen
import kotlin.math.roundToInt
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KikenOtsu4Theme {
                AppRoot()
            }
        }
    }
}

private enum class BottomTab { HOME, PROGRESS, SETTINGS }
private enum class HomeMode { MENU, FREE_STUDY, CURRICULUM, MOCK }
private enum class FreeStudyMode { HOME, TEXT_LIST }

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val quizLogStore = remember { QuizLogStore(context) }

    var texts by remember { mutableStateOf<List<TextItem>>(emptyList()) }
    var questions by remember { mutableStateOf<List<QuizQuestion>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    var selectedTab by rememberSaveable { mutableStateOf(BottomTab.HOME) }
    var homeMode by rememberSaveable { mutableStateOf(HomeMode.MENU) }
    var freeStudyMode by rememberSaveable { mutableStateOf(FreeStudyMode.HOME) }

    var selectedTextId by rememberSaveable { mutableStateOf<String?>(null) }
    var quizTextId by rememberSaveable { mutableStateOf<String?>(null) }
    // 指定された問題IDだけ出題したい時に使う（例：間違いだけもう一度）
    // null ではなく emptyList() で管理して、状態遷移を安定させる
    @Suppress("ASSIGNED_VALUE_IS_NEVER_READ")
    var quizQuestionIds by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
// ✅ Section celebration (ResultScreenの代わり)
    var celebrationMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var celebrationTextId by rememberSaveable { mutableStateOf<String?>(null) }
    var celebrationIsCurriculum by rememberSaveable { mutableStateOf(false) }
    // 進捗の再読み込みトリガー（解答ごとにインクリメント）

    var progressVersion by rememberSaveable { mutableIntStateOf(0) }



    LaunchedEffect(Unit) {
        runCatching {
            texts = AssetRepository.loadTexts(context)
            questions = AssetRepository.loadQuestions(context)

            // DEBUG: questions.json から読み込めている「原文」を確認
            val q0 = questions.firstOrNull()
            if (q0 != null) {
                Log.d("MainActivity", "loaded q0.id=${q0.id} len=${q0.question.length} q='${q0.question}'")
            } else {
                Log.d("MainActivity", "loaded questions is empty")
            }
        }.onFailure {
            error = it.message ?: it.toString()
        }
    }

    val selected: TextItem? = selectedTextId?.let { id -> texts.firstOrNull { it.id == id } }

    fun topBarTextProgressParts(): Pair<String, String?> {
        val total = texts.size
        val id = selectedTextId
        if (id == null || total <= 0) return "テキスト" to null
        val idx0 = texts.indexOfFirst { it.id == id }
        val current = if (idx0 >= 0) idx0 + 1 else 0
        val sub = if (current > 0) "$current / $total" else "- / $total"
        return "テキスト" to sub
    }



    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    if (selected != null) {
                        val (t, sub) = topBarTextProgressParts()
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(t, style = MaterialTheme.typography.titleSmall)
                            if (sub != null) {
                                Text(
                                    sub,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        val topTitle =
                            when {
                                selectedTab == BottomTab.PROGRESS -> "進捗"
                                selectedTab == BottomTab.SETTINGS -> "設定"
                                celebrationMessage != null -> "お疲れさま！"
                                quizTextId != null -> "クイズ"
                                homeMode == HomeMode.MENU -> "ホーム"
                                homeMode == HomeMode.CURRICULUM -> "カリキュラム"
                                homeMode == HomeMode.MOCK -> "模擬テスト"
                                homeMode == HomeMode.FREE_STUDY && freeStudyMode == FreeStudyMode.TEXT_LIST -> "テキスト一覧"
                                else -> "自分で学ぶ"
                            }

                        Text(
                            text = topTitle,
                            style = if (topTitle == "ホーム") MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                            fontWeight = if (topTitle == "ホーム" || topTitle == "自分で学ぶ") FontWeight.Bold else FontWeight.Normal
                        )
                    }
                },
                navigationIcon = {
                    when {
                        // ✅ Celebration: back to the text detail
                        celebrationMessage != null && celebrationTextId != null -> {
                            val tid = celebrationTextId!!
                            IconButton(
                                onClick = {
                                    @Suppress("ASSIGNED_VALUE_IS_NEVER_READ")
                                    run {
                                        selectedTextId = tid
                                        celebrationMessage = null
                                        celebrationTextId = null
                                        celebrationIsCurriculum = false
                                    }
                                },
                                modifier = Modifier
                                    .padding(start = 12.dp)
                                    .size(36.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "テキストへ戻る",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // ✅ Quiz: back to the text detail for this quiz
                        quizTextId != null -> {
                            val tid = quizTextId!!
                            IconButton(
                                onClick = {
                                    @Suppress("ASSIGNED_VALUE_IS_NEVER_READ")
                                    run {
                                        selectedTextId = tid
                                        quizTextId = null
                                        quizQuestionIds = emptyList()
                                    }
                                },
                                modifier = Modifier
                                    .padding(start = 12.dp)
                                    .size(36.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "テキストへ戻る",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // ✅ Text detail: back to list by clearing selection
                        selected != null -> {
                            IconButton(
                                onClick = {
                                    @Suppress("ASSIGNED_VALUE_IS_NEVER_READ")
                                    run { selectedTextId = null }
                                },
                                modifier = Modifier
                                    .padding(start = 12.dp)
                                    .size(36.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "一覧へ戻る",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // ✅ FreeStudy list: back to FreeStudy home
                        homeMode == HomeMode.FREE_STUDY && freeStudyMode == FreeStudyMode.TEXT_LIST -> {
                            IconButton(
                                onClick = { freeStudyMode = FreeStudyMode.HOME },
                                modifier = Modifier
                                    .padding(start = 12.dp)
                                    .size(36.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "自分で学ぶへ戻る",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                actions = {
                    // no actions on this screen for now
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == BottomTab.HOME,
                    onClick = {
                        @Suppress("ASSIGNED_VALUE_IS_NEVER_READ")
                        run {
                            selectedTab = BottomTab.HOME
                            homeMode = HomeMode.MENU
                            selectedTextId = null
                            quizTextId = null
                            quizQuestionIds = emptyList()
                            celebrationMessage = null
                            celebrationTextId = null
                            celebrationIsCurriculum = false
                        }
                    },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "ホーム") },
                    label = { Text("ホーム") }
                )
                NavigationBarItem(
                    selected = selectedTab == BottomTab.PROGRESS,
                    onClick = { selectedTab = BottomTab.PROGRESS },
                    icon = { Icon(Icons.Filled.BarChart, contentDescription = "進捗") },
                    label = { Text("進捗") }
                )
                NavigationBarItem(
                    selected = selectedTab == BottomTab.SETTINGS,
                    onClick = { selectedTab = BottomTab.SETTINGS },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "設定") },
                    label = { Text("設定") }
                )
            }
        }
    ) { innerPadding ->


        when {
            // Tabs that override everything
            selectedTab == BottomTab.PROGRESS -> {
                ProgressScreen(
                    quizLogStore = quizLogStore,
                    contentPadding = innerPadding,
                    progressVersion = progressVersion,
                    onBack = { selectedTab = BottomTab.HOME }
                )
            }

            selectedTab == BottomTab.SETTINGS -> {
                SettingsScreen(
                    contentPadding = innerPadding
                )
            }

            // Home tab sub-flows
            error != null -> {
                Text("読み込みに失敗しました: $error")
            }
            celebrationMessage != null && celebrationTextId != null -> {
                val msg = celebrationMessage!!
                val tid = celebrationTextId!!

                SectionCelebrationScreen(
                    message = msg,
                    isCurriculum = celebrationIsCurriculum,
                    onTapNext = {
                        @Suppress("ASSIGNED_VALUE_IS_NEVER_READ")
                        run {
                            // ひとまず「テキストへ戻る」挙動（iOSの「一覧へ戻る」相当）
                            selectedTextId = tid
                            celebrationMessage = null
                            celebrationTextId = null
                            celebrationIsCurriculum = false
                        }
                    }
                )
            }



            quizTextId != null -> {
                val tid = quizTextId!!

                // DEBUG: QuizScreen に渡す直前の値を確認（ここで全文ならUI側、ここで末尾だけならデータ側）
                val firstForTid = questions.firstOrNull { it.textId == tid }
                if (firstForTid != null) {
                    Log.d(
                        "MainActivity",
                        "before QuizScreen tid=$tid questions.size=${questions.size} first.id=${firstForTid.id} len=${firstForTid.question.length} q='${firstForTid.question}'"
                    )
                } else {
                    Log.d("MainActivity", "before QuizScreen tid=$tid questions.size=${questions.size} firstForTid=null")
                }

                QuizScreen(
                    textId = tid,
                    allQuestions = questions,
                    contentPadding = innerPadding,
                    onBack = {
                        @Suppress("ASSIGNED_VALUE_IS_NEVER_READ")
                        run {
                            // ✅ Return to the text that this quiz belongs to
                            selectedTextId = tid
                            quizTextId = null
                            quizQuestionIds = emptyList()
                        }
                    },
                    questionIds = quizQuestionIds.takeIf { it.isNotEmpty() },
                    onAnswerCommitted = { qid, isCorrect ->
                        quizLogStore.recordAnswer(qid, isCorrect)
                        progressVersion += 1
                    },
                    onShowCelebration = { total, correct, _ ->
                        @Suppress("ASSIGNED_VALUE_IS_NEVER_READ")
                        run {
                            val allCorrect = (correct == total)
                            celebrationTextId = tid
                            celebrationIsCurriculum = false
                            celebrationMessage = if (allCorrect) {
                                "全問正解！最高！🎉"
                            } else {
                                "おつかれさま！よく頑張ったね！🎉"
                            }

                            // close quiz
                            quizTextId = null
                            quizQuestionIds = emptyList()
                        }
                    },
                    onFinish = { _, _, _ ->
                        // Quiz end handling is done via onShowCelebration
                    }
                )
            }

            selected != null -> {
                TextDetailScreen(
                    textItem = selected,
                    contentPadding = innerPadding,
                    onStartQuiz = {
                        @Suppress("ASSIGNED_VALUE_IS_NEVER_READ")
                        run {
                            quizQuestionIds = emptyList()
                            quizTextId = it
                        }
                    }
                )
            }

            homeMode == HomeMode.FREE_STUDY -> {
                when (freeStudyMode) {
                    FreeStudyMode.HOME -> {
                        FreeStudyHomeScreen(
                            contentPadding = innerPadding,
                            onTextQuiz = {
                                // まずは既存のテキスト一覧へ繋ぐ（暫定）
                                freeStudyMode = FreeStudyMode.TEXT_LIST
                            },
                            onBookmarks = {
                                // TODO: Bookmark画面を作ったら遷移
                                homeMode = HomeMode.MENU
                            },
                            onTodayReview = {
                                // TODO: Review実装後に遷移
                                homeMode = HomeMode.MENU
                            },
                            onSearch = {
                                // TODO: Search画面を作ったら遷移
                                homeMode = HomeMode.MENU
                            },
                            characterImage1 = R.drawable.nicosme_normal,
                            characterImage2 = R.drawable.nicosme_openmouth
                        )
                    }

                    FreeStudyMode.TEXT_LIST -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            TextListScreen(
                                items = texts,
                                contentPadding = PaddingValues(0.dp),
                                onOpen = {
                                    @Suppress("ASSIGNED_VALUE_IS_NEVER_READ")
                                    run { selectedTextId = it }
                                }
                            )
                        }
                    }
                }
            }

            homeMode == HomeMode.CURRICULUM -> {
                PlaceholderModeScreen(
                    title = "カリキュラムで学ぶ（準備中）",
                    contentPadding = innerPadding,
                    onBack = { homeMode = HomeMode.MENU }
                )
            }

            homeMode == HomeMode.MOCK -> {
                PlaceholderModeScreen(
                    title = "模擬テストで学ぶ（準備中）",
                    contentPadding = innerPadding,
                    onBack = { homeMode = HomeMode.MENU }
                )
            }

            else -> {
                HomeMenuScreen(
                    contentPadding = innerPadding,
                    onCurriculum = { homeMode = HomeMode.CURRICULUM },
                    onFreeStudy = {
                        homeMode = HomeMode.FREE_STUDY
                        freeStudyMode = FreeStudyMode.HOME
                    },
                    onMock = { homeMode = HomeMode.MOCK }
                )
            }
        }
    }
}


@Composable
private fun ProgressScreen(
    quizLogStore: QuizLogStore,
    contentPadding: PaddingValues,
    progressVersion: Int,
    onBack: () -> Unit
) {
    var stats by remember { mutableStateOf(quizLogStore.getStats()) }

    LaunchedEffect(progressVersion) {
        stats = quizLogStore.getStats()
    }

    val accuracy = if (stats.totalAnswered == 0) 0
    else ((stats.totalCorrect.toFloat() / stats.totalAnswered.toFloat()) * 100f).roundToInt()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatRow("解いた問題数", "${stats.totalAnswered}問")
                StatRow("正解数", "${stats.totalCorrect}問")
                StatRow("正答率", "${accuracy}%")

                LinearProgressIndicator(
                    progress = {
                        if (stats.totalAnswered == 0) 0f
                        else stats.totalCorrect.toFloat() / stats.totalAnswered.toFloat()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                )

                StatRow("間違い（復習対象）", "${stats.wrongCount}問")
            }
        }

        Spacer(Modifier.height(4.dp))

        TextButton(onClick = onBack) {
            Text("← 戻る")
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Column(Modifier.fillMaxWidth()) {
        Text(label)
        Text(value)
    }
}


@Composable
private fun HomeMenuScreen(
    contentPadding: PaddingValues,
    onCurriculum: () -> Unit,
    onFreeStudy: () -> Unit,
    onMock: () -> Unit
) {
    // 改行は手動で入れず、端末幅に応じて自然に折り返す
    val phrases = remember {
        listOf(
            "学ぼうとする姿勢が素晴らしい！拍手が止まらない！",
            "今日の1問が未来を変えるよ！",
            "コツコツがいちばん強い！",
            "小さな積み重ねが大きな力になるよ！"
        )
    }
    val phrase = remember { phrases[Random.nextInt(phrases.size)] }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- Hero ---
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // キャラクター + 吹き出し（中央寄せ）
            CharacterSpeechBubbleView(
                characterImage1 = R.drawable.nicosme_normal,
                characterImage2 = R.drawable.nicosme_run,
                durationMillis = 2000L,
                text = phrase,
                modifier = Modifier.fillMaxWidth(),
                characterSize = 120.dp
            )
        }

        Spacer(Modifier.height(16.dp))

        // --- Menu cards ---
        HomeMenuCard(
            icon = Icons.Filled.School,
            title = "カリキュラムで学ぶ",
            subtitle = "順番通りに学んで合格までアテンド",
            backgroundColor = Color(0xFFCB7A2B),
            onClick = onCurriculum
        )
        Spacer(Modifier.height(10.dp))
        HomeMenuCard(
            icon = Icons.AutoMirrored.Filled.MenuBook,
            title = "自分で学ぶ",
            subtitle = "テキスト・問題・復習を自由に選べる",
            backgroundColor = Color(0xFF4FAE72),
            onClick = onFreeStudy
        )
        Spacer(Modifier.height(10.dp))
        HomeMenuCard(
            icon = Icons.AutoMirrored.Filled.Assignment,
            title = "模擬テストで学ぶ",
            subtitle = "本番形式で実力チェック",
            backgroundColor = Color(0xFF3B78C8),
            onClick = onMock
        )

        Spacer(Modifier.height(8.dp))
    }
}


@Composable
private fun HomeMenuCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    backgroundColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(22.dp)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(104.dp)
            .clickable { onClick() },
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Icon badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.92f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Chevron
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.95f),
                modifier = Modifier
                    .size(22.dp)
                    .background(Color.White.copy(alpha = 0.14f), CircleShape)
                    .padding(2.dp)
                    .clip(CircleShape)
            )
        }
    }
}

@Composable
private fun PlaceholderModeScreen(
    title: String,
    contentPadding: PaddingValues,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title)
        Button(onClick = onBack) {
            Text("戻る")
        }
    }
}

@Composable
private fun SettingsScreen(
    contentPadding: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("設定（準備中）")
        Text("ここに通知・サウンド・開発用設定などを追加していきます。")
    }
}