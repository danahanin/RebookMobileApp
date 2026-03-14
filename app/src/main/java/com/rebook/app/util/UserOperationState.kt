package com.rebook.app.util

sealed class UserOperationState {
    object Idle : UserOperationState()
    object Loading : UserOperationState()
    object Success : UserOperationState()
    data class Error(val message: String) : UserOperationState()
}
