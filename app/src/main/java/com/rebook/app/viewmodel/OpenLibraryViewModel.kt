package com.rebook.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rebook.app.data.api.OpenLibraryBook
import com.rebook.app.data.repository.OpenLibraryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OpenLibraryViewModel : ViewModel() {

    private val repository = OpenLibraryRepository()

    private val _searchResults = MutableLiveData<List<OpenLibraryBook>>(emptyList())
    val searchResults: LiveData<List<OpenLibraryBook>> = _searchResults

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _selectedBook = MutableLiveData<OpenLibraryBook?>(null)
    val selectedBook: LiveData<OpenLibraryBook?> = _selectedBook

    private val _fetchedDescription = MutableLiveData<String?>(null)
    val fetchedDescription: LiveData<String?> = _fetchedDescription

    private val _isFetchingDetails = MutableLiveData(false)
    val isFetchingDetails: LiveData<Boolean> = _isFetchingDetails

    private var searchJob: Job? = null

    fun searchBooks(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            _isLoading.value = true
            _error.value = null

            val result = repository.searchBooks(query)
            result.fold(
                onSuccess = { books ->
                    _searchResults.value = books
                },
                onFailure = { e ->
                    _error.value = e.message ?: "Search failed"
                    _searchResults.value = emptyList()
                }
            )
            _isLoading.value = false
        }
    }

    fun selectBook(book: OpenLibraryBook) {
        _selectedBook.value = book
    }

    fun clearSelection() {
        _selectedBook.value = null
    }

    fun clearResults() {
        _searchResults.value = emptyList()
        _error.value = null
    }

    fun clearError() {
        _error.value = null
    }

    fun fetchBookDescription(workKey: String, onComplete: (String?) -> Unit) {
        viewModelScope.launch {
            _isFetchingDetails.value = true
            val result = repository.getWorkDescription(workKey)
            val description = result.getOrNull()
            _fetchedDescription.value = description
            _isFetchingDetails.value = false
            onComplete(description)
        }
    }

    fun clearFetchedDescription() {
        _fetchedDescription.value = null
    }
}
