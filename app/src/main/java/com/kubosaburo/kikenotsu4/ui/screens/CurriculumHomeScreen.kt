
package com.kubosaburo.kikenotsu4.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kubosaburo.kikenotsu4.ui.theme.KikenOtsu4Theme
import androidx.compose.ui.viewinterop.AndroidView
import com.kubosaburo.kikenotsu4.R
import com.kubosaburo.kikenotsu4.data.CurriculumChapter
import com.kubosaburo.kikenotsu4.data.CurriculumSection
import com.kubosaburo.kikenotsu4.data.CurriculumSectionType
import com.kubosaburo.kikenotsu4.ui.components.CharacterSpeechBubbleView
import com.kubosaburo.kikenotsu4.ui.components.StudyListChapterStyleCard
import com.kubosaburo.kikenotsu4.ui.components.studyListScreenBackgroundColor
import com.kubosaburo.kikenotsu4.ui.ads.createStudyBannerAdView

@Composable
fun CurriculumHomeScreen(
    contentPadding: PaddingValues,
    chapters: List<CurriculumChapter>,
    onOpenChapter: (String) -> Unit,
    /** AppRoot の Column 内では weight(1f) を付けて高さを確保すること */
    modifier: Modifier = Modifier,
    /** false のとき一覧下部のバナー広告を出さない */
    showBannerAd: Boolean = true,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .fillMaxWidth()
            .background(studyListScreenBackgroundColor())
            .padding(contentPadding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // 上部の吹き出し（iOSの雰囲気寄せ）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                CharacterSpeechBubbleView(
                    characterImage1 = R.drawable.nicosme_normal,
                    characterImage2 = R.drawable.nicosme_openmouth,
                    durationMillis = 2200L,
                    text = "順番に学んで、合格まで一気にいこう！",
                    modifier = Modifier.fillMaxWidth(),
                    characterSize = 120.dp
                )
            }
        }

        if (chapters.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("カリキュラムがありません", fontWeight = FontWeight.Bold)
                        Text(
                            text = "assets/curriculum.json の読み込みに失敗している可能性があります。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(
                chapters,
                key = { ch -> "${ch.id}_${ch.description.hashCode()}" },
            ) { ch ->
                // 説明が空でも必ず1行は出るようにする（UI経路の切り分け用）
                val body = ch.description.trim().ifBlank {
                    ch.sections.firstOrNull { it.type == CurriculumSectionType.TEXT }
                        ?.title?.trim().orEmpty()
                }.ifBlank {
                    "（この章の説明文を表示できません。データまたはアプリの更新をご確認ください。）"
                }
                StudyListChapterStyleCard(
                    title = ch.title,
                    description = body,
                    categoryLabel = ch.categoryLabel,
                    onClick = { onOpenChapter(ch.id) }
                )
            }
        }

        item { Spacer(Modifier.height(20.dp)) }

        if (showBannerAd) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AdMobBanner()
                }
            }
        }
    }
}

@Composable
private fun AdMobBanner(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        factory = { ctx ->
            createStudyBannerAdView(ctx)
        }
    )
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun CurriculumHomeScreenPreview() {
    KikenOtsu4Theme {
        CurriculumHomeScreen(
            contentPadding = PaddingValues(0.dp),
            chapters = listOf(
                CurriculumChapter(
                    id = "ch_preview",
                    title = "第1章 法令の基礎",
                    description = "危険物の定義と分類を学びます。",
                    sections = listOf(
                        CurriculumSection(
                            id = "s1",
                            title = "テキスト",
                            type = CurriculumSectionType.TEXT,
                            refId = "text_001",
                        ),
                        CurriculumSection(
                            id = "s2",
                            title = "確認クイズ",
                            type = CurriculumSectionType.QUIZ,
                            refId = "g001",
                        ),
                    ),
                    categoryLabel = "法令",
                ),
            ),
            onOpenChapter = {},
            showBannerAd = false,
        )
    }
}

