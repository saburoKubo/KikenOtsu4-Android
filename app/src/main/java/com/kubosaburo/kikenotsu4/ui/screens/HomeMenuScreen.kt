
package com.kubosaburo.kikenotsu4.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.kubosaburo.kikenotsu4.R
import com.kubosaburo.kikenotsu4.data.StartingPraiseProvider
import com.kubosaburo.kikenotsu4.ui.components.CharacterSpeechBubbleView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun HomeMenuScreen(
    contentPadding: PaddingValues,
    onGoCurriculum: () -> Unit,
    onGoFreeStudy: () -> Unit,
    onGoMock: () -> Unit,
) {
    val provider = StartingPraiseProvider(androidx.compose.ui.platform.LocalContext.current)
    val phrase = provider.randomStarting()

    Column(
        modifier = Modifier
            .padding(contentPadding)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
//        // Header
//        Text(
//            text = "ホーム",
//            style = MaterialTheme.typography.titleMedium,
//            fontWeight = FontWeight.Bold
//        )

        // Character bubble
        CharacterSpeechBubbleView(
            characterImage1 = R.drawable.nicosme_normal,
            characterImage2 = R.drawable.nicosme_run,
            durationMillis = 2200L,
            text = phrase,
            modifier = Modifier.fillMaxWidth(),
            characterSize = 120.dp
        )

        // Cards
        HomeMenuCard(
            title = "カリキュラムで学ぶ",
            subtitle = "順番に読んで、すぐ確認",
            leadingIcon = Icons.Filled.School,
            containerColor = Color(0xFFE7F0FF),
            onClick = onGoCurriculum
        )

        HomeMenuCard(
            title = "自分で学ぶ",
            subtitle = "テキスト・ブックマーク・検索",
            leadingIcon = Icons.AutoMirrored.Filled.MenuBook,
            containerColor = Color(0xFFFFF2E8),
            onClick = onGoFreeStudy
        )

        HomeMenuCard(
            title = "模擬テストで学ぶ",
            subtitle = "本番形式で実力チェック",
            leadingIcon = Icons.AutoMirrored.Filled.Assignment,
            containerColor = Color(0xFFEAF7EE),
            onClick = onGoMock
        )

        Spacer(modifier = Modifier.size(6.dp))

        // ホーム画面下部のバナー広告（領域が分かるように背景付き）
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { context ->
                    AdView(context).apply {
                        // テスト用バナー広告ユニットID（本番前にご自身のIDへ差し替え）
                        adUnitId = "ca-app-pub-3940256099942544/6300978111"
                        setAdSize(AdSize.BANNER)
                        loadAd(AdRequest.Builder().build())
                    }
                }
            )
        }
    }
}

@Composable
private fun HomeMenuCard(
    title: String,
    subtitle: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Leading icon
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(34.dp)
                    .background(Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                    .padding(6.dp)
            )

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

