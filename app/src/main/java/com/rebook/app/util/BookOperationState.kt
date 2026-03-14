package com.rebook.app.util

sealed class BookOperationState {
    object Idle : BookOperationState()
    object Loading : BookOperationState()
    object Success : BookOperationState()
    data class Error(val message: String) : BookOperationState()
}
