package com.kubosaburo.kikenotsu4.data

import android.content.Context
import android.util.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

object AssetRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    fun loadTexts(context: Context): List<TextItem> {
        val raw = context.assets.open("texts.json")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
            .trimStart('\uFEFF')

        val root = json.decodeFromString(TextsRoot.serializer(), raw)

        // category_main / content を JSON から直接マージ（デコードで空になる不整合への保険）
        val patched = runCatching {
            val arr = json.parseToJsonElement(raw).jsonObject["texts"]?.jsonArray
                ?: return@runCatching root.texts
            if (arr.size != root.texts.size) {
                Log.w(
                    "AssetRepository",
                    "texts size mismatch json=${arr.size} decoded=${root.texts.size}"
                )
            }
            root.texts.mapIndexed { i, item ->
                val obj = arr.getOrNull(i)?.jsonObject ?: return@mapIndexed item
                val cat = (obj["category_main"] as? JsonPrimitive)
                    ?.content
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                val linesFromJson: List<String> =
                    obj["content"]?.jsonArray?.mapNotNull { elem ->
                        (elem as? JsonPrimitive)?.content
                    } ?: emptyList()

                val mergedCat = item.categoryMain?.trim()?.takeIf { it.isNotEmpty() } ?: cat
                val mergedContent =
                    if (item.content.any { it.isNotBlank() }) item.content else linesFromJson

                item.copy(categoryMain = mergedCat, content = mergedContent)
            }
        }.getOrElse { e ->
            Log.e("AssetRepository", "texts patch failed", e)
            root.texts
        }

        val t0 = patched.firstOrNull()
        Log.d(
            "AssetRepository",
            "texts count=${patched.size} firstCat='${t0?.categoryMain}' firstContent0Len=${t0?.content?.firstOrNull()?.length ?: 0}"
        )
        return patched
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

    fun loadCurriculum(context: Context): CurriculumRoot {
        val text = context.assets.open("curriculum.json")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
            .trimStart('\uFEFF')

        val root = json.decodeFromString(CurriculumRoot.serializer(), text)

        // JSON の "description" を素で読み、デコード結果とマージ（説明が空になる不整合への保険）
        val patchedChapters = runCatching {
            val arr = json.parseToJsonElement(text).jsonObject["curriculums"]?.jsonArray
                ?: return@runCatching root.chapters
            if (arr.size != root.chapters.size) {
                Log.w(
                    "AssetRepository",
                    "curriculum size mismatch json=${arr.size} decoded=${root.chapters.size}"
                )
            }
            root.chapters.mapIndexed { i, ch ->
                val descEl = arr.getOrNull(i)?.jsonObject?.get("description")
                val rawDesc = (descEl as? JsonPrimitive)
                    ?.content
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                if (!rawDesc.isNullOrBlank()) {
                    ch.copy(description = rawDesc)
                } else {
                    ch
                }
            }
        }.getOrElse { e ->
            Log.e("AssetRepository", "curriculum description patch failed", e)
            root.chapters
        }

        val out = CurriculumRoot(chapters = patchedChapters)
        val c0 = out.chapters.firstOrNull()
        Log.d(
            "AssetRepository",
            "curriculum chapters=${out.chapters.size} firstId=${c0?.id ?: "-"} firstDescLen=${c0?.description?.length ?: 0}"
        )
        return out
    }
}