package com.example.bookshare.ui

import android.text.TextUtils
import android.view.View
import android.widget.TextView
import com.example.bookshare.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val COLLAPSED_MAX_LINES = 2

fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val thisYear = Calendar.getInstance().get(Calendar.YEAR)
    val dateYear = Calendar.getInstance().also { it.time = date }.get(Calendar.YEAR)
    val pattern = if (dateYear == thisYear) "MMM d" else "MMM d, yyyy"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(date)
}

fun bindCollapsibleDescription(descriptionView: TextView, toggle: TextView, description: String) {
    val context = descriptionView.context
    descriptionView.text = description
    var expanded = false

    fun render() {
        if (expanded) {
            descriptionView.maxLines = Integer.MAX_VALUE
            descriptionView.ellipsize = null
            toggle.text = context.getString(R.string.feed_card_show_less)
        } else {
            descriptionView.maxLines = COLLAPSED_MAX_LINES
            descriptionView.ellipsize = TextUtils.TruncateAt.END
            toggle.text = context.getString(R.string.feed_card_read_more)
        }
    }

    render()
    toggle.visibility = View.GONE
    toggle.setOnClickListener {
        expanded = !expanded
        render()
        toggle.visibility = View.VISIBLE
    }

    descriptionView.post {
        if (expanded) return@post
        val layout = descriptionView.layout ?: return@post
        val lastLine = layout.lineCount - 1
        val overflowed = lastLine >= 0 && layout.getEllipsisCount(lastLine) > 0
        toggle.visibility = if (overflowed) View.VISIBLE else View.GONE
    }
}
