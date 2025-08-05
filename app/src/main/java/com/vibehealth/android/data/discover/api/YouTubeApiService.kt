package com.vibehealth.android.data.discover.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * YouTube Data API v3 service interface
 * Base URL: https://www.googleapis.com/youtube/v3/
 */
interface YouTubeApiService {
    
    /**
     * Search for health and wellness videos
     * Endpoint: /search
     */
    @GET("search")
    suspend fun searchHealthVideos(
        @Query("part") part: String = "snippet",
        @Query("q") query: String = "health wellness fitness yoga meditation nutrition",
        @Query("type") type: String = "video",
        @Query("order") order: String = "relevance",
        @Query("maxResults") maxResults: Int = 20,
        @Query("safeSearch") safeSearch: String = "strict",
        @Query("videoDefinition") videoDefinition: String = "any",
        @Query("videoDuration") videoDuration: String = "medium", // 4-20 minutes
        @Query("key") apiKey: String
    ): Response<YouTubeSearchResponse>
    
    /**
     * Get video details including duration
     * Endpoint: /videos
     */
    @GET("videos")
    suspend fun getVideoDetails(
        @Query("part") part: String = "contentDetails",
        @Query("id") videoIds: String, // Comma-separated video IDs
        @Query("key") apiKey: String
    ): Response<YouTubeVideoDetailsResponse>
}