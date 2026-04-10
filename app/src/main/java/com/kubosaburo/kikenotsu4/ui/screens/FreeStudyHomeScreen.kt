package com.kubosaburo.kikenotsu4.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kubosaburo.kikenotsu4.ui.theme.KikenOtsu4Theme
import com.kubosaburo.kikenotsu4.R
import com.kubosaburo.kikenotsu4.ui.components.CharacterSpeechBubbleView

/**
 * iOS の FreeStudyHomeView 相当。
 * - キャラ吹き出し
 * - 4つのメニュー（テキスト+問題 / ブックマーク / 本日の復習 / 検索）
 *
 * NOTE:
 * - TopAppBar は MainActivity 側で出す想定（この画面は中身だけ）
 */
@Suppress("unused")
@Composable
fun FreeStudyHomeScreen(
    contentPadding: PaddingValues,
    onTextQuiz: () -> Unit,
    onBookmarks: () -> Unit,
    onTodayReview: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    // まずは固定文言。後で外から注入してもOK
    phrase: String = "気になるところを、自由に学習しよう",
    @DrawableRes characterImage1: Int = R.drawable.ic_launcher_foreground,
    @DrawableRes characterImage2: Int? = null,
) {
    val dark = isSystemInDarkTheme()
    val bubbleOutline =
        if (dark) MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        else Color(0xFFF6A6C7)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(Modifier.height(12.dp))

        // 🗨 キャラ吹き出し
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CharacterSpeechBubbleView(
                characterImage1 = characterImage1,
                characterImage2 = characterImage2,
                durationMillis = 2600L,
                text = phrase,
                modifier = Modifier,
                characterSize = 120.dp,
                bubbleBorderColor = bubbleOutline
            )
        }

        // 1) テキスト＋問題で学ぶ
        FreeStudyCategoryCard(
            title = "テキスト＋問題で学ぶ",
            subtitle = "読んで → すぐ確認",
            icon = Icons.AutoMirrored.Filled.MenuBook,
            iconBackgroundColor = Color(0xFFF59E0B),
            onClick = onTextQuiz
        )

        // 2) ブックマークで学ぶ
        FreeStudyCategoryCard(
            title = "ブックマークで学ぶ",
            subtitle = "保存したテキスト・問題",
            icon = Icons.Filled.Bookmark,
            iconBackgroundColor = Color(0xFF3B82F6),
            onClick = onBookmarks
        )

        // 3) 本日の復習
        FreeStudyCategoryCard(
            title = "本日の復習",
            subtitle = "忘れかけをサクッと確認",
            icon = Icons.AutoMirrored.Filled.Undo,
            iconBackgroundColor = Color(0xFFA855F7),
            onClick = onTodayReview
        )

        // 4) 検索して学ぶ
        FreeStudyCategoryCard(
            title = "検索して学ぶ",
            subtitle = "キーワード検索",
            icon = Icons.Filled.Search,
            iconBackgroundColor = Color(0xFFEF4444),
            onClick = onSearch
        )

        Spacer(Modifier.height(60.dp))
    }
}

@Composable
private fun FreeStudyCategoryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBackgroundColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(22.dp)
    val dark = isSystemInDarkTheme()
    val cardBg =
        if (dark) MaterialTheme.colorScheme.surfaceContainerHigh
        else Color(0xFFF2F3F7)
    val contentCol = contentColorFor(cardBg)
    val subtitleCol =
        if (dark) MaterialTheme.colorScheme.onSurfaceVariant
        else contentCol.copy(alpha = 0.65f)
    val chevronTint =
        if (dark) MaterialTheme.colorScheme.onSurfaceVariant
        else contentCol.copy(alpha = 0.5f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp)
            .clickable { onClick() },
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = cardBg,
            contentColor = contentCol,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left colored icon tile
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clickable { onClick() }
                    .padding(0.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = iconBackgroundColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentCol
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = subtitleCol
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = chevronTint,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun FreeStudyHomeScreenPreview() {
    KikenOtsu4Theme {
        FreeStudyHomeScreen(
            contentPadding = PaddingValues(0.dp),
            onTextQuiz = {},
            onBookmarks = {},
            onTodayReview = {},
            onSearch = {},
        )
    }
}
