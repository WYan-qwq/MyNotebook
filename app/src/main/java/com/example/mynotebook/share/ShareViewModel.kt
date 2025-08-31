package com.example.mynotebook.share

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mynotebook.api.LikeRequest
import com.example.mynotebook.api.ShareView
import com.example.mynotebook.api.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ShareUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val items: List<ShareView> = emptyList(),
    val liked: Set<Int> = emptySet(),
    val likeLoading: Set<Int> = emptySet()
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
    fun toggleLike(share: ShareView, userId: Int) = viewModelScope.launch {
        val id = share.sharingId
        val state = _ui.value
        if (state.likeLoading.contains(id)) return@launch

        val currentlyLiked = state.liked.contains(id)

        // 标记 loading + 乐观更新 likes 数量与 liked 集合
        _ui.update {
            it.copy(
                likeLoading = it.likeLoading + id,
                liked = if (currentlyLiked) it.liked - id else it.liked + id,
                items = it.items.map { s ->
                    if (s.sharingId == id)
                        s.copy(likes = s.likes + if (currentlyLiked) -1 else +1)
                    else s
                }
            )
        }

        try {
            val body = LikeRequest(shareId = id, userId = userId)
            val resp = if (currentlyLiked)
                RetrofitClient.api.unlike(body)
            else
                RetrofitClient.api.like(body)

            if (!resp.isSuccessful) throw RuntimeException("HTTP ${resp.code()}")
        } catch (e: Exception) {
            // 失败回滚
            _ui.update {
                it.copy(
                    liked = if (currentlyLiked) it.liked + id else it.liked - id,
                    items = it.items.map { s ->
                        if (s.sharingId == id)
                            s.copy(likes = s.likes + if (currentlyLiked) +1 else -1)
                        else s
                    },
                    error = e.message ?: "Network error"
                )
            }
        } finally {
            _ui.update { it.copy(likeLoading = it.likeLoading - id) }
        }
    }
}