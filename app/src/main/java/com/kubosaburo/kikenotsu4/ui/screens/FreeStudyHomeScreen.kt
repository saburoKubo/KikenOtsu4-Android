package com.kubosaburo.kikenotsu4.ui.screens

import androidx.annotation.DrawableRes
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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kubosaburo.kikenotsu4.R
import com.kubosaburo.kikenotsu4.ui.components.CharacterSpeechBubbleView
import androidx.compose.foundation.layout.wrapContentWidth

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
                durationMillis = 1600L,
                text = phrase,
                modifier = Modifier.wrapContentWidth(),
                characterSize = 120.dp,
                bubbleBorderColor = Color(0xFFFF4DA6) // pink-ish
            )
        }

        // 1) テキスト＋問題で学ぶ
        FreeStudyCategoryCard(
            title = "テキスト＋問題で学ぶ",
            subtitle = "読んで → すぐ確認",
            icon = Icons.AutoMirrored.Filled.MenuBook,
            backgroundColor = Color(0xFFFF8A1E),
            onClick = onTextQuiz
        )

        // 2) ブックマークで学ぶ
        FreeStudyCategoryCard(
            title = "ブックマークで学ぶ",
            subtitle = "保存したテキスト・問題",
            icon = Icons.Filled.Bookmark,
            backgroundColor = Color(0xFF1E88FF),
            onClick = onBookmarks
        )

        // 3) 本日の復習
        FreeStudyCategoryCard(
            title = "本日の復習",
            subtitle = "忘れかけをサクッと確認",
            icon = Icons.AutoMirrored.Filled.Undo,
            backgroundColor = Color(0xFF8B5CF6),
            onClick = onTodayReview
        )

        // 4) 検索して学ぶ
        FreeStudyCategoryCard(
            title = "検索して学ぶ",
            subtitle = "キーワード検索",
            icon = Icons.Filled.Search,
            backgroundColor = Color(0xFFFF4DA6),
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
    backgroundColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(22.dp)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp)
            .clickable { onClick() },
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }

            Box(
                modifier = Modifier.width(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(">", color = Color.White, style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
