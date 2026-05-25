package com.example.bookshare.ui.feed

import android.text.TextUtils
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.example.bookshare.R
import com.example.bookshare.databinding.ItemBookBinding
import com.example.bookshare.local.Book
import com.squareup.picasso.Picasso

class BookViewHolder(
    private val binding: ItemBookBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(book: Book, ownerName: String?, ownerAvatarUrl: String?) {
        binding.titleTextView.text = book.title
        binding.authorTextView.text = book.author
        binding.ownerNameTextView.text = ownerName ?: book.ownerId
        bindCollapsibleDescription(book.description)

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

    /**
     * Shows the description collapsed to [COLLAPSED_MAX_LINES] and reveals the
     * toggle link only when the text is actually truncated. Tapping it expands to
     * the full text ("Show Less"); tapping again collapses back ("Read More").
     * State is reset on every bind so recycled rows don't inherit a previous
     * row's expanded state.
     */
    private fun bindCollapsibleDescription(description: String) {
        val descriptionView = binding.descriptionTextView
        val toggle = binding.readMoreTextView
        val context = descriptionView.context

        descriptionView.text = description
        var expanded = false

        fun render() {
            if (expanded) {
                descriptionView.maxLines = Integer.MAX_VALUE
                descriptionView.ellipsize = null
                toggle.text = context.getString(R.string.feed_card_show_less)
            } else {
                descriptionView.maxLines = COLLAPSED_MAX_LINES
                descriptionView.ellipsize = TextUtils.TruncateAt.END
                toggle.text = context.getString(R.string.feed_card_read_more)
            }
        }

        render()
        toggle.visibility = View.GONE
        toggle.setOnClickListener {
            expanded = !expanded
            render()
            // Once revealed, the toggle stays visible so the user can collapse again.
            toggle.visibility = View.VISIBLE
        }

        // getEllipsisCount is only valid after layout; only matters while collapsed.
        descriptionView.post {
            if (expanded) return@post
            val layout = descriptionView.layout ?: return@post
            val lastLine = layout.lineCount - 1
            val overflowed = lastLine >= 0 && layout.getEllipsisCount(lastLine) > 0
            toggle.visibility = if (overflowed) View.VISIBLE else View.GONE
        }
    }

    private companion object {
        const val COLLAPSED_MAX_LINES = 2
    }
}
