package com.ironfleet.urlshortener.data.remote

import com.ironfleet.urlshortener.data.remote.dto.ShortenRequest
import com.ironfleet.urlshortener.data.remote.dto.ShortenResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit service interface for the URL Shortener Flask API.
 *
 * Base URL is injected at runtime from DataStore (Settings screen),
 * defaulting to https://url-shortener-1-m70j.onrender.com/
 *
 * Redirect short URLs are opened in Chrome Custom Tabs directly —
 * no Retrofit call needed for the /<short_code> route.
 */
interface UrlShortenerApi {

    /**
     * Shorten a long URL.
     * POST /shorten
     * Body: {"url": "https://..."}
     * Returns 201 on success, 400 on missing field, 429 on rate limit.
     */
    @POST("shorten")
    suspend fun shorten(@Body body: ShortenRequest): Response<ShortenResponse>
}
