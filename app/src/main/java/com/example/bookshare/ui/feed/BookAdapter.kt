package com.example.bookshare.ui.feed

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bookshare.databinding.ItemBookBinding
import com.example.bookshare.model.Book

class BookAdapter(
    var items: MutableList<Book>?
) : RecyclerView.Adapter<BookViewHolder>() {

    /** Optional owner-name lookup. Adapter shows ownerId if a name isn't provided. */
    var ownerNameProvider: ((ownerId: String) -> String?)? = null

    /** Optional owner-avatar lookup. */
    var ownerAvatarProvider: ((ownerId: String) -> String?)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val binding = ItemBookBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookViewHolder(binding)
    }

    override fun getItemCount(): Int = items?.size ?: 0

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = items?.get(position) ?: return
        holder.bind(book, ownerNameProvider?.invoke(book.ownerId), ownerAvatarProvider?.invoke(book.ownerId))
    }

    fun submit(newItems: List<Book>) {
        items = newItems.toMutableList()
        notifyDataSetChanged()
    }
}
