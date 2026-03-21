package com.rebook.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.rebook.app.data.model.BookMessage
import com.rebook.app.data.model.toBookMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

class MessageRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun messagesCollection(bookId: String) =
        firestore.collection("books").document(bookId).collection("messages")

    suspend fun sendMessage(bookId: String, text: String): Result<Unit> = withContext(Dispatchers.IO) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Message cannot be empty"))
        }
        val uid = auth.currentUser?.uid
            ?: return@withContext Result.failure(IllegalStateException("User not logged in"))
        try {
            val data = hashMapOf(
                "senderId" to uid,
                "text" to trimmed,
                "createdAt" to System.currentTimeMillis()
            )
            messagesCollection(bookId).add(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeMessages(bookId: String): Flow<List<BookMessage>> = callbackFlow {
        val query = messagesCollection(bookId).orderBy("createdAt", Query.Direction.ASCENDING)
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { it.toBookMessage() }.orEmpty()
            trySend(list)
        }
        awaitClose { registration.remove() }
    }
}
