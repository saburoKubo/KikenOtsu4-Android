
package com.kubosaburo.kikenotsu4.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
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
import com.kubosaburo.kikenotsu4.ui.ads.createStudyBannerAdView

@Composable
fun HomeMenuScreen(
    contentPadding: PaddingValues,
    // カリキュラム進捗（null の場合はカードを非表示）
    totalSections: Int? = null,
    completedSections: Int? = null,
    /** [com.kubosaburo.kikenotsu4.data.CurriculumProgressStore.loadLap]。2 以上のとき「○周目」を進捗付近に表示 */
    curriculumLap: Int = 1,
    // 今日の復習件数（null の場合は 0 扱い）
    todayReviewCount: Int? = null,
    /** false のとき下部バナー広告を出さない（有料版・デバッグ強制 Pro など） */
    showBannerAd: Boolean = true,
    onGoCurriculum: () -> Unit,
    onGoFreeStudy: () -> Unit,
    onGoMock: () -> Unit,
) {
    val provider = StartingPraiseProvider(androidx.compose.ui.platform.LocalContext.current)
    val phrase = provider.randomStarting()
    val dark = isSystemInDarkTheme()
    val scheme = MaterialTheme.colorScheme
    val startCardBg =
        if (dark) scheme.surfaceContainerHigh else Color.White
    // contentColorFor(Color.White) が端末・テーマで不適切になる事例があるため、ライトは固定の濃色にする
    val startCardPrimaryText =
        if (dark) scheme.onSurface else Color(0xFF1A1A1A)
    val startCardSecondaryText =
        if (dark) scheme.onSurfaceVariant else Color(0xFF5C5F66)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 「学習スタート」カード（トップの白いカード風）
        if (totalSections != null && completedSections != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = startCardBg,
                    contentColor = startCardPrimaryText,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 上部の小さなラベルと説明
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "学習スタート",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = startCardPrimaryText,
                            )
                            Text(
                                text = "自分に合った学び方を選んで始めよう",
                                style = MaterialTheme.typography.bodySmall,
                                color = startCardSecondaryText,
                            )
                        }

                        // 右上の小さなハウス風アイコンエリア（雰囲気だけ寄せる）
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    color = if (dark) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                    } else {
                                        Color(0xFFFFF3E0)
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.School,
                                contentDescription = null,
                                tint = if (dark) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color(0xFFFF8A1E)
                                },
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "進捗",
                                style = MaterialTheme.typography.bodySmall,
                                color = startCardSecondaryText,
                            )
                            Text(
                                text = "${completedSections}/${totalSections}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = startCardPrimaryText,
                            )
                            if (curriculumLap >= 2) {
                                Text(
                                    text = "${curriculumLap}周目",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF8A1E)
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "復習",
                                style = MaterialTheme.typography.bodySmall,
                                color = startCardSecondaryText,
                            )
                            Text(
                                text = "${todayReviewCount ?: 0}件",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = startCardPrimaryText,
                            )
                        }
                    }
                }
            }
        }

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

        if (showBannerAd) {
            // ホーム画面下部のバナー広告（領域が分かるように背景付き）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (dark) MaterialTheme.colorScheme.surfaceContainerHighest
                        else Color(0xFFE0E0E0),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { context ->
                        createStudyBannerAdView(context)
                    }
                )
            }
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
    val dark = isSystemInDarkTheme()
    val adaptedBg =
        if (dark) MaterialTheme.colorScheme.surfaceContainerHigh else containerColor
    val contentCol = contentColorFor(adaptedBg)
    val subtitleCol =
        if (dark) MaterialTheme.colorScheme.onSurfaceVariant
        else contentCol.copy(alpha = 0.68f)
    val iconTint =
        if (dark) MaterialTheme.colorScheme.onSurfaceVariant
        else contentCol.copy(alpha = 0.55f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = adaptedBg,
            contentColor = contentCol,
        ),
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
                tint = iconTint,
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        if (dark) contentCol.copy(alpha = 0.12f)
                        else Color(0x33FFFFFF),
                        RoundedCornerShape(12.dp)
                    )
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
                    color = subtitleCol,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = iconTint
            )
        }
    }
}

