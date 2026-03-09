package com.rebook.app.data.model

import com.rebook.app.data.local.entity.BookEntity

data class Book(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val description: String = "",
    val imageUrl: String? = null,
    val ownerId: String = "",
    val ownerName: String = "",
    val status: BookStatus = BookStatus.AVAILABLE,
    val requestedById: String? = null,
    val createdAt: Long = 0L
) {
    fun toEntity(): BookEntity = BookEntity(
        id = id,
        title = title,
        author = author,
        description = description,
        imageUrl = imageUrl,
        ownerId = ownerId,
        ownerName = ownerName,
        status = status.name,
        requestedById = requestedById,
        createdAt = createdAt
    )

    companion object {
        fun fromEntity(entity: BookEntity): Book = Book(
            id = entity.id,
            title = entity.title,
            author = entity.author,
            description = entity.description,
            imageUrl = entity.imageUrl,
            ownerId = entity.ownerId,
            ownerName = entity.ownerName,
            status = BookStatus.valueOf(entity.status),
            requestedById = entity.requestedById,
            createdAt = entity.createdAt
        )
    }
}
