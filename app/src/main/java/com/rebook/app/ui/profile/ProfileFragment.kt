package com.rebook.app.ui.profile

import android.os.Bundle
import androidx.core.os.bundleOf
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rebook.app.R
import com.rebook.app.data.model.Book
import com.rebook.app.databinding.FragmentProfileBinding
import com.rebook.app.util.BookOperationState
import com.rebook.app.viewmodel.AuthViewModel
import com.rebook.app.viewmodel.BookViewModel
import com.rebook.app.viewmodel.UserViewModel
import com.squareup.picasso.Picasso

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by activityViewModels()
    private val userViewModel: UserViewModel by activityViewModels()
    private val bookViewModel: BookViewModel by activityViewModels()

    private lateinit var myBooksAdapter: MyBooksAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeUser()
        observeMyBooks()
        observeBookOperations()
        setupButtons()
    }

    override fun onResume() {
        super.onResume()
        userViewModel.loadCurrentUser()
        bookViewModel.syncBooks()
    }

    private fun setupRecyclerView() {
        myBooksAdapter = MyBooksAdapter(
            onMessagesClick = { book -> navigateToBookMessages(book.id) },
            onEditClick = { book -> navigateToEditBook(book.id) },
            onDeleteClick = { book -> showDeleteConfirmation(book.id) }
        )
        binding.rvMyBooks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = myBooksAdapter
        }
    }

    private fun observeUser() {
        userViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                binding.tvUserName.text = user.displayName.ifEmpty { "No name" }
                binding.tvUserEmail.text = user.email
                loadProfileImage(user.profileImageUrl)
            }
        }
    }

    private fun loadProfileImage(url: String?) {
        if (!url.isNullOrEmpty()) {
            Picasso.get()
                .load(url)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .into(binding.ivProfileImage)
        } else {
            binding.ivProfileImage.setImageResource(R.drawable.ic_person)
        }
    }

    private fun observeMyBooks() {
        val booksLiveData = userViewModel.currentUser.switchMap { user ->
            val id = user?.id
            if (id == null) {
                MutableLiveData<List<Book>>(emptyList())
            } else {
                bookViewModel.getBooksByOwner(id)
            }
        }
        booksLiveData.observe(viewLifecycleOwner) { books ->
            val list = books ?: emptyList()
            myBooksAdapter.submitList(list)
            binding.tvEmptyBooks.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            binding.rvMyBooks.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun observeBookOperations() {
        bookViewModel.operationState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is BookOperationState.Loading -> binding.progressBar.visibility = View.VISIBLE
                is BookOperationState.Success -> {
                    binding.progressBar.visibility = View.GONE
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

    private fun setupButtons() {
        binding.btnEditProfile.setOnClickListener {
            navigateToEditProfile()
        }

        binding.btnListBook.setOnClickListener {
            navigateToAddBook()
        }

        binding.btnLogout.setOnClickListener {
            authViewModel.logout()
            val mainNavController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
            mainNavController.navigate(R.id.action_main_to_login)
        }
    }

    private fun navigateToEditProfile() {
        val mainNavController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        mainNavController.navigate(R.id.action_main_to_editProfile)
    }

    private fun navigateToAddBook() {
        val mainNavController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        mainNavController.navigate(R.id.action_main_to_addEditBook)
    }

    private fun navigateToBookMessages(bookId: String) {
        val mainNavController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        mainNavController.navigate(R.id.bookChatListFragment, bundleOf("bookId" to bookId))
    }

    private fun navigateToEditBook(bookId: String) {
        val mainNavController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        val bundle = Bundle().apply { putString("bookId", bookId) }
        mainNavController.navigate(R.id.action_main_to_addEditBook, bundle)
    }

    private fun showDeleteConfirmation(bookId: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_title)
            .setMessage(R.string.dialog_delete_message)
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.btn_delete) { _, _ ->
                bookViewModel.deleteBook(bookId)
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
