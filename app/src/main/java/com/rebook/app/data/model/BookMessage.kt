package com.rebook.app.data.model

import com.google.firebase.firestore.DocumentSnapshot

data class BookMessage(
    val id: String,
    val senderId: String,
    val text: String,
    val createdAt: Long
)

fun bookMessageFromFirestoreData(id: String, data: Map<String, Any>?): BookMessage? {
    if (data == null) return null
    val senderId = data["senderId"] as? String ?: return null
    val text = data["text"] as? String ?: return null
    val createdAt = when (val raw = data["createdAt"]) {
        is Long -> raw
        is Number -> raw.toLong()
        else -> return null
    }
    return BookMessage(id = id, senderId = senderId, text = text, createdAt = createdAt)
}

fun DocumentSnapshot.toBookMessage(): BookMessage? =
    bookMessageFromFirestoreData(id, data)
