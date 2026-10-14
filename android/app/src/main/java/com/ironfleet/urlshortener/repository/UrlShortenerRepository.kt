package com.ironfleet.urlshortener.repository

import com.google.gson.Gson
import com.ironfleet.urlshortener.data.local.PreferencesRepository
import com.ironfleet.urlshortener.data.local.UrlHistoryDao
import com.ironfleet.urlshortener.data.local.UrlHistoryEntity
import com.ironfleet.urlshortener.data.remote.UrlShortenerApi
import com.ironfleet.urlshortener.data.remote.dto.ApiErrorResponse
import com.ironfleet.urlshortener.data.remote.dto.ShortenRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
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
     * This avoids Hilt re-injection complexity while still supporting dynamic URLs.
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
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(UrlShortenerApi::class.java)
        }
        return api!!
    }

    /**
     * Shorten a URL via the Flask API, then persist the result to Room.
     * @param longUrl the original long URL
     * @return [ShortenResult] — Success, RateLimited, or Error
     */
    suspend fun shorten(longUrl: String): ShortenResult {
        return try {
            val response = getApi().shorten(ShortenRequest(longUrl))
            when {
                response.isSuccessful -> {
                    val body = response.body()
                        ?: return ShortenResult.Error("Empty response from server")

                    // Persist to local history
                    dao.insert(
                        UrlHistoryEntity(
                            longUrl = longUrl,
                            shortCode = body.shortCode,
                            shortUrl = body.shortUrl
                        )
                    )
                    ShortenResult.Success(body.shortUrl, body.shortCode)
                }
                response.code() == 429 -> ShortenResult.RateLimited
                else -> {
                    val errorBody = response.errorBody()?.string()
                    val message = try {
                        gson.fromJson(errorBody, ApiErrorResponse::class.java).error
                    } catch (e: Exception) {
                        "HTTP ${response.code()}"
                    }
                    ShortenResult.Error(message)
                }
            }
        } catch (e: java.net.UnknownHostException) {
            ShortenResult.Error("NO_NETWORK")
        } catch (e: java.net.SocketTimeoutException) {
            ShortenResult.Error("Request timed out. Check your connection.")
        } catch (e: Exception) {
            ShortenResult.Error(e.message ?: "Unknown error")
        }
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
