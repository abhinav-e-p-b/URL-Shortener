package com.ironfleet.urlshortener.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for URL history operations.
 * All reads return a Flow so the UI automatically recomposes when data changes.
 */
@Dao
interface UrlHistoryDao {

    /**
     * Insert a newly shortened URL into local history.
     * REPLACE strategy handles the unlikely case of a duplicate primary key.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: UrlHistoryEntity)

    /**
     * Observe all history entries ordered newest-first.
     * Returns a Flow that emits whenever the table changes.
     */
    @Query("SELECT * FROM url_history ORDER BY createdAt DESC")
    fun getAllHistory(): Flow<List<UrlHistoryEntity>>

    /**
     * Delete a specific history entry by its local ID.
     */
    @Query("DELETE FROM url_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Clear all history (used by a future "Clear All" button).
     */
    @Query("DELETE FROM url_history")
    suspend fun clearAll()
}
