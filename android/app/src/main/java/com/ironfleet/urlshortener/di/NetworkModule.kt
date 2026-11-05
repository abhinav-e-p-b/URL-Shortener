package com.ironfleet.urlshortener.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.ironfleet.urlshortener.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt module that provides shared networking primitives (OkHttpClient + Gson).
 *
 * Note: Retrofit and UrlShortenerApi are NOT provided here because
 * UrlShortenerRepository rebuilds the API dynamically whenever the user
 * changes the base URL in Settings. Only OkHttpClient and Gson are shared.
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
            // Log full bodies in debug only — avoids leaking URLs in release logcat
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            // Render free tier cold-starts can take ~30 s — use 45 s to be safe.
            .connectTimeout(45, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(45, TimeUnit.SECONDS)
            .build()
    }
}
