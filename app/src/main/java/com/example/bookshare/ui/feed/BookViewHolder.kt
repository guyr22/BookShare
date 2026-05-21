package com.example.bookshare.ui.feed

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
        binding.reviewTextView.text = book.review.ifBlank { book.description }

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
     * Wires the "View description" link (and tapping the review itself) to open a
     * dialog with the book's full description. The link is hidden when there is no
     * description to show.
     */
    private fun bindDescriptionPopup(book: Book) {
        val hasDescription = book.description.isNotBlank()
        binding.viewDescriptionTextView.visibility = if (hasDescription) View.VISIBLE else View.GONE

        val open = if (hasDescription) View.OnClickListener { showDescriptionDialog(book) } else null
        binding.viewDescriptionTextView.setOnClickListener(open)
        binding.reviewTextView.setOnClickListener(open)
        binding.reviewTextView.isClickable = hasDescription
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
