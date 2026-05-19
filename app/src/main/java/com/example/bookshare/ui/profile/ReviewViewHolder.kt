package com.example.bookshare.ui.profile

import android.text.TextUtils
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.example.bookshare.R
import com.example.bookshare.databinding.ItemReviewCardBinding
import com.example.bookshare.local.Book
import com.squareup.picasso.Picasso

class ReviewViewHolder(
    private val binding: ItemReviewCardBinding,
    private val listener: OnReviewClickListener?
) : RecyclerView.ViewHolder(binding.root) {

    private var current: Book? = null

    init {
        binding.root.setOnClickListener {
            current?.let { listener?.onReviewClick(it) }
        }
    }

    fun bind(book: Book, displayName: String?, avatarUrl: String?) {
        current = book
        binding.authorName.text = displayName ?: "You"
        binding.bookTitle.text = book.title
        bindCollapsibleDescription(
            if (book.description.isNotBlank()) book.description else book.author
        )

        if (!avatarUrl.isNullOrBlank()) {
            Picasso.get()
                .load(avatarUrl)
                .fit()
                .centerCrop()
                .into(binding.authorAvatar)
        } else {
            binding.authorAvatar.setImageDrawable(null)
        }

        if (book.coverUrl.isNotBlank()) {
            Picasso.get()
                .load(book.coverUrl)
                .fit()
                .centerCrop()
                .into(binding.bookCover)
        } else {
            binding.bookCover.setImageDrawable(null)
        }

    }

    /**
     * Collapses the description to [COLLAPSED_MAX_LINES] and only shows the toggle
     * when the text is truncated. Tapping it expands to the full text ("Show Less");
     * tapping again collapses back ("Read More"). State is reset on every bind so
     * recycled rows don't keep a prior row's expansion.
     */
    private fun bindCollapsibleDescription(description: String) {
        val descriptionView = binding.bookDescription
        val toggle = binding.readMore
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
            // Once revealed, the toggle stays visible so the user can collapse again.
            toggle.visibility = View.VISIBLE
        }

        // getEllipsisCount is only valid after layout; only matters while collapsed.
        descriptionView.post {
            if (expanded) return@post
            val layout = descriptionView.layout ?: return@post
            val lastLine = layout.lineCount - 1
            val overflowed = lastLine >= 0 && layout.getEllipsisCount(lastLine) > 0
            toggle.visibility = if (overflowed) View.VISIBLE else View.GONE
        }
    }

    private companion object {
        const val COLLAPSED_MAX_LINES = 2
    }
}
