package com.rebook.app.ui.book

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rebook.app.data.model.ChatThreadRow
import com.rebook.app.databinding.ItemChatThreadBinding

class ChatThreadsAdapter(
    private val onClick: (ChatThreadRow) -> Unit
) : ListAdapter<ChatThreadRow, ChatThreadsAdapter.Holder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemChatThreadBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class Holder(
        private val binding: ItemChatThreadBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(row: ChatThreadRow) {
            binding.tvLabel.text = row.displayLabel
            binding.root.setOnClickListener { onClick(row) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<ChatThreadRow>() {
        override fun areItemsTheSame(a: ChatThreadRow, b: ChatThreadRow) = a.buyerId == b.buyerId
        override fun areContentsTheSame(a: ChatThreadRow, b: ChatThreadRow) = a == b
    }
}
