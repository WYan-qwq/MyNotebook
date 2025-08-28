package com.example.mynotebook.api

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val userName: String? = null,
    val picture: String? = null
)

data class UserResponse(
    val id: Int,
    val userName: String?,
    val email: String,
    val picture: String?
)

data class PlanItem(
    val id: Int,
    val date: String,        // "YYYY-MM-DD"
    val hour: Int?,
    val minute: Int?,
    val title: String?,
    val details: String?,
    val alarm: Int?,
    val finished: Int?
)

data class PlanCreateRequest(
    val userId: Int,
    val createTime: String,   // ISO-8601 instant, e.g. 2025-08-14T02:10:00Z
    val date: String,         // "YYYY-MM-DD"（对应后端 LocalDate）
    val hour: Int,
    val minute: Int,
    val title: String?,
    val details: String?,
    val alarm: Int = 0,       // 0/1
    val finished: Int = 0
)

data class PlanUpdateRequest(
    val userId: Int? = null,
    val createTime: String? = null,
    val date: String? = null,
    val hour: Int? = null,
    val minute: Int? = null,
    val title: String? = null,
    val details: String? = null,
    val alarm: Int? = null,
    val finished: Int? = null
)

data class ShareView(
    val sharingId: Int,
    val createTime: String,   // 后端返回 ISO-8601 字符串
    val likes: Int,
    val comments: Int,
    val author: Author,       // 作者信息
    val share: ShareContent,  // 分享的标题/详情（独立于计划）
    val plan: PlanBrief       // 被分享的计划（简版）
)

// 作者信息（来自 user_info）
data class Author(
    val userId: Int,
    val userName: String,
    val picture: String?      // 头像 URL，可为空
)

// 分享卡片本身的文案
data class ShareContent(
    val title: String,
    val details: String?
)

// 被分享的计划简要信息（字段与后端 ShareDtos.PlanBrief 对应）
data class PlanBrief(
    val id: Int,
    val date: String,         // 为了简单按字符串接收（后端若是 LocalDate/Instant 都可以）
    val hour: Int?,
    val minute: Int?,
    val title: String?,
    val details: String?,
    val alarm: Int?,
    val finished: Int?
)