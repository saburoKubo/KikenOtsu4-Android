package com.kubosaburo.kikenotsu4.data

import android.content.Context
import android.util.Log
import kotlinx.serialization.json.Json

object AssetRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    fun loadTexts(context: Context): List<TextItem> {
        val text = context.assets.open("texts.json")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        return json.decodeFromString(TextsRoot.serializer(), text).texts
    }

    fun loadQuestions(context: Context): List<QuizQuestion> {
        val text = context.assets.open("questions.json")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }

        val wrapper: QuestionsWrapper = json.decodeFromString(text)

        // 先頭の問題文が全文で読み込めているかをLogcatで確認（常に出す）
        if (wrapper.questions.isNotEmpty()) {
            val q0 = wrapper.questions.first()
            Log.d(
                "AssetRepository",
                "q0.id=${q0.id} len=${q0.question.length} head='${q0.question.take(12)}' tail='${q0.question.takeLast(12)}'"
            )
        }

        return wrapper.questions
    }
}