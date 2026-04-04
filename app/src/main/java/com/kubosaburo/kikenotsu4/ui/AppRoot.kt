@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@file:Suppress(
    "ASSIGNED_VALUE_IS_NEVER_READ",
    "UNUSED_VARIABLE"
)
package com.kubosaburo.kikenotsu4.ui
import com.kubosaburo.kikenotsu4.data.MockTestResultStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.mutableIntStateOf
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
import com.kubosaburo.kikenotsu4.data.CurriculumRoot
import com.kubosaburo.kikenotsu4.data.CurriculumProgressStore
import com.kubosaburo.kikenotsu4.data.CurriculumSection
import com.kubosaburo.kikenotsu4.data.CurriculumSectionType
import com.kubosaburo.kikenotsu4.data.textIdToCurriculumChapterDescriptionMap
import com.kubosaburo.kikenotsu4.data.ProManager
import com.kubosaburo.kikenotsu4.data.DailyTextLimitStore
import com.kubosaburo.kikenotsu4.ui.screens.BookmarkScreen
import com.kubosaburo.kikenotsu4.ui.screens.FreeStudyHomeScreen
import com.kubosaburo.kikenotsu4.ui.screens.HomeMenuScreen
import com.kubosaburo.kikenotsu4.ui.screens.ProgressScreen
import com.kubosaburo.kikenotsu4.ui.screens.QuizScreen
import com.kubosaburo.kikenotsu4.ui.screens.SectionCelebrationScreen
import com.kubosaburo.kikenotsu4.ui.screens.TextDetailScreen
import com.kubosaburo.kikenotsu4.ui.screens.TextListScreen
import com.kubosaburo.kikenotsu4.ui.screens.SearchScreen
import com.kubosaburo.kikenotsu4.ui.screens.CurriculumHomeScreen
import com.kubosaburo.kikenotsu4.ui.screens.ReviewIntroScreen
import com.kubosaburo.kikenotsu4.ui.screens.ReviewCompletionScreen
import com.kubosaburo.kikenotsu4.ui.screens.FinalCelebrationScreen
import com.kubosaburo.kikenotsu4.ui.screens.finalCelebrationCopyForCurriculumLap
import com.kubosaburo.kikenotsu4.ui.screens.SettingsScreen as RealSettingsScreen
import com.kubosaburo.kikenotsu4.ui.screens.MockTestHomeScreen
import com.kubosaburo.kikenotsu4.ui.screens.MockTestSessionScreen
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

private enum class BottomTab { HOME, PROGRESS, SETTINGS }
private enum class HomeMode { MENU, FREE_STUDY, CURRICULUM, MOCK }
private enum class FreeStudyMode { HOME, TEXT_LIST, BOOKMARKS, SEARCH }

/** カリキュラムのセクションを開いた結果（上限ブロック時はホームメニューへ戻す判定に使う）。 */
private enum class CurriculumOpenOutcome {
    Opened,
    BlockedByDailyLimit,
    Failed,
}

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val quizLogStore = remember { QuizLogStore(context) }
    val bookmarkStore = remember { BookmarkStore(context) }
    val proManager = remember { ProManager(context) }

    val freeDailyTextLimit = 2

    var bookmarkedTextIds by rememberSaveable { mutableStateOf<Set<String>>(emptySet()) }

    var texts by remember { mutableStateOf<List<TextItem>>(emptyList()) }
    var questions by remember { mutableStateOf<List<QuizQuestion>>(emptyList()) }
    var curriculum by remember { mutableStateOf<CurriculumRoot?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var curriculumError by rememberSaveable { mutableStateOf<String?>(null) }
    var curriculumNextSectionId by rememberSaveable { mutableStateOf<String?>(null) }
    var curriculumCurrentSectionId by rememberSaveable { mutableStateOf<String?>(null) }

    var selectedTab by rememberSaveable { mutableStateOf(BottomTab.HOME) }
    var homeMode by rememberSaveable { mutableStateOf(HomeMode.MENU) }
    var freeStudyMode by rememberSaveable { mutableStateOf(FreeStudyMode.HOME) }

    var selectedTextId by rememberSaveable { mutableStateOf<String?>(null) }
    var quizTextId by rememberSaveable { mutableStateOf<String?>(null) }
    var quizQuestionIds by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }

    var celebrationMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var celebrationTextId by rememberSaveable { mutableStateOf<String?>(null) }
    var celebrationIsCurriculum by rememberSaveable { mutableStateOf(false) }
    var showFinalCelebration by rememberSaveable { mutableStateOf(false) }
    /** DEBUG: 0 以外のとき [finalCelebrationCopyForCurriculumLap] にこの lap を渡す（保存値は変えない）。 */
    var debugFinalCelebrationLapOverride by rememberSaveable { mutableIntStateOf(0) }
    var forceShowHomeRoot by rememberSaveable { mutableStateOf(false) }

    // ✅ Curriculum Auto Review (SRS)
    var isAutoReview by rememberSaveable { mutableStateOf(false) }

    // ✅ Review intro (show before starting auto-review quiz)
    var showReviewIntro by rememberSaveable { mutableStateOf(false) }
    var reviewIntroIds by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var showReviewCompletion by rememberSaveable { mutableStateOf(false) }
