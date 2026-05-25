package com.example.bookshare.ui.profile

import android.text.TextUtils
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.example.bookshare.R
import com.example.bookshare.databinding.ItemReviewCardBinding
import com.example.bookshare.local.Book
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.squareup.picasso.Picasso

class ReviewViewHolder(
    private val binding: ItemReviewCardBinding,
    private val listener: OnReviewClickListener?
) : RecyclerView.ViewHolder(binding.root) {

    private var current: Book? = null

    init {
        // Tapping the card opens the book for editing.
        binding.root.setOnClickListener {
            current?.let { listener?.onReviewClick(it) }
        }
    }

    fun bind(book: Book, displayName: String?, avatarUrl: String?) {
        current = book
        binding.authorName.text = displayName ?: "You"
        binding.bookTitle.text = book.title
        binding.ratingBar.rating = book.rating.toFloat()

        bindCollapsibleReview(book.review.ifBlank { book.description.ifBlank { book.author } })
        bindDescriptionPopup(book)

        if (!avatarUrl.isNullOrBlank()) {
            Picasso.get().load(avatarUrl).fit().centerCrop().into(binding.authorAvatar)
        } else {
            binding.authorAvatar.setImageDrawable(null)
        }

        if (book.coverUrl.isNotBlank()) {
            Picasso.get().load(book.coverUrl).fit().centerCrop().into(binding.bookCover)
        } else {
            binding.bookCover.setImageDrawable(null)
        }
    }

    /** Collapsible review with a Read More / Show Less toggle (see BookViewHolder). */
    private fun bindCollapsibleReview(review: String) {
        val reviewView = binding.reviewText
        val toggle = binding.reviewToggle
        val context = reviewView.context

        reviewView.text = review
        var expanded = false

        fun render() {
            if (expanded) {
                reviewView.maxLines = Integer.MAX_VALUE
                reviewView.ellipsize = null
                toggle.text = context.getString(R.string.feed_card_show_less)
            } else {
                reviewView.maxLines = COLLAPSED_MAX_LINES
                reviewView.ellipsize = TextUtils.TruncateAt.END
                toggle.text = context.getString(R.string.feed_card_read_more)
            }
        }

        render()
        toggle.visibility = View.GONE

        val toggleClick = View.OnClickListener {
            expanded = !expanded
            render()
            toggle.visibility = View.VISIBLE
        }
        toggle.setOnClickListener(toggleClick)
        reviewView.setOnClickListener(toggleClick)

        reviewView.post {
            if (expanded) return@post
            val layout = reviewView.layout ?: return@post
            val lastLine = layout.lineCount - 1
            val overflowed = lastLine >= 0 && layout.getEllipsisCount(lastLine) > 0
            toggle.visibility = if (overflowed) View.VISIBLE else View.GONE
        }
    }

    private fun bindDescriptionPopup(book: Book) {
        val hasDescription = book.description.isNotBlank()
        binding.viewDescription.visibility = if (hasDescription) View.VISIBLE else View.GONE
        binding.viewDescription.setOnClickListener(
            if (hasDescription) View.OnClickListener { showDescriptionDialog(book) } else null
        )
    }

    private fun showDescriptionDialog(book: Book) {
        val context = binding.root.context
        MaterialAlertDialogBuilder(context)
            .setTitle(book.title)
            .setMessage(book.description.ifBlank { context.getString(R.string.description_dialog_empty) })
            .setPositiveButton(R.string.description_dialog_close, null)
            .show()
    }

    private companion object {
        const val COLLAPSED_MAX_LINES = 2
    }
}
