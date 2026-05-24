package com.example.bookstore.ui.feed

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bookstore.databinding.ItemBookBinding
import com.example.bookstore.local.Book

class BookAdapter(
    var items: MutableList<Book>?
) : RecyclerView.Adapter<BookViewHolder>() {

    var listener: OnBookClickListener? = null

    /** Optional owner-name lookup. Adapter shows ownerId if a name isn't provided. */
    var ownerNameProvider: ((ownerId: String) -> String?)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val binding = ItemBookBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookViewHolder(binding, listener)
    }

    override fun getItemCount(): Int = items?.size ?: 0

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = items?.get(position) ?: return
        holder.bind(book, ownerNameProvider?.invoke(book.ownerId))
    }

    fun submit(newItems: List<Book>) {
        items = newItems.toMutableList()
        notifyDataSetChanged()
    }
}

interface OnBookClickListener {
    fun onBookClick(book: Book)
    fun onBookLongClick(book: Book): Boolean = false
}
