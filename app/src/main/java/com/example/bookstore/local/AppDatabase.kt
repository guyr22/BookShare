package com.example.bookstore.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room Database — the single source of truth for all local data.
 *
 * Architecture — Offline-First (Single Source of Truth):
 *   Firebase/Retrofit  ──►  Room  ──►  LiveData  ──►  ViewModel  ──►  UI
 *
 * Versioning
 * ----------
 * Increment [version] whenever the schema changes and provide a Migration object,
 * or use fallbackToDestructiveMigration() during development only.
 *
 * Adding new entities
 * -------------------
 * 1. Create the @Entity class in this package.
 * 2. Add it to the [entities] array below.
 * 3. Add an abstract DAO accessor function.
 * 4. Bump [version] and supply a Migration.
 */
@Database(
    entities = [User::class],
    version = 1,
    exportSchema = true          // keeps a JSON schema history in app/schemas/ — good for code review
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

    companion object {

        // Volatile ensures the singleton value is always up-to-date across all threads.
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Returns the singleton database instance, creating it if necessary.
         * Safe to call from any thread; uses double-checked locking.
         *
         * @param context Use applicationContext to avoid memory leaks.
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bookshare_database"
                )
                    // TODO: replace with proper Migrations before production
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
