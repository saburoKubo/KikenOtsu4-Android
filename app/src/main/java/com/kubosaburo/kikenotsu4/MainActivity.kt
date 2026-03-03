@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.kubosaburo.kikenotsu4

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import com.kubosaburo.kikenotsu4.ui.components.CharacterSpeechBubbleView
import kotlin.random.Random
import android.util.Log
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import com.kubosaburo.kikenotsu4.data.AssetRepository
import com.kubosaburo.kikenotsu4.data.QuizLogStore
import com.kubosaburo.kikenotsu4.data.QuizQuestion
import com.kubosaburo.kikenotsu4.data.TextItem
import com.kubosaburo.kikenotsu4.ui.screens.QuizScreen
import com.kubosaburo.kikenotsu4.ui.screens.ResultScreen
import com.kubosaburo.kikenotsu4.ui.screens.TextDetailScreen
import com.kubosaburo.kikenotsu4.ui.screens.TextListScreen
import com.kubosaburo.kikenotsu4.ui.screens.FreeStudyHomeScreen
import com.kubosaburo.kikenotsu4.ui.theme.KikenOtsu4Theme
import kotlin.math.roundToInt

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
    // クイズ結果
    var quizResult by remember { mutableStateOf<QuizResult?>(null) }

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

    fun startQuizWithIds(tid: String, ids: List<String>) {
        quizQuestionIds = ids
        quizTextId = tid
    }


    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        when {
                            selectedTab == BottomTab.PROGRESS -> "進捗"
                            selectedTab == BottomTab.SETTINGS -> "設定"
                            quizResult != null -> "結果"
                            quizTextId != null -> "クイズ"
                            selected != null -> selected.title
                            homeMode == HomeMode.MENU -> "ホーム"
                            homeMode == HomeMode.CURRICULUM -> "カリキュラム"
                            homeMode == HomeMode.MOCK -> "模擬テスト"
                            homeMode == HomeMode.FREE_STUDY && freeStudyMode == FreeStudyMode.TEXT_LIST -> "テキスト＋問題で学ぶ"
                            else -> "自分で学ぶ"
                        }
                    )
                },
                navigationIcon = {
                    if (homeMode == HomeMode.FREE_STUDY && freeStudyMode == FreeStudyMode.TEXT_LIST) {
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
                            quizResult = null
                            quizQuestionIds = emptyList()
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

            quizResult != null -> {
                val r = quizResult!!
                ResultScreen(
                    total = r.total,
                    correct = r.correct,
                    wrongIds = r.wrongIds,
                    contentPadding = innerPadding,
                    onRetry = {
                        @Suppress("ASSIGNED_VALUE_IS_NEVER_READ")
                        run {
                            val rr = quizResult!!
                            quizResult = null
                            startQuizWithIds(rr.textId, rr.wrongIds)
                        }
                    },
                    onBackHome = {
                        @Suppress("ASSIGNED_VALUE_IS_NEVER_READ")
                        run {
                            quizResult = null
                            selectedTextId = null
                            homeMode = HomeMode.MENU
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
                            quizTextId = null
                            quizQuestionIds = emptyList()
                        }
                    },
                    questionIds = quizQuestionIds.takeIf { it.isNotEmpty() },
                    onAnswerCommitted = { qid, isCorrect ->
                        quizLogStore.recordAnswer(qid, isCorrect)
                        progressVersion += 1
                    },
                    onFinish = { total, correct, wrongIds ->
                        @Suppress("ASSIGNED_VALUE_IS_NEVER_READ")
                        run {
                            quizResult = QuizResult(
                                textId = tid,
                                total = total,
                                correct = correct,
                                wrongIds = wrongIds
                            )
                            quizTextId = null
                            quizQuestionIds = emptyList()
                        }
                    }
                )
            }

            selected != null -> {
                TextDetailScreen(
                    textItem = selected,
                    contentPadding = innerPadding,
                    onBack = {
                        @Suppress("ASSIGNED_VALUE_IS_NEVER_READ")
                        run { selectedTextId = null }
                    },
                    onStartQuiz = {
                        @Suppress("ASSIGNED_VALUE_IS_NEVER_READ")
                        run {
                            quizResult = null
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

private data class QuizResult(
    val textId: String,
    val total: Int,
    val correct: Int,
    val wrongIds: List<String>
)

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
    val phrases = remember {
        listOf(
            "学ぼうとする\n姿勢が素晴らしい！\n拍手が止まらない！",
            "今日の1問が\n未来を変えるよ！",
            "コツコツが\nいちばん強い！",
            "小さな積み重ねが\n大きな力になるよ!"
        )
    }
    // 画面表示ごとに1つ固定（再コンポーズで変わりにくい）
    val phrase = remember { phrases[Random.nextInt(phrases.size)] }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // キャラクター + 吹き出し（共通コンポーネント）
        CharacterSpeechBubbleView(
            characterImage1 = R.drawable.nicosme_normal, // 後でキャラ画像に差し替え
            characterImage2 = R.drawable.nicosme_run, // 2コマ目（仮）。アニメ不要なら null に。
            durationMillis = 1600L,
            text = phrase,
            modifier = Modifier.fillMaxWidth(),
            characterSize = 120.dp
        )

        Spacer(Modifier.height(6.dp))

        HomeMenuCard(
            icon = Icons.Filled.School,
            title = "カリキュラムで学ぶ",
            subtitle = "カリキュラムで学べば合格までアテンドするよ",
            backgroundColor = Color(0xFFFF8A1E),
            onClick = onCurriculum
        )

        HomeMenuCard(
            icon = Icons.AutoMirrored.Filled.MenuBook,
            title = "自分で学ぶ",
            subtitle = "やりたいことを、 自分で選べる",
            backgroundColor = Color(0xFF22C55E),
            onClick = onFreeStudy
        )

        HomeMenuCard(
            icon = Icons.AutoMirrored.Filled.Assignment,
            title = "模擬テストで学ぶ",
            subtitle = "模擬テストで自分の実力を試してみよう",
            backgroundColor = Color(0xFF1E88FF),
            onClick = onMock
        )

        Spacer(Modifier.height(4.dp))
        Text(
            "まずは『自分で学ぶ』から繋いであります。『カリキュラム』『模擬テスト』は次に実装していきましょう。",
            style = MaterialTheme.typography.bodySmall
        )
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
            .height(118.dp)
            .clickable { onClick() },
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }

            Box(
                modifier = Modifier.width(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(">", color = Color.White, style = MaterialTheme.typography.titleLarge)
            }
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