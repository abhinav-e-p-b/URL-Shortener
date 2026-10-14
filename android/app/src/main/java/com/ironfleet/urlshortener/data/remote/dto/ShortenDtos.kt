package com.ironfleet.urlshortener.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Request body sent to POST /shorten
 * Maps to: {"url": "https://example.com/very/long/path"}
 */
data class ShortenRequest(
    @SerializedName("url")
    val url: String
)

/**
 * Response body received from POST /shorten
 * Maps to: {"short_url": "https://..../abc", "short_code": "abc"}
 */
data class ShortenResponse(
    @SerializedName("short_url")
    val shortUrl: String,

    @SerializedName("short_code")
    val shortCode: String
)

/**
 * Error response body from the API (e.g. 400 / 429 / 404)
 * Maps to: {"error": "Rate limit exceeded. Try again later."}
 */
data class ApiErrorResponse(
    @SerializedName("error")
    val error: String
)
