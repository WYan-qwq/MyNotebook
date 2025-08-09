package com.example.mynotebook.api

import com.example.mynotebook.plan.Plan
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @POST("/api/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<UserResponse>

    @POST("/api/auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<UserResponse>


    @GET("api/plans/user/{userId}")
    suspend fun getPlansByUser(@Path("userId") userId: Int): Response<List<Plan>>
}