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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.rebook.app.data.repository.UserRepository
import com.rebook.app.databinding.FragmentBookMessageBinding
import com.rebook.app.viewmodel.BookViewModel
import com.rebook.app.viewmodel.MessageSendState
import com.rebook.app.viewmodel.MessageViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class BookMessageFragment : Fragment() {

    private var _binding: FragmentBookMessageBinding? = null
    private val binding get() = _binding!!

    private val args: BookMessageFragmentArgs by navArgs()
    private val bookViewModel: BookViewModel by activityViewModels()
    private val messageViewModel: MessageViewModel by viewModels()

    private val userRepository by lazy { UserRepository(requireContext()) }
    private var subtitleResolveJob: Job? = null

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
        val chatBuyerUid = args.chatBuyerUid
        if (bookId.isBlank() || chatBuyerUid.isBlank()) {
            findNavController().popBackStack()
            return
        }

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        bookViewModel.loadBook(bookId)
        bookViewModel.selectedBook.observe(viewLifecycleOwner) { book ->
            subtitleResolveJob?.cancel()
            if (book == null) {
                binding.toolbar.subtitle = null
                return@observe
            }
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            when {
                uid == null ->
                    binding.toolbar.subtitle =
                        "${book.title} · ${book.ownerName.ifBlank { "Unknown" }}"
                book.ownerId == uid -> {
                    val preset = args.partnerDisplayName?.takeIf { it.isNotBlank() }
                    if (preset != null) {
                        binding.toolbar.subtitle = "${book.title} · $preset"
                    } else {
                        binding.toolbar.subtitle = "${book.title} · …"
                        subtitleResolveJob = viewLifecycleOwner.lifecycleScope.launch {
                            val name = userRepository.getDisplayNameForUser(chatBuyerUid)
                            val label = name ?: "Reader · ${chatBuyerUid.take(8)}"
                            if (viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                                binding.toolbar.subtitle = "${book.title} · $label"
                            }
                        }
                    }
                }
                else ->
                    binding.toolbar.subtitle =
                        "${book.title} · ${book.ownerName.ifBlank { "Owner" }}"
            }
        }

        messageAdapter = MessageListAdapter(FirebaseAuth.getInstance().currentUser?.uid)
        binding.recyclerMessages.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.recyclerMessages.adapter = messageAdapter

        messageViewModel.startListening(bookId, chatBuyerUid)

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
            messageViewModel.sendMessage(bookId, chatBuyerUid, text)
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
