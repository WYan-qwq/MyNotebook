package com.example.mynotebook.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
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

    @POST("/api/plans")
    suspend fun createPlan(@Body body: PlanCreateRequest): Response<PlanItem>

    @GET("/api/plans/week")
    suspend fun getPlansForWeek(
        @Query("userId") userId: Int,
        @Query("date") date: String   // YYYY-MM-DD
    ): Response<List<PlanItem>>

    @PUT("/api/plans/{id}")
    suspend fun updatePlan(
        @Path("id") id: Int,
        @Body body: PlanUpdateRequest
    ): Response<PlanItem>

    @DELETE("/api/plans/{id}")
    suspend fun deletePlan(@Path("id") id: Int): Response<Unit>

    @GET("/api/share/list")
    suspend fun listShares(@Query("userId") userId: Int? = null): Response<List<ShareView>>
}