package com.ironfleet.urlshortener.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing one shortened URL stored locally.
 * Mirrors the history that the web frontend stored in localStorage.
 *
 * Version history:
 *  v1 — initial schema
 *  v2 — added clickCount (cached from server's /stats endpoint)
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
    val createdAt: Long = System.currentTimeMillis(),

    /**
     * Number of times this short URL has been redirected (by anyone).
     * Fetched from the server's GET /stats/<short_code> endpoint and
     * cached locally so the History screen can display it offline.
     * Defaults to 0 until the first successful stats refresh.
     */
    val clickCount: Int = 0
)
