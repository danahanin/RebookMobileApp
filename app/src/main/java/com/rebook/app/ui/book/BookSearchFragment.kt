package com.rebook.app.ui.book

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.rebook.app.data.api.CoverSize
import com.rebook.app.databinding.FragmentBookSearchBinding
import com.rebook.app.viewmodel.OpenLibraryViewModel

class BookSearchFragment : Fragment() {

    private var _binding: FragmentBookSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OpenLibraryViewModel by viewModels()
    private lateinit var adapter: BookSearchAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        setupSearch()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupRecyclerView() {
        adapter = BookSearchAdapter { book ->
            selectBookAndFetchDescription(book)
        }

        binding.rvResults.layoutManager = LinearLayoutManager(requireContext())
        binding.rvResults.adapter = adapter
    }

    private fun selectBookAndFetchDescription(book: com.rebook.app.data.api.OpenLibraryBook) {
        val workKey = book.key
        if (workKey.isNullOrBlank()) {
            returnBookData(book, null)
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.rvResults.alpha = 0.5f

        viewModel.fetchBookDescription(workKey) { description ->
            binding.rvResults.alpha = 1f
            returnBookData(book, description)
        }
    }

    private fun returnBookData(book: com.rebook.app.data.api.OpenLibraryBook, description: String?) {
        val navController = findNavController()
        navController.previousBackStackEntry?.savedStateHandle?.apply {
            set("selected_title", book.title ?: "")
            set("selected_author", book.getAuthorsString())
            set("selected_cover_url", book.getCoverUrl(CoverSize.LARGE))
            set("selected_description", description ?: "")
        }
        navController.popBackStack()
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener { text ->
            viewModel.searchBooks(text?.toString() ?: "")
        }

        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                viewModel.searchBooks(binding.etSearch.text?.toString() ?: "")
                true
            } else {
                false
            }
        }
    }

    private fun observeViewModel() {
        viewModel.searchResults.observe(viewLifecycleOwner) { results ->
            adapter.submitList(results)
            updateEmptyState(results.isEmpty() && !binding.etSearch.text.isNullOrBlank())
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                binding.tvError.text = error
                binding.tvError.visibility = View.VISIBLE
            } else {
                binding.tvError.visibility = View.GONE
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        val hasError = viewModel.error.value != null
        val isLoading = viewModel.isLoading.value == true

        binding.tvEmpty.visibility = if (isEmpty && !hasError && !isLoading) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
