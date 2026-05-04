package com.example.bookstore.repository

import androidx.lifecycle.LiveData
import com.example.bookstore.local.Book
import com.example.bookstore.local.BookDao
import com.google.firebase.database.DataSnapshot
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

    // ── Delta-fetch sync (course requirement) ────────────────────────────────

    /**
     * Queries Firebase for every book whose lastUpdated is strictly newer than
     * the highest timestamp already stored in Room, then persists the results.
     *
     * Call this from a ViewModel (viewModelScope) on app start or pull-to-refresh.
     * Returns the number of new/updated records written to Room.
     */
    suspend fun syncFromFirebase(): AppResult<Int> {
        return try {
            val since = bookDao.getMaxLastUpdated() ?: 0L

            val snapshot = booksRef
                .orderByChild("lastUpdated")
                .startAt((since + 1).toDouble())   // +1 excludes the record we already have
                .get()
                .await()

            val newBooks = snapshot.children.mapNotNull { it.toBook() }

            if (newBooks.isNotEmpty()) {
                bookDao.insertAll(newBooks)
            }

            AppResult.Success(newBooks.size)
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Sync failed.", e)
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun DataSnapshot.toBook(): Book? {
        return try {
            Book(
                id = key ?: return null,
                title = child("title").getValue(String::class.java) ?: "",
                author = child("author").getValue(String::class.java) ?: "",
                description = child("description").getValue(String::class.java) ?: "",
                coverUrl = child("coverUrl").getValue(String::class.java) ?: "",
                ownerId = child("ownerId").getValue(String::class.java) ?: return null,
                lastUpdated = child("lastUpdated").getValue(Long::class.java)
                    ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun Book.toFirebaseMap(): Map<String, Any> = mapOf(
        "title" to title,
        "author" to author,
        "description" to description,
        "coverUrl" to coverUrl,
        "ownerId" to ownerId,
        "lastUpdated" to lastUpdated
    )
}
