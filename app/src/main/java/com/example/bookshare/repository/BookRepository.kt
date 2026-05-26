package com.example.bookshare.repository

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import com.example.bookshare.BuildConfig
import com.example.bookshare.model.Book
import com.example.bookshare.local.BookDao
import com.example.bookshare.network.NetworkClient
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.bookshare.network.toErrorResult
import retrofit2.HttpException

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
        // Send the key when configured (local.properties → BuildConfig). Keyless
        // requests share a tiny per-IP quota and 429 easily.
        val apiKey = BuildConfig.BOOKS_API_KEY.takeIf { it.isNotBlank() }
        var lastError: Exception? = null

        // Two attempts: a transient 429 is retried once after a short backoff. A
        // persistent 429 means the (keyless) quota is exhausted — surface a clear
        // message pointing at BOOKS_API_KEY.
        repeat(2) { attempt ->
            try {
                val response = NetworkClient.googleBooksApi.searchByTitle(
                    query, maxResults = 10, apiKey = apiKey
                )
                val books = response.items.orEmpty().map { item ->
                    val info = item.volumeInfo
                    Book(
                        id = "",
                        title = info.title,
                        author = info.authors.joinToString(", "),
                        description = info.description,
                        coverUrl = info.imageLinks?.thumbnail.orEmpty().replace("http://", "https://"),
                        ownerId = ""
                    )
                }
                return AppResult.Success(books)
            } catch (e: HttpException) {
                lastError = e
                when {
                    e.code() == 429 && attempt == 0 -> delay(1500L) // back off, then retry
                    e.code() == 429 -> return AppResult.Error(
                        "Google Books is rate-limiting requests (HTTP 429). Set BOOKS_API_KEY " +
                            "in local.properties, or wait a moment and try again.",
                        e
                    )
                    else -> return AppResult.Error("Google Books search failed (HTTP ${e.code()}).", e)
                }
            } catch (e: Exception) {
                return e.toErrorResult("Google Books search failed.")
            }
        }
        return AppResult.Error("Google Books search failed.", lastError)
    }

    // ── Firebase writes (write-through: Firebase first, then Room) ───────────

    suspend fun addBook(book: Book): AppResult<Book> {
        return try {
            val key = booksRef.push().key
                ?: return AppResult.Error("Firebase failed to generate a key.")
            val bookWithKey = book.copy(id = key)
            booksRef.child(key).setValue(bookWithKey.toFirebaseMap()).await()
            val serverTimestamp = readServerTimestamp(key)
            val finalBook = bookWithKey.copy(lastUpdated = serverTimestamp)
            bookDao.insert(finalBook)
            AppResult.Success(finalBook)
        } catch (e: Exception) {
            e.toErrorResult("Failed to add book.")
        }
    }

    suspend fun updateBook(book: Book): AppResult<Book> {
        return try {
            booksRef.child(book.id).setValue(book.toFirebaseMap()).await()
            val serverTimestamp = readServerTimestamp(book.id)
            val updated = book.copy(lastUpdated = serverTimestamp)
            bookDao.update(updated)
            AppResult.Success(updated)
        } catch (e: Exception) {
            e.toErrorResult("Failed to update book.")
        }
    }

    suspend fun deleteBook(book: Book): AppResult<Unit> {
        return try {
            booksRef.child(book.id).removeValue().await()
            bookDao.delete(book)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            e.toErrorResult("Failed to delete book.")
        }
    }

    // ── Save flow (new + edit with optional image upload) ────────────────────

    /**
     * Full save pipeline:
     *   1. If [coverBitmap] is provided, upload it to Firebase Storage and use
     *      the returned URL as the book's coverUrl.
     *   2. Resolve the Firebase key (new book gets push key; edit keeps its id).
     *   3. Write to Firebase with ServerValue.TIMESTAMP so all devices share the same clock.
     *   4. Read the server-assigned timestamp back and persist to Room.
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

            val bookWithKey = book.copy(id = key, coverUrl = coverUrl)

            // Step 3 – push to Firebase with ServerValue.TIMESTAMP (server sets the timestamp)
            booksRef.child(key).setValue(bookWithKey.toFirebaseMap()).await()

            // Step 4 – read back the server timestamp and persist to Room
            val serverTimestamp = readServerTimestamp(key)
            val finalBook = bookWithKey.copy(lastUpdated = serverTimestamp)
            bookDao.insert(finalBook)

            AppResult.Success(finalBook)
        } catch (e: Exception) {
            e.toErrorResult("Failed to save book.")
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
            val maxKnown = bookDao.getMaxLastUpdated() ?: 0L
            // Subtract a 60-second buffer so books whose server timestamp landed just
            // before our local max are not silently skipped by the delta query.
            // INSERT OR REPLACE in the DAO makes re-fetching duplicates safe.
            val since = maxOf(0L, maxKnown - 60_000L)

            val snapshot = booksRef
                .orderByChild("lastUpdated")
                .startAt(since.toDouble())
                .get()
                .await()

            val newBooks = snapshot.children.mapNotNull { it.toBook() }

            if (newBooks.isNotEmpty()) {
                bookDao.insertAll(newBooks)
            }

            AppResult.Success(newBooks.size)
        } catch (e: Exception) {
            e.toErrorResult("Sync failed.")
        }
    }

    // ── Real-time listener ───────────────────────────────────────────────────

    private var childListener: ChildEventListener? = null

    /**
     * Attaches a Firebase ChildEventListener that writes every add/change/remove
     * directly into Room. The Room LiveData then propagates the change to the UI
     * automatically, with no manual sync or logout/login required.
     *
     * Safe to call multiple times — re-attaches only if not already listening.
     */
    fun startRealtimeSync(scope: CoroutineScope) {
        if (childListener != null) return
        childListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                snapshot.toBook()?.let { book ->
                    scope.launch(Dispatchers.IO) { bookDao.insert(book) }
                }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                snapshot.toBook()?.let { book ->
                    scope.launch(Dispatchers.IO) { bookDao.insert(book) }
                }
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {
                val id = snapshot.key ?: return
                scope.launch(Dispatchers.IO) { bookDao.deleteById(id) }
            }
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }
        booksRef.addChildEventListener(childListener!!)
    }

    fun stopRealtimeSync() {
        childListener?.let { booksRef.removeEventListener(it) }
        childListener = null
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
                rating = (child("rating").getValue(Long::class.java) ?: 0L).toInt(),
                review = child("review").getValue(String::class.java) ?: "",
                lastUpdated = child("lastUpdated").getValue(Long::class.java)
                    ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }

    /** Reads the lastUpdated value that Firebase set server-side after a write. */
    private suspend fun readServerTimestamp(key: String): Long =
        booksRef.child(key).child("lastUpdated").get().await()
            .getValue(Long::class.java) ?: System.currentTimeMillis()

    private fun Book.toFirebaseMap(): Map<String, Any> = mapOf(
        "title" to title,
        "author" to author,
        "description" to description,
        "coverUrl" to coverUrl,
        "ownerId" to ownerId,
        "rating" to rating,
        "review" to review,
        "lastUpdated" to ServerValue.TIMESTAMP
    )
}
