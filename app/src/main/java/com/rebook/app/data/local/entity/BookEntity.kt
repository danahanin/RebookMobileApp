package com.rebook.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val author: String,
    val description: String,
    val imageUrl: String?,
    val ownerId: String,
    val ownerName: String = "",
    val status: String,
    val requestedById: String?,
    val createdAt: Long = 0L
)
