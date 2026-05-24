package com.example.bookshare.ui.feed

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.example.bookshare.R
import com.example.bookshare.databinding.ItemBookBinding
import com.example.bookshare.local.Book
import com.example.bookshare.ui.bindCollapsibleDescription
import com.example.bookshare.ui.formatDate
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.squareup.picasso.Picasso

class BookViewHolder(
    private val binding: ItemBookBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(book: Book, ownerName: String?, ownerAvatarUrl: String?) {
        binding.titleTextView.text = book.title
        binding.authorTextView.text = book.author
        binding.ownerNameTextView.text = ownerName ?: book.ownerId
        binding.dateTextView.text = formatDate(book.lastUpdated)
        binding.ratingBar.rating = book.rating.toFloat()

        // The user's review is the primary text; fall back to the description for
        // older books with no review. Reuses the shared collapsible helper so the
        // review gets the same Read More / Show Less toggle behaviour.
        bindCollapsibleDescription(
            binding.reviewTextView,
            binding.reviewToggleTextView,
            book.review.ifBlank { book.description }
        )
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
}
