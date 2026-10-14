package com.ironfleet.urlshortener.di

import android.content.Context
import androidx.room.Room
import com.ironfleet.urlshortener.data.local.UrlHistoryDao
import com.ironfleet.urlshortener.data.local.UrlShortenerDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides Room database and DAO instances.
 * Scoped to SingletonComponent so a single DB connection is shared app-wide.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): UrlShortenerDatabase {
        return Room.databaseBuilder(
            context,
            UrlShortenerDatabase::class.java,
            "url_shortener.db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideUrlHistoryDao(database: UrlShortenerDatabase): UrlHistoryDao {
        return database.urlHistoryDao()
    }
}
