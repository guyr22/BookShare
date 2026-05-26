package com.example.bookshare.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing a BookShare user.
 * Mirrors the user document stored in Firebase; Room is the single source of truth.
 *
 * Fields
 * ------
 * id        – Firebase UID (string), used as the primary key so it matches Firebase exactly.
 * name      – Display name shown on the Feed and Profile screens.
 * email     – Used for authentication and display on the Profile screen.
 * avatarUrl – Remote URL (Firebase Storage) loaded by Picasso in the UI.
 * lastUpdated – Unix epoch millis; used by the delta-fetch sync to skip already-cached records.
 */
@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val id: String,
    val name: String,
    val email: String,
    val avatarUrl: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
)
