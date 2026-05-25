package com.example.bookshare.repository

import android.graphics.Bitmap
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

class StorageRepository(
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {

    suspend fun uploadBitmap(bitmap: Bitmap, storagePath: String): AppResult<String> {
        return try {
            val bytes = ByteArrayOutputStream().use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                stream.toByteArray()
            }

            val ref = storage.reference.child(storagePath)
            val uploadTask = ref.putBytes(bytes).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await().toString()

            AppResult.Success(downloadUrl)
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Upload failed.", e)
        }
    }
}
