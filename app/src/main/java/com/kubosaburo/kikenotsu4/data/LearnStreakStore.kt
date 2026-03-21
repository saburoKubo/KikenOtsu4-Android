package com.kubosaburo.kikenotsu4.data

import android.content.Context
import androidx.core.content.edit
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date

/**
 * 連続学習（streak）の最小実装。
 *
 * - 1日あたり「学習完了」を1回だけカウント
 * - 前日と連続していれば streak +1、連続していなければ streak=1
 * - 日付は SettingsScreen の debug_clock のオフセットも考慮
 */
object LearnStreakStore {
    private const val PREFS = "learn_streak"
    private const val KEY_LAST_DAY = "last_day"
    private const val KEY_STREAK = "streak_count"

    private fun debugOffsetDays(context: Context): Int {
        val prefs = context.applicationContext.getSharedPreferences("debug_clock", Context.MODE_PRIVATE)
        return prefs.getInt("debug_time_offset_days", 0)
    }

    private fun nowMillisWithDebug(context: Context): Long {
        val offsetDays = debugOffsetDays(context)
        return System.currentTimeMillis() + offsetDays.toLong() * 24L * 60L * 60L * 1000L
    }

    private fun todayLocalDate(context: Context): LocalDate {
        val millis = nowMillisWithDebug(context)
        return Date(millis).toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    }

    fun getStreakDays(context: Context): Int {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_STREAK, 0)
    }

    /**
     * 今日を「学習した」として記録する。
     * @return 更新後の streak（今日が未カウントなら増え、すでに記録済みならそのまま）
     */
    @Suppress("unused")
    fun markLearnedToday(context: Context): Int {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        val today = todayLocalDate(appContext)
        val lastDayRaw = prefs.getString(KEY_LAST_DAY, null)
        val lastDay = lastDayRaw?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

        val currentStreak = prefs.getInt(KEY_STREAK, 0)
        val newStreak = when {
            lastDay == null -> 1
            lastDay == today -> currentStreak // 今日はすでにカウント済み
            ChronoUnit.DAYS.between(lastDay, today) == 1L -> currentStreak + 1
            else -> 1
        }

        prefs.edit {
            putString(KEY_LAST_DAY, today.toString())
            putInt(KEY_STREAK, newStreak)
        }

        return newStreak
    }

    fun clear(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { clear() }
    }
}

