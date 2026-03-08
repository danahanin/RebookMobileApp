package com.rebook.app.data.model

import com.rebook.app.data.local.entity.UserEntity

data class User(
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val profileImageUrl: String? = null
) {
    fun toEntity(): UserEntity = UserEntity(
        id = id,
        email = email,
        displayName = displayName,
        profileImageUrl = profileImageUrl
    )

    companion object {
        fun fromEntity(entity: UserEntity): User = User(
            id = entity.id,
            email = entity.email,
            displayName = entity.displayName,
            profileImageUrl = entity.profileImageUrl
        )
    }
}
