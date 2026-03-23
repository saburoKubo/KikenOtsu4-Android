package com.kubosaburo.kikenotsu4.data

import android.content.Context
import androidx.core.content.edit
import android.util.Log

/**
 * カリキュラムの「前回の続き（次に開くセクション）」を保存するストア。
 *
 * - nextSectionId: 次に開くべき sectionId（未開始なら null）
 */
object CurriculumProgressStore {

    private const val PREFS_NAME = "curriculum_progress"
    private const val KEY_NEXT_SECTION_ID = "next_section_id"
    /** カリキュラムを最後まで通した回数ベースの「周目」。1＝初回プレイ、全完了のたびに +1。 */
    private const val KEY_CURRICULUM_LAP = "curriculum_lap"

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
        }
    }

    /** 進捗をリセット（最初から）。周回数（[loadLap]）は維持する。 */
    fun clear(context: Context) {
        Log.d("CurriculumProgress", "clear")
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            remove(KEY_NEXT_SECTION_ID)
        }
    }

    /**
     * 表示用の周回数。未保存時は 1（初回）。
     * 全カリキュラムを最後まで終えてポインタがクリアされる直前に [incrementLapAfterFullCurriculumRound] すると、
     * 先頭からやり直す区間は「2周目」以降として表示できる。
     */
    fun loadLap(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val v = prefs.getInt(KEY_CURRICULUM_LAP, 1)
        return v.coerceAtLeast(1)
    }

    /** カリキュラムを最後まで完了したときに呼ぶ（1周増やす）。 */
    fun incrementLapAfterFullCurriculumRound(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val next = loadLap(context) + 1
        Log.d("CurriculumProgress", "incrementLap -> $next")
        prefs.edit { putInt(KEY_CURRICULUM_LAP, next) }
    }

    /**
     * DEBUG 専用：ホーム・進捗の「○周目」表示の見た目確認用。
     * 本番の全完了フローとは無関係に [incrementLapAfterFullCurriculumRound] と同じく周回を +1 する。
     */
    fun debugIncrementLapForPreview(context: Context) {
        incrementLapAfterFullCurriculumRound(context)
    }

    /** 学習データ全消去など：周回表示を初回に戻す。 */
    fun resetLap(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { remove(KEY_CURRICULUM_LAP) }
    }
}
