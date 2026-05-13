package com.example.bookshare.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/**
 * Data Access Object for the [User] entity.
 *
 * All write operations are suspend functions so they must be called from a coroutine
 * (viewModelScope or a Repository-level coroutine). Read operations return LiveData so
 * the UI automatically reacts to changes without manually re-querying.
 */
@Dao
interface UserDao {

    /**
     * Insert or replace a user. Used when syncing from Firebase —
     * if the user already exists locally the row is replaced with the latest data.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: User)

    /**
     * Insert or replace a batch of users in a single transaction.
     * Used during a bulk sync from Firebase.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<User>)

    /**
     * Update an existing user record (e.g. after the user edits their profile).
     */
    @Update
    suspend fun update(user: User)

    /**
     * Observe the currently logged-in user by Firebase UID.
     * Returns null wrapped in LiveData if the user has never been cached locally.
     */
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun getUserById(userId: String): LiveData<User?>

    /**
     * Observe every cached user. The Feed uses this to map ownerId → display name
     * so other users' posts show their real name instead of a placeholder.
     */
    @Query("SELECT * FROM users")
    fun getAll(): LiveData<List<User>>

    /**
     * One-shot (non-observable) fetch of a user — useful inside suspend functions
     * that need the value immediately (e.g. before pushing to Firebase).
     */
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserByIdOnce(userId: String): User?

    /**
     * Returns the highest lastUpdated timestamp stored locally.
     * The Repository uses this value for the delta-fetch: it queries Firebase
     * for records where lastUpdated > this value so only new/changed users are downloaded.
     */
    @Query("SELECT MAX(lastUpdated) FROM users")
    suspend fun getMaxLastUpdated(): Long?

    /**
     * Delete a user by ID — called when the account is deleted from Firebase Auth.
     */
    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteById(userId: String)
}
