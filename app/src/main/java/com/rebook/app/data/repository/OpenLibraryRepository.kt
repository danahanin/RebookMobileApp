package com.rebook.app.data.repository

import com.rebook.app.data.api.OpenLibraryBook
import com.rebook.app.data.api.OpenLibraryService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OpenLibraryRepository {

    private val api = OpenLibraryService.api

    suspend fun searchBooks(query: String): Result<List<OpenLibraryBook>> = withContext(Dispatchers.IO) {
        try {
            val response = api.searchBooks(query)
            Result.success(response.docs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchByTitle(title: String): Result<List<OpenLibraryBook>> = withContext(Dispatchers.IO) {
        try {
            val response = api.searchByTitle(title)
            Result.success(response.docs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchByIsbn(isbn: String): Result<List<OpenLibraryBook>> = withContext(Dispatchers.IO) {
        try {
            val response = api.searchByIsbn(isbn)
            Result.success(response.docs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
