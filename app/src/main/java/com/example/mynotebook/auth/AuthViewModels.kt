package com.example.mynotebook.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mynotebook.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val userName: String = "",
    val loading: Boolean = false,
    val error: String? = null,
)

class LoginViewModel : ViewModel() {
    private val _ui = MutableStateFlow(AuthUiState())
    val ui: StateFlow<AuthUiState> = _ui

    fun onEmailChange(v: String) { _ui.value = _ui.value.copy(email = v) }
    fun onPasswordChange(v: String) { _ui.value = _ui.value.copy(password = v) }
    fun clearError() { _ui.value = _ui.value.copy(error = null) }

    // 回调类型一定是 (UserResponse) -> Unit
    fun login(onSuccess: (UserResponse) -> Unit) {
        val s = _ui.value
        if (s.email.isBlank() || s.password.isBlank()) {
            _ui.value = s.copy(error = "Email and password are required.")
            return
        }
        viewModelScope.launch {
            _ui.value = s.copy(loading = true, error = null)
            try {
                val resp = RetrofitClient.api.login(LoginRequest(s.email.trim(), s.password))
                if (resp.isSuccessful && resp.body() != null) {
                    onSuccess(resp.body()!!)
                    _ui.value = _ui.value.copy(loading = false)
                } else {
                    _ui.value = _ui.value.copy(
                        loading = false,
                        error = if (resp.code() == 401) "Invalid email or password."
                        else "Login failed (${resp.code()})."
                    )
                }
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(loading = false, error = e.message ?: "Network error.")
            }
        }
    }
}

class RegisterViewModel : ViewModel() {
    private val _ui = MutableStateFlow(AuthUiState())
    val ui: StateFlow<AuthUiState> = _ui

    fun onEmailChange(v: String) { _ui.value = _ui.value.copy(email = v) }
    fun onPasswordChange(v: String) { _ui.value = _ui.value.copy(password = v) }
    fun onUserNameChange(v: String) { _ui.value = _ui.value.copy(userName = v) }
    fun clearError() { _ui.value = _ui.value.copy(error = null) }

    fun register(onSuccess: (UserResponse) -> Unit) {
        val s = _ui.value
        if (s.email.isBlank() || s.password.isBlank()) {
            _ui.value = s.copy(error = "Email and password are required.")
            return
        }
        viewModelScope.launch {
            _ui.value = s.copy(loading = true, error = null)
            try {
                val body = RegisterRequest(
                    email = s.email.trim(),
                    password = s.password,
                    userName = s.userName.ifBlank { null }
                )
                val resp = RetrofitClient.api.register(body)
                if (resp.isSuccessful && resp.body() != null) {
                    onSuccess(resp.body()!!)
                    _ui.value = _ui.value.copy(loading = false)
                } else {
                    _ui.value = _ui.value.copy(
                        loading = false,
                        error = if (resp.code() == 409) "This email is already registered."
                        else "Registration failed (${resp.code()})."
                    )
                }
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(loading = false, error = e.message ?: "Network error.")
            }
        }
    }
}