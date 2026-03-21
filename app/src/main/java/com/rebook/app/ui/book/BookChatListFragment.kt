package com.rebook.app.ui.book

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.rebook.app.databinding.FragmentBookChatListBinding
import com.rebook.app.viewmodel.ChatListViewModel

class BookChatListFragment : Fragment() {

    private var _binding: FragmentBookChatListBinding? = null
    private val binding get() = _binding!!

    private val args: BookChatListFragmentArgs by navArgs()
    private val chatListViewModel: ChatListViewModel by viewModels()

    private lateinit var adapter: ChatThreadsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookChatListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bookId = args.bookId
        if (bookId.isBlank()) {
            findNavController().popBackStack()
            return
        }

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        adapter = ChatThreadsAdapter { row ->
            findNavController().navigate(
                BookChatListFragmentDirections.actionBookChatListToBookMessage(
                    bookId,
                    row.buyerId,
                    row.displayLabel
                )
            )
        }
        binding.recyclerThreads.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerThreads.adapter = adapter

        chatListViewModel.startListening(bookId)

        chatListViewModel.threadRows.observe(viewLifecycleOwner) { rows ->
            adapter.submitList(rows)
            updateEmptyState()
        }

        chatListViewModel.loading.observe(viewLifecycleOwner) {
            binding.progressList.isVisible = it == true
            updateEmptyState()
        }

        chatListViewModel.error.observe(viewLifecycleOwner) { err ->
            if (err != null) {
                Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show()
            }
            updateEmptyState()
        }
    }

    private fun updateEmptyState() {
        val loading = chatListViewModel.loading.value == true
        val rows = chatListViewModel.threadRows.value.orEmpty()
        val err = chatListViewModel.error.value
        binding.tvEmpty.isVisible = rows.isEmpty() && !loading && err == null
        binding.recyclerThreads.isVisible = rows.isNotEmpty() || loading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
