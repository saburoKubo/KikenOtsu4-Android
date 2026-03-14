package com.kubosaburo.kikenotsu4.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import com.kubosaburo.kikenotsu4.ui.screens.DebugProMode

class ProManager(
    private val context: Context
) {

    companion object {
        const val PRO_PRODUCT_ID = "pro_buy_once"

        private const val PREFS = "pro_manager"
        private const val KEY_PRO_PURCHASED = "pro_purchased"
    }

    var isBusy by mutableStateOf(false)
        private set

    var isProEnabled by mutableStateOf(resolveIsProEnabled())
        private set

    var lastErrorMessage by mutableStateOf<String?>(null)
        private set

    /**
     * Android版はまだ本格課金実装前なので、いったん空配列で持っておく。
     * 画面側で products を参照しても落ちないようにしておくための受け皿。
     */
    val products: List<String> = emptyList()

    fun loadProducts() {
        lastErrorMessage = null
    }

    fun updatePurchasedStatus() {
        isProEnabled = resolveIsProEnabled()
    }

    fun purchase() {
        isBusy = true
        lastErrorMessage = null
        runCatching {
            savePurchasedFlag(true)
            isProEnabled = resolveIsProEnabled()
        }.onFailure {
            lastErrorMessage = it.message ?: "Pro購入状態の更新に失敗しました"
        }
        isBusy = false
    }

    fun restore() {
        isBusy = true
        lastErrorMessage = null
        runCatching {
            isProEnabled = resolveIsProEnabled()
        }.onFailure {
            lastErrorMessage = it.message ?: "購入状態の復元に失敗しました"
        }
        isBusy = false
    }

    fun refresh() {
        isProEnabled = resolveIsProEnabled()
    }

    fun markPurchasedForLocalDebug() {
        savePurchasedFlag(true)
        refresh()
    }

    fun clearPurchasedForLocalDebug() {
        savePurchasedFlag(false)
        refresh()
    }

    private fun resolveIsProEnabled(): Boolean {
        return when (DebugProMode.load(context)) {
            DebugProMode.Mode.FORCE_FREE -> false
            DebugProMode.Mode.FORCE_PRO -> true
            DebugProMode.Mode.SYSTEM -> loadPurchasedFlag()
        }
    }

    private fun loadPurchasedFlag(): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_PRO_PURCHASED, false)
    }

    private fun savePurchasedFlag(enabled: Boolean) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit {
            putBoolean(KEY_PRO_PURCHASED, enabled)
        }
    }
}