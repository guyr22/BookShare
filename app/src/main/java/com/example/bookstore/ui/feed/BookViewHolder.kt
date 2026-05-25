package com.example.bookstore.ui.feed

import android.text.TextUtils
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.example.bookstore.databinding.ItemBookBinding
import com.example.bookstore.local.Book
import com.squareup.picasso.Picasso

class BookViewHolder(
    private val binding: ItemBookBinding,
    private val listener: OnBookClickListener?
) : RecyclerView.ViewHolder(binding.root) {

    private var currentBook: Book? = null

    init {
        binding.bookCard.setOnClickListener {
            currentBook?.let { listener?.onBookClick(it) }
        }
        binding.bookCard.setOnLongClickListener {
            val book = currentBook ?: return@setOnLongClickListener false
            listener?.onBookLongClick(book) ?: false
        }
    }

    fun bind(book: Book, ownerName: String?) {
        currentBook = book

        binding.titleTextView.text = book.title
        binding.authorTextView.text = book.author
        binding.ownerNameTextView.text = ownerName ?: book.ownerId
        bindCollapsibleDescription(book.description)

        if (book.coverUrl.isNotBlank()) {
            Picasso.get()
                .load(book.coverUrl)
                .fit()
                .centerCrop()
                .into(binding.coverImageView)
        } else {
            binding.coverImageView.setImageDrawable(null)
        }
    }

    /**
     * Shows the description collapsed to [COLLAPSED_MAX_LINES] and reveals the
     * "Read More" link only when the text is actually truncated. Tapping it
     * expands to the full text and hides the link. State is reset on every bind
     * so recycled rows don't inherit a previous row's expanded state.
     */
    private fun bindCollapsibleDescription(description: String) {
        val descriptionView = binding.descriptionTextView
        val readMore = binding.readMoreTextView

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
