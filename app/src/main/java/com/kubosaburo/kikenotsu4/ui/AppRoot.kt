@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.kubosaburo.kikenotsu4.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kubosaburo.kikenotsu4.R
import com.kubosaburo.kikenotsu4.data.AssetRepository
import com.kubosaburo.kikenotsu4.data.BookmarkStore
import com.kubosaburo.kikenotsu4.data.QuizLogStore
import com.kubosaburo.kikenotsu4.data.QuizQuestion
import com.kubosaburo.kikenotsu4.data.TextItem
import com.kubosaburo.kikenotsu4.ui.screens.BookmarkScreen
import com.kubosaburo.kikenotsu4.ui.screens.FreeStudyHomeScreen
import com.kubosaburo.kikenotsu4.ui.screens.HomeMenuScreen
import com.kubosaburo.kikenotsu4.ui.screens.ProgressScreen
import com.kubosaburo.kikenotsu4.ui.screens.QuizScreen
import com.kubosaburo.kikenotsu4.ui.screens.SectionCelebrationScreen
import com.kubosaburo.kikenotsu4.ui.screens.TextDetailScreen
import com.kubosaburo.kikenotsu4.ui.screens.TextListScreen
import com.kubosaburo.kikenotsu4.ui.screens.SearchScreen

private enum class BottomTab { HOME, PROGRESS, SETTINGS }
private enum class HomeMode { MENU, FREE_STUDY, CURRICULUM, MOCK }
private enum class FreeStudyMode { HOME, TEXT_LIST, BOOKMARKS, SEARCH }

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val quizLogStore = remember { QuizLogStore(context) }
    val bookmarkStore = remember { BookmarkStore(context) }
    var bookmarkedTextIds by rememberSaveable { mutableStateOf<Set<String>>(emptySet()) }

    var texts by remember { mutableStateOf<List<TextItem>>(emptyList()) }
    var questions by remember { mutableStateOf<List<QuizQuestion>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    var selectedTab by rememberSaveable { mutableStateOf(BottomTab.HOME) }
    var homeMode by rememberSaveable { mutableStateOf(HomeMode.MENU) }
    var freeStudyMode by rememberSaveable { mutableStateOf(FreeStudyMode.HOME) }

    var selectedTextId by rememberSaveable { mutableStateOf<String?>(null) }
    var quizTextId by rememberSaveable { mutableStateOf<String?>(null) }
    var quizQuestionIds by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }

    var celebrationMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var celebrationTextId by rememberSaveable { mutableStateOf<String?>(null) }
    var celebrationIsCurriculum by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        runCatching {
            texts = AssetRepository.loadTexts(context)
            questions = AssetRepository.loadQuestions(context)

            val q0 = questions.firstOrNull()
            if (q0 != null) {
                Log.d("AppRoot", "loaded q0.id=${q0.id} len=${q0.question.length} q='${q0.question}'")
            } else {
                Log.d("AppRoot", "loaded questions is empty")
            }

            bookmarkedTextIds = bookmarkStore.loadBookmarkedTextIds().toSet()
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
                                homeMode == HomeMode.FREE_STUDY && freeStudyMode == FreeStudyMode.BOOKMARKS -> "ブックマーク"
                                homeMode == HomeMode.FREE_STUDY && freeStudyMode == FreeStudyMode.SEARCH -> "検索"
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
                        celebrationMessage != null && celebrationTextId != null -> {
                            val tid = celebrationTextId!!
                            IconButton(
                                onClick = {
                                    selectedTextId = tid
                                    celebrationMessage = null
                                    celebrationTextId = null
                                    celebrationIsCurriculum = false
                                },
                                modifier = Modifier
                                    .padding(start = 12.dp)
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "テキストへ戻る",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        quizTextId != null -> {
                            val tid = quizTextId!!
                            IconButton(
                                onClick = {
                                    selectedTextId = tid
                                    quizTextId = null
                                    quizQuestionIds = emptyList()
                                },
                                modifier = Modifier
                                    .padding(start = 12.dp)
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "テキストへ戻る",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        selected != null -> {
                            IconButton(
                                onClick = { selectedTextId = null },
                                modifier = Modifier
                                    .padding(start = 12.dp)
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "一覧へ戻る",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        homeMode == HomeMode.FREE_STUDY && freeStudyMode == FreeStudyMode.TEXT_LIST -> {
                            IconButton(
                                onClick = { freeStudyMode = FreeStudyMode.HOME },
                                modifier = Modifier
                                    .padding(start = 12.dp)
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "自分で学ぶへ戻る",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        homeMode == HomeMode.FREE_STUDY && freeStudyMode == FreeStudyMode.BOOKMARKS -> {
                            IconButton(
                                onClick = { freeStudyMode = FreeStudyMode.HOME },
                                modifier = Modifier
                                    .padding(start = 12.dp)
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "自分で学ぶへ戻る",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        homeMode == HomeMode.FREE_STUDY && freeStudyMode == FreeStudyMode.SEARCH -> {
                            IconButton(
                                onClick = { freeStudyMode = FreeStudyMode.HOME },
                                modifier = Modifier
                                    .padding(start = 12.dp)
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "自分で学ぶへ戻る",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == BottomTab.HOME,
                    onClick = {
                        selectedTab = BottomTab.HOME
                        homeMode = HomeMode.MENU
                        selectedTextId = null
                        quizTextId = null
                        quizQuestionIds = emptyList()
                        celebrationMessage = null
                        celebrationTextId = null
                        celebrationIsCurriculum = false
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
            selectedTab == BottomTab.PROGRESS -> {
                ProgressScreen(
                    quizLogStore = quizLogStore,
                    onBack = { selectedTab = BottomTab.HOME }
                )
            }

            selectedTab == BottomTab.SETTINGS -> {
                SettingsScreen(contentPadding = innerPadding)
            }

            error != null -> {
                Text("読み込みに失敗しました: $error")
            }

            celebrationMessage != null && celebrationTextId != null -> {
                val msg = celebrationMessage!!
                SectionCelebrationScreen(
                    message = msg,
                    isCurriculum = celebrationIsCurriculum,
                    onTapNext = {
                        selectedTextId = null
                        homeMode = HomeMode.FREE_STUDY
                        freeStudyMode = FreeStudyMode.TEXT_LIST
                        celebrationMessage = null
                        celebrationTextId = null
                        celebrationIsCurriculum = false
                    }
                )
            }

            quizTextId != null -> {
                val tid = quizTextId!!
                QuizScreen(
                    textId = tid,
                    allQuestions = questions,
                    contentPadding = innerPadding,
                    onBack = {
                        selectedTextId = tid
                        quizTextId = null
                        quizQuestionIds = emptyList()
                    },
                    questionIds = quizQuestionIds.takeIf { it.isNotEmpty() },
                    onAnswerCommitted = { qid, isCorrect ->
                        quizLogStore.recordAnswer(qid, isCorrect)
                    },
                    onShowCelebration = { total, correct, _ ->
                        val allCorrect = (correct == total)
                        celebrationTextId = tid
                        celebrationIsCurriculum = false
                        celebrationMessage = if (allCorrect) {
                            "全問正解！最高！🎉"
                        } else {
                            "おつかれさま！よく頑張ったね！🎉"
                        }
                        quizTextId = null
                        quizQuestionIds = emptyList()
                    },
                    onFinish = { _, _, _ -> }
                )
            }

            selected != null -> {
                TextDetailScreen(
                    textItem = selected,
                    contentPadding = innerPadding,
                    onStartQuiz = { tid: String ->
                        quizQuestionIds = emptyList()
                        quizTextId = tid
                    },
                    isBookmarked = bookmarkedTextIds.contains(selected.id),
                    onToggleBookmark = {
                        bookmarkStore.toggle(selected.id)
                        bookmarkedTextIds = bookmarkStore.loadBookmarkedTextIds().toSet()
                    }
                )
            }

            homeMode == HomeMode.FREE_STUDY -> {
                when (freeStudyMode) {
                    FreeStudyMode.HOME -> {
                        FreeStudyHomeScreen(
                            contentPadding = innerPadding,
                            onTextQuiz = { freeStudyMode = FreeStudyMode.TEXT_LIST },
                            onBookmarks = { freeStudyMode = FreeStudyMode.BOOKMARKS },
                            onTodayReview = { homeMode = HomeMode.MENU },
                            onSearch = { freeStudyMode = FreeStudyMode.SEARCH },
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
                                onOpen = { tid: String -> selectedTextId = tid }
                            )
                        }
                    }

                    FreeStudyMode.BOOKMARKS -> {
                        BookmarkScreen(
                            contentPadding = innerPadding,
                            texts = texts,
                            bookmarkStore = bookmarkStore,
                            onOpenText = { tid -> selectedTextId = tid },
                            onGoToTextList = { freeStudyMode = FreeStudyMode.TEXT_LIST }
                        )
                    }

                    FreeStudyMode.SEARCH -> {
                        SearchScreen(
                            contentPadding = innerPadding,
                            texts = texts,
                            onOpenText = { tid -> selectedTextId = tid }
                        )
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
                    onGoCurriculum = { homeMode = HomeMode.CURRICULUM },
                    onGoFreeStudy = {
                        homeMode = HomeMode.FREE_STUDY
                        freeStudyMode = FreeStudyMode.HOME
                    },
                    onGoMock = { homeMode = HomeMode.MOCK }
                )
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
        Button(onClick = onBack) { Text("戻る") }
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