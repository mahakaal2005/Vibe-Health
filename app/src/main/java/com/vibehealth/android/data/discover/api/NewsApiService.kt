package com.vibehealth.android.data.discover.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * News API service interface
 * Base URL: https://newsapi.org/v2/
 */
interface NewsApiService {
    
    /**
     * Search for health-related news articles
     * Endpoint: /everything
     */
    @GET("everything")
    suspend fun searchHealthNews(
        @Query("q") query: String = "health OR wellness OR fitness OR nutrition OR mental health",
        @Query("language") language: String = "en",
        @Query("sortBy") sortBy: String = "publishedAt",
        @Query("pageSize") pageSize: Int = 50, // Increased from 20 to get more content
        @Query("page") page: Int = 1,
        @Query("apiKey") apiKey: String
    ): Response<NewsApiResponse>
    
    /**
     * Get top health headlines
     * Endpoint: /top-headlines
     */
    @GET("top-headlines")
    suspend fun getHealthHeadlines(
        @Query("category") category: String = "health",
        @Query("language") language: String = "en",
        @Query("pageSize") pageSize: Int = 50, // Increased from 20 to get more content
        @Query("page") page: Int = 1,
        @Query("apiKey") apiKey: String
    ): Response<NewsApiResponse>
}