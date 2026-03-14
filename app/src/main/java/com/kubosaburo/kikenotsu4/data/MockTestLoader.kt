package com.kubosaburo.kikenotsu4.data

import android.content.Context
import kotlinx.serialization.json.Json

object MockTestLoader {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun loadMockTests(context: Context): MockTestsPayload {
        val raw = context.assets
            .open("mock_tests.json")
            .bufferedReader()
            .use { it.readText() }

        return json.decodeFromString(MockTestsPayload.serializer(), raw)
    }

    fun loadMockTestById(
        context: Context,
        mockTestId: String
    ): MockTestDefinition? {
        val normalized = mockTestId.trim().lowercase()
        return loadMockTests(context).mockTests.firstOrNull {
            it.id.trim().lowercase() == normalized
        }
    }

    fun loadTrialMockTest(context: Context): MockTestDefinition? {
        return loadMockTestById(context, "mock_trial_fixed")
    }

    fun loadRandomMockTest(context: Context): MockTestDefinition? {
        return loadMockTestById(context, "mock_random")
    }
}