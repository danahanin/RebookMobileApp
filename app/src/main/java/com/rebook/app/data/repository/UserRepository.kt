package com.rebook.app.data.repository

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.rebook.app.data.local.AppDatabase
import com.rebook.app.data.local.entity.UserEntity
import com.rebook.app.data.model.User
import kotlinx.coroutines.tasks.await
import java.util.UUID

class UserRepository(context: Context) {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val usersRef = firestore.collection("users")
    private val userDao = AppDatabase.getInstance(context).userDao()

    fun getCurrentUser(): User? {
        val firebaseUser = auth.currentUser ?: return null
        return User(
            id = firebaseUser.uid,
            email = firebaseUser.email ?: "",
            displayName = firebaseUser.displayName ?: "",
            profileImageUrl = firebaseUser.photoUrl?.toString()
        )
    }

    suspend fun getCurrentUserFromFirestore(): User? {
        val firebaseUser = auth.currentUser ?: return null
        return try {
            val doc = usersRef.document(firebaseUser.uid).get().await()
            val profileImageUrl = if (doc.exists()) {
                doc.getString("profileImageUrl") ?: firebaseUser.photoUrl?.toString()
            } else {
                firebaseUser.photoUrl?.toString()
            }
            val displayName = if (doc.exists()) {
                doc.getString("displayName")?.takeIf { it.isNotEmpty() }
                    ?: firebaseUser.displayName ?: ""
            } else {
                firebaseUser.displayName ?: ""
            }
            val user = User(
                id = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                displayName = displayName,
                profileImageUrl = profileImageUrl
            )
            userDao.insertUser(user.toEntity())
            user
        } catch (e: Exception) {
            // Fall back to Room cache, then to in-memory Auth data
            userDao.getUserById(firebaseUser.uid)
                ?.let { User.fromEntity(it) }
                ?: getCurrentUser()
        }
    }

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    suspend fun syncUserDocumentFromAuth(): Result<Unit> {
        val user = auth.currentUser ?: return Result.success(Unit)
        return try {
            usersRef.document(user.uid).set(
                mapOf(
                    "displayName" to (user.displayName ?: ""),
                    "email" to (user.email ?: "")
                ),
                SetOptions.merge()
            ).await()
            userDao.insertUser(
                UserEntity(
                    id = user.uid,
                    email = user.email ?: "",
                    displayName = user.displayName ?: "",
                    profileImageUrl = user.photoUrl?.toString()
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDisplayNameForUser(uid: String): String? {
        // Serve from Room cache when available to avoid unnecessary Firestore reads
        val cached = userDao.getUserById(uid)
        if (cached != null && cached.displayName.isNotEmpty()) {
            return cached.displayName
        }
        return try {
            val doc = usersRef.document(uid).get().await()
            if (!doc.exists()) return null
            val name = doc.getString("displayName")?.takeIf { it.isNotEmpty() }
                ?: doc.getString("email")?.substringBefore("@")?.takeIf { it.isNotEmpty() }
            userDao.insertUser(
                UserEntity(
                    id = uid,
                    email = doc.getString("email") ?: "",
                    displayName = name ?: "",
                    profileImageUrl = doc.getString("profileImageUrl")
                )
            )
            name
        } catch (e: Exception) {
            cached?.displayName?.takeIf { it.isNotEmpty() }
        }
    }

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

            userDao.insertUser(
                UserEntity(
                    id = user.uid,
                    email = user.email ?: "",
                    displayName = displayName,
                    profileImageUrl = imageUrl
                )
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadProfileImage(imageUri: Uri): Result<String> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("User not logged in"))
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
