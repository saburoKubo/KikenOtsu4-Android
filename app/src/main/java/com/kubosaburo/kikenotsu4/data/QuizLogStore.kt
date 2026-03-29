package com.kubosaburo.kikenotsu4.data

import android.content.Context
import androidx.core.content.edit
import java.time.Instant
import java.time.ZoneId

// 進捗表示用の集計
data class QuizStats(
    val totalAnswered: Int,
    val totalCorrect: Int,
    val wrongCount: Int
)

@Suppress("unused")
class QuizLogStore(context: Context) {
    private val appContext: Context = context.applicationContext
    private val prefs = appContext.getSharedPreferences("quiz_log", Context.MODE_PRIVATE)

    @Suppress("unused")
    fun recordAnswer(questionId: String, isCorrect: Boolean) {
        val total = prefs.getInt("total_answered", 0) + 1
        val correct = prefs.getInt("total_correct", 0) + if (isCorrect) 1 else 0

        val wrongSet = prefs.getStringSet("wrong_question_ids", emptySet())
            ?.toMutableSet() ?: mutableSetOf()

        if (isCorrect) {
            wrongSet.remove(questionId)
        } else {
            wrongSet.add(questionId)
        }

        prefs.edit {
            putInt("total_answered", total)
            putInt("total_correct", correct)
            putStringSet("wrong_question_ids", wrongSet)
        }
        // ✅ 忘却曲線（SM-2簡易）用の復習スケジュールを更新
        ReviewStore.updateOnAnswer(appContext, questionId, isCorrect)
    }

    @Suppress("unused")
    fun getWrongIds(): Set<String> =
        prefs.getStringSet("wrong_question_ids", emptySet()) ?: emptySet()

    @Suppress("unused")
    fun getStats(): QuizStats {
        val totalAnswered = prefs.getInt("total_answered", 0)
        val totalCorrect = prefs.getInt("total_correct", 0)
        val wrongCount = getWrongIds().size
        return QuizStats(
            totalAnswered = totalAnswered,
            totalCorrect = totalCorrect,
            wrongCount = wrongCount
        )
    }

    /**
     * 復習（SRS）で期限が来ている問題ID。最大 [maxCount] 件。
     * 日付シミュレーションは [ReviewClock] 経由で反映される。
     */
    fun fetchDueQuestionIds(maxCount: Int = Int.MAX_VALUE): List<String> =
        ReviewStore.fetchDueIds(appContext, maxCount, ReviewClock.nowMillis(appContext))
}

/**
 * DebugClock（SettingsScreen）と同じ SharedPreferences を読み、
 * "日付シミュレーション(+N日)" を復習ロジックでも反映するための時計。
 */
private object ReviewClock {
    private const val PREFS = "debug_clock"
    private const val KEY_OFFSET_DAYS = "debug_time_offset_days"

    fun nowMillis(context: Context): Long {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val days = prefs.getInt(KEY_OFFSET_DAYS, 0)
        return System.currentTimeMillis() + daysToMillis(days)
    }

    private fun daysToMillis(days: Int): Long = days.toLong() * 24L * 60L * 60L * 1000L
}

// =============================
// Review (Spaced Repetition)
// =============================

/**
 * iOS側の「忘却曲線ベース復習」に近い形で、Androidでも最小実装するためのストア。
 * - questionId 単位で復習状態を SharedPreferences に保存
 * - 正解/不正解で nextReviewAt を更新
 *
 * NOTE: まずは動くこと優先の簡易SM-2（正解=品質5 / 不正解=品質2）
 */
private object ReviewStore {

    private const val PREFS_NAME = "review_srs"
    private const val PREFIX = "q_" // key = q_<questionId>

    private data class State(
        val ease: Double,
        val intervalDays: Int,
        val repetition: Int,
        val nextReviewAt: Long,
        val lastReviewedAt: Long
    )

