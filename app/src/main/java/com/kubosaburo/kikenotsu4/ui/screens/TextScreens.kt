package com.kubosaburo.kikenotsu4.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kubosaburo.kikenotsu4.R
import com.kubosaburo.kikenotsu4.data.TextItem
import com.kubosaburo.kikenotsu4.ui.components.CharacterSpeechBubbleView
import com.kubosaburo.kikenotsu4.ui.parseBoldMarkdown

@Composable
fun TextListScreen(
    items: List<TextItem>,
    contentPadding: PaddingValues,
    onOpen: (String) -> Unit,
    isEnabled: (String) -> Boolean = { true }
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header: character + speech bubble
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                CharacterSpeechBubbleView(
                    characterImage1 = R.drawable.nicosme_normal,
                    characterImage2 = R.drawable.nicosme_openmouth,
                    durationMillis = 1800L,
                    text = parseBoldMarkdown("学びたいテキストを選んでね"),
                    modifier = Modifier.wrapContentWidth(),
                    characterSize = 120.dp,
                )
            }
        }

        items(items) { t ->
            val enabled = isEnabled(t.id)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (enabled) 1f else 0.45f)
                    .clickable { onOpen(t.id) },
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "📄",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Text(
                        text = t.title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
fun TextDetailScreen(
    textItem: TextItem,
    contentPadding: PaddingValues,
    onStartQuiz: (String) -> Unit
) {
    var zoomImageResId by remember { mutableStateOf<Int?>(null) }
    if (zoomImageResId != null) {
        var scale by remember(zoomImageResId) { mutableFloatStateOf(1f) }
        var offsetX by remember(zoomImageResId) { mutableFloatStateOf(0f) }
        var offsetY by remember(zoomImageResId) { mutableFloatStateOf(0f) }

        val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
            val newScale = (scale * zoomChange).coerceIn(1f, 4f)
            val scaleRatio = if (scale == 0f) 1f else newScale / scale
            scale = newScale

            if (scale > 1f || newScale > 1f) {
                offsetX += panChange.x * scaleRatio
                offsetY += panChange.y * scaleRatio
            }

            if (scale <= 1f) {
                offsetX = 0f
                offsetY = 0f
            }
        }

        LaunchedEffect(scale) {
            if (scale <= 1f) {
                offsetX = 0f
                offsetY = 0f
            }
        }

        Dialog(
            onDismissRequest = {
                @Suppress("ASSIGNED_VALUE_IS_NEVER_READ")
                zoomImageResId = null
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .statusBarsPadding(),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                @Suppress("ASSIGNED_VALUE_IS_NEVER_READ")
                                zoomImageResId = null
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = zoomImageResId!!),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offsetX
                                    translationY = offsetY
                                }
                                .transformable(state = transformableState),
                            contentScale = ContentScale.Fit
                        )
                    }

                    TextButton(
                        onClick = {
                            @Suppress("ASSIGNED_VALUE_IS_NEVER_READ")
                            zoomImageResId = null
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                    ) {
                        Text("閉じる")
                    }

                    if (scale > 1f) {
                        TextButton(
                            onClick = {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp)
                        ) {
                            Text("リセット")
                        }
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TextTitleCard(title = textItem.title)
        }

        items(textItem.content.indices.toList()) { idx ->
            val line = textItem.content[idx]
            val (c1, c2) = characterFor(idx)

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CharacterSpeechBubbleView(
                    characterImage1 = c1,
                    characterImage2 = c2,
                    durationMillis = 1400L,
                    text = parseBoldMarkdown(line),
                    modifier = Modifier.wrapContentWidth(),
                    characterSize = 72.dp,
                )
            }
        }

        textItem.image?.takeIf { it.isNotBlank() }?.let { imageName ->
            item {
                val resId = remember(imageName) {
                    val fieldName = imageName.trim().lowercase().substringBeforeLast('.')
                    runCatching {
                        R.drawable::class.java.getField(fieldName).getInt(null)
                    }.getOrDefault(0)
                }

                if (resId != 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Image(
                            painter = painterResource(id = resId),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { zoomImageResId = resId },
                            contentScale = ContentScale.FillWidth
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            text = "画像がありません",
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (textItem.table.isNotEmpty()) {
            items(textItem.table) { row ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (row.category.isNotBlank()) {
                            // Header label with soft green background (like iOS screenshot)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x3322C55E))
                                    .padding(vertical = 10.dp, horizontal = 12.dp)
                            ) {
                                Text(
                                    text = row.category,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (row.name.isNotBlank()) {
                            Text(
                                row.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (row.description.isNotBlank()) {
                            Text(parseBoldMarkdown(row.description))
                        }
                        if (row.item.isNotBlank()) {
                            Text(parseBoldMarkdown(row.item))
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = { onStartQuiz(textItem.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 20.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text("このテキストの問題へ ▶︎")
            }
        }
    }
}


@Composable
private fun TextTitleCard(title: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x3322C55E)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Text(
            text = title,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 14.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

private fun characterFor(index: Int): Pair<Int, Int?> {
    return when (index % 3) {
        0 -> R.drawable.nico_professor_normal to R.drawable.nico_professor_left
        1 -> R.drawable.nico_idle to R.drawable.nico_idle_wink
        else -> R.drawable.nico_flagdown to R.drawable.nico_flagup
    }
}
