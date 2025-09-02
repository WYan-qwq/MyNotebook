package com.example.mynotebook.api

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ========= Auth =========
    @POST("/api/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<UserResponse>

    @POST("/api/auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<UserResponse>

    // ========= Plans =========
    @GET("/api/plans/day")
    suspend fun getPlansForDay(
        @Query("userId") userId: Int,
        @Query("date") date: String
    ): Response<List<PlanItem>>

    @POST("/api/plans")
    suspend fun createPlan(@Body body: PlanCreateRequest): Response<PlanItem>

    @GET("/api/plans/week")
    suspend fun getPlansForWeek(
        @Query("userId") userId: Int,
        @Query("date") date: String
    ): Response<List<PlanItem>>

    @PUT("/api/plans/{id}")
    suspend fun updatePlan(
        @Path("id") id: Int,
        @Body body: PlanUpdateRequest
    ): Response<PlanItem>

    @DELETE("/api/plans/{id}")
    suspend fun deletePlan(@Path("id") id: Int): Response<Unit>

    // ========= Share =========
    @GET("/api/share/list")
    suspend fun listShares(@Query("userId") userId: Int? = null): Response<List<ShareView>>

    @POST("/api/share/like")
    suspend fun like(@Body body: LikeRequest): Response<Unit>

    @POST("/api/share/unlike")
    suspend fun unlike(@Body body: LikeRequest): Response<Unit>

    @GET("/api/share/liked")
    suspend fun hasLiked(
        @Query("userId") userId: Int,
        @Query("shareId") shareId: Int
    ): Response<LikedResp>

    @POST("/api/share/create")
    suspend fun createShare(@Body req: ShareCreateRequest): Response<Unit>

    @GET("/api/plans/day")
    suspend fun getPlansByDate(
        @Query("userId") userId: Int,
        @Query("date") date: String
    ): Response<List<PlanBrief>>

    // ========= Comments =========
    @GET("/api/share/{id}/comments")
    suspend fun listComments(@Path("id") shareId: Int): Response<List<CommentView>>

    @POST("/api/share/{id}/addcomments")
    suspend fun addComment(
        @Path("id") id: Int,
        @Body body: CommentCreateRequest
    ): Response<Unit>

    @DELETE("/api/share/deletecomments/{commentId}")
    suspend fun deleteComment(@Path("commentId") commentId: Int): Response<Any>

    @GET("/api/user/{id}/profile")
    suspend fun getProfile(@Path("id") id: Int): Response<ProfileResponse>

    // 修改用户名 / 头像（任意字段可空）
    @PUT("/api/user/{id}/profile")
    suspend fun updateProfile(
        @Path("id") id: Int,
        @Body req: UpdateProfileRequest
    ): Response<ProfileResponse>

    // 修改密码
    @PUT("/api/user/{id}/password")
    suspend fun changePassword(
        @Path("id") id: Int,
        @Body req: ChangePasswordRequest
    ): Response<Map<String, Any>>

    // 上传头像（Multipart）
    @Multipart
    @POST("/api/user/{id}/avatar")
    suspend fun uploadAvatar(
        @Path("id") id: Int,
        @Part file: MultipartBody.Part
    ): Response<UploadAvatarResponse>
}