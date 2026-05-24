package com.example.bookshare.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class Book(
    @PrimaryKey
    val id: String,
    val title: String,
    val author: String,
    val description: String = "",
    val coverUrl: String = "",
    val ownerId: String,
    /** The owner's star rating for the book, 0–5 (0 = unrated). */
    val rating: Int = 0,
    /** The owner's written review. Shown as the primary text on the feed card. */
    val review: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
)
