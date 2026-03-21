package com.kubosaburo.kikenotsu4.data

import android.content.Context
import androidx.core.content.edit

/**
 * 学習に関するローカル保存をまとめて削除する。
 *
 * 含むものの例: カリキュラムの続き、クイズ集計、**復習スケジュール（忘却曲線）**、**ブックマーク**、
 * 1日テキスト制限カウント、連続学習、模擬テスト記録。
 *
 * 残すもの: 有料版状態・学習効果設定・デバッグ設定など。
 */
object LearningDataReset {

    fun clearAll(context: Context) {
        val app = context.applicationContext
        CurriculumProgressStore.clear(app)
        app.getSharedPreferences("quiz_log", Context.MODE_PRIVATE).edit { clear() }
        // 復習（SM-2 相当）の nextReviewAt など
        app.getSharedPreferences("review_srs", Context.MODE_PRIVATE).edit { clear() }
        // テキストのブックマーク一覧
        BookmarkStore(app).clear()
        DailyTextLimitStore.clear(app)
        LearnStreakStore.clear(app)
        MockTestResultStore.clearAll(app)
    }
}
