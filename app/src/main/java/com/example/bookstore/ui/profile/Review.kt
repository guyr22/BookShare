package com.example.bookstore.ui.profile

import androidx.annotation.DrawableRes

data class Review(
    val authorName: String,
    val bookTitle: String,
    val description: String,
    @DrawableRes val coverBackgroundRes: Int
)
