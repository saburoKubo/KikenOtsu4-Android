package com.kubosaburo.kikenotsu4.data

import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.kubosaburo.kikenotsu4.ui.screens.DebugProMode

class ProManager(
    private val context: Context
) : PurchasesUpdatedListener {

    companion object {
        /** Google Play Console の「アプリ内アイテム」に登録する商品 ID。 */
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

    val products: List<String> = listOf(PRO_PRODUCT_ID)

    private var proProductDetails: ProductDetails? = null
    private var isConnecting = false
    private val billingReadyCallbacks = mutableListOf<() -> Unit>()

    private val billingClient: BillingClient =
        BillingClient.newBuilder(context.applicationContext)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .enableAutoServiceReconnection()
            .build()

    fun loadProducts() {
        lastErrorMessage = null
        ensureBillingReady {
            queryProProductDetails {
                // no-op: query result is cached in proProductDetails for the next purchase tap.
            }
        }
    }

    fun updatePurchasedStatus() {
        isProEnabled = resolveIsProEnabled()
    }

    fun purchase(activity: Activity?) {
        if (activity == null) {
            lastErrorMessage = "購入画面を開けませんでした。もう一度お試しください。"
            return
        }

        isBusy = true
        lastErrorMessage = null
        ensureBillingReady {
            queryProProductDetails {
                launchPurchaseFlow(activity)
            }
        }
    }

    fun restore() {
        isBusy = true
        lastErrorMessage = null
        ensureBillingReady {
            queryOwnedPurchases(showNotFoundMessage = true)
        }
    }

    fun refresh() {
        loadProducts()
        ensureBillingReady {
            queryOwnedPurchases(showNotFoundMessage = false)
        }
    }

    fun markPurchasedForLocalDebug() {
        savePurchasedFlag(true)
        refresh()
    }

    fun clearPurchasedForLocalDebug() {
        savePurchasedFlag(false)
        refresh()
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases != null) {
                    handlePurchases(purchases)
                } else {
                    finishBusy()
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                lastErrorMessage = null
                finishBusy()
            }
            else -> {
                lastErrorMessage = billingResult.debugMessage.ifBlank {
                    "購入処理に失敗しました（${billingResult.responseCode}）"
                }
                finishBusy()
            }
        }
    }

    private fun ensureBillingReady(onReady: () -> Unit) {
        if (billingClient.isReady) {
            onReady()
            return
        }

        billingReadyCallbacks += onReady
        if (isConnecting) return

        isConnecting = true
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                isConnecting = false
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val callbacks = billingReadyCallbacks.toList()
                    billingReadyCallbacks.clear()
                    callbacks.forEach { it() }
                } else {
                    lastErrorMessage = billingResult.debugMessage.ifBlank {
                        "Google Play に接続できませんでした（${billingResult.responseCode}）"
                    }
                    billingReadyCallbacks.clear()
                    finishBusy()
                }
            }

            override fun onBillingServiceDisconnected() {
                isConnecting = false
                // enableAutoServiceReconnection() が次回リクエスト時に再接続を試みる。
            }
        })
    }

    private fun queryProProductDetails(onLoaded: () -> Unit) {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRO_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                proProductDetails = productDetailsResult.productDetailsList.firstOrNull {
                    it.productId == PRO_PRODUCT_ID
                }
                if (proProductDetails == null) {
                    lastErrorMessage =
                        "Google Play に Pro 商品が見つかりません。商品 ID「$PRO_PRODUCT_ID」が有効か確認してください。"
                    finishBusy()
                } else {
                    onLoaded()
                }
            } else {
                lastErrorMessage = billingResult.debugMessage.ifBlank {
                    "Pro 商品情報を取得できませんでした（${billingResult.responseCode}）"
                }
                finishBusy()
            }
        }
    }

    private fun launchPurchaseFlow(activity: Activity) {
        val productDetails = proProductDetails
        if (productDetails == null) {
            lastErrorMessage = "Pro 商品情報を取得できませんでした。"
            finishBusy()
            return
        }

        val offerToken = productDetails.oneTimePurchaseOfferDetailsList
            ?.firstOrNull()
            ?.offerToken

        val productDetailsParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
        if (!offerToken.isNullOrBlank()) {
            productDetailsParamsBuilder.setOfferToken(offerToken)
        }

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParamsBuilder.build()))
            .build()
        val billingResult = billingClient.launchBillingFlow(activity, flowParams)
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            lastErrorMessage = billingResult.debugMessage.ifBlank {
                "Google Play の購入画面を開けませんでした（${billingResult.responseCode}）"
            }
            finishBusy()
        }
    }

    private fun queryOwnedPurchases(showNotFoundMessage: Boolean) {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                handlePurchases(purchases)
                val hasPro = purchases.any { purchase ->
                    purchase.products.contains(PRO_PRODUCT_ID) &&
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                if (!hasPro) {
                    savePurchasedFlag(false)
                    updatePurchasedStatus()
                    if (showNotFoundMessage) {
                        lastErrorMessage = "復元できる Pro 購入が見つかりませんでした。"
                    }
                    finishBusy()
                }
            } else {
                lastErrorMessage = billingResult.debugMessage.ifBlank {
                    "購入状態を確認できませんでした（${billingResult.responseCode}）"
                }
                finishBusy()
            }
        }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        val proPurchase = purchases.firstOrNull { purchase ->
            purchase.products.contains(PRO_PRODUCT_ID) &&
                purchase.purchaseState == Purchase.PurchaseState.PURCHASED
        }

        if (proPurchase == null) {
            val hasPendingPro = purchases.any { purchase ->
                purchase.products.contains(PRO_PRODUCT_ID) &&
                    purchase.purchaseState == Purchase.PurchaseState.PENDING
            }
            if (hasPendingPro) {
                lastErrorMessage = "購入は保留中です。支払い完了後に Pro が有効になります。"
            }
            finishBusy()
            return
        }

        savePurchasedFlag(true)
        updatePurchasedStatus()

        if (proPurchase.isAcknowledged) {
            finishBusy()
            return
        }

        val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(proPurchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(acknowledgeParams) { billingResult ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                lastErrorMessage = billingResult.debugMessage.ifBlank {
                    "購入の承認に失敗しました（${billingResult.responseCode}）"
                }
            }
            finishBusy()
        }
    }

    private fun finishBusy() {
        isBusy = false
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
