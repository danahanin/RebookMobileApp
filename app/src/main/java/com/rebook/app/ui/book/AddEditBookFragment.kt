package com.rebook.app.ui.book

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.rebook.app.R
import com.rebook.app.data.model.Book
import com.rebook.app.databinding.FragmentAddEditBookBinding
import com.rebook.app.util.BookOperationState
import com.rebook.app.viewmodel.BookViewModel
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch

class AddEditBookFragment : Fragment() {

    private var _binding: FragmentAddEditBookBinding? = null
    private val binding get() = _binding!!

    private val bookViewModel: BookViewModel by activityViewModels()

    private var bookId: String? = null
    private var existingBook: Book? = null
    private var selectedImageUri: Uri? = null
    private var isUploading = false

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { handleImageSelected(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditBookBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bookId = arguments?.getString("bookId")
        binding.toolbar.title = if (bookId != null) "Edit Book" else "Add Book"
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        setupClickListeners()
        observeOperationState()
        loadBookIfEditing()
    }

    private fun setupClickListeners() {
        binding.cardImage.setOnClickListener { pickImage() }
        binding.btnSave.setOnClickListener { saveBook() }
    }

    private fun pickImage() {
        pickImageLauncher.launch("image/*")
    }

    private fun handleImageSelected(uri: Uri) {
        selectedImageUri = uri
        binding.ivBookImage.visibility = View.VISIBLE
        binding.layoutAddPhoto.visibility = View.GONE
        Picasso.get().load(uri).into(binding.ivBookImage)
    }

    private fun loadBookIfEditing() {
        val id = bookId ?: return

        lifecycleScope.launch {
            existingBook = bookViewModel.getBookById(id)
            existingBook?.let { book ->
                binding.etTitle.setText(book.title)
                binding.etAuthor.setText(book.author)
                binding.etDescription.setText(book.description)

                if (!book.imageUrl.isNullOrEmpty()) {
                    binding.ivBookImage.visibility = View.VISIBLE
                    binding.layoutAddPhoto.visibility = View.GONE
                    Picasso.get()
                        .load(book.imageUrl)
                        .placeholder(R.color.green_light)
                        .into(binding.ivBookImage)
                }
            }
        }
    }

    private fun saveBook() {
        if (isUploading) return

        val title = binding.etTitle.text?.toString()?.trim() ?: ""
        val author = binding.etAuthor.text?.toString()?.trim() ?: ""
        val description = binding.etDescription.text?.toString()?.trim() ?: ""

        if (!validateInput(title, author)) return

        isUploading = true
        showLoading(true)

        lifecycleScope.launch {
            val imageUrl = uploadImageIfNeeded()
            performSave(title, author, description, imageUrl)
        }
    }

    private fun validateInput(title: String, author: String): Boolean {
        var isValid = true

        if (title.isEmpty()) {
            binding.tilTitle.error = getString(R.string.error_empty_title)
            isValid = false
        } else {
            binding.tilTitle.error = null
        }

        if (author.isEmpty()) {
            binding.tilAuthor.error = getString(R.string.error_empty_author)
            isValid = false
        } else {
            binding.tilAuthor.error = null
        }

        return isValid
    }

    private suspend fun uploadImageIfNeeded(): String? {
        val uri = selectedImageUri
        if (uri != null) {
            val result = com.rebook.app.data.repository.BookRepository(requireContext())
                .uploadBookImage(uri)
            return result.getOrNull()
        }
        return existingBook?.imageUrl
    }

    private fun performSave(title: String, author: String, description: String, imageUrl: String?) {
        val existing = existingBook
        if (existing != null) {
            val updatedBook = existing.copy(
                title = title,
                author = author,
                description = description,
                imageUrl = imageUrl
            )
            bookViewModel.updateBook(updatedBook)
        } else {
            val newBook = Book(
                title = title,
                author = author,
                description = description,
                imageUrl = imageUrl
            )
            bookViewModel.addBook(newBook)
        }
    }

    private fun observeOperationState() {
        bookViewModel.operationState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is BookOperationState.Loading -> showLoading(true)
                is BookOperationState.Success -> handleSuccess()
                is BookOperationState.Error -> handleError(state.message)
                is BookOperationState.Idle -> showLoading(false)
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !show
    }

    private fun handleSuccess() {
        isUploading = false
        showLoading(false)
        bookViewModel.resetOperationState()
        findNavController().popBackStack()
    }

    private fun handleError(message: String) {
        isUploading = false
        showLoading(false)
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        bookViewModel.resetOperationState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
