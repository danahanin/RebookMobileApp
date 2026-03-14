package com.rebook.app.ui.explore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.rebook.app.databinding.FragmentExploreBinding
import com.rebook.app.util.BookOperationState
import com.rebook.app.viewmodel.BookViewModel

class ExploreFragment : Fragment() {

    private var _binding: FragmentExploreBinding? = null
    private val binding get() = _binding!!

    private val bookViewModel: BookViewModel by activityViewModels()
    private lateinit var bookAdapter: BookAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExploreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeBooks()
        observeOperationState()
        setupSearch()
        bookViewModel.syncBooks()
    }

    private fun setupRecyclerView() {
        bookAdapter = BookAdapter(
            onRequestClick = { book ->
                bookViewModel.requestBook(book.id)
            },
            onUnrequestClick = { book ->
                bookViewModel.unrequestBook(book.id)
            },
            onBookClick = { book ->
                // TODO: navigate to BookDetailsFragment with book.id
            }
        )
        binding.rvBooks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = bookAdapter
        }
    }

    private fun observeBooks() {
        bookViewModel.filteredBooks.observe(viewLifecycleOwner) { books ->
            bookAdapter.submitList(books)
            binding.tvEmpty.visibility = if (books.isEmpty()) View.VISIBLE else View.GONE
            binding.rvBooks.visibility = if (books.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun observeOperationState() {
        bookViewModel.operationState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is BookOperationState.Loading -> binding.progressBar.visibility = View.VISIBLE
                is BookOperationState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    bookViewModel.syncBooks()
                    bookViewModel.resetOperationState()
                }
                is BookOperationState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    bookViewModel.resetOperationState()
                }
                is BookOperationState.Idle -> binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun setupSearch() {
        binding.etSearch.doAfterTextChanged { text ->
            bookViewModel.setSearchQuery(text?.toString() ?: "")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
