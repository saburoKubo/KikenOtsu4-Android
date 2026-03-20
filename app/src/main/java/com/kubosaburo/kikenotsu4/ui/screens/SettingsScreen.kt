package com.kubosaburo.kikenotsu4.ui.screens

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.kubosaburo.kikenotsu4.ui.theme.KikenOtsu4Theme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.edit
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
    onProModeChanged: (() -> Unit)? = null,
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