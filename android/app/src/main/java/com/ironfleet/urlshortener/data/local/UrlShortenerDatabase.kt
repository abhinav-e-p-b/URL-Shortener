package com.ironfleet.urlshortener.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room database definition.
 * Increment [version] and provide a Migration whenever the schema changes.
 */
@Database(
    entities = [UrlHistoryEntity::class],
    version = 1,
    exportSchema = true
)
abstract class UrlShortenerDatabase : RoomDatabase() {
    abstract fun urlHistoryDao(): UrlHistoryDao
}
