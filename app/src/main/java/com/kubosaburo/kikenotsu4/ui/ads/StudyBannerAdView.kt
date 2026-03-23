@file:Suppress("ASSIGNED_VALUE_IS_NEVER_READ")

package com.kubosaburo.kikenotsu4.ui.ads

import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * 学習画面用バナー [AdView]。
 * [AdView.setAdSize] は Java で `AdView` を返すため、戻り値を使わない呼び出しで IDE が警告することがある。
 * 生成処理はこのファイルに閉じ、`@file:Suppress` で抑止する。
 */
fun createStudyBannerAdView(
    context: Context,
    /** テスト用デフォルト。本番は各画面で本番ユニットIDを渡す */
    adUnitId: String = "ca-app-pub-3940256099942544/6300978111",
): AdView =
    AdView(context).apply {
        this.adUnitId = adUnitId
        setAdSize(AdSize.BANNER)
        loadAd(AdRequest.Builder().build())
    }
