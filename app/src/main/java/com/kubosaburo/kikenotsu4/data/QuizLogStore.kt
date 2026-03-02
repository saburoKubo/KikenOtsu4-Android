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
    private val prefs = context.getSharedPreferences("quiz_log", Context.MODE_PRIVATE)

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