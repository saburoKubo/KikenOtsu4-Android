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
    @SerialName("id")
    val id: String,
    @SerialName("title")
    val title: String,
    /** curriculum.json の説明文（「〜を学びましょう」など） */
    @SerialName("description")
    val description: String = "",
    @SerialName("sections")
    val sections: List<CurriculumSection> = emptyList(),
    /** 章タイトル直下のラベル（例:「法令」）。JSON に無ければ「法令」。 */
    @SerialName("category_label")
    val categoryLabel: String = "法令",
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
    // JSON で mock セクションを使うようになったら const val MOCK = "mock" を追加
}

/**
 * テキスト一覧で、カリキュラム章カードと同じ説明文を出すためのマップ。
 * 各章の **先頭の text セクション** の [CurriculumSection.refId]（textId）→ その章の [CurriculumChapter.description]。
 */
fun textIdToCurriculumChapterDescriptionMap(root: CurriculumRoot?): Map<String, String> {
    if (root == null) return emptyMap()
    val out = LinkedHashMap<String, String>()
    for (ch in root.chapters) {
        val desc = ch.description.trim()
        if (desc.isEmpty()) continue
        val firstText = ch.sections.firstOrNull { it.type == CurriculumSectionType.TEXT } ?: continue
        val ref = firstText.refId.trim()
        if (ref.isNotEmpty()) {
            out[ref] = desc
        }
    }
    return out
}