    fun updateOnAnswer(context: Context, questionId: String, isCorrect: Boolean) {
        val now = ReviewClock.nowMillis(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = PREFIX + questionId

        val prev = prefs.getString(key, null)?.let { decode(it) } ?: State(
            ease = 2.5,
            intervalDays = 0,
            repetition = 0,
            nextReviewAt = now,
            lastReviewedAt = 0L
        )

        val quality = if (isCorrect) 5 else 2
        val updated = sm2Update(prev, quality, now)

        prefs.edit {
            putString(key, encode(updated))
        }
    }

    /**
     * 今日(=now)までに期限が来ている復習IDを返す。
     * デバッグの「日付シミュレーション(+N日)」を反映したい場合は、
     * `now` に `ReviewClock.nowMillis(context)` を渡す。
     */
    fun fetchDueIds(context: Context, maxCount: Int, now: Long): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val all = prefs.all
        if (all.isEmpty()) return emptyList()

        val due = ArrayList<Pair<String, Long>>()
        for ((k, v) in all) {
            if (!k.startsWith(PREFIX)) continue
            val raw = v as? String ?: continue
            val st = decode(raw) ?: continue
            if (st.nextReviewAt <= now) {
                val qid = k.removePrefix(PREFIX)
                due.add(qid to st.nextReviewAt)
            }
        }

        // 古い期限順に
        due.sortBy { it.second }
        return due.take(maxCount).map { it.first }
    }

    // --- SM-2 (simplified) ---
    private fun sm2Update(prev: State, quality: Int, now: Long): State {
        val zone = ZoneId.systemDefault()
        // ease factor update (SM-2)
        val q = quality.coerceIn(0, 5)
        val newEase = (prev.ease + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))).coerceAtLeast(1.3)

        return if (q < 3) {
            // 不正解：やり直し扱い（次回は「翌日 0:00」以降＝カレンダー日ベース）
            val nextAt = nextReviewStartOfLocalDayAfter(now, 1, zone)
            State(
                ease = newEase,
                intervalDays = 1,
                repetition = 0,
                nextReviewAt = nextAt,
                lastReviewedAt = now
            )
        } else {
            val nextInterval = when (prev.repetition) {
                0 -> 1
                1 -> 6
                else -> kotlin.math.round(prev.intervalDays * newEase).toInt().coerceAtLeast(1)
            }
            // 正解：間隔 N 日も「解答した日のローカル日付から N 日後の 0:00」に揃える。
            // now + 24h だと前日夕方の解答が翌日昼まで期限未到来になり、「2日目の朝に復習が出ない」になる。
            val nextAt = nextReviewStartOfLocalDayAfter(now, nextInterval, zone)
            State(
                ease = newEase,
                intervalDays = nextInterval,
                repetition = prev.repetition + 1,
                nextReviewAt = nextAt,
                lastReviewedAt = now
            )
        }
    }

    /**
     * [now] が属するローカル日付から [intervalDays] 日後の日の 0:00（そのタイムゾーン）を epoch millis で返す。
     * 例: 3/1 22:00 に解答・interval=1 → 3/2 0:00（同日中に「翌日朝」から復習対象になる）
     */
    private fun nextReviewStartOfLocalDayAfter(now: Long, intervalDays: Int, zone: ZoneId): Long {
        val days = intervalDays.coerceAtLeast(1)
        val answerDay = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val dueDay = answerDay.plusDays(days.toLong())
        return dueDay.atStartOfDay(zone).toInstant().toEpochMilli()
    }

    // --- Encoding ---
    // 保存形式: ease|intervalDays|repetition|nextReviewAt|lastReviewedAt
    private fun encode(s: State): String =
        "${s.ease}|${s.intervalDays}|${s.repetition}|${s.nextReviewAt}|${s.lastReviewedAt}"

    private fun decode(raw: String): State? {
        val parts = raw.split('|')
        if (parts.size != 5) return null
        return runCatching {
            State(
                ease = parts[0].toDouble(),
                intervalDays = parts[1].toInt(),
                repetition = parts[2].toInt(),
                nextReviewAt = parts[3].toLong(),
                lastReviewedAt = parts[4].toLong()
            )
        }.getOrNull()
    }
}