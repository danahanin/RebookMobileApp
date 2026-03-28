package com.rebook.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.rebook.app.R
import com.rebook.app.data.model.ChatThreadRow
import com.rebook.app.data.repository.MessageRepository
import com.rebook.app.data.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ChatListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MessageRepository()
    private val userRepository = UserRepository(application)

    private val _threadRows = MutableLiveData<List<ChatThreadRow>>(emptyList())
    val threadRows: LiveData<List<ChatThreadRow>> = _threadRows

    private val _loading = MutableLiveData(true)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private var listenJob: Job? = null

    fun startListening(bookId: String) {
        if (bookId.isBlank()) return
        listenJob?.cancel()
        _loading.postValue(true)
        _error.postValue(null)
        listenJob = viewModelScope.launch {
            try {
                repository.observeChatThreads(bookId).collect { summaries ->
                    _loading.postValue(false)
                    val rows = summaries.map { s ->
                        val name = s.buyerDisplayName?.takeIf { it.isNotEmpty() }
                            ?: userRepository.getDisplayNameForUser(s.buyerId)
                        val label = name ?: "Reader · ${s.buyerId.take(8)}"
                        ChatThreadRow(buyerId = s.buyerId, displayLabel = label)
                    }
                    _threadRows.postValue(rows)
                }
            } catch (e: Exception) {
                _loading.postValue(false)
                _error.postValue(
                    e.message ?: getApplication<Application>().getString(R.string.error_messages_listen)
                )
            }
        }
    }

    override fun onCleared() {
        listenJob?.cancel()
        listenJob = null
        super.onCleared()
    }
}
