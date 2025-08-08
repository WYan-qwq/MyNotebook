package com.example.mynotebook.network

import com.example.mynotebook.plan.Plan
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {
    @GET("api/plans/user/{userId}")
    suspend fun getPlansByUser(@Path("userId") userId: Int): Response<List<Plan>>
}