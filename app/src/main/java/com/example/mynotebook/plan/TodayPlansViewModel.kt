package com.example.mynotebook.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mynotebook.api.PlanItem
import com.example.mynotebook.api.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class DayUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val items: List<PlanItem> = emptyList(),
    val date: LocalDate = LocalDate.now()
)

class TodayPlansViewModel(private val userId: Int) : ViewModel() {
    private val _ui = MutableStateFlow(DayUiState())
    val ui: StateFlow<DayUiState> = _ui

    init { refresh() }

    fun refresh(forDate: LocalDate = _ui.value.date) {
        val dateStr = forDate.format(DateTimeFormatter.ISO_DATE)
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true, error = null, date = forDate)
            try {
                val resp = RetrofitClient.api.getPlansForDay(userId, dateStr)
                if (resp.isSuccessful) {
                    val sorted = (resp.body().orEmpty()).sortedWith(
                        compareBy({ it.hour ?: 0 }, { it.minute ?: 0 }, { it.id })
                    )
                    _ui.value = _ui.value.copy(loading = false, items = sorted)
                } else {
                    _ui.value = _ui.value.copy(loading = false, error = "Load failed (${resp.code()})")
                }
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(loading = false, error = e.message ?: "Network error")
            }
        }
    }

    companion object {
        fun provideFactory(userId: Int): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TodayPlansViewModel(userId) as T
                }
            }
    }
}