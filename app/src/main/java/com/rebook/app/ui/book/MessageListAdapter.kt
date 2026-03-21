package com.rebook.app.ui.book

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rebook.app.R
import com.rebook.app.data.model.BookMessage
import com.rebook.app.databinding.ItemMessageBinding

class MessageListAdapter(
    private val currentUserId: String?
) : ListAdapter<BookMessage, MessageListAdapter.MessageViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position), currentUserId)
    }

    class MessageViewHolder(
        private val binding: ItemMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: BookMessage, currentUserId: String?) {
            val isMe = currentUserId != null && message.senderId == currentUserId
            binding.tvMessageText.text = message.text

            val start = binding.spacerStart.layoutParams as LinearLayout.LayoutParams
            val end = binding.spacerEnd.layoutParams as LinearLayout.LayoutParams
            if (isMe) {
                start.width = 0
                start.height = 0
                start.weight = 1f
                end.width = 0
                end.height = 0
                end.weight = 0f
                binding.cardMessage.setCardBackgroundColor(
                    ContextCompat.getColor(binding.root.context, R.color.green_light)
                )
                binding.tvMessageText.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.black)
                )
            } else {
                start.width = 0
                start.height = 0
                start.weight = 0f
                end.width = 0
                end.height = 0
                end.weight = 1f
                val surfaceVariant = com.google.android.material.color.MaterialColors.getColor(
                    binding.root,
                    com.google.android.material.R.attr.colorSurfaceVariant,
                    ContextCompat.getColor(binding.root.context, R.color.gray_background)
                )
                binding.cardMessage.setCardBackgroundColor(surfaceVariant)
                val onSurface = com.google.android.material.color.MaterialColors.getColor(
                    binding.root,
                    com.google.android.material.R.attr.colorOnSurface,
                    ContextCompat.getColor(binding.root.context, R.color.black)
                )
                binding.tvMessageText.setTextColor(onSurface)
            }
        }
    }

    private object Diff : DiffUtil.ItemCallback<BookMessage>() {
        override fun areItemsTheSame(oldItem: BookMessage, newItem: BookMessage): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: BookMessage, newItem: BookMessage): Boolean =
            oldItem == newItem
    }
}
