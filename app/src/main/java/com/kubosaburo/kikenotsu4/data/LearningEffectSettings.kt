package com.kubosaburo.kikenotsu4.data

import android.content.Context
import androidx.core.content.edit

/**
 * 「学習効果」関連のユーザー設定（効果音・音量・視聴学習リマインド）。
 */
object LearningEffectSettings {

    private const val PREFS = "learning_effect_settings"
    private const val KEY_SOUND_EFFECTS = "sound_effects_enabled"
    private const val KEY_VOLUME_PERCENT = "volume_percent"
    private const val KEY_VIDEO_STUDY_REMINDER = "video_study_reminder_enabled"

    fun isSoundEffectsEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_SOUND_EFFECTS, true)
    }

    fun setSoundEffectsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit { putBoolean(KEY_SOUND_EFFECTS, enabled) }
    }

    /** 0〜100 */
    fun getVolumePercent(context: Context): Int {
        return prefs(context).getInt(KEY_VOLUME_PERCENT, 100).coerceIn(0, 100)
    }

    fun setVolumePercent(context: Context, percent: Int) {
        prefs(context).edit { putInt(KEY_VOLUME_PERCENT, percent.coerceIn(0, 100)) }
    }

    /** 0.0f〜1.0f（MediaPlayer#setVolume 用） */
    fun getVolume01(context: Context): Float {
        return getVolumePercent(context) / 100f
    }

    fun isVideoStudyReminderEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_VIDEO_STUDY_REMINDER, false)
    }

    fun setVideoStudyReminderEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit { putBoolean(KEY_VIDEO_STUDY_REMINDER, enabled) }
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
