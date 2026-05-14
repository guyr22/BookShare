package com.example.bookshare

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.bookshare.local.AppDatabase
import com.example.bookshare.local.User
import com.example.bookshare.local.UserDao
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: UserDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.userDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun insertAndRetrieveUser() = runBlocking {
        val user = User(id = "uid_1", name = "Roni", email = "roni@test.com")
        dao.insert(user)

        val result = dao.getUserByIdOnce("uid_1")
        assertEquals(user, result)
    }

    @Test
    fun insertReplacesExistingUser() = runBlocking {
        val original = User(id = "uid_1", name = "Roni", email = "roni@test.com")
        val updated = User(id = "uid_1", name = "Roni Updated", email = "roni@test.com")
        dao.insert(original)
        dao.insert(updated)

        val result = dao.getUserByIdOnce("uid_1")
        assertEquals("Roni Updated", result?.name)
    }

    @Test
    fun getUserByIdOnce_returnsNullForMissingUser() = runBlocking {
        val result = dao.getUserByIdOnce("nonexistent")
        assertNull(result)
    }

    @Test
    fun getMaxLastUpdated_returnsHighestTimestamp() = runBlocking {
        dao.insert(User(id = "uid_1", name = "A", email = "a@test.com", lastUpdated = 1000L))
        dao.insert(User(id = "uid_2", name = "B", email = "b@test.com", lastUpdated = 5000L))

        val max = dao.getMaxLastUpdated()
        assertEquals(5000L, max)
    }

    @Test
    fun deleteById_removesUser() = runBlocking {
        dao.insert(User(id = "uid_1", name = "Roni", email = "roni@test.com"))
        dao.deleteById("uid_1")

        val result = dao.getUserByIdOnce("uid_1")
        assertNull(result)
    }
}
