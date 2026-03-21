@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.kubosaburo.kikenotsu4

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.google.android.gms.ads.MobileAds
import com.kubosaburo.kikenotsu4.reminder.VideoStudyReminderScheduler
import com.kubosaburo.kikenotsu4.ui.AppRoot
import com.kubosaburo.kikenotsu4.ui.theme.KikenOtsu4Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Google Mobile Ads SDK (AdMob) の初期化
        MobileAds.initialize(this) {}

        VideoStudyReminderScheduler.sync(this)

        enableEdgeToEdge()
        setContent {
            KikenOtsu4Theme {
                AppRoot()
            }
        }
    }
}