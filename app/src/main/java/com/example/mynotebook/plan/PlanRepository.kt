package com.example.mynotebook.plan

import com.example.mynotebook.api.ApiService

class PlanRepository(private val api: ApiService) {
    suspend fun fetchPlans(userId: Int): List<Plan> {
        val resp = api.getPlansByUser(userId)
        if (resp.isSuccessful) {
            return resp.body() ?: emptyList()
        } else {
            throw RuntimeException("Error ${resp.code()}: ${resp.message()}")
        }
    }
}