package com.zizzverse.zizzchat.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zizzverse.zizzchat.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _authState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthUiState.Loading
            val result = repository.login(email, pass)
            if (result.isSuccess) {
                _authState.value = AuthUiState.Success
            } else {
                _authState.value = AuthUiState.Error(result.exceptionOrNull()?.message ?: "Login failed")
            }
        }
    }

    fun signup(name: String, email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthUiState.Loading
            val result = repository.signup(name, email, pass)
            if (result.isSuccess) {
                _authState.value = AuthUiState.Success
            } else {
                _authState.value = AuthUiState.Error(result.exceptionOrNull()?.message ?: "Signup failed")
            }
        }
    }
}

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}
