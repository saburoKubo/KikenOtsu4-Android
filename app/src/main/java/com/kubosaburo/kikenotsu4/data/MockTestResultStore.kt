
package com.kubosaburo.kikenotsu4.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object MockTestResultStore {

    @Serializable
    data class CategoryStat(
        val correct: Int,
        val total: Int,
    ) {
        val rate: Double
            get() = if (total > 0) correct.toDouble() / total.toDouble() else 0.0
    }

    @Serializable
    data class Result(
        val mockTestId: String,
        val seed: Long? = null,
        val finishedAtMillis: Long,
        val durationSeconds: Int,
        val total: Int,
        val correct: Int,
        val wrong: Int,
        val categoryStats: Map<String, CategoryStat> = emptyMap(),
    ) {
        val overallRate: Double
            get() = if (total > 0) correct.toDouble() / total.toDouble() else 0.0

        fun isPassed(threshold: Double = 0.60): Boolean {
            return isOverallPassed(categoryStats, threshold)
        }
    }

    @Serializable
    data class Summary(
        val mockTestId: String,
        val attemptCount: Int,
        val passCount: Int,
        val bestOverall: Result? = null,
        val bestPassed: Result? = null,
    )

    private const val PREFS = "mock_test_result_store"
    private const val KEY_PREFIX_LATEST = "mock_test_latest_"
    private const val KEY_PREFIX_SUMMARY = "mock_test_summary_"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private fun normalizedId(mockTestId: String): String {
        return mockTestId.trim().lowercase()
    }

    private fun latestKey(mockTestId: String): String {
        return KEY_PREFIX_LATEST + normalizedId(mockTestId)
    }

    private fun summaryKey(mockTestId: String): String {
        return KEY_PREFIX_SUMMARY + normalizedId(mockTestId)
    }

    fun saveLatest(context: Context, result: Result) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit {
            putString(latestKey(result.mockTestId), json.encodeToString(result))
        }
    }

    fun loadLatest(context: Context, mockTestId: String): Result? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(latestKey(mockTestId), null) ?: return null
        return runCatching { json.decodeFromString(Result.serializer(), raw) }.getOrNull()
    }

    fun clearLatest(context: Context, mockTestId: String) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit {
            remove(latestKey(mockTestId))
        }
    }

    fun recordResult(
        context: Context,
        result: Result,
        passThreshold: Double = 0.60,
    ) {
        saveLatest(context, result)

        val current = loadSummary(context, result.mockTestId)
            ?: Summary(
                mockTestId = result.mockTestId,
                attemptCount = 0,
                passCount = 0,
                bestOverall = null,
                bestPassed = null,
            )

        val newAttemptCount = current.attemptCount + 1
        val newPassCount = current.passCount + if (result.isPassed(passThreshold)) 1 else 0

        val newBestOverall = when (val best = current.bestOverall) {
            null -> result
            else -> {
                if (result.overallRate > best.overallRate) {
                    result
                } else if (result.overallRate == best.overallRate && result.finishedAtMillis > best.finishedAtMillis) {
                    result
                } else {
                    best
                }
            }
        }

        val newBestPassed = if (result.isPassed(passThreshold)) {
            when (val best = current.bestPassed) {
                null -> result
                else -> {
                    if (result.overallRate > best.overallRate) {
                        result
                    } else if (result.overallRate == best.overallRate && result.finishedAtMillis > best.finishedAtMillis) {
                        result
                    } else {
                        best
                    }
                }
            }
        } else {
            current.bestPassed
        }

        val updated = Summary(
            mockTestId = result.mockTestId,
            attemptCount = newAttemptCount,
            passCount = newPassCount,
            bestOverall = newBestOverall,
            bestPassed = newBestPassed,
        )

        saveSummary(context, updated)
    }

    fun loadSummary(context: Context, mockTestId: String): Summary? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(summaryKey(mockTestId), null) ?: return null
        return runCatching { json.decodeFromString(Summary.serializer(), raw) }.getOrNull()
    }

    fun clearSummary(context: Context, mockTestId: String) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit {
            remove(summaryKey(mockTestId))
        }
    }

    fun isOverallPassed(
        categoryStats: Map<String, CategoryStat>,
        threshold: Double = 0.60,
    ): Boolean {
        if (categoryStats.isEmpty()) return false
        return categoryStats.values
            .filter { it.total > 0 }
            .all { it.rate >= threshold }
    }

    private fun saveSummary(context: Context, summary: Summary) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit {
            putString(summaryKey(summary.mockTestId), json.encodeToString(summary))
        }
    }
}

