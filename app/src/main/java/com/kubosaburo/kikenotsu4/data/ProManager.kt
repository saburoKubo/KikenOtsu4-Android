package com.kubosaburo.kikenotsu4.data

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import com.kubosaburo.kikenotsu4.ui.screens.DebugProMode

class ProManager(
    private val context: Context
) {

    companion object {
        /** Google Play に登録する想定の SKU（課金実装時に Billing で使用） */
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
     * 課金実装前の受け皿。現状は [PRO_PRODUCT_ID] のみ。
     * 将来 Billing で問い合わせる SKU 一覧に差し替える。
     */
    val products: List<String> = listOf(PRO_PRODUCT_ID)

    fun loadProducts() {
        lastErrorMessage = null
        // 将来: BillingClient で各 SKU を query。現状は一覧参照のみ。
        products.forEach { sku ->
            if (sku != PRO_PRODUCT_ID) {
                lastErrorMessage = "不明な SKU: $sku"
            }
        }
    }

    fun updatePurchasedStatus() {
        isProEnabled = resolveIsProEnabled()
    }

    fun purchase() {
        isBusy = true
        lastErrorMessage = null
        runCatching {
            savePurchasedFlag(true)
            refresh()
        }.onFailure {
            lastErrorMessage = it.message ?: "Pro購入状態の更新に失敗しました"
        }
        isBusy = false
    }

    fun restore() {
        isBusy = true
        lastErrorMessage = null
        runCatching {
            refresh()
        }.onFailure {
            lastErrorMessage = it.message ?: "購入状態の復元に失敗しました"
        }
        isBusy = false
    }

    fun refresh() {
        loadProducts()
        updatePurchasedStatus()
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
        // release（debuggable=false）ではデバッグ UI が出ないため、
        // 過去にデバッグ APK で保存した FORCE_* が残っていても無視する。
        val mode =
            if (isDebuggableBuild()) DebugProMode.load(context) else DebugProMode.Mode.SYSTEM
        return when (mode) {
            DebugProMode.Mode.FORCE_FREE -> false
            DebugProMode.Mode.FORCE_PRO -> true
            DebugProMode.Mode.SYSTEM -> loadPurchasedFlag()
        }
    }

    private fun isDebuggableBuild(): Boolean =
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

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