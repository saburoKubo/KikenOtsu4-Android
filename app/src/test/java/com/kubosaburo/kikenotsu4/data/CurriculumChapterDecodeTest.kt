package com.kubosaburo.kikenotsu4.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class CurriculumChapterDecodeTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    @Test
    fun `description maps when category_label omitted`() {
        val text =
            """{"curriculums":[{"id":"c1","title":"タイトル","description":"説明文です。","sections":[]}]}"""
        val root = json.decodeFromString(CurriculumRoot.serializer(), text)
        assertEquals("説明文です。", root.chapters.single().description)
        assertEquals("法令", root.chapters.single().categoryLabel)
    }
}
