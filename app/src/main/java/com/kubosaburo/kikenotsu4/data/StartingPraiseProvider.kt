
package com.kubosaburo.kikenotsu4.data

import android.content.Context
import org.json.JSONObject
import kotlin.random.Random

/**
 * assets/praise_messages.json から「開始時メッセージ」を読み込み、ランダムで返す。
 * - キー: startingPraise_messages
 * - 要素: { "id": Int, "text": String }
 */
class StartingPraiseProvider(private val context: Context) {

    private val starting: List<String> = loadStartingPraise()

    /** ランダムに 1 つ返す（空ならフォールバック） */
    fun randomStarting(): String {
        if (starting.isEmpty()) return "今日もコツコツいこうね！"
        return starting[Random.nextInt(starting.size)]
    }

    private fun loadStartingPraise(): List<String> {
        val jsonText = runCatching {
            context.assets.open("praise_messages.json")
                .bufferedReader()
                .use { it.readText() }
        }.getOrDefault("{}")

        val root = runCatching { JSONObject(jsonText) }.getOrNull() ?: JSONObject("{}")
        val arr = root.optJSONArray("startingPraise_messages") ?: return emptyList()

        val out = ArrayList<String>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val text = obj.optString("text").trim()
            if (text.isNotBlank()) out.add(text)
        }
        return out
    }
}

