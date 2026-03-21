package com.rebook.app.ui.book

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.rebook.app.databinding.FragmentBookMessageBinding
import com.rebook.app.viewmodel.BookViewModel
import com.rebook.app.viewmodel.MessageSendState
import com.rebook.app.viewmodel.MessageViewModel

class BookMessageFragment : Fragment() {

    private var _binding: FragmentBookMessageBinding? = null
    private val binding get() = _binding!!

    private val args: BookMessageFragmentArgs by navArgs()
    private val bookViewModel: BookViewModel by activityViewModels()
    private val messageViewModel: MessageViewModel by viewModels()

    private lateinit var messageAdapter: MessageListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookMessageBinding.inflate(inflater, container, false)
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

        bookViewModel.loadBook(bookId)
        bookViewModel.selectedBook.observe(viewLifecycleOwner) { book ->
            if (book != null) {
                val owner = book.ownerName.ifBlank { "Unknown" }
                binding.toolbar.subtitle = "${book.title} · $owner"
            } else {
                binding.toolbar.subtitle = null
            }
        }

        messageAdapter = MessageListAdapter(FirebaseAuth.getInstance().currentUser?.uid)
        binding.recyclerMessages.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.recyclerMessages.adapter = messageAdapter

        messageViewModel.startListening(bookId)

        messageViewModel.messages.observe(viewLifecycleOwner) { list ->
            messageAdapter.submitList(list) {
                if (list.isNotEmpty()) {
                    binding.recyclerMessages.scrollToPosition(list.size - 1)
                }
            }
            updateEmptyAndLoadingUi()
        }

        messageViewModel.listLoading.observe(viewLifecycleOwner) {
            updateEmptyAndLoadingUi()
        }

        messageViewModel.listenError.observe(viewLifecycleOwner) { err ->
            if (err != null) {
                Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show()
            }
            updateEmptyAndLoadingUi()
        }

        messageViewModel.sendState.observe(viewLifecycleOwner) { state ->
            when (state) {
                MessageSendState.Idle -> {
                    binding.progressSend.isVisible = false
                    setComposeEnabled(true)
                }
                MessageSendState.Loading -> {
                    binding.progressSend.isVisible = true
                    setComposeEnabled(false)
                }
                MessageSendState.Success -> {
                    binding.progressSend.isVisible = false
                    binding.etMessage.text?.clear()
                    setComposeEnabled(true)
                    messageViewModel.resetSendState()
                }
                is MessageSendState.Error -> {
                    binding.progressSend.isVisible = false
                    setComposeEnabled(true)
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    messageViewModel.resetSendState()
                }
            }
        }

        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text?.toString().orEmpty()
            messageViewModel.sendMessage(bookId, text)
        }
    }

    private fun setComposeEnabled(enabled: Boolean) {
        binding.etMessage.isEnabled = enabled
        binding.btnSend.isEnabled = enabled
        binding.inputLayoutMessage.isEnabled = enabled
    }

    private fun updateEmptyAndLoadingUi() {
        val list = messageViewModel.messages.value.orEmpty()
        val loading = messageViewModel.listLoading.value == true
        val error = messageViewModel.listenError.value
        binding.progressList.isVisible = loading
        binding.tvEmpty.isVisible = list.isEmpty() && !loading && error == null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
