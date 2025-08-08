package com.example.mynotebook.plan

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mynotebook.network.RetrofitClient
import kotlinx.coroutines.launch

class PlanViewModel : ViewModel() {
    private val repo = PlanRepository(RetrofitClient.apiService)

    private val _plans = MutableLiveData<List<Plan>>()
    val plans: LiveData<List<Plan>> = _plans

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadPlans(userId: Int) {
        viewModelScope.launch {
            try {
                val list = repo.fetchPlans(userId)
                _plans.value = list
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}