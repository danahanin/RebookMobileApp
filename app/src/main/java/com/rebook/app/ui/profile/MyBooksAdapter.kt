package com.rebook.app.ui.profile

import android.view.LayoutInflater
import android.view.View
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
    private val onApproveClick: (Book) -> Unit,
    private val onMessagesClick: (Book) -> Unit,
    private val onEditClick: (Book) -> Unit,
    private val onDeleteClick: (Book) -> Unit
) : ListAdapter<Book, MyBooksAdapter.MyBookViewHolder>(MyBookDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyBookViewHolder {
        val binding = ItemMyBookBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyBookViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyBookViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MyBookViewHolder(
        private val binding: ItemMyBookBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(book: Book) {
            binding.tvBookTitle.text = book.title
            binding.tvBookAuthor.text = book.author

            loadBookImage(book.imageUrl)
            setStatusBadge(book.status)

            val isRequested = book.status == BookStatus.REQUESTED
            binding.btnApprove.visibility = if (isRequested) View.VISIBLE else View.GONE
            binding.btnApprove.setOnClickListener { onApproveClick(book) }

            binding.btnMessages.setOnClickListener { onMessagesClick(book) }
            binding.btnEdit.setOnClickListener { onEditClick(book) }
            binding.btnDelete.setOnClickListener { onDeleteClick(book) }
        }

        private fun loadBookImage(imageUrl: String?) {
            if (!imageUrl.isNullOrEmpty()) {
                Picasso.get()
                    .load(imageUrl)
                    .placeholder(R.color.green_light)
                    .error(R.color.green_light)
                    .into(binding.ivBookCover)
            } else {
                binding.ivBookCover.setImageDrawable(null)
                binding.ivBookCover.setBackgroundColor(
                    binding.root.context.getColor(R.color.green_light)
                )
            }
        }

        private fun setStatusBadge(status: BookStatus) {
            val context = binding.root.context
            val (text, color) = when (status) {
                BookStatus.AVAILABLE -> context.getString(R.string.status_available) to R.color.status_available
                BookStatus.REQUESTED -> context.getString(R.string.status_requested) to R.color.status_requested
                BookStatus.LENT -> context.getString(R.string.status_lent) to R.color.blue_secondary
            }
            binding.tvStatus.text = text
            binding.tvStatus.setBackgroundColor(context.getColor(color))
        }
    }

    private class MyBookDiffCallback : DiffUtil.ItemCallback<Book>() {
        override fun areItemsTheSame(oldItem: Book, newItem: Book) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Book, newItem: Book) = oldItem == newItem
    }
}
