package com.vibehealth.android.data.discover.api

import com.google.gson.annotations.SerializedName

/**
 * Data models for YouTube Data API v3 responses
 * API Documentation: https://developers.google.com/youtube/v3/docs/search/list
 */

data class YouTubeSearchResponse(
    @SerializedName("kind") val kind: String,
    @SerializedName("etag") val etag: String,
    @SerializedName("nextPageToken") val nextPageToken: String?,
    @SerializedName("regionCode") val regionCode: String?,
    @SerializedName("pageInfo") val pageInfo: YouTubePageInfo,
    @SerializedName("items") val items: List<YouTubeSearchItem>
)

data class YouTubePageInfo(
    @SerializedName("totalResults") val totalResults: Int,
    @SerializedName("resultsPerPage") val resultsPerPage: Int
)

data class YouTubeSearchItem(
    @SerializedName("kind") val kind: String,
    @SerializedName("etag") val etag: String,
    @SerializedName("id") val id: YouTubeVideoId,
    @SerializedName("snippet") val snippet: YouTubeSnippet
)

data class YouTubeVideoId(
    @SerializedName("kind") val kind: String,
    @SerializedName("videoId") val videoId: String
)

data class YouTubeSnippet(
    @SerializedName("publishedAt") val publishedAt: String,
    @SerializedName("channelId") val channelId: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("thumbnails") val thumbnails: YouTubeThumbnails,
    @SerializedName("channelTitle") val channelTitle: String,
    @SerializedName("liveBroadcastContent") val liveBroadcastContent: String,
    @SerializedName("publishTime") val publishTime: String
)

data class YouTubeThumbnails(
    @SerializedName("default") val default: YouTubeThumbnail?,
    @SerializedName("medium") val medium: YouTubeThumbnail?,
    @SerializedName("high") val high: YouTubeThumbnail?,
    @SerializedName("standard") val standard: YouTubeThumbnail?,
    @SerializedName("maxres") val maxres: YouTubeThumbnail?
)

data class YouTubeThumbnail(
    @SerializedName("url") val url: String,
    @SerializedName("width") val width: Int,
    @SerializedName("height") val height: Int
)

// For getting video details (duration, etc.)
data class YouTubeVideoDetailsResponse(
    @SerializedName("kind") val kind: String,
    @SerializedName("etag") val etag: String,
    @SerializedName("items") val items: List<YouTubeVideoDetails>
)

data class YouTubeVideoDetails(
    @SerializedName("kind") val kind: String,
    @SerializedName("etag") val etag: String,
    @SerializedName("id") val id: String,
    @SerializedName("contentDetails") val contentDetails: YouTubeContentDetails
)

data class YouTubeContentDetails(
    @SerializedName("duration") val duration: String, // ISO 8601 format (PT4M13S)
    @SerializedName("dimension") val dimension: String,
    @SerializedName("definition") val definition: String,
    @SerializedName("caption") val caption: String,
    @SerializedName("licensedContent") val licensedContent: Boolean,
    @SerializedName("projection") val projection: String
)