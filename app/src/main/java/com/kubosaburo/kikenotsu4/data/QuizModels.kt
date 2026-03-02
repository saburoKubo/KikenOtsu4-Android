package com.kubosaburo.kikenotsu4.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QuestionsWrapper(
    val questions: List<QuizQuestion> = emptyList()
)

@Serializable
data class QuizQuestion(
    val answerIndex: Int? = null,
    val category: String = "",
    val choices: List<String> = emptyList(),
    val correctIndex: Int = 0,
    val difficulty: Int = 0,
    val explanation: String = "",
    @SerialName("group_id") val groupId: String = "",
    val id: String = "",
    val importance: Int? = null,
    val question: String = "",
    val subcategory: String? = null,
    val tags: List<String>? = null,
    @SerialName("text_id") val textId: String = ""
)