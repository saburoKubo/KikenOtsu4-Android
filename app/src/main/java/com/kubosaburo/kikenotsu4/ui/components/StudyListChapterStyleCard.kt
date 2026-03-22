@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.kubosaburo.kikenotsu4.ui.components

import android.text.TextUtils
import android.util.TypedValue
import android.widget.TextView
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * カリキュラム章一覧・「自分で学ぶ」のテキスト一覧などで共有するカード。
 * 説明文は Compose の Text ではなく **TextView（AndroidView）** で描画し、表示されない事象を避ける。
 */
@Composable
fun StudyListChapterStyleCard(
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    /** タイトル直下（説明の上）。null/空なら非表示（テキスト一覧など）。 */
    categoryLabel: String? = null,
) {
    val dark = isSystemInDarkTheme()
    val categoryColor = Color(0xFF6B7280)
    val chevronColor = Color(0xFF9CA3AF)

    // ダーク: リスト床（surfaceContainerLowest 想定）より一段明るくして行の区切りをはっきりさせる
    val cardBg =
        if (dark) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            Color.White
        }
    val cardContentColor = contentColorFor(cardBg)

    val descTrimmed = description.trim()
    val descArgb =
        if (dark) {
            MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
        } else {
            0xFF4B5563.toInt()
        }

    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.45f),
        colors = CardDefaults.cardColors(
            containerColor = cardBg,
            contentColor = cardContentColor,
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = cardContentColor,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            if (!categoryLabel.isNullOrBlank()) {
                Text(
                    text = categoryLabel.trim(),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (dark) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        categoryColor
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (descTrimmed.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 22.dp),
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth(),
                        factory = { ctx ->
                            TextView(ctx).apply {
                                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                                setTextColor(descArgb)
                                setLineSpacing(0f, 1.2f)
                            }
                        },
                        update = { tv ->
                            tv.text = descTrimmed
                            tv.maxLines = 12
                            tv.ellipsize = TextUtils.TruncateAt.END
                            tv.setTextColor(descArgb)
                        },
                    )
                }
            }
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = if (dark) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        chevronColor
                    },
                )
            }
        }
    }
}

/** リスト画面の下地（スクリーンショットの淡い青みグレーに近い色） */
@Composable
fun studyListScreenBackgroundColor(): Color {
    return if (isSystemInDarkTheme()) {
        // background / surface と同色だと行カードと溶けるため、一段暗い床にする
        MaterialTheme.colorScheme.surfaceContainerLowest
    } else {
        Color(0xFFE9EEF5)
    }
}

/**
 * テキスト一覧・ブックマーク等の「1行カード」背景。
 * ライトは白（淡い青灰床とのコントラスト）、ダークは [studyListScreenBackgroundColor] より明るいサーフェス。
 */
@Composable
fun studyListItemCardContainerColor(): Color {
    return if (isSystemInDarkTheme()) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        Color.White
    }
}
