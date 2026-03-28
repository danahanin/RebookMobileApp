package com.rebook.app.ui.book

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rebook.app.R
import com.rebook.app.data.api.OpenLibraryBook
import com.rebook.app.databinding.ItemBookSearchResultBinding
import com.squareup.picasso.Picasso

class BookSearchAdapter(
    private val onBookClick: (OpenLibraryBook) -> Unit
) : ListAdapter<OpenLibraryBook, BookSearchAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBookSearchResultBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemBookSearchResultBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(book: OpenLibraryBook) {
            binding.tvTitle.text = book.title ?: "Unknown Title"
            binding.tvAuthor.text = book.getAuthorsString().ifEmpty { "Unknown Author" }
            binding.tvYear.text = book.firstPublishYear?.toString() ?: ""

            val coverUrl = book.getCoverUrl()
            if (coverUrl != null) {
                Picasso.get()
                    .load(coverUrl)
                    .placeholder(R.color.green_light)
                    .error(R.color.green_light)
                    .into(binding.ivCover)
            } else {
                binding.ivCover.setImageDrawable(null)
                binding.ivCover.setBackgroundColor(
                    binding.root.context.getColor(R.color.green_light)
                )
            }

            binding.root.setOnClickListener { onBookClick(book) }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<OpenLibraryBook>() {
        override fun areItemsTheSame(oldItem: OpenLibraryBook, newItem: OpenLibraryBook): Boolean {
            return oldItem.key == newItem.key
        }

        override fun areContentsTheSame(oldItem: OpenLibraryBook, newItem: OpenLibraryBook): Boolean {
            return oldItem == newItem
        }
    }
}
