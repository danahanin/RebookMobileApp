package com.rebook.app.ui.book

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.rebook.app.R
import com.rebook.app.data.model.BookStatus
import com.rebook.app.databinding.FragmentBookDetailsBinding
import com.rebook.app.util.BookOperationState
import com.rebook.app.viewmodel.BookViewModel
import com.squareup.picasso.Picasso

class BookDetailsFragment : Fragment() {

    private var _binding: FragmentBookDetailsBinding? = null
    private val binding get() = _binding!!

    private val bookViewModel: BookViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bookId = arguments?.getString("bookId")
        if (bookId == null) {
            findNavController().popBackStack()
            return
        }

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        bookViewModel.loadBook(bookId)
        observeBook()
        observeOperationState(bookId)
    }

    private fun observeBook() {
        bookViewModel.selectedBook.observe(viewLifecycleOwner) { book ->
            book ?: return@observe

            binding.tvTitle.text = book.title
            binding.tvAuthor.text = "Author: ${book.author}"
            binding.tvOwner.text = "Owner: ${book.ownerName.ifBlank { "Unknown" }}"
            binding.tvDescription.text = book.description.ifBlank { "No description provided." }

            if (!book.imageUrl.isNullOrEmpty()) {
                Picasso.get()
                    .load(book.imageUrl)
                    .placeholder(R.color.green_light)
                    .error(R.color.green_light)
                    .into(binding.ivBookImage)
            }

            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            val isOwnBook = book.ownerId == currentUserId
            val isRequestedByMe = book.requestedById == currentUserId

            val requestBtn = binding.btnRequest
            val messageBtn = binding.btnMessage

            if (isOwnBook) {
                requestBtn.visibility = View.GONE
                messageBtn.visibility = View.GONE
            } else {
                messageBtn.visibility = View.VISIBLE

                when {
                    book.status == BookStatus.AVAILABLE -> {
                        requestBtn.visibility = View.VISIBLE
                        requestBtn.isEnabled = true
                        requestBtn.text = "Request Book"
                        requestBtn.setBackgroundColor(requireContext().getColor(R.color.green_primary))
                        requestBtn.setOnClickListener {
                            bookViewModel.requestBook(book.id)
                        }
                    }
                    book.status == BookStatus.REQUESTED && isRequestedByMe -> {
                        requestBtn.visibility = View.VISIBLE
                        requestBtn.isEnabled = true
                        requestBtn.text = "Unrequest"
                        requestBtn.setBackgroundColor(requireContext().getColor(R.color.error_red))
                        requestBtn.setOnClickListener {
                            bookViewModel.unrequestBook(book.id)
                        }
                    }
                    book.status == BookStatus.REQUESTED -> {
                        requestBtn.visibility = View.VISIBLE
                        requestBtn.isEnabled = false
                        requestBtn.text = "Requested"
                        requestBtn.setBackgroundColor(requireContext().getColor(android.R.color.darker_gray))
                        requestBtn.setOnClickListener(null)
                    }
                    else -> {
                        requestBtn.visibility = View.GONE
                    }
                }

                messageBtn.setOnClickListener {
                    // TODO Milestone 3 Person B: navigate to messaging screen
                    Toast.makeText(requireContext(), "Messaging coming soon!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun observeOperationState(bookId: String) {
        bookViewModel.operationState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is BookOperationState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnRequest.isEnabled = false
                    binding.btnMessage.isEnabled = false
                }
                is BookOperationState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnRequest.isEnabled = true
                    binding.btnMessage.isEnabled = true
                    bookViewModel.resetOperationState()
                    // Reload the book to reflect updated status
                    bookViewModel.loadBook(bookId)
                }
                is BookOperationState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnRequest.isEnabled = true
                    binding.btnMessage.isEnabled = true
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    bookViewModel.resetOperationState()
                }
                is BookOperationState.Idle -> {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bookViewModel.clearSelectedBook()
        _binding = null
    }
}
