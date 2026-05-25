package com.kubosaburo.kikenotsu4.data

import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Test

class AssetConsistencyTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    @Test
    fun `every text has at least one question`() {
        val texts = loadTexts()
        val questions = loadQuestions()
        val questionTextIds = questions.map { it.textId }.toSet()

        val missing = texts
            .filterNot { it.id in questionTextIds }
            .joinToString(separator = "\n") { "${it.id}: ${it.title}" }

        assertTrue(
            "questions.json に問題がない texts.json の text_id があります:\n$missing",
            missing.isEmpty()
        )
    }

    @Test
    fun `all question text ids exist in texts`() {
        val textIds = loadTexts().map { it.id }.toSet()
        val missing = loadQuestions()
            .filterNot { it.textId in textIds }
            .joinToString(separator = "\n") { "${it.id}: ${it.textId}" }

        assertTrue(
            "questions.json が texts.json に存在しない text_id を参照しています:\n$missing",
            missing.isEmpty()
        )
    }

    @Test
    fun `asset ids are unique`() {
        val duplicateTextIds = loadTexts().map { it.id }.duplicates()
        val duplicateQuestionIds = loadQuestions().map { it.id }.duplicates()

        assertTrue(
            "重複IDがあります: texts=$duplicateTextIds, questions=$duplicateQuestionIds",
            duplicateTextIds.isEmpty() && duplicateQuestionIds.isEmpty()
        )
    }

    @Test
    fun `curriculum references existing texts and question groups`() {
        val textIds = loadTexts().map { it.id }.toSet()
        val questionGroupIds = loadQuestions().map { it.groupId }.toSet()
        val curriculum = json.decodeFromString(
            CurriculumRoot.serializer(),
            assetFile("curriculum.json").readText()
        )

        val missingTextRefs = curriculum.chapters
            .flatMap { chapter -> chapter.sections.map { chapter to it } }
            .filter { (_, section) -> section.type == CurriculumSectionType.TEXT }
            .filterNot { (_, section) -> section.refId in textIds }
            .joinToString(separator = "\n") { (chapter, section) ->
                "${chapter.id}/${section.id}: ${section.refId}"
            }

        val missingQuizRefs = curriculum.chapters
            .flatMap { chapter -> chapter.sections.map { chapter to it } }
            .filter { (_, section) -> section.type == CurriculumSectionType.QUIZ }
            .filterNot { (_, section) -> section.refId in questionGroupIds }
            .joinToString(separator = "\n") { (chapter, section) ->
                "${chapter.id}/${section.id}: ${section.refId}"
            }

        assertTrue(
            "curriculum.json に存在しない参照があります:\ntexts:\n$missingTextRefs\nquiz groups:\n$missingQuizRefs",
            missingTextRefs.isEmpty() && missingQuizRefs.isEmpty()
        )
    }

    @Test
    fun `questions have valid choices and correct index`() {
        val invalid = loadQuestions()
            .filter { question ->
                question.choices.isEmpty() ||
                    question.correctIndex !in question.choices.indices ||
                    question.question.isBlank() ||
                    question.explanation.isBlank()
            }
            .joinToString(separator = "\n") { question ->
                "${question.id}: choices=${question.choices.size}, correctIndex=${question.correctIndex}"
            }

        assertTrue(
            "問題データの選択肢・正解番号・本文・解説に不備があります:\n$invalid",
            invalid.isEmpty()
        )
    }

    private fun loadTexts(): List<TextItem> =
        json.decodeFromString(TextsRoot.serializer(), assetFile("texts.json").readText()).texts

    private fun loadQuestions(): List<QuizQuestion> =
        json.decodeFromString(QuestionsWrapper.serializer(), assetFile("questions.json").readText()).questions

    private fun assetFile(name: String): File {
        val candidates = listOf(
            File("src/main/assets", name),
            File("app/src/main/assets", name)
        )
        return candidates.firstOrNull { it.isFile }
            ?: error("Asset file not found: $name")
    }

    private fun List<String>.duplicates(): List<String> =
        groupingBy { it }.eachCount().filterValues { it > 1 }.keys.sorted()
}
