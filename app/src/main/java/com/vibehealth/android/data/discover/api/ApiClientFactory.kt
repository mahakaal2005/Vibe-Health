package com.vibehealth.android.data.discover.api

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Factory for creating API service clients
 */
object ApiClientFactory {
    
    private const val NEWS_API_BASE_URL = "https://newsapi.org/v2/"
    private const val YOUTUBE_API_BASE_URL = "https://www.googleapis.com/youtube/v3/"
    
    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        Log.d("API_HTTP", message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BASIC // Only log request/response lines
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val newsApiRetrofit = Retrofit.Builder()
        .baseUrl(NEWS_API_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    private val youtubeApiRetrofit = Retrofit.Builder()
        .baseUrl(YOUTUBE_API_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    fun createNewsApiService(): NewsApiService {
        return newsApiRetrofit.create(NewsApiService::class.java)
    }
    
    fun createYouTubeApiService(): YouTubeApiService {
        return youtubeApiRetrofit.create(YouTubeApiService::class.java)
    }
}