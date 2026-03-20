package com.kubosaburo.kikenotsu4.data

import android.content.Context
import androidx.core.content.edit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 無料版の「1日2問まで（テキスト問題のみ）」制限用ストア。
 *
 * - カウント対象: 自動復習（isAutoReview）以外のテキスト問題完了（お祝い画面到達）
 * - 保存: SharedPreferences
 * - 日付は SettingsScreen の debug_clock のオフセットを考慮
 */
object DailyTextLimitStore {
    private const val PREFS = "daily_text_limit"
    private const val KEY_DAY = "day_key"
    private const val KEY_COMPLETED_TEXT_IDS = "completed_text_ids"

    private fun debugOffsetDays(context: Context): Int {
        val prefs = context.applicationContext.getSharedPreferences("debug_clock", Context.MODE_PRIVATE)
        return prefs.getInt("debug_time_offset_days", 0)
    }

    private fun nowMillisWithDebug(context: Context): Long {
        val offsetDays = debugOffsetDays(context)
        val offsetMillis = offsetDays.toLong() * 24L * 60L * 60L * 1000L
        return System.currentTimeMillis() + offsetMillis
    }

    private fun todayKey(context: Context): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date(nowMillisWithDebug(context)))
    }

    private fun ensureToday(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = todayKey(context)
        val currentDay = prefs.getString(KEY_DAY, null)
        if (currentDay != today) {
            prefs.edit {
                putString(KEY_DAY, today)
                putStringSet(KEY_COMPLETED_TEXT_IDS, emptySet<String>())
            }
        }
    }

    fun getCompletedTextIds(context: Context): Set<String> {
        ensureToday(context)
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_COMPLETED_TEXT_IDS, emptySet())?.toSet() ?: emptySet()
    }

    fun getUsedCount(context: Context): Int = getCompletedTextIds(context).size

    fun hasCompletedText(context: Context, textId: String): Boolean {
        return getCompletedTextIds(context).contains(textId)
    }

    /**
     * 完了を1件追加する。
     * @return 新規追加だった場合 true（= 今日のカウントが増えた場合のみ）
     */
    fun markCompletedText(context: Context, textId: String): Boolean {
        if (textId.isBlank()) return false
        ensureToday(context)

        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val existing = prefs.getStringSet(KEY_COMPLETED_TEXT_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
        val added = existing.add(textId)

        if (added) {
            prefs.edit {
                putStringSet(KEY_COMPLETED_TEXT_IDS, existing.toSet())
            }
        }
        return added
    }

    fun isLimitReached(context: Context, limitPerDay: Int): Boolean {
        return getUsedCount(context) >= limitPerDay
    }
}

