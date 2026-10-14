package com.ironfleet.urlshortener.data.remote

import com.ironfleet.urlshortener.data.remote.dto.ShortenRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Retrofit service interface for the URL Shortener Flask API.
 *
 * Base URL is injected at runtime from DataStore (Settings screen),
 * defaulting to https://url-shortener-1-m70j.onrender.com/
 *
 * We use ResponseBody (raw) instead of a typed response so that:
 *  - Gson never auto-parses the body and throws MalformedJsonException
 *    when Render returns an empty 200 during a cold-start.
 *  - The repository can inspect and manually parse the body safely.
 */
interface UrlShortenerApi {

    /**
     * Shorten a long URL.
     * POST /shorten
     * Body: {"url": "https://..."}
     * Returns 201 on success, 400 on missing field, 429 on rate limit.
     */
    @POST("shorten")
    suspend fun shorten(@Body body: ShortenRequest): Response<ResponseBody>

    /**
     * Health check — used to wake up a sleeping Render instance before
     * making the actual /shorten request.
     * GET /health
     * Returns 200 {"status": "healthy"} when both Redis and Postgres are up.
     */
    @GET("health")
    suspend fun health(): Response<ResponseBody>
}
