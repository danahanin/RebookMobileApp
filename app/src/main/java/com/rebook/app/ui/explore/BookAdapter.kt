package com.rebook.app.ui.explore

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.rebook.app.R
import com.rebook.app.data.model.Book
import com.rebook.app.data.model.BookStatus
import com.rebook.app.databinding.ItemBookBinding
import com.squareup.picasso.Picasso

class BookAdapter(
    private val onRequestClick: (Book) -> Unit,
    private val onUnrequestClick: (Book) -> Unit,
    private val onBookClick: (Book) -> Unit
) : ListAdapter<Book, BookAdapter.BookViewHolder>(BookDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val binding = ItemBookBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BookViewHolder(private val binding: ItemBookBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(book: Book) {
            binding.tvBookTitle.text = book.title
            binding.tvBookAuthor.text = "Author: ${book.author}"
            binding.tvBookOwner.text = "Owner: ${book.ownerName.ifBlank { "Unknown" }}"

            if (!book.imageUrl.isNullOrEmpty()) {
                Picasso.get()
                    .load(book.imageUrl)
                    .placeholder(R.color.green_light)
                    .error(R.color.green_light)
                    .into(binding.ivBookCover)
            } else {
                binding.ivBookCover.setImageDrawable(null)
                binding.ivBookCover.setBackgroundColor(
                    binding.root.context.getColor(R.color.green_light)
                )
            }

            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            val isOwnBook = book.ownerId == currentUserId
            val isRequestedByMe = book.requestedById == currentUserId

            val btn = binding.btnRequest

            when {
                isOwnBook -> {
                    // Own book — hide button entirely
                    btn.isEnabled = false
                    btn.visibility = android.view.View.GONE
                }
                book.status == BookStatus.AVAILABLE -> {
                    // Available and not yours — green Request button
                    btn.visibility = android.view.View.VISIBLE
                    btn.isEnabled = true
                    btn.text = "Request"
                    btn.setBackgroundColor(binding.root.context.getColor(R.color.green_primary))
                    btn.setOnClickListener { onRequestClick(book) }
                }
                book.status == BookStatus.REQUESTED && isRequestedByMe -> {
                    // You requested it — red Unrequest button
                    btn.visibility = android.view.View.VISIBLE
                    btn.isEnabled = true
                    btn.text = "Unrequest"
                    btn.setBackgroundColor(binding.root.context.getColor(R.color.error_red))
                    btn.setOnClickListener { onUnrequestClick(book) }
                }
                book.status == BookStatus.REQUESTED -> {
                    // Requested by someone else — gray disabled button
                    btn.visibility = android.view.View.VISIBLE
                    btn.isEnabled = false
                    btn.text = "Requested"
                    btn.setBackgroundColor(binding.root.context.getColor(android.R.color.darker_gray))
                    btn.setOnClickListener(null)
                }
                book.status == BookStatus.LENT && isRequestedByMe -> {
                    // My request was approved — blue Approved badge
                    btn.visibility = android.view.View.VISIBLE
                    btn.isEnabled = false
                    btn.text = binding.root.context.getString(R.string.status_approved)
                    btn.setBackgroundColor(binding.root.context.getColor(R.color.blue_secondary))
                    btn.setOnClickListener(null)
                }
                else -> {
                    // LENT to someone else — show Unavailable
                    btn.visibility = android.view.View.VISIBLE
                    btn.isEnabled = false
                    btn.text = binding.root.context.getString(R.string.status_unavailable)
                    btn.setBackgroundColor(binding.root.context.getColor(android.R.color.darker_gray))
                    btn.setOnClickListener(null)
                }
            }

            binding.root.setOnClickListener { onBookClick(book) }
        }
    }

    private class BookDiffCallback : DiffUtil.ItemCallback<Book>() {
        override fun areItemsTheSame(oldItem: Book, newItem: Book) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Book, newItem: Book) = oldItem == newItem
    }
}
