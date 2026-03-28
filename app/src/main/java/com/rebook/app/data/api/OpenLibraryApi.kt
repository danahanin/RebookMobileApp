package com.rebook.app.data.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OpenLibraryApi {

    @GET("search.json")
    suspend fun searchBooks(
        @Query("q") query: String,
        @Query("limit") limit: Int = 20,
        @Query("page") page: Int = 1
    ): OpenLibrarySearchResponse

    @GET("search.json")
    suspend fun searchByTitle(
        @Query("title") title: String,
        @Query("limit") limit: Int = 20
    ): OpenLibrarySearchResponse

    @GET("search.json")
    suspend fun searchByAuthor(
        @Query("author") author: String,
        @Query("limit") limit: Int = 20
    ): OpenLibrarySearchResponse

    @GET("search.json")
    suspend fun searchByIsbn(
        @Query("isbn") isbn: String,
        @Query("limit") limit: Int = 10
    ): OpenLibrarySearchResponse

    @GET("works/{workId}.json")
    suspend fun getWorkDetails(
        @Path("workId") workId: String
    ): OpenLibraryWorkDetails
}
