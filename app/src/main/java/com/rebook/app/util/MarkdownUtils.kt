package com.rebook.app.util

import android.os.Build
import android.text.Html
import android.text.Spanned

object MarkdownUtils {

    fun toSpanned(markdown: String): Spanned {
        val html = markdown
            .replace(Regex("^######\\s+(.+)$", RegexOption.MULTILINE), "<h6>$1</h6>")
            .replace(Regex("^#####\\s+(.+)$", RegexOption.MULTILINE), "<h5>$1</h5>")
            .replace(Regex("^####\\s+(.+)$", RegexOption.MULTILINE), "<h4>$1</h4>")
            .replace(Regex("^###\\s+(.+)$", RegexOption.MULTILINE), "<h3>$1</h3>")
            .replace(Regex("^##\\s+(.+)$", RegexOption.MULTILINE), "<h2>$1</h2>")
            .replace(Regex("^#\\s+(.+)$", RegexOption.MULTILINE), "<h1>$1</h1>")
            .replace(Regex("\\*\\*(.+?)\\*\\*"), "<b>$1</b>")
            .replace(Regex("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)"), "<i>$1</i>")
            .replace(Regex("\\[(.+?)\\]\\((.+?)\\)"), "<a href=\"$2\">$1</a>")
            .replace("\n", "<br>")

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(html)
        }
    }
}
