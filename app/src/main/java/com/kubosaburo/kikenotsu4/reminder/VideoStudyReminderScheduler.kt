package com.kubosaburo.kikenotsu4.reminder

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.kubosaburo.kikenotsu4.data.LearningEffectSettings
import java.util.concurrent.TimeUnit

object VideoStudyReminderScheduler {

    private const val UNIQUE_NAME = "video_study_reminder_periodic"

    /** 設定の ON/OFF に合わせて周期ジョブを登録／解除する */
    fun sync(context: Context) {
        val app = context.applicationContext
        val wm = WorkManager.getInstance(app)
        if (LearningEffectSettings.isVideoStudyReminderEnabled(app)) {
            val request = PeriodicWorkRequestBuilder<VideoStudyReminderWorker>(24, TimeUnit.HOURS)
                .build()
            wm.enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        } else {
            wm.cancelUniqueWork(UNIQUE_NAME)
        }
    }
}
