package com.example.bookshare.ui.profile

import androidx.recyclerview.widget.RecyclerView
import com.example.bookshare.databinding.ItemReviewCardBinding
import com.example.bookshare.local.Book
import com.example.bookshare.ui.bindCollapsibleDescription
import com.example.bookshare.ui.formatDate
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
        binding.dateTextView.text = formatDate(book.lastUpdated)
        binding.bookTitle.text = book.title
        bindCollapsibleDescription(
            binding.bookDescription,
            binding.readMore,
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

}
