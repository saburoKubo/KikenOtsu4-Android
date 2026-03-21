package com.kubosaburo.kikenotsu4.ui.screens

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.kubosaburo.kikenotsu4.ui.theme.KikenOtsu4Theme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.kubosaburo.kikenotsu4.data.LearningDataReset
import com.kubosaburo.kikenotsu4.data.LearningEffectSettings
import com.kubosaburo.kikenotsu4.reminder.VideoStudyReminderScheduler
import com.kubosaburo.kikenotsu4.reminder.VideoStudyReminderWorker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Settings screen.
 *
 * - DEBUG ビルドのときだけ「日付シミュレーション（+N日）」を表示
 * - SharedPreferences に保存して、復習ロジック側で DebugClock.nowMillis(context) を使えば
 *   iOS と同じように "日付をずらして復習を出す" テストができます。
 */
@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    isProEnabled: Boolean = false,
    isProBusy: Boolean = false,
    proErrorMessage: String? = null,
    onProPurchase: () -> Unit = {},
    onProRestore: () -> Unit = {},
    onProModeChanged: (() -> Unit)? = null,
    onLearningDataCleared: (() -> Unit)? = null,
) {
    val context = LocalContext.current

    // 画面表示用：保存値を読み込む
    var offsetDays by rememberSaveable {
        mutableIntStateOf(DebugClock.loadOffsetDays(context))
    }
    var showFinalCelebrationPreview by rememberSaveable { mutableStateOf(false) }
    var proDebugOverride by rememberSaveable { mutableStateOf(DebugProMode.load(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("設定", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "ここに通知・サウンド・学習設定などを追加していきます。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LearningEffectsSettingsCard()

        ProUpgradeCard(
            isProEnabled = isProEnabled,
            isBusy = isProBusy,
            errorMessage = proErrorMessage,
            onPurchase = onProPurchase,
            onRestore = onProRestore,
        )

        LearningDataSettingsCard(
            onLearningDataCleared = onLearningDataCleared,
        )

        SupportAppInfoSection()

        if (isDebugBuild(context)) {
            DebugTimeTravelCard(
                offsetDays = offsetDays,
                onChange = { newDays ->
                    offsetDays = newDays
                    DebugClock.saveOffsetDays(context, newDays)
                }
            )
            DebugProModeCard(
                currentMode = proDebugOverride,
                onChange = { newMode ->
                    proDebugOverride = newMode
                    DebugProMode.save(context, newMode)
                    onProModeChanged?.invoke()
                }
            )
            DebugFinalCelebrationTestCard(
                onOpen = { showFinalCelebrationPreview = true }
            )
        }

        if (showFinalCelebrationPreview) {
            Dialog(
                onDismissRequest = { showFinalCelebrationPreview = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                ) {
                    // FinalCelebrationScreen should call onGoHome when the user taps its main button.
                    FinalCelebrationScreen(
                        contentPadding = PaddingValues(0.dp),
                        onGoHome = { showFinalCelebrationPreview = false }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun LearningEffectsSettingsCard() {
    val context = LocalContext.current

    var soundEnabled by remember {
        mutableStateOf(LearningEffectSettings.isSoundEffectsEnabled(context))
    }
    var volumePercent by remember {
        mutableIntStateOf(LearningEffectSettings.getVolumePercent(context))
    }
    var videoReminder by remember {
        mutableStateOf(LearningEffectSettings.isVideoStudyReminderEnabled(context))
    }
    var pendingEnableVideoReminder by remember { mutableStateOf(false) }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (pendingEnableVideoReminder) {
            pendingEnableVideoReminder = false
            if (granted) {
                LearningEffectSettings.setVideoStudyReminderEnabled(context, true)
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                VideoStudyReminderWorker.ensureChannel(nm)
                VideoStudyReminderScheduler.sync(context)
                videoReminder = true
            }
        }
    }

    val accent = MaterialTheme.colorScheme.primaryContainer
    val onAccent = MaterialTheme.colorScheme.onPrimaryContainer

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.GraphicEq,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "学習効果",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = onAccent
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text("効果音", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "正誤・セクション完了などの効果音",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = soundEnabled,
                    onCheckedChange = {
                        soundEnabled = it
                        LearningEffectSettings.setSoundEffectsEnabled(context, it)
                    }
                )
            }

            Text("音量", style = MaterialTheme.typography.bodyMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Slider(
                    value = volumePercent / 100f,
                    onValueChange = { v ->
                        volumePercent = (v * 100f).toInt().coerceIn(0, 100)
                        LearningEffectSettings.setVolumePercent(context, volumePercent)
                    },
                    enabled = soundEnabled,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${volumePercent}%",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.widthIn(min = 40.dp)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text("視聴学習リマインド", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "約24時間ごとに通知します（Android の省電力設定によりずれることがあります）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = videoReminder,
                    onCheckedChange = { wantOn ->
                        if (!wantOn) {
                            LearningEffectSettings.setVideoStudyReminderEnabled(context, false)
                            VideoStudyReminderScheduler.sync(context)
                            videoReminder = false
                            return@Switch
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val perm = Manifest.permission.POST_NOTIFICATIONS
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                perm
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                LearningEffectSettings.setVideoStudyReminderEnabled(context, true)
                                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                                VideoStudyReminderWorker.ensureChannel(nm)
                                VideoStudyReminderScheduler.sync(context)
                                videoReminder = true
                            } else if (context.findComponentActivity() != null) {
                                pendingEnableVideoReminder = true
                                notifPermissionLauncher.launch(perm)
                            }
                        } else {
                            LearningEffectSettings.setVideoStudyReminderEnabled(context, true)
                            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            VideoStudyReminderWorker.ensureChannel(nm)
                            VideoStudyReminderScheduler.sync(context)
                            videoReminder = true
                        }
                    }
                )
            }
        }
    }
}

private tailrec fun Context.findComponentActivity(): androidx.activity.ComponentActivity? {
    return when (this) {
        is androidx.activity.ComponentActivity -> this
        is ContextWrapper -> baseContext.findComponentActivity()
        else -> null
    }
}

@Composable
private fun LearningDataSettingsCard(
    onLearningDataCleared: (() -> Unit)?,
) {
    val context = LocalContext.current
    var showEraseDialog by remember { mutableStateOf(false) }

    if (showEraseDialog) {
        AlertDialog(
            onDismissRequest = { showEraseDialog = false },
            title = {
                Text(
                    text = "学習データを消去しますか？",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "次のデータをこの端末から削除します。\n\n" +
                        "・カリキュラムの続き\n" +
                        "・クイズの集計（正答数・間違い履歴）\n" +
                        "・復習（忘却曲線に基づく出題スケジュール）\n" +
                        "・ブックマーク\n" +
                        "・本日のテキスト学習カウント（無料版の1日上限用）\n" +
                        "・連続学習日数\n" +
                        "・模擬テストの記録\n\n" +
                        "残すもの: 有料版の購入状態、効果音・リマインドなどの設定、デバッグ用の日付シミュレーション等。\n\n" +
                        "この操作は取り消せません。"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        LearningDataReset.clearAll(context)
                        showEraseDialog = false
                        onLearningDataCleared?.invoke()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("消去")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEraseDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }

    val accent = MaterialTheme.colorScheme.primaryContainer
    val onAccent = MaterialTheme.colorScheme.onPrimaryContainer

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Storage,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "学習データ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = onAccent
                )
            }
            Text(
                text = "カリキュラムの続き、クイズ集計、復習スケジュール、ブックマークなど、この端末の学習データをまとめて削除できます。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = { showEraseDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("学習データを消去")
            }
            Text(
                text = "※ ブックマーク登録や復習の予定もすべて消え、元に戻せません。他の端末・クラウドのデータは消えません。必要な事項は消去前にメモなどで残してください。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProUpgradeCard(
    isProEnabled: Boolean,
    isBusy: Boolean,
    errorMessage: String?,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primaryContainer
    val onAccent = MaterialTheme.colorScheme.onPrimaryContainer

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "有料版（Pro）",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = onAccent
                )
            }
            Text(
                text = if (isProEnabled) "ステータス：有料版" else "ステータス：無料版",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isProEnabled) {
                Text(
                    text = "Pro をご利用中です。ありがとうございます。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Text(
                    text = "次の機能が使えます。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ProBullet("広告を表示しません")
                    ProBullet("カリキュラム・自分で学ぶの「1日あたりのテキスト学習」制限なし")
                    ProBullet("模擬テストは毎回ランダム出題")
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (!isProEnabled) {
                Button(
                    onClick = onPurchase,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isBusy
                ) {
                    Text(if (isBusy) "処理中…" else "Pro にする")
                }
                OutlinedButton(
                    onClick = onRestore,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isBusy
                ) {
                    Text("購入を復元")
                }
                Text(
                    text = "※ 実際の課金は今後 Google Play に対応予定です。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                OutlinedButton(
                    onClick = onRestore,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isBusy
                ) {
                    Text("購入を復元")
                }
            }
        }
    }
}

@Composable
private fun ProBullet(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DebugProModeCard(
    currentMode: DebugProMode.Mode,
    onChange: (DebugProMode.Mode) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("開発用（DEBUG）", fontWeight = FontWeight.Bold)

            Text(
                when (currentMode) {
                    DebugProMode.Mode.SYSTEM -> "現在の課金状態に従う（強制なし）"
                    DebugProMode.Mode.FORCE_FREE -> "無料版として動作させる（DEBUG強制）"
                    DebugProMode.Mode.FORCE_PRO -> "有料版として動作させる（DEBUG強制）"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentMode == DebugProMode.Mode.FORCE_FREE) {
                    Button(
                        onClick = { onChange(DebugProMode.Mode.FORCE_FREE) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("無料版にする")
                    }
                } else {
                    OutlinedButton(
                        onClick = { onChange(DebugProMode.Mode.FORCE_FREE) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("無料版にする")
                    }
                }

                if (currentMode == DebugProMode.Mode.FORCE_PRO) {
                    Button(
                        onClick = { onChange(DebugProMode.Mode.FORCE_PRO) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("有料版にする")
                    }
                } else {
                    OutlinedButton(
                        onClick = { onChange(DebugProMode.Mode.FORCE_PRO) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("有料版にする")
                    }
                }
            }

            if (currentMode == DebugProMode.Mode.SYSTEM) {
                Button(
                    onClick = { onChange(DebugProMode.Mode.SYSTEM) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("強制を解除")
                }
            } else {
                OutlinedButton(
                    onClick = { onChange(DebugProMode.Mode.SYSTEM) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("強制を解除")
                }
            }

            Text(
                "※ ProManager 側で DebugProMode.load(context) を参照すると、DEBUG時だけ無料版/有料版を切り替えて検証できます。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DebugTimeTravelCard(
    offsetDays: Int,
    onChange: (Int) -> Unit,
) {
    val context = LocalContext.current

    val simulatedMillis = remember(offsetDays) { DebugClock.nowMillis(context) }
    val fmt = remember {
        SimpleDateFormat("yyyy/MM/dd (EEE) HH:mm", Locale.JAPAN)
    }
    val simulatedText = remember(simulatedMillis) {
        fmt.format(Date(simulatedMillis))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("開発用（DEBUG）", fontWeight = FontWeight.Bold)

            Text(
                "日付シミュレーション：${signedDays(offsetDays)}（$simulatedText 相当）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { onChange(offsetDays - 1) },
                    modifier = Modifier.weight(1f)
                ) { Text("-1日") }

                OutlinedButton(
                    onClick = { onChange(offsetDays + 1) },
                    modifier = Modifier.weight(1f)
                ) { Text("+1日") }

                Button(
                    onClick = { onChange(0) },
                    modifier = Modifier.weight(1f)
                ) { Text("リセット") }
            }

            Text(
                "※ 復習（忘却曲線）の期限判定で DebugClock.nowMillis(context) を使うと、日付をずらして復習を出せます。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DebugFinalCelebrationTestCard(
    onOpen: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("開発用（DEBUG）", fontWeight = FontWeight.Bold)

            Text(
                "最終祝画面（FinalCelebration）をいつでも開いて、UIとボタン導線（閉じる動作）を確認できます。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onOpen,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("最終祝画面を表示")
                }
            }

            Text(
                "※ このプレビューは設定画面上の全画面ダイアログとして表示します。実際のアプリ遷移（ホームへ戻る等）の確認は AppRoot 側の導線で行ってください。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun signedDays(days: Int): String {
    return if (days >= 0) "+${days}日" else "${days}日"
}

/**
 * iOS の DebugClock と同じ目的。
 * "今" を System.currentTimeMillis() ではなく、オフセットを加味して返す。
 *
 * 使い方：
 * - nowMillis(context)
 * - loadOffsetDays(context)
 */
object DebugClock {

    private const val PREFS = "debug_clock"
    private const val KEY_OFFSET_DAYS = "debug_time_offset_days"

    fun loadOffsetDays(context: Context): Int {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_OFFSET_DAYS, 0)
    }

    fun saveOffsetDays(context: Context, days: Int) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit {
            putInt(KEY_OFFSET_DAYS, days)
        }
    }

    fun nowMillis(context: Context): Long {
        val days = loadOffsetDays(context)
        return System.currentTimeMillis() + daysToMillis(days)
    }

    private fun daysToMillis(days: Int): Long = days.toLong() * 24L * 60L * 60L * 1000L
}

// BuildConfig に依存せず「このアプリがデバッグ可能か」を判定する
private fun isDebugBuild(context: Context): Boolean {
    return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SettingsScreenPreview() {
    KikenOtsu4Theme {
        SettingsScreen(
            contentPadding = PaddingValues(0.dp)
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsScreenDarkPreview() {
    KikenOtsu4Theme {
        SettingsScreen(
            contentPadding = PaddingValues(0.dp)
        )
    }
}

object DebugProMode {

    enum class Mode {
        SYSTEM,
        FORCE_FREE,
        FORCE_PRO,
    }

    private const val PREFS = "debug_pro_mode"
    private const val KEY_MODE = "debug_pro_mode_value"

    fun load(context: Context): Mode {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_MODE, Mode.SYSTEM.name).orEmpty()
        return Mode.entries.firstOrNull { it.name == raw } ?: Mode.SYSTEM
    }

    fun save(context: Context, mode: Mode) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit {
            putString(KEY_MODE, mode.name)
        }
    }
}