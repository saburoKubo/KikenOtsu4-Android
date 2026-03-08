package com.kubosaburo.kikenotsu4.data

import android.content.Context
import androidx.core.content.edit

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

    /** 日付シミュレーションを反映した "今" で due を返す */
    fun fetchDueIds(context: Context, maxCount: Int): List<String> {
        return fetchDueIds(context, maxCount, ReviewClock.nowMillis(context))
    }

    /** 今日(=now)までに期限が来ている復習IDを返す（UI側は後で） */
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
        // ease factor update (SM-2)
        val q = quality.coerceIn(0, 5)
        val newEase = (prev.ease + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))).coerceAtLeast(1.3)

        return if (q < 3) {
            // 不正解：やり直し扱い（次回は短め）
            val nextAt = now + daysToMillis(1)
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
            val nextAt = now + daysToMillis(nextInterval)
            State(
                ease = newEase,
                intervalDays = nextInterval,
                repetition = prev.repetition + 1,
                nextReviewAt = nextAt,
                lastReviewedAt = now
            )
        }
    }

    private fun daysToMillis(days: Int): Long = days.toLong() * 24L * 60L * 60L * 1000L

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