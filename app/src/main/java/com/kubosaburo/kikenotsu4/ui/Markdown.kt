package com.kubosaburo.kikenotsu4.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight

// **bold** だけ対応（ネスト/エスケープは未対応）
fun parseBoldMarkdown(src: String): AnnotatedString {
    val open = "**"
    val close = "**"

    return buildAnnotatedString {
        var i = 0
        while (i < src.length) {
            val start = src.indexOf(open, startIndex = i)
            if (start == -1) {
                append(src.substring(i))
                break
            }
            if (start > i) append(src.substring(i, start))

            val end = src.indexOf(close, startIndex = start + open.length)
            if (end == -1) {
                append(src.substring(start))
                break
            }

            val boldText = src.substring(start + open.length, end)
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append(boldText)
            pop()

            i = end + close.length
        }
    }
}
