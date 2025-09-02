package com.example.mynotebook.share

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mynotebook.api.CommentCreateRequest
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
import java.time.Instant

data class ShareUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val items: List<ShareView> = emptyList(),
    val liked: Set<Int> = emptySet(),        // 已点赞的 sharingId 集合
    val likeLoading: Set<Int> = emptySet()   // 正在切换点赞状态的 sharingId 集合
)

data class CommentsUi(
    val loading: Boolean = false,
    val error: String? = null,
    val items: List<CommentView> = emptyList()
)

// --------------------- ViewModel ---------------------

class ShareViewModel : ViewModel() {

    // 分享列表
    private val _ui = MutableStateFlow(ShareUiState())
    val ui: StateFlow<ShareUiState> = _ui

    // 每条分享的评论： key = shareId
    private val _comments = MutableStateFlow<Map<Int, CommentsUi>>(emptyMap())
    val comments: StateFlow<Map<Int, CommentsUi>> = _comments

    /**
     * 刷新分享列表；若传 userId，会并发查询每条分享的“是否已点赞”
     */
    fun refresh(userId: Int? = null) = viewModelScope.launch {
        _ui.update { it.copy(loading = true, error = null) }
        try {
            val listResp = RetrofitClient.api.listShares(null) // 后端若支持可传 userId 过滤
            if (!listResp.isSuccessful || listResp.body() == null) {
                _ui.update { it.copy(loading = false, error = "Load failed: ${listResp.code()}") }
                return@launch
            }
            val items = listResp.body()!!

            val likedSet: Set<Int> =
                if (userId != null) queryLikedSet(userId, items) else emptySet()

            _ui.update { it.copy(loading = false, items = items, liked = likedSet) }
        } catch (e: Exception) {
            _ui.update { it.copy(loading = false, error = e.message ?: "Network error") }
        }
    }

    /**
     * 点赞/取消赞（乐观更新 + 失败回滚；对 404/409 做幂等处理）
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

            // 把 404(已取消过) / 409(已点过赞) 当作幂等成功
            val ok = resp.isSuccessful ||
                    (currentlyLiked && resp.code() == 404) ||
                    (!currentlyLiked && resp.code() == 409)

            if (!ok) rollbackLike(id, currentlyLiked)
        } catch (_: Exception) {
            rollbackLike(id, currentlyLiked)
        } finally {
            _ui.update { it.copy(likeLoading = it.likeLoading - id) }
        }
    }

    private fun rollbackLike(shareId: Int, wasLiked: Boolean) {
        _ui.update {
            it.copy(
                liked = if (wasLiked) it.liked + shareId else it.liked - shareId,
                items = it.items.map { s ->
                    if (s.sharingId == shareId)
                        s.copy(likes = s.likes + if (wasLiked) +1 else -1)
                    else s
                }
            )
        }
    }

    /**
     * 并发查询所有分享是否已点赞，返回已点赞的 sharingId 集合
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

    // --------------------- Comments ---------------------

    /** 加载某条分享的评论 */
    fun loadComments(shareId: Int) = viewModelScope.launch {
        _comments.update {
            it + (shareId to (it[shareId]?.copy(loading = true, error = null)
                ?: CommentsUi(loading = true)))
        }
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

    /** 新增评论（顶层评论 preCommentId 传 null；回复传目标 commentId） */
    fun addComment(
        shareId: Int,
        userId: Int,
        content: String,
        preCommentId: Int?
    ) = viewModelScope.launch {
        if (content.isBlank()) return@launch

        // 标记该 share 评论进入加载
        _comments.update { it + (shareId to CommentsUi(loading = true)) }

        try {
            val body = CommentCreateRequest(
                userId = userId,
                sharingId = shareId,
                content = content.trim(),
                createTime = Instant.now().toString(),
                preCommentId = preCommentId
            )
            val resp = RetrofitClient.api.addComment(shareId, body)
            if (!resp.isSuccessful) throw RuntimeException("HTTP ${resp.code()}")

            // 刷新这条分享的评论
            loadComments(shareId)

            // 可选：把分享列表里的 comments +1（乐观更新）
            _ui.update { state ->
                state.copy(
                    items = state.items.map {
                        if (it.sharingId == shareId) it.copy(comments = it.comments + 1) else it
                    }
                )
            }
        } catch (e: Exception) {
            _comments.update { it + (shareId to CommentsUi(error = e.message ?: "Network error")) }
        }
    }

    /** 删除评论（成功后刷新当前分享的评论） */
    fun deleteComment(shareId: Int, commentId: Int) = viewModelScope.launch {
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