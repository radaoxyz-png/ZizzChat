package com.zizzverse.zizzchat.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zizzverse.zizzchat.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(private val repository: AuthRepository = AuthRepository()) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = repository.loginUser(email, password)
            if (result.isSuccess) {
                _uiState.value = AuthUiState.Success
            } else {
                val errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Login failed"
                _uiState.value = AuthUiState.Error(errorMessage)
            }
        }
    }

    fun signup(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = repository.signupUser(email, password)
            if (result.isSuccess) {
                _uiState.value = AuthUiState.Success
            } else {
                val errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Signup failed"
                _uiState.value = AuthUiState.Error(errorMessage)
            }
        }
    }
}
