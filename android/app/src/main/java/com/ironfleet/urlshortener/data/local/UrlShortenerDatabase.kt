package com.ironfleet.urlshortener.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database definition.
 * Increment [version] and provide a Migration whenever the schema changes.
 *
 * Version history:
 *  1 — initial schema (id, longUrl, shortCode, shortUrl, createdAt)
 *  2 — added clickCount INTEGER NOT NULL DEFAULT 0
 */
@Database(
    entities = [UrlHistoryEntity::class],
    version = 2,
    exportSchema = true
)
abstract class UrlShortenerDatabase : RoomDatabase() {
    abstract fun urlHistoryDao(): UrlHistoryDao

    companion object {
        /**
         * Migration from v1 to v2: adds the clickCount column.
         * Existing rows default to 0 — the HistoryViewModel will refresh
         * live counts from the server on the next app launch.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE url_history ADD COLUMN clickCount INTEGER NOT NULL DEFAULT 0"
                )
            }
        }
    }
}
