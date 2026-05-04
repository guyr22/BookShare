package com.example.bookstore.repository

import androidx.lifecycle.LiveData
import com.example.bookstore.local.Book
import com.example.bookstore.local.BookDao
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class BookRepository(
    private val bookDao: BookDao,
    private val db: FirebaseDatabase = FirebaseDatabase.getInstance()
) {

    private val booksRef: DatabaseReference = db.getReference("books")

    // ── Room reads (single source of truth for the UI) ───────────────────────

    fun getAllBooks(): LiveData<List<Book>> = bookDao.getAllBooks()

    fun getBooksByOwner(ownerId: String): LiveData<List<Book>> =
        bookDao.getBooksByOwner(ownerId)

    // ── Firebase writes (write-through: Firebase first, then Room) ───────────

    suspend fun addBook(book: Book): AppResult<Book> {
        return try {
            val key = booksRef.push().key
                ?: return AppResult.Error("Firebase failed to generate a key.")
            val bookWithId = book.copy(id = key, lastUpdated = System.currentTimeMillis())
            booksRef.child(key).setValue(bookWithId.toFirebaseMap()).await()
            bookDao.insert(bookWithId)
            AppResult.Success(bookWithId)
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Failed to add book.", e)
        }
    }

    suspend fun updateBook(book: Book): AppResult<Book> {
        return try {
            val updated = book.copy(lastUpdated = System.currentTimeMillis())
            booksRef.child(updated.id).setValue(updated.toFirebaseMap()).await()
            bookDao.update(updated)
            AppResult.Success(updated)
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Failed to update book.", e)
        }
    }

    suspend fun deleteBook(book: Book): AppResult<Unit> {
        return try {
            booksRef.child(book.id).removeValue().await()
            bookDao.delete(book)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Failed to delete book.", e)
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun Book.toFirebaseMap(): Map<String, Any> = mapOf(
        "title" to title,
        "author" to author,
        "description" to description,
        "coverUrl" to coverUrl,
        "ownerId" to ownerId,
        "lastUpdated" to lastUpdated
    )
}
