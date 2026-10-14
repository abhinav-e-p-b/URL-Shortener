package com.ironfleet.urlshortener.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.ironfleet.urlshortener.data.local.PreferencesRepository
import com.ironfleet.urlshortener.data.remote.UrlShortenerApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt module that provides Retrofit + OkHttp networking dependencies.
 *
 * The Retrofit base URL is read from DataStore at startup so the user can
 * change it in the Settings screen. A Retrofit rebuild is triggered by the
 * repository whenever the URL changes.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().create()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            // Log request/response bodies in debug; set NONE in release via BuildConfig if needed
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            // Render free tier cold-starts can take ~30 s — use 45 s to be safe.
            .connectTimeout(45, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(45, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson,
        preferencesRepository: PreferencesRepository
    ): Retrofit {
        // Read the saved base URL synchronously at injection time.
        // The UrlShortenerRepository rebuilds the API instance when the URL changes.
        val baseUrl = runBlocking { preferencesRepository.baseUrlFlow.first() }
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideUrlShortenerApi(retrofit: Retrofit): UrlShortenerApi {
        return retrofit.create(UrlShortenerApi::class.java)
    }
}
