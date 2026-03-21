package com.rebook.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.rebook.app.data.model.BookMessage
import com.rebook.app.data.model.ChatThreadSummary
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

    private fun chatsCollection(bookId: String) =
        firestore.collection("books").document(bookId).collection("chats")

    private fun messagesCollection(bookId: String, chatBuyerUid: String) =
        chatsCollection(bookId).document(chatBuyerUid).collection("messages")

    suspend fun sendMessage(
        bookId: String,
        chatBuyerUid: String,
        text: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Message cannot be empty"))
        }
        val uid = auth.currentUser?.uid
            ?: return@withContext Result.failure(IllegalStateException("User not logged in"))
        try {
            val chatFields = hashMapOf<String, Any>(
                "buyerId" to chatBuyerUid,
                "updatedAt" to System.currentTimeMillis()
            )
            if (uid == chatBuyerUid) {
                val label = auth.currentUser?.displayName?.takeIf { it.isNotEmpty() }
                    ?: auth.currentUser?.email?.substringBefore("@")?.takeIf { it.isNotEmpty() }
                    ?: ""
                if (label.isNotEmpty()) {
                    chatFields["buyerDisplayName"] = label
                }
            }
            chatsCollection(bookId).document(chatBuyerUid).set(
                chatFields,
                SetOptions.merge()
            ).await()
            val data = hashMapOf(
                "senderId" to uid,
                "text" to trimmed,
                "createdAt" to System.currentTimeMillis()
            )
            messagesCollection(bookId, chatBuyerUid).add(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeMessages(bookId: String, chatBuyerUid: String): Flow<List<BookMessage>> = callbackFlow {
        if (bookId.isBlank() || chatBuyerUid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val query = messagesCollection(bookId, chatBuyerUid)
            .orderBy("createdAt", Query.Direction.ASCENDING)
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

    fun observeChatThreads(bookId: String): Flow<List<ChatThreadSummary>> = callbackFlow {
        if (bookId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val registration = chatsCollection(bookId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.map { doc ->
                val updated = doc.getLong("updatedAt") ?: 0L
                val name = doc.getString("buyerDisplayName")?.takeIf { it.isNotEmpty() }
                ChatThreadSummary(
                    buyerId = doc.id,
                    updatedAtMillis = updated,
                    buyerDisplayName = name
                )
            }.orEmpty().sortedByDescending { it.updatedAtMillis }
            trySend(list)
        }
        awaitClose { registration.remove() }
    }
}
