package com.example.mynotebook.me

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mynotebook.api.ChangePasswordRequest
import com.example.mynotebook.api.ProfileResponse
import com.example.mynotebook.api.RetrofitClient
import com.example.mynotebook.api.UpdateProfileRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MeUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val profile: ProfileResponse? = null,
    val saving: Boolean = false,
    val savingError: String? = null
)

class MeViewModel : ViewModel() {

    private val _ui = MutableStateFlow(MeUiState(loading = true))
    val ui: StateFlow<MeUiState> = _ui

    fun load(userId: Int) = viewModelScope.launch {
        _ui.update { it.copy(loading = true, error = null) }
        try {
            val resp = RetrofitClient.api.getProfile(userId)
            if (resp.isSuccessful && resp.body() != null) {
                _ui.update { it.copy(loading = false, profile = resp.body()) }
            } else {
                _ui.update { it.copy(loading = false, error = "Load failed: ${resp.code()}") }
            }
        } catch (e: Exception) {
            _ui.update { it.copy(loading = false, error = e.message ?: "Network error") }
        }
    }

    fun updateProfile(userId: Int, name: String?, picture: String?) = viewModelScope.launch {
        _ui.update { it.copy(saving = true, savingError = null) }
        try {
            val resp = RetrofitClient.api.updateProfile(userId, UpdateProfileRequest(name, picture))
            if (!resp.isSuccessful) throw RuntimeException("HTTP ${resp.code()}")
            // 重新拉取或直接更新本地
            load(userId)
        } catch (e: Exception) {
            _ui.update { it.copy(savingError = e.message ?: "Network error") }
        } finally {
            _ui.update { it.copy(saving = false) }
        }
    }

    fun changePassword(userId: Int, newPwd: String) = viewModelScope.launch {
        _ui.update { it.copy(saving = true, savingError = null) }
        try {
            val resp = RetrofitClient.api.changePassword(userId, ChangePasswordRequest(newPwd))
            if (!resp.isSuccessful) throw RuntimeException("HTTP ${resp.code()}")
        } catch (e: Exception) {
            _ui.update { it.copy(savingError = e.message ?: "Network error") }
        } finally {
            _ui.update { it.copy(saving = false) }
        }
    }
}