package com.kubosaburo.kikenotsu4.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kubosaburo.kikenotsu4.BuildConfig

private fun openInBrowser(context: Context, url: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

private fun openSupportEmail(context: Context) {
    val version = BuildConfig.VERSION_NAME
    val subject = Uri.encode("危険物乙4 学習アプリのお問い合わせ")
    val body = Uri.encode(
        "（ここにお問い合わせ内容をご記入ください）\n\n" +
            "----------\n" +
            "アプリバージョン: $version\n" +
            "端末: ${Build.MANUFACTURER} ${Build.MODEL}\n" +
            "OS: Android ${Build.VERSION.RELEASE}"
    )
    val uri = Uri.parse("mailto:${SupportLegalTexts.SUPPORT_EMAIL}?subject=$subject&body=$body")
    runCatching {
        val intent = Intent(Intent.ACTION_SENDTO, uri)
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

@Composable
fun SupportAppInfoSection() {
    val context = LocalContext.current
    var showHowTo by remember { mutableStateOf(false) }

    if (showHowTo) {
        ScrollableLegalDialog(
            title = "使い方",
            body = SupportLegalTexts.HOW_TO_USE,
            onDismiss = { showHowTo = false }
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
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "サポート / アプリ情報",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = onAccent
                )
            }

            SupportInfoRow(label = "使い方", onClick = { showHowTo = true })
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SupportInfoRow(label = "公式サイト") {
                openInBrowser(context, SupportLegalTexts.OFFICIAL_SITE_URL)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SupportInfoRow(label = "プライバシーポリシー") {
                openInBrowser(context, SupportLegalTexts.PRIVACY_POLICY_URL)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SupportInfoRow(label = "お問い合わせ") {
                openSupportEmail(context)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SupportInfoRow(label = "利用規約") {
                openInBrowser(context, SupportLegalTexts.TERMS_OF_SERVICE_URL)
            }

            HorizontalDivider(
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "バージョン",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = BuildConfig.VERSION_NAME,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SupportInfoRow(
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ScrollableLegalDialog(
    title: String,
    body: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(text = body, style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("閉じる")
            }
        }
    )
}
