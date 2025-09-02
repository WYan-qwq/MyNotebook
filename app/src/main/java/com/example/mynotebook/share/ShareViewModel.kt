package com.example.mynotebook.share

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mynotebook.api.CommentView
import com.example.mynotebook.api.LikeRequest
import com.example.mynotebook.api.RetrofitClient
import com.example.mynotebook.api.ShareView
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ShareUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val items: List<ShareView> = emptyList(),
    val liked: Set<Int> = emptySet(),        // 已点赞的 sharingId 集合
    val likeLoading: Set<Int> = emptySet()   // 正在切换点赞状态的 sharingId
)
data class CommentsUi(
    val loading: Boolean = false,
    val error: String? = null,
    val items: List<CommentView> = emptyList()
)
class ShareViewModel : ViewModel() {
    private val _ui = MutableStateFlow(ShareUiState())
    val ui: StateFlow<ShareUiState> = _ui

    /**
     * 刷新列表；如果传入 userId，会顺带并发查询每条分享是否已点赞。
     * 注意：建议在进入 Share 页时显式调用 refresh(userId)。
     */
    fun refresh(userId: Int? = null) = viewModelScope.launch {
        _ui.update { it.copy(loading = true, error = null) }
        try {
            val listResp = RetrofitClient.api.listShares(null)   // 这里后端可选支持按 userId 过滤
            if (!listResp.isSuccessful || listResp.body() == null) {
                _ui.update { it.copy(loading = false, error = "Load failed: ${listResp.code()}") }
                return@launch
            }
            val items = listResp.body()!!

            // 并发查询“是否已点赞”
            val likedSet: Set<Int> =
                if (userId != null) queryLikedSet(userId, items) else emptySet()

            _ui.update { it.copy(loading = false, items = items, liked = likedSet) }
        } catch (e: Exception) {
            _ui.update { it.copy(loading = false, error = e.message ?: "Network error") }
        }
    }

    /**
     * 点赞/取消赞（乐观更新 + 失败回滚）
     */
    fun toggleLike(share: ShareView, userId: Int) = viewModelScope.launch {
        val id = share.sharingId
        val state = _ui.value
        if (state.likeLoading.contains(id)) return@launch

        val currentlyLiked = state.liked.contains(id)

        // 1) 乐观更新
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
            val resp = if (currentlyLiked) {
                RetrofitClient.api.unlike(body)
            } else {
                RetrofitClient.api.like(body)
            }

            // 👉 把 404(已取消过) / 409(已点过赞) 当作幂等成功，别回滚、也别抛错
            val ok = resp.isSuccessful ||
                    (currentlyLiked && resp.code() == 404) ||
                    (!currentlyLiked && resp.code() == 409)

            if (!ok) {
                // 回滚，不设置 ui.error，避免整页错误态
                _ui.update {
                    it.copy(
                        liked = if (currentlyLiked) it.liked + id else it.liked - id,
                        items = it.items.map { s ->
                            if (s.sharingId == id)
                                s.copy(likes = s.likes + if (currentlyLiked) +1 else -1)
                            else s
                        }
                    )
                }
            }
        } catch (_: Exception) {
            // 网络异常时回滚，同样不设置 ui.error（可按需做 snackbar）
            _ui.update {
                it.copy(
                    liked = if (currentlyLiked) it.liked + id else it.liked - id,
                    items = it.items.map { s ->
                        if (s.sharingId == id)
                            s.copy(likes = s.likes + if (currentlyLiked) +1 else -1)
                        else s
                    }
                )
            }
        } finally {
            _ui.update { it.copy(likeLoading = it.likeLoading - id) }
        }
    }
    /**
     * 并发查询所有分享的 liked 状态，返回已点赞的 sharingId 集合
     */
    private suspend fun queryLikedSet(userId: Int, items: List<ShareView>): Set<Int> =
        coroutineScope {
            items.map { share ->
                async {
                    try {
                        val r = RetrofitClient.api.hasLiked(userId, share.sharingId)
                        share.sharingId.takeIf { r.isSuccessful && r.body()?.liked == true }
                    } catch (_: Exception) {
                        null
                    }
                }
            }.mapNotNull { it.await() }.toSet()
        }
    private val _comments = MutableStateFlow<Map<Int, CommentsUi>>(emptyMap())
    val comments: StateFlow<Map<Int, CommentsUi>> = _comments

    fun loadComments(shareId: Int) = viewModelScope.launch {
        _comments.update { it + (shareId to (it[shareId]?.copy(loading = true, error = null)
            ?: CommentsUi(loading = true))) }
        try {
            val resp = RetrofitClient.api.listComments(shareId)
            if (resp.isSuccessful) {
                _comments.update { it + (shareId to CommentsUi(items = resp.body().orEmpty())) }
            } else {
                _comments.update { it + (shareId to CommentsUi(error = "Load comments failed: ${resp.code()}")) }
            }
        } catch (e: Exception) {
            _comments.update { it + (shareId to CommentsUi(error = e.message ?: "Network error")) }
        }
    }

    fun deleteComment(shareId: Int, commentId: Int) = viewModelScope.launch {
        // 简单做法：直接调用，成功后刷新
        try {
            val resp = RetrofitClient.api.deleteComment(commentId)
            if (resp.isSuccessful) {
                loadComments(shareId)
            } else {
                _comments.update { m ->
                    val old = m[shareId] ?: CommentsUi()
                    m + (shareId to old.copy(error = "Delete failed: ${resp.code()}"))
                }
            }
        } catch (e: Exception) {
            _comments.update { m ->
                val old = m[shareId] ?: CommentsUi()
                m + (shareId to old.copy(error = e.message ?: "Network error"))
            }
        }
    }
}