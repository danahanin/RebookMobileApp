package com.rebook.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.rebook.app.R
import com.rebook.app.data.model.BookMessage
import com.rebook.app.data.repository.MessageRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

sealed class MessageSendState {
    data object Idle : MessageSendState()
    data object Loading : MessageSendState()
    data object Success : MessageSendState()
    data class Error(val message: String) : MessageSendState()
}

class MessageViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MessageRepository()

    private val _messages = MutableLiveData<List<BookMessage>>(emptyList())
    val messages: LiveData<List<BookMessage>> = _messages

    private val _listLoading = MutableLiveData(true)
    val listLoading: LiveData<Boolean> = _listLoading

    private val _listenError = MutableLiveData<String?>(null)
    val listenError: LiveData<String?> = _listenError

    private val _sendState = MutableLiveData<MessageSendState>(MessageSendState.Idle)
    val sendState: LiveData<MessageSendState> = _sendState

    private var listenJob: Job? = null

    fun startListening(bookId: String) {
        if (bookId.isBlank()) return
        listenJob?.cancel()
        _listLoading.postValue(true)
        _listenError.postValue(null)
        listenJob = viewModelScope.launch {
            try {
                repository.observeMessages(bookId).collect { list ->
                    _listLoading.postValue(false)
                    _messages.postValue(list)
                }
            } catch (e: Exception) {
                _listLoading.postValue(false)
                _listenError.postValue(
                    e.message ?: getApplication<Application>().getString(R.string.error_messages_listen)
                )
            }
        }
    }

    fun sendMessage(bookId: String, text: String) {
        viewModelScope.launch {
            _sendState.value = MessageSendState.Loading
            val result = repository.sendMessage(bookId, text)
            _sendState.value = if (result.isSuccess) {
                MessageSendState.Success
            } else {
                MessageSendState.Error(
                    result.exceptionOrNull()?.message
                        ?: getApplication<Application>().getString(R.string.error_generic)
                )
            }
        }
    }

    fun resetSendState() {
        _sendState.value = MessageSendState.Idle
    }

    override fun onCleared() {
        listenJob?.cancel()
        listenJob = null
        super.onCleared()
    }
}
