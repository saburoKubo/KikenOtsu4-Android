package com.kubosaburo.kikenotsu4.data

import android.content.Context
import androidx.core.content.edit

/**
 * カリキュラム順とは独立した「学習済み項目」。
 *
 * カリキュラムでも自分で学ぶでも、通常クイズを完了した text_id を保存する。
 * 復習セッションは新規学習ではないため対象外。
 */
object CompletedTextStore {
    private const val PREFS = "completed_texts"
    private const val KEY_COMPLETED_TEXT_IDS = "completed_text_ids"

    fun getCompletedTextIds(context: Context): Set<String> {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_COMPLETED_TEXT_IDS, emptySet())?.toSet() ?: emptySet()
    }

    fun markCompleted(context: Context, textId: String): Boolean {
        val normalized = textId.trim()
        if (normalized.isEmpty()) return false

        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val existing = prefs.getStringSet(KEY_COMPLETED_TEXT_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
        val added = existing.add(normalized)

        if (added) {
            prefs.edit {
                putStringSet(KEY_COMPLETED_TEXT_IDS, existing.toSet())
            }
        }

        return added
    }

    fun clear(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { clear() }
    }
}
