package com.kubosaburo.kikenotsu4.data

import android.content.Context
import android.media.MediaPlayer
import androidx.annotation.RawRes

/**
 * 学習効果の設定（ON/OFF・音量）を反映して効果音を鳴らす。
 */
object LearningEffectSound {

    fun playOneShot(context: Context, @RawRes resId: Int) {
        if (resId == 0) return
        if (!LearningEffectSettings.isSoundEffectsEnabled(context)) return
        val vol = LearningEffectSettings.getVolume01(context)
        runCatching {
            val mp = MediaPlayer.create(context, resId) ?: return
            mp.setVolume(vol, vol)
            mp.setOnCompletionListener { it.release() }
            mp.start()
        }
    }
}
