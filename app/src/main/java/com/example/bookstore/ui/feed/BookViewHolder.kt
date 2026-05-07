package com.example.bookstore.ui.feed

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
        binding.descriptionTextView.text = book.description
        binding.ownerNameTextView.text = ownerName ?: book.ownerId

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
