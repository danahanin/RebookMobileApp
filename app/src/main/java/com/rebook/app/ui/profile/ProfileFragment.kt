package com.rebook.app.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rebook.app.R
import com.rebook.app.data.model.Book
import com.rebook.app.data.model.User
import com.rebook.app.databinding.FragmentProfileBinding
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
        setupAdapter()
        setupClickListeners()
        observeUser()
        observeMyBooks()
    }

    private fun setupAdapter() {
        myBooksAdapter = MyBooksAdapter(
            onEditClick = ::onEditBookClick,
            onDeleteClick = ::onDeleteBookClick
        )
        binding.rvMyBooks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = myBooksAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnLogout.setOnClickListener {
            authViewModel.logout()
            val mainNavController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
            mainNavController.navigate(R.id.action_main_to_login)
        }

        binding.btnEditProfile.setOnClickListener {
            val mainNavController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
            mainNavController.navigate(R.id.action_main_to_editProfile)
        }

        binding.btnListBook.setOnClickListener {
            val mainNavController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
            mainNavController.navigate(R.id.action_main_to_addEditBook)
        }
    }

    private fun observeUser() {
        userViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            user?.let { bindUserInfo(it) }
        }
    }

    private fun bindUserInfo(user: User) {
        binding.tvUserName.text = user.displayName
        binding.tvUserEmail.text = user.email

        if (!user.profileImageUrl.isNullOrEmpty()) {
            Picasso.get()
                .load(user.profileImageUrl)
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .into(binding.ivProfileImage)
        } else {
            binding.ivProfileImage.setImageResource(R.drawable.ic_profile_placeholder)
        }
    }

    private fun observeMyBooks() {
        userViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            user?.let {
                bookViewModel.getBooksByOwner(it.id).observe(viewLifecycleOwner) { books ->
                    updateBooksUI(books)
                }
            }
        }
    }

    private fun updateBooksUI(books: List<Book>) {
        myBooksAdapter.submitList(books)
        if (books.isEmpty()) {
            binding.rvMyBooks.visibility = View.GONE
            binding.tvEmptyBooks.visibility = View.VISIBLE
        } else {
            binding.rvMyBooks.visibility = View.VISIBLE
            binding.tvEmptyBooks.visibility = View.GONE
        }
    }

    private fun onEditBookClick(book: Book) {
        val mainNavController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        mainNavController.navigate(
            R.id.action_main_to_addEditBook,
            bundleOf("bookId" to book.id)
        )
    }

    private fun onDeleteBookClick(book: Book) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_title)
            .setMessage(R.string.dialog_delete_message)
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.btn_delete) { _, _ ->
                bookViewModel.deleteBook(book.id)
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
