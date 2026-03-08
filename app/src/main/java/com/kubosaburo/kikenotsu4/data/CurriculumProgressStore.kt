package com.kubosaburo.kikenotsu4.data

import android.content.Context
import androidx.core.content.edit
import android.util.Log

/**
 * カリキュラムの「前回の続き（次に開くセクション）」を保存するストア。
 *
 * - nextSectionId: 次に開くべき sectionId（未開始なら null）
 * - lastOpenedAt: 最後に更新した時刻（デバッグ用）
 */
object CurriculumProgressStore {

    private const val PREFS_NAME = "curriculum_progress"
    private const val KEY_NEXT_SECTION_ID = "next_section_id"
    private const val KEY_LAST_OPENED_AT = "last_opened_at"

    /** 次に開くセクションID（未開始なら null） */
    fun loadNextSectionId(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_NEXT_SECTION_ID, null)
        val value = raw?.trim()?.takeIf { it.isNotEmpty() }
        Log.d("CurriculumProgress", "loadNextSectionId value=$value")
        return value
    }

    /** 次に開くセクションIDを保存 */
    fun saveNextSectionId(context: Context, nextSectionId: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val value = nextSectionId?.trim().orEmpty().takeIf { it.isNotEmpty() }
        Log.d("CurriculumProgress", "saveNextSectionId nextSectionId=$nextSectionId normalized=$value")
        prefs.edit {
            if (value == null) {
                remove(KEY_NEXT_SECTION_ID)
            } else {
                putString(KEY_NEXT_SECTION_ID, value)
            }
            putLong(KEY_LAST_OPENED_AT, System.currentTimeMillis())
        }
    }

    /** デバッグ用：最後に更新した時刻（未保存なら 0） */
    fun loadLastOpenedAt(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_OPENED_AT, 0L)
    }

    /** 進捗をリセット（最初から） */
    fun clear(context: Context) {
        Log.d("CurriculumProgress", "clear")
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            remove(KEY_NEXT_SECTION_ID)
            remove(KEY_LAST_OPENED_AT)
        }
    }
}
