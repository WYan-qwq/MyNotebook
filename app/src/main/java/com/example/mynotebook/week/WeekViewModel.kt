package com.example.mynotebook.week

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mynotebook.api.PlanItem
import com.example.mynotebook.api.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

data class DayGroup(
    val date: LocalDate,
    val plans: List<PlanItem> = emptyList()
) {
    val total: Int get() = plans.size
    val done: Int get() = plans.count { (it.finished ?: 0) == 1 }
    val ratio: Double get() = if (total == 0) 0.0 else done.toDouble() / total
}

data class WeekUiState(
    val weekStart: LocalDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
    val weekEnd: LocalDate = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)),
    val groups: List<DayGroup> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val expanded: LocalDate? = null,
    val today: LocalDate = LocalDate.now()
)

class WeekViewModel(private val userId: Int) : ViewModel() {

    private val _ui = MutableStateFlow(WeekUiState())
    val ui: StateFlow<WeekUiState> = _ui

    init { loadFor(LocalDate.now()) }

    private fun computeRange(anchor: LocalDate): Pair<LocalDate, LocalDate> {
        val start = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val end = anchor.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        return start to end
    }

    fun prevWeek() = loadFor(_ui.value.weekStart.minusDays(1))
    fun nextWeek() = loadFor(_ui.value.weekEnd.plusDays(1))
    fun setWeekByDate(d: LocalDate) = loadFor(d)

    fun toggleDay(date: LocalDate) {
        _ui.value = _ui.value.copy(expanded = if (_ui.value.expanded == date) null else date)
    }

    private fun sortPlans(list: List<PlanItem>) =
        list.sortedWith(compareBy({ it.date ?: "" }, { it.hour ?: 0 }, { it.minute ?: 0 }, { it.id }))

    private fun toLocalDate(s: String?): LocalDate? =
        try { if (s == null) null else LocalDate.parse(s) } catch (_: Exception) { null }

    private fun buildSevenDays(start: LocalDate, raw: List<PlanItem>): List<DayGroup> {
        val map = raw.groupBy { toLocalDate(it.date) ?: _ui.value.today }
        return (0..6).map { offset ->
            val d = start.plusDays(offset.toLong())
            DayGroup(d, sortPlans(map[d].orEmpty()))
        }
    }

    private fun loadFor(anyDate: LocalDate) {
        val (start, end) = computeRange(anyDate)
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true, error = null, weekStart = start, weekEnd = end)
            try {
                val resp = RetrofitClient.api.getPlansForWeek(
                    userId = userId,
                    date = anyDate.format(DateTimeFormatter.ISO_DATE)
                )
                if (resp.isSuccessful) {
                    val list = resp.body().orEmpty()
                    _ui.value = _ui.value.copy(
                        loading = false,
                        groups = buildSevenDays(start, list)
                    )
                } else {
                    _ui.value = _ui.value.copy(loading = false, error = "Load failed (${resp.code()})")
                }
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(loading = false, error = e.message ?: "Network error")
            }
        }
    }

    fun deletePlan(id: Int) {
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.api.deletePlan(id)
                if (resp.isSuccessful) {
                    // 用当前周起始日作为锚点刷新
                    loadFor(_ui.value.weekStart)
                } else {
                    // 可选：_ui.value = _ui.value.copy(error = "Delete failed (${resp.code()})")
                }
            } catch (e: Exception) {
                // 可选：_ui.value = _ui.value.copy(error = e.message ?: "Network error")
            }
        }
    }

    companion object {
        fun provideFactory(userId: Int): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return WeekViewModel(userId) as T
                }
            }
    }
}