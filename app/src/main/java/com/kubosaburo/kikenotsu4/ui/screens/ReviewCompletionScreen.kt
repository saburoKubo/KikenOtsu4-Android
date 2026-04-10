package com.kubosaburo.kikenotsu4.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kubosaburo.kikenotsu4.ui.theme.KikenOtsu4Theme
import com.kubosaburo.kikenotsu4.R
import com.kubosaburo.kikenotsu4.ui.components.CharacterSpeechBubbleView

@Composable
fun ReviewCompletionScreen(
    contentPadding: PaddingValues,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        CharacterSpeechBubbleView(
            characterImage1 = R.drawable.nicosme_nicoji_katakumi2,
            characterImage2 = R.drawable.nicosme_nicoji_katakumi0,
            durationMillis = 2000L,
            text = "復習ぜんぶクリア！この積み重ねが、合格への近道やで！",
            modifier = Modifier.fillMaxWidth(),
            characterSize = 120.dp
        )

        Text(
            text = "おつかれさま！次の学習に進もう",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("つづける")
        }
    }
}

@Preview(showBackground = true, heightDp = 600)
@Composable
private fun ReviewCompletionScreenPreview() {
    KikenOtsu4Theme {
        ReviewCompletionScreen(
            contentPadding = PaddingValues(0.dp),
            onContinue = {},
        )
    }
}

