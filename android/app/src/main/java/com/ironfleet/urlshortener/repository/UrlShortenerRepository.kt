package com.ironfleet.urlshortener.repository

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.ironfleet.urlshortener.data.local.PreferencesRepository
import com.ironfleet.urlshortener.data.local.UrlHistoryDao
import com.ironfleet.urlshortener.data.local.UrlHistoryEntity
import com.ironfleet.urlshortener.data.remote.UrlShortenerApi
import com.ironfleet.urlshortener.data.remote.dto.ApiErrorResponse
import com.ironfleet.urlshortener.data.remote.dto.ShortenRequest
import com.ironfleet.urlshortener.data.remote.dto.ShortenResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sealed result type — wraps success and typed failure states
 * so the ViewModel never has to inspect raw HTTP responses.
 */
sealed class ShortenResult {
    data class Success(val shortUrl: String, val shortCode: String) : ShortenResult()
    data object RateLimited : ShortenResult()
    data class Error(val message: String) : ShortenResult()
}

/**
 * Repository — single source of truth for the app.
 * Coordinates:
 *  - Retrofit API calls (remote)
 *  - Room inserts/reads (local history)
 *  - DataStore base URL changes (rebuilds Retrofit on-the-fly)
 *
 * Why raw ResponseBody instead of typed Gson conversion?
 * Render's free tier returns HTTP 200 with an EMPTY body while the
 * service is cold-starting. If GsonConverterFactory tries to auto-parse
 * that empty body as ShortenResponse it throws:
 *   "MalformedJsonException: End of input at line 1 column 1 path $"
 * By receiving ResponseBody we can safely read the string first,
 * check if it's non-empty, and only then parse it with Gson.
 */
@Singleton
class UrlShortenerRepository @Inject constructor(
    private val dao: UrlHistoryDao,
    private val preferencesRepository: PreferencesRepository,
    private val gson: Gson,
    private val okHttpClient: OkHttpClient
) {
    /**
     * Lazily created API instance. Rebuilt when the base URL changes in Settings.
     */
    private var currentBaseUrl: String = ""
    private var api: UrlShortenerApi? = null

    private suspend fun getApi(): UrlShortenerApi {
        val baseUrl = preferencesRepository.baseUrlFlow.first()
        if (api == null || baseUrl != currentBaseUrl) {
            currentBaseUrl = baseUrl
            api = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                // ScalarsConverterFactory not needed since we use ResponseBody directly,
                // but keep it to satisfy Retrofit's converter chain for @Body serialization.
                .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create(gson))
                .build()
                .create(UrlShortenerApi::class.java)
        }
        return api!!
    }

    /**
     * Shorten a URL via the Flask API, then persist the result to Room.
     *
     * Strategy for Render cold-starts:
     *  1. POST /shorten
     *  2. If the response is 2xx but the body is EMPTY, Render is still waking up.
     *     Ping /health until it returns "healthy", then retry /shorten (up to 3 times).
     *
     * @param longUrl the original long URL
     * @return [ShortenResult] — Success, RateLimited, or Error
     */
    suspend fun shorten(longUrl: String): ShortenResult {
        return try {
            shortenWithRetry(longUrl, attemptsLeft = 3)
        } catch (e: java.net.UnknownHostException) {
            ShortenResult.Error("NO_NETWORK")
        } catch (e: java.net.SocketTimeoutException) {
            ShortenResult.Error("Request timed out. Is the server running?")
        } catch (e: Exception) {
            ShortenResult.Error("Unexpected error: ${e.message}")
        }
    }

    private suspend fun shortenWithRetry(longUrl: String, attemptsLeft: Int): ShortenResult {
        val response = getApi().shorten(ShortenRequest(longUrl))
        val rawBody = response.body()?.string()?.trim()

        return when {
            // ── Success path ──────────────────────────────────────────────────
            response.isSuccessful && !rawBody.isNullOrEmpty() -> {
                val parsed = tryParseSuccess(rawBody)
                    ?: return ShortenResult.Error("Unexpected response format from server")

                // Persist to local history
                dao.insert(
                    UrlHistoryEntity(
                        longUrl = longUrl,
                        shortCode = parsed.shortCode,
                        shortUrl = parsed.shortUrl
                    )
                )
                ShortenResult.Success(parsed.shortUrl, parsed.shortCode)
            }

            // ── Empty body on 2xx → Render cold-start ────────────────────────
            // The service is waking up. Wait for /health to return healthy,
            // then retry the shorten call.
            response.isSuccessful && rawBody.isNullOrEmpty() -> {
                if (attemptsLeft <= 0) {
                    return ShortenResult.Error("Server is still waking up. Please try again in a moment.")
                }
                waitForHealth(maxWaitMs = 30_000)
                delay(1_000) // brief pause before retry
                shortenWithRetry(longUrl, attemptsLeft - 1)
            }

            // ── Rate limited ──────────────────────────────────────────────────
            response.code() == 429 -> ShortenResult.RateLimited

            // ── Other HTTP errors ─────────────────────────────────────────────
            else -> {
                val errorRaw = response.errorBody()?.string()
                val message = if (!errorRaw.isNullOrBlank()) {
                    try {
                        gson.fromJson(errorRaw, ApiErrorResponse::class.java).error
                    } catch (e: JsonSyntaxException) {
                        "HTTP ${response.code()}"
                    }
                } else {
                    "HTTP ${response.code()}"
                }
                ShortenResult.Error(message)
            }
        }
    }

    /**
     * Poll GET /health until the server responds with a non-empty body
     * (meaning Flask + Redis + Postgres are all up), or until [maxWaitMs] elapses.
     */
    private suspend fun waitForHealth(maxWaitMs: Long) {
        val intervalMs = 3_000L
        val deadline = System.currentTimeMillis() + maxWaitMs
        while (System.currentTimeMillis() < deadline) {
            try {
                val healthResponse = getApi().health()
                val body = healthResponse.body()?.string()?.trim()
                if (healthResponse.isSuccessful && !body.isNullOrEmpty()) {
                    return // server is healthy
                }
            } catch (_: Exception) {
                // server still waking — keep polling
            }
            delay(intervalMs)
        }
    }

    /** Parse the raw JSON string into ShortenResponse, or null on failure */
    private fun tryParseSuccess(json: String): ShortenResponse? = try {
        gson.fromJson(json, ShortenResponse::class.java)
            .takeIf { it.shortUrl.isNotBlank() && it.shortCode.isNotBlank() }
    } catch (e: JsonSyntaxException) {
        null
    }

    /** Observe all history entries (newest-first) as a reactive Flow */
    fun getHistory(): Flow<List<UrlHistoryEntity>> = dao.getAllHistory()

    /** Delete a single history entry by its local Room ID */
    suspend fun deleteHistoryEntry(id: Long) = dao.deleteById(id)

    /** Clear the entire history */
    suspend fun clearAllHistory() = dao.clearAll()

    /** Expose the current API base URL for the Settings screen pre-fill */
    val baseUrlFlow: Flow<String> = preferencesRepository.baseUrlFlow

    /** Persist a new API base URL from the Settings screen */
    suspend fun saveBaseUrl(url: String) = preferencesRepository.saveBaseUrl(url)
}
