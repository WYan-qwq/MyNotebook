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