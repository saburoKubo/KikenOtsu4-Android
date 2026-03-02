package com.kubosaburo.kikenotsu4.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TextsRoot(
    val texts: List<TextItem> = emptyList()
)

@Serializable
data class TextItem(
    val id: String,
    val title: String,
    @SerialName("category_main") val categoryMain: String? = null,
    val content: List<String> = emptyList(),
    val table: List<TextTableRow> = emptyList(),
    val image: String? = null
)

@Serializable
data class TextTableRow(
    val category: String = "",
    val name: String = "",
    val description: String = "",
    val item: String = ""
)