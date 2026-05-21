package com.example.bookshare.ui.feed

import androidx.recyclerview.widget.RecyclerView
import com.example.bookshare.databinding.ItemBookBinding
import com.example.bookshare.local.Book
import com.example.bookshare.ui.bindCollapsibleDescription
import com.example.bookshare.ui.formatDate
import com.squareup.picasso.Picasso

class BookViewHolder(
    private val binding: ItemBookBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(book: Book, ownerName: String?, ownerAvatarUrl: String?) {
        binding.titleTextView.text = book.title
        binding.authorTextView.text = book.author
        binding.ownerNameTextView.text = ownerName ?: book.ownerId
        binding.dateTextView.text = formatDate(book.lastUpdated)
        bindCollapsibleDescription(binding.descriptionTextView, binding.readMoreTextView, book.description)

        if (!ownerAvatarUrl.isNullOrBlank()) {
            Picasso.get()
                .load(ownerAvatarUrl)
                .fit()
                .centerCrop()
                .into(binding.ownerAvatarImageView)
        } else {
            binding.ownerAvatarImageView.setImageDrawable(null)
        }

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

}
