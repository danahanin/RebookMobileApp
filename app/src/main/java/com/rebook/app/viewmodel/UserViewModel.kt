package com.rebook.app.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.rebook.app.data.model.User
import com.rebook.app.data.repository.UserRepository
import com.rebook.app.util.UserOperationState
import kotlinx.coroutines.launch

class UserViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UserRepository(application)

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
        _currentUser.value = repository.getCurrentUser()
        viewModelScope.launch {
            repository.syncCurrentUser()
        }
    }

    fun updateProfile(displayName: String, imageUrl: String?) {
        _operationState.value = UserOperationState.Loading
        viewModelScope.launch {
            val result = repository.updateProfile(displayName, imageUrl)
            _operationState.value = if (result.isSuccess) {
                loadCurrentUser()
                UserOperationState.Success
            } else {
                UserOperationState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to update profile"
                )
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
                    result.exceptionOrNull()?.message ?: "Failed to upload image"
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
