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