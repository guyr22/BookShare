package com.example.bookstore.network

import com.example.bookstore.network.dto.GoogleBooksResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface GoogleBooksApiService {

    @GET("volumes")
    suspend fun searchByTitle(
        @Query("q") title: String,
        @Query("maxResults") maxResults: Int = 20
    ): GoogleBooksResponse

    @GET("volumes")
    suspend fun searchByIsbn(
        @Query("q") isbn: String,
        @Query("maxResults") maxResults: Int = 1
    ): GoogleBooksResponse
}
