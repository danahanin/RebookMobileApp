package com.rebook.app.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.rebook.app.data.model.Book
import com.rebook.app.data.model.BookStatus
import com.rebook.app.data.repository.BookRepository
import com.rebook.app.util.BookOperationState
import kotlinx.coroutines.launch

class BookViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BookRepository(application)

    private val _allBooks: LiveData<List<Book>> = repository.getAllBooks().asLiveData()

    private val _searchQuery = MutableLiveData("")

    /** null = show all, BookStatus.AVAILABLE = show available only */
    private val _statusFilter = MutableLiveData<BookStatus?>(null)
    val statusFilter: LiveData<BookStatus?> = _statusFilter

    val filteredBooks: LiveData<List<Book>> = MediatorLiveData<List<Book>>().apply {
        var books: List<Book> = emptyList()
        var query = ""
        var filter: BookStatus? = null

        addSource(_allBooks) {
            books = it ?: emptyList()
            value = applyFilter(books, query, filter)
        }
        addSource(_searchQuery) {
            query = it ?: ""
            value = applyFilter(books, query, filter)
        }
        addSource(_statusFilter) {
            filter = it
            value = applyFilter(books, query, filter)
        }
    }

    private val _operationState =
        MutableLiveData<BookOperationState>(BookOperationState.Idle)
    val operationState: LiveData<BookOperationState> = _operationState

    private fun applyFilter(books: List<Book>, query: String, statusFilter: BookStatus?): List<Book> {
        var result = books
        if (statusFilter != null) {
            result = result.filter { it.status == statusFilter }
        }
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            result = result.filter {
                it.title.lowercase().contains(q) || it.author.lowercase().contains(q)
            }
        }
        return result
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStatusFilter(status: BookStatus?) {
        _statusFilter.value = status
    }

    fun getBooksByOwner(ownerId: String): LiveData<List<Book>> =
        repository.getBooksByOwner(ownerId).asLiveData()

    suspend fun getBookById(bookId: String): Book? =
        repository.getBookById(bookId)

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

    fun unrequestBook(bookId: String) {
        _operationState.value = BookOperationState.Loading
        viewModelScope.launch {
            val result = repository.unrequestBook(bookId)
            _operationState.value = if (result.isSuccess) {
                BookOperationState.Success
            } else {
                BookOperationState.Error(result.exceptionOrNull()?.message ?: "Failed to cancel request")
            }
        }
    }

    fun resetOperationState() {
        _operationState.value = BookOperationState.Idle
    }

    private val _selectedBook = MutableLiveData<Book?>()
    val selectedBook: LiveData<Book?> = _selectedBook

    fun loadBook(bookId: String) {
        viewModelScope.launch {
            _selectedBook.value = repository.getBookById(bookId)
        }
    }

    fun clearSelectedBook() {
        _selectedBook.value = null
    }

    private val _uploadedImageUrl = MutableLiveData<String?>()
    val uploadedImageUrl: LiveData<String?> = _uploadedImageUrl

    fun uploadBookImage(imageUri: Uri) {
        _operationState.value = BookOperationState.Loading
        viewModelScope.launch {
            val result = repository.uploadBookImage(imageUri)
            if (result.isSuccess) {
                _uploadedImageUrl.value = result.getOrNull()
                _operationState.value = BookOperationState.Idle
            } else {
                _operationState.value = BookOperationState.Error(
                    result.exceptionOrNull()?.message ?: "Upload failed"
                )
            }
        }
    }

    fun clearUploadedImageUrl() {
        _uploadedImageUrl.value = null
    }
}
