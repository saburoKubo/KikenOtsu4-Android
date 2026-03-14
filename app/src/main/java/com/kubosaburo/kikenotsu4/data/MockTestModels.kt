package com.kubosaburo.kikenotsu4.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MockTestsPayload(
    val version: Int,
    @SerialName("mock_tests")
    val mockTests: List<MockTestDefinition>
)

@Serializable
data class MockTestDefinition(
    val id: String,
    val title: String,
    val description: String,
    @SerialName("time_limit")
    val timeLimit: Int,
    val mode: String,
    @SerialName("question_ids")
    val questionIds: List<String> = emptyList(),
    val sections: List<MockTestSection> = emptyList()
) {
    fun isFixed(): Boolean = mode.equals("fixed", ignoreCase = true)

    fun isRandom(): Boolean = mode.equals("random", ignoreCase = true)

    fun countFor(title: String): Int {
        return sections.firstOrNull { it.title == title }?.count ?: 0
    }
}

@Serializable
data class MockTestSection(
    val title: String,
    val count: Int,
    val range: MockTestRange
)

@Serializable
data class MockTestRange(
    val start: String,
    val end: String
)
