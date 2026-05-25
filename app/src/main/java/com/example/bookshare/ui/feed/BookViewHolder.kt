package com.example.bookshare.ui.feed

import android.text.TextUtils
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.example.bookshare.R
import com.example.bookshare.databinding.ItemBookBinding
import com.example.bookshare.local.Book
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.squareup.picasso.Picasso

class BookViewHolder(
    private val binding: ItemBookBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(book: Book, ownerName: String?, ownerAvatarUrl: String?) {
        binding.titleTextView.text = book.title
        binding.authorTextView.text = book.author
        binding.ownerNameTextView.text = ownerName ?: book.ownerId
        binding.ratingBar.rating = book.rating.toFloat()

        // The user's review is the primary text; fall back to the description for
        // older books that have no review yet.
        bindCollapsibleReview(book.review.ifBlank { book.description })
        bindDescriptionPopup(book)

        if (!ownerAvatarUrl.isNullOrBlank()) {
            Picasso.get().load(ownerAvatarUrl).fit().centerCrop().into(binding.ownerAvatarImageView)
        } else {
            binding.ownerAvatarImageView.setImageDrawable(null)
        }

        if (book.coverUrl.isNotBlank()) {
            Picasso.get().load(book.coverUrl).fit().centerCrop().into(binding.coverImageView)
        } else {
            binding.coverImageView.setImageDrawable(null)
        }
    }

    /**
     * Shows the review collapsed to [COLLAPSED_MAX_LINES] and reveals a toggle only
     * when the text is actually truncated. Tapping the toggle (or the review) expands
     * to the full text ("Show Less") and collapses back ("Read More"). State resets
     * on every bind so recycled rows don't inherit a previous row's expansion.
     */
    private fun bindCollapsibleReview(review: String) {
        val reviewView = binding.reviewTextView
        val toggle = binding.reviewToggleTextView
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
            toggle.visibility = View.VISIBLE   // stays visible so the user can collapse again
        }
        toggle.setOnClickListener(toggleClick)
        reviewView.setOnClickListener(toggleClick)

        // getEllipsisCount is only valid after layout; only matters while collapsed.
        reviewView.post {
            if (expanded) return@post
            val layout = reviewView.layout ?: return@post
            val lastLine = layout.lineCount - 1
            val overflowed = lastLine >= 0 && layout.getEllipsisCount(lastLine) > 0
            toggle.visibility = if (overflowed) View.VISIBLE else View.GONE
        }
    }

    /**
     * Wires the "View description" link to open a dialog with the book's full
     * description; hidden when there is no description.
     */
    private fun bindDescriptionPopup(book: Book) {
        val hasDescription = book.description.isNotBlank()
        binding.viewDescriptionTextView.visibility = if (hasDescription) View.VISIBLE else View.GONE
        binding.viewDescriptionTextView.setOnClickListener(
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
        const val COLLAPSED_MAX_LINES = 3
    }
}
