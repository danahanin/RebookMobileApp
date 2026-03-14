package com.rebook.app.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.rebook.app.data.model.User
import kotlinx.coroutines.tasks.await
import java.util.UUID

class UserRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val usersRef = firestore.collection("users")

    fun getCurrentUser(): User? {
        val firebaseUser = auth.currentUser ?: return null
        return User(
            id = firebaseUser.uid,
            email = firebaseUser.email ?: "",
            displayName = firebaseUser.displayName ?: "",
            profileImageUrl = firebaseUser.photoUrl?.toString()
        )
    }

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    suspend fun updateProfile(displayName: String, imageUrl: String?): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("Not logged in"))

            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .apply { imageUrl?.let { setPhotoUri(Uri.parse(it)) } }
                .build()

            user.updateProfile(profileUpdates).await()

            usersRef.document(user.uid).set(
                mapOf(
                    "displayName" to displayName,
                    "email" to (user.email ?: ""),
                    "profileImageUrl" to imageUrl
                )
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadProfileImage(imageUri: Uri): Result<String> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("Not logged in"))
            val filename = "profile_${user.uid}_${UUID.randomUUID()}.jpg"
            val ref = storage.reference.child("profile_images/$filename")

            ref.putFile(imageUri).await()
            val downloadUrl = ref.downloadUrl.await().toString()

            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
