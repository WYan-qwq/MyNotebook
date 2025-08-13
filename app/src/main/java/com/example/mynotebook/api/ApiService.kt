package com.example.mynotebook.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @POST("/api/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<UserResponse>

    @POST("/api/auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<UserResponse>

    @GET("/api/plans/day")
    suspend fun getPlansForDay(
        @Query("userId") userId: Int,
        @Query("date") date: String    // "YYYY-MM-DD"
    ): Response<List<PlanItem>>
}