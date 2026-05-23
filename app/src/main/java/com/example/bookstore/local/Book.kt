package com.example.bookstore.local

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
    val lastUpdated: Long = System.currentTimeMillis()
)
