package com.rebook.app.ui.book

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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

            val approveBtn = binding.btnApprove
            val requestBtn = binding.btnRequest
            val messageBtn = binding.btnMessage

            if (isOwnBook) {
                requestBtn.visibility = View.GONE
                messageBtn.visibility = View.VISIBLE
                messageBtn.text = getString(R.string.btn_book_message_threads)
                messageBtn.setOnClickListener {
                    findNavController().navigate(
                        BookDetailsFragmentDirections.actionBookDetailsToBookChatList(book.id)
                    )
                }

                if (book.status == BookStatus.REQUESTED) {
                    approveBtn.visibility = View.VISIBLE
                    approveBtn.setOnClickListener {
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.dialog_approve_title)
                            .setMessage(R.string.dialog_approve_message)
                            .setNegativeButton(R.string.btn_cancel, null)
                            .setPositiveButton(R.string.btn_approve_request) { _, _ ->
                                bookViewModel.approveRequest(book.id)
                            }
                            .show()
                    }
                } else {
                    approveBtn.visibility = View.GONE
                }
            } else {
                approveBtn.visibility = View.GONE
                messageBtn.visibility = View.VISIBLE
                messageBtn.text = getString(R.string.btn_send_message_to_owner)

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
                    book.status == BookStatus.LENT && isRequestedByMe -> {
                        requestBtn.visibility = View.VISIBLE
                        requestBtn.isEnabled = false
                        requestBtn.text = getString(R.string.status_approved)
                        requestBtn.setBackgroundColor(requireContext().getColor(R.color.blue_secondary))
                        requestBtn.setOnClickListener(null)
                    }
                    book.status == BookStatus.REQUESTED -> {
                        requestBtn.visibility = View.VISIBLE
                        requestBtn.isEnabled = false
                        requestBtn.text = "Requested"
                        requestBtn.setBackgroundColor(requireContext().getColor(android.R.color.darker_gray))
                        requestBtn.setOnClickListener(null)
                    }
                    else -> {
                        // LENT to someone else — show Unavailable
                        requestBtn.visibility = View.VISIBLE
                        requestBtn.isEnabled = false
                        requestBtn.text = getString(R.string.status_unavailable)
                        requestBtn.setBackgroundColor(requireContext().getColor(android.R.color.darker_gray))
                        requestBtn.setOnClickListener(null)
                    }
                }

                val uid = currentUserId
                messageBtn.setOnClickListener {
                    if (uid != null) {
                        findNavController().navigate(
                            BookDetailsFragmentDirections.actionBookDetailsToBookMessage(
                                book.id,
                                uid,
                                null
                            )
                        )
                    } else {
                        Toast.makeText(requireContext(), R.string.error_generic, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun observeOperationState(bookId: String) {
        bookViewModel.operationState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is BookOperationState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnApprove.isEnabled = false
                    binding.btnRequest.isEnabled = false
                    binding.btnMessage.isEnabled = false
                }
                is BookOperationState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnApprove.isEnabled = true
                    binding.btnRequest.isEnabled = true
                    binding.btnMessage.isEnabled = true
                    bookViewModel.resetOperationState()
                    bookViewModel.loadBook(bookId)
                }
                is BookOperationState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnApprove.isEnabled = true
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
