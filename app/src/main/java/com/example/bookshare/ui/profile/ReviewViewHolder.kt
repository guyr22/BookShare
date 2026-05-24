package com.example.bookshare.ui.profile

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.example.bookshare.R
import com.example.bookshare.databinding.ItemReviewCardBinding
import com.example.bookshare.local.Book
import com.example.bookshare.ui.bindCollapsibleDescription
import com.example.bookshare.ui.formatDate
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
        binding.dateTextView.text = formatDate(book.lastUpdated)
        binding.bookTitle.text = book.title
        binding.ratingBar.rating = book.rating.toFloat()

        bindCollapsibleDescription(
            binding.reviewText,
            binding.reviewToggle,
            book.review.ifBlank { book.description.ifBlank { book.author } }
        )
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
}
