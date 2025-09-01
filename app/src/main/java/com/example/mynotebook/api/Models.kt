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
    val createTime: String,     // ISO-8601
    val likes: Int,
    val comments: Int,
    val author: Author,         // 作者（user_info）
    val share: ShareContent,    // 分享文案
    val planDate: String,       // "YYYY-MM-DD"
    val plans: List<PlanBrief>  // 这一天的所有计划
)

data class Author(
    val userId: Int,
    val userName: String?,      // 后端可能为空
    val picture: String?        // 头像 URL，可为空
)

data class ShareContent(
    val title: String?,
    val details: String?
)

data class PlanBrief(
    val id: Int,
    val date: String,           // "YYYY-MM-DD"
    val hour: Int?,
    val minute: Int?,
    val title: String?,
    val details: String?,
    val alarm: Int?,
    val finished: Int?
)

data class LikeRequest(
    val shareId: Int,
    val userId: Int
)

data class LikedResp(
    val userId:Int,
    val shareId:Int,
    val liked:Boolean
)

data class ShareCreateRequest(
    val userId: Int,
    val planDate: String,   // 形如 2025-08-31
    val title: String?,
    val details: String?
)