package com.ironfleet.urlshortener.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing one shortened URL stored locally.
 * Mirrors the history that the web frontend stored in localStorage.
 */
@Entity(tableName = "url_history")
data class UrlHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** The original long URL the user pasted */
    val longUrl: String,

    /** The short code returned by the API (e.g. "abc") */
    val shortCode: String,

    /** The full short URL (e.g. "https://url-shortener-1-m70j.onrender.com/abc") */
    val shortUrl: String,

    /** Unix epoch millis when this entry was created */
    val createdAt: Long = System.currentTimeMillis()
)
