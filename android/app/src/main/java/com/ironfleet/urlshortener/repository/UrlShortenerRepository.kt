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
 *
 * Why raw ResponseBody?
 * Render's free tier returns HTTP 200 with an EMPTY body while cold-starting.
 * GsonConverterFactory would throw "End of input at line 1 column 1 path $"
 * trying to auto-parse it. We read the raw string first and check it's non-empty
 * before handing it to Gson.
 *
 * Cold-start strategy:
 *  1. Wake the server: poll GET /health with a SHORT timeout (8 s per call)
 *     every 3 s for up to 60 s — fast failure, no 45-second stalls.
 *  2. Once healthy, POST /shorten (up to 3 total POST attempts).
 */
@Singleton
class UrlShortenerRepository @Inject constructor(
    private val dao: UrlHistoryDao,
    private val preferencesRepository: PreferencesRepository,
    private val gson: Gson,
    private val okHttpClient: OkHttpClient
) {
    // Main API — built with the user's saved base URL and the full 45 s timeout
    private var currentBaseUrl: String = ""
    private var api: UrlShortenerApi? = null

    // Health-check API — same base URL but only 8 s per call so polls are fast
    private var healthApi: UrlShortenerApi? = null

    private val shortTimeoutClient: OkHttpClient by lazy {
        okHttpClient.newBuilder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    private suspend fun getApi(): UrlShortenerApi {
        val baseUrl = preferencesRepository.baseUrlFlow.first()
        if (api == null || baseUrl != currentBaseUrl) {
            currentBaseUrl = baseUrl
            api = buildApi(baseUrl, okHttpClient)
            healthApi = buildApi(baseUrl, shortTimeoutClient)
        }
        return api!!
    }

    private fun buildApi(baseUrl: String, client: OkHttpClient): UrlShortenerApi =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create(gson))
            .build()
            .create(UrlShortenerApi::class.java)

    /**
     * Shorten a URL via the Flask API, then persist the result to Room.
     *
     * Flow:
     *  1. Wake server — poll /health (8 s timeout each) until healthy or 60 s elapsed.
     *  2. POST /shorten — if body is still empty, retry up to 3 times with 2 s gap.
     */
    suspend fun shorten(longUrl: String): ShortenResult {
        return try {
            // Always ensure the API object (and healthApi) is initialised
            getApi()

            // Step 1: wake the server before trying to shorten
            val serverReady = waitForHealth(maxWaitMs = 60_000)
            if (!serverReady) {
                return ShortenResult.Error("Server is taking too long to wake up. Please try again.")
            }

            // Step 2: POST /shorten — retry up to 3 times if we still get an empty body
            shortenWithRetry(longUrl, attemptsLeft = 3)
        } catch (e: java.net.UnknownHostException) {
            ShortenResult.Error("NO_NETWORK")
        } catch (e: java.net.SocketTimeoutException) {
            ShortenResult.Error("Connection timed out. Please check your internet.")
        } catch (e: Exception) {
            ShortenResult.Error("Unexpected error: ${e.message}")
        }
    }

    private suspend fun shortenWithRetry(longUrl: String, attemptsLeft: Int): ShortenResult {
        val response = getApi().shorten(ShortenRequest(longUrl))
        val rawBody = response.body()?.string()?.trim()

        return when {
            // ── Success ───────────────────────────────────────────────────────
            response.isSuccessful && !rawBody.isNullOrEmpty() -> {
                val parsed = tryParseSuccess(rawBody)
                    ?: return ShortenResult.Error("Unexpected server response. Please try again.")

                dao.insert(
                    UrlHistoryEntity(
                        longUrl = longUrl,
                        shortCode = parsed.shortCode,
                        shortUrl = parsed.shortUrl
                    )
                )
                ShortenResult.Success(parsed.shortUrl, parsed.shortCode)
            }

            // ── Empty body after health passed → transient — retry ─────────
            response.isSuccessful && rawBody.isNullOrEmpty() -> {
                if (attemptsLeft <= 0) {
                    return ShortenResult.Error("Server woke up but returned no data. Please try again.")
                }
                delay(2_000)
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
                        "Server error (HTTP ${response.code()})"
                    }
                } else {
                    "Server error (HTTP ${response.code()})"
                }
                ShortenResult.Error(message)
            }
        }
    }

    /**
     * Poll GET /health using [healthApi] (8 s timeout per call) every 3 s.
     * Returns true as soon as a non-empty 2xx body is received.
     * Returns false if [maxWaitMs] elapses with no healthy response.
     *
     * Using a short per-call timeout means each poll fails fast so the
     * 60 s budget actually covers ~20 polls instead of stalling for 45 s each.
     */
    private suspend fun waitForHealth(maxWaitMs: Long): Boolean {
        val intervalMs = 3_000L
        val deadline = System.currentTimeMillis() + maxWaitMs
        while (System.currentTimeMillis() < deadline) {
            try {
                val r = healthApi!!.health()
                val body = r.body()?.string()?.trim()
                if (r.isSuccessful && !body.isNullOrEmpty()) return true
            } catch (_: Exception) {
                // still waking — swallow and retry
            }
            delay(intervalMs)
        }
        return false
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
