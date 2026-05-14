package com.example.bookshare.repository

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import com.example.bookshare.local.Book
import com.example.bookshare.local.BookDao
import com.example.bookshare.network.NetworkClient
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class BookRepository(
    private val bookDao: BookDao,
    private val storageRepository: StorageRepository = StorageRepository(),
    private val db: FirebaseDatabase = FirebaseDatabase.getInstance()
) {

    private val booksRef: DatabaseReference = db.getReference("books")

    // ── Room reads (single source of truth for the UI) ───────────────────────

    fun getAllBooks(): LiveData<List<Book>> = bookDao.getAllBooks()

    fun getBooksByOwner(ownerId: String): LiveData<List<Book>> =
        bookDao.getBooksByOwner(ownerId)

    fun getBookById(bookId: String): LiveData<Book?> =
        bookDao.getBookById(bookId)

    // ── Google Books search (pre-fill helper for the AddEdit screen) ─────────

    suspend fun searchGoogleBooks(query: String): AppResult<List<Book>> {
        return try {
            val response = NetworkClient.googleBooksApi.searchByTitle(query, maxResults = 10)
            val books = response.items.orEmpty().map { item ->
                val info = item.volumeInfo
                Book(
                    id = "",
                    title = info.title,
                    author = info.authors.joinToString(", "),
                    description = info.description,
                    coverUrl = info.imageLinks?.thumbnail.orEmpty(),
                    ownerId = ""
                )
            }
            AppResult.Success(books)
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Google Books search failed.", e)
        }
    }

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

    // ── Save flow (new + edit with optional image upload) ────────────────────

    /**
     * Full save pipeline:
     *   1. If [coverBitmap] is provided, upload it to Firebase Storage and use
     *      the returned URL as the book's coverUrl.
     *   2. Generate a Firebase key locally (no network call required).
     *   3. Write the finished book to Room immediately so the UI updates at once.
     *   4. Push the same record to Firebase Realtime Database.
     *
     * For edits, pass the existing book with a non-null [coverBitmap] only when
     * the user has selected a new image; otherwise the existing coverUrl is kept.
     */
    suspend fun saveBook(book: Book, coverBitmap: Bitmap? = null): AppResult<Book> {
        return try {
            // Step 1 – optional image upload
            val coverUrl = if (coverBitmap != null) {
                val path = "book_covers/${book.ownerId}/${System.currentTimeMillis()}.jpg"
                when (val result = storageRepository.uploadBitmap(coverBitmap, path)) {
                    is AppResult.Success -> result.data
                    is AppResult.Error -> return result
                }
            } else {
                book.coverUrl
            }

            // Step 2 – resolve the Firebase key (new book gets push key; edit keeps its id)
            val key = if (book.id.isBlank()) {
                booksRef.push().key ?: return AppResult.Error("Firebase failed to generate a key.")
            } else {
                book.id
            }

            // Step 3 – build the final record with resolved id, coverUrl, and timestamp
            val finalBook = book.copy(
                id = key,
                coverUrl = coverUrl,
                lastUpdated = System.currentTimeMillis()
            )

            // Step 4 – persist to Room first so the UI reacts immediately
            bookDao.insert(finalBook)

            // Step 5 – push to Firebase Realtime Database
            booksRef.child(key).setValue(finalBook.toFirebaseMap()).await()

            AppResult.Success(finalBook)
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Failed to save book.", e)
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
