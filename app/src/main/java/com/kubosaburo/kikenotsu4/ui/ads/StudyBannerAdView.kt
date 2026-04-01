@file:Suppress("ASSIGNED_VALUE_IS_NEVER_READ")

package com.kubosaburo.kikenotsu4.ui.ads

import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.kubosaburo.kikenotsu4.R

/**
 * 学習画面用バナー [AdView]。
 * ユニット ID は [R.string.admob_banner_ad_unit_id]（admob.xml）。本番は AdMob で同じアプリ用ユニットを作成して差し替える。
 */
fun createStudyBannerAdView(
    context: Context,
    /** null のときは strings のバナー ID を使う */
    adUnitId: String? = null,
): AdView {
    val unit = adUnitId ?: context.getString(R.string.admob_banner_ad_unit_id)
    return AdView(context).apply {
        this.adUnitId = unit
        setAdSize(AdSize.BANNER)
        loadAd(AdRequest.Builder().build())
    }
}
