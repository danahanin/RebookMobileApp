package com.rebook.app.data.model

data class ChatThreadSummary(
    val buyerId: String,
    val updatedAtMillis: Long,
    val buyerDisplayName: String? = null
)
