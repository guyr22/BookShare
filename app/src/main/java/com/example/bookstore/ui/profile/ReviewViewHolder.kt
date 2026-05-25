package com.example.bookstore.ui.profile

import android.text.TextUtils
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.example.bookstore.databinding.ItemReviewCardBinding
import com.example.bookstore.local.Book
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

    fun bind(book: Book, displayName: String?) {
        current = book
        binding.authorName.text = displayName ?: "You"
        binding.bookTitle.text = book.title
        bindCollapsibleDescription(
            if (book.description.isNotBlank()) book.description else book.author
        )

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
     * Collapses the description to [COLLAPSED_MAX_LINES] and only shows "Read More"
     * when the text is truncated. Tapping it expands to the full text. State is
     * reset on every bind so recycled rows don't keep a prior row's expansion.
     */
    private fun bindCollapsibleDescription(description: String) {
        val descriptionView = binding.bookDescription
        val readMore = binding.readMore

        descriptionView.text = description
        descriptionView.maxLines = COLLAPSED_MAX_LINES
        descriptionView.ellipsize = TextUtils.TruncateAt.END
        readMore.visibility = View.GONE

        readMore.setOnClickListener {
            descriptionView.maxLines = Integer.MAX_VALUE
            descriptionView.ellipsize = null
            readMore.visibility = View.GONE
        }

        // getEllipsisCount is only valid after the view has been laid out.
        descriptionView.post {
            val layout = descriptionView.layout ?: return@post
            val lastLine = layout.lineCount - 1
            val overflowed = lastLine >= 0 && layout.getEllipsisCount(lastLine) > 0
            readMore.visibility = if (overflowed) View.VISIBLE else View.GONE
        }
    }

    private companion object {
        const val COLLAPSED_MAX_LINES = 2
    }
}
