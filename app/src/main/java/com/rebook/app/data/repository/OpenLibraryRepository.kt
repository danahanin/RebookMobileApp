package com.rebook.app.data.repository

import com.rebook.app.data.api.OpenLibraryBook
import com.rebook.app.data.api.OpenLibraryService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OpenLibraryRepository {

    private val api = OpenLibraryService.api

    suspend fun searchBooks(query: String): Result<List<OpenLibraryBook>> =
        ioResult { api.searchBooks(query).docs }

    suspend fun searchByTitle(title: String): Result<List<OpenLibraryBook>> =
        ioResult { api.searchByTitle(title).docs }

    suspend fun searchByIsbn(isbn: String): Result<List<OpenLibraryBook>> =
        ioResult { api.searchByIsbn(isbn).docs }

    suspend fun getWorkDescription(workKey: String): Result<String?> = ioResult {
        val workId = workKey.removePrefix("/works/")
        api.getWorkDetails(workId).getDescriptionText()
    }

    private suspend fun <T> ioResult(block: suspend () -> T): Result<T> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(block())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
