package com.kubosaburo.kikenotsu4.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kubosaburo.kikenotsu4.R
import com.kubosaburo.kikenotsu4.data.LearningEffectSettings

/**
 * 視聴学習リマインド用。1日おきに通知を出す（WorkManager 周期ジョブ）。
 */
class VideoStudyReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        if (!LearningEffectSettings.isVideoStudyReminderEnabled(ctx)) {
            return Result.success()
        }

        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(nm)

        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(ctx.getString(R.string.video_study_reminder_notification_title))
            .setContentText(ctx.getString(R.string.video_study_reminder_notification_text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        nm.notify(NOTIFICATION_ID, notification)
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "video_study_reminder"
        private const val NOTIFICATION_ID = 71001

        fun ensureChannel(notificationManager: NotificationManager) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val existing = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (existing != null) return
            val name = "視聴学習リマインド"
            val channel = NotificationChannel(
                CHANNEL_ID,
                name,
                NotificationManager.IMPORTANCE_DEFAULT,
            )
            notificationManager.createNotificationChannel(channel)
        }
    }
}
