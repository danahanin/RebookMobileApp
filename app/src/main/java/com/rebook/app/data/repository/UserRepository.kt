package com.rebook.app.data.repository

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.rebook.app.data.local.AppDatabase
import com.rebook.app.data.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class UserRepository(context: Context) {

    private val userDao = AppDatabase.getInstance(context).userDao()
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

    fun observeCurrentUser(): Flow<User?> {
        val uid = auth.currentUser?.uid ?: return kotlinx.coroutines.flow.flowOf(null)
        return userDao.observeUserById(uid).map { entity ->
            entity?.let { User.fromEntity(it) }
        }
    }

    suspend fun syncCurrentUser() {
        val firebaseUser = auth.currentUser ?: return
        val user = User(
            id = firebaseUser.uid,
            email = firebaseUser.email ?: "",
            displayName = firebaseUser.displayName ?: "",
            profileImageUrl = firebaseUser.photoUrl?.toString()
        )
        userDao.insertUser(user.toEntity())
    }

    suspend fun updateProfile(displayName: String, imageUrl: String?): Result<Unit> {
        return try {
            val firebaseUser = auth.currentUser
                ?: return Result.failure(Exception("User not logged in"))

            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .apply { imageUrl?.let { setPhotoUri(Uri.parse(it)) } }
                .build()

            firebaseUser.updateProfile(profileUpdates).await()

            val userData = hashMapOf(
                "displayName" to displayName,
                "profileImageUrl" to imageUrl,
                "email" to firebaseUser.email
            )
            usersRef.document(firebaseUser.uid).set(userData).await()

            val updatedUser = User(
                id = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                displayName = displayName,
                profileImageUrl = imageUrl
            )
            userDao.insertUser(updatedUser.toEntity())

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadProfileImage(imageUri: Uri): Result<String> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not logged in"))

            val ref = storage.reference.child("profile_images/$uid.jpg")
            ref.putFile(imageUri).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
