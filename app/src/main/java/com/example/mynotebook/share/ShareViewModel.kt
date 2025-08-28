package com.example.mynotebook.share

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mynotebook.api.ShareView
import com.example.mynotebook.api.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ShareUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val items: List<ShareView> = emptyList()
)

class ShareViewModel : ViewModel() {
    private val _ui = MutableStateFlow(ShareUiState(loading = true))
    val ui: StateFlow<ShareUiState> = _ui

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _ui.value = _ui.value.copy(loading = true, error = null)
        try {
            val resp = RetrofitClient.api.listShares(null)
            if (resp.isSuccessful) {
                _ui.value = ShareUiState(loading = false, items = resp.body().orEmpty())
            } else {
                _ui.value = ShareUiState(loading = false, error = "Load failed: ${resp.code()}")
            }
        } catch (e: Exception) {
            _ui.value = ShareUiState(loading = false, error = e.message ?: "Network error")
        }
    }
}