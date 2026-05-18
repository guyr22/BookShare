package com.example.bookshare.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bookshare.databinding.ItemBookSearchResultBinding
import com.example.bookshare.local.Book
import com.squareup.picasso.Picasso

/**
 * Compact list of Google Books matches shown beneath the AddEdit search box.
 * Tapping a row reports the chosen [Book] back through [onClick].
 */
class BookSearchResultAdapter(
    private val onClick: (Book) -> Unit
) : RecyclerView.Adapter<BookSearchResultAdapter.ViewHolder>() {

    private val items = mutableListOf<Book>()

    fun submit(books: List<Book>) {
        items.clear()
        items.addAll(books)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBookSearchResultBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position])

    inner class ViewHolder(
        private val binding: ItemBookSearchResultBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) onClick(items[pos])
            }
        }

        fun bind(book: Book) {
            binding.resultTitle.text = book.title
            binding.resultAuthor.text = book.author
            if (book.coverUrl.isNotBlank()) {
                Picasso.get().load(book.coverUrl).fit().centerCrop().into(binding.resultCover)
            } else {
                binding.resultCover.setImageDrawable(null)
            }
        }
    }
}
