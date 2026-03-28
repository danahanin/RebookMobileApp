package com.rebook.app.ui.explore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rebook.app.R
import com.rebook.app.data.model.BookStatus
import com.rebook.app.databinding.FragmentExploreBinding
import com.rebook.app.util.observeBookOperations
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
        binding.swipeRefresh.setOnRefreshListener {
            bookViewModel.syncBooks { binding.swipeRefresh.isRefreshing = false }
        }
        setupFilterButton()
        observeStatusFilter()
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
                val outerNavController = Navigation.findNavController(
                    requireActivity(), R.id.nav_host_fragment
                )
                outerNavController.navigate(
                    R.id.bookDetailsFragment,
                    bundleOf("bookId" to book.id)
                )
            }
        )
        val layoutManager = LinearLayoutManager(requireContext())
        binding.rvBooks.apply {
            this.layoutManager = layoutManager
            adapter = bookAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy <= 0) return
                    val totalItems = layoutManager.itemCount
                    val lastVisible = layoutManager.findLastVisibleItemPosition()
                    if (lastVisible >= totalItems - 4 && bookViewModel.hasMoreBooks) {
                        bookViewModel.loadMoreBooks()
                    }
                }
            })
        }
    }

    private fun observeBooks() {
        bookViewModel.filteredBooks.observe(viewLifecycleOwner) { books ->
            bookAdapter.submitList(books)
            binding.tvEmpty.visibility = if (books.isEmpty()) View.VISIBLE else View.GONE
            binding.rvBooks.visibility = if (books.isEmpty()) View.GONE else View.VISIBLE
        }
        bookViewModel.isLoadingMore.observe(viewLifecycleOwner) { loading ->
            binding.progressBarLoadMore.visibility = if (loading) View.VISIBLE else View.GONE
        }
    }

    private fun observeOperationState() {
        observeBookOperations(bookViewModel, { show ->
            binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        }, onSuccess = {
            bookViewModel.syncBooks()
        })
    }

    private fun setupSearch() {
        binding.etSearch.doAfterTextChanged { text ->
            bookViewModel.setSearchQuery(text?.toString() ?: "")
        }
    }

    private fun observeStatusFilter() {
        bookViewModel.statusFilter.observe(viewLifecycleOwner) { filter ->
            binding.tvSectionTitle.text = getString(
                if (filter == BookStatus.AVAILABLE) R.string.title_books_available
                else R.string.title_all_books
            )
        }
    }

    private fun setupFilterButton() {
        val options = arrayOf(getString(R.string.filter_all_books), getString(R.string.filter_available_only))
        binding.btnFilter.setOnClickListener {
            val currentFilter = bookViewModel.statusFilter.value
            val checkedItem = if (currentFilter == BookStatus.AVAILABLE) 1 else 0

            AlertDialog.Builder(requireContext())
                .setTitle(R.string.title_filter_books)
                .setSingleChoiceItems(options, checkedItem) { dialog, which ->
                    when (which) {
                        0 -> bookViewModel.setStatusFilter(null)
                        1 -> bookViewModel.setStatusFilter(BookStatus.AVAILABLE)
                    }
                    dialog.dismiss()
                }
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
