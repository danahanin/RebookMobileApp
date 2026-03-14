package com.rebook.app.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rebook.app.data.model.User
import com.rebook.app.data.repository.UserRepository
import com.rebook.app.util.UserOperationState
import kotlinx.coroutines.launch

class UserViewModel : ViewModel() {

    private val repository = UserRepository()

    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    private val _operationState = MutableLiveData<UserOperationState>(UserOperationState.Idle)
    val operationState: LiveData<UserOperationState> = _operationState

    private val _uploadedImageUrl = MutableLiveData<String?>()
    val uploadedImageUrl: LiveData<String?> = _uploadedImageUrl

    init {
        loadCurrentUser()
    }

    fun loadCurrentUser() {
        viewModelScope.launch {
            _currentUser.value = repository.getCurrentUserFromFirestore()
        }
    }

    fun getCurrentUserId(): String? = repository.getCurrentUserId()

    fun updateProfile(displayName: String, imageUrl: String?) {
        _operationState.value = UserOperationState.Loading
        viewModelScope.launch {
            val result = repository.updateProfile(displayName, imageUrl)
            _operationState.value = if (result.isSuccess) {
                loadCurrentUser()
                UserOperationState.Success
            } else {
                UserOperationState.Error(result.exceptionOrNull()?.message ?: "Update failed")
            }
        }
    }

    fun uploadProfileImage(imageUri: Uri) {
        _operationState.value = UserOperationState.Loading
        viewModelScope.launch {
            val result = repository.uploadProfileImage(imageUri)
            if (result.isSuccess) {
                _uploadedImageUrl.value = result.getOrNull()
                _operationState.value = UserOperationState.Success
            } else {
                _operationState.value = UserOperationState.Error(
                    result.exceptionOrNull()?.message ?: "Upload failed"
                )
            }
        }
    }

    fun resetOperationState() {
        _operationState.value = UserOperationState.Idle
    }

    fun clearUploadedImageUrl() {
        _uploadedImageUrl.value = null
    }
}