// ✅ 現在の復習セッションID（戻る用に保持）
    var activeReviewIds by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var isFreeStudyTodayReview by rememberSaveable { mutableStateOf(false) }
    var showNoTodayReviewDialog by rememberSaveable { mutableStateOf(false) }
    var curriculumTextOpenedFromResume by rememberSaveable { mutableStateOf(false) }
    var showMockTestSession by rememberSaveable { mutableStateOf(false) }
    var showMockTestHome by rememberSaveable { mutableStateOf(false) }

    // セクション完了後に表示するインタースティシャル広告
    var sectionInterstitialAd by remember { mutableStateOf<InterstitialAd?>(null) }
    var isLoadingSectionInterstitial by remember { mutableStateOf(false) }

    // 無料版の「本日の上限に達しました」ダイアログ
    var showDailyTextLimitDialog by rememberSaveable { mutableStateOf(false) }

    fun fetchDueReviewIds(
        @Suppress("UNUSED_PARAMETER") context: Context,
        maxCount: Int = Int.MAX_VALUE,
    ): List<String> =
        quizLogStore.fetchDueQuestionIds(maxCount)

    fun mapQuestionIdsToTextId(ids: List<String>, fallbackTextId: String?): String? {
        val safeIds = ids.ifEmpty { return fallbackTextId }
        val firstId = safeIds.first()
        return questions.firstOrNull { it.id == firstId }?.textId ?: fallbackTextId
    }

    fun resetToHomeRoot() {
        // switch to Home first so we never fall back to Settings on the next recomposition
        selectedTab = BottomTab.HOME
        forceShowHomeRoot = true
        homeMode = HomeMode.MENU
        freeStudyMode = FreeStudyMode.HOME

        selectedTextId = null
        quizTextId = null
        quizQuestionIds = emptyList()

        celebrationMessage = null
        celebrationTextId = null
        celebrationIsCurriculum = false

        showReviewIntro = false
        reviewIntroIds = emptyList()
        showReviewCompletion = false
        activeReviewIds = emptyList()
        isFreeStudyTodayReview = false
        showNoTodayReviewDialog = false
        isAutoReview = false

        curriculumError = null

        showFinalCelebration = false
        debugFinalCelebrationLapOverride = 0
        curriculumTextOpenedFromResume = false
        showMockTestSession = false
        showMockTestHome = false
    }


    LaunchedEffect(Unit) {
        runCatching {
            texts = AssetRepository.loadTexts(context)
            questions = AssetRepository.loadQuestions(context)
            curriculum = AssetRepository.loadCurriculum(context)

            curriculumNextSectionId = CurriculumProgressStore.loadNextSectionId(context)

            bookmarkedTextIds = bookmarkStore.loadBookmarkedTextIds().toSet()
            proManager.refresh()
        }.onFailure {
            error = it.message ?: it.toString()
        }
    }

    val selected: TextItem? = selectedTextId?.let { id -> texts.firstOrNull { it.id == id } }

    fun formatMockResultText(result: MockTestResultStore.Result?): String? {
        if (result == null) return null
        val percent = if (result.total > 0) (result.correct * 100 / result.total) else 0
        return "${result.correct} / ${result.total}問 正解（${percent}%）"
    }

    fun formatMockDateText(result: MockTestResultStore.Result?): String? {
        if (result == null) return null
        val formatter = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN)
        return formatter.format(Date(result.finishedAtMillis))
    }

    fun flattenedCurriculumSections(): List<CurriculumSection> {
        return curriculum?.chapters?.flatMap { it.sections } ?: emptyList()
    }

    fun totalSectionCount(): Int {
        return flattenedCurriculumSections().size
    }

    fun completedSectionCount(): Int {
        val allSections = flattenedCurriculumSections()
        if (allSections.isEmpty()) return 0

        val nextId = curriculumNextSectionId
        if (nextId == null) {
            // 続き ID が無い状態は「未着手」と「全セクション完了後（clear 済み）」の両方あり得る。
            // 初回インストールは lap 既定値 1 のままなので未着手 → 0。1 周以上完了済みは lap が 2 以上。
            val lap = CurriculumProgressStore.loadLap(context)
            return if (lap <= 1) 0 else allSections.size
        }

        val nextIndex = allSections.indexOfFirst { it.id == nextId }
        return if (nextIndex >= 0) nextIndex else 0
    }

    fun findCurriculumSection(sectionId: String): CurriculumSection? {
        val root = curriculum ?: return null
        for (ch in root.chapters) {
            for (sec in ch.sections) {
                if (sec.id == sectionId) return sec
            }
        }
        return null
    }

    /**
     * クイズセクションの直前にあるテキストセクション（curriculum.json の text.nextId == quizSectionId）。
     * 再開時にテキスト画面だけ先に出す場合、current をクイズIDのままにすると
     * 「このテキストの問題へ」で quiz.nextId（次章テキスト等）へ誤遷移するため使う。
     */
    fun findTextSectionPrecedingQuiz(quizSectionId: String): CurriculumSection? {
        return flattenedCurriculumSections().firstOrNull { sec ->
            sec.type == CurriculumSectionType.TEXT && sec.nextId == quizSectionId
        }
    }

    fun findTextSectionByRefId(textId: String): CurriculumSection? {
        return flattenedCurriculumSections().firstOrNull { sec ->
            sec.type == CurriculumSectionType.TEXT && sec.refId == textId
        }
    }

    fun loadSectionInterstitialIfNeeded() {
        if (proManager.isProEnabled) return
        if (sectionInterstitialAd != null || isLoadingSectionInterstitial) return

        isLoadingSectionInterstitial = true
        val adRequest = AdRequest.Builder().build()

        val adUnitId = try {
            context.getString(R.string.admob_interstitial_ad_unit_id)
        } catch (e: Exception) {
            Log.e("KikenOtsuAds", "admob_interstitial_ad_unit_id missing", e)
            isLoadingSectionInterstitial = false
            return
        }

        InterstitialAd.load(
            context,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    sectionInterstitialAd = ad
                    isLoadingSectionInterstitial = false
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    sectionInterstitialAd = null
                    isLoadingSectionInterstitial = false
                    Log.w(
                        "KikenOtsuAds",
                        "Interstitial failed: code=${adError.code} message=${adError.message} domain=${adError.domain}",
                    )
                }
            }
        )
    }

    // クイズ中に先読みしておき、セクション祝画面の「次へ」までにインタースティシャルが載りやすくする
    LaunchedEffect(quizTextId) {
        if (quizTextId != null) {
            loadSectionInterstitialIfNeeded()
        }
    }

    fun openCurriculumSection(sectionId: String, sectionType: String, refId: String): CurriculumOpenOutcome {
        curriculumError = null

        // 無料版は「テキスト問題（お祝い到達）」が1日2問まで。
        // その上限を超えるセクション開始はブロックする（ただし今日すでに解いた問題は許可）。
        if (!proManager.isProEnabled && (sectionType == CurriculumSectionType.TEXT || sectionType == CurriculumSectionType.QUIZ)) {
            val targetTextId: String? = when (sectionType) {
                CurriculumSectionType.TEXT -> refId
                CurriculumSectionType.QUIZ -> {
                    val qForGroup = questions.filter { it.groupId == refId }
                    qForGroup.firstOrNull()?.textId
                }
                else -> null
            }

            if (targetTextId != null &&
                DailyTextLimitStore.isLimitReached(context, freeDailyTextLimit) &&
                !DailyTextLimitStore.hasCompletedText(context, targetTextId)
            ) {
                showDailyTextLimitDialog = true
                return CurriculumOpenOutcome.BlockedByDailyLimit
            }
        }

        curriculumNextSectionId = sectionId
        CurriculumProgressStore.saveNextSectionId(context, sectionId)
        curriculumCurrentSectionId = sectionId

        when (sectionType) {
            CurriculumSectionType.TEXT -> {
                // refId is textId like "text_001"
                selectedTextId = refId
            }
            CurriculumSectionType.QUIZ -> {
                // refId is groupId like "g067". QuizScreen currently expects a textId,
                // so we map groupId -> first question's textId and also pass questionIds to limit the quiz.
                val qForGroup = questions.filter { it.groupId == refId }
                val firstQ = qForGroup.ifEmpty {
                    curriculumError = "クイズの参照IDが見つかりません: $refId"
                    return CurriculumOpenOutcome.Failed
                }.first()

                val mappedTextId = firstQ.textId
                quizQuestionIds = qForGroup.map { it.id }
                quizTextId = mappedTextId
            }
            else -> {
                curriculumError = "未対応のセクション種別: $sectionType"
                return CurriculumOpenOutcome.Failed
            }
        }
        return CurriculumOpenOutcome.Opened
    }

    /**
     * カリキュラムの先頭テキストセクションを開く（ホームの「カリキュラムで学ぶ」や最終お祝いからの再開用）。
     * @return 開けたら true（カリキュラムが空などで開けなければ false）
     */
    fun openFirstCurriculumTextSection(): Boolean {
        val all = flattenedCurriculumSections()
        val first = all.firstOrNull { it.type == CurriculumSectionType.TEXT }
            ?: all.firstOrNull()
            ?: return false
        return openCurriculumSection(first.id, first.type, first.refId) == CurriculumOpenOutcome.Opened
    }

    fun advanceCurriculumFromCurrentSection() {
        val curId = curriculumCurrentSectionId ?: return
        val sec = findCurriculumSection(curId) ?: return
        val nextId = sec.nextId

        // ✅ advance both "next" and "current" pointers
        curriculumNextSectionId = nextId
        curriculumCurrentSectionId = nextId

        if (nextId == null) {
            // 最後のセクションまで到達したら周回を進める（次に 0/N と並べて「2周目」等を表示）
            CurriculumProgressStore.incrementLapAfterFullCurriculumRound(context)
            CurriculumProgressStore.clear(context)
            curriculumCurrentSectionId = null
        } else {
            CurriculumProgressStore.saveNextSectionId(context, nextId)
        }
    }

    fun openSavedCurriculumOrHome() {
        curriculumError = null

        val nextId = curriculumNextSectionId
        if (nextId == null) {
            // 「自分で学ぶ」で章を選べるので、続きなしのときは章一覧ではなく常に先頭テキストから
            selectedTab = BottomTab.HOME
            forceShowHomeRoot = false
            freeStudyMode = FreeStudyMode.HOME
            curriculumTextOpenedFromResume = false
            val all = flattenedCurriculumSections()
            val first = all.firstOrNull { it.type == CurriculumSectionType.TEXT }
                ?: all.firstOrNull()
            if (first == null) {
                homeMode = HomeMode.CURRICULUM
                return
            }
            homeMode = when (openCurriculumSection(first.id, first.type, first.refId)) {
                CurriculumOpenOutcome.BlockedByDailyLimit -> HomeMode.MENU
                else -> HomeMode.CURRICULUM
            }
            return
        }

        val dueIds = fetchDueReviewIds(context)
        if (dueIds.isNotEmpty()) {
            isFreeStudyTodayReview = false
            showReviewIntro = true
            reviewIntroIds = dueIds
            activeReviewIds = emptyList()
            selectedTab = BottomTab.HOME
            homeMode = HomeMode.CURRICULUM
            freeStudyMode = FreeStudyMode.HOME
            return
        }

        val sec = findCurriculumSection(nextId)
        if (sec == null) {
            CurriculumProgressStore.clear(context)
            curriculumNextSectionId = null
            curriculumCurrentSectionId = null
            selectedTab = BottomTab.HOME
            forceShowHomeRoot = false
            freeStudyMode = FreeStudyMode.HOME
            curriculumTextOpenedFromResume = false
            val all = flattenedCurriculumSections()
            val first = all.firstOrNull { it.type == CurriculumSectionType.TEXT }
                ?: all.firstOrNull()
            if (first == null) {
                homeMode = HomeMode.CURRICULUM
                curriculumError = "続きのセクションが見つかりません: $nextId"
                return
            }
            val reopenOutcome = openCurriculumSection(first.id, first.type, first.refId)
            if (reopenOutcome == CurriculumOpenOutcome.Failed) {
                curriculumError = "続きのセクションが見つかりません: $nextId"
            }
            homeMode = when (reopenOutcome) {
                CurriculumOpenOutcome.BlockedByDailyLimit -> HomeMode.MENU
                else -> HomeMode.CURRICULUM
            }
            return
        }

        homeMode = HomeMode.CURRICULUM
        freeStudyMode = FreeStudyMode.HOME
        curriculumTextOpenedFromResume = true

        // ホームから「カリキュラムで学ぶ」に入るときは、quiz セクション保存中でも
        // いきなり問題画面へ飛ばさず、対応するテキスト画面から再開する。
        if (sec.type == CurriculumSectionType.QUIZ) {
            val qForGroup = questions.filter { it.groupId == sec.refId }
            val firstQ = qForGroup.firstOrNull()
            if (firstQ != null) {
                val textSec = findTextSectionPrecedingQuiz(sec.id)
                    ?: findTextSectionByRefId(firstQ.textId)
                curriculumCurrentSectionId = textSec?.id ?: sec.id
                selectedTextId = firstQ.textId
                return
            }
        }

        val openOutcome = openCurriculumSection(sec.id, sec.type, sec.refId)
        if (openOutcome == CurriculumOpenOutcome.BlockedByDailyLimit) {
            homeMode = HomeMode.MENU
            curriculumTextOpenedFromResume = false
        }
    }

    /**
     * カリキュラムでクイズ画面からテキストへ戻したとき、current/next がクイズ ID のまま残ると
     * 「問題へ」で [findCurriculumSection] がクイズを text 扱いし、next が次章になってしまう。テキストセクションに戻す。
     */
    fun restoreCurriculumPointersToTextSection(textId: String) {
        if (homeMode != HomeMode.CURRICULUM) return
        val textSec = findTextSectionByRefId(textId) ?: return
        curriculumCurrentSectionId = textSec.id
        curriculumNextSectionId = textSec.id
        CurriculumProgressStore.saveNextSectionId(context, textSec.id)
    }

    fun closeTextDetailToPreviousFlow() {
        selectedTextId = null
        quizTextId = null
        quizQuestionIds = emptyList()
        celebrationMessage = null
        celebrationTextId = null

        if (curriculumCurrentSectionId != null) {
            if (curriculumTextOpenedFromResume) {
                curriculumTextOpenedFromResume = false
                resetToHomeRoot()
            } else {
                selectedTab = BottomTab.HOME
                forceShowHomeRoot = false
                homeMode = HomeMode.CURRICULUM
                freeStudyMode = FreeStudyMode.HOME
            }
        }
    }


    fun topBarTextProgressParts(): Pair<String, String?> {
        val total = texts.size
        val id = selectedTextId
        if (id == null || total <= 0) return "テキスト" to null
        val idx0 = texts.indexOfFirst { it.id == id }
        val current = if (idx0 >= 0) idx0 + 1 else 0
        val sub = if (current > 0) "$current / $total" else "- / $total"
        return "テキスト" to sub
    }

    fun handleSystemBack() {
        val activity = context as? Activity
        when {
            showDailyTextLimitDialog -> showDailyTextLimitDialog = false
            showNoTodayReviewDialog -> showNoTodayReviewDialog = false
            showFinalCelebration -> {
                debugFinalCelebrationLapOverride = 0
                showFinalCelebration = false
                resetToHomeRoot()
            }
            showMockTestSession -> {
                proManager.refresh()
                showMockTestSession = false
                showMockTestHome = true
                forceShowHomeRoot = false
                selectedTab = BottomTab.HOME
                homeMode = HomeMode.MOCK
                freeStudyMode = FreeStudyMode.HOME
                selectedTextId = null
                quizTextId = null
                quizQuestionIds = emptyList()
                celebrationMessage = null
                celebrationTextId = null
            }
            showReviewCompletion -> {
                showReviewCompletion = false
                resetToHomeRoot()
            }
            showReviewIntro && reviewIntroIds.isNotEmpty() -> {
                showReviewIntro = false
                reviewIntroIds = emptyList()
                activeReviewIds = emptyList()
                isAutoReview = false
                if (isFreeStudyTodayReview) {
                    isFreeStudyTodayReview = false
                    selectedTab = BottomTab.HOME
                    homeMode = HomeMode.FREE_STUDY
                    freeStudyMode = FreeStudyMode.HOME
                }
            }
            celebrationMessage != null && celebrationTextId != null -> {
                val tid = celebrationTextId!!
                selectedTextId = tid
                celebrationMessage = null
                celebrationTextId = null
                celebrationIsCurriculum = false
            }
            quizTextId != null -> {
                val tid = quizTextId!!
                if (isAutoReview) {
                    quizTextId = null
                    quizQuestionIds = emptyList()
                    val ids = activeReviewIds.ifEmpty {
                        fetchDueReviewIds(context)
                    }
                    reviewIntroIds = ids
                    showReviewIntro = ids.isNotEmpty()
                    isAutoReview = false
                } else {
                    restoreCurriculumPointersToTextSection(tid)
                    selectedTextId = tid
                    quizTextId = null
                    quizQuestionIds = emptyList()
                }
            }
            selected != null -> closeTextDetailToPreviousFlow()
            selectedTab == BottomTab.SETTINGS -> {
                forceShowHomeRoot = false
                selectedTab = BottomTab.HOME
            }
            selectedTab == BottomTab.PROGRESS -> {
                forceShowHomeRoot = false
                selectedTab = BottomTab.HOME
            }
            homeMode == HomeMode.FREE_STUDY && freeStudyMode != FreeStudyMode.HOME -> {
                freeStudyMode = FreeStudyMode.HOME
            }
            homeMode == HomeMode.FREE_STUDY -> {
                homeMode = HomeMode.MENU
                freeStudyMode = FreeStudyMode.HOME
                selectedTab = BottomTab.HOME
                forceShowHomeRoot = false
            }
            homeMode == HomeMode.CURRICULUM -> {
                homeMode = HomeMode.MENU
                freeStudyMode = FreeStudyMode.HOME
                selectedTextId = null
                quizTextId = null
                quizQuestionIds = emptyList()
                curriculumError = null
            }
            showMockTestHome -> {
                showMockTestHome = false
                homeMode = HomeMode.MENU
                freeStudyMode = FreeStudyMode.HOME
                selectedTab = BottomTab.HOME
                forceShowHomeRoot = false
            }
            homeMode == HomeMode.MOCK -> {
                homeMode = HomeMode.MENU
                freeStudyMode = FreeStudyMode.HOME
                selectedTab = BottomTab.HOME
                forceShowHomeRoot = false
                showMockTestSession = false
                showMockTestHome = false
            }
            error != null -> {
                error = null
                resetToHomeRoot()
            }
            else -> activity?.moveTaskToBack(true)
        }
    }

    BackHandler { handleSystemBack() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    if (selected != null && selectedTab == BottomTab.HOME) {
                        val (_, sub) = topBarTextProgressParts()
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                sub ?: "-",
                                style = MaterialTheme.typography.titleSmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        val topTitle =
                            when {
                                showFinalCelebration -> "おめでとう！"
                                forceShowHomeRoot -> "危険物乙４学習アプリ"
                                celebrationMessage != null -> "お疲れさま！"
                                // クイズ画面はバックグラウンドに残り得るが、進捗/設定タブのときはそのタイトルを優先する
                                selectedTab == BottomTab.PROGRESS -> "進捗"
                                selectedTab == BottomTab.SETTINGS -> "設定"
                                quizTextId != null && isAutoReview -> "復習問題"
                                quizTextId != null -> "クイズ"
                                homeMode == HomeMode.MENU -> "危険物乙４学習アプリ"
                                homeMode == HomeMode.CURRICULUM -> "カリキュラム"
                                homeMode == HomeMode.MOCK -> "模擬テスト"
                                homeMode == HomeMode.FREE_STUDY && freeStudyMode == FreeStudyMode.TEXT_LIST -> "テキスト一覧"
                                homeMode == HomeMode.FREE_STUDY && freeStudyMode == FreeStudyMode.BOOKMARKS -> "ブックマーク"
                                homeMode == HomeMode.FREE_STUDY && freeStudyMode == FreeStudyMode.SEARCH -> "検索"
                                else -> "自分で学ぶ"
                            }

                        Text(
                            text = topTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
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

                        quizTextId != null && selectedTab == BottomTab.HOME -> {
                            val tid = quizTextId!!
                            IconButton(
                                onClick = {
                                    if (isAutoReview) {
                                        // ✅ 復習中：ReviewIntroScreenへ戻る
                                        quizTextId = null
                                        quizQuestionIds = emptyList()

                                        val ids = activeReviewIds.ifEmpty {
                                            fetchDueReviewIds(context)
                                        }

                                        reviewIntroIds = ids
                                        showReviewIntro = ids.isNotEmpty()

                                        // 復習モード解除（Introに戻ったので）
                                        isAutoReview = false
                                    } else {
                                        // ✅ 通常クイズ：テキストへ戻る
                                        restoreCurriculumPointersToTextSection(tid)
                                        selectedTextId = tid
                                        quizTextId = null
                                        quizQuestionIds = emptyList()
                                    }
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
                                onClick = {
                                    closeTextDetailToPreviousFlow()
                                },
                                modifier = Modifier
                                    .padding(start = 12.dp)
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "前の画面へ戻る",
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
                },
                actions = {
                    if (selected != null) {
                        IconButton(
                            onClick = {
                                bookmarkStore.toggle(selected.id)
                                bookmarkedTextIds = bookmarkStore.loadBookmarkedTextIds().toSet()
                            },
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            val isBookmarked = bookmarkedTextIds.contains(selected.id)
                            Icon(
                                imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = if (isBookmarked) "ブックマーク解除" else "ブックマーク追加",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        },

        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = forceShowHomeRoot || selectedTab == BottomTab.HOME,
                    onClick = {
                        resetToHomeRoot()
                    },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "ホーム") },
                    label = { Text("ホーム") }
                )
                NavigationBarItem(
                    selected = !forceShowHomeRoot && selectedTab == BottomTab.PROGRESS,
                    onClick = {
                        forceShowHomeRoot = false
                        selectedTab = BottomTab.PROGRESS
                    },
                    icon = { Icon(Icons.Filled.BarChart, contentDescription = "進捗") },
                    label = { Text("進捗") }
                )
                NavigationBarItem(
                    selected = !forceShowHomeRoot && selectedTab == BottomTab.SETTINGS,
                    onClick = {
                        forceShowHomeRoot = false
                        selectedTab = BottomTab.SETTINGS
                    },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "設定") },
                    label = { Text("設定") }
                )
            }
        }
    ) { innerPadding ->
        if (showDailyTextLimitDialog) {
            AlertDialog(
                onDismissRequest = { showDailyTextLimitDialog = false },
                title = {
                    Text(
                        text = "本日の上限に達しました",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "無料版ではテキスト問題が1日2問までです。続けるには有料版が必要です。",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showDailyTextLimitDialog = false }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDailyTextLimitDialog = false
                            selectedTab = BottomTab.SETTINGS
                            forceShowHomeRoot = false
                            homeMode = HomeMode.MENU
                            freeStudyMode = FreeStudyMode.HOME
                        }
                    ) {
                        Text("有料版にする")
                    }
                }
            )
        }

        when {
            showFinalCelebration -> {
                LaunchedEffect(Unit) {
                    selectedTab = BottomTab.HOME
                    homeMode = HomeMode.MENU
                    freeStudyMode = FreeStudyMode.HOME
                }

                val lapForFinalCelebrationCopy =
                    if (debugFinalCelebrationLapOverride != 0) {
                        debugFinalCelebrationLapOverride
                    } else {
                        CurriculumProgressStore.loadLap(context)
                    }
                val (finalCelebrationTitle, finalCelebrationMessage) =
                    finalCelebrationCopyForCurriculumLap(lapForFinalCelebrationCopy)
                FinalCelebrationScreen(
                    contentPadding = innerPadding,
                    title = finalCelebrationTitle,
                    message = finalCelebrationMessage,
                    onRestartFromFirst = {
                        debugFinalCelebrationLapOverride = 0
                        showFinalCelebration = false
                        celebrationMessage = null
                        celebrationTextId = null
                        celebrationIsCurriculum = false
                        curriculumTextOpenedFromResume = false
                        curriculumError = null
                        showReviewIntro = false
                        reviewIntroIds = emptyList()
                        showReviewCompletion = false
                        activeReviewIds = emptyList()
                        isAutoReview = false
                        quizTextId = null
                        quizQuestionIds = emptyList()
                        selectedTab = BottomTab.HOME
                        forceShowHomeRoot = false
                        homeMode = HomeMode.CURRICULUM
                        freeStudyMode = FreeStudyMode.HOME
                        if (!openFirstCurriculumTextSection()) {
                            resetToHomeRoot()
                        }
                    },
                    onGoHome = {
                        resetToHomeRoot()
                    }
                )
            }
            selectedTab == BottomTab.SETTINGS -> {
                RealSettingsScreen(
                    contentPadding = innerPadding,
                    isProEnabled = proManager.isProEnabled,
                    isProBusy = proManager.isBusy,
                    proErrorMessage = proManager.lastErrorMessage,
                    onProPurchase = { proManager.purchase() },
                    onProRestore = { proManager.restore() },
                    onProModeChanged = {
                        proManager.refresh()
                    },
                    onDebugMarkProPurchased = { proManager.markPurchasedForLocalDebug() },
                    onDebugClearProPurchased = { proManager.clearPurchasedForLocalDebug() },
                    onLearningDataCleared = {
                        curriculumNextSectionId = CurriculumProgressStore.loadNextSectionId(context)
                        bookmarkedTextIds = bookmarkStore.loadBookmarkedTextIds().toSet()
                    },
                    onDebugOpenFinalCelebration = {
                        debugFinalCelebrationLapOverride = 0
                        showFinalCelebration = true
                    },
                    onDebugPreviewFinalCelebrationCopyLap = { lapForCopy ->
                        debugFinalCelebrationLapOverride = lapForCopy
                        showFinalCelebration = true
                    },
                )
            }
            selectedTab == BottomTab.PROGRESS -> {
                ProgressScreen(
                    quizLogStore = quizLogStore,
                    completedSectionCount = completedSectionCount(),
                    totalSectionCount = totalSectionCount(),
                    curriculumLap = CurriculumProgressStore.loadLap(context),
                    contentPadding = innerPadding,
                )
            }
            showMockTestSession -> {
                LaunchedEffect(Unit) {
                    proManager.refresh()
                }
                MockTestSessionScreen(
                    contentPadding = innerPadding,
                    onBack = {
                        proManager.refresh()
                        showMockTestSession = false
                        showMockTestHome = true
                        forceShowHomeRoot = false
                        selectedTab = BottomTab.HOME
                        homeMode = HomeMode.MOCK
                        freeStudyMode = FreeStudyMode.HOME
                        selectedTextId = null
                        quizTextId = null
                        quizQuestionIds = emptyList()
                        celebrationMessage = null
                        celebrationTextId = null
                    },
                    isPro = proManager.isProEnabled
                )
            }
            showMockTestHome -> {
                LaunchedEffect(Unit) {
                    proManager.refresh()
                }

                val latestTrialResult = remember(showMockTestHome) {
                    MockTestResultStore.loadLatest(context, "mock_trial_fixed")
                }
                val latestRandomResult = remember(showMockTestHome) {
                    MockTestResultStore.loadLatest(context, "mock_random")
                }

                MockTestHomeScreen(
                    contentPadding = innerPadding,
                    onStartTrial = {
                        proManager.refresh()
                        selectedTextId = null
                        quizTextId = null
                        quizQuestionIds = emptyList()
                        celebrationMessage = null
                        celebrationTextId = null
                        celebrationIsCurriculum = false
                        showReviewIntro = false
                        reviewIntroIds = emptyList()
                        activeReviewIds = emptyList()
                        isFreeStudyTodayReview = false
                        showNoTodayReviewDialog = false
                        isAutoReview = false
                        showFinalCelebration = false
                        forceShowHomeRoot = false
                        curriculumError = null

                        selectedTab = BottomTab.HOME
                        homeMode = HomeMode.MOCK
                        freeStudyMode = FreeStudyMode.HOME
                        showMockTestHome = false
                        showMockTestSession = true
                    },
                    onStartNormalMock = {
                        proManager.refresh()
                        selectedTextId = null
                        quizTextId = null
                        quizQuestionIds = emptyList()
                        celebrationMessage = null
                        celebrationTextId = null
                        celebrationIsCurriculum = false
                        showReviewIntro = false
                        reviewIntroIds = emptyList()
                        activeReviewIds = emptyList()
                        isFreeStudyTodayReview = false
                        showNoTodayReviewDialog = false
                        isAutoReview = false
                        showFinalCelebration = false
                        forceShowHomeRoot = false
                        curriculumError = null

                        selectedTab = BottomTab.HOME
                        homeMode = HomeMode.MOCK
                        freeStudyMode = FreeStudyMode.HOME
                        showMockTestHome = false
                        showMockTestSession = true
                    },
                    isPro = proManager.isProEnabled,
                    latestTrialResultText = formatMockResultText(latestTrialResult),
                    latestTrialDateText = formatMockDateText(latestTrialResult),
                    latestRandomResultText = formatMockResultText(latestRandomResult),
                    latestRandomDateText = formatMockDateText(latestRandomResult)
                )
            }
            forceShowHomeRoot -> {
                HomeMenuScreen(
                    contentPadding = innerPadding,
                    totalSections = totalSectionCount().takeIf { it > 0 },
                    completedSections = completedSectionCount(),
                    curriculumLap = CurriculumProgressStore.loadLap(context),
                    todayReviewCount = fetchDueReviewIds(context).size,
                    showBannerAd = !proManager.isProEnabled,
                    onGoCurriculum = {
                        forceShowHomeRoot = false
                        openSavedCurriculumOrHome()
                    },
                    onGoFreeStudy = {
                        forceShowHomeRoot = false
                        homeMode = HomeMode.FREE_STUDY
                        freeStudyMode = FreeStudyMode.HOME
                        // カリキュラムの「現在地」は自分で学ぶとは無関係。残るとクイズ完了時に誤って次セクションへ進む。
                        curriculumCurrentSectionId = null
                    },
                    onGoMock = {
                        proManager.refresh()
                        showMockTestSession = false
                        showMockTestHome = true
                        forceShowHomeRoot = false
                        selectedTab = BottomTab.HOME
                        homeMode = HomeMode.MOCK
                        freeStudyMode = FreeStudyMode.HOME
                    }
                )
            }


            error != null -> {
                Text("読み込みに失敗しました: $error")
            }

            showReviewIntro && reviewIntroIds.isNotEmpty() -> {
                ReviewIntroScreen(
                    contentPadding = innerPadding,
                    dueCount = reviewIntroIds.size,
                    onStartReview = {
                        isAutoReview = true

                        val mappedTextId = mapQuestionIdsToTextId(
                            reviewIntroIds,
                            curriculumCurrentSectionId?.let { curSecId ->
                                val curSec = findCurriculumSection(curSecId)
                                if (curSec?.type == CurriculumSectionType.TEXT) curSec.refId else null
                            }
                        )
                        activeReviewIds = reviewIntroIds

                        quizQuestionIds = reviewIntroIds
                        quizTextId = mappedTextId

                        // close intro
                        showReviewIntro = false
                        reviewIntroIds = emptyList()

                        selectedTab = BottomTab.HOME
                        if (isFreeStudyTodayReview) {
                            homeMode = HomeMode.FREE_STUDY
                            freeStudyMode = FreeStudyMode.HOME
                        } else {
                            homeMode = HomeMode.CURRICULUM
                            freeStudyMode = FreeStudyMode.HOME
                        }
                    },
                    onLater = {
                        showReviewIntro = false
                        reviewIntroIds = emptyList()
                        activeReviewIds = emptyList()
                        isAutoReview = false

                        if (isFreeStudyTodayReview) {
                            isFreeStudyTodayReview = false
                            selectedTab = BottomTab.HOME
                            homeMode = HomeMode.FREE_STUDY
                            freeStudyMode = FreeStudyMode.HOME
                        }
                    }
                )
            }

            showReviewCompletion -> {
                ReviewCompletionScreen(
                    contentPadding = innerPadding,
                    onContinue = {
                        showReviewCompletion = false
                        selectedTab = BottomTab.HOME
                        homeMode = HomeMode.CURRICULUM
                        freeStudyMode = FreeStudyMode.HOME

                        val nextId = curriculumNextSectionId
                        if (nextId == null) {
                            CurriculumProgressStore.clear(context)
                            curriculumNextSectionId = null
                            curriculumCurrentSectionId = null
                            debugFinalCelebrationLapOverride = 0
                            showFinalCelebration = true
                        } else {
                            val nextSec = findCurriculumSection(nextId)
                            if (nextSec == null) {
                                curriculumError = "続きのセクションが見つかりません: $nextId"
                                CurriculumProgressStore.clear(context)
                                curriculumNextSectionId = null
                                curriculumCurrentSectionId = null
                                homeMode = HomeMode.MENU
                            } else {
                                // 復習終了後は、問題セクションで中断していた場合でもテキスト画面に戻す
                                var resumeTextOnly = false
                                if (nextSec.type == CurriculumSectionType.QUIZ) {
                                    val qForGroup = questions.filter { it.groupId == nextSec.refId }
                                    val firstQ = qForGroup.firstOrNull()
                                    if (firstQ != null) {
                                        val textSec = findTextSectionPrecedingQuiz(nextSec.id)
                                            ?: findTextSectionByRefId(firstQ.textId)
                                        curriculumCurrentSectionId = textSec?.id ?: nextSec.id
                                        selectedTextId = firstQ.textId
                                        resumeTextOnly = true
                                    }
                                }
                                if (!resumeTextOnly &&
                                    openCurriculumSection(
                                        nextSec.id,
                                        nextSec.type,
                                        nextSec.refId
                                    ) == CurriculumOpenOutcome.BlockedByDailyLimit
                                ) {
                                    homeMode = HomeMode.MENU
                                }
                            }
                        }
                    }
                )
            }

            celebrationMessage != null && celebrationTextId != null -> {
                val msg = celebrationMessage!!
                val activityForInterstitialAd = LocalActivity.current
                // 次のセクションに進むタイミングでインタースティシャル広告を出したいので、事前にロードしておく
                LaunchedEffect(Unit) {
                    loadSectionInterstitialIfNeeded()
                }
                SectionCelebrationScreen(
                    message = msg,
                    isCurriculum = celebrationIsCurriculum,
                    onTapNext = {
                        // 無料版: 本日のテキスト上限を超える次セクション開始はブロックする
                        if (celebrationIsCurriculum && !proManager.isProEnabled) {
                            val nextId = curriculumNextSectionId
                            val nextSec = nextId?.let { findCurriculumSection(it) }

                            val targetTextId: String? = when (nextSec?.type) {
                                "text" -> nextSec.refId
                                "quiz" -> {
                                    val qForGroup = questions.filter { it.groupId == nextSec.refId }
                                    qForGroup.firstOrNull()?.textId
                                }
                                else -> null
                            }

                            if (targetTextId != null &&
                                DailyTextLimitStore.isLimitReached(context, freeDailyTextLimit) &&
                                !DailyTextLimitStore.hasCompletedText(context, targetTextId)
                            ) {
                                showDailyTextLimitDialog = true
                                return@SectionCelebrationScreen
                            }
                        }

                        // clear celebration + quiz UI state
                        selectedTextId = null
                        quizTextId = null
                        quizQuestionIds = emptyList()
                        celebrationMessage = null
                        celebrationTextId = null
                        curriculumError = null

                        // 有料版はバナー同様インタースティシャルも出さない
                        if (!proManager.isProEnabled) {
                            sectionInterstitialAd?.let { ad ->
                                val host = activityForInterstitialAd
                                if (host != null) {
                                    ad.show(host)
                                } else {
                                    Log.w(
                                        "KikenOtsuAds",
                                        "Interstitial not shown: Activity was null (use LocalActivity)",
                                    )
                                }
                                sectionInterstitialAd = null
                            }
                        } else {
                            sectionInterstitialAd = null
                        }

                        if (celebrationIsCurriculum) {
                            // ✅ セクション完了後は、そのまま次のセクションへ進む。
                            // 追加の復習は「カリキュラムで進む」を押したときの一度きりにする。
                            isAutoReview = false
                            reviewIntroIds = emptyList()
                            showReviewIntro = false
                            // ✅ Curriculum: open the next section immediately (no list)
                            celebrationIsCurriculum = false
                            selectedTab = BottomTab.HOME
                            homeMode = HomeMode.CURRICULUM

                            val nextId = curriculumNextSectionId
                            if (nextId == null) {
                                // ✅ curriculum finished -> show final celebration
                                CurriculumProgressStore.clear(context)
                                curriculumNextSectionId = null
                                curriculumCurrentSectionId = null
                                debugFinalCelebrationLapOverride = 0
                                showFinalCelebration = true
                                return@SectionCelebrationScreen
                            }

                            val nextSec = findCurriculumSection(nextId)
                            if (nextSec == null) {
                                curriculumError = "続きのセクションが見つかりません: $nextId"
                                CurriculumProgressStore.clear(context)
                                curriculumNextSectionId = null
                                curriculumCurrentSectionId = null
                                homeMode = HomeMode.MENU
                                return@SectionCelebrationScreen
                            }

                            if (openCurriculumSection(
                                    nextSec.id,
                                    nextSec.type,
                                    nextSec.refId
                                ) == CurriculumOpenOutcome.BlockedByDailyLimit
                            ) {
                                homeMode = HomeMode.MENU
                            }
                        } else {
                            // FreeStudy: return to text list
                            celebrationIsCurriculum = false
                            homeMode = HomeMode.FREE_STUDY
                            freeStudyMode = FreeStudyMode.TEXT_LIST
                        }
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
                        if (!isAutoReview) {
                            restoreCurriculumPointersToTextSection(tid)
                        }
                        selectedTextId = tid
                        quizTextId = null
                        quizQuestionIds = emptyList()
                        isAutoReview = false
                    },
                    questionIds = quizQuestionIds.takeIf { it.isNotEmpty() },
                    onAnswerCommitted = { qid, isCorrect ->
                        quizLogStore.recordAnswer(qid, isCorrect)
                    },
                    onShowCelebration = { total, correct, _ ->
                        // 1日1回だけ「学習した」として streak を更新
                        com.kubosaburo.kikenotsu4.data.LearnStreakStore.markLearnedToday(context)

                        // ✅ capture state at the exact moment the quiz finishes
                        val curHomeMode = homeMode

                        // 自分で学ぶ（FREE_STUDY）では curriculumCurrentSectionId が前回カリキュラムのまま残っていても
                        // カリキュラム進行にしてはいけない。カリキュラムかどうかは homeMode で判定する。
                        val isCurriculumNow = curHomeMode == HomeMode.CURRICULUM

                        if (isCurriculumNow) {
                            if (isAutoReview) {
                                // ✅ Auto-review finished: do NOT advance curriculum pointers here.
                                celebrationIsCurriculum = true
                            } else {
                                // ✅ curriculum: advance to next section (usually the next text)
                                advanceCurriculumFromCurrentSection()
                                celebrationIsCurriculum = true
                                // 念のため、タブ/モードもカリキュラムに寄せておく（戻り先が一覧になる事故を防ぐ）
                                selectedTab = BottomTab.HOME
                                homeMode = HomeMode.CURRICULUM
                                freeStudyMode = FreeStudyMode.HOME
                            }
                        } else {
                            celebrationIsCurriculum = false
                        }

                        val allCorrect = (correct == total)
                        celebrationTextId = tid
                        celebrationMessage = if (allCorrect) {
                            "全問正解！最高！🎉"
                        } else {
                            "おつかれさま！よく頑張ったね！🎉"
                        }

                        // 無料版の「本日の上限」カウント対象（テキスト問題のみ）
                        // 復習（auto-review）ではカウントしない。
                        if (!isAutoReview) {
                            DailyTextLimitStore.markCompletedText(context, tid)
                        }

                        // close quiz
                        quizTextId = null
                        quizQuestionIds = emptyList()
                    },
                    onFinish = { total, correct, _ ->
                        // 1日1回だけ「学習した」として streak を更新
                        com.kubosaburo.kikenotsu4.data.LearnStreakStore.markLearnedToday(context)

                        if (isAutoReview) {
                            if (isFreeStudyTodayReview) {
                                quizTextId = null
                                quizQuestionIds = emptyList()
                                showReviewIntro = false
                                reviewIntroIds = emptyList()
                                activeReviewIds = emptyList()
                                isAutoReview = false
                                isFreeStudyTodayReview = false

                                selectedTab = BottomTab.HOME
                                homeMode = HomeMode.FREE_STUDY
                                freeStudyMode = FreeStudyMode.HOME
                            } else {
                                // 復習10問がすべて終わったら、SectionCelebration を出さずに
                                // いったん労い画面を挟んでから次のセクションへ戻す。
                                quizTextId = null
                                quizQuestionIds = emptyList()
                                showReviewIntro = false
                                reviewIntroIds = emptyList()
                                activeReviewIds = emptyList()
                                isAutoReview = false

                                showReviewCompletion = true
                            }
                        } else {
                            // 通常クイズ（カリキュラムの quiz セクションなど）で
                            // questionIds 経由の完了時も、最後は祝画面へ進める。
                            val curHomeMode = homeMode
                            val isCurriculumNow = curHomeMode == HomeMode.CURRICULUM

                            if (isCurriculumNow) {
                                advanceCurriculumFromCurrentSection()
                                celebrationIsCurriculum = true
                                selectedTab = BottomTab.HOME
                                homeMode = HomeMode.CURRICULUM
                                freeStudyMode = FreeStudyMode.HOME
                            } else {
                                celebrationIsCurriculum = false
                            }

                            val allCorrect = (correct == total)
                            celebrationTextId = tid
                            celebrationMessage = if (allCorrect) {
                                "全問正解！最高！🎉"
                            } else {
                                "おつかれさま！よく頑張ったね！🎉"
                            }

                            // 通常クイズ完了時も、無料版の上限カウント対象（テキスト問題のみ）。
                            DailyTextLimitStore.markCompletedText(context, tid)

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
                    onStartQuiz = { tid: String ->
                        if (homeMode == HomeMode.CURRICULUM) {
                            // ✅ カリキュラム：現在の text セクションの nextId（通常は quiz セクション）を開く
                            val textSecId = curriculumCurrentSectionId
                            val textSec = textSecId?.let { findCurriculumSection(it) }
                            val quizSec = textSec?.nextId?.let { findCurriculumSection(it) }

                            if (quizSec == null) {
                                curriculumError = "次のクイズセクションが見つかりません"
                                return@TextDetailScreen
                            }

                            // quiz を「現在地」にして開く
                            openCurriculumSection(quizSec.id, quizSec.type, quizSec.refId)
                            return@TextDetailScreen
                        }

                        // FreeStudy等：従来通り textId でクイズを開く
                        if (!proManager.isProEnabled &&
                            DailyTextLimitStore.isLimitReached(context, freeDailyTextLimit) &&
                            !DailyTextLimitStore.hasCompletedText(context, tid)
                        ) {
                            showDailyTextLimitDialog = true
                            return@TextDetailScreen
                        }

                        quizQuestionIds = emptyList()
                        quizTextId = tid
                    },
                )
            }

            homeMode == HomeMode.FREE_STUDY -> {
                when (freeStudyMode) {
                    FreeStudyMode.HOME -> {
                        FreeStudyHomeScreen(
                            contentPadding = innerPadding,
                            onTextQuiz = { freeStudyMode = FreeStudyMode.TEXT_LIST },
                            onBookmarks = { freeStudyMode = FreeStudyMode.BOOKMARKS },
                            onTodayReview = {
                                val dueIds = fetchDueReviewIds(context)
                                if (dueIds.isNotEmpty()) {
                                    isFreeStudyTodayReview = true
                                    showReviewIntro = true
                                    reviewIntroIds = dueIds
                                    activeReviewIds = emptyList()
                                    selectedTab = BottomTab.HOME
                                    homeMode = HomeMode.FREE_STUDY
                                    freeStudyMode = FreeStudyMode.HOME
                                } else {
                                    showNoTodayReviewDialog = true
                                }
                            },
                            onSearch = { freeStudyMode = FreeStudyMode.SEARCH },
                            characterImage1 = R.drawable.nicosme_normal,
                            characterImage2 = R.drawable.nicosme_openmouth
                        )

                        if (showNoTodayReviewDialog) {
                            AlertDialog(
                                onDismissRequest = { showNoTodayReviewDialog = false },
                                title = {
                                    Text(
                                        text = "今日は復習はありません",
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                text = {
                                    Text("今日は復習対象がありません。学習を進めると復習が出てきます")
                                },
                                confirmButton = {
                                    TextButton(onClick = { showNoTodayReviewDialog = false }) {
                                        Text("OK")
                                    }
                                }
                            )
                        }
                    }

                    FreeStudyMode.TEXT_LIST -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            val isFreeLimitReached =
                                !proManager.isProEnabled && DailyTextLimitStore.isLimitReached(
                                    context,
                                    freeDailyTextLimit
                                )
                            val completedTodayTextIds = if (isFreeLimitReached) {
                                DailyTextLimitStore.getCompletedTextIds(context)
                            } else {
                                emptySet()
                            }

                            val curriculumDescByTextId = remember(curriculum) {
                                textIdToCurriculumChapterDescriptionMap(curriculum)
                            }
                            TextListScreen(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                items = texts,
                                contentPadding = PaddingValues(0.dp),
                                showBannerAd = !proManager.isProEnabled,
                                curriculumDescriptionsByTextId = curriculumDescByTextId,
                                isEnabled = { tid -> !isFreeLimitReached || completedTodayTextIds.contains(tid) },
                                onOpen = { tid: String ->
                                    if (isFreeLimitReached && !completedTodayTextIds.contains(tid)) {
                                        showDailyTextLimitDialog = true
                                    } else {
                                        selectedTextId = tid
                                    }
                                }
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (curriculumError != null) {
                        Text(
                            text = curriculumError!!,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    CurriculumHomeScreen(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(0.dp),
                        showBannerAd = !proManager.isProEnabled,
                        chapters = curriculum?.chapters ?: emptyList(),
                        onOpenChapter = { chapterId ->
                            curriculumTextOpenedFromResume = false
                            val ch = curriculum?.chapters?.firstOrNull { it.id == chapterId }
                            val first = ch?.sections?.firstOrNull()
                            if (first == null) {
                                curriculumError = "この章にはセクションがありません: $chapterId"
                                return@CurriculumHomeScreen
                            }
                            if (openCurriculumSection(
                                    first.id,
                                    first.type,
                                    first.refId
                                ) == CurriculumOpenOutcome.BlockedByDailyLimit
                            ) {
                                homeMode = HomeMode.MENU
                            }
                        }
                    )
                }
            }


            else -> {
                HomeMenuScreen(
                    contentPadding = innerPadding,
                    totalSections = totalSectionCount().takeIf { it > 0 },
                    completedSections = completedSectionCount(),
                    curriculumLap = CurriculumProgressStore.loadLap(context),
                    todayReviewCount = fetchDueReviewIds(context).size,
                    showBannerAd = !proManager.isProEnabled,
                    onGoCurriculum = {
                        openSavedCurriculumOrHome()
                    },
                    onGoFreeStudy = {
                        homeMode = HomeMode.FREE_STUDY
                        freeStudyMode = FreeStudyMode.HOME
                        curriculumCurrentSectionId = null
                    },
                    onGoMock = {
                        proManager.refresh()
                        showMockTestSession = false
                        showMockTestHome = true
                        forceShowHomeRoot = false
                        selectedTab = BottomTab.HOME
                        homeMode = HomeMode.MOCK
                        freeStudyMode = FreeStudyMode.HOME
                    }
                )
            }
        }
    }
}

