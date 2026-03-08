package com.kubosaburo.kikenotsu4.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * curriculum.json のルート
 * 例: { "curriculums": [ ... ] }
 */
@Serializable
data class CurriculumRoot(
    @SerialName("curriculums")
    val chapters: List<CurriculumChapter> = emptyList()
)

/**
 * 章（チャプター）
 */
@Serializable
data class CurriculumChapter(
    val id: String,
    val title: String,
    val description: String = "",
    val sections: List<CurriculumSection> = emptyList()
)

/**
 * セクション（学習ステップ）
 * - type: "text" / "quiz" / "mock" など
 * - refId: type に応じた参照ID
 *   - text: texts.json の textId (例: "text_001")
 *   - quiz: questions.json の group_id (例: "g001") など
 * - nextId: 次のセクションID（最後は null）
 */
@Serializable
data class CurriculumSection(
    val id: String,
    val title: String = "",
    val type: String,
    val refId: String = "",
    val nextId: String? = null,
)

/**
 * type の標準値（文字列運用で十分だが、判定用に定数を用意）
 */
object CurriculumSectionType {
    const val TEXT = "text"
    const val QUIZ = "quiz"
    const val MOCK = "mock"
}
