package com.rebook.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.rebook.app.data.model.Book
import com.rebook.app.data.repository.BookRepository
import com.rebook.app.util.BookOperationState
import kotlinx.coroutines.launch

class BookViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BookRepository(application)

    private val _allBooks: LiveData<List<Book>> = repository.getAllBooks().asLiveData()

    private val _searchQuery = MutableLiveData("")

    val filteredBooks: LiveData<List<Book>> = MediatorLiveData<List<Book>>().apply {
        var books: List<Book> = emptyList()
        var query = ""

        addSource(_allBooks) {
            books = it ?: emptyList()
            value = applyFilter(books, query)
        }
        addSource(_searchQuery) {
            query = it ?: ""
            value = applyFilter(books, query)
        }
    }

    private val _operationState =
        MutableLiveData<BookOperationState>(BookOperationState.Idle)
    val operationState: LiveData<BookOperationState> = _operationState

    private fun applyFilter(books: List<Book>, query: String): List<Book> {
        if (query.isBlank()) return books
        val q = query.trim().lowercase()
        return books.filter {
            it.title.lowercase().contains(q) || it.author.lowercase().contains(q)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun getBooksByOwner(ownerId: String): LiveData<List<Book>> =
        repository.getBooksByOwner(ownerId).asLiveData()

    fun syncBooks() {
        viewModelScope.launch {
            repository.syncBooksFromFirestore()
        }
    }

    fun addBook(book: Book) {
        _operationState.value = BookOperationState.Loading
        viewModelScope.launch {
            val result = repository.addBook(book)
            _operationState.value = if (result.isSuccess) {
                BookOperationState.Success
            } else {
                BookOperationState.Error(result.exceptionOrNull()?.message ?: "Failed to add book")
            }
        }
    }

    fun updateBook(book: Book) {
        _operationState.value = BookOperationState.Loading
        viewModelScope.launch {
            val result = repository.updateBook(book)
            _operationState.value = if (result.isSuccess) {
                BookOperationState.Success
            } else {
                BookOperationState.Error(result.exceptionOrNull()?.message ?: "Failed to update book")
            }
        }
    }

    fun deleteBook(bookId: String) {
        _operationState.value = BookOperationState.Loading
        viewModelScope.launch {
            val result = repository.deleteBook(bookId)
            _operationState.value = if (result.isSuccess) {
                BookOperationState.Success
            } else {
                BookOperationState.Error(result.exceptionOrNull()?.message ?: "Failed to delete book")
            }
        }
    }

    fun requestBook(bookId: String) {
        _operationState.value = BookOperationState.Loading
        viewModelScope.launch {
            val result = repository.requestBook(bookId)
            _operationState.value = if (result.isSuccess) {
                BookOperationState.Success
            } else {
                BookOperationState.Error(result.exceptionOrNull()?.message ?: "Failed to request book")
            }
        }
    }

    fun resetOperationState() {
        _operationState.value = BookOperationState.Idle
    }
}
