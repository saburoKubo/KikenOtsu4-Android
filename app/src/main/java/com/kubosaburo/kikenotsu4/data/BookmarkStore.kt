package com.kubosaburo.kikenotsu4.data

import android.content.Context
import androidx.core.content.edit

class BookmarkStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** 現在のブックマーク textId セットを取得 */
    fun loadBookmarkedTextIds(): Set<String> {
        return prefs.getStringSet(KEY_TEXT_IDS, emptySet())?.toSet() ?: emptySet()
    }

    /** 指定 textId がブックマーク済みか */
    fun isBookmarked(textId: String): Boolean {
        return loadBookmarkedTextIds().contains(textId)
    }

    /** ブックマークの付け外し */
    fun toggle(textId: String) {
        if (isBookmarked(textId)) {
            remove(textId)
        } else {
            add(textId)
        }
    }

    /** 明示的に追加 */
    fun add(textId: String) {
        val cur = loadBookmarkedTextIds().toMutableSet()
        if (cur.add(textId)) {
            prefs.edit { putStringSet(KEY_TEXT_IDS, cur) }
        }
    }

    /** 明示的に削除 */
    fun remove(textId: String) {
        val cur = loadBookmarkedTextIds().toMutableSet()
        if (cur.remove(textId)) {
            prefs.edit { putStringSet(KEY_TEXT_IDS, cur) }
        }
    }

    /** 全削除 */
    fun clear() {
        prefs.edit { remove(KEY_TEXT_IDS) }
    }

    private companion object {
        private const val PREFS_NAME = "bookmarks"
        private const val KEY_TEXT_IDS = "bookmarked_text_ids"
    }
}
