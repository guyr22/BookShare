package com.example.bookshare.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface BookDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(book: Book)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(books: List<Book>)

    @Update
    suspend fun update(book: Book)

    @Delete
    suspend fun delete(book: Book)

    @Query("DELETE FROM books WHERE id = :bookId")
    suspend fun deleteById(bookId: String)

    @Query("SELECT * FROM books ORDER BY lastUpdated DESC")
    fun getAllBooks(): LiveData<List<Book>>

    @Query("SELECT * FROM books WHERE ownerId = :ownerId ORDER BY lastUpdated DESC")
    fun getBooksByOwner(ownerId: String): LiveData<List<Book>>

    @Query("SELECT * FROM books WHERE id = :bookId LIMIT 1")
    fun getBookById(bookId: String): LiveData<Book?>

    @Query("SELECT MAX(lastUpdated) FROM books")
    suspend fun getMaxLastUpdated(): Long?
}
