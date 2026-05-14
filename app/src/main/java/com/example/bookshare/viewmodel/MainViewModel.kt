package com.example.bookshare.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.bookshare.local.Book
import com.example.bookshare.repository.AppResult
import com.example.bookshare.repository.BookRepository
import kotlinx.coroutines.launch

class MainViewModel(private val bookRepository: BookRepository) : ViewModel() {

    // All books from Room — UI observes this and reacts to every local change.
    val allBooks: LiveData<List<Book>> = bookRepository.getAllBooks()

    private val _syncResult = MutableLiveData<AppResult<Int>>()
    val syncResult: LiveData<AppResult<Int>> = _syncResult

    private val _bookOperation = MutableLiveData<AppResult<*>>()
    val bookOperation: LiveData<AppResult<*>> = _bookOperation

    // ── Sync ─────────────────────────────────────────────────────────────────

    fun syncBooks() {
        viewModelScope.launch {
            _syncResult.value = bookRepository.syncFromFirebase()
        }
    }

    // ── Per-owner query (Profile screen) ─────────────────────────────────────

    fun getBooksByOwner(ownerId: String): LiveData<List<Book>> =
        bookRepository.getBooksByOwner(ownerId)

    fun getBookById(bookId: String): LiveData<Book?> =
        bookRepository.getBookById(bookId)

    // ── Google Books search (AddEdit pre-fill) ───────────────────────────────

    private val _searchResult = MutableLiveData<AppResult<List<Book>>>()
    val searchResult: LiveData<AppResult<List<Book>>> = _searchResult

    fun searchGoogleBooks(query: String) {
        viewModelScope.launch {
            _searchResult.value = bookRepository.searchGoogleBooks(query)
        }
    }

    // ── Save (new + edit with optional cover image) ───────────────────────────

    fun saveBook(book: Book, coverBitmap: Bitmap? = null) {
        viewModelScope.launch {
            _bookOperation.value = bookRepository.saveBook(book, coverBitmap)
        }
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────

    fun addBook(book: Book) {
        viewModelScope.launch {
            _bookOperation.value = bookRepository.addBook(book)
        }
    }

    fun updateBook(book: Book) {
        viewModelScope.launch {
            _bookOperation.value = bookRepository.updateBook(book)
        }
    }

    fun deleteBook(book: Book) {
        viewModelScope.launch {
            _bookOperation.value = bookRepository.deleteBook(book)
        }
    }

    class Factory(private val bookRepository: BookRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MainViewModel(bookRepository) as T
    }
}
