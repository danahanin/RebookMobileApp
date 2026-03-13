package com.rebook.app.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rebook.app.R
import com.rebook.app.data.model.Book
import com.rebook.app.data.model.BookStatus
import com.rebook.app.databinding.ItemMyBookBinding
import com.squareup.picasso.Picasso

class MyBooksAdapter(
    private val onEditClick: (Book) -> Unit,
    private val onDeleteClick: (Book) -> Unit
) : ListAdapter<Book, MyBooksAdapter.ViewHolder>(BookDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMyBookBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemMyBookBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(book: Book) {
            binding.tvTitle.text = book.title
            binding.tvAuthor.text = book.author

            bindBookImage(book.imageUrl)
            bindStatus(book.status)

            binding.btnEdit.setOnClickListener { onEditClick(book) }
            binding.btnDelete.setOnClickListener { onDeleteClick(book) }
        }

        private fun bindBookImage(imageUrl: String?) {
            if (!imageUrl.isNullOrEmpty()) {
                Picasso.get()
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .into(binding.ivBookCover)
            } else {
                binding.ivBookCover.setImageResource(R.drawable.ic_profile_placeholder)
            }
        }

        private fun bindStatus(status: BookStatus) {
            val context = binding.root.context
            when (status) {
                BookStatus.AVAILABLE -> {
                    binding.chipStatus.text = context.getString(R.string.status_available)
                    binding.chipStatus.setChipBackgroundColorResource(R.color.green_primary)
                    binding.chipStatus.setTextColor(context.getColor(R.color.white))
                }
                BookStatus.REQUESTED -> {
                    binding.chipStatus.text = context.getString(R.string.status_requested)
                    binding.chipStatus.setChipBackgroundColorResource(R.color.orange_accent)
                    binding.chipStatus.setTextColor(context.getColor(R.color.white))
                }
                BookStatus.LENT -> {
                    binding.chipStatus.text = context.getString(R.string.status_lent)
                    binding.chipStatus.setChipBackgroundColorResource(R.color.gray_background)
                    binding.chipStatus.setTextColor(context.getColor(R.color.text_secondary))
                }
            }
        }
    }

    private class BookDiffCallback : DiffUtil.ItemCallback<Book>() {
        override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean =
            oldItem == newItem
    }
}
